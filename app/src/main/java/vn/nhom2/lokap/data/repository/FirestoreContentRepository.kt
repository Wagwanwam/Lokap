package vn.nhom2.lokap.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import vn.nhom2.lokap.data.remote.*
import vn.nhom2.lokap.domain.model.*
import vn.nhom2.lokap.domain.repository.*
import vn.nhom2.lokap.feature.chat.ChatThemeConfig

/** All content comes from Firestore listeners. No seeded users, messages or simulated replies. */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class FirestoreContentRepository @Inject constructor(
    private val dbLazy: dagger.Lazy<FirebaseFirestore>, private val auth: AuthRepository
) : MessagingRepository, CommentRepository, NotificationRepository {
    private val db get() = dbLazy.get()
    private fun me() = auth.currentUser.value?.id ?: error("Bạn cần đăng nhập lại")
    private fun sessions() = auth.currentUser.map { it?.id }.distinctUntilChanged()
    private fun conversations() = db.collection("conversations")
    private fun messages(id: String) = conversations().document(id).collection("messages")
    private fun comments(id: String) = db.collection("posts").document(id).collection("comments")
    private fun notices(uid: String) = db.collection("users").document(uid).collection("notifications")
    private suspend fun user(id: String) = db.collection("users").document(id).get().await().toUser()
        ?: User(id = id, username = "", displayName = "Tài khoản đã xoá", avatarUrl = null, bio = "")

    override fun observeInbox(): Flow<Result<List<Conversation>>> = sessions().flatMapLatest { uid ->
        if (uid == null) flowOf(Result.success(emptyList())) else conversations().whereArrayContains("members", uid).watch()
            // Chỉ dựng lại chatFlow khi tập cuộc trò chuyện đổi; readAt, tin mới đã có listener riêng trong từng chatFlow
            .map { docs -> docs.map { it.id }.toSet() }.distinctUntilChanged()
            .flatMapLatest { ids ->
                if (ids.isEmpty()) flowOf(emptyList()) else combine(ids.map { conversationId ->
                    chatFlow(conversationId, uid).map { it.conversation }
                }) { chats -> chats.filterNotNull().sortedByDescending { it.lastMessageAt } }
            }.flowOn(Dispatchers.Default).asResults()
    }
    override fun observeChat(id: String): Flow<Result<ChatData>> = sessions().flatMapLatest { uid ->
        if (uid == null) flowOf(Result.success(ChatData(null, emptyList()))) else chatFlow(id, uid).flowOn(Dispatchers.Default).asResults()
    }
    private fun chatFlow(id: String, uid: String): Flow<ChatData> = combine(
        // Tin nhắn đọc hasPendingWrites (Đang gửi / Đã gửi) nên cần cả thay đổi chỉ có metadata
        conversations().document(id).watch(), messages(id).orderBy("createdAt").watch(MetadataChanges.INCLUDE)
    ) { conv, docs ->
        if (!conv.exists()) return@combine ChatData(null, emptyList())
        val members = (conv.get("members") as? List<*>)?.filterIsInstance<String>().orEmpty()
        val otherId = members.firstOrNull { it != uid } ?: return@combine ChatData(null, emptyList())
        val readAt = conv.get("readAt") as? Map<*, *> ?: emptyMap<Any, Any>()
        val myRead = (readAt[uid] as? Timestamp)?.toDate()?.toInstant() ?: Instant.EPOCH
        val theirRead = (readAt[otherId] as? Timestamp)?.toDate()?.toInstant() ?: Instant.EPOCH
        val list = docs.map { d ->
            val created = d.getTimestamp("createdAt")?.toDate()?.toInstant() ?: Instant.now()
            Message(d.id, id, d.getString("senderId").orEmpty(), d.getString("body"), null, null,
                created, if (d.metadata.hasPendingWrites()) PostSendState.PENDING else PostSendState.SENT,
                !created.isAfter(theirRead))
        }
        ChatData(Conversation(id, user(otherId), list.lastOrNull()?.body.orEmpty(),
            list.lastOrNull()?.createdAt ?: conv.getTimestamp("createdAt")?.toDate()?.toInstant() ?: Instant.EPOCH,
            list.count { it.senderId != uid && it.createdAt.isAfter(myRead) }), list)
    }
    override suspend fun openConversation(userId: String): Result<String> = call {
        val uid = me()
        require(userId != uid) { "Hãy chọn một người bạn để nhắn tin" }
        val id = Fs.pairId(uid, userId)
        val ref = conversations().document(id)
        db.runTransaction { tx ->
            if (!tx.get(ref).exists()) tx.set(ref, mapOf("members" to listOf(uid, userId).sorted(),
                "createdAt" to FieldValue.serverTimestamp(), "readAt" to emptyMap<String, Any>()))
        }.await()
        id
    }
    override suspend fun send(id: String, body: String): Result<Unit> = call {
        val uid = me(); val text = validText(body)
        val conv = conversations().document(id).get(Source.SERVER).await()
        val other = (conv.get("members") as List<*>).filterIsInstance<String>().single { it != uid }
        val ref = messages(id).document()
        db.batch().apply {
            set(ref, mapOf("senderId" to uid, "body" to text, "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()))
            set(notices(other).document("message_" + ref.id), notificationData("NEW_MESSAGE", uid, conversationId = id, sourceId = ref.id))
        }.commit().await()
    }
    override suspend fun edit(id: String, messageId: String, body: String): Result<Unit> = call {
        messages(id).document(messageId).update(mapOf("body" to validText(body), "updatedAt" to FieldValue.serverTimestamp())).await(); Unit
    }
    override suspend fun delete(id: String, messageId: String): Result<Unit> = call {
        messages(id).document(messageId).delete().await(); Unit
    }
    override suspend fun markRead(id: String): Result<Unit> = call {
        conversations().document(id).update(FieldPath.of("readAt", me()), FieldValue.serverTimestamp()).await(); Unit
    }
    private fun preference(uid: String, id: String) = db.collection("users").document(uid).collection("chatPreferences").document(id)
    override fun observeTheme(id: String): Flow<ChatThemeConfig> = sessions().flatMapLatest { uid ->
        if (uid == null) flowOf(ChatThemeConfig()) else preference(uid, id).watch().map { d ->
            ChatThemeConfig(d.getString("presetId") ?: "LOKAP_PINK", d.getString("customWallpaperUri"), d.getString("wallpaperAlignment") ?: "CENTER")
        }.catch { emit(ChatThemeConfig()) }
    }
    override suspend fun saveTheme(id: String, theme: ChatThemeConfig): Result<Unit> = call {
        preference(me(), id).set(mapOf("presetId" to theme.presetId, "customWallpaperUri" to theme.customWallpaperUri, "wallpaperAlignment" to theme.wallpaperAlignment)).await(); Unit
    }
    override fun observeComments(postId: String): Flow<Result<List<PostComment>>> = sessions().flatMapLatest { uid ->
        if (uid == null) flowOf(Result.success(emptyList())) else comments(postId).orderBy("createdAt").watch().map { docs ->
            val authors = docs.mapNotNull { it.getString("authorId") }.distinct().associateWith { user(it) }
            docs.map { d ->
                val votes = d.get("votes") as? Map<*, *> ?: emptyMap<Any, Any>()
                PostComment(d.id, authors.getValue(d.getString("authorId")!!), d.getString("content").orEmpty(), d.getString("parentId"),
                    d.getTimestamp("createdAt")?.toDate()?.toInstant() ?: Instant.now(),
                    votes.values.sumOf { (it as? Number)?.toInt() ?: 0 }, (votes[uid] as? Number)?.toInt() ?: 0)
            }
        }.flowOn(Dispatchers.Default).asResults()
    }
    // Cùng truy vấn với observeComments (trả lời nằm chung collection) nhưng chỉ đếm: không dựng PostComment, không tải tác giả
    override fun observeCommentCount(postId: String): Flow<Int> = sessions().flatMapLatest { uid ->
        if (uid == null) flowOf(0) else comments(postId).orderBy("createdAt").watchCount().catch { emit(0) }
    }.distinctUntilChanged()
    override suspend fun add(postId: String, content: String, parentId: String?): Result<Unit> = call {
        val uid = me(); val text = validText(content)
        val post = db.collection("posts").document(postId).get(Source.SERVER).await()
        val owner = post.getString("authorId") ?: error("Bài viết không còn tồn tại")
        val ref = comments(postId).document()
        db.batch().apply {
            set(ref, mapOf("authorId" to uid, "content" to text, "parentId" to parentId,
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp(), "votes" to emptyMap<String, Int>()))
            if (owner != uid) set(notices(owner).document("comment_" + ref.id), notificationData("COMMENT", uid, postId = postId, sourceId = ref.id))
        }.commit().await()
    }
    override suspend fun editComment(postId: String, id: String, content: String): Result<Unit> = call {
        comments(postId).document(id).update(mapOf("content" to validText(content), "updatedAt" to FieldValue.serverTimestamp())).await(); Unit
    }
    override suspend fun deleteComment(postId: String, id: String): Result<Unit> = call { comments(postId).document(id).delete().await(); Unit }
    override suspend fun vote(postId: String, id: String, up: Boolean): Result<Unit> = call {
        val uid = me(); val ref = comments(postId).document(id)
        db.runTransaction { tx ->
            val current = ((tx.get(ref).get("votes") as? Map<*, *>)?.get(uid) as? Number)?.toInt() ?: 0
            val requested = if (up) 1 else -1
            tx.update(ref, FieldPath.of("votes", uid), if (current == requested) FieldValue.delete() else requested)
        }.await(); Unit
    }
    override fun observeNotifications(): Flow<Result<List<AppNotification>>> = sessions().flatMapLatest { uid ->
        if (uid == null) flowOf(Result.success(emptyList())) else notices(uid).orderBy("createdAt", Query.Direction.DESCENDING).watch().map { docs ->
            val actors = docs.mapNotNull { it.getString("actorId") }.distinct().associateWith { user(it) }
            docs.mapNotNull { d ->
                val type = runCatching { NotificationType.valueOf(d.getString("type").orEmpty()) }.getOrNull() ?: return@mapNotNull null
                val actor = actors[d.getString("actorId")]
                val text = when (type) {
                    NotificationType.NEW_MESSAGE -> "đã gửi bạn một tin nhắn"
                    NotificationType.COMMENT -> "đã bình luận về bài viết của bạn"
                    NotificationType.FRIEND_REQUEST -> "đã gửi lời mời kết bạn"
                    NotificationType.FRIEND_ACCEPTED -> "đã chấp nhận lời mời kết bạn"
                    NotificationType.REACTION -> "đã bày tỏ cảm xúc về bài viết của bạn"
                    NotificationType.NEW_POST -> "đã chia sẻ một khoảnh khắc mới"
                    NotificationType.POST_HIDDEN -> "Bài viết đã bị ẩn"
                }
                AppNotification(d.id, type, actor, d.getString("postId"), listOfNotNull(actor?.displayName, text).joinToString(" "),
                    d.getTimestamp("createdAt")?.toDate()?.toInstant() ?: Instant.now(), d.getBoolean("isRead") ?: false, d.getString("conversationId"))
            }
        }.flowOn(Dispatchers.Default).asResults()
    }
    // Huy hiệu chuông chỉ cần số chưa đọc: tối đa 100 (BadgeDot hiện "99+"), không dựng AppNotification
    override fun observeUnreadCount(): Flow<Int> = sessions().flatMapLatest { uid ->
        if (uid == null) flowOf(0) else notices(uid).whereEqualTo("isRead", false).limit(100).watchCount().catch { emit(0) }
    }.distinctUntilChanged()
    override suspend fun markNotificationRead(id: String, read: Boolean): Result<Unit> = call { notices(me()).document(id).update("isRead", read).await(); Unit }
    override suspend fun markAllRead(): Result<Unit> = call {
        notices(me()).whereEqualTo("isRead", false).get(Source.SERVER).await().documents.chunked(400).forEach { docs ->
            db.batch().apply { docs.forEach { update(it.reference, "isRead", true) } }.commit().await()
        }
    }
    override suspend fun deleteNotification(id: String): Result<Unit> = call { notices(me()).document(id).delete().await(); Unit }
    private fun validText(value: String): String = value.trim().also { require(it.isNotEmpty() && it.length <= 1000) { "Nội dung phải có từ 1 đến 1000 ký tự" } }
    private suspend fun <T> call(block: suspend () -> T): Result<T> = try { Result.success(block()) }
        catch (e: CancellationException) { throw e } catch (e: Exception) { Result.failure(e.toAppException()) }
}

internal fun notificationData(type: String, actorId: String, postId: String? = null, conversationId: String? = null, sourceId: String): Map<String, Any?> =
    mapOf("type" to type, "actorId" to actorId, "postId" to postId, "conversationId" to conversationId,
        "sourceId" to sourceId, "createdAt" to FieldValue.serverTimestamp(), "isRead" to false)
// Mặc định bỏ sự kiện chỉ đổi metadata; ai đọc metadata (hasPendingWrites, isFromCache) thì truyền INCLUDE.
// conflate: người nhận chỉ cần snapshot mới nhất, trySend không bao giờ rơi khi người nhận chậm
internal fun Query.watch(meta: MetadataChanges = MetadataChanges.EXCLUDE): Flow<List<DocumentSnapshot>> = callbackFlow {
    val listener = addSnapshotListener(meta) { snap, error ->
        if (error != null) close(error) else if (snap != null) trySend(snap.documents)
    }
    awaitClose { listener.remove() }
}.conflate()
internal fun DocumentReference.watch(meta: MetadataChanges = MetadataChanges.EXCLUDE): Flow<DocumentSnapshot> = callbackFlow {
    val listener = addSnapshotListener(meta) { snap, error ->
        if (error != null) close(error) else if (snap != null) trySend(snap)
    }
    awaitClose { listener.remove() }
}.conflate()
/** Chỉ đếm số document khớp truy vấn: không dựng object, không đọc thêm. */
private fun Query.watchCount(): Flow<Int> = callbackFlow {
    val listener = addSnapshotListener(MetadataChanges.EXCLUDE) { snap, error ->
        if (error != null) close(error) else if (snap != null) trySend(snap.size())
    }
    awaitClose { listener.remove() }
}.conflate()
private fun <T> Flow<T>.asResults(): Flow<Result<T>> = map { Result.success(it) }.catch { emit(Result.failure(it.toAppException())) }
