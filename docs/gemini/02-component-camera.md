```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/core/designsystem/component/ShutterButton.kt
package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme

@Composable
fun ShutterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = LokapTheme.colors
    val shapes = LokapTheme.shapes
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.86f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "shutter_scale"
    )

    Box(
        modifier = modifier
            .size(80.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shapes.pill)
            .border(
                width = 4.dp,
                color = colors.textPrimary,
                shape = shapes.pill
            )
            .semantics {
                role = Role.Button
                contentDescription = "Chụp ảnh"
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val released = tryAwaitRelease()
                        isPressed = false
                        if (released) {
                            onClick()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(shapes.pill)
                .background(if (enabled) colors.primary else colors.outline)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun ShutterButtonPreview() {
    LokapTheme {
        Row(
            modifier = Modifier
                .background(LokapTheme.colors.background)
                .padding(LokapTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.xl)
        ) {
            ShutterButton(onClick = {}, enabled = true)
            ShutterButton(onClick = {}, enabled = false)
        }
    }
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/core/designsystem/component/Buttons.kt
package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme

@Composable
fun PinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Button(
        onClick = { if (!loading) onClick() },
        enabled = enabled && !loading,
        shape = LokapTheme.shapes.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            disabledContainerColor = colors.surfaceHigh,
            disabledContentColor = colors.textTertiary
        ),
        contentPadding = PaddingValues(horizontal = spacing.xl, vertical = spacing.md),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = colors.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(spacing.sm))
        } else if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(spacing.sm))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    OutlinedButton(
        onClick = { if (!loading) onClick() },
        enabled = enabled && !loading,
        shape = LokapTheme.shapes.pill,
        border = BorderStroke(1.dp, colors.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surface,
            contentColor = colors.textPrimary,
            disabledContainerColor = colors.background,
            disabledContentColor = colors.textTertiary
        ),
        contentPadding = PaddingValues(horizontal = spacing.xl, vertical = spacing.md),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = colors.primary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(spacing.sm))
        } else if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(spacing.sm))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun ButtonsPreview() {
    LokapTheme {
        Column(
            modifier = Modifier
                .background(LokapTheme.colors.background)
                .padding(LokapTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.md)
        ) {
            PinkButton(text = "Cho phép máy ảnh", onClick = {})
            PinkButton(text = "Đang gửi ảnh...", onClick = {}, loading = true)
            GhostButton(text = "Thử lại", onClick = {})
            GhostButton(text = "Đang tải...", onClick = {}, loading = true)
        }
    }
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/core/designsystem/component/GlassChip.kt
package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.data.fake.SampleData

@Composable
fun GlassChip(
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shape = LokapTheme.shapes.pill
    val borderColor = if (selected) colors.primary else colors.outline

    Row(
        modifier = modifier
            .clip(shape)
            .background(colors.surfaceHigh)
            .border(width = if (selected) 1.5.dp else 1.dp, color = borderColor, shape = shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (selected) colors.primary else colors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.primary else colors.textPrimary
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun GlassChipPreview() {
    LokapTheme {
        Row(
            modifier = Modifier
                .background(LokapTheme.colors.background)
                .padding(LokapTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
        ) {
            GlassChip(
                label = "Bạn bè · ${SampleData.users.size - 1}",
                leadingIcon = Icons.Rounded.Person,
                selected = false,
                onClick = {}
            )
            GlassChip(
                label = SampleData.landmarks.first().name,
                leadingIcon = Icons.Rounded.LocationOn,
                selected = true,
                onClick = {}
            )
        }
    }
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/core/designsystem/component/AvatarRing.kt
package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    onClick: (() -> Unit)? = null
) {
    val colors = LokapTheme.colors
    val shape = LokapTheme.shapes.pill
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
        modifier = modifier
            .size(size)
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
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/core/designsystem/component/BadgeDot.kt
package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.data.fake.SampleData

@Composable
fun BadgeDot(
    count: Int,
    modifier: Modifier = Modifier
) {
    if (count <= 0) return

    val colors = LokapTheme.colors
    val shape = LokapTheme.shapes.pill
    val displayText = if (count > 99) "99+" else count.toString()

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
            .clip(shape)
            .background(colors.primary)
            .border(1.5.dp, colors.background, shape)
            .padding(horizontal = 5.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onPrimary
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun BadgeDotPreview() {
    LokapTheme {
        val unreadTotal = SampleData.conversations.sumOf { it.unreadCount }
        Row(
            modifier = Modifier
                .background(LokapTheme.colors.background)
                .padding(LokapTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.md)
        ) {
            BadgeDot(count = unreadTotal)
            BadgeDot(count = 12)
            BadgeDot(count = 128)
        }
    }
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/core/designsystem/component/EmojiBar.kt
package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.Reaction

private val DefaultEmojiSet = listOf("😍", "😂", "🔥", "😮", "😢")

@Composable
fun EmojiBar(
    reactions: List<Reaction>,
    selectedEmoji: String?,
    onSelectEmoji: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shape = LokapTheme.shapes.pill

    val countsByEmoji = remember(reactions) {
        reactions.associate { it.emoji to it.count }
    }

    Row(
        modifier = modifier
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.outline, shape)
            .padding(horizontal = spacing.sm, vertical = spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DefaultEmojiSet.forEach { emoji ->
            val isSelected = selectedEmoji == emoji
            val count = countsByEmoji[emoji] ?: 0

            Row(
                modifier = Modifier
                    .clip(shape)
                    .background(if (isSelected) colors.surfaceHigh else colors.surface)
                    .border(
                        width = if (isSelected) 1.5.dp else 0.dp,
                        color = if (isSelected) colors.primary else colors.surface,
                        shape = shape
                    )
                    .clickable { onSelectEmoji(emoji) }
                    .padding(horizontal = spacing.sm, vertical = spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (count > 0) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) colors.primary else colors.textSecondary
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun EmojiBarPreview() {
    LokapTheme {
        val samplePost = SampleData.posts.first()
        Row(
            modifier = Modifier
                .background(LokapTheme.colors.background)
                .padding(LokapTheme.spacing.lg)
        ) {
            EmojiBar(
                reactions = samplePost.reactions,
                selectedEmoji = samplePost.myReaction,
                onSelectEmoji = {}
            )
        }
    }
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/core/designsystem/component/PhotoCard.kt
package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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

            // Lớp phủ gradient trên & dưới để chữ nổi bật
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

            // Thanh trên: Avatar + tên + thời gian + vị trí + menu ⋮
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

            // Chú thích ảnh phía dưới
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

            // Trạng thái PENDING / FAILED
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
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/core/designsystem/component/RegionCard.kt
package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(shapes.card)
            .background(regionGradient(code))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(spacing.lg)
    ) {
        // 3 ảnh xếp chồng nghiêng ±6° kiểu Polaroid ở góc phải
        val polaroidAngles = listOf(-6f, 6f, 0f)
        val polaroidOffsets = listOf(
            (-18).dp to 8.dp,
            14.dp to (-6).dp,
            0.dp to 0.dp
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = spacing.sm)
        ) {
            for (i in 0 until 3) {
                val url = photoUrls.getOrNull(i)
                val angle = polaroidAngles[i]
                val (offsetX, offsetY) = polaroidOffsets[i]
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

        // Sticker Emoji trang trí
        Row(
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
            style = MaterialTheme.typography.displayLarge,
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
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/core/designsystem/component/MapBubbles.kt
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
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/core/designsystem/component/StateFeedback.kt
package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme

@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(LokapTheme.shapes.pill)
                .background(colors.surfaceHigh)
                .border(1.dp, colors.outline, LokapTheme.shapes.pill),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = colors.primary
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
        if (actionLabel != null && onActionClick != null) {
            PinkButton(
                text = actionLabel,
                onClick = onActionClick
            )
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryLabel: String = "Thử lại"
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(LokapTheme.shapes.card)
            .background(colors.surface)
            .border(1.dp, colors.outline, LokapTheme.shapes.card)
            .padding(spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        Icon(
            imageVector = Icons.Rounded.Warning,
            contentDescription = null,
            tint = colors.error,
            modifier = Modifier.size(36.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        PinkButton(
            text = retryLabel,
            leadingIcon = Icons.Rounded.Refresh,
            onClick = onRetry
        )
    }
}

@Composable
fun OfflineBanner(
    modifier: Modifier = Modifier,
    message: String = "Không có kết nối mạng · Ảnh sẽ tự động gửi khi có mạng lại"
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(LokapTheme.shapes.pill)
            .background(colors.surfaceHigh)
            .border(1.dp, colors.accentYellow, LokapTheme.shapes.pill)
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        Icon(
            imageVector = Icons.Rounded.Warning,
            contentDescription = null,
            tint = colors.accentYellow,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.labelLarge,
            color = colors.textPrimary
        )
    }
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = LokapTheme.shapes.card
) {
    val colors = LokapTheme.colors
    val transition = rememberInfiniteTransition(label = "lokap_shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            colors.surface,
            colors.surfaceHigh,
            colors.surface
        ),
        start = Offset(progress - 300f, progress - 300f),
        end = Offset(progress, progress)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun StateFeedbackPreview() {
    LokapTheme {
        Column(
            modifier = Modifier
                .background(LokapTheme.colors.background)
                .padding(LokapTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.md)
        ) {
            OfflineBanner()
            EmptyState(
                title = "Chưa có khoảnh khắc nào",
                subtitle = "Hãy chụp tấm ảnh đầu tiên cùng hội bạn thân ở Đà Nẵng"
            )
            ErrorState(
                message = "Không thể khởi động máy ảnh. Vui lòng đóng các ứng dụng khác đang dùng camera.",
                onRetry = {}
            )
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
            )
        }
    }
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/core/navigation/Route.kt
package vn.nhom2.lokap.core.navigation

import kotlinx.serialization.Serializable

sealed interface Route {

    @Serializable
    data object Camera : Route

    @Serializable
    data class Composer(val imagePath: String) : Route

    @Serializable
    data object Feed : Route

    @Serializable
    data object Map : Route

    @Serializable
    data class RegionAlbum(val level: String, val code: String) : Route

    @Serializable
    data class Landmark(val id: String) : Route

    @Serializable
    data class PostDetail(val id: String) : Route

    @Serializable
    data class Profile(val userId: String? = null) : Route

    @Serializable
    data object Friends : Route

    @Serializable
    data object Notifications : Route

    @Serializable
    data object Inbox : Route

    @Serializable
    data class Chat(val conversationId: String) : Route

    @Serializable
    data object Settings : Route
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/core/navigation/AppNavHost.kt
package vn.nhom2.lokap.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import vn.nhom2.lokap.core.designsystem.component.EmptyState
import vn.nhom2.lokap.core.designsystem.component.GhostButton
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.feature.camera.CameraRoute

private const val NAV_ANIM_DURATION = 320

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Route.Camera,
        modifier = modifier
            .fillMaxSize()
            .background(LokapTheme.colors.background),
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(NAV_ANIM_DURATION)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(NAV_ANIM_DURATION)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(NAV_ANIM_DURATION)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(NAV_ANIM_DURATION)
            )
        }
    ) {
        composable<Route.Camera> {
            CameraRoute(
                onNavigateToComposer = { path -> navController.navigate(Route.Composer(path)) },
                onNavigateToFeed = { navController.navigate(Route.Feed) },
                onNavigateToMap = { navController.navigate(Route.Map) },
                onNavigateToFriends = { navController.navigate(Route.Friends) },
                onNavigateToInbox = { navController.navigate(Route.Inbox) },
                onNavigateToProfile = { userId -> navController.navigate(Route.Profile(userId)) }
            )
        }

        composable<Route.Feed>(
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(NAV_ANIM_DURATION)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(NAV_ANIM_DURATION)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(NAV_ANIM_DURATION)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(NAV_ANIM_DURATION)
                )
            }
        ) {
            PlaceholderScreen(
                title = "Bảng tin Bạn bè & Khám phá",
                subtitle = "Đang xây dựng",
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.Composer> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Composer>()
            PlaceholderScreen(
                title = "Đăng ảnh mới",
                subtitle = "Đang xây dựng · File: ${route.imagePath.substringAfterLast('/')}",
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.Map> {
            PlaceholderScreen(
                title = "Bản đồ 34 Tỉnh/Thành",
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.RegionAlbum> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.RegionAlbum>()
            PlaceholderScreen(
                title = "Album khu vực (${route.level} · ${route.code})",
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.Landmark> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Landmark>()
            PlaceholderScreen(
                title = "Địa danh (${route.id})",
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.PostDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.PostDetail>()
            PlaceholderScreen(
                title = "Chi tiết bài đăng (${route.id})",
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.Profile> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Profile>()
            PlaceholderScreen(
                title = "Hồ sơ cá nhân (${route.userId ?: "Tôi"})",
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.Friends> {
            PlaceholderScreen(
                title = "Danh sách bạn bè",
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.Notifications> {
            PlaceholderScreen(
                title = "Thông báo",
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.Inbox> {
            PlaceholderScreen(
                title = "Hộp thư tin nhắn",
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.Chat> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Chat>()
            PlaceholderScreen(
                title = "Trò chuyện (${route.conversationId})",
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.Settings> {
            PlaceholderScreen(
                title = "Cài đặt",
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun PlaceholderScreen(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String = "Đang xây dựng"
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .padding(spacing.lg),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = colors.textPrimary
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
        }

        EmptyState(
            title = subtitle,
            subtitle = "Màn hình \"$title\" sẽ được hoàn thiện ở bước tiếp theo."
        )

        GhostButton(
            text = "Quay lại",
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun PlaceholderScreenPreview() {
    LokapTheme {
        PlaceholderScreen(
            title = "Bản đồ 34 Tỉnh/Thành",
            onBack = {}
        )
    }
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/camera/CameraContract.kt
package vn.nhom2.lokap.feature.camera

import androidx.compose.runtime.Immutable
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.User

enum class CameraPermissionState {
    NOT_REQUESTED,
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED
}

enum class CameraLens {
    BACK,
    FRONT
}

enum class FlashMode {
    OFF,
    ON,
    AUTO
}

@Immutable
data class CameraUiState(
    val permission: CameraPermissionState = CameraPermissionState.NOT_REQUESTED,
    val lensFacing: CameraLens = CameraLens.BACK,
    val flashMode: FlashMode = FlashMode.OFF,
    val isCapturing: Boolean = false,
    val friendCount: Int = SampleData.users.size - 1,
    val unreadMessages: Int = SampleData.conversations.sumOf { it.unreadCount },
    val me: User = SampleData.currentUser,
    val zoomRatio: Float = 1f,
    val cameraError: String? = null
)

sealed interface CameraAction {
    data class OnPermissionResult(
        val isGranted: Boolean,
        val shouldShowRationale: Boolean
    ) : CameraAction

    data object OnSyncPermissionGranted : CameraAction
    data object OnRequestPermissionClick : CameraAction
    data object OnOpenSettingsClick : CameraAction
    data object OnToggleLens : CameraAction
    data object OnCycleFlash : CameraAction
    data class OnZoomChange(val zoomRatio: Float) : CameraAction
    data object OnShutterClick : CameraAction
    data class OnPhotoSaved(val imagePath: String) : CameraAction
    data class OnCameraError(val message: String) : CameraAction
    data object OnRetryCamera : CameraAction
    data object OnSwipeUpToFeed : CameraAction
    data object OnMapClick : CameraAction
    data object OnFriendsClick : CameraAction
    data object OnInboxClick : CameraAction
    data object OnProfileClick : CameraAction
}

sealed interface CameraEffect {
    data object LaunchPermissionRequest : CameraEffect
    data object OpenAppSettings : CameraEffect
    data object TriggerCapture : CameraEffect
    data class NavigateToComposer(val imagePath: String) : CameraEffect
    data object NavigateToFeed : CameraEffect
    data object NavigateToMap : CameraEffect
    data object NavigateToFriends : CameraEffect
    data object NavigateToInbox : CameraEffect
    data class NavigateToProfile(val userId: String?) : CameraEffect
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/camera/CameraViewModel.kt
package vn.nhom2.lokap.feature.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CameraViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _effects = Channel<CameraEffect>(Channel.BUFFERED)
    val effects: Flow<CameraEffect> = _effects.receiveAsFlow()

    fun onAction(action: CameraAction) {
        when (action) {
            is CameraAction.OnSyncPermissionGranted -> {
                _uiState.update {
                    it.copy(permission = CameraPermissionState.GRANTED, cameraError = null)
                }
            }

            is CameraAction.OnPermissionResult -> {
                val nextPermission = when {
                    action.isGranted -> CameraPermissionState.GRANTED
                    action.shouldShowRationale -> CameraPermissionState.DENIED
                    else -> CameraPermissionState.PERMANENTLY_DENIED
                }
                _uiState.update {
                    it.copy(permission = nextPermission, cameraError = null)
                }
            }

            is CameraAction.OnRequestPermissionClick -> {
                sendEffect(CameraEffect.LaunchPermissionRequest)
            }

            is CameraAction.OnOpenSettingsClick -> {
                sendEffect(CameraEffect.OpenAppSettings)
            }

            is CameraAction.OnToggleLens -> {
                _uiState.update { state ->
                    val nextLens = if (state.lensFacing == CameraLens.BACK) {
                        CameraLens.FRONT
                    } else {
                        CameraLens.BACK
                    }
                    state.copy(lensFacing = nextLens, zoomRatio = 1f, cameraError = null)
                }
            }

            is CameraAction.OnCycleFlash -> {
                _uiState.update { state ->
                    val nextFlash = when (state.flashMode) {
                        FlashMode.OFF -> FlashMode.ON
                        FlashMode.ON -> FlashMode.AUTO
                        FlashMode.AUTO -> FlashMode.OFF
                    }
                    state.copy(flashMode = nextFlash)
                }
            }

            is CameraAction.OnZoomChange -> {
                _uiState.update { state ->
                    state.copy(zoomRatio = action.zoomRatio.coerceIn(1f, 8f))
                }
            }

            is CameraAction.OnShutterClick -> {
                val current = _uiState.value
                if (current.permission == CameraPermissionState.GRANTED &&
                    !current.isCapturing &&
                    current.cameraError == null
                ) {
                    _uiState.update { it.copy(isCapturing = true) }
                    sendEffect(CameraEffect.TriggerCapture)
                }
            }

            is CameraAction.OnPhotoSaved -> {
                _uiState.update { it.copy(isCapturing = false) }
                sendEffect(CameraEffect.NavigateToComposer(action.imagePath))
            }

            is CameraAction.OnCameraError -> {
                _uiState.update {
                    it.copy(isCapturing = false, cameraError = action.message)
                }
            }

            is CameraAction.OnRetryCamera -> {
                _uiState.update { it.copy(cameraError = null, isCapturing = false) }
            }

            is CameraAction.OnSwipeUpToFeed -> {
                sendEffect(CameraEffect.NavigateToFeed)
            }

            is CameraAction.OnMapClick -> {
                sendEffect(CameraEffect.NavigateToMap)
            }

            is CameraAction.OnFriendsClick -> {
                sendEffect(CameraEffect.NavigateToFriends)
            }

            is CameraAction.OnInboxClick -> {
                sendEffect(CameraEffect.NavigateToInbox)
            }

            is CameraAction.OnProfileClick -> {
                sendEffect(CameraEffect.NavigateToProfile(_uiState.value.me.id))
            }
        }
    }

    private fun sendEffect(effect: CameraEffect) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/camera/CameraScreen.kt
package vn.nhom2.lokap.feature.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Size
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalZeroShutterLag
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview as CameraXPreview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.nhom2.lokap.core.designsystem.component.AvatarRing
import vn.nhom2.lokap.core.designsystem.component.BadgeDot
import vn.nhom2.lokap.core.designsystem.component.ErrorState
import vn.nhom2.lokap.core.designsystem.component.GlassChip
import vn.nhom2.lokap.core.designsystem.component.PinkButton
import vn.nhom2.lokap.core.designsystem.component.ShutterButton
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.designsystem.theme.regionGradient
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun CameraRoute(
    onNavigateToComposer: (String) -> Unit,
    onNavigateToFeed: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToProfile: (String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var captureTriggerTick by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val activity = context.findActivity()
        val rationale = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
        } ?: false
        viewModel.onAction(CameraAction.OnPermissionResult(isGranted, rationale))
    }

    // Kiểm tra quyền khi vào màn hình hoặc quay lại từ Cài đặt
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    viewModel.onAction(CameraAction.OnSyncPermissionGranted)
                } else if (state.permission == CameraPermissionState.NOT_REQUESTED) {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CameraEffect.LaunchPermissionRequest -> {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }

                CameraEffect.OpenAppSettings -> {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }

                CameraEffect.TriggerCapture -> {
                    captureTriggerTick++
                }

                is CameraEffect.NavigateToComposer -> onNavigateToComposer(effect.imagePath)
                CameraEffect.NavigateToFeed -> onNavigateToFeed()
                CameraEffect.NavigateToMap -> onNavigateToMap()
                CameraEffect.NavigateToFriends -> onNavigateToFriends()
                CameraEffect.NavigateToInbox -> onNavigateToInbox()
                is CameraEffect.NavigateToProfile -> onNavigateToProfile(effect.userId)
            }
        }
    }

    CameraScreen(
        state = state,
        onAction = viewModel::onAction,
        captureTriggerTick = captureTriggerTick,
        modifier = modifier
    )
}

@Composable
fun CameraScreen(
    state: CameraUiState,
    onAction: (CameraAction) -> Unit,
    modifier: Modifier = Modifier,
    captureTriggerTick: Int = 0
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes
    var accumulatedDragY by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { accumulatedDragY = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDragY += dragAmount
                    },
                    onDragEnd = {
                        if (accumulatedDragY < -90f) {
                            onAction(CameraAction.OnSwipeUpToFeed)
                        }
                        accumulatedDragY = 0f
                    }
                )
            },
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Thanh trên: AvatarRing, Chip "Bạn bè · N", Nút Map, Nút Inbox + BadgeDot
        CameraTopBar(
            state = state,
            onAction = onAction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg, vertical = spacing.sm)
        )

        // 2. Khung Preview vuông bo 40dp (cameraFrame), cách lề ngang 16dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg)
                .aspectRatio(1f)
                .clip(shapes.cameraFrame)
                .background(colors.surface)
                .border(1.5.dp, colors.outline, shapes.cameraFrame),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.permission != CameraPermissionState.GRANTED -> {
                    PermissionPromptContent(
                        permissionState = state.permission,
                        onRequestPermission = { onAction(CameraAction.OnRequestPermissionClick) },
                        onOpenSettings = { onAction(CameraAction.OnOpenSettingsClick) }
                    )
                }

                state.cameraError != null -> {
                    ErrorState(
                        message = state.cameraError,
                        onRetry = { onAction(CameraAction.OnRetryCamera) },
                        modifier = Modifier.padding(spacing.lg)
                    )
                }

                else -> {
                    CameraPreviewViewport(
                        state = state,
                        captureTriggerTick = captureTriggerTick,
                        onDoubleTapFlip = { onAction(CameraAction.OnToggleLens) },
                        onZoomChange = { onAction(CameraAction.OnZoomChange(it)) },
                        onPhotoSaved = { onAction(CameraAction.OnPhotoSaved(it)) },
                        onCameraError = { onAction(CameraAction.OnCameraError(it)) }
                    )
                }
            }

            // Lớp phủ đang chụp
            if (state.isCapturing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.background.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        CircularProgressIndicator(color = colors.primary)
                        Text(
                            text = "Đang xử lý ảnh…",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textPrimary
                        )
                    }
                }
            }
        }

        // 3. Thanh điều khiển dưới: Flash, ShutterButton, Lật Camera (KHÔNG có nút thư viện ảnh)
        CameraControlsBar(
            state = state,
            onAction = onAction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.xl)
        )

        // 4. Gợi ý vuốt lên xem bài
        Column(
            modifier = Modifier
                .clickable { onAction(CameraAction.OnSwipeUpToFeed) }
                .padding(bottom = spacing.md, top = spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowUp,
                contentDescription = null,
                tint = colors.textSecondary
            )
            Text(
                text = "Vuốt lên xem bài",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun CameraTopBar(
    state: CameraUiState,
    onAction: (CameraAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val pill = LokapTheme.shapes.pill

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AvatarRing(
            avatarUrl = state.me.avatarUrl,
            displayName = state.me.displayName,
            size = 44.dp,
            hasNewPost = true,
            onClick = { onAction(CameraAction.OnProfileClick) }
        )

        GlassChip(
            label = "Bạn bè · ${state.friendCount}",
            onClick = { onAction(CameraAction.OnFriendsClick) }
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(pill)
                    .background(colors.surfaceHigh)
                    .border(1.dp, colors.outline, pill)
                    .clickable { onAction(CameraAction.OnMapClick) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Place,
                    contentDescription = "Bản đồ",
                    tint = colors.textPrimary
                )
            }

            Box(contentAlignment = Alignment.TopEnd) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(pill)
                        .background(colors.surfaceHigh)
                        .border(1.dp, colors.outline, pill)
                        .clickable { onAction(CameraAction.OnInboxClick) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Email,
                        contentDescription = "Tin nhắn",
                        tint = colors.textPrimary
                    )
                }
                BadgeDot(count = state.unreadMessages)
            }
        }
    }
}

@Composable
private fun CameraControlsBar(
    state: CameraUiState,
    onAction: (CameraAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val pill = LokapTheme.shapes.pill
    val controlsEnabled = state.permission == CameraPermissionState.GRANTED &&
        !state.isCapturing &&
        state.cameraError == null

    val flashLabel = when (state.flashMode) {
        FlashMode.OFF -> "⚡ Tắt"
        FlashMode.ON -> "⚡ Bật"
        FlashMode.AUTO -> "⚡ Tự động"
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Nút Flash
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(pill)
                .background(colors.surfaceHigh)
                .border(
                    width = 1.dp,
                    color = if (state.flashMode != FlashMode.OFF) colors.accentYellow else colors.outline,
                    shape = pill
                )
                .clickable(enabled = controlsEnabled) { onAction(CameraAction.OnCycleFlash) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = flashLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (state.flashMode != FlashMode.OFF) colors.accentYellow else colors.textPrimary,
                textAlign = TextAlign.Center
            )
        }

        // Nút chụp ảnh chính
        ShutterButton(
            onClick = { onAction(CameraAction.OnShutterClick) },
            enabled = controlsEnabled
        )

        // Nút lật camera trước/sau
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(pill)
                .background(colors.surfaceHigh)
                .border(1.dp, colors.outline, pill)
                .clickable(enabled = controlsEnabled) { onAction(CameraAction.OnToggleLens) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Lật máy ảnh",
                tint = colors.textPrimary
            )
        }
    }
}

@Composable
private fun PermissionPromptContent(
    permissionState: CameraPermissionState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when (permissionState) {
                CameraPermissionState.PERMANENTLY_DENIED -> "Cần mở quyền máy ảnh trong Cài đặt"
                else -> "Cho phép Lokap dùng máy ảnh"
            },
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = when (permissionState) {
                CameraPermissionState.PERMANENTLY_DENIED ->
                    "Bạn đã tắt quyền truy cập máy ảnh. Hãy mở Cài đặt ứng dụng để bật lại và chụp khoảnh khắc cùng bạn bè."
                else ->
                    "Lokap chỉ chụp ảnh trực tiếp từ camera để giữ mọi khoảnh khắc chân thật nhất."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.sm, bottom = spacing.lg)
        )

        if (permissionState == CameraPermissionState.PERMANENTLY_DENIED) {
            PinkButton(
                text = "Mở Cài đặt",
                onClick = onOpenSettings
            )
        } else {
            PinkButton(
                text = "Cho phép",
                onClick = onRequestPermission
            )
        }
    }
}

@OptIn(ExperimentalZeroShutterLag::class)
@SuppressLint("MissingPermission")
@Composable
private fun CameraPreviewViewport(
    state: CameraUiState,
    captureTriggerTick: Int,
    onDoubleTapFlip: () -> Unit,
    onZoomChange: (Float) -> Unit,
    onPhotoSaved: (String) -> Unit,
    onCameraError: (String) -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val isPreview = LocalInspectionMode.current

    if (isPreview) {
        // Trong @Preview: vẽ khung giả, tuyệt đối không gọi CameraX
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(regionGradient("da-nang")),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                Text(
                    text = if (state.lensFacing == CameraLens.FRONT) "Camera trước · 1080p" else "Camera sau · 1080p",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onPrimary
                )
                Text(
                    text = "Chạm đúp để lật · Chụm 2 ngón để thu phóng",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onPrimary
                )
            }
        }
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    var cameraRef by remember { mutableStateOf<Camera?>(null) }
    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }

    // Bind CameraX theo lifecycle và lensFacing
    LaunchedEffect(state.lensFacing, lifecycleOwner) {
        try {
            val cameraProvider = context.awaitCameraProvider()
            val lensInt = if (state.lensFacing == CameraLens.FRONT) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensInt)
                .build()

            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(1080, 1920),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                )
                .build()

            val previewUseCase = CameraXPreview.Builder()
                .setResolutionSelector(resolutionSelector)
                .build()
                .also { it.surfaceProvider = previewView.surfaceProvider }

            val zslSupported = cameraProvider.isZslSupported(cameraSelector)
            val captureMode = if (zslSupported) {
                ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG
            } else {
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
            }

            val captureUseCase = ImageCapture.Builder()
                .setCaptureMode(captureMode)
                .setResolutionSelector(resolutionSelector)
                .setTargetRotation(previewView.display?.rotation ?: Surface.ROTATION_0)
                .build()

            cameraProvider.unbindAll()
            val boundCamera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                previewUseCase,
                captureUseCase
            )
            cameraRef = boundCamera
            imageCaptureRef = captureUseCase
        } catch (_: Exception) {
            onCameraError("Máy ảnh đang bận hoặc không thể khởi tạo. Vui lòng thử lại.")
        }
    }

    // Cập nhật chế độ đèn Flash
    LaunchedEffect(state.flashMode, imageCaptureRef) {
        imageCaptureRef?.flashMode = when (state.flashMode) {
            FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
            FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
        }
    }

    // Cập nhật mức Zoom
    LaunchedEffect(state.zoomRatio, cameraRef) {
        cameraRef?.cameraControl?.setZoomRatio(state.zoomRatio)
    }

    // Chụp ảnh ra cacheDir khi nhận TriggerCapture; lật ngang nếu là camera trước
    LaunchedEffect(captureTriggerTick) {
        if (captureTriggerTick == 0) return@LaunchedEffect
        val capture = imageCaptureRef ?: run {
            onCameraError("Máy ảnh chưa sẵn sàng để chụp.")
            return@LaunchedEffect
        }

        val outputFile = File(context.cacheDir, "lokap_${System.currentTimeMillis()}.jpg")
        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = (state.lensFacing == CameraLens.FRONT)
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile)
            .setMetadata(metadata)
            .build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onPhotoSaved(outputFile.absolutePath)
                }

                override fun onError(exception: ImageCaptureException) {
                    onCameraError("Không thể lưu ảnh chụp. Vui lòng thử lại.")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTapFlip() }
                )
            }
            .pointerInput(state.zoomRatio) {
                detectTransformGestures { _, _, zoomChange, _ ->
                    if (zoomChange != 1f) {
                        onZoomChange(state.zoomRatio * zoomChange)
                    }
                }
            }
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalZeroShutterLag::class)
private fun ProcessCameraProvider.isZslSupported(selector: CameraSelector): Boolean {
    return try {
        availableCameraInfos
            .let { selector.filter(it) }
            .firstOrNull()
            ?.isZslSupported == true
    } catch (_: Exception) {
        false
    }
}

private suspend fun Context.awaitCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener(
            { continuation.resume(future.get()) },
            ContextCompat.getMainExecutor(this)
        )
    }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Preview(name = "1. Camera - Bình thường", showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun CameraScreenNormalPreview() {
    LokapTheme {
        CameraScreen(
            state = CameraUiState(
                permission = CameraPermissionState.GRANTED,
                flashMode = FlashMode.AUTO
            ),
            onAction = {}
        )
    }
}

@Preview(name = "2. Camera - Chưa cấp quyền", showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun CameraScreenPermissionDeniedPreview() {
    LokapTheme {
        CameraScreen(
            state = CameraUiState(
                permission = CameraPermissionState.DENIED
            ),
            onAction = {}
        )
    }
}

@Preview(name = "3. Camera - Từ chối vĩnh viễn", showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun CameraScreenPermanentlyDeniedPreview() {
    LokapTheme {
        CameraScreen(
            state = CameraUiState(
                permission = CameraPermissionState.PERMANENTLY_DENIED
            ),
            onAction = {}
        )
    }
}

@Preview(name = "4. Camera - Đang chụp", showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun CameraScreenCapturingPreview() {
    LokapTheme {
        CameraScreen(
            state = CameraUiState(
                permission = CameraPermissionState.GRANTED,
                isCapturing = true
            ),
            onAction = {}
        )
    }
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/MainActivity.kt
package vn.nhom2.lokap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.AppNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LokapTheme {
                AppNavHost()
            }
        }
    }
}
```

```kotlin
<!-- FILE: app/src/main/AndroidManifest.xml -->
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.INTERNET" />

    <uses-feature
        android:name="android.hardware.camera"
        android:required="false" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

<!-- tail -->
       android:required="false" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>

Plugins & Dependencies cần thêm (không ghi phiên bản)

Plugin:

org.jetbrains.kotlin.plugin.serialization

Dependencies (app/build.gradle.kts):

androidx.navigation:navigation-compose

org.jetbrains.kotlinx:kotlinx-serialization-json

androidx.lifecycle:lifecycle-viewmodel-compose

androidx.lifecycle:lifecycle-runtime-compose

androidx.camera:camera-core

androidx.camera:camera-camera2

androidx.camera:camera-lifecycle

androidx.camera:camera-view

io.coil-kt.coil3:coil-compose

io.coil-kt.coil3:coil-network-okhttp