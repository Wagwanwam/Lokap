package vn.nhom2.lokap.feature.feed

import androidx.compose.runtime.Immutable
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.Post
import java.time.Instant

enum class FeedTab(val labelVi: String) {
    FRIENDS("Bạn bè"),
    EXPLORE("Khám phá")
}

@Immutable
data class FeedUiState(
    val selectedTab: FeedTab = FeedTab.FRIENDS,
    val friendPosts: List<Post> = emptyList(),
    val nearbyPosts: List<Post> = emptyList(),
    val landmarkPosts: List<Post> = emptyList(),
    val unreadNotificationsCount: Int = SampleData.notifications.count { !it.isRead },
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,
    val currentProvinceName: String = "Đà Nẵng",
    val now: Instant = SampleData.NOW,
    val currentUserId: String = SampleData.currentUser.id,
    /** Người đang chờ xác nhận chặn; khác null thì hiện hộp xác nhận. */
    val pendingBlock: BlockTarget? = null
)

@Immutable
data class BlockTarget(val userId: String, val displayName: String)

sealed interface FeedAction {
    data class OnSelectTab(val tab: FeedTab) : FeedAction
    data object OnRefresh : FeedAction
    data object OnRetry : FeedAction
    data object OnSwipeDownToCamera : FeedAction
    data object OnNotificationsClick : FeedAction
    data class OnPostClick(val postId: String) : FeedAction
    data class OnLocationChipClick(val post: Post) : FeedAction
    data class OnAuthorClick(val userId: String) : FeedAction
    data class OnReplyClick(val post: Post) : FeedAction
    data class OnReact(val postId: String, val emoji: String) : FeedAction
    data class OnRetrySendPost(val postId: String) : FeedAction
    data class OnReportPost(val postId: String) : FeedAction
    /** Bấm "Chặn" trong menu: chỉ mở hộp xác nhận. */
    data class OnBlockUser(val userId: String, val displayName: String) : FeedAction
    data object OnConfirmBlock : FeedAction
    data object OnDismissBlockDialog : FeedAction
}

sealed interface FeedEffect {
    data object NavigateBackToCamera : FeedEffect
    data object NavigateToNotifications : FeedEffect
    data class NavigateToPostDetail(val postId: String) : FeedEffect
    data class NavigateToRegionAlbum(val level: String, val code: String) : FeedEffect
    data class NavigateToProfile(val userId: String) : FeedEffect
    data class NavigateToChat(val conversationId: String) : FeedEffect
    data class ShowMessage(val message: String) : FeedEffect
}
