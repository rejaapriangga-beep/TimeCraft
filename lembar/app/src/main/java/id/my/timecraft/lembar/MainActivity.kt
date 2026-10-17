package id.my.timecraft.lembar

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.my.timecraft.lembar.data.Note
import id.my.timecraft.lembar.data.NoteMode
import id.my.timecraft.lembar.ui.ALL
import id.my.timecraft.lembar.ui.EditorScreen
import id.my.timecraft.lembar.ui.NoteListScreen
import id.my.timecraft.lembar.ui.theme.Caveat
import id.my.timecraft.lembar.ui.theme.Lembar
import id.my.timecraft.lembar.ui.theme.LembarTheme
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LembarTheme { LembarApp() } }
    }
}

/**
 * Yang sedang dibuka di editor. Disimpan sebagai string agar selamat dari rotasi layar:
 * "note:<id>" untuk catatan lama, "new:<mode>:<id>" untuk catatan baru.
 */
private sealed interface Target {
    data class Existing(val id: String) : Target
    data class New(val mode: NoteMode, val id: String) : Target

    companion object {
        fun parse(s: String?): Target? {
            val p = s?.split(":") ?: return null
            return when (p.firstOrNull()) {
                "note" -> Existing(p[1])
                "new" -> New(NoteMode.entries.firstOrNull { it.key == p[1] } ?: NoteMode.Ketik, p[2])
                else -> null
            }
        }
    }
}

private fun Target.encode() = when (this) {
    is Target.Existing -> "note:$id"
    is Target.New -> "new:${mode.key}:$id"
}

/** Draft terakhir dari editor, disimpan otomatis saat aplikasi masuk latar belakang. */
private class DraftHolder { var note: Note? = null }

@Composable
private fun LembarApp(vm: NotesViewModel = viewModel()) {
    val c = Lembar.colors
    val context = LocalContext.current
    val notes by vm.notes.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    var targetKey by rememberSaveable { mutableStateOf<String?>(null) }
    var filter by rememberSaveable { mutableStateOf(ALL) }
    var query by rememberSaveable { mutableStateOf("") }
    val draft = remember { DraftHolder() }
    val target = Target.parse(targetKey)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) draft.note?.let { vm.save(it) }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun finish(note: Note) {
        when (vm.save(note)) {
            SaveResult.Created -> Toast.makeText(context, "Catatan disimpan", Toast.LENGTH_SHORT).show()
            SaveResult.Updated -> Toast.makeText(context, "Perubahan disimpan", Toast.LENGTH_SHORT).show()
            SaveResult.DeletedEmpty -> Toast.makeText(context, "Catatan kosong dihapus", Toast.LENGTH_SHORT).show()
            SaveResult.Discarded -> Unit
        }
        draft.note = null
        targetKey = null
    }

    fun open(next: Target) {
        // Di tablet, berpindah catatan menyimpan draft yang sedang terbuka lebih dulu.
        draft.note?.let { vm.save(it) }
        draft.note = null
        targetKey = next.encode()
    }

    val editorNote: Note? = when (target) {
        is Target.Existing -> vm.find(target.id) ?: draft.note?.takeIf { it.id == target.id }
        is Target.New -> vm.find(target.id) ?: Note(
            id = target.id,
            mode = target.mode,
            space = if (filter == ALL) "Pribadi" else filter
        )
        null -> null
    }

    @Composable
    fun Editor(note: Note, showBack: Boolean, modifier: Modifier = Modifier) {
        EditorScreen(
            initial = note,
            isNew = vm.find(note.id) == null,
            showBack = showBack,
            onDone = ::finish,
            onDelete = { id ->
                vm.delete(id)
                draft.note = null
                targetKey = null
                Toast.makeText(context, "Catatan dihapus", Toast.LENGTH_SHORT).show()
            },
            onDraftChange = { draft.note = it },
            modifier = modifier
        )
    }

    @Composable
    fun ListPane(modifier: Modifier = Modifier) {
        NoteListScreen(
            notes = notes,
            filter = filter, onFilter = { filter = it },
            query = query, onQuery = { query = it },
            selectedId = editorNote?.id,
            onOpen = { open(Target.Existing(it.id)) },
            onNew = { mode -> open(Target.New(mode, UUID.randomUUID().toString())) },
            modifier = modifier
        )
    }

    BoxWithConstraints(
        Modifier.fillMaxSize().background(c.bg).windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        if (!loaded) return@BoxWithConstraints
        val wide = maxWidth >= 720.dp
        if (wide) {
            // Tablet: daftar di kiri, editor di kanan. Kanvas tulis memenuhi panel kanan.
            Row(Modifier.fillMaxSize()) {
                ListPane(Modifier.width(380.dp).fillMaxHeight())
                Box(Modifier.width(1.dp).fillMaxHeight().background(c.line))
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    if (editorNote != null) {
                        Editor(editorNote, showBack = false)
                    } else {
                        Column(
                            Modifier.fillMaxSize().padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("pilih catatan, atau mulai yang baru", style = MaterialTheme.typography.headlineSmall.copy(fontFamily = Caveat), color = c.muted, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        } else if (editorNote != null) {
            Editor(editorNote, showBack = true)
        } else {
            ListPane()
        }
    }
}
