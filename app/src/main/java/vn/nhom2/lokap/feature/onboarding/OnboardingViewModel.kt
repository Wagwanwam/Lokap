package vn.nhom2.lokap.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.data.fake.FakeAuthStore

class OnboardingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _effect = Channel<OnboardingEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    /** Quyền đã bị từ chối ít nhất một lần (để phân biệt "đóng hộp thoại lần đầu" với "từ chối hẳn"). */
    private val deniedOnce = mutableSetOf<String>()

    /**
     * Cấp -> GRANTED. Còn rationale -> NOT_GRANTED (nút "Cấp quyền" xin lại được).
     * Không rationale: lần đầu coi như đóng hộp thoại (NOT_GRANTED), từ lần 2 là từ chối hẳn (DENIED -> Mở Cài đặt).
     */
    private fun statusAfterRequest(key: String, granted: Boolean, rationale: Boolean): PermissionStatus = when {
        granted -> PermissionStatus.GRANTED
        rationale -> PermissionStatus.NOT_GRANTED.also { deniedOnce += key }
        deniedOnce.add(key) -> PermissionStatus.NOT_GRANTED
        else -> PermissionStatus.DENIED
    }

    fun onAction(action: OnboardingAction) {
        when (action) {
            is OnboardingAction.PageChanged -> {
                _uiState.update { it.copy(currentPage = action.page.coerceIn(0, it.totalPages - 1)) }
            }

            OnboardingAction.NextPageClicked -> {
                _uiState.update { state ->
                    val next = (state.currentPage + 1).coerceAtMost(state.totalPages - 1)
                    state.copy(currentPage = next)
                }
            }

            OnboardingAction.SkipToPermissionsClicked -> {
                _uiState.update { state ->
                    state.copy(currentPage = state.totalPages - 1)
                }
            }

            is OnboardingAction.SyncPermissionState -> {
                _uiState.update { state ->
                    val hasAnyLocation = action.fineLocationGranted || action.coarseLocationGranted
                    val accuracy = when {
                        action.fineLocationGranted -> LocationAccuracyState.FINE
                        action.coarseLocationGranted -> LocationAccuracyState.COARSE_ONLY
                        else -> LocationAccuracyState.NONE
                    }
                    state.copy(
                        cameraStatus = if (action.cameraGranted) PermissionStatus.GRANTED else state.cameraStatus,
                        locationStatus = if (hasAnyLocation) PermissionStatus.GRANTED else state.locationStatus,
                        locationAccuracy = accuracy,
                        notificationStatus = if (action.notificationGranted) PermissionStatus.GRANTED else state.notificationStatus
                    )
                }
            }

            is OnboardingAction.CameraPermissionResult -> {
                _uiState.update { state ->
                    state.copy(cameraStatus = statusAfterRequest("camera", action.granted, action.shouldShowRationale))
                }
            }

            is OnboardingAction.LocationPermissionResult -> {
                _uiState.update { state ->
                    val anyGranted = action.fineGranted || action.coarseGranted
                    val accuracy = when {
                        action.fineGranted -> LocationAccuracyState.FINE
                        action.coarseGranted -> LocationAccuracyState.COARSE_ONLY
                        else -> LocationAccuracyState.NONE
                    }
                    state.copy(
                        locationStatus = statusAfterRequest("location", anyGranted, action.shouldShowRationale),
                        locationAccuracy = accuracy
                    )
                }
            }

            is OnboardingAction.NotificationPermissionResult -> {
                _uiState.update { state ->
                    state.copy(notificationStatus = statusAfterRequest("notification", action.granted, action.shouldShowRationale))
                }
            }

            OnboardingAction.StartClicked -> {
                val current = _uiState.value
                if (!current.canStart) return
                _uiState.update { it.copy(isSubmitting = true) }
                FakeAuthStore.markOnboardingDone()
                viewModelScope.launch {
                    _effect.send(OnboardingEffect.NavigateToLogin)
                }
            }
        }
    }
}
