package vn.nhom2.lokap.domain.model

import androidx.compose.runtime.Immutable
enum class MapBubbleType {
    PROVINCE,
    WARD,
    PROVINCE_WIDE,
    PLACE,
    LANDMARK
}

@Immutable
data class MapBubble(
    val id: String,
    val type: MapBubbleType,
    val code: String?,
    val label: String,
    val lat: Double,
    val lng: Double,
    val postCount: Int,
    val coverImageUrl: String?,
    val hasFriendPosts: Boolean
)

/**
 * Địa danh (collection `landmarks`): do người dùng thêm từ một địa điểm VietMap, toạ độ là điểm công khai.
 * Ảnh bìa, số ảnh không lưu trên server mà tính từ các bài public gắn địa danh (xem LandmarkPhotos.kt).
 */
@Immutable
data class Landmark(
    val id: String,
    val name: String,
    val provinceCode: String,
    val wardCode: String?,
    val lat: Double,
    val lng: Double,
    val category: String = "",
    /** Ảnh của bài có nhiều cảm xúc nhất; null khi chưa có ảnh nào. */
    val coverUrl: String? = null,
    /** Tên người chụp ảnh bìa. */
    val coverAttribution: String = "",
    val description: String = "",
    val postCount: Int = 0,
    val provinceName: String = "",
    val wardName: String? = null,
    val address: String = "",
    val createdBy: String? = null,
    val coverPostId: String? = null,
    val coverReactionCount: Int = 0
)

@Immutable
data class Place(
    val id: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double
)
