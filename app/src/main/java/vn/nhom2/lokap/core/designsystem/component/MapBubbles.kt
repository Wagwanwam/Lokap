package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import coil3.compose.AsyncImage
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.*
private val CardWidth: Dp = 58.dp
private val CardHeight: Dp = 66.dp

@Composable
fun MapBubbleItem(
    bubble: MapBubble,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    zoom: Float = 6f,
    photos: List<String> = emptyList()
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    val stackCount = when {
        zoom >= 13f -> 1
        zoom >= 9f -> 2
        else -> 3
    }

    val resolvedPhotos = remember(bubble.id, photos) {
        val list = mutableListOf<String>()
        if (!bubble.coverImageUrl.isNullOrBlank()) {
            list.add(bubble.coverImageUrl)
        }
        photos.forEach { if (!list.contains(it) && it.isNotBlank()) list.add(it) }
        val samplePool = SampleData.posts.map { it.imageUrl }.filter { it.isNotBlank() }
        for (sample in samplePool) {
            if (list.size >= 3) break
            if (!list.contains(sample)) list.add(sample)
        }
        list
    }

    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier.size(width = CardWidth + 12.dp, height = CardHeight + 10.dp),
                contentAlignment = Alignment.Center
            ) {

                if (stackCount >= 3) {
                    val photo3 = resolvedPhotos.getOrNull(2) ?: resolvedPhotos.firstOrNull()
                    SquarePhotoCard(
                        imageUrl = photo3,
                        modifier = Modifier
                            .offset(x = (-6).dp, y = 3.dp)
                            .rotate(-8f)
                    )
                }

                if (stackCount >= 2) {
                    val photo2 = resolvedPhotos.getOrNull(1) ?: resolvedPhotos.firstOrNull()
                    SquarePhotoCard(
                        imageUrl = photo2,
                        modifier = Modifier
                            .offset(x = 5.dp, y = (-2).dp)
                            .rotate(7f)
                    )
                }

                val photo1 = resolvedPhotos.firstOrNull()
                SquarePhotoCard(
                    imageUrl = photo1,
                    modifier = Modifier.rotate(0f),
                    hasFriendPosts = bubble.hasFriendPosts
                )
            }

            BadgeDot(count = bubble.postCount)
        }

        Box(
            modifier = Modifier
                .clip(LokapTheme.shapes.pill)
                .background(colors.surface)
                .border(1.dp, if (bubble.hasFriendPosts) colors.primary else colors.outline, LokapTheme.shapes.pill)
                .padding(horizontal = spacing.sm, vertical = 3.dp)
        ) {
            Text(
                text = bubble.label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SquarePhotoCard(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    hasFriendPosts: Boolean = false
) {
    val isPreview = LocalInspectionMode.current
    val frameShape = RoundedCornerShape(6.dp)
    val colors = LokapTheme.colors

    Box(
        modifier = modifier
            .size(width = CardWidth, height = CardHeight)
            .shadow(4.dp, frameShape)
            .clip(frameShape)
            .background(Color.White)
            .border(
                width = if (hasFriendPosts) 2.dp else 1.dp,
                color = if (hasFriendPosts) colors.primary else Color(0xFFE2E8F0),
                shape = frameShape
            )
            .padding(top = 3.dp, start = 3.dp, end = 3.dp, bottom = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(4.dp))
                .background(colors.surfaceHigh)
        ) {
            if (!isPreview && !imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun LandmarkBubble(
    landmark: Landmark,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val isPreview = LocalInspectionMode.current
    val frameShape = RoundedCornerShape(8.dp)

    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        Box(
            modifier = Modifier
                .size(width = CardWidth + 4.dp, height = CardHeight + 4.dp)
                .shadow(6.dp, frameShape)
                .clip(frameShape)
                .background(Color.White)
                .border(2.dp, colors.primary, frameShape)
                .padding(top = 3.dp, start = 3.dp, end = 3.dp, bottom = 11.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(5.dp))
                    .background(colors.surfaceHigh)
            ) {
                if (!isPreview && !landmark.coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = landmark.coverUrl,
                        contentDescription = landmark.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .clip(LokapTheme.shapes.pill)
                .background(colors.surface)
                .border(1.dp, colors.primary, LokapTheme.shapes.pill)
                .padding(horizontal = spacing.sm, vertical = 3.dp)
        ) {
            Text(
                text = landmark.name,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(name = "1. MapBubble - Stack 3 ảnh (Zoom xa)", showBackground = true, backgroundColor = 0xFF121214)
@Composable
private fun MapBubbleStack3Preview() {
    LokapTheme {
        MapBubbleItem(
            bubble = MapBubble(
                id = "bubble_dn",
                type = vn.nhom2.lokap.domain.model.MapBubbleType.PROVINCE,
                label = "Đà Nẵng",
                code = "48",
                lat = 16.0544,
                lng = 108.2022,
                postCount = 42,
                coverImageUrl = null,
                hasFriendPosts = true
            ),
            zoom = 6f,
            onClick = {}
        )
    }
}

@Preview(name = "2. MapBubble - Stack 1 ảnh (Zoom gần)", showBackground = true, backgroundColor = 0xFFF8F9FA)
@Composable
private fun MapBubbleStack1Preview() {
    LokapTheme {
        MapBubbleItem(
            bubble = MapBubble(
                id = "bubble_hn",
                type = vn.nhom2.lokap.domain.model.MapBubbleType.WARD,
                label = "Hoàn Kiếm",
                code = "01",
                lat = 21.0285,
                lng = 105.8542,
                postCount = 15,
                coverImageUrl = null,
                hasFriendPosts = false
            ),
            zoom = 14f,
            onClick = {}
        )
    }
}
