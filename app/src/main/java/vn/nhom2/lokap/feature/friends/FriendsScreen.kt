package vn.nhom2.lokap.feature.friends

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
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
import vn.nhom2.lokap.core.designsystem.component.*
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.core.navigation.navigateSingleTop
import vn.nhom2.lokap.core.navigation.popBackStackSafely
import vn.nhom2.lokap.data.fake.SampleSocialData
import vn.nhom2.lokap.domain.model.*
fun NavGraphBuilder.friendsScreen(navController: NavController) {
    composable<Route.Friends> {
        FriendsRoute(
            onNavigateBack = { navController.popBackStackSafely() },
            onNavigateToProfile = { userId ->
                navController.navigateSingleTop(Route.Profile(userId = userId))
            }
        )
    }
}

@Composable
fun FriendsRoute(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    viewModel: FriendsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                FriendsEffect.NavigateBack -> onNavigateBack()
                is FriendsEffect.NavigateToProfile -> onNavigateToProfile(effect.userId)
                is FriendsEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    FriendsScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState
    )
}

@Composable
fun FriendsScreen(
    uiState: FriendsUiState,
    onAction: (FriendsAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(LokapTheme.colors.background)
            .systemBarsPadding(),
        containerColor = LokapTheme.colors.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            FriendsTopBar(
                friendCount = uiState.friends.size,
                maxFriends = uiState.maxFriends,
                onBackClick = { onAction(FriendsAction.NavigateBack) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isOffline) {
                OfflineBanner(modifier = Modifier.fillMaxWidth())
            }

            FriendsSearchBar(
                query = uiState.searchQuery,
                onQueryChange = { onAction(FriendsAction.ChangeSearchQuery(it)) },
                onClear = { onAction(FriendsAction.ClearSearch) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = LokapTheme.spacing.md,
                        vertical = LokapTheme.spacing.xs
                    )
            )

            if (uiState.isFriendLimitReached) {
                FriendLimitWarningBanner(
                    maxFriends = uiState.maxFriends,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = LokapTheme.spacing.md,
                            vertical = LokapTheme.spacing.xs
                        )
                )
            }

            when {
                uiState.isLoading -> {
                    FriendsLoadingList(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = LokapTheme.spacing.md)
                    )
                }

                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        ErrorState(
                            message = uiState.errorMessage,
                            onRetry = { onAction(FriendsAction.Retry) },
                            modifier = Modifier.padding(LokapTheme.spacing.lg)
                        )
                    }
                }

                uiState.isSearching -> {
                    if (uiState.searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyState(
                                title = "Không có ai tên như vậy",
                                subtitle = "Thử kiểm tra lại @username xem có gõ nhầm ký tự nào không nhé.",
                                actionLabel = "Xoá tìm kiếm",
                                onActionClick = { onAction(FriendsAction.ClearSearch) },
                                modifier = Modifier.padding(LokapTheme.spacing.lg)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = LokapTheme.spacing.md,
                                vertical = LokapTheme.spacing.sm
                            ),
                            verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
                        ) {
                            item {
                                SectionHeaderRow(
                                    title = "Kết quả tìm kiếm",
                                    badgeCount = uiState.searchResults.size
                                )
                            }
                            items(
                                items = uiState.searchResults,
                                key = { "search_${it.user.id}" }
                            ) { item ->
                                SearchResultUserRow(
                                    item = item,
                                    isFriendLimitReached = uiState.isFriendLimitReached,
                                    onAction = onAction
                                )
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = LokapTheme.spacing.md,
                            end = LokapTheme.spacing.md,
                            top = LokapTheme.spacing.sm,
                            bottom = LokapTheme.spacing.xxl
                        ),
                        verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
                    ) {
                        if (uiState.incomingRequests.isNotEmpty()) {
                            item(key = "header_incoming") {
                                SectionHeaderRow(
                                    title = "Lời mời kết bạn",
                                    badgeCount = uiState.incomingRequests.size
                                )
                            }
                            items(
                                items = uiState.incomingRequests,
                                key = { "incoming_${it.id}" }
                            ) { user ->
                                IncomingRequestRow(
                                    user = user,
                                    isFriendLimitReached = uiState.isFriendLimitReached,
                                    onOpenProfile = { onAction(FriendsAction.OpenProfile(user.id)) },
                                    onAccept = { onAction(FriendsAction.AcceptRequest(user.id)) },
                                    onDecline = { onAction(FriendsAction.DeclineRequest(user.id)) }
                                )
                            }
                            item(key = "spacer_incoming") {
                                Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))
                            }
                        }

                        if (uiState.sentRequests.isNotEmpty()) {
                            item(key = "header_sent") {
                                SectionHeaderRow(
                                    title = "Đã gửi",
                                    badgeCount = uiState.sentRequests.size
                                )
                            }
                            items(
                                items = uiState.sentRequests,
                                key = { "sent_${it.id}" }
                            ) { user ->
                                SentRequestRow(
                                    user = user,
                                    onOpenProfile = { onAction(FriendsAction.OpenProfile(user.id)) },
                                    onCancel = { onAction(FriendsAction.CancelSentRequest(user.id)) }
                                )
                            }
                            item(key = "spacer_sent") {
                                Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))
                            }
                        }

                        item(key = "header_friends") {
                            SectionHeaderRow(
                                title = "Bạn bè (${uiState.friends.size})",
                                badgeCount = 0
                            )
                        }

                        if (uiState.friends.isEmpty()) {
                            item(key = "empty_friends") {
                                EmptyState(
                                    title = "Rủ bạn bè cài Lokap nhé",
                                    subtitle = "Kết nối tối đa 20 người bạn thân nhất để bắn ảnh trực tiếp và gom dấu chân khắp 34 tỉnh thành!",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = LokapTheme.spacing.xl)
                                )
                            }
                        } else {
                            items(
                                items = uiState.friends,
                                key = { "friend_${it.id}" }
                            ) { friend ->
                                FriendRowItem(
                                    user = friend,
                                    onClick = { onAction(FriendsAction.OpenProfile(friend.id)) },
                                    onRequestUnfriend = { onAction(FriendsAction.RequestUnfriendConfirm(friend)) },
                                    onRequestBlock = { onAction(FriendsAction.RequestBlockConfirm(friend)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    uiState.pendingConfirmation?.let { pending ->
        FriendConfirmationDialog(
            pending = pending,
            onDismiss = { onAction(FriendsAction.DismissConfirmDialog) },
            onConfirm = { onAction(FriendsAction.ConfirmPendingAction) }
        )
    }
}

@Composable
private fun FriendsTopBar(
    friendCount: Int,
    maxFriends: Int,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = LokapTheme.spacing.sm,
                vertical = LokapTheme.spacing.xs
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Quay lại",
                tint = LokapTheme.colors.textPrimary
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Hội bạn thân",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LokapTheme.colors.textPrimary
            )
            Text(
                text = "$friendCount/$maxFriends người bạn trên Lokap",
                style = MaterialTheme.typography.labelSmall,
                color = LokapTheme.colors.textSecondary
            )
        }

        Box(
            modifier = Modifier
                .padding(end = LokapTheme.spacing.sm)
                .clip(LokapTheme.shapes.pill)
                .background(
                    if (friendCount >= maxFriends) {
                        LokapTheme.colors.accentYellow.copy(alpha = 0.2f)
                    } else {
                        LokapTheme.colors.primarySoft
                    }
                )
                .padding(
                    horizontal = LokapTheme.spacing.sm,
                    vertical = LokapTheme.spacing.xs
                )
        ) {
            Text(
                text = "$friendCount/$maxFriends",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (friendCount >= maxFriends) {
                    LokapTheme.colors.accentYellow
                } else {
                    LokapTheme.colors.primary
                }
            )
        }
    }
}

@Composable
private fun FriendsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        shape = LokapTheme.shapes.pill,
        placeholder = {
            Text(
                text = "Tìm bạn bè theo @username...",
                color = LokapTheme.colors.textTertiary
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = LokapTheme.colors.textSecondary
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Xoá",
                        tint = LokapTheme.colors.textSecondary
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = LokapTheme.colors.surface,
            unfocusedContainerColor = LokapTheme.colors.surface,
            focusedBorderColor = LokapTheme.colors.primary,
            unfocusedBorderColor = LokapTheme.colors.outline,
            cursorColor = LokapTheme.colors.primary,
            focusedTextColor = LokapTheme.colors.textPrimary,
            unfocusedTextColor = LokapTheme.colors.textPrimary
        ),
        modifier = modifier
    )
}

