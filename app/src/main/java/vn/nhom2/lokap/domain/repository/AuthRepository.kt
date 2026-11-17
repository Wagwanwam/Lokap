package vn.nhom2.lokap.domain.repository

import kotlinx.coroutines.flow.StateFlow
import vn.nhom2.lokap.domain.model.User

/** Trạng thái phiên đăng nhập, dùng để chọn màn đầu tiên khi mở app. */
sealed interface AuthSession {
    /** Đang khôi phục phiên đã lưu (chưa biết đã đăng nhập hay chưa). */
    data object Loading : AuthSession
    data object SignedOut : AuthSession
    /** Đã có tài khoản nhưng chưa tạo hồ sơ (username, tên hiển thị). */
    data class NeedsProfile(val userId: String, val email: String?) : AuthSession
    data class SignedIn(val user: User) : AuthSession
}

sealed interface RegisterResult {
    /** Đăng ký xong và đã đăng nhập: sang Thiết lập hồ sơ. */
    data object SignedIn : RegisterResult
    /** Firebase đã gửi link xác nhận tới email: bấm link rồi quay lại app mới tạo hồ sơ được. */
    data class NeedsEmailVerification(val email: String) : RegisterResult
}

/** Thông tin ở màn Thiết lập hồ sơ (bước 2 của đăng ký). */
data class ProfileDraft(
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val bio: String = "",
    val birthYear: Int? = null,
    val homeProvinceCode: String? = null
)

interface AuthRepository {
    val session: StateFlow<AuthSession>

    /** Người đang đăng nhập đã có hồ sơ; null khi chưa đăng nhập hoặc chưa tạo hồ sơ. */
    val currentUser: StateFlow<User?>

    val onboardingDone: StateFlow<Boolean>

    fun markOnboardingDone()

    /** Thành công → User; username rỗng nghĩa là chưa tạo hồ sơ. */
    suspend fun login(email: String, password: String): Result<User>

    /**
     * Đăng nhập bằng ID token Google (màn Đăng nhập lấy qua Credential Manager).
     * Lần đầu thì Firebase tự tạo tài khoản → username rỗng → sang Thiết lập hồ sơ.
     */
    suspend fun loginWithGoogle(idToken: String): Result<User>

    suspend fun register(email: String, password: String): Result<RegisterResult>

    /**
     * Người dùng báo đã bấm link xác nhận: tải lại tài khoản, đã xác nhận thì vào bước hồ sơ.
     * Chưa xác nhận → AuthStoreException.EmailNotConfirmed.
     */
    suspend fun confirmEmailVerified(): Result<Unit>

    suspend fun resendVerificationEmail(email: String): Result<Unit>

    /** Bản Firebase không hỏi được (chống dò email) → trả lỗi Unsupported, lúc gửi form sẽ báo trùng. */
    suspend fun isEmailAvailable(email: String): Result<Boolean>

    suspend fun isUsernameAvailable(username: String): Result<Boolean>

    suspend fun completeProfile(draft: ProfileDraft): Result<User>

    /** Sửa hồ sơ (màn Hồ sơ / Cài đặt). Username trùng → AuthStoreException.UsernameAlreadyTaken. */
    suspend fun updateProfile(displayName: String, username: String, bio: String): Result<User>

    /** Xác thực lại mật khẩu hiện tại trước khi đổi; không tự thêm mật khẩu cho tài khoản Google. */
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>

    suspend fun resetPassword(email: String): Result<Unit>

    suspend fun logout()

    /** Xoá tài khoản và toàn bộ dữ liệu trên Firestore (không hoàn tác). */
    suspend fun deleteAccount(): Result<Unit>
}
