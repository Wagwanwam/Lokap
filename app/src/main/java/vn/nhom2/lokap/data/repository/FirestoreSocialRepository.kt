package vn.nhom2.lokap.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import vn.nhom2.lokap.data.remote.Fs
import vn.nhom2.lokap.data.remote.toAppException
import vn.nhom2.lokap.data.remote.toUser
import vn.nhom2.lokap.di.ApplicationScope
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.model.User
import vn.nhom2.lokap.domain.repository.AppException
import vn.nhom2.lokap.domain.repository.AuthRepository
import vn.nhom2.lokap.domain.repository.AuthSession
import vn.nhom2.lokap.domain.repository.PostRepository
import vn.nhom2.lokap.domain.repository.SocialGraph
import vn.nhom2.lokap.domain.repository.SocialRepository

/**
 * Bạn bè / chặn trên Firestore. Mỗi cặp bạn là 1 document `friendships/{uidNhỏ_uidLớn}` (pending → accepted),
 * chặn là `blocks/{người chặn_người bị chặn}`. Rules kiểm tra: chỉ người trong cặp đọc/xoá được, chỉ người
 * được mời mới chấp nhận được, không mời được khi một bên đã chặn bên kia.
 * Giới hạn 20 bạn kiểm ở app (Rules không đếm được số document).
 */
