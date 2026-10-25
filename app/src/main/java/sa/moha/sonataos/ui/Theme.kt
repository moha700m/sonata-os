package sa.moha.sonataos.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import sa.moha.sonataos.R

val Plex = FontFamily(
    Font(R.font.plex_arabic_light, FontWeight.Light),
    Font(R.font.plex_arabic_regular, FontWeight.Normal),
    Font(R.font.plex_arabic_medium, FontWeight.Medium),
    Font(R.font.plex_arabic_semibold, FontWeight.SemiBold),
    Font(R.font.plex_arabic_bold, FontWeight.Bold),
)

/** Palette: "Night Highway" — deep asphalt blues, Hyundai-ish blue accent, amber for warnings only. */
@Immutable
data class SonataPalette(
    val bg: Color, val surface: Color, val surfaceHi: Color, val stroke: Color,
    val text: Color, val textDim: Color, val accent: Color, val warn: Color, val danger: Color, val ok: Color, val dark: Boolean,
)

fun darkPalette(accent: Color) = SonataPalette(
    bg = Color(0xFF070B12), surface = Color(0xFF0F1622), surfaceHi = Color(0xFF172133), stroke = Color(0xFF223047),
    text = Color(0xFFE8EEF7), textDim = Color(0xFF8A9AB2), accent = accent, warn = Color(0xFFF5A524),
    danger = Color(0xFFF04452), ok = Color(0xFF2BD49A), dark = true,
)

fun lightPalette(accent: Color) = SonataPalette(
    bg = Color(0xFFE9EEF5), surface = Color(0xFFFFFFFF), surfaceHi = Color(0xFFF3F6FA), stroke = Color(0xFFD3DCE8),
    text = Color(0xFF0B1220), textDim = Color(0xFF52617A), accent = accent, warn = Color(0xFFB86E00),
    danger = Color(0xFFD0202F), ok = Color(0xFF0E8F63), dark = false,
)

val LocalPalette = staticCompositionLocalOf { darkPalette(Color(0xFF2F7DF6)) }
val LocalEastern = staticCompositionLocalOf { true }

object Accents {
    val all = listOf(0xFF2F7DF6, 0xFF00AAD2, 0xFF2BD49A, 0xFFF5A524, 0xFFE5484D, 0xFF9B7BFF)
    val names = listOf("أزرق هيونداي", "سماوي", "أخضر", "كهرماني", "أحمر", "بنفسجي")
}

@Composable
fun SonataTheme(dark: Boolean, accent: Long, fontScale: Float = 1f, eastern: Boolean = true, content: @Composable () -> Unit) {
    val p = if (dark) darkPalette(Color(accent)) else lightPalette(Color(accent))
    val base = TextStyle(fontFamily = Plex, color = p.text)
    val typo = Typography(
        displayLarge = base.copy(fontSize = 96.sp, fontWeight = FontWeight.Light, lineHeight = 110.sp),
        displayMedium = base.copy(fontSize = 64.sp, fontWeight = FontWeight.SemiBold, lineHeight = 76.sp),
        headlineMedium = base.copy(fontSize = 30.sp, fontWeight = FontWeight.SemiBold, lineHeight = 42.sp),
        titleLarge = base.copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp),
        titleMedium = base.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium, lineHeight = 28.sp),
        bodyLarge = base.copy(fontSize = 18.sp, lineHeight = 28.sp),
        bodyMedium = base.copy(fontSize = 16.sp, lineHeight = 24.sp),
        labelLarge = base.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp),
        labelMedium = base.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp),
    )
    val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = p.accent, onPrimary = Color.White, background = p.bg, surface = p.surface,
        onSurface = p.text, onBackground = p.text, surfaceVariant = p.surfaceHi, outline = p.stroke, error = p.danger,
    )
    val d = LocalDensity.current
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalPalette provides p,
        LocalEastern provides eastern,
        LocalDensity provides Density(d.density, d.fontScale * fontScale),
    ) {
        MaterialTheme(colorScheme = scheme, typography = typo, content = content)
    }
}
