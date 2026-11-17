package vn.nhom2.lokap.feature.camera

import androidx.compose.runtime.Immutable
import vn.nhom2.lokap.core.image.PhotoColorFilter
import vn.nhom2.lokap.domain.model.User
enum class CameraPermissionState {
    NOT_REQUESTED,
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED
}

enum class CameraLens {
    BACK,
    FRONT
}

enum class FlashMode {
    OFF,
    ON,
    AUTO
}

@Immutable
data class CameraUiState(
    val permission: CameraPermissionState = CameraPermissionState.NOT_REQUESTED,
    val lensFacing: CameraLens = CameraLens.BACK,
    val flashMode: FlashMode = FlashMode.OFF,
    val isCapturing: Boolean = false,
    val friendCount: Int = 0,
    val unreadMessages: Int = 0,
    /** Người đang đăng nhập; rỗng cho tới khi AuthRepository tải xong hồ sơ. */
    val me: User = User(id = "", username = "", displayName = "", avatarUrl = null),
    val zoomRatio: Float = 1f,
    val cameraError: String? = null,
    /** Bộ lọc màu đang dùng: áp lên khung xem trước và vào ảnh khi chụp. */
    val selectedFilter: PhotoColorFilter = PhotoColorFilter.ORIGINAL,
    /** Vòng bộ lọc đang mở (người dùng đang nhấn giữ nút chụp). */
    val isFilterPickerOpen: Boolean = false,
    /** Bộ lọc dưới ngón tay khi đang lướt trên vòng; null = ngón tay còn ở giữa. */
    val hoveredFilter: PhotoColorFilter? = null
) {
    /** Bộ lọc hiện trên khung xem trước: đang lướt thì xem thử bộ lọc dưới ngón tay. */
    val previewFilter: PhotoColorFilter
        get() = if (isFilterPickerOpen) hoveredFilter ?: selectedFilter else selectedFilter
}

sealed interface CameraAction {
    data class OnPermissionResult(
        val isGranted: Boolean,
        val shouldShowRationale: Boolean,

        val shouldShowRationaleBefore: Boolean
    ) : CameraAction

    data object OnSyncPermissionGranted : CameraAction
    data object OnRequestPermissionClick : CameraAction
    data object OnOpenSettingsClick : CameraAction
    data object OnToggleLens : CameraAction
    data object OnCycleFlash : CameraAction
    data class OnZoomChange(val zoomRatio: Float) : CameraAction
    data object OnShutterClick : CameraAction
    /** Nhấn giữ nút chụp: mở vòng bộ lọc. */
    data object OnFilterPickerOpen : CameraAction
    data class OnFilterHover(val filter: PhotoColorFilter?) : CameraAction
    /** Thả tay: chọn bộ lọc đang trỏ (nếu có) rồi đóng vòng; chưa chụp. */
    data object OnFilterPickerRelease : CameraAction
    /** TalkBack: nhấn giữ nút chụp để chuyển sang bộ lọc kế tiếp. */
    data object OnCycleFilter : CameraAction
    data class OnPhotoSaved(val imagePath: String) : CameraAction
    data class OnGalleryPermissionResult(val granted: Boolean) : CameraAction
    data class OnCameraError(val message: String) : CameraAction
    data object OnRetryCamera : CameraAction
    data object OnSwipeUpToFeed : CameraAction
    data object OnMapClick : CameraAction
    data object OnFriendsClick : CameraAction
    data object OnInboxClick : CameraAction
    data object OnProfileClick : CameraAction
}

sealed interface CameraEffect {
    data object LaunchPermissionRequest : CameraEffect
    data object OpenAppSettings : CameraEffect
    data object TriggerCapture : CameraEffect
    data object RequestGalleryWritePermission : CameraEffect
    data class ShowMessage(val message: String) : CameraEffect
    data class NavigateToComposer(val imagePath: String) : CameraEffect
    data object NavigateToFeed : CameraEffect
    data object NavigateToMap : CameraEffect
    data object NavigateToFriends : CameraEffect
    data object NavigateToInbox : CameraEffect
    data class NavigateToProfile(val userId: String?) : CameraEffect
}
