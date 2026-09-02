package com.keluargakendali.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.keluargakendali.R

/**
 * Lapisan tambahan (opsional) untuk mempersulit anak mencabut izin Mode Kunci/Blokir Domain
 * lewat Pengaturan sistem - lihat diskusi desain: Android TIDAK punya API untuk aplikasi biasa
 * benar-benar MEMBLOKIR akses ke Pengaturan sistem (beda dari klaim awal kami sebelum riset -
 * app "parental control" populer seperti Qustodio TIDAK memakai Device Owner/factory reset,
 * tapi Accessibility Service seperti ini).
 *
 * CARA KERJA (best-effort, BUKAN pemblokiran level-OS): begitu Accessibility Service ini
 * diaktifkan manual oleh pengguna (lewat Pengaturan > Aksesibilitas, sama seperti overlay/VPN -
 * WAJIB dialog sistem, TIDAK pernah diam-diam), service ini memantau tiap kali layar berganti
 * (typeWindowStateChanged) di aplikasi mana pun. Kalau layar yang baru dibuka kelihatannya
 * adalah bagian dari aplikasi Pengaturan sistem DAN isi teksnya menyebut "TimeCraft" atau "VPN"
 * (indikasi anak sedang membuka Info Aplikasi TimeCraft, entri TimeCraft di daftar
 * Aksesibilitas, atau layar Pengaturan VPN), service ini otomatis menekan tombol "kembali" dan
 * menampilkan Toast singkat penjelasan - MENGUSIR anak dari layar itu, bukan mencegahnya
 * membuka Pengaturan sama sekali.
 *
 * BATASAN YANG JUJUR (harus dipahami operator aplikasi):
 * - Ini heuristik teks, bukan deteksi presisi per elemen UI - bisa gagal kenali di ROM/versi
 *   Android tertentu (nama layar/susunan UI beda-beda per pabrikan), dan BISA salah memicu
 *   (false positive) kalau kebetulan kata "TimeCraft"/"VPN" muncul di konteks tidak terkait di
 *   dalam aplikasi Pengaturan - risiko diterima karena akibatnya cuma tombol "kembali" tertekan
 *   sekali, bukan sesuatu yang merusak.
 * - Anak yang cukup teknis tetap bisa mengakalinya lewat Safe Mode (mode aman Android
 *   MENONAKTIFKAN SEMUA accessibility service pihak ketiga sampai HP dinyalakan ulang normal) -
 *   ini keterbatasan Android sendiri, BUKAN bug di kode ini, dan tidak ada cara resmi untuk
 *   mencegahnya tanpa Device Owner penuh (yang berarti factory reset, sudah dibahas & belum
 *   disepakati untuk dipakai).
 * - BELUM PERNAH diuji di perangkat Android sungguhan (sandbox pengembangan ini tidak punya
 *   emulator/device) - WAJIB dites langsung di HP anak sebelum diandalkan: coba buka Info
 *   Aplikasi TimeCraft & entri TimeCraft di Pengaturan > Aksesibilitas, pastikan otomatis
 *   terlempar keluar; pastikan TIDAK mengganggu navigasi aplikasi lain sama sekali.
 *
 * Status aktif/tidaknya dipoll & dilaporkan ke server sama persis polanya dengan izin
 * overlay/VPN (lihat ChildScreen.kt & POST /children/permission-status) - kalau anak berhasil
 * menonaktifkan (mis. lewat Safe Mode), orang tua tetap melihatnya lewat status "Dicabut anak",
 * konsisten dengan filosofi "visibilitas, bukan jaminan pencegahan" di seluruh fitur kunci ini.
 */
class SettingsGuardAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        // Cuma perlu peduli kalau layar yang baru muncul kemungkinan bagian dari aplikasi
        // Pengaturan sistem - nama paket persisnya beda-beda per pabrikan (com.android.settings,
        // com.miui.securitycenter, com.huawei.systemmanager, dst), jadi dicek longgar lewat kata
        // "settings"/"security" alih-alih daftar tetap yang pasti tidak lengkap.
        val looksLikeSettingsApp = packageName.contains("settings", ignoreCase = true) ||
            packageName.contains("security", ignoreCase = true) ||
            packageName.contains("systemmanager", ignoreCase = true)
        if (!looksLikeSettingsApp) return

        val root = rootInActiveWindow ?: return
        val screenText = StringBuilder()
        collectText(root, screenText, depth = 0, visited = 0)
        val text = screenText.toString()
        val mentionsApp = text.contains(APP_LABEL, ignoreCase = true)
        val mentionsVpn = text.contains("VPN", ignoreCase = true)
        if (mentionsApp || mentionsVpn) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            Toast.makeText(applicationContext, getString(R.string.toast_settings_guard_blocked), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onInterrupt() {
        // Tidak ada state berkelanjutan yang perlu dibersihkan - setiap event diproses berdiri
        // sendiri (lihat onAccessibilityEvent).
    }

    /**
     * Kumpulkan semua teks/deskripsi node di pohon UI layar aktif, dibatasi kedalaman & jumlah
     * simpul (defensif terhadap pohon yang sangat dalam/lebar di beberapa ROM) supaya tidak
     * memicu ANR pada thread accessibility yang berbagi main thread dengan sistem.
     */
    private fun collectText(node: AccessibilityNodeInfo?, out: StringBuilder, depth: Int, visited: Int): Int {
        if (node == null || depth > MAX_DEPTH || visited > MAX_NODES) return visited
        var count = visited + 1
        node.text?.let { out.append(it).append(' ') }
        node.contentDescription?.let { out.append(it).append(' ') }
        for (i in 0 until node.childCount) {
            if (count > MAX_NODES) break
            count = collectText(node.getChild(i), out, depth + 1, count)
        }
        return count
    }

    companion object {
        /** Sama persis dengan android:label di AndroidManifest.xml - dicari apa adanya di teks layar Pengaturan. */
        private const val APP_LABEL = "TimeCraft"
        private const val MAX_DEPTH = 12
        private const val MAX_NODES = 300
    }
}
