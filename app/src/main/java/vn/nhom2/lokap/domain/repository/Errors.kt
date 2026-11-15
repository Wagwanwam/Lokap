package vn.nhom2.lokap.domain.repository

/**
 * Lỗi đăng nhập/đăng ký. Repository trả các lớp này
 * nên ViewModel không cần biết đang chạy dữ liệu nào.
 */
sealed class AuthStoreException(override val message: String) : Exception(message) {
    class InvalidCredentials : AuthStoreException("Email hoặc mật khẩu không đúng")
    class EmailAlreadyTaken : AuthStoreException(MESSAGE) {
        companion object {
            // Màn Đăng ký dùng lại câu này khi báo trùng lúc đang gõ
            const val MESSAGE = "Email này đã được đăng ký tài khoản khác"
        }
    }
    /** Đăng nhập Google nhưng email đó đã có tài khoản mật khẩu mà Firebase không tự gộp (email không phải Gmail). */
    class AccountExistsWithPassword : AuthStoreException("Email này đã đăng ký bằng mật khẩu. Hãy đăng nhập bằng email.")
    class UsernameAlreadyTaken : AuthStoreException("Username này đã có người dùng")
    class NetworkError : AuthStoreException("Mất kết nối mạng. Vui lòng kiểm tra và thử lại.")

    /** Đã đăng ký nhưng chưa bấm link xác nhận trong email. */
    class EmailNotConfirmed(val email: String) : AuthStoreException("Email chưa được xác nhận. Mở email và bấm link xác nhận.")
    /** Firebase yêu cầu đăng nhập lại trước khi đổi mật khẩu / xoá tài khoản. */
    class RecentLoginRequired : AuthStoreException("Vì an toàn, hãy đăng xuất rồi đăng nhập lại trước khi làm việc này")
    class WeakPassword : AuthStoreException("Mật khẩu cần ít nhất 8 ký tự, có cả chữ và số")
    class RateLimited : AuthStoreException("Bạn thao tác quá nhanh. Đợi một lát rồi thử lại.")
    class Unsupported(message: String) : AuthStoreException(message)
    class Unknown(message: String) : AuthStoreException(message)
}

/** Lỗi chung khi gọi dữ liệu (bài đăng, bạn bè, cài đặt). [message] là câu tiếng Việt hiện cho người dùng. */
class AppException(
    message: String,
    val reason: Reason = Reason.OTHER,
    cause: Throwable? = null
) : Exception(message, cause) {
    enum class Reason {
        OFFLINE,
        NOT_SIGNED_IN,
        NOT_FOUND,
        FRIEND_LIMIT,
        FRIEND_LIMIT_OTHER,
        ACCOUNT_TOO_NEW,
        OTHER
    }
}

/** true khi lỗi là do mất mạng (để hiện OfflineBanner thay cho thông báo lỗi). */
val Throwable.isOffline: Boolean
    get() = (this is AppException && reason == AppException.Reason.OFFLINE) || this is AuthStoreException.NetworkError
