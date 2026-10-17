package id.my.timecraft.lembar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.my.timecraft.lembar.data.Note
import id.my.timecraft.lembar.data.NoteStore
import id.my.timecraft.lembar.data.SampleNotes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotesViewModel(app: Application) : AndroidViewModel(app) {
    private val store = NoteStore(app)

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = store.load()
            _notes.value = saved ?: SampleNotes.create().also { store.save(it) }
            _loaded.value = true
        }
    }

    fun find(id: String): Note? = _notes.value.firstOrNull { it.id == id }

    /** Menyimpan catatan. Catatan kosong tidak disimpan, dan dihapus kalau sebelumnya sudah ada. */
    fun save(note: Note): SaveResult {
        val exists = _notes.value.any { it.id == note.id }
        if (note.isEmpty) {
            if (exists) delete(note.id)
            return if (exists) SaveResult.DeletedEmpty else SaveResult.Discarded
        }
        val clean = note.copy(
            title = note.title.trim(),
            body = note.body.trim(),
            checks = note.checks.filter { it.text.isNotBlank() },
            updatedAt = System.currentTimeMillis()
        )
        _notes.update { list -> if (exists) list.map { if (it.id == clean.id) clean else it } else listOf(clean) + list }
        persist()
        return if (exists) SaveResult.Updated else SaveResult.Created
    }

    fun delete(id: String) {
        _notes.update { list -> list.filterNot { it.id == id } }
        persist()
    }

    private fun persist() {
        val snapshot = _notes.value
        viewModelScope.launch { store.save(snapshot) }
    }
}

enum class SaveResult { Created, Updated, DeletedEmpty, Discarded }
