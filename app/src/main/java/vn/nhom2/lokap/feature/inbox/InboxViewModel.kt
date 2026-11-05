package vn.nhom2.lokap.feature.inbox

import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.data.fake.SampleSocialData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.data.fake.FakeChatStore
import vn.nhom2.lokap.data.fake.FakePostStore
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.User

class InboxViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(InboxUiState(isLoading = true))
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    private val _effects = Channel<InboxEffect>(Channel.BUFFERED)
    val effects: Flow<InboxEffect> = _effects.receiveAsFlow()

    init {
        observeInboxData()
    }

    private fun observeInboxData() {
        viewModelScope.launch {
            combine(
                FakeChatStore.conversations,
                FakeChatStore.messages,
                FakePostStore.posts,
                SampleSocialData.friendStatuses
            ) { conversations, messages, posts, statuses ->
                val myId = SampleData.currentUser.id
                val authorIdsWithPosts = posts.map { it.author.id }.toSet()

                val sortedItems = conversations
                    .sortedByDescending { it.lastMessageAt }
                    .map { conv ->
                        val preview = FakeChatStore.previewForConversation(conv, messages)
                        InboxConversationItem(
                            conversation = conv,
                            displayPreview = preview.ifBlank { "Bắt đầu cuộc trò chuyện 👋" },
                            hasNewPostFromFriend = conv.friend.id in authorIdsWithPosts
                        )
                    }

                // Chỉ bạn bè hiện tại mới bắt đầu chat được (nhắn tin chỉ giữa bạn bè)
                val availableFriends = SampleSocialData.directoryUsers.filter {
                    it.id != myId && statuses[it.id] == FriendStatus.FRIENDS
                }
                sortedItems to availableFriends
            }.collect { (sortedItems, availableFriends) ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = null,
                        items = sortedItems,
                        friends = availableFriends,
                        now = SampleData.NOW
                    )
                }
            }
        }
    }

    fun onAction(action: InboxAction) {
        when (action) {
            is InboxAction.OpenConversation -> {
                FakeChatStore.markConversationRead(action.conversationId)
                sendEffect(InboxEffect.NavigateToChat(action.conversationId))
            }

            is InboxAction.MarkAsRead -> {
                FakeChatStore.markConversationRead(action.conversationId)
            }

            InboxAction.OpenNewMessageSheet -> {
                _uiState.update { it.copy(isNewMessageSheetOpen = true) }
            }

            InboxAction.DismissNewMessageSheet -> {
                _uiState.update { it.copy(isNewMessageSheetOpen = false) }
            }

            is InboxAction.SelectFriendToChat -> {
                handleSelectFriend(action.friend)
            }

            InboxAction.Retry -> {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                observeInboxData()
            }

            InboxAction.NavigateBack -> {
                sendEffect(InboxEffect.NavigateBack)
            }
        }
    }

    private fun handleSelectFriend(friend: User) {
        _uiState.update { it.copy(isNewMessageSheetOpen = false) }
        val conversation = FakeChatStore.getOrCreateConversationWithFriend(friend)
        sendEffect(InboxEffect.NavigateToChat(conversation.id))
    }

    private fun sendEffect(effect: InboxEffect) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }
}
