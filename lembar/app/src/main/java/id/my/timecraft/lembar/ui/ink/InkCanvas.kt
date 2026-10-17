@file:OptIn(ExperimentalComposeUiApi::class)

package id.my.timecraft.lembar.ui.ink

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import id.my.timecraft.lembar.data.INK_WIDTH
import id.my.timecraft.lembar.data.InkColor
import id.my.timecraft.lembar.data.InkStroke
import id.my.timecraft.lembar.data.InkTool
import id.my.timecraft.lembar.data.Paper
import kotlin.math.hypot

enum class InkMode { Pena, Stabilo, Hapus }

/** Jarak (unit logis) dari titik penghapus ke titik goresan yang dianggap "kena". */
private const val ERASE_RADIUS = 28f

/**
 * Kanvas tulis tangan.
 *
 * - Tebal goresan mengikuti tekanan pensil ([PointerInputChange.pressure]); jari dan mouse
 *   memakai tekanan sedang.
 * - Kalau [penOnly] aktif, sentuhan jari/telapak diabaikan sehingga tangan bisa bertumpu di layar.
 *   Begitu pensil pertama kali terdeteksi, [onStylusDetected] dipanggil supaya mode ini bisa
 *   dinyalakan otomatis.
 * - Ujung penghapus pensil (PointerType.Eraser) selalu menghapus, apa pun alat yang dipilih.
 * - Penghapus bekerja per goresan utuh. Setiap perubahan dilaporkan lewat [onStrokesChange];
 *   [onBeforeChange] dipanggil sekali sebelum perubahan pertama supaya pemanggil bisa menyimpan
 *   riwayat untuk tombol urungkan.
 */
@Composable
fun InkCanvas(
    strokes: List<InkStroke>,
    paper: Paper,
    mode: InkMode,
    color: InkColor,
    penOnly: Boolean,
    onStylusDetected: () -> Unit,
    onBeforeChange: () -> Unit,
    onStrokesChange: (List<InkStroke>) -> Unit,
    resolve: (InkColor) -> Color,
    paperLine: Color,
    paperDot: Color,
    modifier: Modifier = Modifier
) {
    val currentStrokes by rememberUpdatedState(strokes)
    val currentMode by rememberUpdatedState(mode)
    val currentColor by rememberUpdatedState(color)
    val currentPenOnly by rememberUpdatedState(penOnly)
    val stylusDetected by rememberUpdatedState(onStylusDetected)
    val beforeChange by rememberUpdatedState(onBeforeChange)
    val strokesChange by rememberUpdatedState(onStrokesChange)

    // Goresan yang sedang ditulis disimpan di buffer biasa; [liveCount] memicu gambar ulang.
    val live = remember { LiveStroke() }
    var liveCount by remember { mutableIntStateOf(0) }

    Box(
        modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val isPen = down.type == PointerType.Stylus || down.type == PointerType.Eraser
                if (isPen) stylusDetected()
                if (currentPenOnly && !isPen) return@awaitEachGesture

                val scale = size.width / INK_WIDTH
                fun logical(o: Offset) = Offset(o.x / scale, o.y / scale)
                fun pressureOf(c: PointerInputChange) =
                    if (c.type == PointerType.Stylus || c.type == PointerType.Eraser) c.pressure else 0.5f

                val erasing = currentMode == InkMode.Hapus || down.type == PointerType.Eraser
                down.consume()

                if (erasing) {
                    // Daftar kerja lokal: state baru terbarui setelah recomposition, padahal satu
                    // frame bisa membawa banyak titik penghapus sekaligus.
                    var working = currentStrokes
                    var changed = false
                    fun eraseAt(o: Offset) {
                        val p = logical(o)
                        val remaining = working.filterNot { s -> s.hits(p.x, p.y) }
                        if (remaining.size != working.size) {
                            if (!changed) beforeChange()
                            changed = true
                            working = remaining
                            strokesChange(remaining)
                        }
                    }
                    eraseAt(down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        change.historical.forEach { eraseAt(it.position) }
                        eraseAt(change.position)
                        change.consume()
                        if (!change.pressed) break
                    }
                    return@awaitEachGesture
                }

                val tool = if (currentMode == InkMode.Stabilo) InkTool.Stabilo else InkTool.Pena
                // Stabilo dengan tinta hitam tidak berguna, jadi otomatis jadi kuning.
                val inkColor = if (tool == InkTool.Stabilo && currentColor == InkColor.Tinta) InkColor.Kuning else currentColor
                live.start(tool, inkColor)
                live.add(logical(down.position), pressureOf(down))
                liveCount = live.count

                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    val pressure = pressureOf(change)
                    change.historical.forEach { live.add(logical(it.position), pressure) }
                    live.add(logical(change.position), pressure)
                    liveCount = live.count
                    change.consume()
                    if (!change.pressed) break
                }

                val finished = live.finish()
                liveCount = 0
                if (finished != null) {
                    beforeChange()
                    strokesChange(currentStrokes + finished)
                }
            }
        }
    ) {
        // Lapisan 1: kertas + goresan tersimpan. Hanya digambar ulang kalau daftar goresan berubah.
        Canvas(Modifier.fillMaxSize()) {
            val scale = inkScale()
            drawPaper(paper, scale, paperLine, paperDot)
            strokes.forEach { s -> drawInk(s.tool, s.color, s.points, s.size, scale, resolve) }
        }
        // Lapisan 2: goresan yang sedang ditulis, digambar ulang setiap titik baru.
        Canvas(Modifier.fillMaxSize()) {
            val n = liveCount
            if (n > 0) drawInk(live.tool, live.color, live.buffer, n, inkScale(), resolve)
        }
    }
}

private fun InkStroke.hits(x: Float, y: Float): Boolean {
    for (i in 0 until size) {
        if (hypot(x(i) - x, y(i) - y) < ERASE_RADIUS) return true
    }
    return false
}

/** Buffer titik yang tumbuh untuk goresan aktif, supaya tidak membuat array baru per titik. */
private class LiveStroke {
    var tool = InkTool.Pena
    var color = InkColor.Tinta
    var buffer = FloatArray(3 * 256)
    var count = 0

    fun start(tool: InkTool, color: InkColor) {
        this.tool = tool
        this.color = color
        count = 0
    }

    fun add(p: Offset, pressure: Float) {
        if (count > 0) {
            val lx = buffer[(count - 1) * 3]
            val ly = buffer[(count - 1) * 3 + 1]
            if (hypot(p.x - lx, p.y - ly) < 2f) return
        }
        if ((count + 1) * 3 > buffer.size) buffer = buffer.copyOf(buffer.size * 2)
        buffer[count * 3] = p.x
        buffer[count * 3 + 1] = p.y
        buffer[count * 3 + 2] = pressure
        count++
    }

    fun finish(): InkStroke? {
        if (count == 0) return null
        val s = InkStroke(tool, color, buffer.copyOf(count * 3))
        count = 0
        return s
    }
}
