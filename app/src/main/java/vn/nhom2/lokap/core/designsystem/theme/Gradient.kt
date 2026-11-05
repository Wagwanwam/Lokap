package vn.nhom2.lokap.core.designsystem.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val RegionGradientPairs: List<Pair<Color, Color>> = listOf(
    PrimaryToken to AccentLilacToken,
    PrimaryToken to AccentYellowToken,
    PrimarySoftToken to AccentCyanToken,
    AccentLilacToken to AccentCyanToken,
    AccentCyanToken to AccentLimeToken,
    AccentLimeToken to AccentYellowToken,
    PrimaryToken to PrimarySoftToken,
    AccentLilacToken to PrimarySoftToken
)

/**
 * Trả về gradient 2 màu cố định theo mã tỉnh (cùng mã luôn cùng màu).
 * Dùng thuật toán băm FNV-1a 32-bit ổn định, không dùng Random.
 */
fun regionGradient(provinceCode: String): Brush {
    val normalized = provinceCode.trim().lowercase()
    var hash = 0x811c9dc5.toInt()
    for (ch in normalized) {
        hash = hash xor ch.code
        hash *= 0x01000193
    }
    val index = (hash and Int.MAX_VALUE) % RegionGradientPairs.size
    val (startColor, endColor) = RegionGradientPairs[index]
    return Brush.linearGradient(
        colors = listOf(startColor, endColor),
        start = Offset.Zero,
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
}
