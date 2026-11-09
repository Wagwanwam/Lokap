package vn.nhom2.lokap.feature.launch

import androidx.compose.runtime.*
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.data.fake.FakeAuthStore
import vn.nhom2.lokap.feature.auth.LoginRoute
import vn.nhom2.lokap.feature.auth.setup.SetupProfileRoute
import vn.nhom2.lokap.feature.onboarding.OnboardingRoute
@Composable
fun rememberStartDestination(): Any = remember { startDestinationFor() }

internal fun startDestinationFor(): Any {
    val user = FakeAuthStore.currentUser.value
    return when {
        !FakeAuthStore.onboardingDone.value -> OnboardingRoute
        user == null -> LoginRoute
        user.username.isBlank() -> SetupProfileRoute
        else -> Route.Feed
    }
}
