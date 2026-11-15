package vn.nhom2.lokap.data.repository

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import java.io.File
import java.time.Instant
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.nhom2.lokap.core.image.ImageProcessor
import vn.nhom2.lokap.data.local.LocalCache
import vn.nhom2.lokap.data.local.PostDao
import vn.nhom2.lokap.data.local.PostEntity
import vn.nhom2.lokap.data.local.RefreshThrottles
import vn.nhom2.lokap.data.remote.CloudinaryUploader
import vn.nhom2.lokap.data.remote.Fs
import vn.nhom2.lokap.data.remote.isPermissionDenied
import vn.nhom2.lokap.data.remote.isRetryable
import vn.nhom2.lokap.data.remote.toAppException
import vn.nhom2.lokap.data.remote.toAuthorMap
import vn.nhom2.lokap.data.worker.UploadPostWorker
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.Landmark
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.model.PostLocation
import vn.nhom2.lokap.domain.model.PostSendState
import vn.nhom2.lokap.domain.model.Reaction
import vn.nhom2.lokap.domain.model.User
import vn.nhom2.lokap.domain.repository.AppException
import vn.nhom2.lokap.domain.repository.AuthRepository
import vn.nhom2.lokap.domain.repository.PostDraft
import vn.nhom2.lokap.domain.repository.PostRepository

/** Số cảm xúc theo emoji, lưu trong Room dạng JSON. */
@Serializable
internal data class ReactionCount(val emoji: String, val count: Int)

/**
 * Bài đăng trên Firestore, ảnh trên Cloudinary, cache bằng Room.
 * Đọc: truy vấn khớp đúng Security Rules (rules không lọc hộ, truy vấn sai là bị từ chối cả danh sách):
 * - feed Bạn bè: `viewerIds array-contains uid` + `status == active`;
 * - Khám phá: `audience == public` + `status == active` + `publicVisibleAt <= giờ hiện tại`.
 * Ghi: tạo bài ở UploadPostWorker (upload Cloudinary → ghi Firestore), sửa/xoá/cảm xúc/báo cáo ghi thẳng.
 */
