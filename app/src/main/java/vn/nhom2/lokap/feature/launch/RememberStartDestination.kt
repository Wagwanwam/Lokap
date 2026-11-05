package vn.nhom2.lokap.feature.launch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.data.fake.FakeAuthStore
import vn.nhom2.lokap.feature.auth.LoginRoute
import vn.nhom2.lokap.feature.auth.setup.SetupProfileRoute
import vn.nhom2.lokap.feature.onboarding.OnboardingRoute

/**
 * Route khởi đầu, chốt MỘT lần lúc mở app (đọc `.value`, không collect).
 * Nếu startDestination đổi theo trạng thái đăng nhập, NavHost dựng lại graph và reset back stack;
 * sau khi mở app, mọi chuyển màn do đăng nhập/đăng xuất đi bằng navigateClearingBackStack.
 */
@Composable
fun rememberStartDestination(): Any = remember { startDestinationFor() }

internal fun startDestinationFor(): Any {
    val user = FakeAuthStore.currentUser.value
    return when {
        !FakeAuthStore.onboardingDone.value -> OnboardingRoute
        user == null -> LoginRoute
        user.username.isBlank() -> SetupProfileRoute
        else -> Route.Camera
    }
}
