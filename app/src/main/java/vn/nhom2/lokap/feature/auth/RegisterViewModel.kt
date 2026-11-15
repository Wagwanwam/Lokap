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
import vn.nhom2.lokap.domain.repository.AuthStoreException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import vn.nhom2.lokap.domain.repository.AuthRepository
import vn.nhom2.lokap.domain.repository.RegisterResult
import vn.nhom2.lokap.domain.repository.isOffline
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val auth: AuthRepository
) : ViewModel() {

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
                        passwordStrength = AuthValidators.calculatePasswordStrength(action.value),
                        passwordError = if (state.passwordError != null) AuthValidators.validatePassword(action.value) else null,
                        confirmPasswordError = if (state.confirmPasswordError != null) {
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
                        confirmPasswordError = if (state.confirmPasswordError != null) {
                            AuthValidators.validateConfirmPassword(state.password, action.value)
                        } else {
                            null
                        },
                        serverError = null
                    )
                }
            }

            is RegisterAction.AcceptedTermsChanged -> {
                _uiState.update { state ->
                    state.copy(
                        acceptedTerms = action.accepted,
                        termsError = if (action.accepted) null else state.termsError
                    )
                }
            }

            RegisterAction.EmailFocusLost -> {
                _uiState.update { state ->
                    state.copy(
                        emailTouched = state.email.isNotBlank(),
                        // Ô trống thì chưa báo lỗi; có chữ thì báo sai định dạng hoặc email đã có người dùng
                        emailError = if (state.email.isNotBlank()) emailErrorFor(state) else null
                    )
                }
            }

            RegisterAction.PasswordFocusLost -> {
                _uiState.update { state ->
                    val err = if (state.password.isNotBlank()) AuthValidators.validatePassword(state.password) else null
                    state.copy(
                        passwordTouched = state.password.isNotBlank(),
                        passwordError = err
                    )
                }
            }

            RegisterAction.ConfirmPasswordFocusLost -> {
                _uiState.update { state ->
                    val err = if (state.confirmPassword.isNotBlank()) {
                        AuthValidators.validateConfirmPassword(state.password, state.confirmPassword)
                    } else {
                        null
                    }
                    state.copy(
                        confirmPasswordTouched = state.confirmPassword.isNotBlank(),
                        confirmPasswordError = err
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
            val result = auth.isEmailAvailable(value)
            _uiState.update { state ->
                if (state.email != value) return@update state
                result.fold(
                    onSuccess = { available ->
                        state.copy(
                            emailAvailability = if (available) EmailAvailability.AVAILABLE else EmailAvailability.TAKEN,
                            emailError = if (available) null else AuthStoreException.EmailAlreadyTaken.MESSAGE
                        )
                    },
                    // Không đặt serverError: nút Thử lại của nó gửi cả form.
                    // Lỗi khác mất mạng (ví dụ kiểm tra quá nhiều lần) thì chỉ bỏ qua, lúc gửi form server tự báo trùng.
                    onFailure = { error ->
                        state.copy(
                            emailAvailability = EmailAvailability.IDLE,
                            isOffline = error.isOffline
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
            val result = auth.register(
                email = current.email,
                password = current.password
            )
            result.fold(
                onSuccess = { registered ->
                    _uiState.update { it.copy(isLoading = false) }
                    when (registered) {
                        RegisterResult.SignedIn -> _effect.send(RegisterEffect.NavigateToSetupProfile)
                        // Firebase gửi link xác nhận: bấm link trước khi tạo hồ sơ
                        is RegisterResult.NeedsEmailVerification ->
                            _effect.send(RegisterEffect.NavigateToVerifyEmail(registered.email))
                    }
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
