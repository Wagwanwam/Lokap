package vn.nhom2.lokap.feature.chat

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
@Immutable
data class ChatThemePreset(
    val id: String,
    val nameVi: String,
    val isGradient: Boolean,
    val primaryColor: Color,
    val gradientColors: List<Color> = emptyList(),
    val bubbleTextColor: Color = Color.White
) {
    fun getBubbleBrush(): Brush {
        return if (isGradient && gradientColors.size >= 2) {
            Brush.linearGradient(gradientColors)
        } else {
            Brush.linearGradient(listOf(primaryColor, primaryColor))
        }
    }
}

@Immutable
data class ChatThemeConfig(
    val presetId: String = "LOKAP_PINK",
    val customWallpaperUri: String? = null,
    val wallpaperAlignment: String = "CENTER"
) {
    val activePreset: ChatThemePreset
        get() = ChatThemePresets.findById(presetId)
}

object ChatThemePresets {

    val SOLID_PRESETS = listOf(
        ChatThemePreset(
            id = "LOKAP_PINK",
            nameVi = "Hồng Lokap",
            isGradient = false,
            primaryColor = Color(0xFFFF2D78),
            bubbleTextColor = Color.White
        ),
        ChatThemePreset(
            id = "OCEAN_BLUE",
            nameVi = "Xanh Dương",
            isGradient = false,
            primaryColor = Color(0xFF007AFF),
            bubbleTextColor = Color.White
        ),
        ChatThemePreset(
            id = "EMERALD_GREEN",
            nameVi = "Ngọc Lục",
            isGradient = false,
            primaryColor = Color(0xFF10B981),
            bubbleTextColor = Color.White
        ),
        ChatThemePreset(
            id = "LAVENDER_PURPLE",
            nameVi = "Tím Mộng",
            isGradient = false,
            primaryColor = Color(0xFF8B5CF6),
            bubbleTextColor = Color.White
        ),
        ChatThemePreset(
            id = "SUNSET_CORAL",
            nameVi = "Cam San Hô",
            isGradient = false,
            primaryColor = Color(0xFFFF6433),
            bubbleTextColor = Color.White
        ),
        ChatThemePreset(
            id = "RUBY_RED",
            nameVi = "Đỏ Ruby",
            isGradient = false,
            primaryColor = Color(0xFFE11D48),
            bubbleTextColor = Color.White
        ),
        ChatThemePreset(
            id = "MIDNIGHT_BLACK",
            nameVi = "Đen Than",
            isGradient = false,
            primaryColor = Color(0xFF232731),
            bubbleTextColor = Color.White
        )
    )

    val GRADIENT_PRESETS = listOf(
        ChatThemePreset(
            id = "CYBERPUNK",
            nameVi = "Cyberpunk",
            isGradient = true,
            primaryColor = Color(0xFFFF007F),
            gradientColors = listOf(Color(0xFF00E5FF), Color(0xFFFF007F)),
            bubbleTextColor = Color.White
        ),
        ChatThemePreset(
            id = "SUNSET_GLOW",
            nameVi = "Hoàng Hôn",
            isGradient = true,
            primaryColor = Color(0xFFFF5E62),
            gradientColors = listOf(Color(0xFFFF9966), Color(0xFFFF5E62)),
            bubbleTextColor = Color.White
        ),
        ChatThemePreset(
            id = "AURORA",
            nameVi = "Cực Quang",
            isGradient = true,
            primaryColor = Color(0xFF00C9FF),
            gradientColors = listOf(Color(0xFF00C9FF), Color(0xFF92FE9D)),
            bubbleTextColor = Color(0xFF0F172A)
        ),
        ChatThemePreset(
            id = "PASTEL_DREAM",
            nameVi = "Mơ Màng",
            isGradient = true,
            primaryColor = Color(0xFFC471ED),
            gradientColors = listOf(Color(0xFFC471ED), Color(0xFFF64F59)),
            bubbleTextColor = Color.White
        ),
        ChatThemePreset(
            id = "GOLDEN_HOUR",
            nameVi = "Nắng Vàng",
            isGradient = true,
            primaryColor = Color(0xFFF5AF19),
            gradientColors = listOf(Color(0xFFF5AF19), Color(0xFFE65C00)),
            bubbleTextColor = Color.White
        ),
        ChatThemePreset(
            id = "GALAXY_NIGHT",
            nameVi = "Ngân Hà",
            isGradient = true,
            primaryColor = Color(0xFF3A1C71),
            gradientColors = listOf(Color(0xFF3A1C71), Color(0xFFD76D77)),
            bubbleTextColor = Color.White
        ),
        ChatThemePreset(
            id = "MATCHA_ZEN",
            nameVi = "Trà Xanh",
            isGradient = true,
            primaryColor = Color(0xFF11998E),
            gradientColors = listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
            bubbleTextColor = Color.White
        ),
        ChatThemePreset(
            id = "SAKURA_SPRING",
            nameVi = "Anh Đào",
            isGradient = true,
            primaryColor = Color(0xFFFF758C),
            gradientColors = listOf(Color(0xFFFF758C), Color(0xFFFF7EB3)),
            bubbleTextColor = Color.White
        )
    )

    val ALL_PRESETS: List<ChatThemePreset> = SOLID_PRESETS + GRADIENT_PRESETS

    fun findById(id: String): ChatThemePreset {
        return ALL_PRESETS.firstOrNull { it.id == id } ?: SOLID_PRESETS.first()
    }
}
