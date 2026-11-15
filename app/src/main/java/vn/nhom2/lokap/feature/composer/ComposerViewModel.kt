package vn.nhom2.lokap.feature.composer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.floor
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.data.location.ProvinceCatalog
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.POST_CAPTION_MAX_LENGTH
import vn.nhom2.lokap.domain.model.Place
import vn.nhom2.lokap.domain.model.PostLocation
import vn.nhom2.lokap.domain.model.PublishDelay
import vn.nhom2.lokap.domain.repository.DeviceLocation
import vn.nhom2.lokap.domain.repository.LandmarkRepository
import vn.nhom2.lokap.domain.repository.LocationRepository
import vn.nhom2.lokap.domain.repository.PostDraft
import vn.nhom2.lokap.domain.repository.PostRepository

/**
 * Soạn bài: gửi qua PostRepository.createPost (lưu bài "Đang gửi" vào máy rồi upload nền).
 * Vị trí: Fused Location Provider → VietMap đổi ra tỉnh/phường; địa điểm tìm bằng VietMap Autocomplete.
 * Toạ độ hiển thị luôn là điểm cố định đã làm mờ (tâm tỉnh / ô lưới ~2 km / toạ độ địa điểm công khai),
 * toạ độ GPS gốc chỉ đi vào chỗ riêng của bài (chỉ tác giả đọc được).
 */
