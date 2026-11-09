package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import coil3.compose.AsyncImage
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.data.fake.SampleData
@Composable
fun AvatarRing(
    avatarUrl: String?,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    hasNewPost: Boolean = false,
    isOnline: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val colors = LokapTheme.colors
    val shape = CircleShape
    val isPreview = LocalInspectionMode.current
    var imageFailed by remember(avatarUrl) { mutableStateOf(false) }

    val initial = remember(displayName) {
        displayName.trim()
            .split(" ")
            .lastOrNull()
            ?.firstOrNull()
            ?.uppercaseChar()
            ?.toString() ?: "L"
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .border(
                    width = if (hasNewPost) 2.5.dp else 1.dp,
                    color = if (hasNewPost) colors.primary else colors.outline,
                    shape = shape
                )
                .then(
                    if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
                )
                .padding(if (hasNewPost) 3.dp else 1.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(colors.surfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.primary
                )
                if (!isPreview && !avatarUrl.isNullOrBlank() && !imageFailed) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = displayName,
                        contentScale = ContentScale.Crop,
                        onError = { imageFailed = true },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (isOnline) {
            OnlineIndicatorDot(
                isOnline = true,
                size = (size.value * 0.28f).coerceIn(10f, 15f).dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 1.dp, bottom = 1.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun AvatarRingPreview() {
    LokapTheme {
        Row(
            modifier = Modifier
                .background(LokapTheme.colors.background)
                .padding(LokapTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.md)
        ) {
            val user1 = SampleData.users[0]
            val user2 = SampleData.users[1]
            AvatarRing(
                avatarUrl = user1.avatarUrl,
                displayName = user1.displayName,
                hasNewPost = true,
                onClick = {}
            )
            AvatarRing(
                avatarUrl = null,
                displayName = user2.displayName,
                hasNewPost = false
            )
        }
    }
}
