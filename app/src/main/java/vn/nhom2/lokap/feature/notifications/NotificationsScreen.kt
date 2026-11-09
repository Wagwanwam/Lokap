package vn.nhom2.lokap.feature.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import vn.nhom2.lokap.core.common.formatTimeAgo
import vn.nhom2.lokap.core.designsystem.component.*
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.core.navigation.navigateSingleTop
import vn.nhom2.lokap.core.navigation.popBackStackSafely
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.*
private enum class NotificationFilter(val label: String) {
    ALL("Tất cả"),
    UNREAD("Chưa đọc"),
    FRIENDS("Bạn bè")
}

fun NavGraphBuilder.notificationsScreen(navController: NavController) {
    composable<Route.Notifications> {
        NotificationsRoute(
            onNavigateToPostDetail = { postId ->
                navController.navigateSingleTop(Route.PostDetail(id = postId))
            },
            onNavigateToFriends = {
                navController.navigateSingleTop(Route.Friends)
            },
            onNavigateToInbox = {
                navController.navigateSingleTop(Route.Inbox)
            },
            onNavigateToChat = { conversationId ->
                navController.navigateSingleTop(Route.Chat(conversationId))
            },
            onNavigateBack = {
                navController.popBackStackSafely()
            }
        )
    }
}

@Composable
fun NotificationsRoute(
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: NotificationsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is NotificationsEffect.NavigateToPostDetail -> onNavigateToPostDetail(effect.postId)
                NotificationsEffect.NavigateToFriends -> onNavigateToFriends()
                NotificationsEffect.NavigateToInbox -> onNavigateToInbox()
                is NotificationsEffect.NavigateToChat -> onNavigateToChat(effect.conversationId)
                NotificationsEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    NotificationsScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@Composable
fun NotificationsScreen(
    uiState: NotificationsUiState,
    onAction: (NotificationsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    var currentFilter by remember { mutableStateOf(NotificationFilter.ALL) }

    val filteredUnread = remember(uiState.unreadNotifications, currentFilter) {
        when (currentFilter) {
            NotificationFilter.ALL, NotificationFilter.UNREAD -> uiState.unreadNotifications
            NotificationFilter.FRIENDS -> uiState.unreadNotifications.filter {
                it.type == NotificationType.FRIEND_REQUEST || it.type == NotificationType.FRIEND_ACCEPTED
            }
        }
    }

    val filteredEarlier = remember(uiState.earlierNotifications, currentFilter) {
        when (currentFilter) {
            NotificationFilter.ALL -> uiState.earlierNotifications
            NotificationFilter.UNREAD -> emptyList()
            NotificationFilter.FRIENDS -> uiState.earlierNotifications.filter {
                it.type == NotificationType.FRIEND_REQUEST || it.type == NotificationType.FRIEND_ACCEPTED
            }
        }
    }

    val isListEmpty = filteredUnread.isEmpty() && filteredEarlier.isEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
    ) {
        NotificationsTopBar(
            hasUnread = uiState.hasUnread,
            onBackClick = { onAction(NotificationsAction.NavigateBack) },
            onMarkAllReadClick = { onAction(NotificationsAction.MarkAllAsRead) }
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = spacing.xs),
            contentPadding = PaddingValues(horizontal = spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(NotificationFilter.entries.toTypedArray()) { filter ->
                val isSelected = currentFilter == filter
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) colors.primary.copy(alpha = 0.12f) else colors.surfaceHigh,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) colors.primary.copy(alpha = 0.4f) else colors.outline.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { currentFilter = filter }
                ) {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) colors.primary else colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                uiState.isLoading -> {
                    NotificationsLoadingSkeleton()
                }

                uiState.errorMessage != null -> {
                    ErrorState(
                        message = uiState.errorMessage,
                        onRetry = { onAction(NotificationsAction.Retry) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.lg)
                    )
                }

                uiState.isEmpty -> {
                    EmptyState(
                        title = "Chưa có thông báo nào",
                        subtitle = "Khi bạn bè thả cảm xúc hoặc đăng ảnh mới, thông báo sẽ xuất hiện tại đây",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.lg)
                    )
                }

                isListEmpty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.lg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Không có thông báo nào trong mục này",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = spacing.lg,
                            vertical = spacing.sm
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (filteredUnread.isNotEmpty()) {
                            item(key = "section_new") {
                                NotificationSectionHeader(title = "Mới nhận")
                            }
                            items(
                                items = filteredUnread,
                                key = { it.id }
                            ) { notification ->
                                NotificationRowItem(
                                    notification = notification,
                                    now = uiState.now,
                                    friendRequestDecision = uiState.handledFriendRequestIds[notification.id],
                                    onClick = {
                                        onAction(NotificationsAction.ClickNotification(notification))
                                    },
                                    onAcceptFriend = {
                                        onAction(NotificationsAction.AcceptFriendRequest(notification.id))
                                    },
                                    onDeclineFriend = {
                                        onAction(NotificationsAction.DeclineFriendRequest(notification.id))
                                    }
                                )
                            }
                        }

                        if (filteredEarlier.isNotEmpty()) {
                            item(key = "section_earlier") {
                                NotificationSectionHeader(title = "Trước đó")
                            }
                            items(
                                items = filteredEarlier,
                                key = { it.id }
                            ) { notification ->
                                NotificationRowItem(
                                    notification = notification,
                                    now = uiState.now,
                                    friendRequestDecision = uiState.handledFriendRequestIds[notification.id],
                                    onClick = {
                                        onAction(NotificationsAction.ClickNotification(notification))
                                    },
                                    onAcceptFriend = {
                                        onAction(NotificationsAction.AcceptFriendRequest(notification.id))
                                    },
                                    onDeclineFriend = {
                                        onAction(NotificationsAction.DeclineFriendRequest(notification.id))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationsTopBar(
    hasUnread: Boolean,
    onBackClick: () -> Unit,
    onMarkAllReadClick: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Surface(
                shape = CircleShape,
                color = colors.surfaceHigh,
                border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.25f)),
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBackClick)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = "Thông báo",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                ),
                color = colors.textPrimary
            )
        }

        if (hasUnread) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.primary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onMarkAllReadClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Đọc tất cả",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationSectionHeader(title: String) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = colors.textSecondary,
        modifier = Modifier.padding(
            horizontal = spacing.xs,
            vertical = spacing.xs
        )
    )
}

