package vn.nhom2.lokap.feature.inbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import vn.nhom2.lokap.domain.model.User
fun NavGraphBuilder.inboxScreen(navController: NavController) {
    composable<Route.Inbox> {
        InboxRoute(
            onNavigateToChat = { conversationId ->
                navController.navigateSingleTop(Route.Chat(conversationId = conversationId))
            },
            onNavigateBack = {
                navController.popBackStackSafely()
            },
            onNavigateToTab = { tab ->
                when (tab) {
                    MainTab.FEED -> navController.navigateSingleTop(Route.Feed)
                    MainTab.MAP -> navController.navigateSingleTop(Route.Map)
                    MainTab.CAMERA -> navController.navigateSingleTop(Route.Camera)
                    MainTab.INBOX -> Unit
                    MainTab.PROFILE -> navController.navigateSingleTop(Route.Profile())
                }
            }
        )
    }
}

@Composable
fun InboxRoute(
    onNavigateToChat: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToTab: (MainTab) -> Unit = {},
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
        onAction = viewModel::onAction,
        onNavigateToTab = onNavigateToTab
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    uiState: InboxUiState,
    onAction: (InboxAction) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToTab: (MainTab) -> Unit = {}
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems = remember(uiState.items, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.items
        } else {
            uiState.items.filter { item ->
                item.conversation.friend.displayName.contains(searchQuery, ignoreCase = true) ||
                    item.displayPreview.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val totalUnread = remember(uiState.items) {
        uiState.items.sumOf { it.conversation.unreadCount }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
    ) {
        InboxTopBar(
            totalUnreadCount = totalUnread,
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

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "Tìm kiếm cuộc trò chuyện...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textTertiary
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Xoá",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surfaceHigh,
                unfocusedContainerColor = colors.surfaceHigh,
                focusedBorderColor = colors.primary.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg, vertical = spacing.xs)
        )

        if (uiState.friends.isNotEmpty() && searchQuery.isBlank()) {
            ActiveFriendsStoriesBar(
                friends = uiState.friends,
                onSelectFriend = { friend ->
                    onAction(InboxAction.SelectFriendToChat(friend))
                },
                onCameraClick = {
                    onNavigateToTab(MainTab.CAMERA)
                }
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
                        title = "Chưa có tin nhắn nào",
                        subtitle = "Chụp một bức ảnh hoặc nhấn Soạn tin để gửi lời chào đến hội bạn thân",
                        actionLabel = "Soạn tin mới",
                        onActionClick = { onAction(InboxAction.OpenNewMessageSheet) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.lg)
                    )
                }

                filteredItems.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.lg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Không tìm thấy cuộc trò chuyện nào phù hợp",
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
                        items(
                            items = filteredItems,
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

        Spacer(
            modifier = Modifier
                .navigationBarsPadding()
                .height(64.dp)
        )
    }

    if (uiState.isNewMessageSheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onAction(InboxAction.DismissNewMessageSheet) },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
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
    totalUnreadCount: Int,
    onBackClick: () -> Unit,
    onNewMessageClick: () -> Unit
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
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Hộp thư",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp
                ),
                color = colors.textPrimary
            )

            if (totalUnreadCount > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.primary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "$totalUnreadCount mới",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.primary,
            shadowElevation = 2.dp,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onNewMessageClick)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = colors.buttonText,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Soạn tin",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.buttonText
                )
            }
        }
    }
}

@Composable
private fun ActiveFriendsStoriesBar(
    friends: List<User>,
    onSelectFriend: (User) -> Unit,
    onCameraClick: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.xs),
        contentPadding = PaddingValues(horizontal = spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        item(key = "quick_camera") {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onCameraClick)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceHigh)
                        .border(1.5.dp, colors.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Chụp nhanh",
                        tint = colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "Chụp ảnh",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = colors.textSecondary,
                    maxLines = 1
                )
            }
        }

        items(items = friends, key = { it.id }) { friend ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .width(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectFriend(friend) }
            ) {
                AvatarRing(
                    avatarUrl = friend.avatarUrl,
                    displayName = friend.displayName,
                    size = 52.dp
                )
                Text(
                    text = friend.displayName.split(" ").lastOrNull() ?: friend.displayName,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.5f }
    )

    LaunchedEffect(dismissState.targetValue) {
        if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
            onMarkRead()
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
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
                        text = "Đã đọc",
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
    val conv = item.conversation
    val isUnread = conv.unreadCount > 0

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isUnread) colors.surfaceHigh else colors.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isUnread) colors.primary.copy(alpha = 0.35f) else colors.outline.copy(alpha = 0.2f)
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AvatarRing(
                    avatarUrl = conv.friend.avatarUrl,
                    displayName = conv.friend.displayName,
                    size = 50.dp,
                    hasNewPost = item.hasNewPostFromFriend
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conv.friend.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isUnread) FontWeight.ExtraBold else FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = formatTimeAgo(conv.lastMessageAt, now),
                        style = MaterialTheme.typography.labelSmall,
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
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 13.sp
                        ),
                        color = if (isUnread) colors.textPrimary else colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (conv.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = colors.primary
                        ) {
                            Text(
                                text = conv.unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.buttonText,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
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
    var query by remember { mutableStateOf("") }

    val filtered = remember(friends, query) {
        if (query.isBlank()) friends
        else friends.filter {
            it.displayName.contains(query, ignoreCase = true) ||
                it.username.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.sm)
    ) {
        Text(
            text = "Trò chuyện mới",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = colors.textPrimary,
            modifier = Modifier.padding(vertical = spacing.xs)
        )
        Text(
            text = "Chọn bạn bè để bắt đầu gửi ảnh và tin nhắn riêng",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Tìm tên bạn bè...", color = colors.textTertiary) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surfaceHigh,
                unfocusedContainerColor = colors.surfaceHigh,
                focusedBorderColor = colors.primary.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(spacing.md))

        if (filtered.isEmpty()) {
            EmptyState(
                title = if (query.isBlank()) "Chưa có bạn bè nào" else "Không tìm thấy kết quả",
                subtitle = "Hãy kết bạn thêm để bắt đầu trò chuyện trên Lokap",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.lg)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = spacing.xl),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = filtered,
                    key = { it.id }
                ) { friend ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceHigh.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectFriend(friend) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarRing(
                                avatarUrl = friend.avatarUrl,
                                displayName = friend.displayName,
                                size = 44.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = friend.displayName,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = friend.username,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = colors.primary.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "Nhắn tin",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = colors.primary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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
private fun InboxLoadingList() {
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes

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
                shape = shapes.card
            )
        }
    }
}

@Preview(name = "Inbox - Đầy đủ", showBackground = true)
@Composable
private fun InboxScreenPopulatedPreview() {
    LokapTheme {
        InboxScreen(
            uiState = InboxUiState(
                items = listOf(
                    InboxConversationItem(
                        conversation = SampleData.conversations.first(),
                        displayPreview = "Alo cà phê cuối tuần không?",
                        hasNewPostFromFriend = true
                    )
                ),
                friends = SampleData.users.drop(1)
            ),
            onAction = {}
        )
    }
}
