package vn.nhom2.lokap.core.navigation

import androidx.navigation.NavController
fun NavController.navigateSingleTop(route: Route) =
    navigate(route) { launchSingleTop = true }

fun <T : Any> NavController.navigateClearingBackStack(route: T) =
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }

fun NavController.popBackStackSafely() {
    if (previousBackStackEntry != null) popBackStack()
}
