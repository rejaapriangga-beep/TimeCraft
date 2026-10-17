@file:OptIn(ExperimentalLayoutApi::class)

package id.my.timecraft.lembar.ui

import android.widget.Toast
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import id.my.timecraft.lembar.data.InkPage
import id.my.timecraft.lembar.ui.ink.InkClipboard
import id.my.timecraft.lembar.ui.ink.boundsOf
import id.my.timecraft.lembar.ui.ink.clampMove
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.my.timecraft.lembar.data.CardColor
import id.my.timecraft.lembar.data.CheckItem
import id.my.timecraft.lembar.data.InkColor
import id.my.timecraft.lembar.data.InkStroke
import id.my.timecraft.lembar.data.Note
import id.my.timecraft.lembar.data.NoteMode
import id.my.timecraft.lembar.data.Paper
import id.my.timecraft.lembar.data.PenPrefs
import id.my.timecraft.lembar.data.SPACES
import id.my.timecraft.lembar.ui.ink.INK_ASPECT
import id.my.timecraft.lembar.ui.ink.InkCanvas
import id.my.timecraft.lembar.ui.ink.InkMode
import id.my.timecraft.lembar.ui.theme.Caveat
import id.my.timecraft.lembar.ui.theme.Lembar

private const val MAX_UNDO = 50

