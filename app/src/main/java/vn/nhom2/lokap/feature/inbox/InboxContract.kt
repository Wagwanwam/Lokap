package vn.nhom2.lokap.feature.inbox

import java.time.Instant
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.*
data class InboxConversationItem(
    val conversation: Conversation,
    val displayPreview: String,
    val hasNewPostFromFriend: Boolean = false
)

data class InboxUiState(
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,
    val items: List<InboxConversationItem> = emptyList(),
    val friends: List<User> = emptyList(),
    val isNewMessageSheetOpen: Boolean = false,
    val now: Instant = SampleData.NOW
)

sealed interface InboxAction {
    data class OpenConversation(val conversationId: String) : InboxAction
    data class MarkAsRead(val conversationId: String) : InboxAction
    data object OpenNewMessageSheet : InboxAction
    data object DismissNewMessageSheet : InboxAction
    data class SelectFriendToChat(val friend: User) : InboxAction
    data object Retry : InboxAction
    data object NavigateBack : InboxAction
}

sealed interface InboxEffect {
    data class NavigateToChat(val conversationId: String) : InboxEffect
    data object NavigateBack : InboxEffect
}
