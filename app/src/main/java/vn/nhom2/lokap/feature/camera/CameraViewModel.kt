package vn.nhom2.lokap.feature.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CameraViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _effects = Channel<CameraEffect>(Channel.BUFFERED)
    val effects: Flow<CameraEffect> = _effects.receiveAsFlow()

    fun onAction(action: CameraAction) {
        when (action) {
            is CameraAction.OnSyncPermissionGranted -> {
                _uiState.update {
                    it.copy(permission = CameraPermissionState.GRANTED, cameraError = null)
                }
            }

            is CameraAction.OnPermissionResult -> {
                val nextPermission = when {
                    action.isGranted -> CameraPermissionState.GRANTED
                    action.shouldShowRationale -> CameraPermissionState.DENIED
                    else -> CameraPermissionState.PERMANENTLY_DENIED
                }
                _uiState.update {
                    it.copy(permission = nextPermission, cameraError = null)
                }
            }

            is CameraAction.OnRequestPermissionClick -> {
                sendEffect(CameraEffect.LaunchPermissionRequest)
            }

            is CameraAction.OnOpenSettingsClick -> {
                sendEffect(CameraEffect.OpenAppSettings)
            }

            is CameraAction.OnToggleLens -> {
                _uiState.update { state ->
                    val nextLens = if (state.lensFacing == CameraLens.BACK) {
                        CameraLens.FRONT
                    } else {
                        CameraLens.BACK
                    }
                    state.copy(lensFacing = nextLens, zoomRatio = 1f, cameraError = null)
                }
            }

            is CameraAction.OnCycleFlash -> {
                _uiState.update { state ->
                    val nextFlash = when (state.flashMode) {
                        FlashMode.OFF -> FlashMode.ON
                        FlashMode.ON -> FlashMode.AUTO
                        FlashMode.AUTO -> FlashMode.OFF
                    }
                    state.copy(flashMode = nextFlash)
                }
            }

            is CameraAction.OnZoomChange -> {
                _uiState.update { state ->
                    state.copy(zoomRatio = action.zoomRatio.coerceIn(1f, 8f))
                }
            }

            is CameraAction.OnShutterClick -> {
                val current = _uiState.value
                if (current.permission == CameraPermissionState.GRANTED &&
                    !current.isCapturing &&
                    current.cameraError == null
                ) {
                    _uiState.update { it.copy(isCapturing = true) }
                    sendEffect(CameraEffect.TriggerCapture)
                }
            }

            is CameraAction.OnPhotoSaved -> {
                _uiState.update { it.copy(isCapturing = false) }
                sendEffect(CameraEffect.NavigateToComposer(action.imagePath))
            }

            is CameraAction.OnCameraError -> {
                _uiState.update {
                    it.copy(isCapturing = false, cameraError = action.message)
                }
            }

            is CameraAction.OnRetryCamera -> {
                _uiState.update { it.copy(cameraError = null, isCapturing = false) }
            }

            is CameraAction.OnSwipeUpToFeed -> {
                sendEffect(CameraEffect.NavigateToFeed)
            }

            is CameraAction.OnMapClick -> {
                sendEffect(CameraEffect.NavigateToMap)
            }

            is CameraAction.OnFriendsClick -> {
                sendEffect(CameraEffect.NavigateToFriends)
            }

            is CameraAction.OnInboxClick -> {
                sendEffect(CameraEffect.NavigateToInbox)
            }

            is CameraAction.OnProfileClick -> {
                sendEffect(CameraEffect.NavigateToProfile(_uiState.value.me.id))
            }
        }
    }

    private fun sendEffect(effect: CameraEffect) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }
}
