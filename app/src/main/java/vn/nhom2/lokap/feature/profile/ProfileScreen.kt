package vn.nhom2.lokap.feature.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import coil3.compose.AsyncImage
import vn.nhom2.lokap.core.designsystem.component.*
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.designsystem.theme.ThemeManager
import vn.nhom2.lokap.core.designsystem.theme.regionGradient
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.core.navigation.navigateSingleTop
import vn.nhom2.lokap.core.navigation.popBackStackSafely
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.*
import vn.nhom2.lokap.feature.map.LokapMap
fun NavGraphBuilder.profileScreen(navController: NavController) {
    composable<Route.Profile> {
        ProfileRoute(
            onNavigateBack = { navController.popBackStackSafely() },
            onNavigateToSettings = {
                navController.navigateSingleTop(Route.Settings)
            },
            onNavigateToPostDetail = { postId ->
                navController.navigateSingleTop(Route.PostDetail(postId))
            },
            onNavigateToChat = { conversationId ->
                navController.navigateSingleTop(Route.Chat(conversationId))
            },
            onNavigateToRegionAlbum = { provinceCode ->
                navController.navigateSingleTop(Route.RegionAlbum(
                        level = RegionLevel.PROVINCE.name,
                        code = provinceCode
                    ))
            },
            onNavigateToTab = { tab ->
                when (tab) {
                    MainTab.FEED -> navController.navigateSingleTop(Route.Feed)
                    MainTab.MAP -> navController.navigateSingleTop(Route.Map)
                    MainTab.CAMERA -> navController.navigateSingleTop(Route.Camera)
                    MainTab.INBOX -> navController.navigateSingleTop(Route.Inbox)
                    MainTab.PROFILE -> Unit
                }
            }
        )
    }
}

@Composable
fun ProfileRoute(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToRegionAlbum: (String) -> Unit,
    onNavigateToTab: (MainTab) -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProfileEffect.NavigateBack -> onNavigateBack()
                ProfileEffect.NavigateToSettings -> onNavigateToSettings()
                is ProfileEffect.NavigateToPostDetail -> onNavigateToPostDetail(effect.postId)
                is ProfileEffect.NavigateToChat -> onNavigateToChat(effect.conversationId)
                is ProfileEffect.NavigateToRegionAlbum -> onNavigateToRegionAlbum(effect.provinceCode)
                is ProfileEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    ProfileScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onNavigateToTab = onNavigateToTab,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToTab: (MainTab) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(LokapTheme.colors.background)
            .systemBarsPadding(),
        containerColor = LokapTheme.colors.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (uiState.isMe) {
                Spacer(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .height(64.dp)
                )
            }
        },
        topBar = {
            ProfileTopBar(
                username = uiState.user?.username,
                isMe = uiState.isMe,
                onBackClick = { onAction(ProfileAction.NavigateBack) },
                onSettingsClick = { onAction(ProfileAction.OpenSettings) }
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

            when {
                uiState.isLoading -> {
                    ProfileLoadingContent(
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
                            onRetry = { onAction(ProfileAction.Retry) },
                            modifier = Modifier.padding(LokapTheme.spacing.lg)
                        )
                    }
                }

                uiState.isUserNotFound || uiState.user == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            title = "Người dùng không tồn tại",
                            subtitle = "Tài khoản này có thể đã đổi tên hoặc không còn hoạt động trên Lokap.",
                            actionLabel = "Quay lại",
                            onActionClick = { onAction(ProfileAction.NavigateBack) },
                            modifier = Modifier.padding(LokapTheme.spacing.lg)
                        )
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = LokapTheme.spacing.xxl),
                        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                        verticalArrangement = Arrangement.spacedBy(1.5.dp)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            ProfileHeaderSection(
                                uiState = uiState,
                                onAction = onAction
                            )
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            ProfileTabSelector(
                                selectedTab = uiState.selectedTab,
                                onSelectTab = { onAction(ProfileAction.SelectTab(it)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        when (uiState.selectedTab) {
                            ProfileTab.PHOTOS -> {
                                if (uiState.visiblePosts.isEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        EmptyState(
                                            title = "Chưa có khoảnh khắc nào",
                                            subtitle = if (uiState.isMe) {
                                                "Mở camera lên và chụp ngay bức ảnh đầu tiên nhé!"
                                            } else {
                                                "Người này chưa chia sẻ khoảnh khắc nào."
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    horizontal = LokapTheme.spacing.md,
                                                    vertical = LokapTheme.spacing.xxl
                                                )
                                        )
                                    }
                                } else {
                                    items(
                                        items = uiState.visiblePosts,
                                        key = { it.id }
                                    ) { post ->
                                        ProfilePhotoGridItem(
                                            post = post,
                                            onClick = { onAction(ProfileAction.ClickPost(post.id)) }
                                        )
                                    }
                                }
                            }

                            ProfileTab.FOOTPRINTS -> {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    ProfileFootprintsSection(
                                        uiState = uiState,
                                        onAction = onAction
                                    )
                                }
                            }

                            ProfileTab.SAVED -> {
                                if (uiState.savedPosts.isEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        EmptyState(
                                            title = "Chưa có bài lưu nào",
                                            subtitle = "Lưu lại những khoảnh khắc bạn yêu thích để xem lại sau.",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    horizontal = LokapTheme.spacing.md,
                                                    vertical = LokapTheme.spacing.xxl
                                                )
                                        )
                                    }
                                } else {
                                    items(
                                        items = uiState.savedPosts,
                                        key = { "saved_${it.id}" }
                                    ) { post ->
                                        ProfilePhotoGridItem(
                                            post = post,
                                            onClick = { onAction(ProfileAction.ClickPost(post.id)) }
                                        )
                                    }
                                }
                            }

                            ProfileTab.REPOSTS -> {
                                if (uiState.repostedPosts.isEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        EmptyState(
                                            title = "Chưa có bài đăng lại",
                                            subtitle = "Các bài viết bạn đăng lại sẽ xuất hiện ở đây.",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    horizontal = LokapTheme.spacing.md,
                                                    vertical = LokapTheme.spacing.xxl
                                                )
                                        )
                                    }
                                } else {
                                    items(
                                        items = uiState.repostedPosts,
                                        key = { "repost_${it.id}" }
                                    ) { post ->
                                        ProfilePhotoGridItem(
                                            post = post,
                                            onClick = { onAction(ProfileAction.ClickPost(post.id)) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.isEditSheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onAction(ProfileAction.DismissEditProfileSheet) },
            sheetState = sheetState,
            shape = LokapTheme.shapes.sheet,
            containerColor = LokapTheme.colors.surface,
            contentColor = LokapTheme.colors.textPrimary
        ) {
            EditProfileBottomSheetContent(
                displayName = uiState.editDisplayNameInput,
                username = uiState.editUsernameInput,
                bio = uiState.editBioInput,
                displayNameError = uiState.editDisplayNameError,
                usernameError = uiState.editUsernameError,
                isSaving = uiState.isSavingProfile,
                onNameChange = { onAction(ProfileAction.ChangeEditDisplayName(it)) },
                onUsernameChange = { onAction(ProfileAction.ChangeEditUsername(it)) },
                onBioChange = { onAction(ProfileAction.ChangeEditBio(it)) },
                onCancel = { onAction(ProfileAction.DismissEditProfileSheet) },
                onSave = { onAction(ProfileAction.SaveEditProfile) }
            )
        }
    }
}

