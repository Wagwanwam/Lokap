package vn.nhom2.lokap.feature.auth.verify

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.domain.repository.AuthRepository
import vn.nhom2.lokap.domain.repository.AuthSession
import vn.nhom2.lokap.domain.repository.isOffline

@HiltViewModel
class VerifyEmailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val auth: AuthRepository
) : ViewModel() {

    private val route: VerifyEmailRoute = runCatching { savedStateHandle.toRoute<VerifyEmailRoute>() }
        .getOrElse { VerifyEmailRoute(email = savedStateHandle.get<String>("email").orEmpty()) }

    private val _uiState = MutableStateFlow(VerifyEmailUiState(email = route.email))
    val uiState: StateFlow<VerifyEmailUiState> = _uiState.asStateFlow()

    private val _effect = Channel<VerifyEmailEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var countdownJob: Job? = null

    init {
        if (route.resendOnOpen) resend() else startCountdown()
    }

    fun onAction(action: VerifyEmailAction) {
        when (action) {
            VerifyEmailAction.ConfirmClicked -> check(silent = false)
            VerifyEmailAction.ScreenResumed -> check(silent = true)
            VerifyEmailAction.ResendClicked -> if (_uiState.value.canResend) resend()
            VerifyEmailAction.BackClicked -> viewModelScope.launch { _effect.send(VerifyEmailEffect.NavigateBack) }
        }
    }

    /** [silent]: tự kiểm tra khi quay lại app, chưa xác nhận thì không hiện lỗi. */
    private fun check(silent: Boolean) {
        val state = _uiState.value
        if (!state.canSubmit) return
        _uiState.update { it.copy(isChecking = true, errorMessage = if (silent) it.errorMessage else null, infoMessage = null) }
        viewModelScope.launch {
            auth.confirmEmailVerified().fold(
                onSuccess = {
                    _uiState.update { it.copy(isChecking = false) }
                    // Đã có hồ sơ (đăng ký từ máy khác) thì vào thẳng Camera
                    if (auth.session.value is AuthSession.SignedIn) {
                        _effect.send(VerifyEmailEffect.NavigateToCamera)
                    } else {
                        _effect.send(VerifyEmailEffect.NavigateToSetupProfile)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        if (silent && !error.isOffline) {
                            it.copy(isChecking = false)
                        } else {
                            it.copy(
                                isChecking = false,
                                isOffline = error.isOffline,
                                errorMessage = error.message ?: "Email chưa được xác nhận"
                            )
                        }
                    }
                }
            )
        }
    }

    private fun resend() {
        _uiState.update { it.copy(isResending = true, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            auth.resendVerificationEmail(_uiState.value.email).fold(
                onSuccess = {
                    _uiState.update { it.copy(isResending = false, infoMessage = "Đã gửi lại link") }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isResending = false,
                            isOffline = error.isOffline,
                            errorMessage = error.message ?: "Không gửi được link, thử lại sau"
                        )
                    }
                }
            )
            startCountdown()
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (left in RESEND_COOLDOWN_SECONDS downTo 0) {
                _uiState.update { it.copy(resendSecondsLeft = left) }
                if (left > 0) delay(1_000)
            }
        }
    }
}
