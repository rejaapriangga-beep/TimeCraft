package id.my.timecraft.lembar.ui.ink

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import id.my.timecraft.lembar.data.InkStroke
import id.my.timecraft.lembar.ui.theme.Lembar

/** Pratinjau kecil tulisan tangan di kartu daftar: bagian atas halaman, dipotong setinggi [modifier]. */
@Composable
fun InkPreview(strokes: List<InkStroke>, modifier: Modifier = Modifier) {
    val colors = Lembar.colors
    Canvas(modifier.clipToBounds()) {
        // Tampilkan ~700 unit logis teratas (setengah halaman) supaya coretan cukup besar dibaca.
        val scale = maxOf(inkScale(), size.height / 700f)
        strokes.forEach { s -> drawInk(s.tool, s.color, s.points, s.size, scale, colors::inkOf) }
    }
}
