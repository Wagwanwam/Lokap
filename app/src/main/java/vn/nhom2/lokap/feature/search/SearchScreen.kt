package vn.nhom2.lokap.feature.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import vn.nhom2.lokap.core.designsystem.component.*
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.core.navigation.navigateSingleTop
import vn.nhom2.lokap.core.navigation.popBackStackSafely
import vn.nhom2.lokap.core.navigation.rememberEntrySettled
import vn.nhom2.lokap.domain.model.*
fun NavGraphBuilder.searchScreen(navController: NavController) {
    composable<Route.Search> {
        SearchRoute(
            onNavigateBack = { navController.popBackStackSafely() },
            onNavigateToProfile = { userId ->
                navController.navigateSingleTop(Route.Profile(userId))
            },
            onNavigateToPlace = { place ->
                val uri = Uri.parse("geo:${place.lat},${place.lng}?q=${place.lat},${place.lng}(${Uri.encode(place.name)})")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                runCatching { navController.context.startActivity(intent) }.onFailure {
                    android.widget.Toast.makeText(navController.context, "Không có ứng dụng để mở bản đồ", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun SearchRoute(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToPlace: (Place) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                // Bỏ focus ô tìm kiếm trước khi rời màn: bàn phím ẩn ngay, không che màn kế tiếp suốt animation
                SearchEffect.NavigateBack -> {
                    focusManager.clearFocus(force = true)
                    onNavigateBack()
                }
                is SearchEffect.NavigateToProfile -> {
                    focusManager.clearFocus(force = true)
                    onNavigateToProfile(effect.userId)
                }
                is SearchEffect.NavigateToPlace -> onNavigateToPlace(effect.place)
                is SearchEffect.ShowMessage -> scope.launch { snackbarHostState.showSnackbar(effect.message) }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SearchScreen(
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
fun SearchScreen(
    uiState: SearchUiState,
    onAction: (SearchAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    // Chỉ tự focus (mở bàn phím) 1 lần cho mỗi lần vào màn Tìm kiếm và đợi animation chuyển màn xong:
    // quay lại từ Hồ sơ (kể cả lúc bắt đầu vuốt Back) không bật bàn phím giữa animation
    var autoFocused by rememberSaveable { mutableStateOf(false) }
    // Ô đang được focus: lưu lại để focus lại sau khi xoay màn hình / khôi phục app (trước khi rời màn đã clearFocus)
    var fieldFocused by rememberSaveable { mutableStateOf(false) }
    val settled = rememberEntrySettled()

    LaunchedEffect(settled) {
        if (settled && (!autoFocused || fieldFocused)) {
            focusRequester.requestFocus()
            autoFocused = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Surface(
                shape = CircleShape,
                color = colors.surfaceHigh,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { onAction(SearchAction.BackClicked) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }

            OutlinedTextField(
                value = uiState.query,
                onValueChange = { onAction(SearchAction.QueryChanged(it)) },
                placeholder = {
                    // Một dòng: ô cao ~56dp, gợi ý xuống dòng sẽ lệch khỏi tâm ô
                    Text(
                        text = "Tìm bạn bè, địa danh",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        color = colors.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(25.dp)
                    )
                },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { onAction(SearchAction.QueryChanged("")) }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Xoá",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.outline.copy(alpha = 0.25f),
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    cursorColor = colors.primary,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                // Không ép chiều cao: ô nhập Material cần ~56dp, ép 48dp là mất phần dưới chữ đang gõ
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { fieldFocused = it.isFocused }
            )
        }

        if (uiState.query.isBlank() && uiState.recentSearches.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.md, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Gợi ý tìm kiếm",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.textTertiary
                )
                Text(
                    text = "Xoá",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary,
                    modifier = Modifier.clickable { onAction(SearchAction.ClearRecentSearches) }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = spacing.md, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                uiState.recentSearches.forEach { term ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceHigh,
                        border = BorderStroke(0.6.dp, colors.outline.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onAction(SearchAction.SelectRecentSearch(term)) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = colors.textTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = term,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.5.sp),
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(spacing.xs))
        }

        TabRow(
            selectedTabIndex = uiState.selectedTab.ordinal,
            containerColor = colors.background,
            contentColor = colors.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab.ordinal]),
                    color = colors.primary,
                    height = 2.5.dp
                )
            },
            divider = {}
        ) {
            Tab(
                selected = uiState.selectedTab == SearchTab.PEOPLE,
                onClick = { onAction(SearchAction.SelectTab(SearchTab.PEOPLE)) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.People,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Mọi người (${uiState.userResults.size})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (uiState.selectedTab == SearchTab.PEOPLE) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                }
            )

            Tab(
                selected = uiState.selectedTab == SearchTab.PLACES,
                onClick = { onAction(SearchAction.SelectTab(SearchTab.PLACES)) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Place,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Địa danh (${uiState.placeResults.size})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (uiState.selectedTab == SearchTab.PLACES) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                }
            )
        }

        when (uiState.selectedTab) {
            SearchTab.PEOPLE -> {
                if (uiState.userResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.xl),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            title = "Không tìm thấy"
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(spacing.md),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.userResults, key = { it.id }) { user ->
                            val status = uiState.friendStatuses[user.id] ?: FriendStatus.NONE

                            SearchUserItem(
                                user = user,
                                isOnline = false,
                                friendStatus = status,
                                onAuthorClick = { onAction(SearchAction.AuthorClicked(user.id)) },
                                onToggleFriend = { onAction(SearchAction.ToggleFriendRequest(user.id)) }
                            )
                        }
                    }
                }
            }

            SearchTab.PLACES -> {
                if (uiState.placeResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.xl),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            title = "Không tìm thấy"
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(spacing.md),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.placeResults, key = { it.id }) { place ->
                            SearchPlaceItem(
                                place = place,
                                onClick = { onAction(SearchAction.PlaceClicked(place.id)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchUserItem(
    user: User,
    isOnline: Boolean,
    friendStatus: FriendStatus,
    onAuthorClick: () -> Unit,
    onToggleFriend: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(0.6.dp, colors.outline.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onAuthorClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AvatarRing(
                    avatarUrl = user.avatarUrl,
                    displayName = user.displayName,
                    size = 40.dp,
                    onClick = onAuthorClick
                )
                OnlineIndicatorDot(
                    isOnline = isOnline,
                    size = 10.dp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp),
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Chỉ icon (không chữ) để tên người dùng có đủ chỗ; mô tả đọc màn hình nói rõ trạng thái
            when (friendStatus) {
                FriendStatus.REQUEST_SENT -> FriendActionIcon(
                    icon = Icons.Rounded.Schedule,
                    description = "Đã gửi lời mời, chạm để huỷ",
                    highlighted = false,
                    onClick = onToggleFriend
                )
                FriendStatus.FRIENDS -> FriendActionIcon(
                    icon = Icons.Rounded.HowToReg,
                    description = "Bạn bè",
                    highlighted = false,
                    onClick = onAuthorClick
                )
                FriendStatus.REQUEST_RECEIVED -> FriendActionIcon(
                    icon = Icons.Rounded.PersonAdd,
                    description = "Chấp nhận lời mời kết bạn",
                    highlighted = true,
                    onClick = onToggleFriend
                )
                else -> FriendActionIcon(
                    icon = Icons.Rounded.PersonAdd,
                    description = "Kết bạn",
                    highlighted = true,
                    onClick = onToggleFriend
                )
            }
        }
    }
}

/** Nút tròn chỉ có icon (48dp) cho trạng thái kết bạn ở kết quả tìm người. */
@Composable
private fun FriendActionIcon(
    icon: ImageVector,
    description: String,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    val colors = LokapTheme.colors
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (highlighted) colors.primary else colors.surfaceHigh)
            .border(1.dp, if (highlighted) colors.primary else colors.outline, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (highlighted) colors.onPrimary else colors.textPrimary,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun SearchPlaceItem(
    place: Place,
    onClick: () -> Unit
) {
    val colors = LokapTheme.colors

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(0.6.dp, colors.outline.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = colors.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Place,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = place.address,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp),
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
