package vn.nhom2.lokap.domain.model

import java.time.Instant

/** Tổng số cảm xúc của bài (mỗi người thả 1 emoji). */
val Post.reactionTotal: Int get() = reactions.sumOf { it.count }

/** Bài public đã tới giờ hiện: ai cũng xem được, nên mọi người thấy cùng một ảnh bìa. */
fun Post.isPubliclyVisible(now: Instant): Boolean =
    audience == Audience.PUBLIC && sendState == PostSendState.SENT &&
        (publicVisibleAt == null || !publicVisibleAt.isAfter(now))

/** Nhiều cảm xúc hơn đứng trước; bằng nhau thì ảnh mới hơn đứng trước. */
val PostPopularityOrder: Comparator<Post> =
    compareByDescending<Post> { it.reactionTotal }.thenByDescending { it.takenAt }.thenBy { it.id }

/**
 * Ảnh bìa địa danh = bài public đã hiện có nhiều cảm xúc nhất. Bài đang hẹn giờ (chỉ bạn bè thấy sớm)
 * không được chọn, để mọi người cùng thấy một ảnh bìa.
 */
fun pickLandmarkCover(posts: List<Post>, now: Instant): Post? =
    posts.filter { it.isPubliclyVisible(now) }.minWithOrNull(PostPopularityOrder)

/** Các cách xếp / lọc lưới ảnh ở màn Địa danh. */
enum class LandmarkPhotoTab(val label: String) {
    TOP("Nổi bật"),
    NEWEST("Mới nhất"),
    FRIENDS("Bạn bè"),
    MINE("Của tôi")
}

fun List<Post>.arrangeForLandmark(tab: LandmarkPhotoTab, friendIds: Set<String>, meId: String?): List<Post> =
    when (tab) {
        LandmarkPhotoTab.TOP -> sortedWith(PostPopularityOrder)
        LandmarkPhotoTab.NEWEST -> sortedWith(compareByDescending<Post> { it.takenAt }.thenBy { it.id })
        LandmarkPhotoTab.FRIENDS -> filter { it.author.id in friendIds }.sortedWith(PostPopularityOrder)
        LandmarkPhotoTab.MINE -> filter { meId != null && it.author.id == meId }.sortedWith(PostPopularityOrder)
    }

/** Ảnh bìa, tên người chụp và số ảnh của địa danh, tính từ [posts] đã gắn đúng địa danh này. */
fun Landmark.withPhotos(posts: List<Post>, now: Instant): Landmark {
    val own = posts.filter { it.location.landmarkId == id }
    val cover = pickLandmarkCover(own, now)
    return copy(
        coverUrl = cover?.imageUrl,
        coverAttribution = cover?.author?.displayName.orEmpty(),
        coverPostId = cover?.id,
        coverReactionCount = cover?.reactionTotal ?: 0,
        postCount = own.size
    )
}

/**
 * Địa danh cho bản đồ: danh mục trên server (kể cả địa danh chưa có ảnh) + địa danh chỉ biết qua bài đã tải
 * (danh mục chưa tải được khi offline). Số ảnh và ảnh bìa đếm từ bài người xem có quyền xem.
 */
fun landmarksForMap(catalog: List<Landmark>, posts: List<Post>, now: Instant): List<Landmark> {
    val byId = catalog.associateBy { it.id }
    val fromPosts = landmarksFromPosts(posts, now).filter { it.id !in byId }
    return catalog.map { it.withPhotos(posts, now) } + fromPosts
}
