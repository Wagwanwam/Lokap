package vn.nhom2.lokap.domain.model

import androidx.compose.runtime.Immutable
@Immutable
data class User(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val bio: String = ""
)

enum class FriendStatus {
    NONE,
    REQUEST_SENT,
    REQUEST_RECEIVED,
    FRIENDS,
    BLOCKED
}
