@file:OptIn(ExperimentalComposeUiApi::class)

package id.my.timecraft.lembar.ui.ink

import android.os.Build
import android.view.MotionEvent
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
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.input.pointer.pointerInput
import id.my.timecraft.lembar.data.INK_WIDTH
import id.my.timecraft.lembar.data.InkColor
import id.my.timecraft.lembar.data.InkStroke
import id.my.timecraft.lembar.data.InkTool
import id.my.timecraft.lembar.data.Paper
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.hypot

enum class InkMode { Pena, Stabilo, Hapus, Pilih }

/** Jarak (unit logis) dari titik penghapus ke titik goresan yang dianggap "kena". */
private const val ERASE_RADIUS = 28f

/**
 * Kanvas tulis tangan untuk satu lembar.
 *
 * - Tebal goresan mengikuti tekanan pensil ([PointerInputChange.pressure]); jari dan mouse
 *   memakai tekanan sedang.
 * - Penolakan telapak (palm rejection), berlapis:
 *   1. Kalau [penOnly] aktif, sentuhan jari/telapak diabaikan sepenuhnya.
 *   2. Kalau tidak, pensil tetap menang: begitu pensil menyentuh layar, goresan sentuhan yang
 *      sedang berjalan dibuang dan pensil langsung menulis, walaupun telapak menempel lebih dulu.
 *   3. Sentuhan yang dibatalkan Android sendiri sebagai telapak juga dibuang.
 *   [onStylusDetected] dipanggil setiap ada event pensil supaya mode 1 bisa dinyalakan otomatis.
 * - Ujung penghapus pensil (PointerType.Eraser) selalu menghapus, apa pun alat yang dipilih.
 * - Penghapus bekerja per goresan utuh.
 * - Mode [InkMode.Pilih]: seret untuk membuat kotak pilihan; seret dari dalam kotak pilihan
 *   untuk memindahkan coretan terpilih. Pilihan dikelola pemanggil lewat [selected]/[onSelect].
 *
 * Setiap perubahan goresan dilaporkan lewat [onStrokesChange]; [onBeforeChange] dipanggil sekali
 * sebelum perubahan pertama supaya pemanggil bisa menyimpan riwayat urungkan, dan
 * [onDiscardLastUndo] membatalkan entri itu lagi kalau perubahannya ternyata dari telapak.
 */
