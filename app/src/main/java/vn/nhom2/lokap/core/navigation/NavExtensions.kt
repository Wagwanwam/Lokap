package vn.nhom2.lokap.core.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.toRoute

fun NavController.navigateSingleTop(route: Route) =
    navigate(route) { launchSingleTop = true }

/**
 * Đổi tab chính (Feed, Bản đồ, Camera, Hộp thư, Hồ sơ của tôi): bỏ mọi màn phía trên màn gốc rồi mở tab,
 * nên back stack luôn là [gốc, tab, …]. Back từ tab về màn gốc, Back lần nữa thì thoát app;
 * không còn lùi qua lịch sử tab và dựng lại Bản đồ/Camera nhiều lần.
 * Màn gốc = màn ở ĐÁY back stack: Feed khi mở app đã đăng nhập, Camera sau khi đăng nhập/đăng ký.
 */
fun NavController.navigateToTab(route: Route) {
    val current = currentBackStackEntry ?: return
    // Bấm lại tab đang mở: giữ nguyên màn (không dựng lại ViewModel, bản đồ, camera)
    if (current.isTab(route)) return
    val root = rootEntry()
    if (root == null) {
        navigate(route) { launchSingleTop = true }
        return
    }
    val rootId = root.destination.id
    // popUpTo/popBackStack(id) dừng ở entry TRÊN CÙNG cùng loại màn: nếu phía trên có màn trùng loại với màn gốc
    // (vd Camera mở từ Chat) thì bỏ các màn đó trước, để lần pop dưới đây về đúng màn gốc ở đáy
    while (getBackStackEntry(rootId).id != root.id) {
        if (!popBackStack(rootId, inclusive = true)) break
    }
    if (root.isTab(route)) {
        // Tab đích chính là màn gốc: pop về nó như Back (giữ entry cũ, chờ animation xong mới lên RESUMED)
        popBackStack(rootId, inclusive = false)
    } else {
        navigate(route) {
            popUpTo(rootId)
            launchSingleTop = true
        }
    }
}

/**
 * Entry ở đáy back stack. Mọi màn của app đều là màn Compose (không có dialog destination), nên đáy back stack
 * của ComposeNavigator chính là màn gốc. Không dùng điểm bắt đầu của graph: nó đổi theo phiên (Feed/Login…)
 * còn back stack được khôi phục sau khi xoay máy hay đăng xuất rồi đăng nhập lại thì không.
 */
private fun NavController.rootEntry(): NavBackStackEntry? =
    navigatorProvider.getNavigator(ComposeNavigator::class.java).backStack.value.firstOrNull()

/** Entry đang là tab [route]; tab Hồ sơ chỉ là hồ sơ của mình (userId = null), không phải hồ sơ bạn bè. */
private fun NavBackStackEntry.isTab(route: Route): Boolean =
    destination.hasRoute(route::class) &&
        (route !is Route.Profile || runCatching { toRoute<Route.Profile>().userId == null }.getOrDefault(false))

fun <T : Any> NavController.navigateClearingBackStack(route: T) =
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }

fun NavController.popBackStackSafely() {
    if (previousBackStackEntry != null) popBackStack()
}
