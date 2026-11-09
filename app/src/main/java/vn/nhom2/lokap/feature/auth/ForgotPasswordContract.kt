package vn.nhom2.lokap.feature.auth

import kotlinx.serialization.Serializable
@Serializable
data object ForgotPasswordRoute

data class ForgotPasswordUiState(
    val email: String = "",
    val emailTouched: Boolean = false,
    val emailError: String? = null,
    val isLoading: Boolean = false,
    val submittedEmail: String? = null,
    val serverError: String? = null,
    val isOffline: Boolean = false
)

sealed interface ForgotPasswordAction {
    data class EmailChanged(val value: String) : ForgotPasswordAction
    data object EmailFocusLost : ForgotPasswordAction
    data object SubmitClicked : ForgotPasswordAction
    data object RetryClicked : ForgotPasswordAction
    data object BackToLoginClicked : ForgotPasswordAction
}

sealed interface ForgotPasswordEffect {
    data object NavigateBackToLogin : ForgotPasswordEffect
}
