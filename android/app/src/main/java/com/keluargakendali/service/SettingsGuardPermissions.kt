package com.keluargakendali.service

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils

/**
 * Status Accessibility Service TimeCraft (lihat SettingsGuardAccessibilityService) - BEDA dari
 * overlay/VPN di atas, tidak ada API `start()`/`stop()` untuk accessibility service: sistem
 * Android sendiri yang menjalankannya begitu pengguna mengaktifkannya lewat Pengaturan >
 * Aksesibilitas, jadi di sini cuma perlu cek status & sediakan Intent untuk membuka layar itu.
 */
object SettingsGuardPermissions {

    /**
     * Membaca daftar accessibility service yang sedang AKTIF dari Settings.Secure - satu-satunya
     * cara resmi mengecek status ini tanpa accessibility service itu sendiri harus "melapor diri".
     */
    fun isEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val expectedComponent = "${context.packageName}/${SettingsGuardAccessibilityService::class.java.name}"
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        for (component in splitter) {
            if (component.equals(expectedComponent, ignoreCase = true)) return true
        }
        return false
    }

    /** Tidak ada Intent aktivasi langsung resmi (beda dari overlay) - cuma bisa buka daftar Aksesibilitas, pengguna sendiri yang mencari & mengaktifkan TimeCraft di dalamnya. */
    fun accessibilitySettingsIntent(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
}