@HiltViewModel
class ComposerViewModel @Inject constructor(
    private val posts: PostRepository,
    private val location: LocationRepository,
    private val landmarks: LandmarkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ComposerUiState(locationStatus = LocationAcquisitionState.FETCHING, filteredPlaces = emptyList())
    )
    val uiState: StateFlow<ComposerUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ComposerEffect>(Channel.BUFFERED)
    val effects: Flow<ComposerEffect> = _effects.receiveAsFlow()

    /** Toạ độ GPS gốc, không đưa vào UiState (không cần hiện, tránh lộ qua log/preview). */
    private var deviceLocation: DeviceLocation? = null
    private var placeSearchJob: Job? = null
    private var landmarkJob: Job? = null

    init {
        loadLocation()
    }

    private fun loadLocation() {
        viewModelScope.launch {
            val loc = location.currentLocation().getOrNull()
            val region = loc?.let { location.resolveRegion(it.lat, it.lng).getOrNull() }
            if (loc == null || region == null) {
                // Không có vị trí: chỉ đăng được ở mức "Việt Nam" (ảnh địa danh vẫn lấy vị trí của địa danh)
                _uiState.update {
                    it.copy(
                        locationStatus = LocationAcquisitionState.UNAVAILABLE,
                        locationLevel = if (it.isLandmarkMode) LocationLevel.PLACE else LocationLevel.COUNTRY
                    )
                }
                return@launch
            }
            deviceLocation = loc
            _uiState.update { state ->
                state.copy(
                    locationStatus = LocationAcquisitionState.READY,
                    currentProvinceCode = region.provinceCode,
                    currentProvinceName = region.provinceName,
                    currentWardCode = region.wardCode.orEmpty(),
                    currentWardName = region.wardName.orEmpty(),
                    // Không biết phường (chưa có key VietMap) thì lùi về mức tỉnh
                    locationLevel = if (state.isLandmarkMode) {
                        LocationLevel.PLACE
                    } else if (state.locationLevel == LocationLevel.WARD && region.wardCode == null) {
                        LocationLevel.PROVINCE
                    } else {
                        state.locationLevel
                    }
                )
            }
            searchPlaces("")
        }
    }

    fun initImagePath(path: String) {
        if (_uiState.value.imagePath.isEmpty()) {
            _uiState.update { it.copy(imagePath = path) }
        }
    }

    /**
     * Đưa ảnh lên địa danh [landmarkId] (mở từ màn Địa danh): khoá Công khai, vị trí = điểm của địa danh.
     * Rules chỉ nhận bài gắn địa danh khi là bài public có toạ độ hiển thị trùng toạ độ địa danh.
     */
    fun initLandmark(landmarkId: String?) {
        if (landmarkId.isNullOrBlank() || _uiState.value.isLandmarkMode) return
        _uiState.update {
            it.copy(isLandmarkMode = true, audience = Audience.PUBLIC, locationLevel = LocationLevel.PLACE)
        }
        landmarkJob = viewModelScope.launch {
            launch {
                landmarks.refreshLandmark(landmarkId)
                if (landmarks.observeLandmark(landmarkId).first() == null) {
                    sendEffect(ComposerEffect.ShowMessage("Địa danh này không còn"))
                }
            }
            landmarks.observeLandmark(landmarkId).collect { landmark ->
                _uiState.update { it.copy(landmark = landmark) }
            }
        }
    }

    fun onAction(action: ComposerAction) {
        when (action) {
            is ComposerAction.OnCaptionChange -> {
                val trimmed = action.text.take(POST_CAPTION_MAX_LENGTH)
                _uiState.update { it.copy(caption = trimmed) }
            }

            is ComposerAction.OnSelectAudience -> {
                if (_uiState.value.isLandmarkMode && action.audience != Audience.PUBLIC) {
                    sendEffect(ComposerEffect.ShowMessage("Ảnh ở địa danh luôn công khai"))
                    return
                }
                if (action.audience == Audience.FRIENDS || action.audience == Audience.PUBLIC) {
                    _uiState.update { it.copy(audience = action.audience) }
                }
            }

            is ComposerAction.OnSelectTemplate -> {
                _uiState.update { it.copy(selectedTemplate = action.template) }
            }

            is ComposerAction.OnSelectStamp -> {
                _uiState.update { it.copy(selectedStamp = action.stamp) }
            }

            is ComposerAction.OnSelectLocationLevel -> {
                val current = _uiState.value
                if (current.isLandmarkMode) return
                if (current.locationStatus != LocationAcquisitionState.READY &&
                    action.level != LocationLevel.COUNTRY
                ) {
                    return
                }
                if (action.level == LocationLevel.WARD && current.currentWardCode.isBlank()) return
                val openSheet = action.level == LocationLevel.PLACE && current.selectedPlace == null
                _uiState.update {
                    it.copy(
                        locationLevel = action.level,
                        isPlaceSheetOpen = openSheet
                    )
                }
            }

            ComposerAction.OnOpenPlaceSheet -> {
                _uiState.update { it.copy(isPlaceSheetOpen = true, placeSearchQuery = "") }
                searchPlaces("")
            }

            ComposerAction.OnDismissPlaceSheet -> {
                _uiState.update { it.copy(isPlaceSheetOpen = false) }
            }

            is ComposerAction.OnPlaceSearchQueryChange -> {
                _uiState.update { it.copy(placeSearchQuery = action.query) }
                searchPlaces(action.query, debounce = true)
            }

            is ComposerAction.OnSelectPlace -> selectPlace(action.place)

            is ComposerAction.OnSelectPublishDelay -> {
                _uiState.update { it.copy(publishDelay = action.delay) }
            }

            ComposerAction.OnCancelClick -> {
                if (_uiState.value.caption.isNotBlank()) {
                    _uiState.update { it.copy(showDiscardDialog = true) }
                } else {
                    sendEffect(ComposerEffect.NavigateBackToCamera)
                }
            }

            ComposerAction.OnDismissDiscardDialog -> {
                _uiState.update { it.copy(showDiscardDialog = false) }
            }

            ComposerAction.OnConfirmDiscard -> {
                _uiState.update { it.copy(showDiscardDialog = false) }
                sendEffect(ComposerEffect.NavigateBackToCamera)
            }

            ComposerAction.OnSendClick -> send()
        }
    }

    /** Tìm địa điểm quanh vị trí hiện tại (VietMap); gõ liên tục thì chỉ gọi sau khi ngừng 300 ms. */
    private fun searchPlaces(query: String, debounce: Boolean = false) {
        placeSearchJob?.cancel()
        placeSearchJob = viewModelScope.launch {
            if (debounce) delay(300)
            location.searchPlaces(query, deviceLocation).onSuccess { places ->
                _uiState.update { if (it.placeSearchQuery == query) it.copy(filteredPlaces = places) else it }
            }
        }
    }

    /** Kết quả tìm kiếm chưa có toạ độ: hỏi VietMap Place rồi mới chọn. */
    private fun selectPlace(place: Place) {
        viewModelScope.launch {
            location.placeDetail(place).fold(
                onSuccess = { detailed ->
                    _uiState.update {
                        it.copy(selectedPlace = detailed, locationLevel = LocationLevel.PLACE, isPlaceSheetOpen = false)
                    }
                },
                onFailure = { error ->
                    sendEffect(ComposerEffect.ShowMessage(error.message ?: "Không chọn được địa điểm này"))
                }
            )
        }
    }

    private fun send() {
        val state = _uiState.value
        if (!state.canSend) return
        _uiState.update { it.copy(isSubmitting = true) }

        val draft = PostDraft(
            imagePath = state.imagePath,
            caption = state.caption.trim().ifEmpty { null },
            audience = state.audience,
            location = buildPostLocation(state),
            placeId = state.selectedPlace?.id?.takeIf { state.locationLevel == LocationLevel.PLACE && !state.isLandmarkMode },
            // Hẹn giờ chỉ có nghĩa với bài public
            publishDelay = if (state.audience == Audience.PUBLIC) state.publishDelay else PublishDelay.NOW,
            // Chọn "Việt Nam" thì không lưu cả toạ độ gốc
            exactLocation = deviceLocation.takeIf { state.locationLevel != LocationLevel.COUNTRY }
        )

        viewModelScope.launch {
            posts.createPost(draft).fold(
                onSuccess = { sendEffect(ComposerEffect.PostSent) },
                onFailure = { error ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    sendEffect(ComposerEffect.ShowMessage(error.message ?: "Không gửi được bài, thử lại sau"))
                }
            )
        }
    }

    private fun buildPostLocation(state: ComposerUiState): PostLocation {
        state.landmark?.takeIf { state.isLandmarkMode }?.let { landmark ->
            // Điểm công khai của địa danh (không phải toạ độ người chụp), cùng tỉnh với địa danh
            return PostLocation(
                level = LocationLevel.PLACE,
                provinceCode = landmark.provinceCode,
                provinceName = landmark.provinceName.ifBlank { null },
                wardCode = landmark.wardCode,
                wardName = landmark.wardName,
                placeName = landmark.name,
                landmarkId = landmark.id,
                lat = landmark.lat,
                lng = landmark.lng
            )
        }
        val province = ProvinceCatalog.findByCode(state.currentProvinceCode)
        return when (state.locationLevel) {
            LocationLevel.PLACE -> {
                val place = state.selectedPlace
                PostLocation(
                    level = LocationLevel.PLACE,
                    provinceCode = state.currentProvinceCode,
                    provinceName = state.currentProvinceName,
                    wardCode = state.currentWardCode.ifBlank { null },
                    wardName = state.currentWardName.ifBlank { null },
                    placeName = place?.name,
                    landmarkId = null,
                    // Địa điểm công khai (quán, công viên…): toạ độ của địa điểm, không phải của người chụp
                    lat = place?.lat,
                    lng = place?.lng
                )
            }

            LocationLevel.WARD -> {
                val (lat, lng) = deviceLocation?.let { snapToGrid(it.lat, it.lng) } ?: (province?.lat to province?.lng)
                PostLocation(
                    level = LocationLevel.WARD,
                    provinceCode = state.currentProvinceCode,
                    provinceName = state.currentProvinceName,
                    wardCode = state.currentWardCode,
                    wardName = state.currentWardName,
                    placeName = null,
                    landmarkId = null,
                    lat = lat,
                    lng = lng
                )
            }

            LocationLevel.PROVINCE -> PostLocation(
                level = LocationLevel.PROVINCE,
                provinceCode = state.currentProvinceCode,
                provinceName = state.currentProvinceName,
                wardCode = null,
                wardName = null,
                placeName = null,
                landmarkId = null,
                // Tâm tỉnh cố định: mọi bài cùng tỉnh chung 1 điểm
                lat = province?.lat,
                lng = province?.lng
            )

            LocationLevel.COUNTRY -> PostLocation(
                level = LocationLevel.COUNTRY,
                provinceCode = null,
                provinceName = null,
                wardCode = null,
                wardName = null,
                placeName = null,
                landmarkId = null,
                lat = null,
                lng = null
            )
        }
    }

    /**
     * Mức phường: chưa có dữ liệu tâm phường/xã mới, nên dùng tâm ô lưới 0,02° (~2 km) chứa vị trí.
     * Cố định (cùng ô → cùng điểm), không cộng nhiễu ngẫu nhiên nên đăng nhiều bài cũng không lần ra được.
     */
    private fun snapToGrid(lat: Double, lng: Double): Pair<Double, Double> =
        (floor(lat / WARD_GRID) * WARD_GRID + WARD_GRID / 2) to (floor(lng / WARD_GRID) * WARD_GRID + WARD_GRID / 2)

    private fun sendEffect(effect: ComposerEffect) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }

    private companion object {
        const val WARD_GRID = 0.02
    }
}
