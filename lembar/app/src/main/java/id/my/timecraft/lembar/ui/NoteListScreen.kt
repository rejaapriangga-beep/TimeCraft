package id.my.timecraft.lembar.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.my.timecraft.lembar.data.Note
import id.my.timecraft.lembar.data.NoteMode
import id.my.timecraft.lembar.data.SPACES
import id.my.timecraft.lembar.ui.ink.InkPreview
import id.my.timecraft.lembar.ui.theme.Lembar
import java.util.Calendar

const val ALL = "Semua"

@Composable
fun NoteListScreen(
    notes: List<Note>,
    filter: String,
    onFilter: (String) -> Unit,
    query: String,
    onQuery: (String) -> Unit,
    selectedId: String?,
    onOpen: (Note) -> Unit,
    onNew: (NoteMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Lembar.colors
    val visible = remember(notes, filter, query) {
        notes.filter { (filter == ALL || it.space == filter) && it.matches(query) }
            .sortedByDescending { it.updatedAt }
    }
    val pinned = visible.filter { it.pinned }
    val rest = visible.filterNot { it.pinned }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(greeting(), style = MaterialTheme.typography.bodyMedium, color = c.muted)
                    Text("Catatanmu", style = MaterialTheme.typography.displaySmall, color = c.ink)
                    Spacer(Modifier.height(10.dp))
                    SearchField(query, onQuery)
                }
            }
            item(key = "tabs") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                    items(listOf(ALL) + SPACES) { s ->
                        val n = if (s == ALL) notes.size else notes.count { it.space == s }
                        Pill("$s · $n", selected = s == filter, onClick = { onFilter(s) })
                    }
                }
            }
            if (visible.isEmpty()) {
                item(key = "empty") {
                    Text(
                        if (query.isNotBlank()) "Tidak ada catatan yang cocok dengan \"$query\".\nCoba kata lain atau pilih ruang \"Semua\"."
                        else "Ruang ini masih kosong.\nKetuk \"Catatan baru\" untuk mulai menulis.",
                        style = MaterialTheme.typography.bodyMedium, color = c.muted,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            if (pinned.isNotEmpty()) {
                item(key = "lbl-pin") { SectionLabel("Disematkan", Modifier.padding(top = 6.dp)) }
                items(pinned, key = { it.id }) { NoteCard(it, it.id == selectedId) { onOpen(it) } }
            }
            if (rest.isNotEmpty()) {
                if (pinned.isNotEmpty()) item(key = "lbl-rest") { SectionLabel("Lainnya", Modifier.padding(top = 6.dp)) }
                items(rest, key = { it.id }) { NoteCard(it, it.id == selectedId) { onOpen(it) } }
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { onNew(NoteMode.Tulis) },
                containerColor = c.sun, contentColor = c.onSun,
                modifier = Modifier.size(48.dp)
            ) { Icon(Icons.Rounded.Draw, contentDescription = "Catatan tulis tangan baru") }
            ExtendedFloatingActionButton(
                onClick = { onNew(NoteMode.Ketik) },
                containerColor = c.leaf, contentColor = Color.White,
                shape = RoundedCornerShape(50),
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Catatan baru", style = MaterialTheme.typography.labelLarge) }
            )
        }
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    val c = Lembar.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.card).padding(start = 12.dp, end = 4.dp)
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = c.muted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = c.ink),
            cursorBrush = SolidColor(c.leaf),
            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
            decorationBox = { inner ->
                if (query.isEmpty()) Text("Cari judul, isi, atau checklist", style = MaterialTheme.typography.bodyMedium, color = c.muted)
                inner()
            }
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQuery("") }) {
                Icon(Icons.Rounded.Close, contentDescription = "Hapus pencarian", tint = c.muted)
            }
        } else {
            Spacer(Modifier.height(44.dp))
        }
    }
}

@Composable
private fun NoteCard(note: Note, selected: Boolean, onClick: () -> Unit) {
    val c = Lembar.colors
    val fg = c.onCardOf(note.color)
    val total = note.checks.size
    val done = note.checks.count { it.done }
    val preview = note.body.ifBlank { note.checks.joinToString("   ") { (if (it.done) "✓ " else "○ ") + it.text } }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = c.cardOf(note.color),
        contentColor = fg,
        border = if (selected) BorderStroke(2.dp, c.leaf) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    note.title.ifBlank { "Tanpa judul" },
                    style = MaterialTheme.typography.titleMedium, color = fg,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
                if (note.pinned) Icon(Icons.Rounded.PushPin, contentDescription = "Disematkan", tint = fg, modifier = Modifier.size(18.dp))
            }
            val firstPage = note.pages.firstOrNull { it.isNotEmpty() }
            if (firstPage != null) {
                InkPreview(
                    firstPage,
                    Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(12.dp)).background(c.surface.copy(alpha = 0.55f))
                )
            }
            if (preview.isNotBlank()) {
                Text(preview, style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.78f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (total > 0) {
                LinearProgressIndicator(
                    progress = { done.toFloat() / total },
                    color = c.leaf, trackColor = fg.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50))
                )
            }
            Row {
                val meta = buildString {
                    append(note.space)
                    if (note.hasInk) {
                        append(" · tulis tangan")
                        if (note.pages.size > 1) append(" · ${note.pages.size} lembar")
                    }
                    if (total > 0) append(" · $done/$total selesai")
                }
                Text(meta, style = MaterialTheme.typography.labelMedium, color = fg.copy(alpha = 0.7f), modifier = Modifier.weight(1f))
                Text(timeAgo(note.updatedAt), style = MaterialTheme.typography.labelMedium, color = fg.copy(alpha = 0.7f))
            }
        }
    }
}

private fun greeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        h < 11 -> "Selamat pagi"
        h < 15 -> "Selamat siang"
        h < 18 -> "Selamat sore"
        else -> "Selamat malam"
    }
}
