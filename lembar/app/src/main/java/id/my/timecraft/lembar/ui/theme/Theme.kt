@file:OptIn(ExperimentalTextApi::class)

package id.my.timecraft.lembar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import id.my.timecraft.lembar.R
import id.my.timecraft.lembar.data.CardColor
import id.my.timecraft.lembar.data.InkColor

/**
 * Palet Lembar: kertas hangat dan kartu pastel (gaya aliabdaal.com), hijau khusus untuk aksi
 * dan progres (gaya Bibit). Mode gelap memakai hue yang sama dengan kecerahan diturunkan.
 */
@Immutable
data class LembarColors(
    val bg: Color,
    val card: Color,
    val surface: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val sun: Color,
    val onSun: Color,
    val sky: Color,
    val skySoft: Color,
    val leaf: Color,
    val leafSoft: Color,
    val peach: Color,
    val peachSoft: Color,
    val lilac: Color,
    val rose: Color,
    val danger: Color
) {
    fun cardOf(c: CardColor) = when (c) {
        CardColor.Krem -> card
        CardColor.Kuning -> sun
        CardColor.Hijau -> leafSoft
        CardColor.Biru -> skySoft
        CardColor.Peach -> peachSoft
        CardColor.Ungu -> lilac
        CardColor.Merah -> rose
    }

    fun onCardOf(c: CardColor) = if (c == CardColor.Kuning) onSun else ink

    fun inkOf(c: InkColor) = when (c) {
        InkColor.Tinta -> ink
        InkColor.Hijau -> leaf
        InkColor.Biru -> sky
        InkColor.Peach -> peach
        InkColor.Kuning -> sun
    }
}

private val Light = LembarColors(
    bg = Color(0xFFFBFAF7), card = Color(0xFFF3F0EA), surface = Color(0xFFFFFFFF),
    ink = Color(0xFF1B1A26), muted = Color(0xFF5E5B6B), line = Color(0xFFE4DFD6),
    sun = Color(0xFFEFD27C), onSun = Color(0xFF1B1A26), sky = Color(0xFF7FC6EA), skySoft = Color(0xFFD3ECF8),
    leaf = Color(0xFF00A86B), leafSoft = Color(0xFFDDF3E9), peach = Color(0xFFF0B48C),
    peachSoft = Color(0xFFF9DFCC), lilac = Color(0xFFE6E0F6), rose = Color(0xFFF7D9DE), danger = Color(0xFFC2412D)
)

private val Dark = LembarColors(
    bg = Color(0xFF15141B), card = Color(0xFF211F29), surface = Color(0xFF1B1A22),
    ink = Color(0xFFF2EFE8), muted = Color(0xFFA9A5B5), line = Color(0xFF33303D),
    sun = Color(0xFFD9B95E), onSun = Color(0xFF1B1A26), sky = Color(0xFF6FB8DE), skySoft = Color(0xFF1F3542),
    leaf = Color(0xFF2BC48A), leafSoft = Color(0xFF173A2D), peach = Color(0xFFD99A72),
    peachSoft = Color(0xFF412E23), lilac = Color(0xFF2E2943), rose = Color(0xFF412630), danger = Color(0xFFF07A64)
)

val LocalLembarColors = staticCompositionLocalOf { Light }

private fun variableFont(resId: Int, weight: FontWeight) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val Fraunces = FontFamily(
    variableFont(R.font.fraunces_variable, FontWeight.Normal),
    variableFont(R.font.fraunces_variable, FontWeight.SemiBold),
    variableFont(R.font.fraunces_variable, FontWeight.Bold),
    variableFont(R.font.fraunces_variable, FontWeight.ExtraBold)
)

val Jakarta = FontFamily(
    variableFont(R.font.plus_jakarta_sans_variable, FontWeight.Normal),
    variableFont(R.font.plus_jakarta_sans_variable, FontWeight.Medium),
    variableFont(R.font.plus_jakarta_sans_variable, FontWeight.SemiBold),
    variableFont(R.font.plus_jakarta_sans_variable, FontWeight.Bold)
)

val Caveat = FontFamily(variableFont(R.font.caveat_variable, FontWeight.SemiBold))

private val LembarTypography = Typography(
    displaySmall = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    labelSmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.2.sp)
)

@Composable
fun LembarTheme(content: @Composable () -> Unit) {
    val c = if (isSystemInDarkTheme()) Dark else Light
    val dark = c == Dark
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.leaf, onPrimary = Color.White, secondary = c.sun, onSecondary = c.onSun,
            background = c.bg, onBackground = c.ink, surface = c.surface, onSurface = c.ink,
            surfaceVariant = c.card, onSurfaceVariant = c.muted, outline = c.line, error = c.danger
        )
    } else {
        lightColorScheme(
            primary = c.leaf, onPrimary = Color.White, secondary = c.sun, onSecondary = c.onSun,
            background = c.bg, onBackground = c.ink, surface = c.surface, onSurface = c.ink,
            surfaceVariant = c.card, onSurfaceVariant = c.muted, outline = c.line, error = c.danger
        )
    }
    CompositionLocalProvider(LocalLembarColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = LembarTypography, content = content)
    }
}

/** Akses singkat ke palet: `Lembar.colors.leaf`. */
object Lembar {
    val colors: LembarColors
        @Composable get() = LocalLembarColors.current
}