@Composable
private fun ProfileTopBar(
    username: String?,
    isMe: Boolean,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.sm, vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isMe) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = colors.textPrimary
                )
            }
        } else {
            Spacer(modifier = Modifier.width(spacing.md))
        }

        Text(
            text = if (username != null) "@$username" else "Hồ sơ cá nhân",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            ),
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (isMe) {
            Surface(
                shape = CircleShape,
                color = colors.surfaceHigh,
                border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.25f)),
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onSettingsClick)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Cài đặt",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileHeaderSection(
    uiState: ProfileUiState,
    onAction: (ProfileAction) -> Unit
) {
    val user = uiState.user ?: return
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surfaceHigh.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.22f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                AvatarRing(
                    avatarUrl = user.avatarUrl,
                    displayName = user.displayName,
                    size = 76.dp,
                    hasNewPost = uiState.visiblePosts.isNotEmpty(),
                    isOnline = true
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.primary,
                        textAlign = TextAlign.Center
                    )
                }

                val bioText = user.bio.ifBlank { uiState.bio }
                if (bioText.isNotBlank()) {
                    Text(
                        text = bioText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp
                        ),
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.28f)),
                    modifier = Modifier.clip(CircleShape)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Đã check-in ${uiState.visitedProvincesCount}/${uiState.totalProvincesCount} tỉnh thành",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = colors.textPrimary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surface.copy(alpha = 0.7f),
                    border = BorderStroke(0.7.dp, colors.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileStatItem(
                            value = uiState.postCount.toString(),
                            label = "Bài viết"
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(22.dp)
                                .background(colors.outline.copy(alpha = 0.25f))
                        )
                        ProfileStatItem(
                            value = uiState.followersCount.toString(),
                            label = "Người theo dõi"
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(22.dp)
                                .background(colors.outline.copy(alpha = 0.25f))
                        )
                        ProfileStatItem(
                            value = uiState.followingCount.toString(),
                            label = "Đang theo dõi"
                        )
                    }
                }

                if (uiState.isMe) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colors.primary,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clickable { onAction(ProfileAction.OpenEditProfileSheet) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Chỉnh sửa hồ sơ",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clickable { onAction(ProfileAction.OpenSettings) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = null,
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Cài đặt",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    ),
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }
                } else {
                    OtherUserRelationshipBar(
                        friendStatus = uiState.friendStatus,
                        isFriendLimitReached = uiState.isFriendLimitReached,
                        onAction = onAction
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileStatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            ),
            color = colors.textPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            ),
            color = colors.textTertiary
        )
    }
}

