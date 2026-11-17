package vn.nhom2.lokap.feature.post

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import java.io.File
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.nhom2.lokap.core.common.formatTimeAgo
import vn.nhom2.lokap.core.designsystem.component.AvatarRing
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.Post

/** Emoji thả khi nhấn đúp vào ảnh. */
private const val DOUBLE_TAP_EMOJI = "😍"

/** Nút đăng lại dưới ảnh công khai; [enabled] = false: chỉ xem số lượt (ảnh của mình, đang hẹn giờ). */
data class RepostToggle(
    val isReposted: Boolean,
    val count: Int,
    val enabled: Boolean,
    val onToggle: () -> Unit
)

/**
 * Thẻ bài: tác giả, vị trí, chú thích, ảnh, hàng hành động (số cảm xúc → bảng chọn emoji, bình luận → popup).
 * Feed Khám phá và trang ảnh (mở từ Bản đồ, Hồ sơ, Album, Địa danh, Thông báo, Chat) dùng chung; không mở màn nào khác.
 */
@Composable
fun PostCard(
    post: Post,
    now: Instant,
    onReact: (String) -> Unit,
    onAuthorClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** null = ẩn nút bình luận. */
    commentsCount: Int? = null,
    onOpenComments: () -> Unit = {},
    onLocationClick: (() -> Unit)? = null,
    repost: RepostToggle? = null,
    headerTrailing: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val isPreview = LocalInspectionMode.current
    val scope = rememberCoroutineScope()
    var showHeart by remember { mutableStateOf(false) }
    val heartScale by animateFloatAsState(
        targetValue = if (showHeart) 1.25f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "doubleTapHeart"
    )
    val timeLabel = remember(post.takenAt, now) { formatTimeAgo(post.takenAt, now) }
    val imageModel: Any = remember(post.imageUrl) {
        if (post.imageUrl.startsWith("/")) File(post.imageUrl) else post.imageUrl
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(LokapTheme.shapes.card)
            .background(colors.surface)
            .border(1.dp, colors.outline, LokapTheme.shapes.card)
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            AvatarRing(
                avatarUrl = post.author.avatarUrl,
                displayName = post.author.displayName,
                size = 40.dp,
                onClick = onAuthorClick
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = post.author.displayName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clickable(role = Role.Button, onClickLabel = "Xem hồ sơ", onClick = onAuthorClick)
                    )
                    Text(
                        text = " · $timeLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                val locText = listOfNotNull(post.location.placeName ?: post.location.wardName, post.location.provinceName)
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString(" · ")
                if (locText.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .then(
                                if (onLocationClick != null) {
                                    Modifier.clickable(role = Role.Button, onClickLabel = "Mở album $locText", onClick = onLocationClick)
                                } else Modifier
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = locText,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            headerTrailing?.invoke()
        }

        if (!post.caption.isNullOrBlank()) {
            Text(
                text = post.caption,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary
            )
        }

        if (post.imageUrl.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceHigh)
                    .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
                    .semantics { contentDescription = post.caption ?: "Ảnh của ${post.author.displayName}" }
                    .pointerInput(post.id, post.myReaction) {
                        detectTapGestures(
                            onDoubleTap = {
                                // Nhấn đúp chỉ thả, không bỏ cảm xúc đang có
                                if (post.myReaction == null) onReact(DOUBLE_TAP_EMOJI)
                                showHeart = true
                                scope.launch {
                                    delay(650)
                                    showHeart = false
                                }
                            }
                        )
                    }
            ) {
                if (!isPreview) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                ImageChip(
                    icon = if (post.audience == Audience.PUBLIC) Icons.Filled.Public else Icons.Filled.People,
                    label = if (post.audience == Audience.PUBLIC) "Công khai" else "Bạn bè",
                    background = colors.primary,
                    content = colors.buttonText,
                    modifier = Modifier.align(Alignment.TopStart)
                )
                ImageChip(
                    icon = Icons.Filled.Schedule,
                    label = timeLabel,
                    background = Color.Black.copy(alpha = 0.65f),
                    content = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
                if (heartScale > 0.05f) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(72.dp)
                            .scale(heartScale)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ReactionCountButton(reactions = post.reactions, myReaction = post.myReaction, onReact = onReact)
            if (commentsCount != null) {
                CommentCountButton(count = commentsCount, onClick = onOpenComments)
            }
            if (repost != null) {
                RepostButton(
                    count = repost.count,
                    isReposted = repost.isReposted,
                    enabled = repost.enabled,
                    onClick = repost.onToggle
                )
            }
        }

        footer?.invoke()
    }
}

@Composable
private fun ImageChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    background: Color,
    content: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = content, modifier = Modifier.size(15.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = content,
            fontSize = 12.sp
        )
    }
}
