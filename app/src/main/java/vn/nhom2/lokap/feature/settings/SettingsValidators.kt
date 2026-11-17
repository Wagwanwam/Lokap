package vn.nhom2.lokap.feature.settings

import vn.nhom2.lokap.feature.auth.AuthValidators

/** Cùng luật cho trạng thái nút và kiểm tra trước khi xử lý action. */
object SettingsValidators {
    private val domesticPhone = Regex("^0[0-9]{9,10}$")
    private val internationalPhone = Regex("^\\+[1-9][0-9]{7,14}$")
    private val parenthesizedDigits = Regex("\\([0-9]{1,4}\\)")
    private val phoneSeparators = setOf(' ', '-', '.', '(', ')')

    /** Chỉ bỏ dấu phân cách hiển thị; không xoá chữ để biến đầu vào sai thành số hợp lệ. */
    fun normalizeRecoveryPhone(input: String): String = input.trim().filterNot { it in phoneSeparators }

    fun validateRecoveryPhone(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "Vui lòng nhập số điện thoại"
        if (trimmed.any { it !in '0'..'9' && it != '+' && it !in phoneSeparators }) {
            return "Số điện thoại chỉ được chứa chữ số và mã quốc gia"
        }
        if ('(' in trimmed || ')' in trimmed) {
            if (trimmed.count { it == '(' } != 1 || trimmed.count { it == ')' } != 1 ||
                !parenthesizedDigits.containsMatchIn(trimmed)
            ) return "Định dạng số điện thoại không hợp lệ"
        }
        val normalized = normalizeRecoveryPhone(trimmed)
        return if (domesticPhone.matches(normalized) || internationalPhone.matches(normalized)) null
        else "Số điện thoại không hợp lệ (ví dụ: 0912 345 678 hoặc +84 912 345 678)"
    }

    fun validateLinkedEmail(input: String): String? = AuthValidators.validateEmail(input)
}
