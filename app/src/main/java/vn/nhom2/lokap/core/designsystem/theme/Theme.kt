package vn.nhom2.lokap.core.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
private val LokapDarkColorScheme = darkColorScheme(
    primary = SoftPinkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = SoftPinkPrimarySoft,
    onPrimaryContainer = DarkOnPrimary,
    secondary = SoftPinkPrimary,
    onSecondary = DarkOnPrimary,
    tertiary = DarkTextSecondary,
    onTertiary = DarkOnPrimary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    error = DarkError,
    onError = DarkOnPrimary
)

private val LokapLightColorScheme = lightColorScheme(
    primary = SoftPinkPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimarySoft,
    onPrimaryContainer = LightTextPrimary,
    secondary = SoftPinkPrimary,
    onSecondary = LightOnPrimary,
    tertiary = LightTextSecondary,
    onTertiary = LightOnPrimary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    error = LightError,
    onError = LightOnPrimary
)

object LokapTheme {
    val colors: LokapColors
        @Composable
        @ReadOnlyComposable
        get() = LocalLokapColors.current

    val spacing: Spacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacing.current

    val shapes: LokapShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalLokapShapes.current

    val themeMode: ThemeMode
        @Composable
        @ReadOnlyComposable
        get() = LocalThemeMode.current
}

@Composable
fun LokapTheme(
    themeMode: ThemeMode = LocalThemeMode.current,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemDark
    }

    val lokapColors = if (isDark) DarkLokapColors() else LightLokapColors()
    val colorScheme = if (isDark) LokapDarkColorScheme else LokapLightColorScheme
    val spacing = Spacing()
    val lokapShapes = LokapShapes()

    CompositionLocalProvider(
        LocalLokapColors provides lokapColors,
        LocalThemeMode provides themeMode,
        LocalSpacing provides spacing,
        LocalLokapShapes provides lokapShapes
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LokapTypography,
            shapes = MaterialLokapShapes,
            content = content
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Preview(name = "DesignSystemPreview - Dark", showBackground = true, backgroundColor = 0xFF121214, widthDp = 420, heightDp = 920)
@Composable
fun DesignSystemPreviewDark() {
    LokapTheme(themeMode = ThemeMode.DARK) {
        DesignSystemContent()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Preview(name = "DesignSystemPreview - Light", showBackground = true, backgroundColor = 0xFFF8F9FA, widthDp = 420, heightDp = 920)
@Composable
fun DesignSystemPreviewLight() {
    LokapTheme(themeMode = ThemeMode.LIGHT) {
        DesignSystemContent()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DesignSystemContent() {
    val c = LokapTheme.colors
    val s = LokapTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
            .padding(s.lg),
        verticalArrangement = Arrangement.spacedBy(s.lg)
    ) {
        Text(
            text = "LOKAP MONOCHROME + SOFT PINK",
            style = MaterialTheme.typography.labelLarge,
            color = c.primary
        )

        val swatches = listOf(
            "background" to c.background,
            "surface" to c.surface,
            "surfaceHigh" to c.surfaceHigh,
            "outline" to c.outline,
            "primary" to c.primary,
            "primarySoft" to c.primarySoft,
            "textPrimary" to c.textPrimary,
            "textSecondary" to c.textSecondary,
            "error" to c.error,
            "success" to c.success
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(s.sm),
            verticalArrangement = Arrangement.spacedBy(s.sm)
        ) {
            swatches.forEach { (name, color) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(s.xs)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(LokapTheme.shapes.pill)
                            .background(color)
                            .border(1.dp, c.outline, LokapTheme.shapes.pill)
                    )
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textSecondary
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .clip(LokapTheme.shapes.pill)
                .background(c.primary)
                .padding(horizontal = s.lg, vertical = s.sm)
        ) {
            Text(
                text = "Nút Hồng Nhẹ",
                style = MaterialTheme.typography.labelLarge,
                color = c.onPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(LokapTheme.shapes.card)
                .background(c.surface)
                .padding(s.lg),
            verticalArrangement = Arrangement.spacedBy(s.sm)
        ) {
            Text(
                text = "Khoảnh khắc Hội An",
                style = MaterialTheme.typography.displayLarge,
                color = c.textPrimary
            )
            Text(
                text = "Bản đồ khám phá 34 tỉnh thành",
                style = MaterialTheme.typography.titleLarge,
                color = c.textPrimary
            )
            Text(
                text = "Chia sẻ ảnh tức thì cùng bạn bè hoặc khám phá theo từng khu vực.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.textSecondary
            )
        }
    }
}
