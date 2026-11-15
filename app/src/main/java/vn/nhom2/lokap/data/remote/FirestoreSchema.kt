package vn.nhom2.lokap.data.remote

import com.google.firebase.firestore.DocumentSnapshot
import java.text.Normalizer
import vn.nhom2.lokap.domain.model.User

/*
 * Tên collection / trường trên Firestore. Phải khớp firebase/firestore.rules và docs/database.md.
 *
 * users/{uid}                       hồ sơ công khai (mọi người đã đăng nhập đọc được)
 * users/{uid}/private/profile       email, năm sinh, cài đặt — chỉ chủ đọc/ghi
 * usernames/{username}              { uid } — giữ username là duy nhất
 * friendships/{uidA_uidB}           uidA < uidB; pending / accepted
 * blocks/{blocker_blocked}          chỉ người chặn đọc được
 * posts/{postId}                    chỉ có toạ độ đã làm mờ; viewerIds = ai được xem (tác giả + bạn bè / người nhận)
 * posts/{postId}/private/location   toạ độ gốc — chỉ tác giả đọc được
 * posts/{postId}/reports/{uid}      mỗi người báo cáo 1 lần
 * landmarks/{id}                    địa danh (id = "vm_" + băm mã VietMap); ảnh = bài public có landmarkId
 * landmarks/{id}/reports/{uid}      mỗi người báo cáo 1 lần, đủ 3 phiếu thì ẩn
 */
internal object Fs {
    const val USERS = "users"
    const val PRIVATE = "private"
    const val PROFILE = "profile"
    const val USERNAMES = "usernames"
    const val FRIENDSHIPS = "friendships"
    const val BLOCKS = "blocks"
    const val POSTS = "posts"
    const val LOCATION = "location"
    const val REPORTS = "reports"
    /** posts/{postId}/reposts/{uid}: lượt đăng lại (truy vấn nhóm theo uid). */
    const val REPOSTS = "reposts"

    // users
    const val USERNAME = "username"
    const val DISPLAY_NAME = "displayName"
    const val SEARCH_NAME = "searchName"
    const val AVATAR_URL = "avatarUrl"
    const val BIO = "bio"
    const val FRIEND_COUNT = "friendCount"
    const val CREATED_AT = "createdAt"

    // users/{uid}/private/profile
    const val EMAIL = "email"
    const val BIRTH_YEAR = "birthYear"
    const val HOME_PROVINCE = "homeProvinceCode"
    const val DEFAULT_LEVEL = "defaultLocationLevel"
    const val NOTIFY_NEW_PHOTOS = "notifyNewPhotos"
    const val NOTIFY_REACTIONS = "notifyReactions"
    const val NOTIFY_MESSAGES = "notifyMessages"
    const val NOTIFY_FRIEND_REQUESTS = "notifyFriendRequests"

    // usernames
    const val UID = "uid"

    // friendships
    const val PAIR_USERS = "users"
    const val REQUESTER = "requesterId"
    const val ADDRESSEE = "addresseeId"
    const val STATUS = "status"
    const val ACCEPTED_AT = "acceptedAt"
    const val PENDING = "pending"
    const val ACCEPTED = "accepted"

    // blocks
    const val BLOCKER = "blockerId"
    const val BLOCKED = "blockedId"

    // posts
    const val AUTHOR_ID = "authorId"
    const val AUTHOR = "author"
    const val IMAGE_URL = "imageUrl"
    const val IMAGE_PUBLIC_ID = "imagePublicId"
    const val CAPTION = "caption"
    const val AUDIENCE = "audience"
    const val VIEWER_IDS = "viewerIds"
    const val RECIPIENT_ID = "recipientId"
    const val LOCATION_LEVEL = "locationLevel"
    const val PROVINCE_CODE = "provinceCode"
    const val PROVINCE_NAME = "provinceName"
    const val WARD_CODE = "wardCode"
    const val WARD_NAME = "wardName"
    const val PLACE_ID = "placeId"
    const val PLACE_NAME = "placeName"
    const val LANDMARK_ID = "landmarkId"
    const val DISPLAY_LAT = "displayLat"
    const val DISPLAY_LNG = "displayLng"
    const val DELAY_DAYS = "delayDays"
    const val PUBLIC_VISIBLE_AT = "publicVisibleAt"
    const val TAKEN_AT = "takenAt"
    const val REACTIONS = "reactions"
    const val REPORT_COUNT = "reportCount"
    const val ACTIVE = "active"
    const val HIDDEN = "hidden"

    // posts/{id}/private/location
    const val LAT = "lat"
    const val LNG = "lng"
    const val ACCURACY_M = "accuracyM"

    // posts/{id}/reports/{uid}, landmarks/{id}/reports/{uid}
    const val REASON = "reason"

    // landmarks/{id}: địa danh người dùng thêm từ địa điểm VietMap (lat/lng = điểm công khai)
    const val LANDMARKS = "landmarks"
    const val NAME = "name"
    const val ADDRESS = "address"
    const val DESCRIPTION = "description"
    const val VIETMAP_REF = "vietmapRef"
    const val CREATED_BY = "createdBy"

    /** Id của cặp bạn: uid nhỏ trước, nên hai người luôn chung 1 document. */
    fun pairId(a: String, b: String): String = if (a < b) "${a}_$b" else "${b}_$a"

    fun blockId(blocker: String, blocked: String): String = "${blocker}_$blocked"

    /** Tên để tìm kiếm: chữ thường, bỏ dấu ("Nguyễn Đức" → "nguyen duc"). */
    fun searchKey(text: String): String =
        Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace('đ', 'd')
}

internal fun DocumentSnapshot.toUser(): User? {
    if (!exists()) return null
    return User(
        id = id,
        username = getString(Fs.USERNAME).orEmpty(),
        displayName = getString(Fs.DISPLAY_NAME).orEmpty(),
        avatarUrl = getString(Fs.AVATAR_URL),
        bio = getString(Fs.BIO).orEmpty()
    )
}

/** Bản chụp tên/ảnh tác giả lưu kèm bài (Firestore không join được), tác giả tự cập nhật khi đổi hồ sơ. */
internal fun User.toAuthorMap(): Map<String, Any?> = mapOf(
    Fs.USERNAME to username,
    Fs.DISPLAY_NAME to displayName,
    Fs.AVATAR_URL to avatarUrl
)