@Composable
private fun OtherUserRelationshipBar(
    friendStatus: FriendStatus,
    isFriendLimitReached: Boolean,
    onAction: (ProfileAction) -> Unit
) {
    var friendMenuExpanded by remember { mutableStateOf(false) }

    when (friendStatus) {
        FriendStatus.NONE -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PinkButton(
                    text = "Kết bạn",
                    leadingIcon = Icons.Default.Add,
                    enabled = !isFriendLimitReached,
                    onClick = { onAction(ProfileAction.SendFriendRequest) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (isFriendLimitReached) {
                    Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))
                    Text(
                        text = "Bạn đã đạt giới hạn tối đa 20 bạn bè",
                        style = MaterialTheme.typography.labelSmall,
                        color = LokapTheme.colors.accentYellow
                    )
                }
            }
        }

        FriendStatus.REQUEST_SENT -> {
            GhostButton(
                text = "Đã gửi lời mời · Huỷ",
                leadingIcon = Icons.Default.Close,
                onClick = { onAction(ProfileAction.CancelFriendRequest) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        FriendStatus.REQUEST_RECEIVED -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
                ) {
                    PinkButton(
                        text = "Chấp nhận",
                        leadingIcon = Icons.Default.Check,
                        enabled = !isFriendLimitReached,
                        onClick = { onAction(ProfileAction.AcceptFriendRequest) },
                        modifier = Modifier.weight(1f)
                    )
                    GhostButton(
                        text = "Từ chối",
                        leadingIcon = Icons.Default.Close,
                        onClick = { onAction(ProfileAction.DeclineFriendRequest) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (isFriendLimitReached) {
                    Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))
                    Text(
                        text = "Danh sách bạn bè đã đầy (20/20). Không thể chấp nhận thêm.",
                        style = MaterialTheme.typography.labelSmall,
                        color = LokapTheme.colors.accentYellow
                    )
                }
            }
        }

        FriendStatus.FRIENDS -> {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
                ) {
                    PinkButton(
                        text = "Nhắn tin",
                        onClick = { onAction(ProfileAction.MessageUser) },
                        modifier = Modifier.weight(1f)
                    )
                    GhostButton(
                        text = "Bạn bè",
                        leadingIcon = Icons.Default.Person,
                        onClick = { friendMenuExpanded = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                DropdownMenu(
                    expanded = friendMenuExpanded,
                    onDismissRequest = { friendMenuExpanded = false },
                    modifier = Modifier.background(LokapTheme.colors.surfaceHigh)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Nhắn tin",
                                color = LokapTheme.colors.textPrimary
                            )
                        },
                        onClick = {
                            friendMenuExpanded = false
                            onAction(ProfileAction.MessageUser)
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Huỷ kết bạn",
                                color = LokapTheme.colors.error
                            )
                        },
                        onClick = {
                            friendMenuExpanded = false
                            onAction(ProfileAction.Unfriend)
                        }
                    )
                }
            }
        }

        FriendStatus.BLOCKED -> {
            GhostButton(
                text = "Đã chặn · Bỏ chặn",
                onClick = { onAction(ProfileAction.Unblock) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ProfileTabSelector(
    selectedTab: ProfileTab,
    onSelectTab: (ProfileTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .border(
                width = 0.5.dp,
                color = colors.outline
            ),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
            val icon = when (tab) {
                ProfileTab.PHOTOS -> Icons.Default.GridView
                ProfileTab.FOOTPRINTS -> Icons.Rounded.LocationOn
                ProfileTab.SAVED -> Icons.Default.BookmarkBorder
                ProfileTab.REPOSTS -> Icons.Default.Repeat
            }

            val itemColor by animateColorAsState(
                targetValue = if (isSelected) colors.primary else colors.textSecondary,
                animationSpec = spring(stiffness = Spring.StiffnessLow)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectTab(tab) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.titleVi,
                        tint = itemColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = tab.titleVi,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        ),
                        color = itemColor,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(2.5.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) colors.primary else Color.Transparent)
                )
            }
        }
    }
}

