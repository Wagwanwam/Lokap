package vn.nhom2.lokap.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import vn.nhom2.lokap.core.designsystem.component.*
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.feature.addfriend.addFriendScreen
import vn.nhom2.lokap.feature.auth.forgotPasswordScreen
import vn.nhom2.lokap.feature.auth.loginScreen
import vn.nhom2.lokap.feature.auth.registerScreen
import vn.nhom2.lokap.feature.auth.setup.setupProfileScreen
import vn.nhom2.lokap.feature.camera.CameraRoute
import vn.nhom2.lokap.feature.chat.chatScreen
import vn.nhom2.lokap.feature.composer.composerScreen
import vn.nhom2.lokap.feature.feed.feedScreen
import vn.nhom2.lokap.feature.friends.friendsScreen
import vn.nhom2.lokap.feature.inbox.inboxScreen
import vn.nhom2.lokap.feature.landmark.landmarkScreen
import vn.nhom2.lokap.feature.launch.rememberStartDestination
import vn.nhom2.lokap.feature.map.mapScreen
import vn.nhom2.lokap.feature.notifications.notificationsScreen
import vn.nhom2.lokap.feature.onboarding.onboardingScreen
import vn.nhom2.lokap.feature.postdetail.postDetailScreen
import vn.nhom2.lokap.feature.profile.profileScreen
import vn.nhom2.lokap.feature.region.regionAlbumScreen
import vn.nhom2.lokap.feature.search.searchScreen
import vn.nhom2.lokap.feature.settings.settingsScreen
import vn.nhom2.lokap.feature.auth.verify.verifyEmailScreen
private const val NAV_ANIM_DURATION = 320
private const val NAV_TAB_TRANSITION_MS = 200

