package vn.nhom2.lokap.domain.model

import java.time.Instant

/**
 * Bài mức Tỉnh không có vị trí cụ thể: bong bóng "Khắp <tỉnh>" đặt ở tâm tỉnh chỉ hiện khi zoom dưới mức này.
 * Zoom gần hơn (nhìn đường phố) thì ẩn, để không bị hiểu nhầm là ảnh chụp đúng ở tâm tỉnh (Đà Nẵng: gần sân bay).
 */
const val PROVINCE_WIDE_MAX_ZOOM = 11f

/** Dùng toạ độ hiển thị đã làm mờ của bài đăng; không suy ra dữ liệu từ danh mục mẫu. */
fun mapBubblesFromPosts(posts: List<Post>, zoom: Float, friendIds: Set<String>): List<MapBubble> {
    val groups = posts.groupBy { post ->
        val location = post.location
        when {
            zoom < 8f -> location.provinceCode?.let { MapBubbleType.PROVINCE to it }
            location.level == LocationLevel.PROVINCE ->
                location.provinceCode?.takeIf { zoom < PROVINCE_WIDE_MAX_ZOOM }?.let { MapBubbleType.PROVINCE_WIDE to it }
            zoom < 13f -> location.wardCode?.let { MapBubbleType.WARD to it }
            location.level == LocationLevel.PLACE -> MapBubbleType.PLACE to post.id
            else -> null
        }
    }
    return groups.mapNotNull { (key, group) ->
        val (type, code) = key ?: return@mapNotNull null
        val cover = group.maxByOrNull { it.takenAt } ?: return@mapNotNull null
        val location = group.firstOrNull {
            it.location.lat?.isFinite() == true && it.location.lng?.isFinite() == true
        }?.location ?: return@mapNotNull null
        val label = when (type) {
            MapBubbleType.PROVINCE -> location.provinceName
            MapBubbleType.PROVINCE_WIDE -> location.provinceName?.let { "Khắp $it" }
            MapBubbleType.WARD -> location.wardName
            MapBubbleType.PLACE -> location.placeName
            MapBubbleType.LANDMARK -> null
        } ?: return@mapNotNull null
        MapBubble(
            id = "${type.name}_$code", type = type, code = code, label = label,
            lat = location.lat!!, lng = location.lng!!, postCount = group.size,
            coverImageUrl = cover.imageUrl, hasFriendPosts = group.any { it.author.id in friendIds }
        )
    }
}

/** Địa danh suy ra từ bài đã tải (khi chưa có danh mục, hoặc ở bản đồ Hồ sơ). Ảnh bìa: nhiều cảm xúc nhất. */
fun landmarksFromPosts(posts: List<Post>, now: Instant = Instant.now()): List<Landmark> = posts
    .filter { it.location.level == LocationLevel.PLACE && it.sendState == PostSendState.SENT }
    .mapNotNull { post -> post.location.landmarkId?.let { it to post } }
    .groupBy({ it.first }, { it.second })
    .mapNotNull { (id, group) ->
        val cover = pickLandmarkCover(group, now) ?: group.minWithOrNull(PostPopularityOrder) ?: return@mapNotNull null
        val location = cover.location
        val name = location.placeName ?: return@mapNotNull null
        val province = location.provinceCode ?: return@mapNotNull null
        val lat = location.lat?.takeIf { it.isFinite() } ?: return@mapNotNull null
        val lng = location.lng?.takeIf { it.isFinite() } ?: return@mapNotNull null
        Landmark(
            id = id, name = name, provinceCode = province, wardCode = location.wardCode,
            lat = lat, lng = lng, coverUrl = cover.imageUrl,
            coverAttribution = cover.author.displayName, postCount = group.size,
            provinceName = location.provinceName.orEmpty(), wardName = location.wardName,
            coverPostId = cover.id, coverReactionCount = cover.reactionTotal
        )
    }
