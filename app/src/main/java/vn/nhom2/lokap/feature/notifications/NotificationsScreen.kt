package vn.nhom2.lokap.feature.notifications

import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.MarkEmailUnread
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.material3.SnackbarHost
import kotlinx.coroutines.launch
import androidx.compose.material3.SnackbarHostState
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import vn.nhom2.lokap.core.common.formatTimeAgo
import vn.nhom2.lokap.core.designsystem.component.*
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.core.navigation.navigateSingleTop
import vn.nhom2.lokap.core.navigation.popBackStackSafely
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
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is NotificationsEffect.NavigateToPostDetail -> onNavigateToPostDetail(effect.postId)
                NotificationsEffect.NavigateToFriends -> onNavigateToFriends()
                NotificationsEffect.NavigateToInbox -> onNavigateToInbox()
                is NotificationsEffect.NavigateToChat -> onNavigateToChat(effect.conversationId)
                NotificationsEffect.NavigateBack -> onNavigateBack()
                is NotificationsEffect.ShowMessage -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    launch {
                        val result = snackbarHostState.showSnackbar(effect.message,
                            actionLabel = if (effect.retryFriendStatuses) "Thử lại" else null)
                        if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                            viewModel.onAction(NotificationsAction.Retry)
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NotificationsScreen(
            uiState = uiState,
            onAction = viewModel::onAction
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
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
                                    friendRequestDecision = uiState.friendRequestDecision(notification),
                                    isRequestBusy = !uiState.isGraphChecked || notification.actor?.id in uiState.pendingFriendRequestActorIds,
                                    onToggleRead = { onAction(NotificationsAction.ToggleRead(notification)) },
                                    onDelete = { onAction(NotificationsAction.DeleteNotification(notification.id)) },
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
                                    friendRequestDecision = uiState.friendRequestDecision(notification),
                                    isRequestBusy = !uiState.isGraphChecked || notification.actor?.id in uiState.pendingFriendRequestActorIds,
                                    onToggleRead = { onAction(NotificationsAction.ToggleRead(notification)) },
                                    onDelete = { onAction(NotificationsAction.DeleteNotification(notification.id)) },
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
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBackClick)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Text(
                text = "Thông báo",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.5.sp
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
                        modifier = Modifier.size(18.dp)
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
    isRequestBusy: Boolean = false,
    onClick: () -> Unit,
    onAcceptFriend: () -> Unit,
    onDeclineFriend: () -> Unit,
    onToggleRead: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val colors = LokapTheme.colors
    val isUnread = !notification.isRead
    val isFriendRequest = notification.type == NotificationType.FRIEND_REQUEST
    val isPendingRequest = isFriendRequest && friendRequestDecision == null

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
        // Một hàng căn giữa theo chiều dọc: avatar · nội dung · nút bên phải (✕ ✓ hoặc ⋮)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
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
                            modifier = Modifier.size(30.dp)
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
                            modifier = Modifier.size(15.dp)
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
                        fontSize = 15.5.sp
                    ),
                    color = colors.textPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isUnread) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.primary)
                        )
                    }
                    Text(
                        text = formatTimeAgo(notification.createdAt, now),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUnread) colors.primary else colors.textTertiary,
                        maxLines = 1
                    )
                    // Lời mời đã xử lý: chỉ còn dấu ✓ / ✕ nhỏ cạnh giờ
                    if (isFriendRequest && friendRequestDecision != null) {
                        Icon(
                            imageVector = if (friendRequestDecision) Icons.Rounded.Check else Icons.Rounded.Close,
                            contentDescription = if (friendRequestDecision) "Đã kết bạn" else "Lời mời đã đóng",
                            tint = if (friendRequestDecision) colors.success else colors.textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            if (isPendingRequest) {
                Row(
                    modifier = Modifier.padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RequestDecisionButton(
                        icon = Icons.Rounded.Close,
                        description = "Từ chối lời mời kết bạn",
                        highlighted = false,
                        enabled = !isRequestBusy,
                        onClick = onDeclineFriend
                    )
                    RequestDecisionButton(
                        icon = Icons.Rounded.Check,
                        description = "Chấp nhận lời mời kết bạn",
                        highlighted = true,
                        enabled = !isRequestBusy,
                        onClick = onAcceptFriend
                    )
                }
            } else {
                NotificationMenu(
                    notificationId = notification.id,
                    isRead = notification.isRead,
                    onToggleRead = onToggleRead,
                    onDelete = onDelete
                )
            }
        }
    }
}

/** Nút tròn ✕ / ✓ cho lời mời kết bạn: 44dp, vùng chạm 48dp nhờ khoảng cách quanh nút. */
@Composable
private fun RequestDecisionButton(
    icon: ImageVector,
    description: String,
    highlighted: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = LokapTheme.colors
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (highlighted && enabled) colors.primary else colors.surface)
            .border(1.5.dp, if (highlighted && enabled) colors.primary else colors.outline, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (!enabled) colors.textTertiary else if (highlighted) colors.onPrimary else colors.textPrimary,
            modifier = Modifier.size(26.dp)
        )
    }
}

/** ⋮ ở cuối hàng: đánh dấu đã đọc / chưa đọc, xoá thông báo (thay hai nút chữ làm lệch hàng). */
@Composable
private fun NotificationMenu(
    notificationId: String,
    isRead: Boolean,
    onToggleRead: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LokapTheme.colors
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.testTag("notification-menu-$notificationId")
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Tuỳ chọn thông báo",
                tint = colors.textSecondary,
                modifier = Modifier.size(26.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = colors.surfaceHigh
        ) {
            DropdownMenuItem(
                text = { Text(if (isRead) "Đánh dấu chưa đọc" else "Đánh dấu đã đọc", color = colors.textPrimary) },
                leadingIcon = {
                    Icon(
                        imageVector = if (isRead) Icons.Rounded.MarkEmailUnread else Icons.Rounded.MarkEmailRead,
                        contentDescription = null,
                        tint = colors.textSecondary
                    )
                },
                onClick = {
                    expanded = false
                    onToggleRead()
                },
                modifier = Modifier.testTag("notification-read-$notificationId")
            )
            DropdownMenuItem(
                text = { Text("Xoá thông báo", color = colors.error) },
                leadingIcon = {
                    Icon(imageVector = Icons.Rounded.DeleteOutline, contentDescription = null, tint = colors.error)
                },
                onClick = {
                    expanded = false
                    onDelete()
                },
                modifier = Modifier.testTag("notification-delete-$notificationId")
            )
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
        NotificationType.COMMENT, NotificationType.REACTION -> BadgeConfig(
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
