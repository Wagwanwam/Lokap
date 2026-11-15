package vn.nhom2.lokap.feature.launch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.domain.repository.AuthRepository
import vn.nhom2.lokap.domain.repository.AuthSession
import vn.nhom2.lokap.feature.auth.LoginRoute
import vn.nhom2.lokap.feature.auth.setup.SetupProfileRoute
import vn.nhom2.lokap.feature.onboarding.OnboardingRoute

/** Màn đầu tiên khi mở app, tính từ phiên đăng nhập thật (Firebase Auth tự khôi phục phiên đã lưu). */
@HiltViewModel
class LaunchViewModel @Inject constructor(auth: AuthRepository) : ViewModel() {

    /** null = đang khôi phục phiên, chưa biết vào đâu. */
    val startDestination: StateFlow<Any?> = combine(auth.session, auth.onboardingDone) { session, done ->
        startDestinationFor(session, done)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        startDestinationFor(auth.session.value, auth.onboardingDone.value)
    )
}

internal fun startDestinationFor(session: AuthSession, onboardingDone: Boolean): Any? = when {
    // Giới thiệu không cần biết phiên: vào ngay, không bắt đợi mạng
    !onboardingDone -> OnboardingRoute
    session is AuthSession.Loading -> null
    session is AuthSession.SignedOut -> LoginRoute
    session is AuthSession.NeedsProfile -> SetupProfileRoute
    else -> Route.Feed
}

/**
 * Chốt điểm bắt đầu MỘT LẦN lúc mở app (HANDOFF: đổi startDestination sau đó làm NavHost dựng lại graph,
 * mất back stack). Đăng nhập / đăng xuất sau đó chỉ điều hướng bằng navigate(...) { popUpTo(0) }.
 */
@Composable
fun rememberStartDestination(viewModel: LaunchViewModel = hiltViewModel()): Any? {
    val live by viewModel.startDestination.collectAsStateWithLifecycle()
    // Khoá một chiều: giữ giá trị đầu tiên khác null, phiên đổi sau đó không dựng lại graph
    val locked = remember { mutableStateOf(live) }
    LaunchedEffect(live) {
        if (locked.value == null && live != null) locked.value = live
    }
    return locked.value
}
