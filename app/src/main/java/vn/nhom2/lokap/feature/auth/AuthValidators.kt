package vn.nhom2.lokap.feature.auth

object AuthValidators {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val USERNAME_REGEX = Regex("^[a-z0-9._]{3,20}$")

    fun validateEmail(email: String): String? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> "Email không được để trống"
            !EMAIL_REGEX.matches(trimmed) -> "Email sai định dạng (ví dụ: ban@lokap.vn)"
            else -> null
        }
    }

    fun validatePassword(password: String): String? {
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        return when {
            password.isEmpty() -> "Mật khẩu không được để trống"
            password.length < 8 -> "Mật khẩu phải có ít nhất 8 ký tự"
            !hasLetter || !hasDigit -> "Mật khẩu phải bao gồm cả chữ và số"
            else -> null
        }
    }

    /** Lúc đăng nhập chỉ cần không trống; sai thì server báo chung "Email hoặc mật khẩu không đúng". */
    fun validateLoginPassword(password: String): String? =
        if (password.isEmpty()) "Mật khẩu không được để trống" else null

    fun validateConfirmPassword(password: String, confirmPassword: String): String? {
        return when {
            confirmPassword.isEmpty() -> "Vui lòng nhập lại mật khẩu"
            password != confirmPassword -> "Mật khẩu nhập lại không khớp"
            else -> null
        }
    }

    fun validateDisplayName(displayName: String): String? {
        val trimmed = displayName.trim()
        return when {
            trimmed.isEmpty() -> "Tên hiển thị không được để trống"
            trimmed.length < 2 -> "Tên hiển thị cần ít nhất 2 ký tự"
            trimmed.length > 30 -> "Tên hiển thị tối đa 30 ký tự"
            else -> null
        }
    }

    fun validateUsernameFormat(username: String): String? {
        return when {
            username.isEmpty() -> "Username không được để trống"
            username.length < 3 -> "Username cần ít nhất 3 ký tự"
            username.length > 20 -> "Username tối đa 20 ký tự"
            !USERNAME_REGEX.matches(username) -> "Chỉ dùng chữ thường a–z, số 0–9, dấu chấm (.) và gạch dưới (_)"
            else -> null
        }
    }
}
