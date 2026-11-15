package vn.nhom2.lokap.domain.model

/**
 * Mã kết bạn: QR và link mời cùng chứa `lokap://add/<username>`.
 * Username theo luật `usernames/{name}` trong Firestore Rules: 3–20 ký tự a-z, 0-9, dấu chấm, gạch dưới.
 */
object FriendCode {
    const val SCHEME = "lokap"
    const val HOST = "add"
    private const val PREFIX = "$SCHEME://$HOST/"
    private val USERNAME = Regex("^[a-z0-9._]{3,20}$")

    fun linkFor(username: String): String = PREFIX + username.trim().removePrefix("@").lowercase()

    /** Lời mời gửi qua nút Chia sẻ của Android. */
    fun inviteText(username: String): String =
        "Kết bạn với mình trên Lokap nhé: @${username.trim().removePrefix("@")}\n${linkFor(username)}"

    /**
     * Đọc username từ nội dung QR hoặc link. Nhận `lokap://add/<username>` và `@username`;
     * mã khác (link web, chữ thường, QR của app khác) trả về null.
     */
    fun parse(raw: String): String? {
        val text = raw.trim()
        val candidate = when {
            text.startsWith(PREFIX, ignoreCase = true) -> text.substring(PREFIX.length)
                .substringBefore('?')
                .substringBefore('#')
                .trimEnd('/')
            text.startsWith("@") -> text.drop(1)
            else -> return null
        }.lowercase()
        return candidate.takeIf { USERNAME.matches(it) }
    }
}
