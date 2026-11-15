package vn.nhom2.lokap.domain.repository

import kotlinx.coroutines.flow.Flow
import vn.nhom2.lokap.domain.model.Landmark
import vn.nhom2.lokap.domain.model.Place

/** Địa danh mới từ một địa điểm VietMap đã có toạ độ ([place].lat/lng) và tỉnh/phường chứa nó. */
data class LandmarkDraft(
    val place: Place,
    val name: String,
    val description: String?,
    val region: ResolvedRegion
)

/**
 * Danh mục địa danh (Firestore `landmarks`). Ảnh của địa danh là bài public gắn `landmarkId`,
 * đọc qua [PostRepository.observeLandmarkPosts].
 */
interface LandmarkRepository {
    /** Địa danh đang hoạt động đã tải về (chưa kèm ảnh bìa). */
    val landmarks: Flow<List<Landmark>>

    fun observeLandmark(id: String): Flow<Landmark?>

    /** Tải danh mục địa danh. `force = false` (lúc mở màn): bỏ qua nếu vừa tải thành công chưa lâu. */
    suspend fun refresh(force: Boolean = true): Result<Unit>

    /** Tải lại một địa danh; bị ẩn / không còn thì bỏ khỏi danh mục trên máy. */
    suspend fun refreshLandmark(id: String): Result<Unit>

    /**
     * Thêm địa danh. Đã có địa danh của đúng địa điểm này (hoặc trùng tên trong bán kính
     * [DUPLICATE_RADIUS_M]) thì trả về địa danh có sẵn, không tạo bản trùng.
     */
    suspend fun createLandmark(draft: LandmarkDraft): Result<Landmark>

    suspend fun reportLandmark(id: String, reason: String): Result<Unit>

    companion object {
        const val DUPLICATE_RADIUS_M = 150.0
        const val NAME_MAX_LENGTH = 60
        const val DESCRIPTION_MAX_LENGTH = 150
    }
}

/** Chép ảnh người dùng chọn từ thư viện máy (content:// URI) thành file để Composer xử lý như ảnh vừa chụp. */
interface ImageImporter {
    suspend fun importImage(uri: String): Result<String>
}
