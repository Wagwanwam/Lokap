package vn.nhom2.lokap.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.domain.repository.LocationRepository
import kotlinx.coroutines.flow.collectLatest
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.repository.SocialRepository

/**
 * Tìm kiếm: người qua SocialRepository (chưa gõ = gợi ý, gõ = hỏi server sau 300 ms ngừng gõ);
 * địa điểm lấy qua LocationRepository.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val social: SocialRepository,
    private val location: LocationRepository
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")

    private val _uiState = MutableStateFlow(SearchUiState(isLoading = true))
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _effect = Channel<SearchEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            social.graph.collect { graph ->
                _uiState.update { it.copy(friendStatuses = graph.statuses) }
                // Gợi ý suy ra từ graph (bản Firestore): danh sách bạn về muộn thì nạp lại
                if (_uiState.value.query.isBlank()) searchUsers("")
            }
        }
        viewModelScope.launch {
            queryFlow
                .map { it.trim() }
                .distinctUntilChanged()
                .debounce { query -> if (query.isBlank()) 0L else 300L }
                .collectLatest { query ->
                    searchUsers(query)
                    searchPlaces(query)
                }
        }
    }

    private suspend fun searchUsers(query: String) {
        _uiState.update { it.copy(isLoading = true) }
        val result = if (query.isBlank()) social.suggestions() else social.search(query)
        _uiState.update { state ->
            // Kết quả về muộn của chuỗi cũ thì bỏ qua
            if (state.query.trim() != query) return@update state.copy(isLoading = false)
            result.fold(
                onSuccess = { users -> state.copy(userResults = users, isLoading = false) },
                onFailure = { state.copy(isLoading = false) }
            )
        }
        result.onFailure { error ->
            if (query.isNotBlank()) _effect.send(SearchEffect.ShowMessage(error.message ?: "Không tìm được, thử lại sau"))
        }
    }

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.QueryChanged -> setQuery(action.query)

            is SearchAction.SelectTab -> {
                _uiState.update { it.copy(selectedTab = action.tab) }
            }

            is SearchAction.SelectRecentSearch -> setQuery(action.term)

            SearchAction.ClearRecentSearches -> {
                _uiState.update { it.copy(recentSearches = emptyList()) }
            }

            is SearchAction.ToggleFriendRequest -> viewModelScope.launch {
                val message: Result<String> = when (social.graph.value.statusOf(action.userId)) {
                    FriendStatus.NONE -> social.sendFriendRequest(action.userId).map { status ->
                        if (status == FriendStatus.FRIENDS) "Hai bạn đã trở thành bạn bè!" else "Đã gửi lời mời kết bạn"
                    }
                    FriendStatus.REQUEST_SENT -> social.cancelFriendRequest(action.userId).map { "Đã huỷ lời mời kết bạn" }
                    FriendStatus.REQUEST_RECEIVED -> social.acceptFriendRequest(action.userId).map { "Hai bạn đã trở thành bạn bè!" }
                    FriendStatus.FRIENDS -> social.unfriend(action.userId).map { "Đã huỷ kết bạn" }
                    FriendStatus.BLOCKED -> return@launch
                }
                // Trạng thái mới tự về qua graph; lỗi (đủ 20 bạn, mất mạng…) báo đúng câu của server
                message.onFailure { _effect.send(SearchEffect.ShowMessage(it.message ?: "Không thực hiện được")) }
            }

            is SearchAction.AuthorClicked -> {
                viewModelScope.launch {
                    _effect.send(SearchEffect.NavigateToProfile(action.userId))
                }
            }

            is SearchAction.PlaceClicked -> {
                viewModelScope.launch {
                    val place = _uiState.value.placeResults.firstOrNull { it.id == action.placeId } ?: return@launch
                    location.placeDetail(place).fold(
                        onSuccess = { _effect.send(SearchEffect.NavigateToPlace(it)) },
                        onFailure = { _effect.send(SearchEffect.ShowMessage(it.message ?: "Không mở được địa điểm")) }
                    )
                }
            }

            SearchAction.BackClicked -> {
                viewModelScope.launch {
                    _effect.send(SearchEffect.NavigateBack)
                }
            }
        }
    }

    private fun setQuery(query: String) {
        queryFlow.value = query
        _uiState.update { it.copy(query = query, placeResults = emptyList()) }
    }

    private suspend fun searchPlaces(query: String) {
        if (query.isBlank()) return
        location.searchPlaces(query, near = null).fold(
            onSuccess = { places ->
                _uiState.update { if (it.query.trim() == query) it.copy(placeResults = places) else it }
            },
            onFailure = { error ->
                if (_uiState.value.selectedTab == SearchTab.PLACES && _uiState.value.query.trim() == query) {
                    _effect.send(SearchEffect.ShowMessage(error.message ?: "Không tìm được địa điểm"))
                }
            }
        )
    }
}
