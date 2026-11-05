package vn.nhom2.lokap.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import vn.nhom2.lokap.core.common.formatTimeAgo
import vn.nhom2.lokap.core.designsystem.component.AvatarRing
import vn.nhom2.lokap.core.designsystem.component.EmptyState
import vn.nhom2.lokap.core.designsystem.component.ErrorState
import vn.nhom2.lokap.core.designsystem.component.GhostButton
import vn.nhom2.lokap.core.designsystem.component.GlassChip
import vn.nhom2.lokap.core.designsystem.component.PinkButton
import vn.nhom2.lokap.core.designsystem.component.ShimmerBox
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.core.navigation.navigateSingleTop
import vn.nhom2.lokap.core.navigation.popBackStackSafely
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.AppNotification
import vn.nhom2.lokap.domain.model.NotificationType

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
                        subtitle = "Khi bạn bè thả cảm xúc hoặc đăng ảnh Locket mới, thông báo sẽ hiện tại đây",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.lg)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = spacing.md,
                            vertical = spacing.sm
                        ),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        if (uiState.unreadNotifications.isNotEmpty()) {
                            item(key = "section_new") {
                                NotificationSectionHeader(title = "Mới")
                            }
                            items(
                                items = uiState.unreadNotifications,
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

                        if (uiState.earlierNotifications.isNotEmpty()) {
                            item(key = "section_earlier") {
                                NotificationSectionHeader(title = "Trước đó")
                            }
                            items(
                                items = uiState.earlierNotifications,
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
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = colors.textPrimary
                )
            }
            Text(
                text = "Thông báo",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )
        }

        GlassChip(
            label = "Đọc tất cả",
            leadingIcon = Icons.Default.Done,
            selected = hasUnread,
            onClick = if (hasUnread) onMarkAllReadClick else null
        )
    }
}

@Composable
private fun NotificationSectionHeader(title: String) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = colors.textSecondary,
        modifier = Modifier.padding(
            horizontal = spacing.xs,
            vertical = spacing.sm
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
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes
    val isUnread = !notification.isRead

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.card)
            .background(if (isUnread) colors.surfaceHigh else colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (isUnread) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.primary)
                )
            }
        }

        if (notification.type == NotificationType.POST_HIDDEN) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Bài đăng bị ẩn",
                    tint = colors.accentYellow
                )
            }
        } else {
            AvatarRing(
                avatarUrl = notification.actor?.avatarUrl,
                displayName = notification.actor?.displayName ?: "Lokap",
                size = 44.dp
            )
        }

        Spacer(modifier = Modifier.width(spacing.md))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            Text(
                text = notification.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = colors.textPrimary
            )

            Text(
                text = formatTimeAgo(notification.createdAt, now),
                style = MaterialTheme.typography.labelSmall,
                color = if (isUnread) colors.primary else colors.textTertiary
            )

            if (notification.type == NotificationType.FRIEND_REQUEST) {
                Spacer(modifier = Modifier.height(spacing.xs))
                if (friendRequestDecision == null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
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
                        "Đã chấp nhận lời mời kết bạn"
                    } else {
                        "Đã từ chối lời mời kết bạn"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (friendRequestDecision) colors.success else colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationsLoadingSkeleton() {
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        repeat(6) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp),
                    shape = shapes.card
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

@Preview(name = "Notifications - Trống", showBackground = true)
@Composable
private fun NotificationsScreenEmptyPreview() {
    LokapTheme {
        NotificationsScreen(
            uiState = NotificationsUiState(),
            onAction = {}
        )
    }
}

@Preview(name = "Notifications - Đang tải", showBackground = true)
@Composable
private fun NotificationsScreenLoadingPreview() {
    LokapTheme {
        NotificationsScreen(
            uiState = NotificationsUiState(isLoading = true),
            onAction = {}
        )
    }
}

@Preview(name = "Notifications - Lỗi", showBackground = true)
@Composable
private fun NotificationsScreenErrorPreview() {
    LokapTheme {
        NotificationsScreen(
            uiState = NotificationsUiState(
                errorMessage = "Không thể tải thông báo. Vui lòng thử lại sau."
            ),
            onAction = {}
        )
    }
}
