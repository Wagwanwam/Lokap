package vn.nhom2.lokap.feature.post

import java.time.Instant
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.model.PostSendState

/**
 * Đăng lại được: ảnh công khai đã gửi xong, đã tới giờ hiện, của người khác (rules ép thêm: không bị chặn).
 * Ảnh bạn bè / gửi riêng thì không, vì bạn bè của mình không có quyền xem ảnh đó.
 */
fun canRepost(post: Post, currentUserId: String, now: Instant): Boolean =
    currentUserId.isNotBlank() &&
        post.author.id != currentUserId &&
        post.audience == Audience.PUBLIC &&
        post.sendState == PostSendState.SENT &&
        (post.publicVisibleAt == null || !post.publicVisibleAt.isAfter(now))

/** Hiện nút ↻ (kèm số lượt) dưới mọi ảnh công khai đã gửi; bấm được khi [canRepost]. */
fun showsRepostButton(post: Post): Boolean =
    post.audience == Audience.PUBLIC && post.sendState == PostSendState.SENT

/**
 * Tab Bạn bè: bài gốc (đã xếp mới nhất trước) xen với ảnh đăng lại theo lúc đăng lại.
 * Ảnh đã có trong feed gốc thì không hiện lần nữa; nhiều người cùng đăng lại 1 ảnh thì giữ lượt mới nhất.
 */
fun mergeFriendTimeline(originals: List<Post>, reposts: List<Post>): List<Post> {
    val originalIds = originals.mapTo(HashSet()) { it.id }
    val extra = reposts
        .filter { it.id !in originalIds }
        .sortedByDescending { it.repostedAt ?: it.takenAt }
        .distinctBy { it.id }
    if (extra.isEmpty()) return originals
    val merged = ArrayList<Post>(originals.size + extra.size)
    var i = 0
    var j = 0
    while (i < originals.size || j < extra.size) {
        val repostFirst = i >= originals.size ||
            (j < extra.size && (extra[j].repostedAt ?: extra[j].takenAt).isAfter(originals[i].takenAt))
        merged += if (repostFirst) extra[j++] else originals[i++]
    }
    return merged
}
