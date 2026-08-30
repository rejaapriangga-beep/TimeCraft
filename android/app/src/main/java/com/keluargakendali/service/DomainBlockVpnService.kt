package com.keluargakendali.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.keluargakendali.R
import com.keluargakendali.data.LocaleHelper
import com.keluargakendali.data.PactioApi
import com.keluargakendali.data.SecureTokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Menegakkan daftar blokir domain/subdomain yang diatur orang tua (lihat GET
 * /family/blocked-domains di server.js & BlockedDomainsDialog di ParentScreen.kt) lewat filter
 * DNS lokal - VPN lokal resmi Android (VpnService, TIDAK butuh root, TIDAK accessibility
 * service). Selalu terlihat pengguna (ikon kunci VPN sistem + notifikasi persisten di bawah,
 * bisa dicabut kapan saja lewat Pengaturan sistem) - konsisten dengan prinsip transparansi yang
 * sama seperti DeviceLockService.
 *
 * PENTING - cara kerja & batasannya (baca sebelum mengubah kode ini):
 * - VPN builder di sini SENGAJA cuma merutekan trafik ke TUNNEL_ADDRESS (alamat DNS palsu yang
 *   diberitahukan ke sistem lewat addDnsServer) lewat tunnel - BUKAN seluruh trafik perangkat.
 *   Jadi hanya query DNS yang lewat sini; app lain tetap browsing langsung seperti biasa, TIDAK
 *   ada risiko "internet mati total" kalau ada bug di parsing di bawah (worst case: resolusi DNS
 *   gagal untuk satu query, bukan semua trafik terputus).
 * - Filter ini murni berbasis NAMA DOMAIN yang di-resolve (DNS), bukan memeriksa isi trafik
 *   terenkripsi (HTTPS) - tidak pernah mendekripsi/membaca konten apa pun. Anak yang sengaja
 *   mengganti pengaturan DNS pribadi/pakai app DoH (DNS-over-HTTPS) pihak ketiga berpotensi
 *   melewati filter ini - keterbatasan yang disadari & disepakati (bukan celah tak terduga),
 *   lihat diskusi desain fitur ini. Android sendiri otomatis menonaktifkan DNS privat pengguna
 *   selama VPN lokal aktif, jadi celah ini perlu langkah sadar dari anak, bukan otomatis lolos.
 * - Setiap paket diproses dalam try-catch terpisah (lihat tunnelLoop) - paket yang gagal
 *   di-parse cukup DILEWATI (diteruskan apa adanya / diabaikan), TIDAK PERNAH menghentikan
 *   service atau memblokir trafik yang seharusnya tidak diblokir (fail-open, sama filosofinya
 *   dengan default locked=false di DeviceLockService saat status belum diketahui).
 */
