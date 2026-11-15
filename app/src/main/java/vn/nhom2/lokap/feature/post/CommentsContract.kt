package vn.nhom2.lokap.feature.post

import androidx.compose.runtime.Immutable
import java.time.Instant
import vn.nhom2.lokap.core.common.formatTimeAgo
import vn.nhom2.lokap.domain.repository.PostComment

@Immutable
data class CommentUi(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String? = null,
    val timeAgo: String,
    val content: String,
    /** Tổng phiếu (up = +1). Popup chỉ hiện "Thích" nên số > 0 mới hiện. */
    val score: Int = 0,
    val userVote: Int = 0,
    val replies: List<CommentUi> = emptyList(),
    val replyToAuthorName: String? = null
)

/** Popup bình luận toàn màn hình (Feed và trang ảnh dùng chung). */
@Immutable
data class CommentsUiState(
    val postId: String? = null,
    val currentUserId: String = "",
    val isLoading: Boolean = false,
    val comments: List<CommentUi> = emptyList(),
    val errorMessage: String? = null,
    val input: String = "",
    val replyingTo: CommentUi? = null,
    val isSaving: Boolean = false,
    val editing: CommentUi? = null,
    val editText: String = "",
    val deletingId: String? = null
) {
    val totalCount: Int get() = comments.sumOf { 1 + it.replies.countAll() }
    val canSend: Boolean get() = !isSaving && input.isNotBlank()
}

private fun List<CommentUi>.countAll(): Int = sumOf { 1 + it.replies.countAll() }

sealed interface CommentsAction {
    data class OnInputChange(val text: String) : CommentsAction
    data object OnSend : CommentsAction
    data class OnReplyTo(val comment: CommentUi?) : CommentsAction
    data class OnToggleLike(val commentId: String) : CommentsAction
    data class OnEdit(val comment: CommentUi) : CommentsAction
    data class OnEditTextChange(val text: String) : CommentsAction
    data object OnSaveEdit : CommentsAction
    data object OnDismissEdit : CommentsAction
    data class OnDelete(val commentId: String) : CommentsAction
    data object OnConfirmDelete : CommentsAction
    data object OnDismissDelete : CommentsAction
}

/** Dựng cây bình luận: trả lời nằm dưới bình luận gốc, thứ tự theo giờ đăng. */
internal fun List<PostComment>.toCommentUi(now: Instant = Instant.now()): List<CommentUi> {
    val all = associateBy { it.id }
    val children = groupBy { it.parentId?.takeIf(all::containsKey) }
    fun build(parent: String?, depth: Int): List<CommentUi> = if (depth > 100) emptyList() else children[parent].orEmpty().map { c ->
        CommentUi(
            id = c.id,
            authorId = c.author.id,
            authorName = c.author.displayName,
            authorAvatarUrl = c.author.avatarUrl,
            timeAgo = formatTimeAgo(c.createdAt, now),
            content = c.content,
            score = c.score,
            userVote = c.myVote,
            replies = build(c.id, depth + 1),
            replyToAuthorName = c.parentId?.let { all[it]?.author?.displayName }
        )
    }
    return build(null, 0)
}
