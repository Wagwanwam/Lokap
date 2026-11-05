package vn.nhom2.lokap.feature.postdetail

import androidx.compose.runtime.Immutable
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.Post
import java.time.Instant

@Immutable
data class PostDetailUiState(
    val post: Post? = null,
    val isLoading: Boolean = false,
    val isNotFound: Boolean = false,
    val isReportSheetOpen: Boolean = false,
    val showBlockConfirmDialog: Boolean = false,
    val now: Instant = SampleData.NOW,
    val currentUserId: String = SampleData.currentUser.id
)

sealed interface PostDetailAction {
    data object OnBackClick : PostDetailAction
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
}

sealed interface PostDetailEffect {
    data object NavigateBack : PostDetailEffect
    data class NavigateToChat(val conversationId: String) : PostDetailEffect
    data class NavigateToProfile(val userId: String) : PostDetailEffect
    data class NavigateToRegionAlbum(val level: String, val code: String) : PostDetailEffect
    data class ShowSnackbar(val message: String) : PostDetailEffect
}
