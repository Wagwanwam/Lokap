package vn.nhom2.lokap.feature.post

import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.model.PostSendState
import vn.nhom2.lokap.domain.repository.MessagingRepository

/**
 * Thanh "Gửi tin nhắn…" hiện dưới mọi ảnh của bạn bè (gửi bạn bè, gửi riêng mình, hay công khai).
 * Ảnh của người lạ thì không: nhắn tin chỉ giữa bạn bè (rules ép, xem chatWritable + validPhotoReply).
 */
fun canMessageAbout(post: Post, currentUserId: String, authorStatus: FriendStatus): Boolean =
    currentUserId.isNotBlank() &&
        post.author.id != currentUserId &&
        post.sendState == PostSendState.SENT &&
        authorStatus == FriendStatus.FRIENDS

/** Mở (hoặc tạo) cuộc trò chuyện với tác giả rồi gửi tin kèm ảnh; không chuyển màn (giống Locket). */
suspend fun MessagingRepository.sendPhotoReply(post: Post, text: String): Result<Unit> =
    openConversation(post.author.id).fold(
        onSuccess = { conversationId -> send(conversationId, text, replyTo = post) },
        onFailure = { Result.failure(it) }
    )
