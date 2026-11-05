package vn.nhom2.lokap.feature.inbox

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import vn.nhom2.lokap.core.common.formatTimeAgo
import vn.nhom2.lokap.core.designsystem.component.AvatarRing
import vn.nhom2.lokap.core.designsystem.component.BadgeDot
import vn.nhom2.lokap.core.designsystem.component.EmptyState
import vn.nhom2.lokap.core.designsystem.component.ErrorState
import vn.nhom2.lokap.core.designsystem.component.GlassChip
import vn.nhom2.lokap.core.designsystem.component.OfflineBanner
import vn.nhom2.lokap.core.designsystem.component.ShimmerBox
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.core.navigation.navigateSingleTop
import vn.nhom2.lokap.core.navigation.popBackStackSafely
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.User

fun NavGraphBuilder.inboxScreen(navController: NavController) {
    composable<Route.Inbox> {
        InboxRoute(
            onNavigateToChat = { conversationId ->
                navController.navigateSingleTop(Route.Chat(conversationId = conversationId))
            },
            onNavigateBack = {
                navController.popBackStackSafely()
            }
        )
    }
}

@Composable
fun InboxRoute(
    onNavigateToChat: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: InboxViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is InboxEffect.NavigateToChat -> onNavigateToChat(effect.conversationId)
                InboxEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    InboxScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    uiState: InboxUiState,
    onAction: (InboxAction) -> Unit,
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
        InboxTopBar(
            onBackClick = { onAction(InboxAction.NavigateBack) },
            onNewMessageClick = { onAction(InboxAction.OpenNewMessageSheet) }
        )

        if (uiState.isOffline) {
            OfflineBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.md, vertical = spacing.xs)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                uiState.isLoading -> {
                    InboxLoadingList()
                }

                uiState.errorMessage != null -> {
                    ErrorState(
                        message = uiState.errorMessage,
                        onRetry = { onAction(InboxAction.Retry) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.lg)
                    )
                }

                uiState.items.isEmpty() -> {
                    EmptyState(
                        title = "Chưa có tin nhắn. Trả lời một bức ảnh để bắt đầu",
                        subtitle = "Hoặc nhấn Soạn mới để gửi lời chào đến hội bạn thân",
                        actionLabel = "Soạn mới",
                        onActionClick = { onAction(InboxAction.OpenNewMessageSheet) },
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
                        items(
                            items = uiState.items,
                            key = { it.conversation.id }
                        ) { item ->
                            SwipeableConversationRow(
                                item = item,
                                now = uiState.now,
                                onClick = {
                                    onAction(InboxAction.OpenConversation(item.conversation.id))
                                },
                                onMarkRead = {
                                    onAction(InboxAction.MarkAsRead(item.conversation.id))
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.isNewMessageSheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onAction(InboxAction.DismissNewMessageSheet) },
            sheetState = sheetState,
            shape = LokapTheme.shapes.sheet,
            containerColor = colors.surface,
            contentColor = colors.textPrimary
        ) {
            NewMessageFriendPickerSheet(
                friends = uiState.friends,
                onSelectFriend = { friend ->
                    onAction(InboxAction.SelectFriendToChat(friend))
                }
            )
        }
    }
}

@Composable
private fun InboxTopBar(
    onBackClick: () -> Unit,
    onNewMessageClick: () -> Unit
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
                text = "Tin nhắn",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )
        }

        GlassChip(
            label = "Soạn mới",
            leadingIcon = Icons.Default.Add,
            selected = true,
            onClick = onNewMessageClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableConversationRow(
    item: InboxConversationItem,
    now: java.time.Instant,
    onClick: () -> Unit,
    onMarkRead: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onMarkRead()
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shapes.card)
                    .background(colors.surfaceHigh)
                    .padding(horizontal = spacing.md),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextButton(onClick = onMarkRead) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(spacing.xs))
                    Text(
                        text = "Đánh dấu đã đọc",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.primary
                    )
                }
            }
        }
    ) {
        ConversationRowContent(
            item = item,
            now = now,
            onClick = onClick
        )
    }
}