@Singleton
class FirestoreSocialRepository @Inject constructor(
    private val dbLazy: dagger.Lazy<FirebaseFirestore>,
    private val auth: AuthRepository,
    private val graphSource: FirestoreGraph,
    // Lazy: chỉ cần khi huỷ kết bạn / chặn, tránh vòng phụ thuộc khi khởi tạo
    private val postsLazy: dagger.Lazy<PostRepository>,
    @ApplicationScope private val appScope: CoroutineScope
) : SocialRepository {

    private val db: FirebaseFirestore get() = dbLazy.get()

    override val maxFriends: Int = 20

    private val mutationGate = SocialMutationGate()

    private val _graph = MutableStateFlow(SocialGraph())
    override val graph: StateFlow<SocialGraph> = _graph.asStateFlow()

    /** Người đã gặp qua tìm kiếm / hồ sơ, để hiện tên ngay khi trạng thái đổi. */
    private val knownUsers = MutableStateFlow<Map<String, User>>(emptyMap())

    init {
        appScope.launch {
            auth.session
                .map { (it as? AuthSession.SignedIn)?.user?.id }
                .distinctUntilChanged()
                .collect { uid ->
                    if (uid == null) {
                        _graph.value = SocialGraph()
                        knownUsers.value = emptyMap()
                    } else {
                        refresh()
                    }
                }
        }
    }

    override suspend fun refresh(): Result<Unit> = appCall {
        val me = requireMe()
        val statuses = mutableMapOf<String, FriendStatus>()
        val pendingCreatedAt = mutableMapOf<String, Instant>()
        graphSource.friendshipsOf(me).forEach { doc ->
            val other = graphSource.otherUser(doc, me) ?: return@forEach
            statuses[other] = when {
                doc.getString(Fs.STATUS) == Fs.ACCEPTED -> FriendStatus.FRIENDS
                doc.getString(Fs.REQUESTER) == me -> FriendStatus.REQUEST_SENT
                else -> FriendStatus.REQUEST_RECEIVED
            }
            if (doc.getString(Fs.STATUS) == Fs.PENDING && statuses[other] == FriendStatus.REQUEST_RECEIVED) {
                doc.getTimestamp(Fs.CREATED_AT)?.toDate()?.toInstant()?.let { pendingCreatedAt[other] = it }
            }
        }
        graphSource.blockedIds(me).forEach { statuses[it] = FriendStatus.BLOCKED }

        val users = fetchUsers(statuses.keys.toList())
        if (auth.currentUser.value?.id != me) return@appCall
        remember(users)
        _graph.value = SocialGraph(statuses = statuses, users = knownUsers.value,
            pendingRequestCreatedAt = pendingCreatedAt.filterKeys { statuses[it] == FriendStatus.REQUEST_RECEIVED })

        // Người khác đọc số bạn ở hồ sơ công khai của mình (Rules không cho đếm friendships của người khác)
        val friendCount = statuses.values.count { it == FriendStatus.FRIENDS }
        runCatching { db.collection(Fs.USERS).document(me).update(Fs.FRIEND_COUNT, friendCount).await() }
    }

    override suspend fun search(query: String): Result<List<User>> = appCall {
        val me = requireMe()
        val q = query.trim().removePrefix("@").lowercase()
        if (q.isBlank()) return@appCall emptyList()
        val users = db.collection(Fs.USERS)
        // Firestore không có tìm kiếm toàn văn: tìm theo tiền tố username và tên đã bỏ dấu
        val byUsername = users.orderBy(Fs.USERNAME).startAt(q).endAt(q + "").limit(SEARCH_LIMIT).get().await()
        val key = Fs.searchKey(q)
        val byName = users.orderBy(Fs.SEARCH_NAME).startAt(key).endAt(key + "").limit(SEARCH_LIMIT).get().await()
        val blocked = _graph.value.usersWith(FriendStatus.BLOCKED).map { it.id }.toSet()
        val found = (byUsername.documents + byName.documents)
            .distinctBy { it.id }
            .filter { it.id != me && it.id !in blocked }
            .mapNotNull { it.toUser() }
            .filter { it.username.isNotBlank() }
            .take(SEARCH_LIMIT.toInt())
        remember(found)
        found
    }

    override suspend fun suggestions(): Result<List<User>> = Result.success(
        // Chưa có thuật toán gợi ý: hiện bạn bè và người đang mời mình
        _graph.value.usersWith(FriendStatus.REQUEST_RECEIVED) + _graph.value.usersWith(FriendStatus.FRIENDS)
    )

    override suspend fun getUser(userId: String): Result<User?> = appCall {
        auth.currentUser.value?.takeIf { it.id == userId }
            ?: db.collection(Fs.USERS).document(userId).get().await().toUser()?.also { remember(listOf(it)) }
    }

    override suspend fun findByUsername(username: String): Result<User?> = appCall {
        val me = requireMe()
        val name = username.trim().removePrefix("@").lowercase()
        if (name.isBlank()) return@appCall null
        // usernames/{name} → uid: đọc 1 document, không cần index như tìm theo tiền tố
        val uid = db.collection(Fs.USERNAMES).document(name).get().await().getString(Fs.UID)
            ?: return@appCall null
        if (uid == me) return@appCall auth.currentUser.value
        if (_graph.value.statusOf(uid) == FriendStatus.BLOCKED) return@appCall null
        db.collection(Fs.USERS).document(uid).get().await().toUser()?.also { remember(listOf(it)) }
    }

    override suspend fun getFriendCount(userId: String): Result<Int> = appCall {
        if (userId == auth.currentUser.value?.id) return@appCall _graph.value.friendCount
        db.collection(Fs.USERS).document(userId).get().await().getLong(Fs.FRIEND_COUNT)?.toInt() ?: 0
    }

    override suspend fun sendFriendRequest(userId: String): Result<FriendStatus> = mutate(userId) { me ->
        val ref = db.collection(Fs.FRIENDSHIPS).document(Fs.pairId(me, userId))
        // Transaction có thể chạy lại callback. Cùng một thao tác luôn dùng cùng document thông báo.
        // Thu hồi rồi gửi lại là thao tác mới, vẫn tạo thông báo mới như trước.
        val notice = db.collection(Fs.USERS).document(userId).collection("notifications").document()
        val status = db.runTransaction { tx ->
            val current = tx.get(ref).friendStatus(me)
            when (current) {
                FriendStatus.NONE -> {
                    ensureFriendCapacity()
                    val otherCount = tx.get(db.collection(Fs.USERS).document(userId)).getLong(Fs.FRIEND_COUNT) ?: 0L
                    if (otherCount >= maxFriends) {
                        throw AppException("Người này đã đủ $maxFriends bạn", AppException.Reason.FRIEND_LIMIT_OTHER)
                    }
                    tx.set(
                        ref,
                        mapOf(
                            Fs.PAIR_USERS to listOf(me, userId).sorted(),
                            Fs.REQUESTER to me,
                            Fs.ADDRESSEE to userId,
                            Fs.STATUS to Fs.PENDING,
                            Fs.CREATED_AT to FieldValue.serverTimestamp()
                        )
                    )
                    tx.set(notice, notificationData("FRIEND_REQUEST", me, sourceId = ref.id))
                    FriendStatus.REQUEST_SENT
                }
                // Người kia đã mời mình trước: mời lại = chấp nhận luôn.
                FriendStatus.REQUEST_RECEIVED -> {
                    ensureFriendCapacity()
                    val otherCount = tx.get(db.collection(Fs.USERS).document(userId)).getLong(Fs.FRIEND_COUNT) ?: 0L
                    if (otherCount >= maxFriends) {
                        throw AppException("Người này đã đủ $maxFriends bạn", AppException.Reason.FRIEND_LIMIT_OTHER)
                    }
                    tx.update(ref, mapOf(Fs.STATUS to Fs.ACCEPTED, Fs.ACCEPTED_AT to FieldValue.serverTimestamp()))
                    tx.set(notice, notificationData("FRIEND_ACCEPTED", me, sourceId = ref.id))
                    FriendStatus.FRIENDS
                }
                else -> current // Retry / màn khác đã gửi: không ghi thêm lời mời hoặc thông báo.
            }
        }.await()
        setStatus(userId, status)
        status
    }

    override suspend fun cancelFriendRequest(userId: String): Result<Unit> =
        deleteFriendship(userId, expected = FriendStatus.REQUEST_SENT)

    override suspend fun acceptFriendRequest(userId: String): Result<Unit> = mutate(userId) { me ->
        val ref = db.collection(Fs.FRIENDSHIPS).document(Fs.pairId(me, userId))
        val notice = db.collection(Fs.USERS).document(userId).collection("notifications").document()
        // Đọc lại lời mời: thông báo cũ có thể đã thu hồi hoặc đã chấp nhận trên máy khác.
        val status = db.runTransaction { tx ->
            val current = tx.get(ref).friendStatus(me)
            if (current == FriendStatus.REQUEST_RECEIVED) {
                ensureFriendCapacity()
                tx.update(ref, mapOf(Fs.STATUS to Fs.ACCEPTED, Fs.ACCEPTED_AT to FieldValue.serverTimestamp()))
                tx.set(notice, notificationData("FRIEND_ACCEPTED", me, sourceId = ref.id))
                FriendStatus.FRIENDS
            } else {
                current
            }
        }.await()
        setStatus(userId, status)
        when (status) {
            // Bài cũ sẽ thêm bạn mới vào viewerIds ở lần làm mới feed kế tiếp (FirestoreGraph).
            FriendStatus.FRIENDS -> Unit
            FriendStatus.REQUEST_SENT -> throw AppException("Bạn đang chờ người này chấp nhận lời mời")
            else -> throw AppException("Lời mời này đã bị thu hồi", AppException.Reason.NOT_FOUND)
        }
    }

    override suspend fun declineFriendRequest(userId: String): Result<Unit> =
        deleteFriendship(userId, expected = FriendStatus.REQUEST_RECEIVED)

    override suspend fun unfriend(userId: String): Result<Unit> =
        deleteFriendship(userId, expected = FriendStatus.FRIENDS)

    override suspend fun block(userId: String): Result<Unit> = mutate(userId) { me ->
        db.batch()
            .set(
                db.collection(Fs.BLOCKS).document(Fs.blockId(me, userId)),
                mapOf(Fs.BLOCKER to me, Fs.BLOCKED to userId, Fs.CREATED_AT to FieldValue.serverTimestamp())
            )
            .delete(db.collection(Fs.FRIENDSHIPS).document(Fs.pairId(me, userId)))
            .commit()
            .await()
        setStatus(userId, FriendStatus.BLOCKED)
        runCatching { graphSource.detachViewers(me, userId) }
        dropPostsOf(userId)
    }

    override suspend fun unblock(userId: String): Result<Unit> = mutate(userId) { me ->
        db.collection(Fs.BLOCKS).document(Fs.blockId(me, userId)).delete().await()
        setStatus(userId, FriendStatus.NONE)
    }

    private suspend fun deleteFriendship(userId: String, expected: FriendStatus): Result<Unit> = mutate(userId) { me ->
        val ref = db.collection(Fs.FRIENDSHIPS).document(Fs.pairId(me, userId))
        val status = db.runTransaction { tx ->
            val current = tx.get(ref).friendStatus(me)
            if (current == expected) {
                tx.delete(ref)
                FriendStatus.NONE
            } else {
                current
            }
        }.await()
        setStatus(userId, status)
        if (status != FriendStatus.NONE) {
            // Ví dụ: chấp nhận vừa thắng cuộc đua với từ chối. Không được xoá quan hệ accepted.
            throw AppException("Quan hệ bạn bè đã thay đổi. Vui lòng thử lại")
        }
        // Hết là bạn: hai bên không còn thấy bài bạn bè của nhau.
        if (expected == FriendStatus.FRIENDS) {
            runCatching { graphSource.detachViewers(me, userId) }
            dropPostsOf(userId)
        }
    }

    private fun DocumentSnapshot.friendStatus(me: String): FriendStatus = when {
        !exists() -> FriendStatus.NONE
        getString(Fs.STATUS) == Fs.ACCEPTED -> FriendStatus.FRIENDS
        getString(Fs.STATUS) == Fs.PENDING && getString(Fs.REQUESTER) == me -> FriendStatus.REQUEST_SENT
        getString(Fs.STATUS) == Fs.PENDING && getString(Fs.ADDRESSEE) == me -> FriendStatus.REQUEST_RECEIVED
        else -> throw AppException("Quan hệ bạn bè không hợp lệ. Vui lòng tải lại")
    }

    private fun ensureFriendCapacity() {
        if (_graph.value.friendCount >= maxFriends) {
            throw AppException("Bạn đã đạt giới hạn $maxFriends người bạn", AppException.Reason.FRIEND_LIMIT)
        }
    }

    private suspend fun <T> mutate(userId: String, block: suspend (String) -> T): Result<T> = appCall {
        val me = requireMe()
        if (userId.isBlank() || userId == me) throw AppException("Không thể kết bạn với chính mình")
        mutationGate.run(Fs.pairId(me, userId)) { block(me) }
    }

    /**
     * Huỷ kết bạn / chặn: bỏ ngay bài của người đó khỏi máy (feed Bạn bè, bản đồ, hồ sơ đã lưu),
     * rồi tải lại feed từ Firestore ở nền: bài public đã tới giờ hiện của họ sẽ về lại tab Khám phá.
     */
    private suspend fun dropPostsOf(userId: String) {
        val posts = postsLazy.get()
        runCatching { posts.hidePostsByAuthor(userId) }
        appScope.launch { posts.refreshFeeds(force = true) }
    }

    /** Tải hồ sơ theo id, mỗi lần tối đa 30 id (giới hạn của truy vấn `in`). */
    private suspend fun fetchUsers(ids: List<String>): List<User> =
        ids.chunked(30).flatMap { chunk ->
            db.collection(Fs.USERS).whereIn(FieldPath.documentId(), chunk).get().await().documents.mapNotNull { it.toUser() }
        }

    private fun setStatus(userId: String, status: FriendStatus) {
        _graph.update { graph ->
            val statuses = if (status == FriendStatus.NONE) graph.statuses - userId else graph.statuses + (userId to status)
            graph.copy(statuses = statuses, users = graph.users + knownUsers.value.filterKeys { it == userId })
        }
    }

    private fun remember(users: List<User>) {
        knownUsers.update { known -> known + users.associateBy { it.id } }
    }

    private fun requireMe(): String = auth.currentUser.value?.id
        ?: throw AppException("Bạn cần đăng nhập lại", AppException.Reason.NOT_SIGNED_IN)

    private inline fun <T> appCall(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e.toAppException())
    }

    private companion object {
        const val SEARCH_LIMIT = 20L
    }
}
