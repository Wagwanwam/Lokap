package vn.nhom2.lokap.feature.postdetail

import androidx.compose.runtime.Immutable
import java.time.Instant
import vn.nhom2.lokap.domain.model.Post
@Immutable
data class RedditCommentUi(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String? = null,
    val timeAgo: String,
    val content: String,
    val score: Int = 1,
    val userVote: Int = 0,
    val replies: List<RedditCommentUi> = emptyList(),
    val replyToAuthorName: String? = null
)

@Immutable
data class PostDetailUiState(
    val post: Post? = null,
    /** Đang tải bài từ server lần đầu (chưa có trong máy). */
    val isLoading: Boolean = false,
    val isNotFound: Boolean = false,
    /** Không tải được (mất mạng…) và chưa có bài trong máy: hiện lỗi + Thử lại. */
    val errorMessage: String? = null,
    val isOffline: Boolean = false,
    val isReportSheetOpen: Boolean = false,
    val showBlockConfirmDialog: Boolean = false,
    val now: Instant = Instant.now(),
    val currentUserId: String = "",
    val comments: List<RedditCommentUi> = emptyList(),
    val commentInput: String = "",
    val isSavingComment: Boolean = false,
    val commentsError: String? = null,
    val editingComment: RedditCommentUi? = null,
    val editCommentText: String = "",
    val deletingCommentId: String? = null,
    val replyingToComment: RedditCommentUi? = null,
    // Bài của mình: Sửa lời nhắn (≤ 60 ký tự) / Xoá bài
    val isEditCaptionOpen: Boolean = false,
    val editCaptionInput: String = "",
    val isSavingCaption: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val isDeleting: Boolean = false
) {
    val isOwnPost: Boolean
        get() = post != null && currentUserId.isNotBlank() && post.author.id == currentUserId
}

sealed interface PostDetailAction {
    data object OnBackClick : PostDetailAction
    data object OnRetry : PostDetailAction
    data class OnReact(val emoji: String) : PostDetailAction
    data object OnReplyClick : PostDetailAction
    data object OnAuthorClick : PostDetailAction
    data object OnLocationClick : PostDetailAction
    data object OnOpenReportSheet : PostDetailAction
    data object OnDismissReportSheet : PostDetailAction
    data class OnSubmitReport(val reason: String) : PostDetailAction
    data object OnOpenBlockDialog : PostDetailAction
    data object OnDismissBlockDialog : PostDetailAction
    data object OnConfirmBlock : PostDetailAction

    data object OnOpenEditCaption : PostDetailAction
    data object OnDismissEditCaption : PostDetailAction
    data class OnChangeCaptionInput(val text: String) : PostDetailAction
    data object OnSaveCaption : PostDetailAction
    data object OnOpenDeleteDialog : PostDetailAction
    data object OnDismissDeleteDialog : PostDetailAction
    data object OnConfirmDelete : PostDetailAction

    data class OnVoteComment(val commentId: String, val isUpvote: Boolean) : PostDetailAction
    data class OnReplyToComment(val comment: RedditCommentUi?) : PostDetailAction
    data class OnChangeCommentInput(val text: String) : PostDetailAction
    data class OnEditComment(val comment: RedditCommentUi) : PostDetailAction
    data class OnEditCommentText(val text: String) : PostDetailAction
    data object OnSaveComment : PostDetailAction
    data object OnDismissCommentEdit : PostDetailAction
    data class OnDeleteComment(val id: String) : PostDetailAction
    data object OnConfirmCommentDelete : PostDetailAction
    data object OnDismissCommentDelete : PostDetailAction
    data object OnSendComment : PostDetailAction
}

sealed interface PostDetailEffect {
    data object NavigateBack : PostDetailEffect
    data class NavigateToChat(val conversationId: String) : PostDetailEffect
    data class NavigateToProfile(val userId: String) : PostDetailEffect
    data class NavigateToRegionAlbum(val level: String, val code: String) : PostDetailEffect
    data class ShowSnackbar(val message: String) : PostDetailEffect
}

internal fun List<vn.nhom2.lokap.domain.repository.PostComment>.toCommentUi(): List<RedditCommentUi> {
    val all = associateBy { it.id }
    val children = groupBy { it.parentId?.takeIf(all::containsKey) }
    fun build(parent: String?, depth: Int): List<RedditCommentUi> = if (depth > 100) emptyList() else children[parent].orEmpty().map { c ->
        val minutes = java.time.Duration.between(c.createdAt, Instant.now()).toMinutes().coerceAtLeast(0)
        RedditCommentUi(c.id, c.author.id, c.author.displayName, c.author.avatarUrl,
            if (minutes < 1) "Vừa xong" else if (minutes < 60) "$minutes phút trước" else "${minutes / 60} giờ trước",
            c.content, c.score, c.myVote, build(c.id, depth + 1), c.parentId?.let { all[it]?.author?.displayName })
    }
    return build(null, 0)
}
