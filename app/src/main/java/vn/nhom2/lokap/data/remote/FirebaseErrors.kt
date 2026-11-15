package vn.nhom2.lokap.data.remote

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import vn.nhom2.lokap.domain.repository.AppException
import vn.nhom2.lokap.domain.repository.AuthStoreException

/*
 * Đổi lỗi của Firebase / Cloudinary sang câu tiếng Việt cho người dùng.
 * Security Rules từ chối → PERMISSION_DENIED (không có mã lỗi chi tiết như RPC của Postgres),
 * nên app tự kiểm tra trước các điều kiện hay gặp (giới hạn 20 bạn, tài khoản < 24 giờ…) để báo đúng câu.
 */

fun Throwable.isNetworkError(): Boolean = when {
    this is IOException || this is FirebaseNetworkException -> true
    this is FirebaseFirestoreException &&
        (code == FirebaseFirestoreException.Code.UNAVAILABLE || code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED) -> true
    else -> cause?.let { it !== this && it.isNetworkError() } == true
}

/** Lỗi tạm thời, thử lại sau là được (dùng cho upload nền). */
fun Throwable.isRetryable(): Boolean = isNetworkError() || this is CloudinaryException && retryable ||
    this is FirebaseFirestoreException && code in setOf(
        FirebaseFirestoreException.Code.ABORTED,
        FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED,
        FirebaseFirestoreException.Code.INTERNAL
    )

val Throwable.isPermissionDenied: Boolean
    get() = this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.PERMISSION_DENIED

fun Throwable.toAppException(fallback: String = "Đã xảy ra lỗi, thử lại sau"): AppException = when {
    this is AppException -> this
    isNetworkError() -> AppException("Mất kết nối mạng. Kiểm tra mạng rồi thử lại.", AppException.Reason.OFFLINE, this)
    this is FirebaseFirestoreException -> when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            AppException("Bạn không có quyền xem hoặc sửa nội dung này", AppException.Reason.OTHER, this)
        FirebaseFirestoreException.Code.NOT_FOUND ->
            AppException("Bài đã bị xoá hoặc ẩn", AppException.Reason.NOT_FOUND, this)
        FirebaseFirestoreException.Code.UNAUTHENTICATED ->
            AppException("Phiên đăng nhập đã hết, hãy đăng nhập lại", AppException.Reason.NOT_SIGNED_IN, this)
        FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
            AppException("Máy chủ đang quá tải, thử lại sau", AppException.Reason.OTHER, this)
        else -> AppException(fallback, AppException.Reason.OTHER, this)
    }
    this is FirebaseTooManyRequestsException -> AppException("Bạn thao tác quá nhanh. Đợi một lát rồi thử lại.", AppException.Reason.OTHER, this)
    this is CloudinaryException -> AppException(message ?: fallback, AppException.Reason.OTHER, this)
    else -> AppException(fallback, AppException.Reason.OTHER, this)
}

fun Throwable.toAuthException(): AuthStoreException = when {
    this is AuthStoreException -> this
    isNetworkError() -> AuthStoreException.NetworkError()
    this is FirebaseAuthWeakPasswordException -> AuthStoreException.WeakPassword()
    this is FirebaseAuthUserCollisionException -> when (errorCode) {
        "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> AuthStoreException.AccountExistsWithPassword()
        else -> AuthStoreException.EmailAlreadyTaken()
    }
    this is FirebaseAuthRecentLoginRequiredException -> AuthStoreException.RecentLoginRequired()
    // Bật "chống dò email" (mặc định ở project mới): sai mật khẩu và không có tài khoản đều là INVALID_CREDENTIAL
    this is FirebaseAuthInvalidCredentialsException -> when (errorCode) {
        "ERROR_INVALID_EMAIL" -> AuthStoreException.Unknown("Email sai định dạng")
        else -> AuthStoreException.InvalidCredentials()
    }
    this is FirebaseAuthInvalidUserException -> when (errorCode) {
        "ERROR_USER_DISABLED" -> AuthStoreException.Unknown("Tài khoản đã bị khoá")
        else -> AuthStoreException.InvalidCredentials()
    }
    // Provider (vd Google) chưa bật trong Firebase Console → Authentication → Sign-in method
    this is FirebaseAuthException && errorCode == "ERROR_OPERATION_NOT_ALLOWED" ->
        AuthStoreException.Unknown("Cách đăng nhập này chưa được bật trên Firebase")
    this is FirebaseTooManyRequestsException -> AuthStoreException.RateLimited()
    this is FirebaseFirestoreException || this is AppException || this is CloudinaryException ->
        AuthStoreException.Unknown(toAppException().message ?: "Đã xảy ra lỗi, thử lại sau")
    else -> AuthStoreException.Unknown("Đã xảy ra lỗi, thử lại sau")
}

/** Lỗi khi upload ảnh lên Cloudinary. [retryable]: lỗi mạng / máy chủ, thử lại sau được. */
class CloudinaryException(message: String, val retryable: Boolean, cause: Throwable? = null) : Exception(message, cause)
