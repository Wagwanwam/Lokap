package vn.nhom2.lokap.core.designsystem.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
private val DarkSubtleTonePairs: List<Pair<Color, Color>> = listOf(
    Color(0xFF26262D) to Color(0xFF1E1E23),
    Color(0xFF232328) to Color(0xFF2C2C33),
    Color(0xFF1E1E23) to Color(0xFF282830),
    Color(0xFF2A2A32) to Color(0xFF202025)
)

private val LightSubtleTonePairs: List<Pair<Color, Color>> = listOf(
    Color(0xFFFFFFFF) to Color(0xFFF4F4F5),
    Color(0xFFF8F9FA) to Color(0xFFEFEFEF),
    Color(0xFFFFFFFF) to Color(0xFFEAEAEA),
    Color(0xFFF5F5F7) to Color(0xFFE8E8EC)
)

fun regionGradient(provinceCode: String, isDark: Boolean = true): Brush {
    val normalized = provinceCode.trim().lowercase()
    var hash = 0x811c9dc5.toInt()
    for (ch in normalized) {
        hash = hash xor ch.code
        hash *= 0x01000193
    }
    val pairs = if (isDark) DarkSubtleTonePairs else LightSubtleTonePairs
    val index = (hash and Int.MAX_VALUE) % pairs.size
    val (startColor, endColor) = pairs[index]
    return Brush.linearGradient(
        colors = listOf(startColor, endColor),
        start = Offset.Zero,
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
}
