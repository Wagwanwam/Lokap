package vn.nhom2.lokap.feature.camera

import androidx.compose.runtime.Immutable
import vn.nhom2.lokap.data.fake.SampleData
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
    val friendCount: Int = SampleData.users.size - 1,
    val unreadMessages: Int = SampleData.conversations.sumOf { it.unreadCount },
    val me: User = SampleData.currentUser,
    val zoomRatio: Float = 1f,
    val cameraError: String? = null
)

sealed interface CameraAction {
    data class OnPermissionResult(
        val isGranted: Boolean,
        val shouldShowRationale: Boolean
    ) : CameraAction

    data object OnSyncPermissionGranted : CameraAction
    data object OnRequestPermissionClick : CameraAction
    data object OnOpenSettingsClick : CameraAction
    data object OnToggleLens : CameraAction
    data object OnCycleFlash : CameraAction
    data class OnZoomChange(val zoomRatio: Float) : CameraAction
    data object OnShutterClick : CameraAction
    data class OnPhotoSaved(val imagePath: String) : CameraAction
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
    data class NavigateToComposer(val imagePath: String) : CameraEffect
    data object NavigateToFeed : CameraEffect
    data object NavigateToMap : CameraEffect
    data object NavigateToFriends : CameraEffect
    data object NavigateToInbox : CameraEffect
    data class NavigateToProfile(val userId: String?) : CameraEffect
}
