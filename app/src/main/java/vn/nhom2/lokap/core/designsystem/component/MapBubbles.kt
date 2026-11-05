package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.designsystem.theme.regionGradient
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.Landmark
import vn.nhom2.lokap.domain.model.MapBubble

private val BaseBubbleSize: Dp = 56.dp

@Composable
fun MapBubbleItem(
    bubble: MapBubble,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shape = LokapTheme.shapes.pill
    val isPreview = LocalInspectionMode.current
    val borderColor = if (bubble.hasFriendPosts) colors.accentCyan else colors.outline

    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .size(BaseBubbleSize)
                    .clip(shape)
                    .background(regionGradient(bubble.code ?: bubble.id))
                    .border(
                        width = if (bubble.hasFriendPosts) 2.5.dp else 1.5.dp,
                        color = borderColor,
                        shape = shape
                    )
            ) {
                if (!isPreview && !bubble.coverImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = bubble.coverImageUrl,
                        contentDescription = bubble.label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            BadgeDot(count = bubble.postCount)
        }

        Box(
            modifier = Modifier
                .clip(shape)
                .background(colors.surfaceHigh)
                .border(1.dp, colors.outline, shape)
                .padding(horizontal = spacing.sm, vertical = 2.dp)
        ) {
            Text(
                text = bubble.label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
    val shape = LokapTheme.shapes.pill
    val isPreview = LocalInspectionMode.current
    val landmarkSize = BaseBubbleSize * 1.3f

    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .size(landmarkSize)
                    .clip(shape)
                    .background(regionGradient(landmark.provinceCode))
                    .border(
                        width = 3.dp,
                        color = colors.accentYellow,
                        shape = shape
                    )
            ) {
                if (!isPreview) {
                    AsyncImage(
                        model = landmark.coverUrl,
                        contentDescription = landmark.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            BadgeDot(count = landmark.postCount)
        }

        Box(
            modifier = Modifier
                .clip(shape)
                .background(colors.accentYellow)
                .padding(horizontal = spacing.sm, vertical = 2.dp)
        ) {
            Text(
                text = landmark.name,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun MapBubblesPreview() {
    LokapTheme {
        Row(
            modifier = Modifier
                .background(LokapTheme.colors.background)
                .padding(LokapTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.xl),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MapBubbleItem(
                bubble = SampleData.provinceMapBubbles.first(),
                onClick = {}
            )
            LandmarkBubble(
                landmark = SampleData.landmarks.first(),
                onClick = {}
            )
        }
    }
}
