package vn.nhom2.lokap.feature.auth

import kotlinx.serialization.Serializable
@Serializable
data object RegisterRoute

enum class PasswordStrength {
    NONE,
    WEAK,
    MEDIUM,
    STRONG
}

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val acceptedTerms: Boolean = false,
    val emailTouched: Boolean = false,
    val passwordTouched: Boolean = false,
    val confirmPasswordTouched: Boolean = false,
    val termsTouched: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val termsError: String? = null,
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val passwordStrength: PasswordStrength = PasswordStrength.NONE,
    val isLoading: Boolean = false,
    val serverError: String? = null,
    val isOffline: Boolean = false
)

sealed interface RegisterAction {
    data class EmailChanged(val value: String) : RegisterAction
    data class PasswordChanged(val value: String) : RegisterAction
    data class ConfirmPasswordChanged(val value: String) : RegisterAction
    data class AcceptedTermsChanged(val accepted: Boolean) : RegisterAction
    data object EmailFocusLost : RegisterAction
    data object PasswordFocusLost : RegisterAction
    data object ConfirmPasswordFocusLost : RegisterAction
    data object TogglePasswordVisibility : RegisterAction
    data object ToggleConfirmPasswordVisibility : RegisterAction
    data object SubmitClicked : RegisterAction
    data object RetryClicked : RegisterAction
    data object BackToLoginClicked : RegisterAction
}

sealed interface RegisterEffect {
    data object NavigateToSetupProfile : RegisterEffect
    data object NavigateBackToLogin : RegisterEffect
}