class DomainBlockVpnService : VpnService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null
    private var tunnelJob: Job? = null
    @Volatile private var vpnInterface: ParcelFileDescriptor? = null
    @Volatile private var blockedSuffixes: Set<String> = emptySet()

    // Sama alasannya dengan DeviceLockService.attachBaseContext - notifikasi service ini perlu
    // ikut bahasa yang dipilih pengguna di dalam aplikasi, bukan cuma bahasa sistem HP.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        vpnInterface = runCatching { establishTunnel() }.getOrNull()
        syncJob = scope.launch { syncLoop() }
        tunnelJob = scope.launch {
            tunnelLoop()
            // tunnelLoop keluar (tunnel gagal dibuat, fd rusak/ditutup, atau error tak terduga
            // lain) - JANGAN biarkan VPN "zombie" tetap aktif tanpa ada yang memproses paket:
            // sistem sudah terlanjur merutekan semua query DNS perangkat ke tunnel ini (lihat
            // addDnsServer di establishTunnel), jadi kalau dibiarkan, SEMUA resolusi DNS gagal
            // total (bukan cuma domain yang mau diblokir) - jauh lebih parah daripada fitur ini
            // sekadar tidak berfungsi. Matikan diri sendiri supaya sistem otomatis kembali ke
            // DNS normal, bukan diam saja dalam keadaan rusak.
            Log.w(TAG, "tunnelLoop berhenti - menghentikan service supaya perangkat kembali ke DNS normal.")
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        syncJob?.cancel()
        tunnelJob?.cancel()
        runCatching { vpnInterface?.close() }
        vpnInterface = null
        super.onDestroy()
    }

    /**
     * Dipanggil sistem kalau pengguna mencabut izin VPN lewat Pengaturan, atau aplikasi VPN
     * lain diaktifkan (cuma boleh 1 VPN aktif per waktu di Android) - berhenti bersih, JANGAN
     * coba membangun ulang tunnel secara paksa.
     *
     * Sekalian lapor ke server SEBELUM stopSelf() - ini sinyal PALING andal untuk "izin VPN
     * dicabut" (beda dari polling di ChildScreen.kt yang cuma jalan kalau proses aplikasi masih
     * hidup) karena callback sistem ini terpanggil persis saat pencabutan terjadi, tanpa perlu
     * app TimeCraft sedang dibuka. Best-effort (fire-and-forget) - kalau gagal terkirim (mis.
     * tidak ada jaringan), tetap lanjut stopSelf(), laporan berikutnya dari ChildScreen.kt yang
     * akan menyusulkan status ini kalau app dibuka lagi.
     */
    override fun onRevoke() {
        val tokenStore = SecureTokenStore(applicationContext)
        val hasOverlay = DeviceLockPermissions.hasOverlayPermission(applicationContext)
        scope.launch {
            val token = tokenStore.loadToken()
            if (token != null) {
                runCatching { PactioApi.reportPermissionStatus(token, hasOverlay, false) }
            }
        }
        stopSelf()
        super.onRevoke()
    }

    private fun establishTunnel(): ParcelFileDescriptor? {
        return Builder()
            .setSession("TimeCraft") // sama dengan android:label di AndroidManifest.xml - tidak ada R.string.app_name terpisah di project ini
            .addAddress(TUNNEL_ADDRESS, 32)
            .addDnsServer(TUNNEL_ADDRESS)
            .addRoute(TUNNEL_ADDRESS, 32)
            // WAJIB true - default Builder adalah NON-blocking, yang membuat input.read() di
            // tunnelLoop() langsung melempar IOException ("would block") begitu belum ada paket
            // masuk (bukan menunggu/blocking seperti asumsi kode di bawah), memicu catch -> break
            // -> tunnelLoop berhenti total dalam hitungan milidetik SEJAK SEBELUM paket pertama
            // pun sempat lewat - tapi rute DNS ke tunnel ini SUDAH terlanjur aktif di sistem, jadi
            // hasilnya semua resolusi DNS di perangkat gagal total (bukan cuma domain yang mau

            // diblokir) selama VPN ini aktif. Ini BUG NYATA yang sempat lolos ke produksi -
            // lihat juga pengaman stopSelf() di onCreate() kalau tunnelLoop tetap berhenti karena
            // sebab lain di masa depan.
            .setBlocking(true)
            .establish()
    }

    /** Sinkron berkala daftar blokir dari server - lihat catatan fail-open di kelas ini soal kenapa defaultnya kosong (tidak memblokir apa pun) sampai sinkron pertama berhasil. */
    private suspend fun syncLoop() {
        val tokenStore = SecureTokenStore(applicationContext)
        while (true) {
            val token = tokenStore.loadToken()
            if (token != null) {
                runCatching { PactioApi.getBlockedDomains(token) }
                    .onSuccess { result -> blockedSuffixes = result.effectiveDomains.map { it.lowercase() }.toSet() }
            }
            delay(SYNC_INTERVAL_MS)
        }
    }

    private fun isBlocked(host: String): Boolean {
        val suffixes = blockedSuffixes
        if (suffixes.isEmpty()) return false
        val lower = host.lowercase().trimEnd('.')
        return suffixes.any { blocked -> lower == blocked || lower.endsWith(".$blocked") }
    }

    private suspend fun tunnelLoop() {
        val tunnel = vpnInterface ?: return
        val input = FileInputStream(tunnel.fileDescriptor)
        val output = FileOutputStream(tunnel.fileDescriptor)
        val buffer = ByteArray(MAX_PACKET_SIZE)

        while (true) {
            val length = try {
                input.read(buffer)
            } catch (error: Exception) {
                Log.w(TAG, "Tunnel berhenti membaca (interface ditutup?), keluar dari loop.", error)
                break
            }
            if (length <= 0) continue
            runCatching { handlePacket(buffer, length, output) }
                .onFailure { error -> Log.w(TAG, "Gagal memproses satu paket, dilewati (fail-open).", error) }
        }
    }

    /** Cuma menangani paket IPv4/UDP tujuan port 53 (DNS) - selain itu sengaja diabaikan begitu saja (lihat catatan kelas soal kenapa cuma DNS yang lewat rute VPN ini). */
    private fun handlePacket(buffer: ByteArray, length: Int, output: FileOutputStream) {
        if (length < IPV4_HEADER_MIN_LENGTH) return
        val versionAndIhl = buffer[0].toInt() and 0xFF
        val version = versionAndIhl shr 4
        if (version != 4) return
        val ihl = (versionAndIhl and 0x0F) * 4
        if (ihl < IPV4_HEADER_MIN_LENGTH || length < ihl + UDP_HEADER_LENGTH) return
        val protocol = buffer[9].toInt() and 0xFF
        if (protocol != UDP_PROTOCOL) return

        val udpOffset = ihl
        val dstPort = readUInt16(buffer, udpOffset + 2)
        if (dstPort != DNS_PORT) return

        val udpLength = readUInt16(buffer, udpOffset + 4)
        val dnsOffset = udpOffset + UDP_HEADER_LENGTH
        val dnsLength = udpLength - UDP_HEADER_LENGTH
        if (dnsLength <= DNS_HEADER_LENGTH || dnsOffset + dnsLength > length) return

        val srcPort = readUInt16(buffer, udpOffset)
        val sourceIp = buffer.copyOfRange(12, 16)
        val questionHost = parseDnsQuestionHost(buffer, dnsOffset, dnsLength)

        if (questionHost != null && isBlocked(questionHost)) {
            val response = buildNxDomainResponse(buffer, dnsOffset, dnsLength)
            writeDnsResponsePacket(output, sourceIp, srcPort, response)
        } else {
            val upstreamResponse = forwardToUpstreamDns(buffer, dnsOffset, dnsLength) ?: return
            writeDnsResponsePacket(output, sourceIp, srcPort, upstreamResponse)
        }
    }

    /** Nama domain yang ditanyakan (QNAME) - null kalau gagal di-parse (mis. dipakai kompresi DNS di question, tidak lazim tapi mungkin) - null berarti TIDAK diblokir (fail-open), diteruskan apa adanya ke forwardToUpstreamDns. */
    private fun parseDnsQuestionHost(buffer: ByteArray, dnsOffset: Int, dnsLength: Int): String? {
        var pos = dnsOffset + DNS_HEADER_LENGTH
        val end = dnsOffset + dnsLength
        val labels = StringBuilder()
        var guard = 0
        while (pos < end && guard < MAX_DNS_LABELS) {
            val labelLength = buffer[pos].toInt() and 0xFF
            if (labelLength == 0) break
            if (labelLength and 0xC0 == 0xC0) return null // kompresi DNS - tidak didukung, aman diabaikan
            pos += 1
            if (pos + labelLength > end) return null
            if (labels.isNotEmpty()) labels.append('.')
            labels.append(String(buffer, pos, labelLength, Charsets.US_ASCII))
            pos += labelLength
            guard += 1
        }
        return labels.toString().ifEmpty { null }
    }

    /**
     * Jawaban "domain tidak ada" (NXDOMAIN) - dibuat dari SALINAN byte pertanyaan asli apa
     * adanya (header + question section, TIDAK diubah sama sekali) supaya tidak perlu
     * menyusun ulang question section dari nol (sumber bug paling mungkin) - cuma 2 byte flags
     * yang diubah supaya paket ini terbaca sebagai RESPONS, bukan pertanyaan lagi.
     */
    private fun buildNxDomainResponse(buffer: ByteArray, dnsOffset: Int, dnsLength: Int): ByteArray {
        val response = buffer.copyOfRange(dnsOffset, dnsOffset + dnsLength)
        response[2] = 0x81.toByte() // QR=1 (respons), Opcode=0, AA=0, TC=0, RD=1
        response[3] = 0x83.toByte() // RA=1, Z=0, RCODE=3 (NXDOMAIN)
        return response
    }

    /**
     * Meneruskan APA ADANYA (raw bytes) ke resolver DNS publik sungguhan - tidak menyusun ulang
     * paket sama sekali di sini, cuma relay murni. protect() WAJIB dipanggil supaya socket ini
     * SENDIRI tidak ikut tertangkap balik oleh tunnel VPN kita sendiri (mencegah infinite loop).
     */
    private fun forwardToUpstreamDns(buffer: ByteArray, dnsOffset: Int, dnsLength: Int): ByteArray? {
        return try {
            DatagramSocket().use { socket ->
                protect(socket)
                socket.soTimeout = UPSTREAM_TIMEOUT_MS
                val query = buffer.copyOfRange(dnsOffset, dnsOffset + dnsLength)
                socket.send(DatagramPacket(query, query.size, InetAddress.getByName(UPSTREAM_DNS), DNS_PORT))
                val responseBuffer = ByteArray(MAX_PACKET_SIZE)
                val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
                socket.receive(responsePacket)
                responseBuffer.copyOfRange(0, responsePacket.length)
            }
        } catch (error: Exception) {
            null
        }
    }

    /** Membungkus payload DNS (respons blokir atau hasil relay upstream) jadi satu paket IPv4/UDP utuh, ditulis balik ke tunnel seolah datang dari TUNNEL_ADDRESS:53. */
    private fun writeDnsResponsePacket(output: FileOutputStream, destIp: ByteArray, destPort: Int, dnsPayload: ByteArray) {
        val udpLength = UDP_HEADER_LENGTH + dnsPayload.size
        val totalLength = IPV4_HEADER_MIN_LENGTH + udpLength
        val packet = ByteArray(totalLength)

        val zero: Byte = 0
        packet[0] = 0x45.toByte() // versi 4, IHL 5 (20 byte, tanpa opsi)
        packet[1] = zero
        writeUInt16(packet, 2, totalLength)
        packet[4] = zero; packet[5] = zero // identification - tidak signifikan di sini (paket kecil, tidak pernah terfragmentasi)
        packet[6] = 0x40.toByte(); packet[7] = zero // flags: don't fragment
        packet[8] = 64.toByte() // TTL
        packet[9] = UDP_PROTOCOL.toByte()
        packet[10] = zero; packet[11] = zero // header checksum - dihitung & ditimpa di bawah
        TUNNEL_ADDRESS_BYTES.copyInto(packet, 12)
        destIp.copyInto(packet, 16)
        writeUInt16(packet, 10, ipv4HeaderChecksum(packet))

        val udpOffset = IPV4_HEADER_MIN_LENGTH
        writeUInt16(packet, udpOffset, DNS_PORT) // source port 53
        writeUInt16(packet, udpOffset + 2, destPort)
        writeUInt16(packet, udpOffset + 4, udpLength)
        packet[udpOffset + 6] = zero; packet[udpOffset + 7] = zero // UDP checksum - 0 = tidak dipakai, VALID untuk IPv4 (RFC 768)
        dnsPayload.copyInto(packet, udpOffset + UDP_HEADER_LENGTH)

        output.write(packet)
    }

    private fun readUInt16(buffer: ByteArray, offset: Int): Int =
        ((buffer[offset].toInt() and 0xFF) shl 8) or (buffer[offset + 1].toInt() and 0xFF)

    private fun writeUInt16(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = ((value shr 8) and 0xFF).toByte()
        buffer[offset + 1] = (value and 0xFF).toByte()
    }

    /** Checksum standar header IPv4 (RFC 791) - jumlah one's complement tiap word 16-bit di header (field checksum sendiri dianggap 0 saat dihitung), lalu di-komplemen sekali lagi. */
    private fun ipv4HeaderChecksum(packet: ByteArray): Int {
        var sum = 0
        var i = 0
        while (i < IPV4_HEADER_MIN_LENGTH) {
            sum += readUInt16(packet, i)
            i += 2
        }
        while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum shr 16)
        return sum.inv() and 0xFFFF
    }

    private fun buildNotification(): Notification {
        val channel = NotificationChannel(CHANNEL_ID, getString(R.string.channel_domain_block), NotificationManager.IMPORTANCE_LOW)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title_domain_block_active))
            .setContentText(getString(R.string.notif_text_domain_block))
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "DomainBlockVpnService"
        private const val NOTIFICATION_ID = 4202
        private const val CHANNEL_ID = "domain_block"
        private const val TUNNEL_ADDRESS = "10.0.0.2"
        private val TUNNEL_ADDRESS_BYTES = byteArrayOf(10, 0, 0, 2)
        // Preset kategori (lihat preset-blocklists.json) bisa berisi puluhan ribu domain (~1,6MB
        // sebagai JSON) - sinkron tiap beberapa jam, BUKAN tiap menit, supaya tidak boros kuota
        // data anak (1,6MB/menit = >2GB/hari kalau terlalu sering). Perubahan blokir dari orang
        // tua tidak butuh langsung berlaku detik itu juga, beda dengan status Mode Kunci.
        private const val SYNC_INTERVAL_MS = 4 * 60 * 60 * 1000L
        private const val UPSTREAM_DNS = "1.1.1.1"
        private const val UPSTREAM_TIMEOUT_MS = 5_000
        private const val MAX_PACKET_SIZE = 32767
        private const val DNS_PORT = 53
        private const val UDP_PROTOCOL = 17
        private const val IPV4_HEADER_MIN_LENGTH = 20
        private const val UDP_HEADER_LENGTH = 8
        private const val DNS_HEADER_LENGTH = 12
        private const val MAX_DNS_LABELS = 128
    }
}
