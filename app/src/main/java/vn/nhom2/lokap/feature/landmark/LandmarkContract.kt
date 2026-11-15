package vn.nhom2.lokap.feature.landmark

import androidx.compose.runtime.Immutable
import vn.nhom2.lokap.domain.model.Landmark
import vn.nhom2.lokap.domain.model.LandmarkPhotoTab
import vn.nhom2.lokap.domain.model.Post

@Immutable
data class LandmarkUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    /** Địa danh kèm ảnh bìa (bài public nhiều cảm xúc nhất) và số ảnh. */
    val landmark: Landmark? = null,
    val provinceName: String = "",
    /** Lưới ảnh theo [tab] đang chọn. */
    val posts: List<Post> = emptyList(),
    val totalPostCount: Int = 0,
    val tab: LandmarkPhotoTab = LandmarkPhotoTab.TOP,
    val meId: String? = null,
    /** Bảng chọn nguồn ảnh: Chụp · Thư viện · Ảnh đã đăng. */
    val isAddSheetOpen: Boolean = false,
    /** Bảng chọn bài public đã đăng của mình để gắn vào địa danh. */
    val isAttachSheetOpen: Boolean = false,
    val attachablePosts: List<Post> = emptyList(),
    /** Ảnh của mình đang hỏi "Gỡ khỏi địa danh?". */
    val detachCandidate: Post? = null,
    val isReportSheetOpen: Boolean = false,
    /** Đang chép ảnh từ thư viện / đang gắn, gỡ ảnh. */
    val isBusy: Boolean = false
)

sealed interface LandmarkAction {
    data class OnPostClick(val postId: String) : LandmarkAction
    data class OnPostLongClick(val post: Post) : LandmarkAction
    data class OnSelectTab(val tab: LandmarkPhotoTab) : LandmarkAction
    data object OnAddPhotoClick : LandmarkAction
    data object OnDismissAddSheet : LandmarkAction
    data object OnTakePhotoClick : LandmarkAction
    data object OnPickFromGalleryClick : LandmarkAction
    /** Photo Picker trả về ảnh (null = người dùng huỷ). */
    data class OnGalleryResult(val uri: String?) : LandmarkAction
    data object OnAttachExistingClick : LandmarkAction
    data object OnDismissAttachSheet : LandmarkAction
    data class OnAttachPost(val postId: String) : LandmarkAction
    data object OnConfirmDetach : LandmarkAction
    data object OnDismissDetach : LandmarkAction
    data object OnReportClick : LandmarkAction
    data object OnDismissReport : LandmarkAction
    data class OnSubmitReport(val reason: String) : LandmarkAction
    data object OnRetry : LandmarkAction
    data object OnBackClick : LandmarkAction
}

sealed interface LandmarkEffect {
    data object NavigateBack : LandmarkEffect
    data class NavigateToPostDetail(val postId: String) : LandmarkEffect
    data class NavigateToCamera(val landmarkId: String) : LandmarkEffect
    data class NavigateToComposer(val imagePath: String, val landmarkId: String) : LandmarkEffect
    /** Mở Photo Picker của hệ thống (không cần quyền đọc bộ nhớ). */
    data object LaunchGalleryPicker : LandmarkEffect
    data class ShowMessage(val message: String) : LandmarkEffect
}
