package id.my.timecraft.lembar.ui.ink

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import id.my.timecraft.lembar.data.INK_HEIGHT
import id.my.timecraft.lembar.data.INK_WIDTH
import id.my.timecraft.lembar.data.InkStroke

/**
 * Papan klip coretan, berlaku untuk seluruh aplikasi: coretan yang disalin bisa ditempel
 * di lembar lain atau di catatan lain.
 */
object InkClipboard {
    var strokes by mutableStateOf<List<InkStroke>>(emptyList())
}

/** Kotak pembatas (koordinat logis) semua titik di [strokes], ditambah sedikit ruang. */
fun boundsOf(strokes: Collection<InkStroke>, pad: Float = 14f): Rect? {
    if (strokes.isEmpty()) return null
    var l = Float.MAX_VALUE
    var t = Float.MAX_VALUE
    var r = -Float.MAX_VALUE
    var b = -Float.MAX_VALUE
    for (s in strokes) for (i in 0 until s.size) {
        l = minOf(l, s.x(i)); r = maxOf(r, s.x(i))
        t = minOf(t, s.y(i)); b = maxOf(b, s.y(i))
    }
    return Rect(l - pad, t - pad, r + pad, b + pad)
}

/** Coretan dianggap terpilih kalau minimal separuh titiknya ada di dalam kotak [area]. */
fun selectIn(strokes: List<InkStroke>, area: Rect): Set<InkStroke> {
    val box = Rect(
        minOf(area.left, area.right), minOf(area.top, area.bottom),
        maxOf(area.left, area.right), maxOf(area.top, area.bottom)
    )
    return strokes.filter { s ->
        var inside = 0
        for (i in 0 until s.size) if (box.contains(Offset(s.x(i), s.y(i)))) inside++
        s.size > 0 && inside * 2 >= s.size
    }.toSet()
}

/**
 * Batasi geseran agar kotak [bounds] tetap berada di dalam lembar, supaya coretan
 * tidak hilang keluar halaman.
 */
fun clampMove(bounds: Rect, dx: Float, dy: Float): Offset {
    val minDx = -bounds.left
    val maxDx = INK_WIDTH - bounds.right
    val minDy = -bounds.top
    val maxDy = INK_HEIGHT - bounds.bottom
    return Offset(
        if (minDx > maxDx) 0f else dx.coerceIn(minDx, maxDx),
        if (minDy > maxDy) 0f else dy.coerceIn(minDy, maxDy)
    )
}
