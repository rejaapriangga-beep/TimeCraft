package id.my.timecraft.lembar.data

import java.util.UUID

/** Mode terakhir yang dibuka untuk sebuah catatan. Satu catatan boleh berisi teks dan tulisan tangan. */
enum class NoteMode(val key: String) { Ketik("ketik"), Tulis("tulis") }

enum class CardColor(val key: String) { Krem("krem"), Kuning("kuning"), Hijau("hijau"), Biru("biru") }

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
}

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
    val ink: List<InkStroke> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isEmpty: Boolean
        get() = title.isBlank() && body.isBlank() && checks.none { it.text.isNotBlank() } && ink.isEmpty()

    fun matches(query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()
        return title.lowercase().contains(q) || body.lowercase().contains(q) ||
            checks.any { it.text.lowercase().contains(q) }
    }
}