@Composable
private fun ProfilePhotoGridItem(
    post: Post,
    onClick: () -> Unit
) {
    val viewCount = remember(post.id) {
        val count = ((post.id.hashCode().toLong() and 0x7FFFFFFF) % 3800 + 150)
        if (count >= 1000) {
            String.format(java.util.Locale.US, "%.1fK", count / 1000.0)
        } else {
            count.toString()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .background(LokapTheme.colors.surfaceHigh)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = post.imageUrl,
            contentDescription = post.caption?.takeIf { it.isNotBlank() } ?: post.location.provinceName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .align(Alignment.BottomCenter)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color.Transparent,
                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.72f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Visibility,
                contentDescription = "Lượt xem",
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = viewCount,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ProfileFootprintsSection(
    uiState: ProfileUiState,
    onAction: (ProfileAction) -> Unit
) {
    if (uiState.visitedProvinces.isEmpty()) {
        EmptyState(
            title = "Chưa có khoảnh khắc nào",
            subtitle = "Chưa có dấu chân nào được ghi nhận trên bản đồ.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = LokapTheme.spacing.md,
                    vertical = LokapTheme.spacing.xxl
                )
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = LokapTheme.spacing.md,
                vertical = LokapTheme.spacing.xs
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(LokapTheme.shapes.cameraFrame)
                .border(
                    width = 1.dp,
                    color = LokapTheme.colors.outline,
                    shape = LokapTheme.shapes.cameraFrame
                )
        ) {
            LokapMap(
                bubbles = uiState.mapBubbles,
                landmarks = uiState.landmarks,
                camera = uiState.mapCamera,
                onCameraChange = { onAction(ProfileAction.UpdateMapCamera(it)) },
                onBubbleClick = { onAction(ProfileAction.ClickMapBubble(it)) },
                onLandmarkClick = {},
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(LokapTheme.spacing.sm)
                    .clip(LokapTheme.shapes.pill)
                    .background(LokapTheme.colors.surface.copy(alpha = 0.85f))
                    .padding(
                        horizontal = LokapTheme.spacing.sm,
                        vertical = LokapTheme.spacing.xs
                    )
            ) {
                Text(
                    text = "Đã mở khoá ${uiState.visitedProvincesCount}/${uiState.totalProvincesCount} tỉnh",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LokapTheme.colors.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(LokapTheme.spacing.md))

        Text(
            text = "Hộ chiếu vùng miền",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = LokapTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm),
            contentPadding = PaddingValues(vertical = LokapTheme.spacing.xs)
        ) {
            items(
                items = uiState.visitedProvinces,
                key = { it.code }
            ) { province ->
                RegionCard(
                    code = province.code,
                    regionName = province.name,
                    photoUrls = province.photoUrls,
                    postCount = province.postCount,
                    visited = true,
                    onClick = { onAction(ProfileAction.ClickProvinceCard(province.code)) },
                    modifier = Modifier.width(210.dp)
                )
            }
        }
    }
}

@Composable
private fun EditProfileBottomSheetContent(
    displayName: String,
    username: String,
    bio: String,
    displayNameError: String?,
    usernameError: String?,
    isSaving: Boolean,
    onNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                horizontal = LokapTheme.spacing.lg,
                vertical = LokapTheme.spacing.md
            )
    ) {
        Text(
            text = "Chỉnh sửa hồ sơ",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = LokapTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))

        Text(
            text = "Cập nhật tên hiển thị, tên người dùng và tiểu sử trên Lokap.",
            style = MaterialTheme.typography.bodySmall,
            color = LokapTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.md))

        OutlinedTextField(
            value = displayName,
            onValueChange = onNameChange,
            singleLine = true,
            label = { Text("Tên hiển thị") },
            isError = displayNameError != null,
            supportingText = {
                Text(
                    text = displayNameError ?: "${displayName.trim().length}/30",
                    color = if (displayNameError != null) LokapTheme.colors.error else LokapTheme.colors.textTertiary
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LokapTheme.colors.primary,
                unfocusedBorderColor = LokapTheme.colors.outline,
                errorBorderColor = LokapTheme.colors.error,
                focusedLabelColor = LokapTheme.colors.primary,
                unfocusedLabelColor = LokapTheme.colors.textSecondary,
                cursorColor = LokapTheme.colors.primary,
                focusedTextColor = LokapTheme.colors.textPrimary,
                unfocusedTextColor = LokapTheme.colors.textPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            singleLine = true,
            label = { Text("Tên người dùng (@username)") },
            isError = usernameError != null,
            supportingText = {
                Text(
                    text = usernameError ?: "Tối thiểu 3 ký tự",
                    color = if (usernameError != null) LokapTheme.colors.error else LokapTheme.colors.textTertiary
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LokapTheme.colors.primary,
                unfocusedBorderColor = LokapTheme.colors.outline,
                errorBorderColor = LokapTheme.colors.error,
                focusedLabelColor = LokapTheme.colors.primary,
                unfocusedLabelColor = LokapTheme.colors.textSecondary,
                cursorColor = LokapTheme.colors.primary,
                focusedTextColor = LokapTheme.colors.textPrimary,
                unfocusedTextColor = LokapTheme.colors.textPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

        OutlinedTextField(
            value = bio,
            onValueChange = onBioChange,
            maxLines = 3,
            label = { Text("Tiểu sử") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LokapTheme.colors.primary,
                unfocusedBorderColor = LokapTheme.colors.outline,
                errorBorderColor = LokapTheme.colors.error,
                focusedLabelColor = LokapTheme.colors.primary,
                unfocusedLabelColor = LokapTheme.colors.textSecondary,
                cursorColor = LokapTheme.colors.primary,
                focusedTextColor = LokapTheme.colors.textPrimary,
                unfocusedTextColor = LokapTheme.colors.textPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.lg))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
        ) {
            GhostButton(
                text = "Huỷ",
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            )
            PinkButton(
                text = "Lưu thay đổi",
                loading = isSaving,
                enabled = displayNameError == null && usernameError == null && displayName.trim().length in 2..30,
                onClick = onSave,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(LokapTheme.spacing.md))
    }
}

@Composable
private fun ProfileLoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(LokapTheme.spacing.md))
        ShimmerBox(
            modifier = Modifier.size(96.dp),
            shape = LokapTheme.shapes.pill
        )
        Spacer(modifier = Modifier.height(LokapTheme.spacing.md))
        ShimmerBox(
            modifier = Modifier
                .width(160.dp)
                .height(24.dp),
            shape = LokapTheme.shapes.pill
        )
        Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))
        ShimmerBox(
            modifier = Modifier
                .width(100.dp)
                .height(16.dp),
            shape = LokapTheme.shapes.pill
        )
        Spacer(modifier = Modifier.height(LokapTheme.spacing.lg))
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp),
            shape = LokapTheme.shapes.card
        )
        Spacer(modifier = Modifier.height(LokapTheme.spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.xs)
        ) {
            repeat(3) {
                ShimmerBox(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    shape = LokapTheme.shapes.card
                )
            }
        }
    }
}

