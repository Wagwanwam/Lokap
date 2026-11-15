package vn.nhom2.lokap.feature
import java.time.Instant
import kotlinx.coroutines.flow.*
import vn.nhom2.lokap.domain.repository.*
import vn.nhom2.lokap.domain.model.*
import vn.nhom2.lokap.feature.chat.ChatThemeConfig
/** Unit-test only; never bound into the Android app. */
class ContentTestRepository : MessagingRepository, CommentRepository, NotificationRepository {
    val chat = MutableStateFlow(Result.success(ChatData(null, emptyList())))
    val notices = MutableStateFlow(Result.success(emptyList<AppNotification>()))
    val theme = MutableStateFlow(ChatThemeConfig())
    var failure: Throwable? = null
    var sendCount = 0
    var readCount = 0
    private fun result() = failure?.let { Result.failure<Unit>(it) } ?: Result.success(Unit)
    override fun observeInbox() = chat.map { it.map { data -> listOfNotNull(data.conversation) } }
    override fun observeChat(id: String) = chat
    override suspend fun openConversation(userId: String) = Result.success("chat_$userId")
    override suspend fun send(id: String, body: String): Result<Unit> { sendCount++; return result() }
    override suspend fun edit(id: String, messageId: String, body: String) = result()
    override suspend fun delete(id: String, messageId: String) = result()
    override suspend fun markRead(id: String): Result<Unit> { readCount++; return result() }
    override fun observeTheme(id: String) = theme
    override suspend fun saveTheme(id: String, theme: ChatThemeConfig): Result<Unit> { this.theme.value=theme; return result() }
    override fun observeComments(postId: String) = flowOf(Result.success(emptyList<PostComment>()))
    override suspend fun add(postId: String, content: String, parentId: String?) = result()
    override suspend fun editComment(postId: String, id: String, content: String) = result()
    override suspend fun deleteComment(postId: String, id: String) = result()
    override suspend fun vote(postId: String, id: String, up: Boolean) = result()
    override fun observeNotifications() = notices
    override suspend fun markNotificationRead(id: String, read: Boolean): Result<Unit> {
        if (failure == null) notices.value = notices.value.map { list -> list.map { if(it.id==id) it.copy(isRead=read) else it } }; return result()
    }
    override suspend fun markAllRead() = result()
    override suspend fun deleteNotification(id: String) = result()
}
