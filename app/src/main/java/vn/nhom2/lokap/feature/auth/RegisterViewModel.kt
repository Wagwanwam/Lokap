package vn.nhom2.lokap.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.data.fake.AuthStoreException
import vn.nhom2.lokap.data.fake.FakeAuthStore

class RegisterViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _effect = Channel<RegisterEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var emailCheckJob: Job? = null

    fun onAction(action: RegisterAction) {
        when (action) {
            is RegisterAction.EmailChanged -> onEmailInputChanged(action.value)

            is RegisterAction.PasswordChanged -> {
                _uiState.update { state ->
                    state.copy(
                        password = action.value,
                        passwordError = if (state.passwordTouched) AuthValidators.validatePassword(action.value) else null,
                        confirmPasswordError = if (state.confirmPasswordTouched) {
                            AuthValidators.validateConfirmPassword(action.value, state.confirmPassword)
                        } else {
                            null
                        },
                        serverError = null,
                        isOffline = false
                    )
                }
            }

            is RegisterAction.ConfirmPasswordChanged -> {
                _uiState.update { state ->
                    state.copy(
                        confirmPassword = action.value,
                        confirmPasswordError = if (state.confirmPasswordTouched) {
                            AuthValidators.validateConfirmPassword(state.password, action.value)
                        } else {
                            null
                        },
                        serverError = null,
                        isOffline = false
                    )
                }
            }

            is RegisterAction.AcceptedTermsChanged -> {
                _uiState.update { state ->
                    state.copy(
                        acceptedTerms = action.accepted,
                        termsTouched = true,
                        termsError = if (action.accepted) null else "Bạn cần đồng ý Điều khoản & Chính sách riêng tư"
                    )
                }
            }

            RegisterAction.EmailFocusLost -> {
                _uiState.update { state ->
                    state.copy(
                        emailTouched = true,
                        emailError = emailErrorFor(state)
                    )
                }
            }

            RegisterAction.PasswordFocusLost -> {
                _uiState.update { state ->
                    state.copy(
                        passwordTouched = true,
                        passwordError = AuthValidators.validatePassword(state.password)
                    )
                }
            }

            RegisterAction.ConfirmPasswordFocusLost -> {
                _uiState.update { state ->
                    state.copy(
                        confirmPasswordTouched = true,
                        confirmPasswordError = AuthValidators.validateConfirmPassword(
                            state.password,
                            state.confirmPassword
                        )
                    )
                }
            }

            RegisterAction.TogglePasswordVisibility -> {
                _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }

            RegisterAction.ToggleConfirmPasswordVisibility -> {
                _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
            }

            RegisterAction.SubmitClicked, RegisterAction.RetryClicked -> submitRegister()

            RegisterAction.BackToLoginClicked -> {
                viewModelScope.launch {
                    _effect.send(RegisterEffect.NavigateBackToLogin)
                }
            }
        }
    }

    private fun onEmailInputChanged(value: String) {
        emailCheckJob?.cancel()

        val formatError = AuthValidators.validateEmail(value)
        if (formatError != null) {
            _uiState.update { state ->
                state.copy(
                    email = value,
                    emailAvailability = EmailAvailability.IDLE,
                    emailError = if (state.emailTouched) formatError else null,
                    serverError = null,
                    isOffline = false
                )
            }
            return
        }

        _uiState.update { state ->
            state.copy(
                email = value,
                emailAvailability = EmailAvailability.CHECKING,
                emailError = null,
                serverError = null,
                isOffline = false
            )
        }

        emailCheckJob = viewModelScope.launch {
            delay(400) // debounce: đợi ngừng gõ rồi mới hỏi
            val result = FakeAuthStore.isEmailAvailable(value)
            _uiState.update { state ->
                if (state.email != value) return@update state
                result.fold(
                    onSuccess = { available ->
                        state.copy(
                            emailAvailability = if (available) EmailAvailability.AVAILABLE else EmailAvailability.TAKEN,
                            emailError = if (available) null else AuthStoreException.EmailAlreadyTaken.MESSAGE
                        )
                    },
                    // Không đặt serverError: nút Thử lại của nó gửi cả form
                    onFailure = {
                        state.copy(
                            emailAvailability = EmailAvailability.IDLE,
                            isOffline = true
                        )
                    }
                )
            }
        }
    }

    private fun emailErrorFor(state: RegisterUiState): String? =
        AuthValidators.validateEmail(state.email)
            ?: if (state.emailAvailability == EmailAvailability.TAKEN) AuthStoreException.EmailAlreadyTaken.MESSAGE else null

    private fun submitRegister() {
        val current = _uiState.value
        if (current.isLoading) return

        val emailError = emailErrorFor(current)
        val passwordError = AuthValidators.validatePassword(current.password)
        val confirmError = AuthValidators.validateConfirmPassword(current.password, current.confirmPassword)
        val termsError = if (current.acceptedTerms) {
            null
        } else {
            "Bạn cần đồng ý Điều khoản & Chính sách riêng tư"
        }

        if (emailError != null || passwordError != null || confirmError != null || termsError != null) {
            _uiState.update {
                it.copy(
                    emailTouched = true,
                    passwordTouched = true,
                    confirmPasswordTouched = true,
                    termsTouched = true,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmError,
                    termsError = termsError
                )
            }
            return
        }

        // Kết quả kiểm tra về muộn không được ghi đè kết quả đăng ký; register() tự báo trùng
        emailCheckJob?.cancel()
        _uiState.update {
            it.copy(
                isLoading = true,
                emailAvailability = if (it.emailAvailability == EmailAvailability.CHECKING) {
                    EmailAvailability.IDLE
                } else {
                    it.emailAvailability
                },
                serverError = null,
                isOffline = false
            )
        }

        viewModelScope.launch {
            val result = FakeAuthStore.register(current.email, current.password)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.send(RegisterEffect.NavigateToSetupProfile)
                },
                onFailure = { error ->
                    val isOffline = error is AuthStoreException.NetworkError
                    val emailTakenError = if (error is AuthStoreException.EmailAlreadyTaken) {
                        error.message
                    } else {
                        null
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isOffline = isOffline,
                            emailAvailability = if (emailTakenError != null) EmailAvailability.TAKEN else it.emailAvailability,
                            emailError = emailTakenError ?: it.emailError,
                            serverError = if (emailTakenError == null) error.message else null
                        )
                    }
                }
            )
        }
    }
}
