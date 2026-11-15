package vn.nhom2.lokap.feature.landmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.domain.model.Place
import vn.nhom2.lokap.domain.repository.DeviceLocation
import vn.nhom2.lokap.domain.repository.LandmarkDraft
import vn.nhom2.lokap.domain.repository.LandmarkRepository
import vn.nhom2.lokap.domain.repository.LandmarkRepository.Companion.DESCRIPTION_MAX_LENGTH
import vn.nhom2.lokap.domain.repository.LandmarkRepository.Companion.NAME_MAX_LENGTH
import vn.nhom2.lokap.domain.repository.LocationRepository

/**
 * Thêm địa danh từ địa điểm VietMap. Toạ độ là của địa điểm công khai (không phải của người thêm);
 * tỉnh/phường lấy bằng VietMap Reverse để địa danh hiện đúng tỉnh trên bản đồ.
 */
@HiltViewModel
class AddLandmarkViewModel @Inject constructor(
    private val location: LocationRepository,
    private val landmarks: LandmarkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddLandmarkUiState())
    val uiState: StateFlow<AddLandmarkUiState> = _uiState.asStateFlow()

    private val _effect = Channel<AddLandmarkEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var searchJob: Job? = null
    /** Vị trí để ưu tiên kết quả gần; chỉ dùng trong lời gọi tìm kiếm, không lưu. */
    private var near: DeviceLocation? = null
    private var askedLocation = false

    /** Mở lại bảng: bắt đầu từ bước tìm kiếm. */
    fun reset() {
        searchJob?.cancel()
        _uiState.value = AddLandmarkUiState()
    }

    fun onAction(action: AddLandmarkAction) {
        when (action) {
            is AddLandmarkAction.OnQueryChange -> search(action.query)
            is AddLandmarkAction.OnPickPlace -> pick(action.place)
            AddLandmarkAction.OnBackToSearch -> _uiState.update {
                it.copy(selected = null, region = null, name = "", description = "")
            }
            is AddLandmarkAction.OnNameChange -> _uiState.update { it.copy(name = action.name.take(NAME_MAX_LENGTH)) }
            is AddLandmarkAction.OnDescriptionChange -> _uiState.update {
                it.copy(description = action.description.take(DESCRIPTION_MAX_LENGTH))
            }
            AddLandmarkAction.OnSave -> save()
        }
    }

    private fun search(query: String) {
        _uiState.update { it.copy(query = query, searchError = null) }
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _uiState.update { it.copy(results = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _uiState.update { it.copy(isSearching = true) }
            if (!askedLocation) {
                askedLocation = true
                near = location.currentLocation().getOrNull()
            }
            location.searchPlaces(query, near).fold(
                onSuccess = { places ->
                    _uiState.update { if (it.query == query) it.copy(results = places, isSearching = false) else it }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isSearching = false, searchError = error.message ?: "Không tìm được") }
                }
            )
        }
    }

    private fun pick(place: Place) {
        if (_uiState.value.isResolving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isResolving = true) }
            val detailed = location.placeDetail(place).getOrElse { error ->
                _uiState.update { it.copy(isResolving = false) }
                _effect.send(AddLandmarkEffect.ShowMessage(error.message ?: "Không lấy được vị trí của địa điểm này"))
                return@launch
            }
            val region = location.resolveRegion(detailed.lat, detailed.lng).getOrElse { error ->
                _uiState.update { it.copy(isResolving = false) }
                _effect.send(AddLandmarkEffect.ShowMessage(error.message ?: "Không xác định được tỉnh của địa điểm"))
                return@launch
            }
            _uiState.update {
                it.copy(
                    isResolving = false,
                    selected = detailed,
                    region = region,
                    name = detailed.name.take(NAME_MAX_LENGTH),
                    description = ""
                )
            }
        }
    }

    private fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        val place = state.selected ?: return
        val region = state.region ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            landmarks.createLandmark(
                LandmarkDraft(
                    place = place,
                    name = state.name,
                    description = state.description.ifBlank { null },
                    region = region
                )
            ).fold(
                onSuccess = { landmark ->
                    _uiState.update { it.copy(isSaving = false) }
                    _effect.send(AddLandmarkEffect.OpenLandmark(landmark.id))
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isSaving = false) }
                    _effect.send(AddLandmarkEffect.ShowMessage(error.message ?: "Không thêm được địa danh"))
                }
            )
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
