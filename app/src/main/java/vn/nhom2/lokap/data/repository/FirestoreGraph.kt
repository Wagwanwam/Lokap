package vn.nhom2.lokap.data.repository

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await
import vn.nhom2.lokap.data.local.SessionPrefs
import vn.nhom2.lokap.data.remote.Fs
import vn.nhom2.lokap.data.remote.toAuthorMap
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.User

/**
 * Quan hệ bạn bè / chặn trên Firestore và danh sách người xem của bài (viewerIds).
 *
 * Firestore không join được như Postgres, và Security Rules không lọc được danh sách theo bảng khác,
 * nên mỗi bài lưu sẵn `viewerIds` = tác giả + bạn bè (hoặc + người nhận với bài gửi riêng).
 * Rules chỉ cho đọc khi `uid in viewerIds` → feed Bạn bè là 1 truy vấn `array-contains`.
 * Khi danh sách bạn đổi, tác giả cập nhật lại viewerIds trên bài của mình ([reconcileMyPosts]).
 */
@Singleton
class FirestoreGraph @Inject constructor(
    private val dbLazy: dagger.Lazy<FirebaseFirestore>,
    private val prefs: SessionPrefs
) {
    private val db: FirebaseFirestore get() = dbLazy.get()

    suspend fun friendshipsOf(uid: String): List<DocumentSnapshot> =
        db.collection(Fs.FRIENDSHIPS).whereArrayContains(Fs.PAIR_USERS, uid).get().await().documents

    suspend fun friendIds(uid: String): List<String> =
        friendshipsOf(uid)
            .filter { it.getString(Fs.STATUS) == Fs.ACCEPTED }
            .mapNotNull { otherUser(it, uid) }

    suspend fun blockedIds(uid: String): Set<String> =
        db.collection(Fs.BLOCKS).whereEqualTo(Fs.BLOCKER, uid).get().await().documents
            .mapNotNull { it.getString(Fs.BLOCKED) }
            .toSet()

    fun otherUser(friendship: DocumentSnapshot, me: String): String? =
        (friendship.get(Fs.PAIR_USERS) as? List<*>)?.filterIsInstance<String>()?.firstOrNull { it != me }

    /** Huỷ kết bạn / chặn: bỏ [other] khỏi bài của mình và bỏ mình khỏi bài của [other]. */
    suspend fun detachViewers(me: String, other: String) {
        val mine = db.collection(Fs.POSTS)
            .whereEqualTo(Fs.AUTHOR_ID, me)
            .whereArrayContains(Fs.VIEWER_IDS, other)
            .get().await().documents
        // Rules chỉ cho đọc bài đang active của người khác → truy vấn phải có điều kiện status
        val theirs = db.collection(Fs.POSTS)
            .whereEqualTo(Fs.AUTHOR_ID, other)
            .whereArrayContains(Fs.VIEWER_IDS, me)
            .whereEqualTo(Fs.STATUS, Fs.ACTIVE)
            .get().await().documents
        commitUpdates(
            mine.map { it.reference to mapOf(Fs.VIEWER_IDS to FieldValue.arrayRemove(other)) } +
                theirs.map { it.reference to mapOf(Fs.VIEWER_IDS to FieldValue.arrayRemove(me)) }
        )
        prefs.reconciledFingerprint = null
    }

    /**
     * Đặt lại viewerIds (= mình + bạn bè hiện tại) và bản chụp tên/ảnh tác giả trên bài của mình.
     * Chỉ chạy khi danh sách bạn hoặc hồ sơ đổi so với lần trước (đỡ tốn lượt đọc Firestore).
     */
    suspend fun reconcileMyPosts(me: User, friendIds: List<String>) {
        val viewers = (listOf(me.id) + friendIds.sorted()).distinct()
        val author = me.toAuthorMap()
        val fingerprint = (viewers + author.values.map { it.toString() }).joinToString("|").hashCode().toString()
        if (prefs.reconciledFingerprint == fingerprint) return

        val posts = db.collection(Fs.POSTS).whereEqualTo(Fs.AUTHOR_ID, me.id).limit(MAX_OWN_POSTS).get().await().documents
        val updates = posts.mapNotNull { doc ->
            val change = mutableMapOf<String, Any>()
            val isDirect = doc.getString(Fs.AUDIENCE) == Audience.DIRECT.name.lowercase()
            val current = (doc.get(Fs.VIEWER_IDS) as? List<*>)?.filterIsInstance<String>().orEmpty()
            if (!isDirect && current.toSet() != viewers.toSet()) change[Fs.VIEWER_IDS] = viewers
            if (doc.get(Fs.AUTHOR) != author) change[Fs.AUTHOR] = author
            change.takeIf { it.isNotEmpty() }?.let { doc.reference to it }
        }
        commitUpdates(updates)
        prefs.reconciledFingerprint = fingerprint
    }

    /** Ghi nhiều update, mỗi batch tối đa 500 thao tác (giới hạn của Firestore). */
    suspend fun commitUpdates(updates: List<Pair<DocumentReference, Map<String, Any>>>) {
        updates.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { (ref, data) -> batch.update(ref, data) }
            batch.commit().await()
        }
    }

    private companion object {
        const val BATCH_LIMIT = 450
        const val MAX_OWN_POSTS = 500L
    }
}