/**
 * Editor satu catatan. Perubahan ditampung di [draft] lokal dan baru diserahkan lewat [onDone]
 * (tombol Simpan, panah kembali, atau tombol back sistem). [onDraftChange] dipanggil setiap
 * draft berubah supaya aktivitas bisa menyimpan otomatis saat aplikasi ditutup.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    initial: Note,
    isNew: Boolean,
    showBack: Boolean,
    onDone: (Note) -> Unit,
    onDelete: (String) -> Unit,
    onDraftChange: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Lembar.colors
    val context = LocalContext.current
    var draft by remember(initial.id) { mutableStateOf(initial) }
    // Riwayat urungkan menyimpan seluruh lembar, jadi menambah/menghapus lembar juga bisa diurungkan.
    val undo = remember(initial.id) { mutableStateListOf<List<InkPage>>() }
    var pageIndex by remember(initial.id) { mutableIntStateOf(0) }
    var selected by remember(initial.id) { mutableStateOf<Set<InkStroke>>(emptySet()) }
    var inkMode by rememberSaveable { mutableStateOf(InkMode.Pena) }
    var inkColor by rememberSaveable { mutableStateOf(InkColor.Tinta) }
    val penPrefs = remember { PenPrefs(context) }
    var penOnly by remember { mutableStateOf(penPrefs.penOnly) }
    var stylusSeen by remember { mutableStateOf(penPrefs.stylusSeen) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var pageMenu by remember { mutableStateOf(false) }

    val page = pageIndex.coerceIn(0, draft.pages.lastIndex)
    val strokes = draft.pages[page]
    val selection = strokes.filter { it in selected }

    LaunchedEffect(draft) { onDraftChange(draft) }
    BackHandler { onDone(draft) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    fun snapshot() {
        undo.add(draft.pages)
        if (undo.size > MAX_UNDO) undo.removeAt(0)
    }
    fun setStrokes(list: List<InkStroke>) {
        draft = draft.copy(pages = draft.pages.replace(page, list))
    }
    fun goTo(i: Int) {
        pageIndex = i
        selected = emptySet()
    }

    Column(modifier.fillMaxSize().background(c.surface)) {
        // ---------- bilah atas ----------
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (showBack) {
                IconButton(onClick = { onDone(draft) }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Simpan dan kembali", tint = c.ink)
                }
            }
            Spacer(Modifier.weight(1f))
            // Lingkaran warna kartu: membuka pilihan ruang dan warna kartu (berlaku di kedua mode).
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Swatch(c.cardOf(draft.color), selected = false, label = "Ruang dan warna kartu", onClick = { showDetails = true }, size = 28)
            }
            IconButton(onClick = { draft = draft.copy(pinned = !draft.pinned) }) {
                Icon(
                    Icons.Rounded.PushPin,
                    contentDescription = if (draft.pinned) "Lepas sematan" else "Sematkan",
                    tint = if (draft.pinned) c.leaf else c.muted
                )
            }
            if (!isNew) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Hapus catatan", tint = c.danger)
                }
            }
            Button(
                onClick = { onDone(draft) },
                colors = ButtonDefaults.buttonColors(containerColor = c.leaf, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Simpan", style = MaterialTheme.typography.labelLarge) }
        }

        // ---------- judul + pilihan mode ----------
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BasicTextField(
                value = draft.title,
                onValueChange = { draft = draft.copy(title = it) },
                textStyle = MaterialTheme.typography.headlineSmall.copy(color = c.ink),
                cursorBrush = SolidColor(c.leaf),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (draft.title.isEmpty()) Text("Judul", style = MaterialTheme.typography.headlineSmall, color = c.muted.copy(alpha = 0.6f))
                    inner()
                }
            )
            ModeSwitch(draft.mode) { draft = draft.copy(mode = it) }
        }
        Spacer(Modifier.height(12.dp))

        when (draft.mode) {
            NoteMode.Ketik -> TypePane(draft, onChange = { draft = it }, modifier = Modifier.weight(1f))
            NoteMode.Tulis -> Column(
                Modifier.weight(1f).padding(horizontal = 20.dp).padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InkToolbar(
                    mode = inkMode,
                    onMode = {
                        inkMode = it
                        if (it != InkMode.Pilih) selected = emptySet()
                    },
                    color = inkColor,
                    onColor = { ic ->
                        if (inkMode == InkMode.Pilih && selection.isNotEmpty()) {
                            // Mengganti warna coretan yang sedang dipilih.
                            snapshot()
                            val recolored = selection.associateWith { it.recolored(ic) }
                            setStrokes(strokes.map { recolored[it] ?: it })
                            selected = recolored.values.toSet()
                        } else {
                            inkColor = ic
                            if (inkMode == InkMode.Hapus || inkMode == InkMode.Pilih) {
                                inkMode = InkMode.Pena
                                selected = emptySet()
                            }
                        }
                    },
                    canUndo = undo.isNotEmpty(),
                    onUndo = {
                        if (undo.isNotEmpty()) {
                            draft = draft.copy(pages = undo.removeAt(undo.lastIndex))
                            selected = emptySet()
                        }
                    },
                    paper = draft.paper,
                    onPaper = { draft = draft.copy(paper = Paper.entries[(draft.paper.ordinal + 1) % Paper.entries.size]) }
                )
                if (inkMode == InkMode.Pilih) {
                    SelectionBar(
                        count = selection.size,
                        canPaste = InkClipboard.strokes.isNotEmpty(),
                        onCopy = {
                            InkClipboard.strokes = selection
                            toast("${selection.size} coretan disalin")
                        },
                        onCut = {
                            InkClipboard.strokes = selection
                            snapshot()
                            setStrokes(strokes.filterNot { it in selected })
                            selected = emptySet()
                            toast("${selection.size} coretan dipotong")
                        },
                        onDelete = {
                            snapshot()
                            setStrokes(strokes.filterNot { it in selected })
                            selected = emptySet()
                        },
                        onPaste = {
                            val clip = InkClipboard.strokes
                            // Ditempel sedikit bergeser supaya tidak menumpuk persis di atas aslinya.
                            val shift = boundsOf(clip)?.let { clampMove(it, 40f, 40f) } ?: Offset.Zero
                            val pasted = clip.map { it.translated(shift.x, shift.y) }
                            snapshot()
                            setStrokes(strokes + pasted)
                            selected = pasted.toSet()
                        }
                    )
                }
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Box(
                        Modifier
                            .aspectRatio(INK_ASPECT)
                            .clip(RoundedCornerShape(16.dp))
                            .background(c.surface)
                            .border(1.dp, c.line, RoundedCornerShape(16.dp))
                    ) {
                        InkCanvas(
                            strokes = strokes,
                            paper = draft.paper,
                            mode = inkMode,
                            color = inkColor,
                            penOnly = penOnly,
                            selected = selected,
                            onSelect = { selected = it },
                            onStylusDetected = {
                                if (!stylusSeen) {
                                    stylusSeen = true
                                    penPrefs.stylusSeen = true
                                    if (!penOnly) {
                                        penOnly = true
                                        penPrefs.penOnly = true
                                        toast("Pensil terdeteksi, sentuhan telapak diabaikan")
                                    }
                                }
                            },
                            onBeforeChange = { snapshot() },
                            onStrokesChange = { setStrokes(it) },
                            onDiscardLastUndo = { if (undo.isNotEmpty()) undo.removeAt(undo.lastIndex) },
                            resolve = c::inkOf,
                            paperLine = c.sky.copy(alpha = 0.45f),
                            paperDot = c.muted.copy(alpha = 0.45f),
                            selectionColor = c.leaf,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (strokes.isEmpty()) {
                            Text(
                                if (inkMode == InkMode.Pilih) "lembar ini masih kosong" else "tulis di sini…",
                                style = TextStyle(fontFamily = Caveat, fontSize = 26.sp, color = c.muted),
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }

                // ---------- navigasi lembar ----------
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { goTo(page - 1) }, enabled = page > 0) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Lembar sebelumnya", tint = if (page > 0) c.ink else c.line)
                    }
                    Text(
                        "Lembar ${page + 1} / ${draft.pages.size}",
                        style = MaterialTheme.typography.labelMedium, color = c.ink
                    )
                    IconButton(onClick = { goTo(page + 1) }, enabled = page < draft.pages.lastIndex) {
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Lembar berikutnya",
                            tint = if (page < draft.pages.lastIndex) c.ink else c.line
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        draft = draft.copy(pages = draft.pages.toMutableList().apply { add(page + 1, emptyList()) })
                        goTo(page + 1)
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = c.leaf, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Lembar baru", color = c.leaf, style = MaterialTheme.typography.labelLarge)
                    }
                    Box {
                        IconButton(onClick = { pageMenu = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "Menu lembar", tint = c.ink)
                        }
                        DropdownMenu(expanded = pageMenu, onDismissRequest = { pageMenu = false }, containerColor = c.surface) {
                            DropdownMenuItem(
                                text = { Text("Bersihkan lembar ini") },
                                enabled = strokes.isNotEmpty(),
                                onClick = { pageMenu = false; confirmClear = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Hapus lembar ini", color = if (draft.pages.size > 1) c.danger else c.line) },
                                enabled = draft.pages.size > 1,
                                onClick = {
                                    pageMenu = false
                                    snapshot()
                                    draft = draft.copy(pages = draft.pages.filterIndexed { i, _ -> i != page })
                                    goTo((page - 1).coerceAtLeast(0))
                                    toast("Lembar dihapus. Tekan urungkan untuk mengembalikan.")
                                }
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = penOnly, onCheckedChange = { penOnly = it; penPrefs.penOnly = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = c.leaf)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Hanya pensil (abaikan sentuhan telapak)", style = MaterialTheme.typography.labelMedium, color = c.ink)
                }
            }
        }
    }

    if (showDetails) {
        ModalBottomSheet(
            onDismissRequest = { showDetails = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = c.surface
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SectionLabel("Ruang")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SPACES.forEach { s -> Pill(s, selected = s == draft.space, onClick = { draft = draft.copy(space = s) }) }
                }
                SectionLabel("Warna kartu")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CardColor.entries.forEach { cc ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Swatch(c.cardOf(cc), selected = cc == draft.color, label = "Warna ${cc.label}", onClick = { draft = draft.copy(color = cc) }, size = 40)
                            Text(cc.label, style = MaterialTheme.typography.bodySmall, color = c.muted)
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Hapus catatan ini?") },
            text = { Text("Catatan yang dihapus tidak bisa dikembalikan.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(draft.id) }) { Text("Hapus", color = c.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Batal", color = c.ink) } },
            containerColor = c.surface
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Bersihkan lembar ini?") },
            text = { Text("Semua tulisan tangan di lembar ini akan dihapus. Kamu masih bisa mengurungkannya.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    snapshot()
                    setStrokes(emptyList())
                    selected = emptySet()
                }) { Text("Bersihkan", color = c.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Batal", color = c.ink) } },
            containerColor = c.surface
        )
    }
}

@Composable
private fun ModeSwitch(mode: NoteMode, onMode: (NoteMode) -> Unit) {
    val c = Lembar.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.card).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            Triple(NoteMode.Ketik, "Ketik", Icons.Rounded.Keyboard),
            Triple(NoteMode.Tulis, "Tulis", Icons.Rounded.Draw)
        ).forEach { (m, label, icon) ->
            val on = m == mode
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) c.surface else Color.Transparent)
                    .clickable { onMode(m) }
                    .padding(vertical = 10.dp)
            ) {
                Icon(icon, contentDescription = null, tint = if (on) c.ink else c.muted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) c.ink else c.muted)
            }
        }
    }
}

@Composable
private fun TypePane(draft: Note, onChange: (Note) -> Unit, modifier: Modifier) {
    val c = Lembar.colors
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        BasicTextField(
            value = draft.body,
            onValueChange = { onChange(draft.copy(body = it)) },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.ink),
            cursorBrush = SolidColor(c.leaf),
            modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
            decorationBox = { inner ->
                if (draft.body.isEmpty()) Text("Tulis apa saja…", style = MaterialTheme.typography.bodyLarge, color = c.muted)
                inner()
            }
        )

        SectionLabel("Checklist")
        draft.checks.forEachIndexed { i, item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = item.done,
                    onCheckedChange = { v -> onChange(draft.copy(checks = draft.checks.replace(i, item.copy(done = v)))) },
                    colors = CheckboxDefaults.colors(checkedColor = c.leaf, uncheckedColor = c.muted)
                )
                BasicTextField(
                    value = item.text,
                    onValueChange = { v -> onChange(draft.copy(checks = draft.checks.replace(i, item.copy(text = v)))) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = if (item.done) c.muted else c.ink,
                        textDecoration = if (item.done) TextDecoration.LineThrough else null
                    ),
                    cursorBrush = SolidColor(c.leaf),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (item.text.isEmpty()) Text("Item checklist", style = MaterialTheme.typography.bodyMedium, color = c.muted)
                        inner()
                    }
                )
                IconButton(onClick = { onChange(draft.copy(checks = draft.checks.filterIndexed { j, _ -> j != i })) }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Hapus item", tint = c.muted, modifier = Modifier.size(18.dp))
                }
            }
        }
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, c.line, RoundedCornerShape(12.dp))
                .clickable { onChange(draft.copy(checks = draft.checks + CheckItem(""))) }
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) { Text("+ Tambah item", style = MaterialTheme.typography.labelLarge, color = c.leaf) }
    }
}


@Composable
private fun InkToolbar(
    mode: InkMode, onMode: (InkMode) -> Unit,
    color: InkColor, onColor: (InkColor) -> Unit,
    canUndo: Boolean, onUndo: () -> Unit,
    paper: Paper, onPaper: () -> Unit
) {
    val c = Lembar.colors
    // FlowRow: di ponsel sempit alat-alat turun ke baris kedua, tidak terpotong di kanan.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Pill("Pena", mode == InkMode.Pena, { onMode(InkMode.Pena) })
        Pill("Stabilo", mode == InkMode.Stabilo, { onMode(InkMode.Stabilo) })
        Pill("Hapus", mode == InkMode.Hapus, { onMode(InkMode.Hapus) })
        Pill("Pilih", mode == InkMode.Pilih, { onMode(InkMode.Pilih) })
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(40.dp)
        ) {
            listOf(InkColor.Tinta, InkColor.Hijau, InkColor.Biru, InkColor.Peach).forEach { ic ->
                Swatch(
                    c.inkOf(ic), selected = ic == color && mode != InkMode.Hapus,
                    label = "Tinta ${ic.key}", onClick = { onColor(ic) }, size = 26
                )
            }
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Urungkan", tint = if (canUndo) c.ink else c.line)
            }
            Pill(paper.label, false, onPaper)
        }
    }
}

/** Aksi untuk coretan terpilih di mode Pilih. */
@Composable
private fun SelectionBar(
    count: Int, canPaste: Boolean,
    onCopy: () -> Unit, onCut: () -> Unit, onDelete: () -> Unit, onPaste: () -> Unit
) {
    val c = Lembar.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.leafSoft).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            if (count == 0) "Seret kotak di lembar untuk memilih coretan" else "$count coretan dipilih. Seret kotaknya untuk memindahkan.",
            style = MaterialTheme.typography.bodySmall, color = c.ink
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SelectionAction(Icons.Rounded.ContentCopy, "Salin", count > 0, onCopy)
            SelectionAction(Icons.Rounded.ContentCut, "Potong", count > 0, onCut)
            SelectionAction(Icons.Rounded.Delete, "Hapus", count > 0, onDelete)
            SelectionAction(Icons.Rounded.ContentPaste, "Tempel", canPaste, onPaste)
        }
    }
}

@Composable
private fun SelectionAction(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val c = Lembar.colors
    val tint = if (enabled) c.ink else c.muted.copy(alpha = 0.5f)
    TextButton(onClick = onClick, enabled = enabled) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = tint, style = MaterialTheme.typography.labelLarge)
    }
}

private fun <T> List<T>.replace(index: Int, value: T): List<T> = mapIndexed { i, v -> if (i == index) value else v }
