package vn.nhom2.lokap.core.designsystem.theme

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
val DarkBackground = Color(0xFF121214)
val DarkSurface = Color(0xFF1C1C21)
val DarkSurfaceHigh = Color(0xFF26262D)
val DarkOutline = Color(0xFF2D2D36)
val SoftPinkPrimary = Color(0xFFF472B6)
val SoftPinkPrimarySoft = Color(0xFFFBCFE8)
val DarkOnPrimary = Color(0xFF18181B)
val DarkTextPrimary = Color(0xFFF4F4F5)
val DarkTextSecondary = Color(0xFF9CA3AF)
val DarkTextTertiary = Color(0xFF6B7280)
val DarkSuccess = Color(0xFF34D399)
val DarkError = Color(0xFFF87171)

val LightBackground = Color(0xFFF8F9FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceHigh = Color(0xFFF1F3F5)
val LightOutline = Color(0xFFE4E4E7)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimarySoft = Color(0xFFFDF2F8)
val LightTextPrimary = Color(0xFF18181B)
val LightTextSecondary = Color(0xFF6B7280)
val LightTextTertiary = Color(0xFF9CA3AF)
val LightSuccess = Color(0xFF10B981)
val LightError = Color(0xFFEF4444)

val BackgroundToken = DarkBackground
val SurfaceToken = DarkSurface
val SurfaceHighToken = DarkSurfaceHigh
val OutlineToken = DarkOutline
val PrimaryToken = SoftPinkPrimary
val PrimarySoftToken = SoftPinkPrimarySoft
val OnPrimaryToken = DarkOnPrimary
val TextPrimaryToken = DarkTextPrimary
val TextSecondaryToken = DarkTextSecondary
val TextTertiaryToken = DarkTextTertiary
val AccentLilacToken = Color(0xFFD8B4FE)
val AccentLimeToken = Color(0xFF86EFAC)
val AccentCyanToken = Color(0xFF7DD3FC)
val AccentYellowToken = Color(0xFFFDE047)
val ErrorToken = DarkError
val SuccessToken = DarkSuccess

@Immutable
data class LokapColors(
    val background: Color = DarkBackground,
    val surface: Color = DarkSurface,
    val surfaceHigh: Color = DarkSurfaceHigh,
    val outline: Color = DarkOutline,
    val primary: Color = SoftPinkPrimary,
    val primarySoft: Color = SoftPinkPrimarySoft,
    val onPrimary: Color = DarkOnPrimary,
    val isDark: Boolean = true,
    val textPrimary: Color = DarkTextPrimary,
    val textSecondary: Color = DarkTextSecondary,
    val textTertiary: Color = DarkTextTertiary,
    val accentLilac: Color = AccentLilacToken,
    val accentLime: Color = AccentLimeToken,
    val accentCyan: Color = AccentCyanToken,
    val accentYellow: Color = AccentYellowToken,
    val error: Color = DarkError,
    val success: Color = DarkSuccess
) {
    val buttonText: Color get() = onPrimary
}

fun DarkLokapColors(): LokapColors = LokapColors(
    background = DarkBackground,
    surface = DarkSurface,
    surfaceHigh = DarkSurfaceHigh,
    outline = DarkOutline,
    primary = SoftPinkPrimary,
    primarySoft = SoftPinkPrimarySoft,
    onPrimary = DarkOnPrimary,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textTertiary = DarkTextTertiary,
    accentLilac = Color(0xFFD8B4FE),
    accentLime = Color(0xFF86EFAC),
    accentCyan = Color(0xFF7DD3FC),
    accentYellow = Color(0xFFFDE047),
    error = DarkError,
    success = DarkSuccess
)

fun LightLokapColors(): LokapColors = LokapColors(
    background = LightBackground,
    surface = LightSurface,
    surfaceHigh = LightSurfaceHigh,
    outline = LightOutline,
    primary = SoftPinkPrimary,
    primarySoft = LightPrimarySoft,
    onPrimary = Color(0xFF18181B),
    isDark = false,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textTertiary = LightTextTertiary,
    accentLilac = Color(0xFFC084FC),
    accentLime = Color(0xFF4ADE80),
    accentCyan = Color(0xFF38BDF8),
    accentYellow = Color(0xFFFACC15),
    error = LightError,
    success = LightSuccess
)

val LocalLokapColors = staticCompositionLocalOf { DarkLokapColors() }
