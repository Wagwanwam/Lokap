package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import java.time.Instant
import vn.nhom2.lokap.core.common.formatTimeAgo
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.designsystem.theme.regionGradient
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.*
@Composable
fun PhotoCard(
    post: Post,
    now: Instant,
    onReact: (String) -> Unit,
    onRetrySend: () -> Unit,
    onReport: () -> Unit,
    onBlockUser: () -> Unit,
    modifier: Modifier = Modifier,
    onAuthorClick: (() -> Unit)? = null,
    onLocationClick: (() -> Unit)? = null
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes
    val isPreview = LocalInspectionMode.current
    var menuExpanded by remember { mutableStateOf(false) }

    val isDelayedShot = post.audience == Audience.PUBLIC &&
        post.publicVisibleAt != null &&
        post.publicVisibleAt.isAfter(now)

    val timeLabel = remember(post.takenAt, now, isDelayedShot) {
        formatTimeAgo(
            instant = post.takenAt,
            now = now,
            isDelayedShot = isDelayedShot
        )
    }

    val locationLabel = remember(post.location) {
        formatPostLocationLabel(post.location)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shapes.card)
                .background(colors.surface)
                .border(1.dp, colors.outline, shapes.card)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(regionGradient(post.location.provinceCode ?: post.id))
            )

            if (!isPreview) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = post.caption ?: post.author.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to colors.background.copy(alpha = 0.65f),
                            0.28f to colors.background.copy(alpha = 0.0f),
                            0.68f to colors.background.copy(alpha = 0.0f),
                            1.0f to colors.background.copy(alpha = 0.80f)
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    modifier = Modifier.weight(1f)
                ) {
                    AvatarRing(
                        avatarUrl = post.author.avatarUrl,
                        displayName = post.author.displayName,
                        size = 40.dp,
                        hasNewPost = false,
                        onClick = onAuthorClick
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = post.author.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }

                GlassChip(
                    label = locationLabel,
                    leadingIcon = Icons.Rounded.LocationOn,
                    onClick = onLocationClick
                )

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Tùy chọn bài đăng",
                            tint = colors.textPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(colors.surfaceHigh)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Báo cáo bài đăng",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textPrimary
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onReport()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Chặn ${post.author.displayName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.error
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onBlockUser()
                            }
                        )
                    }
                }
            }

            if (!post.caption.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(spacing.md)
                        .clip(shapes.pill)
                        .background(colors.background.copy(alpha = 0.72f))
                        .border(1.dp, colors.outline, shapes.pill)
                        .padding(horizontal = spacing.lg, vertical = spacing.sm)
                ) {
                    Text(
                        text = post.caption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            when (post.sendState) {
                PostSendState.PENDING -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.background.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(shapes.pill)
                                .background(colors.surfaceHigh)
                                .border(1.dp, colors.outline, shapes.pill)
                                .padding(horizontal = spacing.lg, vertical = spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = colors.primary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(spacing.sm))
                            Text(
                                text = "Đang gửi…",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.textPrimary
                            )
                        }
                    }
                }

                PostSendState.FAILED -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.background.copy(alpha = 0.75f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(spacing.md)
                        ) {
                            Text(
                                text = "Gửi ảnh không thành công",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.error
                            )
                            PinkButton(
                                text = "Thử lại",
                                leadingIcon = Icons.Rounded.Refresh,
                                onClick = onRetrySend
                            )
                        }
                    }
                }

                PostSendState.SENT -> Unit
            }
        }

        EmojiBar(
            reactions = post.reactions,
            selectedEmoji = post.myReaction,
            onSelectEmoji = onReact,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

private fun formatPostLocationLabel(location: PostLocation): String {
    return when (location.level) {
        LocationLevel.PLACE -> location.placeName ?: location.wardName ?: location.provinceName ?: "Việt Nam"
        LocationLevel.WARD -> location.wardName ?: location.provinceName ?: "Việt Nam"
        LocationLevel.PROVINCE -> location.provinceName ?: "Việt Nam"
        LocationLevel.COUNTRY -> "Việt Nam"
    }
}

@Preview(name = "PhotoCard - Đã gửi", showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun PhotoCardSentPreview() {
    LokapTheme {
        Box(
            modifier = Modifier
                .background(LokapTheme.colors.background)
                .padding(LokapTheme.spacing.lg)
        ) {
            PhotoCard(
                post = SampleData.posts[0],
                now = SampleData.NOW,
                onReact = {},
                onRetrySend = {},
                onReport = {},
                onBlockUser = {}
            )
        }
    }
}

@Preview(name = "PhotoCard - Gửi lỗi", showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun PhotoCardFailedPreview() {
    LokapTheme {
        Box(
            modifier = Modifier
                .background(LokapTheme.colors.background)
                .padding(LokapTheme.spacing.lg)
        ) {
            PhotoCard(
                post = SampleData.posts[6],
                now = SampleData.NOW,
                onReact = {},
                onRetrySend = {},
                onReport = {},
                onBlockUser = {}
            )
        }
    }
}
