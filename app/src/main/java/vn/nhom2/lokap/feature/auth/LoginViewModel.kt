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
import vn.nhom2.lokap.domain.repository.AuthStoreException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import vn.nhom2.lokap.domain.repository.AuthRepository
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _effect = Channel<LoginEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var lastAttemptWasGoogle = false

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.EmailChanged -> {
                _uiState.update { state ->
                    state.copy(
                        email = action.value,
                        emailError = if (state.emailTouched) AuthValidators.validateEmail(action.value) else null,
                        serverError = null,
                        isOffline = false
                    )
                }
            }

            is LoginAction.PasswordChanged -> {
                _uiState.update { state ->
                    state.copy(
                        password = action.value,
                        passwordError = if (state.passwordTouched) AuthValidators.validateLoginPassword(action.value) else null,
                        serverError = null,
                        isOffline = false
                    )
                }
            }

            LoginAction.EmailFocusLost -> {
                _uiState.update { state ->
                    state.copy(
                        emailTouched = true,
                        emailError = AuthValidators.validateEmail(state.email)
                    )
                }
            }

            LoginAction.PasswordFocusLost -> {
                _uiState.update { state ->
                    state.copy(
                        passwordTouched = true,
                        passwordError = AuthValidators.validateLoginPassword(state.password)
                    )
                }
            }

            LoginAction.TogglePasswordVisibility -> {
                _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }

            LoginAction.SubmitClicked -> submitEmailLogin()

            LoginAction.GoogleSignInClicked -> requestGoogleSignIn()

            is LoginAction.GoogleSignInFinished -> onGoogleSignInFinished(action.result)

            LoginAction.RetryClicked -> {
                if (lastAttemptWasGoogle) {
                    requestGoogleSignIn()
                } else {
                    submitEmailLogin()
                }
            }

            LoginAction.ForgotPasswordClicked -> {
                viewModelScope.launch {
                    _effect.send(LoginEffect.NavigateToForgotPassword)
                }
            }

            LoginAction.RegisterClicked -> {
                viewModelScope.launch {
                    _effect.send(LoginEffect.NavigateToRegister)
                }
            }
        }
    }

    private fun submitEmailLogin() {
        val current = _uiState.value
        if (current.isBusy) return

        val emailError = AuthValidators.validateEmail(current.email)
        val passwordError = AuthValidators.validateLoginPassword(current.password)

        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(
                    emailTouched = true,
                    passwordTouched = true,
                    emailError = emailError,
                    passwordError = passwordError
                )
            }
            return
        }

        lastAttemptWasGoogle = false
        _uiState.update {
            it.copy(
                isLoading = true,
                serverError = null,
                isOffline = false
            )
        }

        viewModelScope.launch {
            val result = auth.login(current.email, current.password)
            result.fold(
                onSuccess = { user ->

                    if (user.username.isBlank()) {
                        _effect.send(LoginEffect.NavigateToSetupProfile)
                    } else {
                        _effect.send(LoginEffect.NavigateToCamera)
                    }
                },
                onFailure = { error ->
                    if (error is AuthStoreException.EmailNotConfirmed) {
                        // Đã đăng ký nhưng chưa bấm link xác nhận: sang màn Xác nhận email, màn đó tự gửi lại link
                        _uiState.update { it.copy(isLoading = false) }
                        _effect.send(LoginEffect.NavigateToVerifyEmail(current.email.trim()))
                    } else {
                        val isOffline = error is AuthStoreException.NetworkError
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isOffline = isOffline,
                                serverError = error.message ?: "Đã xảy ra lỗi"
                            )
                        }
                    }
                }
            )
        }
    }

    /** Bước 1: giữ trạng thái chờ rồi nhờ màn hình mở hộp chọn tài khoản Google (cần Activity). */
    private fun requestGoogleSignIn() {
        if (_uiState.value.isBusy) return

        lastAttemptWasGoogle = true
        _uiState.update {
            it.copy(
                isGoogleLoading = true,
                serverError = null,
                isOffline = false
            )
        }
        viewModelScope.launch { _effect.send(LoginEffect.LaunchGoogleSignIn) }
    }

    /** Bước 2: hộp chọn tài khoản đóng lại. Có ID token thì đăng nhập Firebase, người dùng tự đóng thì thôi. */
    private fun onGoogleSignInFinished(result: GoogleSignInResult) {
        if (!_uiState.value.isGoogleLoading) return
        when (result) {
            is GoogleSignInResult.Success -> submitGoogleLogin(result.idToken)
            GoogleSignInResult.Cancelled -> _uiState.update { it.copy(isGoogleLoading = false) }
            is GoogleSignInResult.Failure -> _uiState.update {
                it.copy(isGoogleLoading = false, serverError = result.message)
            }
        }
    }

    private fun submitGoogleLogin(idToken: String) {
        viewModelScope.launch {
            val result = auth.loginWithGoogle(idToken)
            result.fold(
                onSuccess = { user ->
                    if (user.username.isBlank()) {
                        _effect.send(LoginEffect.NavigateToSetupProfile)
                    } else {
                        _effect.send(LoginEffect.NavigateToCamera)
                    }
                },
                onFailure = { error ->
                    val isOffline = error is AuthStoreException.NetworkError
                    _uiState.update {
                        it.copy(
                            isGoogleLoading = false,
                            isOffline = isOffline,
                            serverError = error.message ?: "Không thể kết nối với Google"
                        )
                    }
                }
            )
        }
    }
}