@Composable
private fun FriendLimitWarningBanner(
    maxFriends: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(LokapTheme.shapes.card)
            .background(LokapTheme.colors.surfaceHigh)
            .border(
                width = 1.dp,
                color = LokapTheme.colors.accentYellow,
                shape = LokapTheme.shapes.card
            )
            .padding(LokapTheme.spacing.sm)
    ) {
        Text(
            text = "Bạn đã đạt tối đa $maxFriends/$maxFriends người bạn như Locket. Các nút Kết bạn và Chấp nhận tạm khoá cho tới khi bạn bớt danh sách.",
            style = MaterialTheme.typography.bodySmall,
            color = LokapTheme.colors.accentYellow
        )
    }
}

@Composable
private fun SectionHeaderRow(
    title: String,
    badgeCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = LokapTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = LokapTheme.colors.textPrimary
        )
        if (badgeCount > 0) {
            Spacer(modifier = Modifier.width(LokapTheme.spacing.xs))
            BadgeDot(count = badgeCount)
        }
    }
}

@Composable
private fun SearchResultUserRow(
    item: FriendUserItemUi,
    isFriendLimitReached: Boolean,
    onAction: (FriendsAction) -> Unit
) {
    val user = item.user
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LokapTheme.shapes.card)
            .background(LokapTheme.colors.surface)
            .border(1.dp, LokapTheme.colors.outline, LokapTheme.shapes.card)
            .padding(LokapTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarRing(
            avatarUrl = user.avatarUrl,
            displayName = user.displayName,
            size = 44.dp,
            onClick = { onAction(FriendsAction.OpenProfile(user.id)) }
        )

        Spacer(modifier = Modifier.width(LokapTheme.spacing.sm))

        Column(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(
                    onClick = { onAction(FriendsAction.OpenProfile(user.id)) }
                )
        ) {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = LokapTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.bodySmall,
                color = LokapTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(LokapTheme.spacing.xs))

        when (item.status) {
            FriendStatus.NONE -> {
                PinkButton(
                    text = "Kết bạn",
                    leadingIcon = Icons.Default.Add,
                    enabled = !isFriendLimitReached,
                    onClick = { onAction(FriendsAction.SendRequest(user.id)) }
                )
            }

            FriendStatus.REQUEST_SENT -> {
                GhostButton(
                    text = "Huỷ",
                    onClick = { onAction(FriendsAction.CancelSentRequest(user.id)) }
                )
            }

            FriendStatus.REQUEST_RECEIVED -> {
                Row(horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.xs)) {
                    PinkButton(
                        text = "Chấp nhận",
                        enabled = !isFriendLimitReached,
                        onClick = { onAction(FriendsAction.AcceptRequest(user.id)) }
                    )
                    GhostButton(
                        text = "Từ chối",
                        onClick = { onAction(FriendsAction.DeclineRequest(user.id)) }
                    )
                }
            }

            FriendStatus.FRIENDS -> {
                GhostButton(
                    text = "Bạn bè",
                    onClick = { onAction(FriendsAction.OpenProfile(user.id)) }
                )
            }

            FriendStatus.BLOCKED -> {
                GhostButton(
                    text = "Bỏ chặn",
                    onClick = { onAction(FriendsAction.UnblockUser(user.id)) }
                )
            }
        }
    }
}

