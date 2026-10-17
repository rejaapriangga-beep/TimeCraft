package id.my.timecraft.lembar.data

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Contoh catatan untuk pembukaan pertama, supaya layar tidak kosong dan semua fitur terlihat. */
object SampleNotes {
    private const val HOUR = 3_600_000L

    fun create(now: Long = System.currentTimeMillis()): List<Note> = listOf(
        Note(
            title = "Target minggu ini", body = "Fokus ke tiga hal saja. Sisanya boleh menunggu.",
            space = "Kerja", color = CardColor.Kuning, pinned = true, updatedAt = now - 2 * HOUR,
            checks = listOf(
                CheckItem("Kirim laporan mingguan", true),
                CheckItem("Review dashboard", true),
                CheckItem("Rapat evaluasi Jumat")
            )
        ),
        Note(
            title = "Sketsa alur aplikasi", space = "Kerja", mode = NoteMode.Tulis,
            updatedAt = now - 3 * HOUR, pages = listOf(sketch())
        ),
        Note(
            title = "Ide video: belajar Kotlin",
            body = "Hook: 30 hari dari nol sampai rilis aplikasi pertama. Tunjukkan prosesnya, bukan hanya hasil.",
            space = "Belajar", updatedAt = now - 5 * HOUR
        ),
        Note(
            title = "Belanja bulanan", space = "Pribadi", color = CardColor.Hijau, updatedAt = now - 26 * HOUR,
            checks = listOf(CheckItem("Beras 5 kg", true), CheckItem("Kopi"), CheckItem("Sabun cuci"), CheckItem("Buah"))
        ),
        Note(
            title = "Catatan buku: Feel-Good Productivity",
            body = "Energi datang dari permainan, kekuatan, dan koneksi. Coba ubah tugas membosankan jadi tantangan kecil.",
            space = "Belajar", color = CardColor.Biru, updatedAt = now - 50 * HOUR
        )
    )

    // Goresan sedikit bergelombang supaya terlihat seperti ditulis tangan.
    private fun wob(i: Int, s: Float) = sin(i * 1.7f + s) * 4f

    private fun line(x1: Float, y1: Float, x2: Float, y2: Float, s: Float): FloatArray {
        val out = FloatArray(25 * 3)
        for (i in 0..24) {
            val t = i / 24f
            out[i * 3] = x1 + (x2 - x1) * t + wob(i, s)
            out[i * 3 + 1] = y1 + (y2 - y1) * t + wob(i, s + 2)
            out[i * 3 + 2] = 0.4f + 0.4f * sin(t * PI.toFloat())
        }
        return out
    }

    private fun box(x: Float, y: Float, w: Float, h: Float, s: Float) =
        line(x, y, x + w, y, s) + line(x + w, y, x + w, y + h, s + 1) +
            line(x + w, y + h, x, y + h, s + 2) + line(x, y + h, x, y + 4, s + 3)

    private fun ring(cx: Float, cy: Float, r: Float, s: Float): FloatArray {
        val out = FloatArray(41 * 3)
        for (i in 0..40) {
            val a = i / 40f * PI.toFloat() * 2.1f - 1f
            out[i * 3] = cx + cos(a) * (r + wob(i, s))
            out[i * 3 + 1] = cy + sin(a) * (r * 0.62f + wob(i, s + 1))
            out[i * 3 + 2] = 0.5f
        }
        return out
    }

    private fun pen(c: InkColor, p: FloatArray) = InkStroke(InkTool.Pena, c, p)

    private fun sketch() = listOf(
        InkStroke(InkTool.Stabilo, InkColor.Kuning, line(90f, 190f, 640f, 190f, 1f)),
        pen(InkColor.Tinta, box(90f, 300f, 300f, 170f, 1f)),
        pen(InkColor.Tinta, box(590f, 300f, 300f, 170f, 2f)),
        pen(InkColor.Tinta, line(400f, 385f, 580f, 385f, 3f)),
        pen(InkColor.Tinta, line(545f, 355f, 582f, 385f, 4f)),
        pen(InkColor.Tinta, line(545f, 415f, 582f, 385f, 5f)),
        pen(InkColor.Hijau, box(340f, 640f, 320f, 180f, 3f)),
        pen(InkColor.Tinta, line(740f, 480f, 560f, 630f, 6f)),
        pen(InkColor.Biru, ring(500f, 730f, 230f, 2f)),
        pen(InkColor.Hijau, line(140f, 980f, 200f, 1040f, 1f)),
        pen(InkColor.Hijau, line(200f, 1040f, 320f, 900f, 2f)),
        pen(InkColor.Tinta, line(380f, 990f, 860f, 990f, 4f)),
        pen(InkColor.Tinta, line(380f, 1070f, 740f, 1070f, 5f))
    )
}
