package vn.nhom2.lokap.core.navigation

import androidx.navigation.NavController

/** Bấm nhanh 2 lần không mở trùng màn hình. */
fun NavController.navigateSingleTop(route: Route) =
    navigate(route) { launchSingleTop = true }

/** Bấm "Quay lại" nhanh 2 lần không pop luôn màn Camera (gây màn trắng). */
fun NavController.popBackStackSafely() {
    if (previousBackStackEntry != null) popBackStack()
}
