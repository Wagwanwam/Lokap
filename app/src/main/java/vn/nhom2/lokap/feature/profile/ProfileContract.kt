package vn.nhom2.lokap.feature.profile

import androidx.compose.runtime.Immutable
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.model.Landmark
import vn.nhom2.lokap.domain.model.MapBubble
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.model.User
import vn.nhom2.lokap.feature.map.MapCameraState

enum class ProfileTab(val titleVi: String) {
    PHOTOS("Ảnh"),
    FOOTPRINTS("Dấu chân")
}

@Immutable
data class VisitedProvinceUi(
    val code: String,
    val name: String,
    val postCount: Int,
    val photoUrls: List<String>,
    val lat: Double,
    val lng: Double
)

@Immutable
data class ProfileUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isUserNotFound: Boolean = false,
    val isOffline: Boolean = false,
    val isMe: Boolean = true,
    val user: User? = null,
    val friendStatus: FriendStatus = FriendStatus.NONE,
    val visitedProvincesCount: Int = 0,
    val totalProvincesCount: Int = 34,
    val postCount: Int = 0,
    val friendCount: Int = 0,
    val isFriendLimitReached: Boolean = false,
    val selectedTab: ProfileTab = ProfileTab.PHOTOS,
    val visiblePosts: List<Post> = emptyList(),
    val visitedProvinces: List<VisitedProvinceUi> = emptyList(),
    val mapBubbles: List<MapBubble> = emptyList(),
    val landmarks: List<Landmark> = emptyList(),
    val mapCamera: MapCameraState = MapCameraState(
        centerLat = 16.047079,
        centerLng = 108.206230,
        zoom = 5.3f
    ),
    val isEditSheetOpen: Boolean = false,
    val editDisplayNameInput: String = "",
    val editDisplayNameError: String? = null,
    val isSavingProfile: Boolean = false
)

sealed interface ProfileAction {
    data object Retry : ProfileAction
    data object NavigateBack : ProfileAction
    data object OpenSettings : ProfileAction
    data class SelectTab(val tab: ProfileTab) : ProfileAction
    data class ClickPost(val postId: String) : ProfileAction
    data class ClickProvinceCard(val provinceCode: String) : ProfileAction
    data class ClickMapBubble(val bubble: MapBubble) : ProfileAction
    data class UpdateMapCamera(val camera: MapCameraState) : ProfileAction

    // Hồ sơ của tôi
    data object OpenEditProfileSheet : ProfileAction
    data object DismissEditProfileSheet : ProfileAction
    data class ChangeEditDisplayName(val value: String) : ProfileAction
    data object SaveEditProfile : ProfileAction

    // Hồ sơ người khác
    data object SendFriendRequest : ProfileAction
    data object CancelFriendRequest : ProfileAction
    data object AcceptFriendRequest : ProfileAction
    data object DeclineFriendRequest : ProfileAction
    data object Unfriend : ProfileAction
    data object Unblock : ProfileAction
    data object MessageUser : ProfileAction
}

sealed interface ProfileEffect {
    data object NavigateBack : ProfileEffect
    data object NavigateToSettings : ProfileEffect
    data class NavigateToPostDetail(val postId: String) : ProfileEffect
    data class NavigateToChat(val conversationId: String) : ProfileEffect
    data class NavigateToRegionAlbum(val provinceCode: String) : ProfileEffect
    data class ShowMessage(val message: String) : ProfileEffect
}
