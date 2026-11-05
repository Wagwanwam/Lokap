package vn.nhom2.lokap.domain.model

import androidx.compose.runtime.Immutable
import java.time.Instant

/**
 * [lat] và [lng] là toạ độ ĐÃ LÀM MỜ để hiển thị an toàn trên UI.
 */
@Immutable
data class PostLocation(
    val level: LocationLevel,
    val provinceCode: String?,
    val provinceName: String?,
    val wardCode: String?,
    val wardName: String?,
    val placeName: String?,
    val landmarkId: String?,
    val lat: Double?,
    val lng: Double?
)

enum class PostSendState {
    PENDING,
    SENT,
    FAILED
}

@Immutable
data class Reaction(
    val emoji: String,
    val count: Int
)

@Immutable
data class Post(
    val id: String,
    val author: User,
    val imageUrl: String,
    val caption: String?,
    val audience: Audience,
    val location: PostLocation,
    val takenAt: Instant,
    val publicVisibleAt: Instant?,
    val reactions: List<Reaction>,
    val myReaction: String?,
    val sendState: PostSendState
)
