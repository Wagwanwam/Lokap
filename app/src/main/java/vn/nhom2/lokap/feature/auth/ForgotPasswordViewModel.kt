package vn.nhom2.lokap.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.data.fake.AuthStoreException
import vn.nhom2.lokap.data.fake.FakeAuthStore

class ForgotPasswordViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    private val _effect = Channel<ForgotPasswordEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onAction(action: ForgotPasswordAction) {
        when (action) {
            is ForgotPasswordAction.EmailChanged -> {
                _uiState.update { state ->
                    state.copy(
                        email = action.value,
                        emailError = if (state.emailTouched) AuthValidators.validateEmail(action.value) else null,
                        serverError = null,
                        isOffline = false
                    )
                }
            }

            ForgotPasswordAction.EmailFocusLost -> {
                _uiState.update { state ->
                    state.copy(
                        emailTouched = true,
                        emailError = AuthValidators.validateEmail(state.email)
                    )
                }
            }

            ForgotPasswordAction.SubmitClicked, ForgotPasswordAction.RetryClicked -> submitReset()

            ForgotPasswordAction.BackToLoginClicked -> {
                viewModelScope.launch {
                    _effect.send(ForgotPasswordEffect.NavigateBackToLogin)
                }
            }
        }
    }

    private fun submitReset() {
        val current = _uiState.value
        if (current.isLoading) return

        val emailError = AuthValidators.validateEmail(current.email)
        if (emailError != null) {
            _uiState.update {
                it.copy(
                    emailTouched = true,
                    emailError = emailError
                )
            }
            return
        }

        val targetEmail = current.email.trim()
        _uiState.update {
            it.copy(
                isLoading = true,
                serverError = null,
                isOffline = false
            )
        }

        viewModelScope.launch {
            val result = FakeAuthStore.resetPassword(targetEmail)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            submittedEmail = targetEmail
                        )
                    }
                },
                onFailure = { error ->
                    val isOffline = error is AuthStoreException.NetworkError
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isOffline = isOffline,
                            serverError = error.message ?: "Đã xảy ra lỗi"
                        )
                    }
                }
            )
        }
    }
}
