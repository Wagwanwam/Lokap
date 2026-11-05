package vn.nhom2.lokap.core.navigation

import vn.nhom2.lokap.feature.auth.setup.setupProfileScreen
import vn.nhom2.lokap.feature.auth.forgotPasswordScreen
import vn.nhom2.lokap.feature.auth.registerScreen
import vn.nhom2.lokap.feature.auth.loginScreen
import vn.nhom2.lokap.feature.onboarding.onboardingScreen
import vn.nhom2.lokap.feature.launch.rememberStartDestination
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import vn.nhom2.lokap.core.designsystem.component.EmptyState
import vn.nhom2.lokap.core.designsystem.component.GhostButton
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.feature.camera.CameraRoute
import vn.nhom2.lokap.feature.chat.chatScreen
import vn.nhom2.lokap.feature.composer.composerScreen
import vn.nhom2.lokap.feature.feed.feedScreen
import vn.nhom2.lokap.feature.friends.friendsScreen
import vn.nhom2.lokap.feature.inbox.inboxScreen
import vn.nhom2.lokap.feature.landmark.landmarkScreen
import vn.nhom2.lokap.feature.map.mapScreen
import vn.nhom2.lokap.feature.notifications.notificationsScreen
import vn.nhom2.lokap.feature.postdetail.postDetailScreen
import vn.nhom2.lokap.feature.profile.profileScreen
import vn.nhom2.lokap.feature.region.regionAlbumScreen
import vn.nhom2.lokap.feature.settings.settingsScreen

private const val NAV_ANIM_DURATION = 320


@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val startDestination = rememberStartDestination()
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
            .fillMaxSize()
            .background(LokapTheme.colors.background),
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(NAV_ANIM_DURATION)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(NAV_ANIM_DURATION)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(NAV_ANIM_DURATION)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(NAV_ANIM_DURATION)
            )
        }
    ) {
        composable<Route.Camera> {
            CameraRoute(
                onNavigateToComposer = { path -> navController.navigateSingleTop(Route.Composer(path)) },
                onNavigateToFeed = { navController.navigateSingleTop(Route.Feed) },
                onNavigateToMap = { navController.navigateSingleTop(Route.Map) },
                onNavigateToFriends = { navController.navigateSingleTop(Route.Friends) },
                onNavigateToInbox = { navController.navigateSingleTop(Route.Inbox) },
                onNavigateToProfile = { userId -> navController.navigateSingleTop(Route.Profile(userId)) }
            )
        }

        onboardingScreen(navController)

        loginScreen(navController)

        registerScreen(navController)

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

        notificationsScreen(navController)

        inboxScreen(navController)

        chatScreen(navController)

        settingsScreen(navController)
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
            title = subtitle,
            subtitle = "Màn hình \"$title\" sẽ được hoàn thiện ở bước tiếp theo."
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
