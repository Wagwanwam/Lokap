package vn.nhom2.lokap.feature.auth

import kotlinx.serialization.Serializable

@Serializable
data object LoginRoute

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailTouched: Boolean = false,
    val passwordTouched: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val serverError: String? = null,
    val isOffline: Boolean = false
) {
    val isBusy: Boolean
        get() = isLoading || isGoogleLoading
}

sealed interface LoginAction {
    data class EmailChanged(val value: String) : LoginAction
    data class PasswordChanged(val value: String) : LoginAction
    data object EmailFocusLost : LoginAction
    data object PasswordFocusLost : LoginAction
    data object TogglePasswordVisibility : LoginAction
    data object SubmitClicked : LoginAction
    data object GoogleSignInClicked : LoginAction
    data object RetryClicked : LoginAction
    data object ForgotPasswordClicked : LoginAction
    data object RegisterClicked : LoginAction
}

sealed interface LoginEffect {
    data object NavigateToCamera : LoginEffect
    data object NavigateToSetupProfile : LoginEffect
    data object NavigateToRegister : LoginEffect
    data object NavigateToForgotPassword : LoginEffect
}
