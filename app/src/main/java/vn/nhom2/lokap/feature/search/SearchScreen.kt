package vn.nhom2.lokap.feature.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
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
import vn.nhom2.lokap.data.presence.PresenceStore
import vn.nhom2.lokap.domain.model.*
fun NavGraphBuilder.searchScreen(navController: NavController) {
    composable<Route.Search> {
        SearchRoute(
            onNavigateBack = { navController.popBackStackSafely() },
            onNavigateToProfile = { userId ->
                navController.navigateSingleTop(Route.Profile(userId))
            },
            onNavigateToPlace = { placeId ->
                navController.navigateSingleTop(Route.Landmark(placeId))
            }
        )
    }
}

@Composable
fun SearchRoute(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToPlace: (String) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SearchEffect.NavigateBack -> onNavigateBack()
                is SearchEffect.NavigateToProfile -> onNavigateToProfile(effect.userId)
                is SearchEffect.NavigateToPlace -> onNavigateToPlace(effect.placeId)
            }
        }
    }

    SearchScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
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

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onAction(SearchAction.BackClicked) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            OutlinedTextField(
                value = uiState.query,
                onValueChange = { onAction(SearchAction.QueryChanged(it)) },
                placeholder = {
                    Text(
                        text = "Tìm bạn bè, địa danh, tỉnh thành…",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                        color = colors.textTertiary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { onAction(SearchAction.QueryChanged("")) }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Xoá",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
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
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .focusRequester(focusRequester)
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
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = term,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
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
                            modifier = Modifier.size(16.dp)
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
                            modifier = Modifier.size(16.dp)
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
                            title = "Không tìm thấy người dùng",
                            subtitle = "Thử tìm kiếm với tên hoặc @username khác nhé."
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
                            val presence by PresenceStore.getPresence(user.id).collectAsStateWithLifecycle()

                            SearchUserItem(
                                user = user,
                                isOnline = presence.isOnline,
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
                            title = "Không tìm thấy địa danh",
                            subtitle = "Thử tìm kiếm với tên địa điểm hoặc tỉnh thành khác."
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
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${user.username} · Đà Nẵng",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            when (friendStatus) {
                FriendStatus.REQUEST_SENT -> {
                    GhostButton(
                        text = "Đã gửi",
                        onClick = onToggleFriend,
                        modifier = Modifier.height(32.dp)
                    )
                }
                FriendStatus.FRIENDS -> {
                    GhostButton(
                        text = "Bạn bè",
                        onClick = onAuthorClick,
                        modifier = Modifier.height(32.dp)
                    )
                }
                else -> {
                    PinkButton(
                        text = "Kết bạn",
                        leadingIcon = Icons.Filled.PersonAdd,
                        onClick = onToggleFriend,
                        modifier = Modifier.height(32.dp)
                    )
                }
            }
        }
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
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = place.address,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
