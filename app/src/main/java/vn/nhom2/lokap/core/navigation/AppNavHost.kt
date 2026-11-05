package vn.nhom2.lokap.core.navigation

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

private const val NAV_ANIM_DURATION = 320

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Route.Camera,
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
                onNavigateToComposer = { path -> navController.navigate(Route.Composer(path)) },
                onNavigateToFeed = { navController.navigate(Route.Feed) },
                onNavigateToMap = { navController.navigate(Route.Map) },
                onNavigateToFriends = { navController.navigate(Route.Friends) },
                onNavigateToInbox = { navController.navigate(Route.Inbox) },
                onNavigateToProfile = { userId -> navController.navigate(Route.Profile(userId)) }
            )
        }

        composable<Route.Feed>(
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(NAV_ANIM_DURATION)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(NAV_ANIM_DURATION)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(NAV_ANIM_DURATION)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(NAV_ANIM_DURATION)
                )
            }
        ) {
            PlaceholderScreen(
                title = "Bảng tin Bạn bè & Khám phá",
                subtitle = "Đang xây dựng",
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.Composer> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Composer>()
            PlaceholderScreen(
                title = "Đăng ảnh mới",
                subtitle = "Đang xây dựng · File: ${route.imagePath.substringAfterLast('/')}",
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.Map> {
            PlaceholderScreen(
                title = "Bản đồ 34 Tỉnh/Thành",
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.RegionAlbum> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.RegionAlbum>()
            PlaceholderScreen(
                title = "Album khu vực (${route.level} · ${route.code})",
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.Landmark> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Landmark>()
            PlaceholderScreen(
                title = "Địa danh (${route.id})",
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.PostDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.PostDetail>()
            PlaceholderScreen(
                title = "Chi tiết bài đăng (${route.id})",
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.Profile> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Profile>()
            PlaceholderScreen(
                title = "Hồ sơ cá nhân (${route.userId ?: "Tôi"})",
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.Friends> {
            PlaceholderScreen(
                title = "Danh sách bạn bè",
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.Notifications> {
            PlaceholderScreen(
                title = "Thông báo",
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.Inbox> {
            PlaceholderScreen(
                title = "Hộp thư tin nhắn",
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.Chat> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Chat>()
            PlaceholderScreen(
                title = "Trò chuyện (${route.conversationId})",
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.Settings> {
            PlaceholderScreen(
                title = "Cài đặt",
                onBack = { navController.popBackStack() }
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
