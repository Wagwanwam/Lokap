package vn.nhom2.lokap.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleData2
import vn.nhom2.lokap.domain.model.FriendStatus
class SearchViewModel : ViewModel() {

    private val allUsers = SampleData.users.filter { it.id != SampleData.currentUser.id }
    private val allPlaces = SampleData2.daNangPlaces

    private val _uiState = MutableStateFlow(
        SearchUiState(
            userResults = allUsers,
            placeResults = allPlaces
        )
    )
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _effect = Channel<SearchEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.QueryChanged -> {
                val q = action.query
                _uiState.update { state ->
                    state.copy(
                        query = q,
                        userResults = filterUsers(q),
                        placeResults = filterPlaces(q)
                    )
                }
            }

            is SearchAction.SelectTab -> {
                _uiState.update { it.copy(selectedTab = action.tab) }
            }

            is SearchAction.SelectRecentSearch -> {
                val q = action.term
                _uiState.update { state ->
                    state.copy(
                        query = q,
                        userResults = filterUsers(q),
                        placeResults = filterPlaces(q)
                    )
                }
            }

            SearchAction.ClearRecentSearches -> {
                _uiState.update { it.copy(recentSearches = emptyList()) }
            }

            is SearchAction.ToggleFriendRequest -> {
                _uiState.update { state ->
                    val current = state.friendStatuses[action.userId] ?: FriendStatus.NONE
                    val next = when (current) {
                        FriendStatus.NONE -> FriendStatus.REQUEST_SENT
                        FriendStatus.REQUEST_SENT -> FriendStatus.NONE
                        FriendStatus.FRIENDS -> FriendStatus.NONE
                        FriendStatus.REQUEST_RECEIVED -> FriendStatus.FRIENDS
                        FriendStatus.BLOCKED -> FriendStatus.BLOCKED
                    }
                    state.copy(friendStatuses = state.friendStatuses + (action.userId to next))
                }
            }

            is SearchAction.AuthorClicked -> {
                viewModelScope.launch {
                    _effect.send(SearchEffect.NavigateToProfile(action.userId))
                }
            }

            is SearchAction.PlaceClicked -> {
                viewModelScope.launch {
                    _effect.send(SearchEffect.NavigateToPlace(action.placeId))
                }
            }

            SearchAction.BackClicked -> {
                viewModelScope.launch {
                    _effect.send(SearchEffect.NavigateBack)
                }
            }
        }
    }

    private fun filterUsers(query: String) = if (query.isBlank()) {
        allUsers
    } else {
        val q = query.trim().lowercase().removePrefix("@")
        allUsers.filter {
            it.displayName.lowercase().contains(q) || it.username.lowercase().contains(q)
        }
    }

    private fun filterPlaces(query: String) = if (query.isBlank()) {
        allPlaces
    } else {
        val q = query.trim().lowercase()
        allPlaces.filter {
            it.name.lowercase().contains(q) || it.address.lowercase().contains(q)
        }
    }
}