@Composable
fun InkCanvas(
    strokes: List<InkStroke>,
    paper: Paper,
    mode: InkMode,
    color: InkColor,
    penOnly: Boolean,
    selected: Set<InkStroke>,
    onSelect: (Set<InkStroke>) -> Unit,
    onStylusDetected: () -> Unit,
    onBeforeChange: () -> Unit,
    onStrokesChange: (List<InkStroke>) -> Unit,
    onDiscardLastUndo: () -> Unit,
    resolve: (InkColor) -> Color,
    paperLine: Color,
    paperDot: Color,
    selectionColor: Color,
    modifier: Modifier = Modifier
) {
    val currentStrokes by rememberUpdatedState(strokes)
    val currentMode by rememberUpdatedState(mode)
    val currentColor by rememberUpdatedState(color)
    val currentPenOnly by rememberUpdatedState(penOnly)
    val currentSelected by rememberUpdatedState(selected)
    val select by rememberUpdatedState(onSelect)
    val stylusDetected by rememberUpdatedState(onStylusDetected)
    val beforeChange by rememberUpdatedState(onBeforeChange)
    val strokesChange by rememberUpdatedState(onStrokesChange)
    val undoDiscard by rememberUpdatedState(onDiscardLastUndo)

    // Goresan yang sedang ditulis disimpan di buffer biasa; [liveCount] memicu gambar ulang.
    val live = remember { LiveStroke() }
    var liveCount by remember { mutableIntStateOf(0) }
    // Mode Pilih: kotak yang sedang ditarik, dan geseran sementara coretan terpilih.
    var marquee by remember { mutableStateOf<Rect?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    // Diisi oleh motionEventSpy saat Android sendiri membatalkan sentuhan karena dikenali sebagai
    // telapak (ACTION_CANCEL, atau FLAG_CANCELED di Android 13+). Goresan sentuhan yang sedang
    // berjalan lalu dibuang, bukan disimpan.
    val palm = remember { PalmSignal() }

    Box(
        modifier
            .motionEventSpy { ev ->
                val action = ev.actionMasked
                val flaggedCanceled = Build.VERSION.SDK_INT >= 33 &&
                    (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) &&
                    (ev.flags and MotionEvent.FLAG_CANCELED) != 0
                if (action == MotionEvent.ACTION_CANCEL || flaggedCanceled) palm.canceled = true
            }
            .pointerInput(Unit) {
                // Satu "gesture" berlangsung selama masih ada jari/telapak/pensil yang menempel.
                // Telapak bisa bertumpu lama sementara pensil diangkat dan diturunkan berkali-kali,
                // jadi satu gesture bisa berisi banyak goresan.
                awaitEachGesture {
                    val first = awaitFirstDown(requireUnconsumed = false)
                    palm.canceled = false

                    val scale = size.width / INK_WIDTH
                    fun logical(o: Offset) = Offset(o.x / scale, o.y / scale)
                    fun isPen(c: PointerInputChange) = c.type == PointerType.Stylus || c.type == PointerType.Eraser
                    fun pressureOf(c: PointerInputChange) = if (isPen(c)) c.pressure else 0.5f

                    // Goresan aktif: hanya satu pointer yang bekerja pada satu waktu.
                    var active: PointerId? = null
                    var activeIsPen = false
                    var action = Action.Draw
                    var base = currentStrokes      // goresan sebelum aksi aktif dimulai
                    var working = currentStrokes   // hasil penghapus sementara (bisa banyak titik per frame)
                    var changed = false            // penghapus sudah mengubah sesuatu di aksi ini
                    var anchor = Offset.Zero       // titik awal kotak pilihan / pemindahan
                    var moving: Set<InkStroke> = emptySet()

                    fun begin(c: PointerInputChange) {
                        active = c.id
                        activeIsPen = isPen(c)
                        base = currentStrokes
                        working = base
                        changed = false
                        val p = logical(c.position)
                        action = when {
                            c.type == PointerType.Eraser || currentMode == InkMode.Hapus -> Action.Erase
                            currentMode == InkMode.Pilih -> {
                                val sel = currentSelected.filter { it in base }.toSet()
                                val box = boundsOf(sel, pad = 30f)
                                if (box != null && box.contains(p)) {
                                    moving = sel
                                    Action.Move
                                } else {
                                    select(emptySet())
                                    Action.Marquee
                                }
                            }
                            else -> Action.Draw
                        }
                        anchor = p
                        when (action) {
                            Action.Draw -> {
                                val tool = if (currentMode == InkMode.Stabilo) InkTool.Stabilo else InkTool.Pena
                                // Stabilo dengan tinta hitam tidak berguna, jadi otomatis jadi kuning.
                                val ink = if (tool == InkTool.Stabilo && currentColor == InkColor.Tinta) InkColor.Kuning else currentColor
                                live.start(tool, ink)
                            }
                            Action.Marquee -> marquee = Rect(p, p)
                            Action.Move -> dragOffset = Offset.Zero
                            Action.Erase -> Unit
                        }
                    }

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

                    fun extend(c: PointerInputChange) {
                        when (action) {
                            Action.Erase -> {
                                c.historical.forEach { eraseAt(it.position) }
                                eraseAt(c.position)
                            }
                            Action.Draw -> {
                                val pressure = pressureOf(c)
                                c.historical.forEach { live.add(logical(it.position), pressure) }
                                live.add(logical(c.position), pressure)
                                liveCount = live.count
                            }
                            Action.Marquee -> marquee = Rect(anchor, logical(c.position))
                            Action.Move -> {
                                val d = logical(c.position) - anchor
                                dragOffset = boundsOf(moving)?.let { clampMove(it, d.x, d.y) } ?: d
                            }
                        }
                    }

                    // Aksi dari telapak/jari: buang seolah tidak pernah terjadi.
                    fun reject() {
                        when (action) {
                            Action.Erase -> if (changed) {
                                strokesChange(base)
                                undoDiscard()
                            }
                            Action.Draw -> {
                                live.finish()
                                liveCount = 0
                            }
                            Action.Marquee -> marquee = null
                            Action.Move -> dragOffset = Offset.Zero
                        }
                        active = null
                    }

                    fun commit() {
                        when (action) {
                            Action.Erase -> Unit
                            Action.Draw -> {
                                val finished = live.finish()
                                liveCount = 0
                                if (finished != null) {
                                    beforeChange()
                                    strokesChange(base + finished)
                                }
                            }
                            Action.Marquee -> {
                                marquee?.let { select(selectIn(base, it)) }
                                marquee = null
                            }
                            Action.Move -> {
                                val d = dragOffset
                                dragOffset = Offset.Zero
                                if (d != Offset.Zero) {
                                    val moved = HashMap<InkStroke, InkStroke>()
                                    moving.forEach { moved[it] = it.translated(d.x, d.y) }
                                    beforeChange()
                                    strokesChange(base.map { moved[it] ?: it })
                                    select(moved.values.toSet())
                                }
                            }
                        }
                        active = null
                    }

                    fun process(changes: List<PointerInputChange>) {
                        for (c in changes) {
                            val pen = isPen(c)
                            if (pen) stylusDetected()
                            val newDown = c.pressed && !c.previousPressed
                            when {
                                newDown && pen -> {
                                    // Pensil selalu menang: aksi sentuhan yang sedang berjalan
                                    // (hampir pasti telapak) dibuang, lalu pensil mulai bekerja.
                                    if (active != null && !activeIsPen) reject()
                                    if (active == null) {
                                        begin(c)
                                        extend(c)
                                    }
                                }
                                newDown -> {
                                    if (active == null && !currentPenOnly) {
                                        begin(c)
                                        extend(c)
                                    }
                                }
                                c.id == active -> {
                                    extend(c)
                                    if (!c.pressed) {
                                        if (!activeIsPen && palm.canceled) reject() else commit()
                                    }
                                }
                            }
                            c.consume()
                        }
                    }

                    process(listOf(first))
                    while (true) {
                        val event = awaitPointerEvent()
                        process(event.changes)
                        if (event.changes.none { it.pressed }) break
                    }
                    // Jaga-jaga kalau pointer aktif hilang tanpa event "lepas".
                    if (active != null) {
                        if (!activeIsPen && palm.canceled) reject() else commit()
                    }
                }
            }
    ) {
        // Lapisan 1: kertas + goresan tersimpan yang tidak terpilih.
        Canvas(Modifier.fillMaxSize()) {
            val scale = inkScale()
            drawPaper(paper, scale, paperLine, paperDot)
            strokes.forEach { s -> if (s !in selected) drawInk(s.tool, s.color, s.points, s.size, scale, resolve) }
        }
        // Lapisan 2: coretan terpilih (ikut bergeser saat dipindah) + kotak pilihannya.
        Canvas(Modifier.fillMaxSize()) {
            val scale = inkScale()
            val sel = strokes.filter { it in selected }
            val d = dragOffset
            translate(d.x * scale, d.y * scale) {
                sel.forEach { s -> drawInk(s.tool, s.color, s.points, s.size, scale, resolve) }
                boundsOf(sel)?.let { drawSelectionBox(it, scale, selectionColor) }
            }
            marquee?.let { drawSelectionBox(it, scale, selectionColor) }
        }
        // Lapisan 3: goresan yang sedang ditulis, digambar ulang setiap titik baru.
        Canvas(Modifier.fillMaxSize()) {
            val n = liveCount
            if (n > 0) drawInk(live.tool, live.color, live.buffer, n, inkScale(), resolve)
        }
    }
}

private enum class Action { Draw, Erase, Marquee, Move }

private fun DrawScope.drawSelectionBox(r: Rect, scale: Float, color: Color) {
    val left = minOf(r.left, r.right) * scale
    val top = minOf(r.top, r.bottom) * scale
    val size = Size(abs(r.width) * scale, abs(r.height) * scale)
    drawRect(color, Offset(left, top), size, alpha = 0.08f)
    drawRect(
        color, Offset(left, top), size,
        style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14.dp.toPx(), 8.dp.toPx())))
    )
}

private class PalmSignal { var canceled = false }

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
