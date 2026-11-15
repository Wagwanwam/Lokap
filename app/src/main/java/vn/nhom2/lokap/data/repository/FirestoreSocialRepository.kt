package vn.nhom2.lokap.data.repository

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
    @ApplicationScope appScope: CoroutineScope
) : SocialRepository {

    private val db: FirebaseFirestore get() = dbLazy.get()

    override val maxFriends: Int = 20

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
        graphSource.friendshipsOf(me).forEach { doc ->
            val other = graphSource.otherUser(doc, me) ?: return@forEach
            statuses[other] = when {
                doc.getString(Fs.STATUS) == Fs.ACCEPTED -> FriendStatus.FRIENDS
                doc.getString(Fs.REQUESTER) == me -> FriendStatus.REQUEST_SENT
                else -> FriendStatus.REQUEST_RECEIVED
            }
        }
        graphSource.blockedIds(me).forEach { statuses[it] = FriendStatus.BLOCKED }

        val users = fetchUsers(statuses.keys.toList())
        remember(users)
        _graph.value = SocialGraph(statuses = statuses, users = knownUsers.value)

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

    override suspend fun sendFriendRequest(userId: String): Result<FriendStatus> = appCall {
        val me = requireMe()
        if (_graph.value.friendCount >= maxFriends) {
            throw AppException("Bạn đã đạt giới hạn $maxFriends người bạn", AppException.Reason.FRIEND_LIMIT)
        }
        val otherCount = db.collection(Fs.USERS).document(userId).get().await().getLong(Fs.FRIEND_COUNT) ?: 0L
        if (otherCount >= maxFriends) {
            throw AppException("Người này đã đủ $maxFriends bạn", AppException.Reason.FRIEND_LIMIT_OTHER)
        }
        val ref = db.collection(Fs.FRIENDSHIPS).document(Fs.pairId(me, userId))
        val status = db.runTransaction { tx ->
            val snap = tx.get(ref)
            when {
                !snap.exists() -> {
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
                    tx.set(db.collection(Fs.USERS).document(userId).collection("notifications").document(),
                        notificationData("FRIEND_REQUEST", me, sourceId = ref.id))
                    FriendStatus.REQUEST_SENT
                }
                snap.getString(Fs.STATUS) == Fs.ACCEPTED -> FriendStatus.FRIENDS
                // Người kia đã mời mình trước: mời lại = chấp nhận luôn
                snap.getString(Fs.REQUESTER) == userId -> {
                    tx.update(ref, mapOf(Fs.STATUS to Fs.ACCEPTED, Fs.ACCEPTED_AT to FieldValue.serverTimestamp()))
                    tx.set(db.collection(Fs.USERS).document(userId).collection("notifications").document(),
                        notificationData("FRIEND_ACCEPTED", me, sourceId = ref.id))
                    FriendStatus.FRIENDS
                }
                else -> FriendStatus.REQUEST_SENT
            }
        }.await()
        setStatus(userId, status)
        status
    }

    override suspend fun cancelFriendRequest(userId: String): Result<Unit> = deleteFriendship(userId, detach = false)

    override suspend fun acceptFriendRequest(userId: String): Result<Unit> = appCall {
        val me = requireMe()
        if (_graph.value.friendCount >= maxFriends) {
            throw AppException("Bạn đã đạt giới hạn $maxFriends người bạn", AppException.Reason.FRIEND_LIMIT)
        }
        val ref = db.collection(Fs.FRIENDSHIPS).document(Fs.pairId(me, userId))
        db.batch().update(ref, mapOf(Fs.STATUS to Fs.ACCEPTED, Fs.ACCEPTED_AT to FieldValue.serverTimestamp()))
            .set(db.collection(Fs.USERS).document(userId).collection("notifications").document(),
                notificationData("FRIEND_ACCEPTED", me, sourceId = ref.id)).commit().await()
        setStatus(userId, FriendStatus.FRIENDS)
        // Bài cũ của mình sẽ thêm người bạn mới vào viewerIds ở lần làm mới feed kế tiếp (FirestoreGraph)
    }

    override suspend fun declineFriendRequest(userId: String): Result<Unit> = deleteFriendship(userId, detach = false)

    override suspend fun unfriend(userId: String): Result<Unit> = deleteFriendship(userId, detach = true)

    override suspend fun block(userId: String): Result<Unit> = appCall {
        val me = requireMe()
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
    }

    override suspend fun unblock(userId: String): Result<Unit> = appCall {
        val me = requireMe()
        db.collection(Fs.BLOCKS).document(Fs.blockId(me, userId)).delete().await()
        setStatus(userId, FriendStatus.NONE)
    }

    private suspend fun deleteFriendship(userId: String, detach: Boolean): Result<Unit> = appCall {
        val me = requireMe()
        db.collection(Fs.FRIENDSHIPS).document(Fs.pairId(me, userId)).delete().await()
        setStatus(userId, FriendStatus.NONE)
        // Hết là bạn: hai bên không còn thấy bài bạn bè của nhau
        if (detach) runCatching { graphSource.detachViewers(me, userId) }
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
