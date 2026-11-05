package vn.nhom2.lokap.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import coil3.compose.AsyncImage
import vn.nhom2.lokap.core.designsystem.component.AvatarRing
import vn.nhom2.lokap.core.designsystem.component.EmptyState
import vn.nhom2.lokap.core.designsystem.component.ErrorState
import vn.nhom2.lokap.core.designsystem.component.GhostButton
import vn.nhom2.lokap.core.designsystem.component.GlassChip
import vn.nhom2.lokap.core.designsystem.component.OfflineBanner
import vn.nhom2.lokap.core.designsystem.component.PinkButton
import vn.nhom2.lokap.core.designsystem.component.RegionCard
import vn.nhom2.lokap.core.designsystem.component.ShimmerBox
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.core.navigation.navigateSingleTop
import vn.nhom2.lokap.core.navigation.popBackStackSafely
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.designsystem.theme.regionGradient
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.RegionLevel
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.Post
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
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
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
                        contentPadding = PaddingValues(
                            start = LokapTheme.spacing.md,
                            end = LokapTheme.spacing.md,
                            bottom = LokapTheme.spacing.xxl
                        ),
                        horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.xs)
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
                                modifier = Modifier.padding(vertical = LokapTheme.spacing.sm)
                            )
                        }

                        when (uiState.selectedTab) {
                            ProfileTab.PHOTOS -> {
                                if (uiState.visiblePosts.isEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        EmptyState(
                                            title = "Chưa có khoảnh khắc nào",
                                            subtitle = if (uiState.isMe) {
                                                "Mở camera lên và thả ngay chiếc ảnh đầu tiên lên bản đồ nhé!"
                                            } else {
                                                "Người này chưa chia sẻ khoảnh khắc nào ở chế độ hiển thị với bạn."
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = LokapTheme.spacing.xxl)
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
                errorText = uiState.editDisplayNameError,
                isSaving = uiState.isSavingProfile,
                onNameChange = { onAction(ProfileAction.ChangeEditDisplayName(it)) },
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

        Text(
            text = if (username != null) "@$username" else "Hồ sơ",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = LokapTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (isMe) {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Cài đặt",
                    tint = LokapTheme.colors.textPrimary
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun ProfileHeaderSection(
    uiState: ProfileUiState,
    onAction: (ProfileAction) -> Unit
) {
    val user = uiState.user ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = LokapTheme.spacing.xs, bottom = LokapTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AvatarRing(
            avatarUrl = user.avatarUrl,
            displayName = user.displayName,
            size = 96.dp,
            hasNewPost = uiState.visiblePosts.isNotEmpty()
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

        Text(
            text = user.displayName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = LokapTheme.colors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))

        Text(
            text = "@${user.username}",
            style = MaterialTheme.typography.bodyMedium,
            color = LokapTheme.colors.primary,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.md))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(LokapTheme.shapes.card)
                .background(LokapTheme.colors.surface)
                .border(
                    width = 1.dp,
                    color = LokapTheme.colors.outline,
                    shape = LokapTheme.shapes.card
                )
                .padding(vertical = LokapTheme.spacing.md, horizontal = LokapTheme.spacing.sm),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileStatItem(
                value = "${uiState.visitedProvincesCount}/${uiState.totalProvincesCount}",
                label = "Đã đến ${uiState.visitedProvincesCount}/${uiState.totalProvincesCount} tỉnh",
                highlight = true,
                modifier = Modifier.weight(1.3f)
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(LokapTheme.colors.outline)
            )

            ProfileStatItem(
                value = uiState.postCount.toString(),
                label = "Bài đăng",
                highlight = false,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(LokapTheme.colors.outline)
            )

            ProfileStatItem(
                value = uiState.friendCount.toString(),
                label = "Bạn bè",
                highlight = false,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(LokapTheme.spacing.md))

        if (uiState.isMe) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
            ) {
                GhostButton(
                    text = "Sửa hồ sơ",
                    leadingIcon = Icons.Default.Edit,
                    onClick = { onAction(ProfileAction.OpenEditProfileSheet) },
                    modifier = Modifier.weight(1f)
                )
                GhostButton(
                    text = "Cài đặt",
                    leadingIcon = Icons.Default.Settings,
                    onClick = { onAction(ProfileAction.OpenSettings) },
                    modifier = Modifier.weight(1f)
                )
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

@Composable
private fun ProfileStatItem(
    value: String,
    label: String,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = if (highlight) LokapTheme.colors.primary else LokapTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LokapTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
                        text = "Bạn đã đạt giới hạn tối đa 20 bạn bè như Locket",
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
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
    ) {
        ProfileTab.entries.forEach { tab ->
            GlassChip(
                label = tab.titleVi,
                leadingIcon = if (tab == ProfileTab.FOOTPRINTS) Icons.Default.LocationOn else null,
                selected = selectedTab == tab,
                onClick = { onSelectTab(tab) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ProfilePhotoGridItem(
    post: Post,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(LokapTheme.shapes.card)
            .background(LokapTheme.colors.surfaceHigh)
            .border(
                width = 1.dp,
                brush = regionGradient(post.location.provinceCode.orEmpty()),
                shape = LokapTheme.shapes.card
            )
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
                .align(Alignment.BottomStart)
                .padding(LokapTheme.spacing.xs)
                .clip(LokapTheme.shapes.pill)
                .background(LokapTheme.colors.background.copy(alpha = 0.72f))
                .padding(
                    horizontal = LokapTheme.spacing.xs,
                    vertical = 2.dp
                )
        ) {
            Text(
                text = post.location.provinceName ?: "Việt Nam",
                style = MaterialTheme.typography.labelSmall,
                color = LokapTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
            subtitle = "Chưa có dấu chân nào được ghi nhận trên bản đồ 34 tỉnh thành.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = LokapTheme.spacing.xxl)
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = LokapTheme.spacing.xs)
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
            text = "Hộ chiếu vùng miền (${uiState.visitedProvinces.size})",
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
    errorText: String?,
    isSaving: Boolean,
    onNameChange: (String) -> Unit,
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
            text = "Đổi tên hiển thị",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = LokapTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))

        Text(
            text = "Tên này sẽ hiện trên ảnh Locket và bản đồ cùng hội bạn thân (2–30 ký tự).",
            style = MaterialTheme.typography.bodySmall,
            color = LokapTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.md))

        OutlinedTextField(
            value = displayName,
            onValueChange = onNameChange,
            singleLine = true,
            label = { Text("Tên hiển thị") },
            isError = errorText != null,
            supportingText = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = errorText ?: "Nhập từ 2 đến 30 ký tự",
                        color = if (errorText != null) {
                            LokapTheme.colors.error
                        } else {
                            LokapTheme.colors.textTertiary
                        }
                    )
                    Text(
                        text = "${displayName.trim().length}/30",
                        color = LokapTheme.colors.textTertiary
                    )
                }
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
                enabled = errorText == null && displayName.trim().length in 2..30,
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
