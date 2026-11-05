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

class RegisterViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _effect = Channel<RegisterEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onAction(action: RegisterAction) {
        when (action) {
            is RegisterAction.EmailChanged -> {
                _uiState.update { state ->
                    state.copy(
                        email = action.value,
                        emailError = if (state.emailTouched) AuthValidators.validateEmail(action.value) else null,
                        serverError = null,
                        isOffline = false
                    )
                }
            }

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
                        emailError = AuthValidators.validateEmail(state.email)
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

    private fun submitRegister() {
        val current = _uiState.value
        if (current.isLoading) return

        val emailError = AuthValidators.validateEmail(current.email)
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

        _uiState.update {
            it.copy(
                isLoading = true,
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
                            emailError = emailTakenError ?: it.emailError,
                            serverError = if (emailTakenError == null) error.message else null
                        )
                    }
                }
            )
        }
    }
}
