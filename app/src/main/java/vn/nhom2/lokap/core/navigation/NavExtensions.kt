package vn.nhom2.lokap.core.navigation

import androidx.navigation.NavController

/** Bấm nhanh 2 lần không mở trùng màn hình. */
fun NavController.navigateSingleTop(route: Route) =
    navigate(route) { launchSingleTop = true }

/**
 * Sang [route] và xoá toàn bộ back stack (đăng nhập xong, đăng xuất): bấm Back sẽ thoát app.
 * Dùng graph.id chứ không dùng popUpTo(0): id 0 không có trong back stack nên lệnh bị bỏ qua.
 */
fun <T : Any> NavController.navigateClearingBackStack(route: T) =
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }

/** Bấm "Quay lại" nhanh 2 lần không pop luôn màn Camera (gây màn trắng). */
fun NavController.popBackStackSafely() {
    if (previousBackStackEntry != null) popBackStack()
}
