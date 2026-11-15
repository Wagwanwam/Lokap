package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.designsystem.theme.regionGradient
@Composable
fun RegionCard(
    code: String,
    regionName: String,
    photoUrls: List<String>,
    postCount: Int,
    visited: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes
    val isPreview = LocalInspectionMode.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(shapes.card)
            .background(regionGradient(code, colors.isDark))
            .border(1.dp, colors.outline, shapes.card)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(spacing.lg)
    ) {
        // Thẻ hẹp (Hộ chiếu vùng miền ở Hồ sơ, rộng ~210dp): thu nhỏ chồng ảnh và tên,
        // nếu không chồng ảnh đè lên "ĐÃ ĐẾN" và tên khu vực.
        val compact = maxWidth < 280.dp

        val polaroidAngles = listOf(-6f, 6f, 0f)
        val polaroidOffsets = listOf(
            (-18).dp to 8.dp,
            14.dp to (-6).dp,
            0.dp to 0.dp
        )
        // Chỉ vẽ đúng số ảnh đang có, ảnh đầu tiên nằm trên cùng.
        // Luôn vẽ 3 khung thì khu vực có 1–2 ảnh lộ khung trước trống (ô xám).
        val stack: List<String?> = photoUrls.take(3).ifEmpty { listOf(null) }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = spacing.sm)
                .graphicsLayer {
                    if (compact) {
                        scaleX = 0.72f
                        scaleY = 0.72f
                        transformOrigin = TransformOrigin(1f, 0.5f)
                    }
                }
        ) {
            // Vẽ từ sau ra trước; slot 2 là khung trước (thẳng, không lệch).
            for (depth in stack.indices.reversed()) {
                val url = stack[depth]
                val slot = 2 - depth
                val angle = polaroidAngles[slot]
                val (offsetX, offsetY) = polaroidOffsets[slot]
                val polaroidShape = RoundedCornerShape(10.dp)

                Box(
                    modifier = Modifier
                        .offset(x = offsetX, y = offsetY)
                        .graphicsLayer { rotationZ = angle }
                        .size(width = 104.dp, height = 118.dp)
                        .clip(polaroidShape)
                        .background(androidx.compose.ui.graphics.Color.White)
                        .border(0.5.dp, androidx.compose.ui.graphics.Color(0x33000000), polaroidShape)
                        .padding(start = 5.dp, top = 5.dp, end = 5.dp, bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.surfaceHigh)
                    ) {
                        if (!isPreview && url != null) {
                            AsyncImage(
                                model = url,
                                contentDescription = regionName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.TopStart),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {

            Box(
                modifier = Modifier
                    .clip(shapes.pill)
                    .background(colors.surface.copy(alpha = 0.9f))
                    .border(0.5.dp, colors.outline, shapes.pill)
                    .padding(horizontal = spacing.md, vertical = spacing.xs)
            ) {
                Text(
                    text = "$postCount khoảnh khắc",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary
                )
            }

            if (visited) {
                Box(
                    modifier = Modifier
                        .graphicsLayer { rotationZ = -4f }
                        .clip(shapes.pill)
                        .background(colors.accentLime)
                        .border(1.dp, colors.outline, shapes.pill)
                        .padding(horizontal = spacing.md, vertical = spacing.xs)
                ) {
                    Text(
                        text = "ĐÃ ĐẾN",
                        style = MaterialTheme.typography.labelLarge,
                        color = androidx.compose.ui.graphics.Color(0xFF064E3B)
                    )
                }
            }
        }

        Text(
            text = regionName,
            style = if (compact) {
                MaterialTheme.typography.displayMedium.copy(fontSize = 24.sp, lineHeight = 30.sp)
            } else {
                MaterialTheme.typography.displayMedium
            },
            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
            color = colors.textPrimary,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}
