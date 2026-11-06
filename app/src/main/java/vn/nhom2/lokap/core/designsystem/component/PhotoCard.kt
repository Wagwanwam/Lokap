package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import vn.nhom2.lokap.core.common.formatTimeAgo
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.designsystem.theme.regionGradient
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.model.PostLocation
import vn.nhom2.lokap.domain.model.PostSendState
import java.time.Instant

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

            // Lớp phủ gradient trên & dưới đậm hơn để chữ luôn nổi bật trên ảnh sáng
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to colors.background.copy(alpha = 0.82f),
                            0.32f to colors.background.copy(alpha = 0.0f),
                            0.65f to colors.background.copy(alpha = 0.0f),
                            1.0f to colors.background.copy(alpha = 0.85f)
                        )
                    )
            )

            // Thanh trên: Hàng tác giả (avatar + tên + thời gian có nền scrim) + chip vị trí + menu ⋮ căn giữa dọc
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                // Box chiếm phần còn lại để chip và nút ⋮ luôn sát mép phải
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        modifier = Modifier
                            .clip(shapes.pill)
                            .background(colors.background.copy(alpha = 0.72f))
                            .border(1.dp, colors.outline, shapes.pill)
                            .then(
                                if (onAuthorClick != null) Modifier.clickable(onClick = onAuthorClick)
                                else Modifier
                            )
                            .padding(start = spacing.xs, end = spacing.md, top = spacing.xs, bottom = spacing.xs)
                    ) {
                        AvatarRing(
                            avatarUrl = post.author.avatarUrl,
                            displayName = post.author.displayName,
                            size = 36.dp,
                            hasNewPost = false,
                            onClick = onAuthorClick
                        )
                        Column(verticalArrangement = Arrangement.Center) {
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
                                color = colors.textSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }

                GlassChip(
                    label = locationLabel,
                    leadingIcon = Icons.Default.LocationOn,
                    onClick = onLocationClick
                )

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(shapes.pill)
                            .background(colors.background.copy(alpha = 0.72f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Tùy chọn bài đăng",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(colors.surfaceHigh)
                    ) {
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Báo cáo",
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            text = {
                                Text(
                                    text = "Báo cáo",
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
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Chặn",
                                    tint = colors.error,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            text = {
                                Text(
                                    text = "Chặn",
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

            // Chú thích ảnh phía dưới căn giữa ngang
            if (!post.caption.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(spacing.md)
                        .clip(shapes.pill)
                        .background(colors.background.copy(alpha = 0.78f))
                        .border(1.dp, colors.outline, shapes.pill)
                        .padding(horizontal = spacing.lg, vertical = spacing.sm),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.caption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Trạng thái PENDING / FAILED bằng icon tối giản
            when (post.sendState) {
                PostSendState.PENDING -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.background.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(shapes.pill)
                                .background(colors.surfaceHigh)
                                .border(1.dp, colors.outline, shapes.pill),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(28.dp)
                                    .semantics { contentDescription = "Đang gửi" },
                                color = colors.primary,
                                strokeWidth = 2.5.dp
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
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Gửi ảnh không thành công",
                                tint = colors.error,
                                modifier = Modifier.size(32.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(shapes.pill)
                                    .background(colors.primary)
                                    .clickable(onClick = onRetrySend),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Thử lại",
                                    tint = colors.onPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                PostSendState.SENT -> Unit
            }
        }

        // Hàng thả cảm xúc bên dưới thẻ ảnh
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
