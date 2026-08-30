package com.keluargakendali.service

import android.content.Context
import android.content.Intent
import android.net.VpnService

/**
 * Izin VPN lokal resmi Android untuk fitur blokir domain (lihat DomainBlockVpnService) - dialog
 * sistem "Izinkan TimeCraft menyiapkan koneksi VPN" yang WAJIB disetujui pengguna sendiri lewat
 * API resmi VpnService.prepare(), bukan diaktifkan diam-diam. Sekali diberikan, tetap berlaku
 * sampai dicabut manual lewat Pengaturan sistem atau diambil alih aplikasi VPN lain (device cuma
 * bisa punya 1 VPN aktif dalam satu waktu - lihat DomainBlockVpnService.onRevoke).
 */
object DomainBlockPermissions {
    /** null kalau izin sudah ada (siap start service langsung) - kalau tidak null, luncurkan Intent ini dulu lewat ActivityResultLauncher sebelum start service. */
    fun prepareIntent(context: Context): Intent? = VpnService.prepare(context)
}