@Singleton
class FirestorePostRepository @Inject constructor(
    private val dbLazy: dagger.Lazy<FirebaseFirestore>,
    private val firebaseAuthLazy: dagger.Lazy<FirebaseAuth>,
    private val dao: PostDao,
    private val auth: AuthRepository,
    private val graph: FirestoreGraph,
    private val cloudinary: CloudinaryUploader,
    private val localCache: LocalCache,
    private val workManager: Provider<WorkManager>,
    private val imageProcessor: ImageProcessor,
    private val refreshThrottles: RefreshThrottles
) : PostRepository, PostUploader {

    private val db: FirebaseFirestore get() = dbLazy.get()
    private val posts get() = db.collection(Fs.POSTS)
    private val json = Json { ignoreUnknownKeys = true }

    // Room truy vấn lại sau mỗi lần ghi bảng posts: bỏ kết quả trùng, giải mã JSON từng bài ngoài luồng chính
    override val friendFeed: Flow<List<Post>> = dao.observeFriendFeed().distinctUntilChanged()
        .map { list -> list.map { it.toDomain() } }.flowOn(Dispatchers.Default)

    override val publicPosts: Flow<List<Post>> = dao.observePublicFeed().distinctUntilChanged()
        .map { list -> list.map { it.toDomain() } }.flowOn(Dispatchers.Default)

    // Chưa có collection "đã lưu" / "đăng lại"
    override val savedPosts: Flow<List<Post>> = flowOf(emptyList())
    override val repostedPosts: Flow<List<Post>> = flowOf(emptyList())

    override fun observePost(postId: String): Flow<Post?> =
        dao.observeById(postId).distinctUntilChanged().map { it?.toDomain() }.flowOn(Dispatchers.Default)

    override fun observeUserPosts(userId: String): Flow<List<Post>> =
        combine(dao.observeByAuthor(userId).distinctUntilChanged(), auth.currentUser) { list, me ->
            val isMe = me?.id == userId
            // Bài riêng (gửi 1 người) không hiện trên trang hồ sơ, trừ hồ sơ của chính mình
            list.map { it.toDomain() }.filter { isMe || it.audience != Audience.DIRECT }
        }.flowOn(Dispatchers.Default)

    override fun observeLandmarkPosts(landmarkId: String): Flow<List<Post>> =
        dao.observeByLandmark(landmarkId).distinctUntilChanged()
            .map { list -> list.map { it.toDomain() } }.flowOn(Dispatchers.Default)

    override fun now(): Instant = Instant.now()

    // Mở màn (force = false) mà cùng tài khoản vừa tải xong chưa tới 30 giây thì bỏ qua: Feed, Bản đồ, Album vùng
    // cùng gọi hàm này lúc mở, trước đây mỗi màn tải lại toàn bộ feed và kết quả về đúng lúc đang Back
    override suspend fun refreshFeeds(force: Boolean): Result<Unit> {
        val uid = auth.currentUser.value?.id ?: return refreshFeedsNow()
        return refreshThrottles.feeds.run(uid, force) { refreshFeedsNow() }
    }

    private suspend fun refreshFeedsNow(): Result<Unit> = appCall {
        val me = requireMe()
        val friendIds = graph.friendIds(me.id)
        val blocked = graph.blockedIds(me.id)
        // Vừa kết bạn / đổi tên: cập nhật người xem + tên trên bài của mình (lỗi thì để lần làm mới sau)
        runCatching { graph.reconcileMyPosts(me, friendIds) }

        val friend = posts
            .whereArrayContains(Fs.VIEWER_IDS, me.id)
            .whereEqualTo(Fs.STATUS, Fs.ACTIVE)
            .orderBy(Fs.CREATED_AT, Query.Direction.DESCENDING)
            .limit(FEED_LIMIT)
            .get().await().documents
            .filterNot { it.getString(Fs.AUTHOR_ID) in blocked }
        val public = publicVisible(posts.whereEqualTo(Fs.AUDIENCE, AUDIENCE_PUBLIC))
            .filterNot { it.getString(Fs.AUTHOR_ID) in blocked }

        storeServerPosts(friend, me.id, inFriendFeed = true)
        storeServerPosts(public, me.id, inPublicFeed = true)
        dao.clearFriendFlagExcept(friend.map { it.id })
        dao.clearPublicFlagExcept(public.map { it.id })
    }

    override suspend fun refreshPost(postId: String): Result<Unit> = appCall {
        val me = requireMe()
        val snap = try {
            posts.document(postId).get().await()
        } catch (e: Exception) {
            // Không còn quyền xem (bị huỷ kết bạn, bài bị ẩn…) cũng coi như bài không còn
            if (e.isPermissionDenied) null else throw e
        }
        val visible = snap != null && snap.exists() &&
            (snap.getString(Fs.STATUS) == Fs.ACTIVE || snap.getString(Fs.AUTHOR_ID) == me.id)
        if (!visible) {
            if (dao.getById(postId)?.sendState == PostSendState.SENT.name) dao.deleteById(postId)
        } else {
            storeServerPosts(listOf(snap!!), me.id)
        }
    }

    override suspend fun refreshUserPosts(userId: String): Result<Unit> = appCall {
        val me = requireMe()
        val docs = if (userId == me.id) {
            posts.whereEqualTo(Fs.AUTHOR_ID, me.id)
                .orderBy(Fs.CREATED_AT, Query.Direction.DESCENDING)
                .limit(USER_POSTS_LIMIT)
                .get().await().documents
        } else {
            // Người lạ chỉ thấy bài public đã tới giờ; bạn bè thấy thêm bài có mình trong viewerIds
            val asViewer = posts.whereEqualTo(Fs.AUTHOR_ID, userId)
                .whereArrayContains(Fs.VIEWER_IDS, me.id)
                .whereEqualTo(Fs.STATUS, Fs.ACTIVE)
                .orderBy(Fs.CREATED_AT, Query.Direction.DESCENDING)
                .limit(USER_POSTS_LIMIT)
                .get().await().documents
            val asPublic = publicVisible(
                posts.whereEqualTo(Fs.AUTHOR_ID, userId).whereEqualTo(Fs.AUDIENCE, AUDIENCE_PUBLIC)
            )
            (asViewer + asPublic).distinctBy { it.id }
        }
        storeServerPosts(docs, me.id)
        if (docs.size < USER_POSTS_LIMIT.toInt()) dao.deleteStaleByAuthor(userId, docs.map { it.id })
    }

    override suspend fun refreshLandmarkPosts(landmarkId: String): Result<Unit> = appCall {
        val me = requireMe()
        val blocked = graph.blockedIds(me.id)
        // Rules chỉ cho truy vấn bài public đã tới giờ → mọi người nhận cùng một tập bài, cùng ảnh bìa
        val docs = publicVisible(
            posts.whereEqualTo(Fs.LANDMARK_ID, landmarkId).whereEqualTo(Fs.AUDIENCE, AUDIENCE_PUBLIC),
            limit = LANDMARK_POSTS_LIMIT
        ).filterNot { it.getString(Fs.AUTHOR_ID) in blocked }
        storeServerPosts(docs, me.id)
        if (docs.size < LANDMARK_POSTS_LIMIT.toInt()) {
            dao.deleteStaleByLandmark(landmarkId, me.id, docs.map { it.id })
        }
    }

    override suspend fun setPostLandmark(postId: String, landmark: Landmark?): Result<Unit> = appCall {
        val me = requireMe()
        val local = dao.getById(postId)
            ?: throw AppException("Bài đã bị xoá hoặc ẩn", AppException.Reason.NOT_FOUND)
        if (local.authorId != me.id) throw AppException("Chỉ gắn được ảnh của bạn")
        if (local.audience != Audience.PUBLIC.name) throw AppException("Chỉ ảnh công khai mới gắn được vào địa danh")
        if (local.sendState != PostSendState.SENT.name) throw AppException("Ảnh đang gửi, thử lại sau")
        val updated: PostEntity
        val changes: Map<String, Any?>
        if (landmark == null) {
            // Gỡ ra: giữ vị trí ở điểm của địa danh (đã công khai từ trước), chỉ bỏ liên kết
            updated = local.copy(landmarkId = null)
            changes = mapOf(Fs.LANDMARK_ID to null)
        } else {
            // Vị trí hiển thị = điểm công khai của địa danh (rules so khớp với document địa danh)
            updated = local.copy(
                landmarkId = landmark.id,
                locationLevel = LocationLevel.PLACE.name,
                provinceCode = landmark.provinceCode,
                provinceName = landmark.provinceName,
                wardCode = landmark.wardCode,
                wardName = landmark.wardName,
                placeId = null,
                placeName = landmark.name,
                displayLat = landmark.lat,
                displayLng = landmark.lng
            )
            changes = mapOf(
                Fs.LANDMARK_ID to landmark.id,
                Fs.LOCATION_LEVEL to "place",
                Fs.PROVINCE_CODE to landmark.provinceCode,
                Fs.PROVINCE_NAME to landmark.provinceName,
                Fs.WARD_CODE to landmark.wardCode,
                Fs.WARD_NAME to landmark.wardName,
                Fs.PLACE_ID to null,
                Fs.PLACE_NAME to landmark.name,
                Fs.DISPLAY_LAT to landmark.lat,
                Fs.DISPLAY_LNG to landmark.lng
            )
        }
        posts.document(postId).update(changes).await()
        dao.upsert(updated)
    }

    override suspend fun createPost(draft: PostDraft): Result<String> = appCall {
        val me = auth.currentUser.value
            ?: throw AppException("Bạn cần đăng nhập để gửi bài", AppException.Reason.NOT_SIGNED_IN)
        val postId = UUID.randomUUID().toString()
        // Ghi file mới đã xoay, cắt vuông, thu nhỏ và KHÔNG có EXIF (không lộ toạ độ GPS trong ảnh)
        val prepared = withContext(Dispatchers.Default) {
            imageProcessor.prepareForUpload(File(draft.imagePath), File(localCache.pendingImagesDir, "$postId.jpg"))
        }
        val now = System.currentTimeMillis()
        val isPublic = draft.audience == Audience.PUBLIC
        dao.upsert(
            PostEntity(
                id = postId,
                authorId = me.id,
                authorUsername = me.username,
                authorDisplayName = me.displayName,
                authorAvatarUrl = me.avatarUrl,
                imagePath = "",
                localImagePath = prepared.absolutePath,
                remoteImageUrl = null,
                caption = draft.caption?.trim()?.take(60)?.ifEmpty { null },
                audience = draft.audience.name,
                locationLevel = draft.location.level.name,
                provinceCode = draft.location.provinceCode,
                provinceName = draft.location.provinceName,
                wardCode = draft.location.wardCode,
                wardName = draft.location.wardName,
                placeId = draft.placeId,
                placeName = draft.location.placeName,
                landmarkId = draft.location.landmarkId,
                displayLat = draft.location.lat,
                displayLng = draft.location.lng,
                takenAt = now,
                publicVisibleAt = if (isPublic) now + draft.publishDelay.days.days.inWholeMilliseconds else null,
                createdAt = now,
                reactionsJson = "[]",
                myReaction = null,
                sendState = PostSendState.PENDING.name,
                inFriendFeed = true,
                inPublicFeed = false,
                pendingDelayDays = if (isPublic) draft.publishDelay.days else 0,
                pendingRecipientId = draft.recipientId,
                pendingExactLat = draft.exactLocation?.lat,
                pendingExactLng = draft.exactLocation?.lng,
                pendingAccuracyM = draft.exactLocation?.accuracyM
            )
        )
        enqueueUpload(postId, ExistingWorkPolicy.KEEP)
        postId
    }

    override suspend fun retrySend(postId: String) {
        dao.updateSendState(postId, PostSendState.PENDING.name)
        enqueueUpload(postId, ExistingWorkPolicy.REPLACE)
    }

    override suspend fun updateCaption(postId: String, caption: String?): Result<Unit> = appCall {
        val clean = caption?.trim()?.ifEmpty { null }
        if (clean != null && clean.length > 60) throw AppException("Lời nhắn tối đa 60 ký tự")
        val local = dao.getById(postId)
        if (local != null && local.sendState != PostSendState.SENT.name) {
            // Chưa gửi xong: sửa trên máy, worker sẽ gửi caption mới
            dao.upsert(local.copy(caption = clean))
            return@appCall
        }
        posts.document(postId).update(Fs.CAPTION, clean).await()
        if (local != null) dao.upsert(local.copy(caption = clean))
    }

    override suspend fun deletePost(postId: String): Result<Unit> = appCall {
        val local = dao.getById(postId)
        if (local != null && local.sendState != PostSendState.SENT.name) {
            workManager.get().cancelUniqueWork(UploadPostWorker.uniqueName(postId))
            removeLocal(local)
            // Phòng khi worker đã kịp tạo bài trên server
            runCatching { deleteOnServer(postId) }
            return@appCall
        }
        deleteOnServer(postId)
        if (local != null) removeLocal(local) else dao.deleteById(postId)
    }

    override suspend fun toggleReaction(postId: String, emoji: String): Result<Unit> = appCall {
        val me = requireMe()
        val local = dao.getById(postId)
            ?: throw AppException("Bài đã bị xoá hoặc ẩn", AppException.Reason.NOT_FOUND)
        if (local.sendState != PostSendState.SENT.name) return@appCall
        val next = if (local.myReaction == emoji) null else emoji
        // Đổi ngay trên máy cho mượt, lỗi thì trả lại
        val counts = recount(decodeReactions(local.reactionsJson), previous = local.myReaction, next = next)
        dao.upsert(local.copy(myReaction = next, reactionsJson = json.encodeToString(counts)))
        try {
            // Rules: người xem chỉ được sửa đúng ô reactions.<uid của mình>
            val batch = db.batch().update(posts.document(postId), FieldPath.of(Fs.REACTIONS, me.id), next ?: FieldValue.delete())
            if (next != null && local.authorId != me.id) batch.set(
                db.collection(Fs.USERS).document(local.authorId).collection("notifications").document(),
                notificationData("REACTION", me.id, postId = postId, sourceId = me.id))
            batch.commit().await()
        } catch (e: Exception) {
            dao.upsert(local)
            throw e
        }
    }

    override suspend fun reportPost(postId: String, reason: String): Result<Unit> = appCall {
        val me = requireMe()
        val createdAt = firebaseAuthLazy.get().currentUser?.metadata?.creationTimestamp ?: 0L
        if (System.currentTimeMillis() - createdAt < 24.hours.inWholeMilliseconds) {
            throw AppException("Tài khoản mới tạo dưới 24 giờ chưa báo cáo được", AppException.Reason.ACCOUNT_TOO_NEW)
        }
        val postRef = posts.document(postId)
        val post = postRef.get().await()
        if (!post.exists()) throw AppException("Bài đã bị xoá hoặc ẩn", AppException.Reason.NOT_FOUND)
        if (post.getString(Fs.AUTHOR_ID) == me.id) throw AppException("Không thể báo cáo bài của chính mình")
        val reportRef = postRef.collection(Fs.REPORTS).document(me.id)
        // Transaction: đếm báo cáo và tự ẩn bài khi đủ 3 (rules kiểm tra đúng +1 và có phiếu báo cáo đi kèm)
        db.runTransaction { tx ->
            val fresh = tx.get(postRef)
            val already = tx.get(reportRef).exists()
            if (fresh.exists() && !already) {
                val count = (fresh.getLong(Fs.REPORT_COUNT) ?: 0L) + 1
                tx.set(reportRef, mapOf(Fs.REASON to reason, Fs.CREATED_AT to FieldValue.serverTimestamp()))
                tx.update(
                    postRef,
                    mapOf(
                        Fs.REPORT_COUNT to count,
                        Fs.STATUS to if (count >= REPORTS_TO_HIDE) Fs.HIDDEN else (fresh.getString(Fs.STATUS) ?: Fs.ACTIVE)
                    )
                )
            }
            null
        }.await()
        // Bài mình đã báo cáo thì bỏ khỏi máy luôn
        dao.deleteById(postId)
    }

    override suspend fun hidePostsByAuthor(authorId: String) = dao.deleteByAuthor(authorId)

    // ------------------------------------------------------------------ upload (UploadPostWorker)

    override suspend fun upload(postId: String): UploadOutcome {
        val entity = dao.getById(postId) ?: return UploadOutcome.DONE
        if (entity.sendState == PostSendState.SENT.name) return UploadOutcome.DONE
        val file = entity.localImagePath?.let(::File)?.takeIf { it.exists() } ?: return UploadOutcome.FAILED
        val me = auth.currentUser.value ?: return UploadOutcome.FAILED
        val postRef = posts.document(postId)
        return try {
            // Lần trước đã ghi xong nhưng mất mạng lúc chờ phản hồi → coi như đã gửi
            val existing = runCatching { postRef.get(Source.SERVER).await() }.getOrNull()
            if (existing != null && existing.exists()) {
                notifyPostViewers(postId, me.id, (existing.get(Fs.VIEWER_IDS) as? List<*>)?.filterIsInstance<String>().orEmpty())
                markSent(postId, existing.getString(Fs.IMAGE_PUBLIC_ID).orEmpty(), existing.getString(Fs.IMAGE_URL), null)
                return UploadOutcome.DONE
            }

            val image = cloudinary.uploadPostImage(file, me.id, postId)
            val audience = Audience.valueOf(entity.audience)
            val viewers = when (audience) {
                Audience.DIRECT -> listOfNotNull(me.id, entity.pendingRecipientId)
                else -> listOf(me.id) + graph.friendIds(me.id)
            }.distinct()
            // Rules so giờ này với giờ server (cho lệch vài phút) → tính lúc gửi, không phải lúc soạn
            val visibleAt = if (audience == Audience.PUBLIC) {
                System.currentTimeMillis() + entity.pendingDelayDays.days.inWholeMilliseconds
            } else {
                null
            }
            val data = mapOf(
                Fs.AUTHOR_ID to me.id,
                Fs.AUTHOR to me.toAuthorMap(),
                Fs.IMAGE_URL to image.url,
                Fs.IMAGE_PUBLIC_ID to image.publicId,
                Fs.CAPTION to entity.caption,
                Fs.AUDIENCE to audience.name.lowercase(),
                Fs.VIEWER_IDS to viewers,
                Fs.RECIPIENT_ID to entity.pendingRecipientId,
                Fs.LOCATION_LEVEL to entity.locationLevel.lowercase(),
                Fs.PROVINCE_CODE to entity.provinceCode,
                Fs.PROVINCE_NAME to entity.provinceName,
                Fs.WARD_CODE to entity.wardCode,
                Fs.WARD_NAME to entity.wardName,
                Fs.PLACE_ID to entity.placeId,
                Fs.PLACE_NAME to entity.placeName,
                Fs.LANDMARK_ID to entity.landmarkId,
                Fs.DISPLAY_LAT to entity.displayLat,
                Fs.DISPLAY_LNG to entity.displayLng,
                Fs.DELAY_DAYS to entity.pendingDelayDays,
                Fs.PUBLIC_VISIBLE_AT to visibleAt?.let { Timestamp(Date(it)) },
                Fs.TAKEN_AT to Timestamp(Date(entity.takenAt)),
                Fs.CREATED_AT to FieldValue.serverTimestamp(),
                Fs.STATUS to Fs.ACTIVE,
                Fs.REACTIONS to emptyMap<String, String>(),
                Fs.REPORT_COUNT to 0
            )
            val batch = db.batch()
            batch.set(postRef, data)
            val lat = entity.pendingExactLat
            val lng = entity.pendingExactLng
            if (lat != null && lng != null) {
                batch.set(
                    postRef.collection(Fs.PRIVATE).document(Fs.LOCATION),
                    mapOf(Fs.LAT to lat, Fs.LNG to lng, Fs.ACCURACY_M to entity.pendingAccuracyM?.toDouble())
                )
            }
            // Firestore giữ lệnh ghi trong hàng đợi khi mất mạng; quá lâu thì để worker thử lại sau
            withTimeoutOrNull(COMMIT_TIMEOUT) { batch.commit().await() } ?: return UploadOutcome.RETRY

            if (dao.getById(postId) == null) {
                // Người dùng đã xoá bài trong lúc đang gửi: xoá luôn trên server
                runCatching { deleteOnServer(postId) }
            } else {
                notifyPostViewers(postId, me.id, viewers)
                markSent(postId, image.publicId, image.url, visibleAt)
            }
            UploadOutcome.DONE
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e.isRetryable()) UploadOutcome.RETRY else UploadOutcome.FAILED
        }
    }

    override suspend fun markFailed(postId: String) = dao.updateSendState(postId, PostSendState.FAILED.name)

    // ------------------------------------------------------------------ helpers

    private suspend fun markSent(postId: String, publicId: String, url: String?, visibleAt: Long?) {
        val latest = dao.getById(postId) ?: return
        dao.upsert(
            latest.copy(
                sendState = PostSendState.SENT.name,
                imagePath = publicId,
                remoteImageUrl = url,
                publicVisibleAt = visibleAt ?: latest.publicVisibleAt,
                // Toạ độ gốc đã nằm trên server (chỉ mình đọc được), không giữ thêm trên máy
                pendingExactLat = null,
                pendingExactLng = null,
                pendingAccuracyM = null
            )
        )
    }

    /**
     * Bài public đã tới giờ hiện. Rules so `publicVisibleAt <= request.time` (giờ server), nên mốc truy vấn
     * phải không vượt giờ server: lùi 2 phút phòng đồng hồ máy chạy nhanh; vẫn bị từ chối thì lùi 30 phút.
     */
    private suspend fun publicVisible(base: Query, limit: Long = FEED_LIMIT): List<DocumentSnapshot> {
        suspend fun query(margin: Duration) = base
            .whereEqualTo(Fs.STATUS, Fs.ACTIVE)
            .whereLessThanOrEqualTo(Fs.PUBLIC_VISIBLE_AT, Timestamp(Date(System.currentTimeMillis() - margin.inWholeMilliseconds)))
            .orderBy(Fs.PUBLIC_VISIBLE_AT, Query.Direction.DESCENDING)
            .limit(limit)
            .get().await().documents
        return try {
            query(2.minutes)
        } catch (e: Exception) {
            if (e.isPermissionDenied) query(30.minutes) else throw e
        }
    }

    private fun enqueueUpload(postId: String, policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<UploadPostWorker>()
            .setInputData(workDataOf(UploadPostWorker.KEY_POST_ID to postId))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .addTag(UploadPostWorker.TAG)
            .build()
        workManager.get().enqueueUniqueWork(UploadPostWorker.uniqueName(postId), policy, request)
    }

    /** Separate small transactions retain all block checks without exceeding rules' batch access limit. */
    private suspend fun notifyPostViewers(postId: String, authorId: String, viewers: List<String>) {
        viewers.filter { it != authorId }.forEach { recipient ->
            val receipt = posts.document(postId).collection("notificationReceipts").document(recipient)
            try {
                db.runTransaction { tx ->
                    if (!tx.get(receipt).exists()) {
                        tx.set(receipt, mapOf("createdAt" to FieldValue.serverTimestamp()))
                        tx.set(db.collection(Fs.USERS).document(recipient).collection("notifications").document("post_" + postId),
                            notificationData("NEW_POST", authorId, postId = postId, sourceId = postId))
                    }
                }.await()
            } catch (e: Exception) {
                // A recipient can block the author while upload is in progress. Never bypass that block.
                if (!e.isPermissionDenied) throw e
            }
        }
    }

    /** Xoá bài + toạ độ gốc. Ảnh trên Cloudinary còn lại (app không có API secret để xoá). */
    private suspend fun deleteOnServer(postId: String) {
        val postRef = posts.document(postId)
        // Firestore does not cascade into subcollections; remove comments while the parent still exists.
        postRef.collection("comments").get(Source.SERVER).await().documents.chunked(400).forEach { docs ->
            db.batch().apply { docs.forEach { delete(it.reference) } }.commit().await()
        }
        val receipts = postRef.collection("notificationReceipts").get(Source.SERVER).await().documents
        db.batch().apply {
            receipts.forEach { delete(it.reference) }
            delete(postRef.collection(Fs.PRIVATE).document(Fs.LOCATION))
            delete(postRef)
        }.commit().await()
    }

    private suspend fun removeLocal(entity: PostEntity) {
        dao.deleteById(entity.id)
        entity.localImagePath?.let { path -> withContext(Dispatchers.IO) { File(path).delete() } }
    }

    /** Gộp bài từ server với bản trên máy (giữ ảnh local, cờ feed) rồi lưu. */
    private suspend fun storeServerPosts(
        docs: List<DocumentSnapshot>,
        me: String,
        inFriendFeed: Boolean? = null,
        inPublicFeed: Boolean? = null
    ) {
        if (docs.isEmpty()) return
        val existing = dao.getByIds(docs.map { it.id }).associateBy { it.id }
        // Đổi DocumentSnapshot → PostEntity (mã hoá JSON từng bài) ngoài luồng chính
        val entities = withContext(Dispatchers.Default) {
            docs.mapNotNull { it.toEntity(me, existing[it.id], inFriendFeed, inPublicFeed) }
        }
        dao.upsertAll(entities)
    }

    private fun DocumentSnapshot.toEntity(
        me: String,
        existing: PostEntity?,
        inFriendFeed: Boolean?,
        inPublicFeed: Boolean?
    ): PostEntity? {
        val authorId = getString(Fs.AUTHOR_ID) ?: return null
        val author = get(Fs.AUTHOR) as? Map<*, *>
        val reactions = (get(Fs.REACTIONS) as? Map<*, *>).orEmpty().entries
            .mapNotNull { (uid, emoji) -> if (uid is String && emoji is String) uid to emoji else null }
            .toMap()
        val counts = reactions.values.groupingBy { it }.eachCount()
            .map { (emoji, count) -> ReactionCount(emoji, count) }
            .sortedByDescending { it.count }
        fun millis(field: String): Long? =
            getTimestamp(field, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
        return PostEntity(
            id = id,
            authorId = authorId,
            authorUsername = author?.get(Fs.USERNAME) as? String ?: existing?.authorUsername.orEmpty(),
            authorDisplayName = author?.get(Fs.DISPLAY_NAME) as? String ?: existing?.authorDisplayName.orEmpty(),
            authorAvatarUrl = author?.get(Fs.AVATAR_URL) as? String,
            imagePath = getString(Fs.IMAGE_PUBLIC_ID).orEmpty(),
            localImagePath = existing?.localImagePath,
            remoteImageUrl = getString(Fs.IMAGE_URL),
            caption = getString(Fs.CAPTION),
            audience = getString(Fs.AUDIENCE).orEmpty().uppercase(),
            locationLevel = getString(Fs.LOCATION_LEVEL).orEmpty().uppercase(),
            provinceCode = getString(Fs.PROVINCE_CODE),
            provinceName = getString(Fs.PROVINCE_NAME),
            wardCode = getString(Fs.WARD_CODE),
            wardName = getString(Fs.WARD_NAME),
            placeId = getString(Fs.PLACE_ID),
            placeName = getString(Fs.PLACE_NAME),
            landmarkId = getString(Fs.LANDMARK_ID),
            displayLat = getDouble(Fs.DISPLAY_LAT),
            displayLng = getDouble(Fs.DISPLAY_LNG),
            takenAt = millis(Fs.TAKEN_AT) ?: existing?.takenAt ?: 0L,
            publicVisibleAt = millis(Fs.PUBLIC_VISIBLE_AT),
            createdAt = millis(Fs.CREATED_AT) ?: existing?.createdAt ?: System.currentTimeMillis(),
            reactionsJson = json.encodeToString(counts),
            myReaction = reactions[me],
            sendState = PostSendState.SENT.name,
            inFriendFeed = inFriendFeed ?: existing?.inFriendFeed ?: false,
            inPublicFeed = inPublicFeed ?: existing?.inPublicFeed ?: false
        )
    }

    private fun PostEntity.toDomain() = Post(
        id = id,
        author = User(id = authorId, username = authorUsername, displayName = authorDisplayName, avatarUrl = authorAvatarUrl),
        imageUrl = localImagePath ?: remoteImageUrl.orEmpty(),
        caption = caption,
        audience = enumOr(audience, Audience.FRIENDS),
        location = PostLocation(
            level = enumOr(locationLevel, LocationLevel.COUNTRY),
            provinceCode = provinceCode,
            provinceName = provinceName,
            wardCode = wardCode,
            wardName = wardName,
            placeName = placeName,
            landmarkId = landmarkId,
            lat = displayLat,
            lng = displayLng
        ),
        takenAt = Instant.ofEpochMilli(takenAt),
        publicVisibleAt = publicVisibleAt?.let(Instant::ofEpochMilli),
        reactions = decodeReactions(reactionsJson).map { Reaction(it.emoji, it.count) },
        myReaction = myReaction,
        sendState = enumOr(sendState, PostSendState.SENT)
    )

    private inline fun <reified T : Enum<T>> enumOr(value: String, default: T): T =
        runCatching { enumValueOf<T>(value) }.getOrDefault(default)

    private fun decodeReactions(raw: String): List<ReactionCount> =
        runCatching { json.decodeFromString<List<ReactionCount>>(raw) }.getOrDefault(emptyList())

    private fun recount(current: List<ReactionCount>, previous: String?, next: String?): List<ReactionCount> {
        val counts = current.associate { it.emoji to it.count }.toMutableMap()
        if (previous != null) {
            val left = (counts[previous] ?: 1) - 1
            if (left <= 0) counts.remove(previous) else counts[previous] = left
        }
        if (next != null) counts[next] = (counts[next] ?: 0) + 1
        return counts.map { (emoji, count) -> ReactionCount(emoji, count) }.sortedByDescending { it.count }
    }

    private fun requireMe(): User = auth.currentUser.value
        ?: throw AppException("Bạn cần đăng nhập lại", AppException.Reason.NOT_SIGNED_IN)

    private inline fun <T> appCall(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e.toAppException())
    }

    private companion object {
        const val AUDIENCE_PUBLIC = "public"
        const val FEED_LIMIT = 50L
        const val USER_POSTS_LIMIT = 100L
        const val LANDMARK_POSTS_LIMIT = 100L
        const val REPORTS_TO_HIDE = 3L
        val COMMIT_TIMEOUT = 30.seconds
    }
}