@Composable
private fun ConversationRowContent(
    item: InboxConversationItem,
    now: java.time.Instant,
    onClick: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes
    val conv = item.conversation
    val isUnread = conv.unreadCount > 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.card)
            .background(if (isUnread) colors.surfaceHigh else colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarRing(
            avatarUrl = conv.friend.avatarUrl,
            displayName = conv.friend.displayName,
            size = 52.dp,
            hasNewPost = item.hasNewPostFromFriend
        )

        Spacer(modifier = Modifier.width(spacing.md))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conv.friend.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(spacing.sm))

                Text(
                    text = formatTimeAgo(conv.lastMessageAt, now),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isUnread) colors.primary else colors.textTertiary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.displayPreview,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (isUnread) colors.textPrimary else colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (conv.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(spacing.sm))
                    BadgeDot(
                        count = conv.unreadCount,
                        modifier = Modifier
                    )
                }
            }
        }
    }
}

@Composable
private fun NewMessageFriendPickerSheet(
    friends: List<User>,
    onSelectFriend: (User) -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.sm)
    ) {
        Text(
            text = "Trò chuyện mới",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = spacing.xs, vertical = spacing.xs)
        )
        Text(
            text = "Chọn một người bạn để gửi tin nhắn hoặc ảnh Locket riêng",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = spacing.xs)
        )

        Spacer(modifier = Modifier.height(spacing.md))

        if (friends.isEmpty()) {
            EmptyState(
                title = "Chưa có bạn bè nào",
                subtitle = "Hãy kết bạn để bắt đầu nhắn tin trên Lokap",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.xl)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = spacing.xl),
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                items(
                    items = friends,
                    key = { it.id }
                ) { friend ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.card)
                            .background(colors.surfaceHigh)
                            .clickable { onSelectFriend(friend) }
                            .padding(horizontal = spacing.md, vertical = spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarRing(
                            avatarUrl = friend.avatarUrl,
                            displayName = friend.displayName,
                            size = 44.dp
                        )
                        Spacer(modifier = Modifier.width(spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = friend.displayName,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "@${friend.username}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                        Text(
                            text = "Nhắn tin",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = colors.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InboxLoadingList() {
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
                    .height(76.dp),
                shape = shapes.card
            )
        }
    }
}

@Preview(name = "Inbox - Đầy đủ", showBackground = true)
@Composable
private fun InboxScreenPopulatedPreview() {
    LokapTheme {
        val sampleItems = SampleData.conversations.mapIndexed { index, conv ->
            InboxConversationItem(
                conversation = conv,
                displayPreview = if (index == 0) "📷 Đã gửi một ảnh" else conv.lastMessagePreview,
                hasNewPostFromFriend = index % 2 == 0
            )
        }
        InboxScreen(
            uiState = InboxUiState(
                items = sampleItems,
                friends = SampleData.users
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Inbox - Trống", showBackground = true)
@Composable
private fun InboxScreenEmptyPreview() {
    LokapTheme {
        InboxScreen(
            uiState = InboxUiState(items = emptyList()),
            onAction = {}
        )
    }
}

@Preview(name = "Inbox - Đang tải", showBackground = true)
@Composable
private fun InboxScreenLoadingPreview() {
    LokapTheme {
        InboxScreen(
            uiState = InboxUiState(isLoading = true),
            onAction = {}
        )
    }
}

@Preview(name = "Inbox - Lỗi & Offline", showBackground = true)
@Composable
private fun InboxScreenErrorOfflinePreview() {
    LokapTheme {
        InboxScreen(
            uiState = InboxUiState(
                isOffline = true,
                errorMessage = "Không thể đồng bộ hộp thư. Kiểm tra kết nối mạng của bạn."
            ),
            onAction = {}
        )
    }
}
