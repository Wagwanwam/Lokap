package vn.nhom2.lokap.core.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private val LokapDarkColorScheme = darkColorScheme(
    primary = PrimaryToken,
    onPrimary = OnPrimaryToken,
    primaryContainer = PrimarySoftToken,
    onPrimaryContainer = OnPrimaryToken,
    secondary = AccentLilacToken,
    onSecondary = OnPrimaryToken,
    tertiary = AccentCyanToken,
    onTertiary = OnPrimaryToken,
    background = BackgroundToken,
    onBackground = TextPrimaryToken,
    surface = SurfaceToken,
    onSurface = TextPrimaryToken,
    surfaceVariant = SurfaceHighToken,
    onSurfaceVariant = TextSecondaryToken,
    outline = OutlineToken,
    error = ErrorToken,
    onError = OnPrimaryToken
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
}

@Composable
fun LokapTheme(
    content: @Composable () -> Unit
) {
    val lokapColors = LokapColors()
    val spacing = Spacing()
    val lokapShapes = LokapShapes()

    CompositionLocalProvider(
        LocalLokapColors provides lokapColors,
        LocalSpacing provides spacing,
        LocalLokapShapes provides lokapShapes
    ) {
        MaterialTheme(
            colorScheme = LokapDarkColorScheme,
            typography = LokapTypography,
            shapes = MaterialLokapShapes,
            content = content
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Preview(name = "DesignSystemPreview", showBackground = true, backgroundColor = 0xFF0B0B0D, widthDp = 420, heightDp = 920)
@Composable
fun DesignSystemPreview() {
    LokapTheme {
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
                text = "LOKAP DESIGN SYSTEM",
                style = MaterialTheme.typography.labelLarge,
                color = c.primary
            )

            // 1. Bảng màu (Color Tokens)
            val swatches = listOf(
                "background" to c.background,
                "surface" to c.surface,
                "surfaceHigh" to c.surfaceHigh,
                "outline" to c.outline,
                "primary" to c.primary,
                "primarySoft" to c.primarySoft,
                "onPrimary" to c.onPrimary,
                "textPrimary" to c.textPrimary,
                "textSecondary" to c.textSecondary,
                "textTertiary" to c.textTertiary,
                "accentLilac" to c.accentLilac,
                "accentLime" to c.accentLime,
                "accentCyan" to c.accentCyan,
                "accentYellow" to c.accentYellow,
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

            // Nút mẫu kiểm tra onPrimary chữ ĐEN trên nền hồng
            Box(
                modifier = Modifier
                    .clip(LokapTheme.shapes.pill)
                    .background(c.primary)
                    .padding(horizontal = s.lg, vertical = s.sm)
            ) {
                Text(
                    text = "Nút Primary (chữ đen #0B0B0D)",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.onPrimary
                )
            }

            // 2. Thang chữ (Typography Scale)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(LokapTheme.shapes.card)
                    .background(c.surface)
                    .padding(s.lg),
                verticalArrangement = Arrangement.spacedBy(s.sm)
            ) {
                Text(
                    text = "Chụp khoảnh khắc ở Hội An",
                    style = MaterialTheme.typography.displayLarge,
                    color = c.textPrimary
                )
                Text(
                    text = "Title 20/28 — Bản đồ khám phá 34 tỉnh thành",
                    style = MaterialTheme.typography.titleLarge,
                    color = c.textPrimary
                )
                Text(
                    text = "Body 15/22 — Chia sẻ ảnh tức thì cùng hội bạn thân hoặc ghim lên bản đồ công khai theo từng phường, xã.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.textSecondary
                )
                Text(
                    text = "Label 12/16 — chụp 3 ngày trước · Phường Hội An, Đà Nẵng",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.textTertiary
                )
            }

            // 3. Ba ô Gradient cố định theo mã tỉnh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(s.md)
            ) {
                listOf("da-nang" to "Đà Nẵng", "ha-noi" to "Hà Nội", "hue" to "Huế").forEach { (code, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(84.dp)
                            .clip(LokapTheme.shapes.card)
                            .background(regionGradient(code))
                            .padding(s.md),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleSmall,
                            color = c.onPrimary
                        )
                    }
                }
            }
        }
    }
}