@Composable
private fun NotificationRowItem(
    notification: AppNotification,
    now: java.time.Instant,
    friendRequestDecision: Boolean?,
    onClick: () -> Unit,
    onAcceptFriend: () -> Unit,
    onDeclineFriend: () -> Unit
) {
    val colors = LokapTheme.colors
    val isUnread = !notification.isRead

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isUnread) colors.surfaceHigh else colors.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isUnread) colors.primary.copy(alpha = 0.35f) else colors.outline.copy(alpha = 0.18f)
        ),
        shadowElevation = if (isUnread) 2.dp else 0.5.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {

            Box(contentAlignment = Alignment.BottomEnd) {
                if (notification.type == NotificationType.POST_HIDDEN) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Bài đăng bị ẩn",
                            tint = colors.accentYellow,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    AvatarRing(
                        avatarUrl = notification.actor?.avatarUrl,
                        displayName = notification.actor?.displayName ?: "Lokap",
                        size = 46.dp
                    )
                }

                val badgeConfig = getNotificationBadgeConfig(notification.type, colors)
                if (badgeConfig != null) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(badgeConfig.containerColor)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = badgeConfig.icon,
                            contentDescription = null,
                            tint = badgeConfig.iconTint,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = notification.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    ),
                    color = colors.textPrimary
                )

                Text(
                    text = formatTimeAgo(notification.createdAt, now),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUnread) colors.primary else colors.textTertiary
                )

                if (notification.type == NotificationType.FRIEND_REQUEST) {
                    Spacer(modifier = Modifier.height(6.dp))
                    if (friendRequestDecision == null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PinkButton(
                                text = "Chấp nhận",
                                onClick = onAcceptFriend,
                                modifier = Modifier.weight(1f)
                            )
                            GhostButton(
                                text = "Từ chối",
                                onClick = onDeclineFriend,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        val statusText = if (friendRequestDecision) {
                            "✓ Đã đồng ý kết bạn"
                        } else {
                            "Đã từ chối lời mời"
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (friendRequestDecision) colors.success.copy(alpha = 0.12f) else colors.surfaceHigh
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (friendRequestDecision) colors.success else colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (isUnread) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.primary)
                )
            }
        }
    }
}

private data class BadgeConfig(
    val icon: ImageVector,
    val containerColor: Color,
    val iconTint: Color
)

@Composable
private fun getNotificationBadgeConfig(
    type: NotificationType,
    colors: vn.nhom2.lokap.core.designsystem.theme.LokapColors
): BadgeConfig? {
    return when (type) {
        NotificationType.REACTION -> BadgeConfig(
            icon = Icons.Filled.Favorite,
            containerColor = colors.primary,
            iconTint = colors.buttonText
        )
        NotificationType.NEW_POST -> BadgeConfig(
            icon = Icons.Filled.CameraAlt,
            containerColor = Color(0xFF3B82F6),
            iconTint = Color.White
        )
        NotificationType.FRIEND_REQUEST -> BadgeConfig(
            icon = Icons.Filled.PersonAdd,
            containerColor = Color(0xFF8B5CF6),
            iconTint = Color.White
        )
        NotificationType.FRIEND_ACCEPTED -> BadgeConfig(
            icon = Icons.Filled.Check,
            containerColor = colors.success,
            iconTint = Color.White
        )
        NotificationType.NEW_MESSAGE -> BadgeConfig(
            icon = Icons.Filled.ChatBubbleOutline,
            containerColor = Color(0xFF06B6D4),
            iconTint = Color.White
        )
        NotificationType.POST_HIDDEN -> null
    }
}

@Composable
private fun NotificationsLoadingSkeleton() {
    val spacing = LokapTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(6) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Preview(name = "Notifications - Đầy đủ", showBackground = true)
@Composable
private fun NotificationsScreenPopulatedPreview() {
    LokapTheme {
        val all = SampleData.notifications
        NotificationsScreen(
            uiState = NotificationsUiState(
                unreadNotifications = all.filter { !it.isRead },
                earlierNotifications = all.filter { it.isRead }
            ),
            onAction = {}
        )
    }
}