@Composable
private fun IncomingRequestRow(
    user: User,
    isFriendLimitReached: Boolean,
    onOpenProfile: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LokapTheme.shapes.card)
            .background(LokapTheme.colors.surface)
            .border(1.dp, LokapTheme.colors.primarySoft, LokapTheme.shapes.card)
            .padding(LokapTheme.spacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarRing(
                avatarUrl = user.avatarUrl,
                displayName = user.displayName,
                size = 44.dp,
                hasNewPost = true,
                onClick = onOpenProfile
            )

            Spacer(modifier = Modifier.width(LokapTheme.spacing.sm))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(onClick = onOpenProfile)
            ) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = LokapTheme.colors.textPrimary
                )
                Text(
                    text = "@${user.username} muốn kết bạn",
                    style = MaterialTheme.typography.bodySmall,
                    color = LokapTheme.colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
        ) {
            PinkButton(
                text = "Chấp nhận",
                leadingIcon = Icons.Default.Check,
                enabled = !isFriendLimitReached,
                onClick = onAccept,
                modifier = Modifier.weight(1f)
            )
            GhostButton(
                text = "Từ chối",
                leadingIcon = Icons.Default.Close,
                onClick = onDecline,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SentRequestRow(
    user: User,
    onOpenProfile: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LokapTheme.shapes.card)
            .background(LokapTheme.colors.surface)
            .border(1.dp, LokapTheme.colors.outline, LokapTheme.shapes.card)
            .padding(LokapTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarRing(
            avatarUrl = user.avatarUrl,
            displayName = user.displayName,
            size = 44.dp,
            onClick = onOpenProfile
        )

        Spacer(modifier = Modifier.width(LokapTheme.spacing.sm))

        Column(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(onClick = onOpenProfile)
        ) {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = LokapTheme.colors.textPrimary
            )
            Text(
                text = "@${user.username} · Đang chờ phản hồi",
                style = MaterialTheme.typography.bodySmall,
                color = LokapTheme.colors.textSecondary
            )
        }

        GhostButton(
            text = "Huỷ",
            onClick = onCancel
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FriendRowItem(
    user: User,
    onClick: () -> Unit,
    onRequestUnfriend: () -> Unit,
    onRequestBlock: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(LokapTheme.shapes.card)
                .background(LokapTheme.colors.surface)
                .border(1.dp, LokapTheme.colors.outline, LokapTheme.shapes.card)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true }
                )
                .padding(LokapTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarRing(
                avatarUrl = user.avatarUrl,
                displayName = user.displayName,
                size = 44.dp,
                onClick = onClick
            )

            Spacer(modifier = Modifier.width(LokapTheme.spacing.sm))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = LokapTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${user.username} · Giữ lâu để tuỳ chọn",
                    style = MaterialTheme.typography.bodySmall,
                    color = LokapTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Tuỳ chọn bạn bè",
                    tint = LokapTheme.colors.textSecondary
                )
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            modifier = Modifier.background(LokapTheme.colors.surfaceHigh)
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Huỷ kết bạn",
                        color = LokapTheme.colors.textPrimary
                    )
                },
                onClick = {
                    menuExpanded = false
                    onRequestUnfriend()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Chặn người này",
                        color = LokapTheme.colors.error
                    )
                },
                onClick = {
                    menuExpanded = false
                    onRequestBlock()
                }
            )
        }
    }
}

