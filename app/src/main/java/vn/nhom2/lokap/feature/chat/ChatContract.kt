package vn.nhom2.lokap.feature.chat

import java.time.Instant
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.Message
import vn.nhom2.lokap.domain.model.User

sealed interface ChatTimelineItem {
    val key: String

    data class MessageNode(
        val message: Message,
        val isMine: Boolean,
        val isLatestMyMessage: Boolean
    ) : ChatTimelineItem {
        override val key: String = "msg_${message.id}"
    }

    data class DateHeader(
        val id: String,
        val label: String
    ) : ChatTimelineItem {
        override val key: String = "date_$id"
    }
}

data class ChatUiState(
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,
    val conversationId: String = "",
    val friend: User? = null,
    val isFriendStatusValid: Boolean = true,
    val timelineItems: List<ChatTimelineItem> = emptyList(),
    val inputText: String = "",
    val isFriendTyping: Boolean = false,
    val now: Instant = SampleData.NOW
) {
    val canSend: Boolean
        get() = isFriendStatusValid && inputText.trim().isNotEmpty() && inputText.length <= 1000
}

sealed interface ChatAction {
    data class InputChanged(val text: String) : ChatAction
    data object SendMessage : ChatAction
    data class RetryMessage(val messageId: String) : ChatAction
    data object OpenCameraForDirectShot : ChatAction
    data class OpenPostDetail(val postId: String) : ChatAction
    data object OpenFriendProfile : ChatAction
    data object RetryLoad : ChatAction
    data object NavigateBack : ChatAction
}

sealed interface ChatEffect {
    data object NavigateBack : ChatEffect
    data object NavigateToCamera : ChatEffect
    data class NavigateToPostDetail(val postId: String) : ChatEffect
    data class NavigateToProfile(val userId: String) : ChatEffect
}
