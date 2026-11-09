package vn.nhom2.lokap.feature.friends

import androidx.compose.runtime.Immutable
import vn.nhom2.lokap.domain.model.*
@Immutable
data class FriendUserItemUi(
    val user: User,
    val status: FriendStatus
)

enum class FriendConfirmActionType {
    UNFRIEND,
    BLOCK
}

@Immutable
data class PendingFriendConfirmation(
    val user: User,
    val actionType: FriendConfirmActionType
)

@Immutable
data class FriendsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isOffline: Boolean = false,
    val searchQuery: String = "",
    val debouncedQuery: String = "",
    val searchResults: List<FriendUserItemUi> = emptyList(),
    val incomingRequests: List<User> = emptyList(),
    val sentRequests: List<User> = emptyList(),
    val friends: List<User> = emptyList(),
    val maxFriends: Int = 20,
    val pendingConfirmation: PendingFriendConfirmation? = null
) {
    val isFriendLimitReached: Boolean
        get() = friends.size >= maxFriends

    val isSearching: Boolean
        get() = debouncedQuery.isNotBlank()
}

sealed interface FriendsAction {
    data object Retry : FriendsAction
    data object NavigateBack : FriendsAction
    data class ChangeSearchQuery(val query: String) : FriendsAction
    data object ClearSearch : FriendsAction
    data class OpenProfile(val userId: String) : FriendsAction

    data class SendRequest(val userId: String) : FriendsAction
    data class CancelSentRequest(val userId: String) : FriendsAction
    data class AcceptRequest(val userId: String) : FriendsAction
    data class DeclineRequest(val userId: String) : FriendsAction
    data class UnblockUser(val userId: String) : FriendsAction

    data class RequestUnfriendConfirm(val user: User) : FriendsAction
    data class RequestBlockConfirm(val user: User) : FriendsAction
    data object DismissConfirmDialog : FriendsAction
    data object ConfirmPendingAction : FriendsAction
}

sealed interface FriendsEffect {
    data object NavigateBack : FriendsEffect
    data class NavigateToProfile(val userId: String) : FriendsEffect
    data class ShowMessage(val message: String) : FriendsEffect
}
