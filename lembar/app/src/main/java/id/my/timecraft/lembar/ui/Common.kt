package id.my.timecraft.lembar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import id.my.timecraft.lembar.ui.theme.Lembar

/** Chip pil untuk filter ruang dan pilihan ruang di editor. Aktif = tinta penuh. */
@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Lembar.colors
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .then(if (selected) Modifier.background(c.ink) else Modifier.border(1.dp, c.line, RoundedCornerShape(50)))
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = if (selected) c.bg else c.ink)
    }
}

/** Lingkaran warna untuk memilih warna kartu atau tinta. */
@Composable
fun Swatch(color: Color, selected: Boolean, label: String, onClick: () -> Unit, size: Int = 30) {
    val c = Lembar.colors
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color)
            .border(if (selected) 3.dp else 1.5.dp, if (selected) c.ink else c.line, CircleShape)
            .clickable(onClickLabel = label, role = Role.RadioButton, onClick = onClick)
    )
}

/** Label kecil huruf kapital, seperti "DISEMATKAN" atau "CHECKLIST". */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = Lembar.colors.muted, modifier = modifier)
}

fun timeAgo(t: Long, now: Long = System.currentTimeMillis()): String {
    val m = (now - t) / 60_000
    if (m < 1) return "baru saja"
    if (m < 60) return "$m mnt lalu"
    val h = (m + 30) / 60
    if (h < 24) return "$h jam lalu"
    return "${(h + 12) / 24} hari lalu"
}
