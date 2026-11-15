package vn.nhom2.lokap.domain.repository

import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import vn.nhom2.lokap.domain.model.*
import vn.nhom2.lokap.feature.chat.ChatThemeConfig

data class PostComment(val id: String, val author: User, val content: String,
    val parentId: String?, val createdAt: Instant, val score: Int, val myVote: Int)
data class ChatData(val conversation: Conversation?, val messages: List<Message>)

interface MessagingRepository {
    fun observeInbox(): Flow<Result<List<Conversation>>>
    fun observeChat(id: String): Flow<Result<ChatData>>
    suspend fun openConversation(userId: String): Result<String>
    suspend fun send(id: String, body: String): Result<Unit>
    suspend fun edit(id: String, messageId: String, body: String): Result<Unit>
    suspend fun delete(id: String, messageId: String): Result<Unit>
    suspend fun markRead(id: String): Result<Unit>
    fun observeTheme(id: String): Flow<ChatThemeConfig>
    suspend fun saveTheme(id: String, theme: ChatThemeConfig): Result<Unit>
}
interface CommentRepository {
    fun observeComments(postId: String): Flow<Result<List<PostComment>>>
    /** Số bình luận (gồm trả lời) cho Feed; lỗi thì 0. Bản Firestore đếm thẳng, không dựng PostComment. */
    fun observeCommentCount(postId: String): Flow<Int> = observeComments(postId).map { it.getOrDefault(emptyList()).size }
    suspend fun add(postId: String, content: String, parentId: String? = null): Result<Unit>
    suspend fun editComment(postId: String, id: String, content: String): Result<Unit>
    suspend fun deleteComment(postId: String, id: String): Result<Unit>
    suspend fun vote(postId: String, id: String, up: Boolean): Result<Unit>
}
interface NotificationRepository {
    fun observeNotifications(): Flow<Result<List<AppNotification>>>
    /** Số thông báo chưa đọc cho huy hiệu chuông; lỗi thì 0. */
    fun observeUnreadCount(): Flow<Int> = observeNotifications().map { r -> r.getOrDefault(emptyList()).count { !it.isRead } }
    suspend fun markNotificationRead(id: String, read: Boolean = true): Result<Unit>
    suspend fun markAllRead(): Result<Unit>
    suspend fun deleteNotification(id: String): Result<Unit>
}
