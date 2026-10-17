package id.my.timecraft.lembar.ui.ink

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import id.my.timecraft.lembar.data.INK_HEIGHT
import id.my.timecraft.lembar.data.INK_WIDTH
import id.my.timecraft.lembar.data.InkColor
import id.my.timecraft.lembar.data.InkTool
import id.my.timecraft.lembar.data.Paper

/** Tebal pena dalam unit logis: 3 (tekanan nol) sampai 13 (tekanan penuh). */
private fun penWidth(pressure: Float) = 3f + pressure.coerceIn(0f, 1f) * 10f

private const val HIGHLIGHTER_WIDTH = 34f
private const val HIGHLIGHTER_ALPHA = 0.4f

/**
 * Menggambar satu goresan. [points] berisi triplet (x, y, tekanan) dalam koordinat logis;
 * [scale] = lebar kanvas dalam piksel / [INK_WIDTH].
 */
fun DrawScope.drawInk(
    tool: InkTool,
    color: InkColor,
    points: FloatArray,
    count: Int,
    scale: Float,
    resolve: (InkColor) -> Color
) {
    if (count == 0) return
    val c = resolve(color)
    fun at(i: Int) = Offset(points[i * 3] * scale, points[i * 3 + 1] * scale)
    when {
        tool == InkTool.Stabilo -> {
            val path = Path().apply {
                moveTo(at(0).x, at(0).y)
                for (i in 1 until count) lineTo(at(i).x, at(i).y)
                if (count == 1) lineTo(at(0).x + 0.1f, at(0).y)
            }
            drawPath(
                path, c, alpha = HIGHLIGHTER_ALPHA,
                style = Stroke(width = HIGHLIGHTER_WIDTH * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
        count == 1 -> drawCircle(c, radius = penWidth(points[2]) * scale / 2f, center = at(0))
        else -> for (i in 1 until count) {
            // Setiap ruas punya tebal sendiri mengikuti tekanan pensil di kedua ujungnya.
            val w = penWidth((points[(i - 1) * 3 + 2] + points[i * 3 + 2]) / 2f) * scale
            drawLine(c, at(i - 1), at(i), strokeWidth = w, cap = StrokeCap.Round)
        }
    }
}

/** Latar kertas: garis horizontal, titik-titik, atau polos. */
fun DrawScope.drawPaper(paper: Paper, scale: Float, lineColor: Color, dotColor: Color) {
    when (paper) {
        Paper.Garis -> {
            val gap = 84f * scale
            var y = gap
            while (y < size.height) {
                drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5f * scale.coerceAtLeast(0.7f))
                y += gap
            }
        }
        Paper.Titik -> {
            val gap = 60f * scale
            val r = 3.2f * scale.coerceAtLeast(0.6f)
            var y = gap
            while (y < size.height) {
                var x = gap
                while (x < size.width) {
                    drawCircle(dotColor, r, Offset(x, y))
                    x += gap
                }
                y += gap
            }
        }
        Paper.Polos -> Unit
    }
}

/** Skala agar kanvas logis selebar [INK_WIDTH] pas dengan lebar piksel yang tersedia. */
fun DrawScope.inkScale() = size.width / INK_WIDTH

/** Tinggi logis yang terlihat (kanvas editor selalu 3:4 = [INK_HEIGHT]). */
val INK_ASPECT = INK_WIDTH / INK_HEIGHT
