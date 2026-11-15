package vn.nhom2.lokap.domain.repository

import java.time.Instant
import kotlinx.coroutines.flow.Flow
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.Landmark
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.model.PostLocation
import vn.nhom2.lokap.domain.model.PublishDelay

/** Bài vừa soạn ở Composer, chưa gửi lên server. */
data class PostDraft(
    val imagePath: String,
    val caption: String?,
    val audience: Audience,
    /** Tên/mã tỉnh, phường, địa điểm để hiện ngay khi "Đang gửi…"; server tự tính lại toạ độ hiển thị. */
    val location: PostLocation,
    val placeId: String?,
    val publishDelay: PublishDelay,
    val recipientId: String? = null,
    /** Toạ độ GPS gốc: chỉ tác giả đọc được (posts/{id}/private/location), null khi chọn "Việt Nam". */
    val exactLocation: DeviceLocation? = null
)

interface PostRepository {
    /** Feed Bạn bè: bài của mình (gồm bài đang gửi), của bạn bè, bài riêng gửi mình. Mới nhất trước. */
    val friendFeed: Flow<List<Post>>

    /** Bài public đã tới giờ hiện (tab Khám phá). */
    val publicPosts: Flow<List<Post>>

    /** Tab "Đã lưu" và "Đăng lại" ở Hồ sơ (bản Firestore chưa có collection nên trả rỗng). */
    val savedPosts: Flow<List<Post>>
    val repostedPosts: Flow<List<Post>>

    fun observePost(postId: String): Flow<Post?>

    /** Bài hiện trên trang hồ sơ của [userId], đã lọc theo luật hiển thị. */
    fun observeUserPosts(userId: String): Flow<List<Post>>

    /** Giờ hiện tại để tính "x phút trước". */
    fun now(): Instant

    /**
     * Tải feed bạn bè + Khám phá từ server về máy. `force = false` (lúc mở màn): bỏ qua nếu cùng tài khoản vừa tải
     * thành công chưa lâu, dữ liệu đã có trong máy. `force = true`: kéo để làm mới / Thử lại.
     */
    suspend fun refreshFeeds(force: Boolean = true): Result<Unit>

    suspend fun refreshPost(postId: String): Result<Unit>

    suspend fun refreshUserPosts(userId: String): Result<Unit>

    /** Lưu bài (trạng thái Đang gửi) và đưa vào hàng đợi upload. Trả về id bài. */
    suspend fun createPost(draft: PostDraft): Result<String>

    suspend fun retrySend(postId: String)

    suspend fun updateCaption(postId: String, caption: String?): Result<Unit>

    suspend fun deletePost(postId: String): Result<Unit>

    /**
     * Ảnh của địa danh: bài public gắn [landmarkId] đã tải về máy (gồm bài của mình và bài bạn bè đang hẹn giờ).
     * Chưa sắp xếp; xem LandmarkPhotos.kt.
     */
    fun observeLandmarkPosts(landmarkId: String): Flow<List<Post>>

    /** Tải tối đa 100 bài public đã tới giờ của địa danh (cùng dữ liệu cho mọi người → cùng ảnh bìa). */
    suspend fun refreshLandmarkPosts(landmarkId: String): Result<Unit>

    /**
     * Gắn bài public đã gửi của mình vào [landmark] (vị trí đổi sang điểm công khai của địa danh),
     * hoặc gỡ ra khi [landmark] = null.
     */
    suspend fun setPostLandmark(postId: String, landmark: Landmark?): Result<Unit>

    /** Bấm lại đúng emoji đang chọn thì bỏ cảm xúc. */
    suspend fun toggleReaction(postId: String, emoji: String): Result<Unit>

    suspend fun reportPost(postId: String, reason: String): Result<Unit>

    /** Sau khi chặn: bỏ ngay mọi bài của người đó khỏi máy. */
    suspend fun hidePostsByAuthor(authorId: String)
}
