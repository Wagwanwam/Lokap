package vn.nhom2.lokap.feature.auth.verify

import kotlinx.serialization.Serializable

/**
 * Màn chờ xác nhận email: Firebase gửi link tới email, người dùng bấm link rồi quay lại app.
 * Quay lại app là tự kiểm tra; nút "Đã xác nhận" để kiểm tra lại bằng tay.
 * [resendOnOpen] = true khi đến từ màn Đăng nhập (đăng ký rồi nhưng chưa xác nhận): tự gửi lại link.
 */
@Serializable
data class VerifyEmailRoute(val email: String, val resendOnOpen: Boolean = false)

const val RESEND_COOLDOWN_SECONDS = 60

data class VerifyEmailUiState(
    val email: String = "",
    val isChecking: Boolean = false,
    val isResending: Boolean = false,
    val resendSecondsLeft: Int = RESEND_COOLDOWN_SECONDS,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isOffline: Boolean = false
) {
    val canSubmit: Boolean
        get() = !isChecking

    val canResend: Boolean
        get() = resendSecondsLeft == 0 && !isResending && !isChecking
}

sealed interface VerifyEmailAction {
    /** Bấm "Đã xác nhận". */
    data object ConfirmClicked : VerifyEmailAction
    /** Quay lại app (vd từ app Gmail): kiểm tra im lặng, chưa xác nhận thì không báo lỗi. */
    data object ScreenResumed : VerifyEmailAction
    data object ResendClicked : VerifyEmailAction
    data object BackClicked : VerifyEmailAction
}

sealed interface VerifyEmailEffect {
    data object NavigateToSetupProfile : VerifyEmailEffect
    data object NavigateToCamera : VerifyEmailEffect
    data object NavigateBack : VerifyEmailEffect
}
