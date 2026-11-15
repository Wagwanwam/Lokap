package vn.nhom2.lokap.domain.model

import androidx.compose.runtime.Immutable
import java.time.Instant

/** Chú thích tối đa 45 ký tự: vừa 2 dòng trong khung chữ trên ảnh (rules vẫn cho ≤ 60). */
const val POST_CAPTION_MAX_LENGTH = 45

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
    val sendState: PostSendState,
    /** Tab Bạn bè: ảnh công khai mà người này (mình hoặc bạn bè) đã đăng lại; null = bài gốc. */
    val repostedBy: User? = null,
    val repostedAt: Instant? = null
)

/** Số lượt đăng lại của 1 bài và mình đã đăng lại chưa. */
@Immutable
data class RepostState(
    val count: Int = 0,
    val isMine: Boolean = false
)