private fun NavBackStackEntry?.isTopLevelDestination(): Boolean {
    if (this == null) return false
    val dest = destination
    return when {
        dest.hasRoute<Route.Feed>() -> true
        dest.hasRoute<Route.Map>() -> true
        dest.hasRoute<Route.Camera>() -> true
        dest.hasRoute<Route.Inbox>() -> true
        dest.hasRoute<Route.Profile>() -> {
            val profile = runCatching { toRoute<Route.Profile>() }.getOrNull()
            profile?.userId == null
        }
        else -> false
    }
}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val startDestination = rememberStartDestination()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val currentTab = when {
        currentDestination?.hasRoute<Route.Feed>() == true -> MainTab.FEED
        currentDestination?.hasRoute<Route.Map>() == true -> MainTab.MAP
        currentDestination?.hasRoute<Route.Camera>() == true -> MainTab.CAMERA
        currentDestination?.hasRoute<Route.Inbox>() == true -> MainTab.INBOX
        currentDestination?.hasRoute<Route.Profile>() == true -> {
            val profile = runCatching { navBackStackEntry?.toRoute<Route.Profile>() }.getOrNull()
            if (profile?.userId == null) MainTab.PROFILE else null
        }
        else -> null
    }

    var lastSelectedTab by remember { mutableStateOf(MainTab.FEED) }
    if (currentTab != null) {
        lastSelectedTab = currentTab
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LokapTheme.colors.background)
    ) {
        // Đang khôi phục phiên đăng nhập (AuthSession.Loading): giữ nền đen, chưa dựng NavHost.
        // Điểm bắt đầu được chốt một lần (rememberStartDestination) nên graph không bị dựng lại.
        val start = startDestination ?: return@Box

        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier
                .fillMaxSize()
                .background(LokapTheme.colors.background),
            enterTransition = {
                if (initialState.isTopLevelDestination() && targetState.isTopLevelDestination()) {
                    fadeIn(animationSpec = tween(NAV_TAB_TRANSITION_MS))
                } else {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(NAV_ANIM_DURATION)
                    )
                }
            },
            exitTransition = {
                if (initialState.isTopLevelDestination() && targetState.isTopLevelDestination()) {
                    fadeOut(animationSpec = tween(NAV_TAB_TRANSITION_MS))
                } else {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(NAV_ANIM_DURATION)
                    )
                }
            },
            popEnterTransition = {
                if (initialState.isTopLevelDestination() && targetState.isTopLevelDestination()) {
                    fadeIn(animationSpec = tween(NAV_TAB_TRANSITION_MS))
                } else {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(NAV_ANIM_DURATION)
                    )
                }
            },
            popExitTransition = {
                if (initialState.isTopLevelDestination() && targetState.isTopLevelDestination()) {
                    fadeOut(animationSpec = tween(NAV_TAB_TRANSITION_MS))
                } else {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(NAV_ANIM_DURATION)
                    )
                }
            },
            // Vuốt back từ mép màn hình (predictive back, bật sẵn từ targetSdk 36). Mặc định của Navigation 2.10
            // là scaleOut(0.7f) + fadeIn: màn cũ co thành khung nhỏ trôi trên màn trước rồi mới thu lại.
            // Nhóm chọn đổi màn ngay (15/11): lúc vuốt màn hiện tại đứng yên, thả tay là sang màn trước.
            // Nút mũi tên back trên màn và back 3 phím không đi qua đây, vẫn dùng popEnter/popExit ở trên.
            predictivePopEnterTransition = { EnterTransition.None },
            predictivePopExitTransition = { ExitTransition.None }
        ) {
            composable<Route.Camera> {
                CameraRoute(
                    onNavigateToComposer = { path -> navController.navigateSingleTop(Route.Composer(path)) },
                    onNavigateToFeed = { navController.navigateToTab(Route.Feed) },
                    onNavigateToMap = { navController.navigateToTab(Route.Map) },
                    onNavigateToFriends = { navController.navigateSingleTop(Route.Friends) },
                    onNavigateToInbox = { navController.navigateToTab(Route.Inbox) },
                    onNavigateToProfile = { userId ->
                        // null = tab Hồ sơ của tôi; có userId = mở hồ sơ bạn bè (màn con, giữ trong stack)
                        if (userId == null) navController.navigateToTab(Route.Profile())
                        else navController.navigateSingleTop(Route.Profile(userId))
                    }
                )
            }

            // Camera mở từ màn Địa danh: chụp xong sang Composer đã chọn sẵn địa danh
            // (Huỷ ở Composer quay lại camera để chụp lại; gửi xong về thẳng màn Địa danh)
            composable<Route.LandmarkCamera> { entry ->
                val landmarkId = entry.toRoute<Route.LandmarkCamera>().landmarkId
                CameraRoute(
                    onNavigateToComposer = { path -> navController.navigateSingleTop(Route.Composer(path, landmarkId)) },
                    onNavigateToFeed = { navController.popBackStackSafely() },
                    onNavigateToMap = { navController.popBackStackSafely() },
                    onNavigateToFriends = { navController.navigateSingleTop(Route.Friends) },
                    onNavigateToInbox = { navController.navigateSingleTop(Route.Inbox) },
                    onNavigateToProfile = { userId -> navController.navigateSingleTop(Route.Profile(userId)) }
                )
            }

            onboardingScreen(navController)

            loginScreen(navController)

            registerScreen(navController)

            verifyEmailScreen(navController)

            forgotPasswordScreen(navController)

            setupProfileScreen(navController)

            feedScreen(navController)

            composerScreen(navController)

            mapScreen(navController)

            regionAlbumScreen(navController)

            landmarkScreen(navController)

            postDetailScreen(navController)

            profileScreen(navController)

            friendsScreen(navController)

            addFriendScreen(navController)

            notificationsScreen(navController)

            inboxScreen(navController)

            chatScreen(navController)

            settingsScreen(navController)

            searchScreen(navController)
        }

        AnimatedVisibility(
            visible = currentTab != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(NAV_ANIM_DURATION)
            ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(NAV_ANIM_DURATION)
            ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
        ) {
            LokapBottomNavBar(
                currentTab = lastSelectedTab,
                onTabSelected = { selectedTab ->
                    when (selectedTab) {
                        MainTab.FEED -> navController.navigateToTab(Route.Feed)
                        MainTab.MAP -> navController.navigateToTab(Route.Map)
                        MainTab.CAMERA -> navController.navigateToTab(Route.Camera)
                        MainTab.INBOX -> navController.navigateToTab(Route.Inbox)
                        MainTab.PROFILE -> navController.navigateToTab(Route.Profile())
                    }
                }
            )
        }
    }
}

@Composable
fun PlaceholderScreen(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String = "Đang xây dựng"
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .padding(spacing.lg),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = colors.textPrimary
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
        }

        EmptyState(
            title = subtitle
        )

        GhostButton(
            text = "Quay lại",
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun PlaceholderScreenPreview() {
    LokapTheme {
        PlaceholderScreen(
            title = "Bản đồ 34 Tỉnh/Thành",
            onBack = {}
        )
    }
}
