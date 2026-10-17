package id.my.timecraft.lembar.data

import android.content.Context

/**
 * Pengaturan pensil yang diingat lintas catatan dan setelah aplikasi dibuka ulang.
 * - [penOnly]: "Hanya pensil" (abaikan sentuhan jari/telapak di kanvas).
 * - [stylusSeen]: pensil pernah terdeteksi di perangkat ini. Setelah true, [penOnly] tidak
 *   dinyalakan otomatis lagi, jadi pilihan pengguna untuk mematikannya tetap dihormati.
 */
class PenPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("lembar_pen", Context.MODE_PRIVATE)

    var penOnly: Boolean
        get() = prefs.getBoolean("pen_only", false)
        set(v) = prefs.edit().putBoolean("pen_only", v).apply()

    var stylusSeen: Boolean
        get() = prefs.getBoolean("stylus_seen", false)
        set(v) = prefs.edit().putBoolean("stylus_seen", v).apply()
}
