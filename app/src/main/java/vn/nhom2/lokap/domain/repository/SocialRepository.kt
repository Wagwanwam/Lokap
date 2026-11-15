package vn.nhom2.lokap.domain.repository

import kotlinx.coroutines.flow.StateFlow
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.model.User

/** Quan hệ của người đang đăng nhập với những người khác. */
data class SocialGraph(
    val statuses: Map<String, FriendStatus> = emptyMap(),
    val users: Map<String, User> = emptyMap()
) {
    fun statusOf(userId: String): FriendStatus = statuses[userId] ?: FriendStatus.NONE

    fun usersWith(status: FriendStatus): List<User> =
        statuses.filterValues { it == status }.keys.mapNotNull { users[it] }

    val friendCount: Int
        get() = statuses.values.count { it == FriendStatus.FRIENDS }
}

interface SocialRepository {
    val maxFriends: Int

    val graph: StateFlow<SocialGraph>

    suspend fun refresh(): Result<Unit>

    /** Tìm theo username hoặc tên; không trả về chính mình và người có quan hệ chặn. */
    suspend fun search(query: String): Result<List<User>>

    /** Gợi ý khi chưa gõ gì ở màn Tìm kiếm. */
    suspend fun suggestions(): Result<List<User>>

    suspend fun getUser(userId: String): Result<User?>

    /**
     * Tìm đúng 1 người theo username (mã QR, link mời). Trả về chính mình nếu là username của mình;
     * null khi không có ai hoặc mình đã chặn người đó. Bản mặc định (dựa trên [search]) dành cho test giả.
     */
    suspend fun findByUsername(username: String): Result<User?> {
        val name = username.trim().removePrefix("@").lowercase()
        return search(name).map { users -> users.firstOrNull { it.username.equals(name, ignoreCase = true) } }
    }

    suspend fun getFriendCount(userId: String): Result<Int>

    /** Trả về trạng thái mới: REQUEST_SENT, hoặc FRIENDS nếu người kia đã mời mình trước. */
    suspend fun sendFriendRequest(userId: String): Result<FriendStatus>

    suspend fun cancelFriendRequest(userId: String): Result<Unit>

    suspend fun acceptFriendRequest(userId: String): Result<Unit>

    suspend fun declineFriendRequest(userId: String): Result<Unit>

    suspend fun unfriend(userId: String): Result<Unit>

    suspend fun block(userId: String): Result<Unit>

    suspend fun unblock(userId: String): Result<Unit>
}
