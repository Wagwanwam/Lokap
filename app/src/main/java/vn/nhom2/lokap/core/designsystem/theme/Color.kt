package vn.nhom2.lokap.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val BackgroundToken = Color(0xFF0B0B0D)
val SurfaceToken = Color(0xFF141417)
val SurfaceHighToken = Color(0xFF1E1E23)
val OutlineToken = Color(0xFF2C2C33)
val PrimaryToken = Color(0xFFFF4D8D)
val PrimarySoftToken = Color(0xFFFF8FB8)
val OnPrimaryToken = Color(0xFF0B0B0D)
val TextPrimaryToken = Color(0xFFF5F5F7)
val TextSecondaryToken = Color(0xFFA1A1AA)
val TextTertiaryToken = Color(0xFF71717A)
val AccentLilacToken = Color(0xFFB98CFF)
val AccentLimeToken = Color(0xFFC6FF4D)
val AccentCyanToken = Color(0xFF4DE1FF)
val AccentYellowToken = Color(0xFFFFD84D)
val ErrorToken = Color(0xFFFF6B6B)
val SuccessToken = Color(0xFF4ADE80)

@Immutable
data class LokapColors(
    val background: Color = BackgroundToken,
    val surface: Color = SurfaceToken,
    val surfaceHigh: Color = SurfaceHighToken,
    val outline: Color = OutlineToken,
    val primary: Color = PrimaryToken,
    val primarySoft: Color = PrimarySoftToken,
    val onPrimary: Color = OnPrimaryToken,
    val textPrimary: Color = TextPrimaryToken,
    val textSecondary: Color = TextSecondaryToken,
    val textTertiary: Color = TextTertiaryToken,
    val accentLilac: Color = AccentLilacToken,
    val accentLime: Color = AccentLimeToken,
    val accentCyan: Color = AccentCyanToken,
    val accentYellow: Color = AccentYellowToken,
    val error: Color = ErrorToken,
    val success: Color = SuccessToken
)

val LocalLokapColors = staticCompositionLocalOf { LokapColors() }
