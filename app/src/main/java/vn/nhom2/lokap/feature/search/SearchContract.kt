package vn.nhom2.lokap.feature.search

import vn.nhom2.lokap.domain.model.*
enum class SearchTab {
    PEOPLE,
    PLACES
}

data class SearchUiState(
    val query: String = "",
    val selectedTab: SearchTab = SearchTab.PEOPLE,
    val recentSearches: List<String> = listOf("Cầu Rồng", "Hội An", "Trần Khánh Linh", "Đà Nẵng", "Sơn Trà"),
    val userResults: List<User> = emptyList(),
    val placeResults: List<Place> = emptyList(),
    val friendStatuses: Map<String, FriendStatus> = emptyMap(),
    val isLoading: Boolean = false
)

sealed interface SearchAction {
    data class QueryChanged(val query: String) : SearchAction
    data class SelectTab(val tab: SearchTab) : SearchAction
    data class SelectRecentSearch(val term: String) : SearchAction
    data object ClearRecentSearches : SearchAction
    data class ToggleFriendRequest(val userId: String) : SearchAction
    data class AuthorClicked(val userId: String) : SearchAction
    data class PlaceClicked(val placeId: String) : SearchAction
    data object BackClicked : SearchAction
}

sealed interface SearchEffect {
    data object NavigateBack : SearchEffect
    data class NavigateToProfile(val userId: String) : SearchEffect
    data class NavigateToPlace(val placeId: String) : SearchEffect
}