@Composable
private fun FriendConfirmationDialog(
    pending: PendingFriendConfirmation,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val isBlock = pending.actionType == FriendConfirmActionType.BLOCK
    val title = if (isBlock) {
        "Chặn ${pending.user.displayName}?"
    } else {
        "Huỷ kết bạn với ${pending.user.displayName}?"
    }
    val description = if (isBlock) {
        "Người này sẽ không thể gửi ảnh Locket, nhắn tin hay tìm thấy hồ sơ của bạn nữa. Bạn có thể bỏ chặn bất cứ lúc nào trong phần Cài đặt."
    } else {
        "Hai bạn sẽ không còn nhận được ảnh mới của nhau trên màn hình chính nữa."
    }
    val confirmLabel = if (isBlock) "Chặn ngay" else "Huỷ kết bạn"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LokapTheme.colors.surface,
        titleContentColor = LokapTheme.colors.textPrimary,
        textContentColor = LokapTheme.colors.textSecondary,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Giữ lại",
                    color = LokapTheme.colors.textSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    color = LokapTheme.colors.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
private fun FriendsLoadingList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
    ) {
        Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))
        repeat(6) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                shape = LokapTheme.shapes.card
            )
        }
    }
}

@Preview(name = "Friends - Đầy đủ danh sách", showBackground = true)
@Composable
private fun FriendsPopulatedPreview() {
    val sampleUsers = SampleSocialData.directoryUsers
    LokapTheme {
        FriendsScreen(
            uiState = FriendsUiState(
                isLoading = false,
                incomingRequests = sampleUsers.take(2),
                sentRequests = sampleUsers.drop(2).take(1),
                friends = sampleUsers.drop(3).take(4)
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Friends - Đủ 20/20 bạn bè", showBackground = true)
@Composable
private fun FriendsMaxLimitPreview() {
    val twentyFriends = (1..20).map { idx ->
        User(
            id = "u_$idx",
            username = "banthen_$idx",
            displayName = "Bạn Thân #$idx",
            avatarUrl = null
        )
    }
    LokapTheme {
        FriendsScreen(
            uiState = FriendsUiState(
                isLoading = false,
                incomingRequests = SampleSocialData.directoryUsers.take(1),
                friends = twentyFriends,
                maxFriends = 20
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Friends - Không tìm thấy username", showBackground = true)
@Composable
private fun FriendsEmptySearchPreview() {
    LokapTheme {
        FriendsScreen(
            uiState = FriendsUiState(
                isLoading = false,
                searchQuery = "khong_ton_tai_99",
                debouncedQuery = "khong_ton_tai_99",
                searchResults = emptyList()
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Friends - Chưa có bạn", showBackground = true)
@Composable
private fun FriendsNoFriendsPreview() {
    LokapTheme {
        FriendsScreen(
            uiState = FriendsUiState(
                isLoading = false,
                incomingRequests = emptyList(),
                sentRequests = emptyList(),
                friends = emptyList()
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Friends - Đang tải & Lỗi", showBackground = true)
@Composable
private fun FriendsErrorPreview() {
    LokapTheme {
        FriendsScreen(
            uiState = FriendsUiState(
                isLoading = false,
                errorMessage = "Mất kết nối tới máy chủ Lokap."
            ),
            onAction = {}
        )
    }
}
