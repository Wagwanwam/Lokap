package vn.nhom2.lokap.feature.auth

object AuthValidators {
    private val EMAIL_LOCAL_PART_REGEX = Regex("^[A-Za-z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#\$%&'*+/=?^_`{|}~-]+)*$")
    private val EMAIL_DOMAIN_LABEL_REGEX = Regex("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?$")
    private val EMAIL_TLD_REGEX = Regex("^[A-Za-z]{2,63}$")
    private val USERNAME_REGEX = Regex("^[a-z0-9._]{3,20}$")

    fun validateEmail(email: String): String? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> "Email không được để trống"
            !isValidEmailAddress(trimmed) -> "Email sai định dạng (ví dụ: ban@lokap.vn)"
            else -> null
        }
    }

    private fun isValidEmailAddress(email: String): Boolean {
        if (email.length > 254 || email.count { it == '@' } != 1) return false
        val localPart = email.substringBefore('@')
        val domain = email.substringAfter('@')
        if (localPart.length !in 1..64 || !EMAIL_LOCAL_PART_REGEX.matches(localPart)) return false
        val labels = domain.split('.')
        return labels.size >= 2 && labels.all(EMAIL_DOMAIN_LABEL_REGEX::matches) &&
            EMAIL_TLD_REGEX.matches(labels.last())
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

    fun calculatePasswordStrength(password: String): PasswordStrength {
        if (password.isEmpty()) return PasswordStrength.NONE
        var score = 0
        if (password.length >= 8) score++
        if (password.length >= 10) score++
        if (password.any { it.isDigit() } && password.any { it.isLetter() }) score++
        if (password.any { !it.isLetterOrDigit() } || (password.any { it.isUpperCase() } && password.any { it.isLowerCase() })) score++
        return when {
            score <= 1 -> PasswordStrength.WEAK
            score in 2..3 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.STRONG
        }
    }

    fun validateBirthYear(year: Int): String? {
        val currentYear = java.time.LocalDate.now().year
        return when {
            year < 1920 || year > currentYear -> "Năm sinh không hợp lệ"
            currentYear - year < 13 -> "Bạn phải đủ 13 tuổi trở lên để sử dụng Lokap"
            else -> null
        }
    }

    fun validateBio(bio: String): String? {
        return if (bio.length > 60) "Giới thiệu tối đa 60 ký tự" else null
    }
}