@Preview(name = "Profile - Hồ sơ của tôi", showBackground = true)
@Composable
private fun ProfileMyScreenPreview() {
    LokapTheme {
        ProfileScreen(
            uiState = ProfileUiState(
                isLoading = false,
                isMe = true,
                user = SampleData.currentUser,
                visitedProvincesCount = 5,
                totalProvincesCount = 34,
                postCount = SampleData.posts.size,
                friendCount = 12,
                visiblePosts = SampleData.posts.take(6)
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Profile - Hồ sơ người khác (Lời mời)", showBackground = true)
@Composable
private fun ProfileOtherUserRequestPreview() {
    LokapTheme {
        ProfileScreen(
            uiState = ProfileUiState(
                isLoading = false,
                isMe = false,
                user = SampleData.users.firstOrNull() ?: SampleData.currentUser,
                friendStatus = FriendStatus.REQUEST_RECEIVED,
                visitedProvincesCount = 2,
                totalProvincesCount = 34,
                postCount = 0,
                friendCount = 8,
                visiblePosts = emptyList()
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Profile - Đang tải", showBackground = true)
@Composable
private fun ProfileLoadingPreview() {
    LokapTheme {
        ProfileScreen(
            uiState = ProfileUiState(isLoading = true),
            onAction = {}
        )
    }
}

@Preview(name = "Profile - Người dùng không tồn tại", showBackground = true)
@Composable
private fun ProfileNotFoundPreview() {
    LokapTheme {
        ProfileScreen(
            uiState = ProfileUiState(
                isLoading = false,
                isUserNotFound = true,
                user = null
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Profile - Lỗi mạng", showBackground = true)
@Composable
private fun ProfileErrorPreview() {
    LokapTheme {
        ProfileScreen(
            uiState = ProfileUiState(
                isLoading = false,
                errorMessage = "Không thể tải hồ sơ. Kiểm tra kết nối mạng nhé!"
            ),
            onAction = {}
        )
    }
}
