package vn.nhom2.lokap.feature.feed

import androidx.compose.runtime.Immutable
import java.time.Instant
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.*
enum class FeedTab(val labelVi: String) {
    EXPLORE("Khám phá"),
    FRIENDS("Bạn bè")
}

const val NATIONWIDE_VIETNAM = "Việt Nam"

val POPULAR_PROVINCES = listOf(
    NATIONWIDE_VIETNAM,
    "Hà Nội",
    "Đà Nẵng",
    "Thành phố Hồ Chí Minh",
    "Huế",
    "Khánh Hòa",
    "Lâm Đồng",
    "Cần Thơ",
    "Lào Cai",
    "Hải Phòng",
    "Quảng Ninh"
)

val ALL_VIETNAM_PROVINCES = listOf(
    NATIONWIDE_VIETNAM,
    "An Giang", "Bà Rịa - Vũng Tàu", "Bắc Giang", "Bắc Kạn", "Bạc Liêu", "Bắc Ninh",
    "Bến Tre", "Bình Định", "Bình Dương", "Bình Phước", "Bình Thuận", "Cà Mau",
    "Cần Thơ", "Cao Bằng", "Đà Nẵng", "Đắk Lắk", "Đắk Nông", "Điện Biên", "Đồng Nai",
    "Đồng Tháp", "Gia Lai", "Hà Giang", "Hà Nam", "Hà Nội", "Hà Tĩnh", "Hải Dương",
    "Hải Phòng", "Hậu Giang", "Hòa Bình", "Hưng Yên", "Khánh Hòa", "Kiên Giang",
    "Kon Tum", "Lai Châu", "Lâm Đồng", "Lạng Sơn", "Lào Cai", "Long An", "Nam Định",
    "Nghệ An", "Ninh Bình", "Ninh Thuận", "Phú Thọ", "Phú Yên", "Quảng Bình",
    "Quảng Nam", "Quảng Ngãi", "Quảng Ninh", "Quảng Trị", "Sóc Trăng", "Sơn La",
    "Tây Ninh", "Thái Bình", "Thái Nguyên", "Thanh Hóa", "Thừa Thiên Huế", "Tiền Giang",
    "Thành phố Hồ Chí Minh", "Trà Vinh", "Tuyên Quang", "Vĩnh Long", "Vĩnh Phúc", "Yên Bái"
)

@Immutable
data class FeedUiState(
    val selectedTab: FeedTab = FeedTab.EXPLORE,
    val friendPosts: List<Post> = emptyList(),
    val explorePosts: List<Post> = emptyList(),
    val allPublicPosts: List<Post> = emptyList(),
    val nearbyPosts: List<Post> = emptyList(),
    val landmarkPosts: List<Post> = emptyList(),
    val unreadNotificationsCount: Int = SampleData.notifications.count { !it.isRead },
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,
    val currentProvinceName: String = NATIONWIDE_VIETNAM,
    val locationSearchQuery: String = "",
    val isProvincePickerOpen: Boolean = false,
    val provinceFilterSearch: String = "",
    val now: Instant = SampleData.NOW,
    val currentUserId: String = SampleData.currentUser.id,
    val pendingBlock: BlockTarget? = null,
    val repostedPostIds: Set<String> = emptySet(),
    val commentsCountMap: Map<String, Int> = emptyMap(),
    val activeCommentPostId: String? = null,
    val activeReactionPickerPostId: String? = null,
    val searchQuery: String = "",
    val searchResults: List<User> = emptyList(),
    val friendStatuses: Map<String, FriendStatus> = emptyMap()
)

@Immutable
data class BlockTarget(val userId: String, val displayName: String)

sealed interface FeedAction {
    data class OnSelectTab(val tab: FeedTab) : FeedAction
    data object OnRefresh : FeedAction
    data object OnRetry : FeedAction
    data object OnSwipeDownToCamera : FeedAction
    data object OnNotificationsClick : FeedAction
    data object OnSearchClick : FeedAction
    data class OnPostClick(val postId: String) : FeedAction
    data class OnLocationChipClick(val post: Post) : FeedAction
    data class OnAuthorClick(val userId: String) : FeedAction
    data class OnReplyClick(val post: Post) : FeedAction
    data class OnReact(val postId: String, val emoji: String) : FeedAction
    data class OnRetrySendPost(val postId: String) : FeedAction
    data class OnReportPost(val postId: String) : FeedAction
    data class OnBlockUser(val userId: String, val displayName: String) : FeedAction
    data object OnConfirmBlock : FeedAction
    data object OnDismissBlockDialog : FeedAction

    data class OnToggleRepost(val postId: String) : FeedAction
    data class OnOpenComments(val postId: String) : FeedAction
    data object OnDismissComments : FeedAction
    data class OnAddComment(val postId: String, val text: String) : FeedAction
    data class OnShareLink(val post: Post) : FeedAction
    data class OnShowReactionPicker(val postId: String) : FeedAction
    data object OnDismissReactionPicker : FeedAction

    data class OnSearchQueryChange(val query: String) : FeedAction
    data class OnToggleFriendRequest(val userId: String) : FeedAction

    data class OnSelectProvince(val provinceName: String) : FeedAction
    data class OnLocationSearchQueryChange(val query: String) : FeedAction
    data object OnClearLocationSearch : FeedAction
    data object OnOpenProvincePicker : FeedAction
    data object OnDismissProvincePicker : FeedAction
    data class OnProvinceFilterSearchChange(val query: String) : FeedAction
}

sealed interface FeedEffect {
    data object NavigateBackToCamera : FeedEffect
    data object NavigateToNotifications : FeedEffect
    data object NavigateToSearch : FeedEffect
    data class NavigateToPostDetail(val postId: String) : FeedEffect
    data class NavigateToRegionAlbum(val level: String, val code: String) : FeedEffect
    data class NavigateToProfile(val userId: String) : FeedEffect
    data class NavigateToChat(val conversationId: String) : FeedEffect
    data class ShowMessage(val message: String) : FeedEffect
}
