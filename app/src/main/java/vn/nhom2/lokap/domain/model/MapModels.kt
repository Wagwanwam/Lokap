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

@Immutable
data class Landmark(
    val id: String,
    val name: String,
    val provinceCode: String,
    val wardCode: String?,
    val lat: Double,
    val lng: Double,
    val category: String,
    val coverUrl: String,
    val coverAttribution: String,
    val description: String,
    val postCount: Int
)

@Immutable
data class Place(
    val id: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double
)
