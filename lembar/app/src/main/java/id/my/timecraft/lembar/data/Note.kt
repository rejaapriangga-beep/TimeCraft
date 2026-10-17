package id.my.timecraft.lembar.data

import java.util.UUID

/** Mode terakhir yang dibuka untuk sebuah catatan. Satu catatan boleh berisi teks dan tulisan tangan. */
enum class NoteMode(val key: String) { Ketik("ketik"), Tulis("tulis") }

enum class CardColor(val key: String, val label: String) {
    Krem("krem", "Krem"), Kuning("kuning", "Kuning"), Hijau("hijau", "Hijau"), Biru("biru", "Biru"),
    Peach("peach", "Peach"), Ungu("ungu", "Ungu"), Merah("merah", "Merah muda")
}

enum class Paper(val key: String, val label: String) { Garis("garis", "Garis"), Titik("titik", "Titik"), Polos("polos", "Polos") }

enum class InkTool(val key: String) { Pena("pena"), Stabilo("stabilo") }

enum class InkColor(val key: String) { Tinta("tinta"), Hijau("hijau"), Biru("biru"), Peach("peach"), Kuning("kuning") }

val SPACES = listOf("Kerja", "Belajar", "Pribadi")

data class CheckItem(val text: String, val done: Boolean = false)

/**
 * Satu goresan tulisan tangan. [points] berisi triplet (x, y, tekanan) berurutan dalam ruang
 * koordinat logis [INK_WIDTH] x [INK_HEIGHT] (rasio 3:4), jadi goresan tetap tajam di layar
 * berukuran berapa pun.
 */
class InkStroke(val tool: InkTool, val color: InkColor, val points: FloatArray) {
    val size: Int get() = points.size / 3
    fun x(i: Int) = points[i * 3]
    fun y(i: Int) = points[i * 3 + 1]
    fun p(i: Int) = points[i * 3 + 2]

    /** Salinan yang digeser sejauh (dx, dy) unit logis. */
    fun translated(dx: Float, dy: Float): InkStroke {
        val out = points.copyOf()
        for (i in 0 until size) {
            out[i * 3] += dx
            out[i * 3 + 1] += dy
        }
        return InkStroke(tool, color, out)
    }

    fun recolored(c: InkColor) = InkStroke(tool, c, points)
}

/** Satu lembar tulisan tangan = daftar goresan. */
typealias InkPage = List<InkStroke>

const val INK_WIDTH = 1000f
const val INK_HEIGHT = 1333f

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val body: String = "",
    val space: String = "Pribadi",
    val color: CardColor = CardColor.Krem,
    val pinned: Boolean = false,
    val mode: NoteMode = NoteMode.Ketik,
    val paper: Paper = Paper.Garis,
    val checks: List<CheckItem> = emptyList(),
    /** Lembar-lembar tulisan tangan. Selalu minimal satu lembar. */
    val pages: List<InkPage> = listOf(emptyList()),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isEmpty: Boolean
        get() = title.isBlank() && body.isBlank() && checks.none { it.text.isNotBlank() } && !hasInk

    val hasInk: Boolean get() = pages.any { it.isNotEmpty() }

    fun matches(query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()
        return title.lowercase().contains(q) || body.lowercase().contains(q) ||
            checks.any { it.text.lowercase().contains(q) }
    }
}
