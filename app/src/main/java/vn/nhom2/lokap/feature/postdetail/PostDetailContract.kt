package vn.nhom2.lokap.feature.postdetail

import androidx.compose.runtime.Immutable
import java.time.Instant
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.model.RepostState
import vn.nhom2.lokap.feature.post.canMessageAbout
import vn.nhom2.lokap.feature.post.canRepost
import vn.nhom2.lokap.feature.post.showsRepostButton

/**
 * Trang 1 ảnh (mở từ Bản đồ, Hồ sơ, Album, Địa danh, Thông báo, Chat): chỉ có thẻ ảnh giống Feed + menu ⋮.
 * Cảm xúc chọn trong bảng emoji, bình luận ở popup toàn màn hình, nhắn tin bằng thanh dưới ảnh bạn bè.
 */
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
    val authorStatus: FriendStatus = FriendStatus.NONE,
    val commentsCount: Int = 0,
    val repostState: RepostState = RepostState(),
    val isCommentsOpen: Boolean = false,
    val messageDraft: String = "",
    val isSendingMessage: Boolean = false,
    // Bài của mình: Sửa lời nhắn / Xoá bài
    val isEditCaptionOpen: Boolean = false,
    val editCaptionInput: String = "",
    val isSavingCaption: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val isDeleting: Boolean = false
) {
    val isOwnPost: Boolean
        get() = post != null && currentUserId.isNotBlank() && post.author.id == currentUserId

    /** Ảnh công khai của người khác: không có thanh nhắn tin. */
    val canMessage: Boolean
        get() = post != null && canMessageAbout(post, currentUserId, authorStatus)

    /** Nút ↻ dưới ảnh công khai; bấm được khi là ảnh đã hiện của người khác. */
    val showRepost: Boolean
        get() = post != null && showsRepostButton(post)

    val canRepost: Boolean
        get() = post != null && canRepost(post, currentUserId, now)

    /** Bình luận dành cho bài công khai; bài khác chỉ hiện khi đã có bình luận cũ. */
    val showComments: Boolean
        get() = post != null && (post.audience == Audience.PUBLIC || commentsCount > 0)
}

sealed interface PostDetailAction {
    data object OnBackClick : PostDetailAction
    data object OnRetry : PostDetailAction
    data class OnReact(val emoji: String) : PostDetailAction
    data object OnAuthorClick : PostDetailAction
    data object OnLocationClick : PostDetailAction
    data object OnOpenComments : PostDetailAction
    data object OnDismissComments : PostDetailAction
    data object OnToggleRepost : PostDetailAction
    data class OnMessageDraftChange(val text: String) : PostDetailAction
    data object OnSendMessage : PostDetailAction
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
}

sealed interface PostDetailEffect {
    data object NavigateBack : PostDetailEffect
    data class NavigateToProfile(val userId: String) : PostDetailEffect
    data class NavigateToRegionAlbum(val level: String, val code: String) : PostDetailEffect
    data class ShowSnackbar(val message: String) : PostDetailEffect
}
