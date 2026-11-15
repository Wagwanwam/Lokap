package vn.nhom2.lokap.feature.addfriend

import androidx.compose.runtime.Immutable
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.model.User

/** Kết quả mở mã QR / link mời, hiện trong bottom sheet. */
@Immutable
sealed interface FriendLookup {
    val username: String

    data class Loading(override val username: String) : FriendLookup

    data class Found(
        val user: User,
        val status: FriendStatus,
        val isSelf: Boolean,
        val isWorking: Boolean = false
    ) : FriendLookup {
        override val username: String get() = user.username
    }

    data class NotFound(override val username: String) : FriendLookup

    data class Failed(override val username: String, val message: String) : FriendLookup
}

@Immutable
data class AddFriendUiState(
    val me: User? = null,
    val friendCount: Int = 0,
    val maxFriends: Int = 20,
    val lookup: FriendLookup? = null
) {
    val isFriendLimitReached: Boolean
        get() = friendCount >= maxFriends

    /** Chưa có username (chưa thiết lập hồ sơ) thì không tạo được mã. */
    val hasCode: Boolean
        get() = !me?.username.isNullOrBlank()
}

sealed interface AddFriendAction {
    data object NavigateBack : AddFriendAction
    data object OpenScanner : AddFriendAction
    data object ShareInvite : AddFriendAction
    data object CopyLink : AddFriendAction
    /** Username đã đọc từ QR hoặc link mời. */
    data class LookUp(val username: String) : AddFriendAction
    data object RetryLookup : AddFriendAction
    data object DismissLookup : AddFriendAction
    data object SendRequest : AddFriendAction
    data object CancelRequest : AddFriendAction
    data object AcceptRequest : AddFriendAction
    data class OpenProfile(val userId: String) : AddFriendAction
}

sealed interface AddFriendEffect {
    data object NavigateBack : AddFriendEffect
    data object OpenScanner : AddFriendEffect
    data class ShareInvite(val text: String) : AddFriendEffect
    data class CopyLink(val link: String) : AddFriendEffect
    data class NavigateToProfile(val userId: String) : AddFriendEffect
    data class ShowMessage(val message: String) : AddFriendEffect
}
