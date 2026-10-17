package id.my.timecraft.lembar.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Menyimpan semua catatan sebagai satu berkas JSON di penyimpanan internal aplikasi.
 * Tanpa database atau dependency tambahan; cukup untuk ribuan catatan pribadi.
 * Penulisan lewat berkas sementara lalu rename, supaya berkas tidak rusak kalau aplikasi
 * mati di tengah penyimpanan.
 */
class NoteStore(context: Context) {
    private val file = File(context.filesDir, "notes.json")
    private val mutex = Mutex()

    /** null berarti belum pernah ada data (pertama kali dibuka). */
    suspend fun load(): List<Note>? = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (!file.exists()) return@withContext null
            runCatching {
                val arr = JSONArray(file.readText())
                List(arr.length()) { noteFromJson(arr.getJSONObject(it)) }
            }.getOrElse { emptyList() }
        }
    }

    // Kunci diambil sebelum pindah ke thread IO, jadi urutan simpan sama dengan urutan pemanggilan.
    suspend fun save(notes: List<Note>): Unit = mutex.withLock {
        withContext(Dispatchers.IO) {
            val arr = JSONArray()
            notes.forEach { arr.put(noteToJson(it)) }
            val tmp = File(file.parentFile, "notes.json.tmp")
            tmp.writeText(arr.toString())
            if (!tmp.renameTo(file)) {
                file.writeText(arr.toString())
                tmp.delete()
            }
        }
    }

    private fun noteToJson(n: Note) = JSONObject().apply {
        put("id", n.id)
        put("title", n.title)
        put("body", n.body)
        put("space", n.space)
        put("color", n.color.key)
        put("pinned", n.pinned)
        put("mode", n.mode.key)
        put("paper", n.paper.key)
        put("updatedAt", n.updatedAt)
        put("checks", JSONArray().apply {
            n.checks.forEach { put(JSONObject().put("t", it.text).put("d", it.done)) }
        })
        put("pages", JSONArray().apply {
            n.pages.forEach { page -> put(strokesToJson(page)) }
        })
    }

    private fun noteFromJson(o: JSONObject): Note {
        val checks = o.optJSONArray("checks") ?: JSONArray()
        // "pages" sejak versi berlembar; catatan lama hanya punya satu lembar di "ink".
        val pages = o.optJSONArray("pages")?.let { arr -> List(arr.length()) { strokesFromJson(arr.getJSONArray(it)) } }
            ?: listOf(strokesFromJson(o.optJSONArray("ink") ?: JSONArray()))
        return Note(
            id = o.getString("id"),
            title = o.optString("title"),
            body = o.optString("body"),
            space = o.optString("space", "Pribadi"),
            color = CardColor.entries.firstOrNull { it.key == o.optString("color") } ?: CardColor.Krem,
            pinned = o.optBoolean("pinned"),
            mode = NoteMode.entries.firstOrNull { it.key == o.optString("mode") } ?: NoteMode.Ketik,
            paper = Paper.entries.firstOrNull { it.key == o.optString("paper") } ?: Paper.Garis,
            updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
            checks = List(checks.length()) {
                val c = checks.getJSONObject(it)
                CheckItem(c.optString("t"), c.optBoolean("d"))
            },
            pages = pages.ifEmpty { listOf(emptyList()) }
        )
    }

    private fun strokesToJson(strokes: List<InkStroke>) = JSONArray().apply {
        strokes.forEach { s ->
            val pts = JSONArray()
            // Dibulatkan 1 desimal: cukup presisi untuk ruang 1000x1333 dan berkas tetap kecil.
            s.points.forEach { v -> pts.put(Math.round(v * 10) / 10.0) }
            put(JSONObject().put("tool", s.tool.key).put("color", s.color.key).put("pts", pts))
        }
    }

    private fun strokesFromJson(arr: JSONArray) = List(arr.length()) {
        val s = arr.getJSONObject(it)
        val pts = s.getJSONArray("pts")
        InkStroke(
            tool = InkTool.entries.firstOrNull { t -> t.key == s.optString("tool") } ?: InkTool.Pena,
            color = InkColor.entries.firstOrNull { c -> c.key == s.optString("color") } ?: InkColor.Tinta,
            points = FloatArray(pts.length()) { i -> pts.getDouble(i).toFloat() }
        )
    }
}
