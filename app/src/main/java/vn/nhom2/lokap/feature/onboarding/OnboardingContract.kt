package vn.nhom2.lokap.feature.onboarding

import kotlinx.serialization.Serializable
@Serializable
data object OnboardingRoute

enum class PermissionStatus {
    NOT_GRANTED,
    GRANTED,
    DENIED
}

enum class LocationAccuracyState {
    NONE,
    COARSE_ONLY,
    FINE
}

data class OnboardingUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 4,
    val cameraStatus: PermissionStatus = PermissionStatus.NOT_GRANTED,
    val locationStatus: PermissionStatus = PermissionStatus.NOT_GRANTED,
    val locationAccuracy: LocationAccuracyState = LocationAccuracyState.NONE,
    val notificationStatus: PermissionStatus = PermissionStatus.NOT_GRANTED,
    val isSubmitting: Boolean = false
) {
    val canStart: Boolean
        get() = !isSubmitting
}

sealed interface OnboardingAction {
    data class PageChanged(val page: Int) : OnboardingAction
    data object NextPageClicked : OnboardingAction
    data object SkipToPermissionsClicked : OnboardingAction
    data class SyncPermissionState(
        val cameraGranted: Boolean,
        val fineLocationGranted: Boolean,
        val coarseLocationGranted: Boolean,
        val notificationGranted: Boolean
    ) : OnboardingAction
    data class CameraPermissionResult(
        val granted: Boolean,
        val shouldShowRationale: Boolean
    ) : OnboardingAction
    data class LocationPermissionResult(
        val fineGranted: Boolean,
        val coarseGranted: Boolean,
        val shouldShowRationale: Boolean
    ) : OnboardingAction
    data class NotificationPermissionResult(
        val granted: Boolean,
        val shouldShowRationale: Boolean
    ) : OnboardingAction
    data object StartClicked : OnboardingAction
}

sealed interface OnboardingEffect {
    data object NavigateToLogin : OnboardingEffect
}
