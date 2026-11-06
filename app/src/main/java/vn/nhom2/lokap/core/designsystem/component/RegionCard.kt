package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import vn.nhom2.lokap.data.fake.SampleData

@Composable
fun RegionCard(
    code: String,
    regionName: String,
    photoUrls: List<String>,
    postCount: Int,
    visited: Boolean,
    modifier: Modifier = Modifier,
    stickers: List<String> = listOf("✨", "📸"),
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
            .background(regionGradient(code))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(spacing.lg)
    ) {
        // Thẻ hẹp (Hộ chiếu vùng miền ở Hồ sơ, rộng 210dp): bỏ sticker và thu nhỏ chồng ảnh,
        // nếu không sticker đè lên badge "N khoảnh khắc" và chồng ảnh đè lên "ĐÃ ĐẾN".
        val compact = maxWidth < 280.dp

        // Tối đa 3 ảnh xếp chồng nghiêng ±6° kiểu Polaroid ở góc phải
        val polaroidAngles = listOf(-6f, 6f, 0f)
        val polaroidOffsets = listOf(
            (-18).dp to 8.dp,
            14.dp to (-6).dp,
            0.dp to 0.dp
        )
        // Chỉ vẽ đúng số ảnh đang có, ảnh đầu tiên nằm trên cùng.
        // Trước đây luôn vẽ 3 khung nên khu vực có 1–2 ảnh lộ khung trước trống (ô xám).
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
                val polaroidShape = RoundedCornerShape(14.dp)

                Box(
                    modifier = Modifier
                        .offset(x = offsetX, y = offsetY)
                        .graphicsLayer { rotationZ = angle }
                        .size(width = 104.dp, height = 118.dp)
                        .clip(polaroidShape)
                        .background(colors.textPrimary)
                        .padding(start = 6.dp, top = 6.dp, end = 6.dp, bottom = 18.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
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

        // Sticker Emoji trang trí (ẩn ở thẻ hẹp)
        if (!compact) Row(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            stickers.take(2).forEach { emoji ->
                Box(
                    modifier = Modifier
                        .clip(shapes.pill)
                        .background(colors.background.copy(alpha = 0.35f))
                        .padding(horizontal = spacing.sm, vertical = spacing.xs)
                ) {
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        // Cột thông tin bên trái
        Column(
            modifier = Modifier.align(Alignment.TopStart),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            // Badge số ảnh
            Box(
                modifier = Modifier
                    .clip(shapes.pill)
                    .background(colors.background.copy(alpha = 0.78f))
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
                        .border(1.5.dp, colors.onPrimary, shapes.pill)
                        .padding(horizontal = spacing.md, vertical = spacing.xs)
                ) {
                    Text(
                        text = "ĐÃ ĐẾN",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onPrimary
                    )
                }
            }
        }

        // Tên khu vực kiểu chữ Display ở góc dưới trái
        Text(
            text = regionName,
            style = if (compact) {
                MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp, lineHeight = 30.sp)
            } else {
                MaterialTheme.typography.displayLarge
            },
            color = colors.onPrimary,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun RegionCardPreview() {
    LokapTheme {
        Column(
            modifier = Modifier
                .background(LokapTheme.colors.background)
                .padding(LokapTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.md)
        ) {
            RegionCard(
                code = "da-nang",
                regionName = "Đà Nẵng",
                photoUrls = SampleData.posts.take(3).map { it.imageUrl },
                postCount = 512,
                visited = true,
                stickers = listOf("🌊", "🔥")
            )
            RegionCard(
                code = "hue",
                regionName = "Huế",
                photoUrls = SampleData.posts.drop(3).take(3).map { it.imageUrl },
                postCount = 248,
                visited = false,
                stickers = listOf("🏮", "✨")
            )
        }
    }
}
