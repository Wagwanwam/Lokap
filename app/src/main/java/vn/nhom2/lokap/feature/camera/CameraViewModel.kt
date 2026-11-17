package vn.nhom2.lokap.feature.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.nhom2.lokap.core.image.ImageProcessor
import vn.nhom2.lokap.core.image.PhotoColorFilter
import vn.nhom2.lokap.domain.repository.MessagingRepository
import vn.nhom2.lokap.domain.repository.AuthRepository
import vn.nhom2.lokap.domain.repository.DeviceSettingsRepository
import vn.nhom2.lokap.domain.repository.PhotoGalleryRepository
import vn.nhom2.lokap.domain.repository.SocialRepository

@HiltViewModel
class CameraViewModel @Inject constructor(
    auth: AuthRepository,
    social: SocialRepository,
    messages: MessagingRepository,
    private val imageProcessor: ImageProcessor,
    private val deviceSettings: DeviceSettingsRepository,
    private val photoGallery: PhotoGalleryRepository
) : ViewModel() {

    /** Luồng xử lý ảnh (đổi được trong test). */
    internal var imageDispatcher: CoroutineDispatcher = Dispatchers.Default

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _effects = Channel<CameraEffect>(Channel.BUFFERED)
    val effects: Flow<CameraEffect> = _effects.receiveAsFlow()

    private data class CaptureRequest(val filter: PhotoColorFilter, val autoSave: Boolean)
    private var captureRequest: CaptureRequest? = null
    private var pendingGalleryPath: String? = null

    init {
        // Avatar góc trên: người đang đăng nhập
        viewModelScope.launch {
            auth.currentUser.collect { me -> if (me != null) _uiState.update { it.copy(me = me) } }
        }
        // Chip "Bạn bè": số bạn thật
        viewModelScope.launch {
            social.graph.collect { graph -> _uiState.update { it.copy(friendCount = graph.friendCount) } }
        }
        // Số tin chưa đọc từ các cuộc trò chuyện trên Firestore
        viewModelScope.launch {
            messages.observeInbox().collect { result -> _uiState.update { it.copy(unreadMessages = result.getOrDefault(emptyList()).sumOf { c -> c.unreadCount }) } }
        }
    }

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

                    action.shouldShowRationaleBefore -> CameraPermissionState.PERMANENTLY_DENIED

                    _uiState.value.permission == CameraPermissionState.NOT_REQUESTED -> CameraPermissionState.DENIED
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
                    captureRequest = CaptureRequest(current.selectedFilter, deviceSettings.settings.value.autoSaveToGallery)
                    _uiState.update { it.copy(isCapturing = true) }
                    sendEffect(CameraEffect.TriggerCapture)
                }
            }

            is CameraAction.OnFilterPickerOpen -> {
                val current = _uiState.value
                if (current.permission == CameraPermissionState.GRANTED &&
                    !current.isCapturing &&
                    current.cameraError == null
                ) {
                    _uiState.update { it.copy(isFilterPickerOpen = true, hoveredFilter = null) }
                }
            }

            is CameraAction.OnFilterHover -> {
                _uiState.update { if (it.isFilterPickerOpen) it.copy(hoveredFilter = action.filter) else it }
            }

            is CameraAction.OnFilterPickerRelease -> {
                _uiState.update {
                    it.copy(
                        selectedFilter = it.hoveredFilter ?: it.selectedFilter,
                        isFilterPickerOpen = false,
                        hoveredFilter = null
                    )
                }
            }

            is CameraAction.OnCycleFilter -> {
                _uiState.update {
                    val all = PhotoColorFilter.entries
                    it.copy(selectedFilter = all[(it.selectedFilter.ordinal + 1) % all.size])
                }
            }

            is CameraAction.OnPhotoSaved -> applyFilterThenCompose(action.imagePath)

            is CameraAction.OnGalleryPermissionResult -> {
                // Consume once: a repeated activity result must not export or navigate twice.
                val path = pendingGalleryPath ?: return
                pendingGalleryPath = null
                viewModelScope.launch {
                    if (action.granted) saveThenCompose(path)
                    else finishCapture(path, "Chưa có quyền lưu vào thư viện. Ảnh vẫn được giữ để soạn bài.")
                }
            }

            is CameraAction.OnCameraError -> {
                captureRequest = null
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
                // Hồ sơ của mình: Route.Profile(userId = null) để thanh điều hướng dưới vẫn hiện
                sendEffect(CameraEffect.NavigateToProfile(null))
            }
        }
    }

    /** Export the processed capture exactly once, using the choice made at shutter time. */
    private fun applyFilterThenCompose(imagePath: String) {
        val request = captureRequest ?: return
        captureRequest = null
        viewModelScope.launch {
            val finalPath = if (request.filter == PhotoColorFilter.ORIGINAL) imagePath else withContext(imageDispatcher) {
                runCatching {
                    val input = File(imagePath)
                    val output = File(input.parentFile, input.nameWithoutExtension + "_${request.filter.name.lowercase()}.jpg")
                    imageProcessor.applyColorFilter(input, output, request.filter)
                    input.delete()
                    output.absolutePath
                }.getOrDefault(imagePath) // lỗi xử lý: vẫn giữ ảnh gốc, không mất khoảnh khắc
            }
            when {
                !request.autoSave -> finishCapture(finalPath)
                photoGallery.requiresWritePermission() -> {
                    pendingGalleryPath = finalPath
                    _effects.send(CameraEffect.RequestGalleryWritePermission)
                }
                else -> saveThenCompose(finalPath)
            }
        }
    }

    private suspend fun saveThenCompose(imagePath: String) {
        val message = photoGallery.savePhoto(imagePath).fold(
            onSuccess = { "Đã lưu ảnh vào thư viện" },
            onFailure = { "Không lưu được vào thư viện. Ảnh vẫn được giữ để soạn bài." }
        )
        finishCapture(imagePath, message)
    }

    private suspend fun finishCapture(imagePath: String, message: String? = null) {
        _uiState.update { it.copy(isCapturing = false) }
        if (message != null) _effects.send(CameraEffect.ShowMessage(message))
        _effects.send(CameraEffect.NavigateToComposer(imagePath))
    }

    private fun sendEffect(effect: CameraEffect) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }
}
