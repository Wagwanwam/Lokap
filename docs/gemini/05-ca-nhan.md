```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/data/fake/SampleSocialData.kt
package vn.nhom2.lokap.data.fake

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.MapBubble
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.model.Province
import vn.nhom2.lokap.domain.model.User
import java.time.Instant

data class AppSettingsData(
    val defaultLocationLevel: LocationLevel = LocationLevel.PROVINCE,
    val notifyNewPhotos: Boolean = true,
    val notifyReactions: Boolean = true,
    val notifyMessages: Boolean = true,
    val notifyFriendRequests: Boolean = true
)

object SampleSocialData {
    const val MAX_FRIENDS = 20

    private val _currentUserOverride = MutableStateFlow<User?>(null)
    val currentUserOverride: StateFlow<User?> = _currentUserOverride.asStateFlow()

    val directoryUsers: List<User> by lazy {
        val existingIds = SampleData.users.map { it.id }.toSet()
        val extraUsers = listOf(
            User(
                id = "u_genz_01",
                username = "linh.chill",
                displayName = "Linh Đà Lạt",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300"
            ),
            User(
                id = "u_genz_02",
                username = "khoi.langthang",
                displayName = "Khôi Đi Phượt",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300"
            ),
            User(
                id = "u_genz_03",
                username = "my.boba",
                displayName = "Trà My Tiệm Trà Chanh",
                avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=300"
            ),
            User(
                id = "u_genz_04",
                username = "nam.skateboard",
                displayName = "Nam Lướt Ván",
                avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300"
            ),
            User(
                id = "u_genz_05",
                username = "vy.matcha",
                displayName = "Thảo Vy Matcha",
                avatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=300"
            ),
            User(
                id = "u_genz_06",
                username = "hieu.film",
                displayName = "Hiếu Chụp Film",
                avatarUrl = null
            ),
            User(
                id = "u_genz_07",
                username = "an. toxic",
                displayName = "An Hay Quạo",
                avatarUrl = null
            )
        ).filter { it.id !in existingIds }

        (SampleData.users + extraUsers).distinctBy { it.id }
    }

    private val _friendStatuses = MutableStateFlow>(buildInitialStatuses())
    val friendStatuses: StateFlow> = _friendStatuses.asStateFlow()

    private val _settings = MutableStateFlow(AppSettingsData())
    val settings: StateFlow<AppSettingsData> = _settings.asStateFlow()

    private fun buildInitialStatuses(): Map<String, FriendStatus> {
        val myId = SampleData.currentUser.id
        val others = directoryUsers.filter { it.id != myId }
        val result = mutableMapOf<String, FriendStatus>()

        others.forEachIndexed { index, user ->
            result[user.id] = when (index) {
                0, 1, 2, 3 -> FriendStatus.FRIENDS
                4, 5 -> FriendStatus.REQUEST_RECEIVED
                6 -> FriendStatus.REQUEST_SENT
                7 -> FriendStatus.BLOCKED
                else -> FriendStatus.NONE
            }
        }
        return result
    }

    fun getCurrentUser(): User {
        return _currentUserOverride.value ?: SampleData.currentUser
    }

    fun findUser(userId: String?): User? {
        val current = getCurrentUser()
        if (userId == null || userId == current.id) return current
        return directoryUsers.find { it.id == userId }?.let { found ->
            if (found.id == current.id) current else found
        }
    }

    fun updateMyDisplayName(newDisplayName: String) {
        val current = getCurrentUser()
        _currentUserOverride.value = current.copy(displayName = newDisplayName.trim())
    }

    fun getFriendStatus(userId: String): FriendStatus {
        if (userId == getCurrentUser().id) return FriendStatus.NONE
        return _friendStatuses.value[userId] ?: FriendStatus.NONE
    }

    fun getFriendsCount(userId: String): Int {
        if (userId == getCurrentUser().id) {
            return _friendStatuses.value.values.count { it == FriendStatus.FRIENDS }
        }
        return ((userId.hashCode() and 0x7FFFFFFF) % 16) + 2
    }

    fun sendFriendRequest(userId: String): Boolean {
        val currentFriends = _friendStatuses.value.values.count { it == FriendStatus.FRIENDS }
        if (currentFriends >= MAX_FRIENDS) return false
        _friendStatuses.update { it + (userId to FriendStatus.REQUEST_SENT) }
        return true
    }

    fun cancelFriendRequest(userId: String) {
        _friendStatuses.update { it + (userId to FriendStatus.NONE) }
    }

    fun acceptFriendRequest(userId: String): Boolean {
        val currentFriends = _friendStatuses.value.values.count { it == FriendStatus.FRIENDS }
        if (currentFriends >= MAX_FRIENDS) return false
        _friendStatuses.update { it + (userId to FriendStatus.FRIENDS) }
        return true
    }

    fun declineFriendRequest(userId: String) {
        _friendStatuses.update { it + (userId to FriendStatus.NONE) }
    }

    fun unfriend(userId: String) {
        _friendStatuses.update { it + (userId to FriendStatus.NONE) }
    }

    fun blockUser(userId: String) {
        _friendStatuses.update { it + (userId to FriendStatus.BLOCKED) }
    }

    fun unblockUser(userId: String) {
        _friendStatuses.update { it + (userId to FriendStatus.NONE) }
    }

    fun updateSettings(transform: (AppSettingsData) -> AppSettingsData) {
        _settings.update(transform)
    }

    fun resetAccountForDemo() {
        _currentUserOverride.value = null
        _friendStatuses.value = buildInitialStatuses()
        _settings.value = AppSettingsData()
    }

    fun conversationIdFor(otherUserId: String): String {
        val myId = getCurrentUser().id
        return if (myId < otherUserId) "conv_${myId}_$otherUserId" else "conv_${otherUserId}_$myId"
    }

    fun isPostVisibleToViewer(
        post: Post,
        isMe: Boolean,
        friendStatus: FriendStatus,
        now: Instant = SampleData.NOW
    ): Boolean {
        if (isMe) return true
        if (friendStatus == FriendStatus.BLOCKED) return false
        val audienceName = post.audience.toString().uppercase()
        return when {
            audienceName.contains("FRIEND") -> friendStatus == FriendStatus.FRIENDS
            audienceName.contains("PUBLIC") -> {
                val visibleAt = post.publicVisibleAt
                visibleAt == null || !visibleAt.isAfter(now)
            }
            else -> false
        }
    }

    fun buildProvinceMapBubbles(
        posts: List<Post>,
        provinces: List<Province>
    ): List<MapBubble> {
        val postsByProvince = posts.groupBy { it.location.provinceCode }
        val sampleBubbles = runCatching {
            val field = SampleData::class.java.declaredFields.firstOrNull {
                it.name.contains("bubble", ignoreCase = true)
            }
            field?.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            (field?.get(SampleData) as? List<MapBubble>).orEmpty()
        }.getOrDefault(emptyList())

        return provinces.mapNotNull { province ->
            val provincePosts = postsByProvince[province.code].orEmpty()
            if (provincePosts.isEmpty()) return@mapNotNull null

            val existing = sampleBubbles.firstOrNull { bubble ->
                runCatching {
                    val codeProp = bubble::class.java.methods.firstOrNull {
                        it.name == "getProvinceCode" || it.name == "getCode" || it.name == "getId"
                    }
                    codeProp?.invoke(bubble) == province.code
                }.getOrDefault(false)
            }

            if (existing != null) {
                existing
            } else {
                instantiateMapBubbleOrNull(
                    province = province,
                    posts = provincePosts
                )
            }
        }
    }

    private fun instantiateMapBubbleOrNull(
        province: Province,
        posts: List<Post>
    ): MapBubble? {
        return runCatching {
            val clazz = MapBubble::class.java
            val constructor = clazz.constructors.maxByOrNull { it.parameterTypes.size }
                ?: return null
            val latestPost = posts.maxByOrNull { it.takenAt } ?: posts.first()

            val args = constructor.parameters.map { param ->
                val name = param.name.orEmpty().lowercase()
                val type = param.type
                when {
                    type == LocationLevel::class.java -> LocationLevel.PROVINCE
                    type == Province::class.java -> province
                    type == Post::class.java -> latestPost
                    type == Int::class.javaPrimitiveType || type == Int::class.javaObjectType -> posts.size
                    type == Double::class.javaPrimitiveType || type == Double::class.javaObjectType -> {
                        if (name.contains("lng") || name.contains("lon")) province.lng else province.lat
                    }
                    type == Float::class.javaPrimitiveType || type == Float::class.javaObjectType -> {
                        if (name.contains("lng") || name.contains("lon")) province.lng.toFloat() else province.lat.toFloat()
                    }
                    type == Boolean::class.javaPrimitiveType || type == Boolean::class.javaObjectType -> true
                    List::class.java.isAssignableFrom(type) -> posts.map { it.imageUrl }
                    type == String::class.java -> when {
                        name.contains("image") || name.contains("photo") || name.contains("thumb") || name.contains("cover") -> latestPost.imageUrl
                        name.contains("name") || name.contains("title") || name.contains("label") -> province.name
                        else -> province.code
                    }
                    else -> null
                }
            }.toTypedArray()

            constructor.newInstance(*args) as? MapBubble
        }.getOrNull()
    }

    fun extractBubbleProvinceCode(
        bubble: MapBubble,
        provinces: List<Province>
    ): String? {
        return runCatching {
            val methods = bubble::class.java.methods
            val codeCandidate = methods.firstOrNull {
                it.name in listOf("getProvinceCode", "getRegionCode", "getCode", "getId")
            }?.invoke(bubble) as? String

            if (codeCandidate != null && provinces.any { it.code == codeCandidate }) {
                return@runCatching codeCandidate
            }

            val lat = methods.firstOrNull { it.name.contains("Lat", ignoreCase = true) }
                ?.invoke(bubble) as? Number
            val lng = methods.firstOrNull {
                it.name.contains("Lng", ignoreCase = true) || it.name.contains("Lon", ignoreCase = true)
            }?.invoke(bubble) as? Number

            if (lat != null && lng != null) {
                provinces.minByOrNull { p ->
                    val dLat = p.lat - lat.toDouble()
                    val dLng = p.lng - lng.toDouble()
                    dLat * dLat + dLng * dLng
                }?.code
            } else {
                null
            }
        }.getOrNull()
    }
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/profile/ProfileContract.kt
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
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/profile/ProfileViewModel.kt
package vn.nhom2.lokap.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.data.fake.FakePostStore
import vn.nhom2.lokap.data.fake.ProvinceCatalog
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleSocialData
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.feature.map.MapCameraState

class ProfileViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val routeUserId: String? = runCatching {
        savedStateHandle.toRoute<Route.Profile>().userId
    }.getOrElse {
        savedStateHandle.get<String?>("userId")
    }

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects: Flow<ProfileEffect> = _effects.receiveAsFlow()

    init {
        observeProfileData()
    }

    private fun observeProfileData() {
        viewModelScope.launch {
            combine(
                FakePostStore.posts,
                SampleSocialData.friendStatuses,
                SampleSocialData.currentUserOverride
            ) { allPosts, statuses, _ ->
                Triple(allPosts, statuses, SampleSocialData.getCurrentUser())
            }.collect { (allPosts, statuses, currentUser) ->
                val targetUser = SampleSocialData.findUser(routeUserId)
                val allProvinces = ProvinceCatalog.allProvinces
                val totalProvinces = if (allProvinces.isNotEmpty()) allProvinces.size else 34

                if (targetUser == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            isUserNotFound = true,
                            user = null,
                            totalProvincesCount = totalProvinces
                        )
                    }
                    return@collect
                }

                val isMe = targetUser.id == currentUser.id
                val friendStatus = if (isMe) {
                    FriendStatus.NONE
                } else {
                    statuses[targetUser.id] ?: FriendStatus.NONE
                }

                val myFriendsCount = statuses.values.count { it == FriendStatus.FRIENDS }
                val isFriendLimitReached = myFriendsCount >= SampleSocialData.MAX_FRIENDS

                val visibleUserPosts = allPosts
                    .filter { post ->
                        post.author.id == targetUser.id &&
                            SampleSocialData.isPostVisibleToViewer(
                                post = post,
                                isMe = isMe,
                                friendStatus = friendStatus,
                                now = SampleData.NOW
                            )
                    }
                    .sortedByDescending { it.takenAt }

                val provinceByCode = allProvinces.associateBy { it.code }
                val postsByProvince = visibleUserPosts
                    .filter { provinceByCode.containsKey(it.location.provinceCode) }
                    .groupBy { it.location.provinceCode }

                val visitedProvinces = postsByProvince.entries.mapNotNull { (code, postsInProvince) ->
                    val province = provinceByCode[code] ?: return@mapNotNull null
                    VisitedProvinceUi(
                        code = province.code,
                        name = province.name,
                        postCount = postsInProvince.size,
                        photoUrls = postsInProvince.map { it.imageUrl }.take(4),
                        lat = province.lat,
                        lng = province.lng
                    )
                }.sortedByDescending { it.postCount }

                val mapBubbles = SampleSocialData.buildProvinceMapBubbles(
                    posts = visibleUserPosts,
                    provinces = allProvinces
                )

                val visitedProvinceCodes = visitedProvinces.map { it.code }.toSet()
                val initialCamera = visitedProvinces.firstOrNull()?.let { first ->
                    MapCameraState(
                        centerLat = first.lat,
                        centerLng = first.lng,
                        zoom = 6.2f
                    )
                } ?: MapCameraState(
                    centerLat = 16.047079,
                    centerLng = 108.206230,
                    zoom = 5.3f
                )

                _uiState.update { prev ->
                    prev.copy(
                        isLoading = false,
                        errorMessage = null,
                        isUserNotFound = false,
                        isMe = isMe,
                        user = targetUser,
                        friendStatus = friendStatus,
                        visitedProvincesCount = visitedProvinceCodes.size,
                        totalProvincesCount = totalProvinces,
                        postCount = visibleUserPosts.size,
                        friendCount = SampleSocialData.getFriendsCount(targetUser.id),
                        isFriendLimitReached = isFriendLimitReached,
                        visiblePosts = visibleUserPosts,
                        visitedProvinces = visitedProvinces,
                        mapBubbles = mapBubbles,
                        landmarks = SampleData.landmarks,
                        mapCamera = if (prev.isLoading) initialCamera else prev.mapCamera
                    )
                }
            }
        }
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            ProfileAction.Retry -> {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                observeProfileData()
            }

            ProfileAction.NavigateBack -> {
                _effects.trySend(ProfileEffect.NavigateBack)
            }

            ProfileAction.OpenSettings -> {
                _effects.trySend(ProfileEffect.NavigateToSettings)
            }

            is ProfileAction.SelectTab -> {
                _uiState.update { it.copy(selectedTab = action.tab) }
            }

            is ProfileAction.ClickPost -> {
                _effects.trySend(ProfileEffect.NavigateToPostDetail(action.postId))
            }

            is ProfileAction.ClickProvinceCard -> {
                val targetProvince = _uiState.value.visitedProvinces.find { it.code == action.provinceCode }
                if (targetProvince != null) {
                    _uiState.update {
                        it.copy(
                            mapCamera = MapCameraState(
                                centerLat = targetProvince.lat,
                                centerLng = targetProvince.lng,
                                zoom = 8.5f
                            )
                        )
                    }
                }
                _effects.trySend(ProfileEffect.NavigateToRegionAlbum(action.provinceCode))
            }

            is ProfileAction.ClickMapBubble -> {
                val provinceCode = SampleSocialData.extractBubbleProvinceCode(
                    bubble = action.bubble,
                    provinces = ProvinceCatalog.allProvinces
                )
                if (provinceCode != null) {
                    _effects.trySend(ProfileEffect.NavigateToRegionAlbum(provinceCode))
                }
            }

            is ProfileAction.UpdateMapCamera -> {
                _uiState.update { it.copy(mapCamera = action.camera) }
            }

            ProfileAction.OpenEditProfileSheet -> {
                val currentName = _uiState.value.user?.displayName.orEmpty()
                _uiState.update {
                    it.copy(
                        isEditSheetOpen = true,
                        editDisplayNameInput = currentName,
                        editDisplayNameError = null
                    )
                }
            }

            ProfileAction.DismissEditProfileSheet -> {
                _uiState.update {
                    it.copy(
                        isEditSheetOpen = false,
                        editDisplayNameError = null,
                        isSavingProfile = false
                    )
                }
            }

            is ProfileAction.ChangeEditDisplayName -> {
                val trimmed = action.value.trim()
                val error = validateDisplayName(trimmed)
                _uiState.update {
                    it.copy(
                        editDisplayNameInput = action.value,
                        editDisplayNameError = error
                    )
                }
            }

            ProfileAction.SaveEditProfile -> {
                val trimmed = _uiState.value.editDisplayNameInput.trim()
                val error = validateDisplayName(trimmed)
                if (error != null) {
                    _uiState.update { it.copy(editDisplayNameError = error) }
                    return
                }
                SampleSocialData.updateMyDisplayName(trimmed)
                _uiState.update {
                    it.copy(
                        isEditSheetOpen = false,
                        editDisplayNameError = null,
                        isSavingProfile = false
                    )
                }
                _effects.trySend(ProfileEffect.ShowMessage("Đã cập nhật tên hiển thị"))
            }

            ProfileAction.SendFriendRequest -> {
                val targetUser = _uiState.value.user ?: return
                val ok = SampleSocialData.sendFriendRequest(targetUser.id)
                if (!ok) {
                    _effects.trySend(
                        ProfileEffect.ShowMessage("Bạn đã đạt giới hạn 20 người bạn trên Lokap")
                    )
                } else {
                    _effects.trySend(ProfileEffect.ShowMessage("Đã gửi lời mời kết bạn"))
                }
            }

            ProfileAction.CancelFriendRequest -> {
                val targetUser = _uiState.value.user ?: return
                SampleSocialData.cancelFriendRequest(targetUser.id)
                _effects.trySend(ProfileEffect.ShowMessage("Đã huỷ lời mời kết bạn"))
            }

            ProfileAction.AcceptFriendRequest -> {
                val targetUser = _uiState.value.user ?: return
                val ok = SampleSocialData.acceptFriendRequest(targetUser.id)
                if (!ok) {
                    _effects.trySend(
                        ProfileEffect.ShowMessage("Đã đủ 20 bạn bè, không thể nhận thêm")
                    )
                } else {
                    _effects.trySend(
                        ProfileEffect.ShowMessage("Hai bạn đã trở thành bạn bè!")
                    )
                }
            }

            ProfileAction.DeclineFriendRequest -> {
                val targetUser = _uiState.value.user ?: return
                SampleSocialData.declineFriendRequest(targetUser.id)
                _effects.trySend(ProfileEffect.ShowMessage("Đã từ chối lời mời"))
            }

            ProfileAction.Unfriend -> {
                val targetUser = _uiState.value.user ?: return
                SampleSocialData.unfriend(targetUser.id)
                _effects.trySend(
                    ProfileEffect.ShowMessage("Đã huỷ kết bạn với ${targetUser.displayName}")
                )
            }

            ProfileAction.Unblock -> {
                val targetUser = _uiState.value.user ?: return
                SampleSocialData.unblockUser(targetUser.id)
                _effects.trySend(
                    ProfileEffect.ShowMessage("Đã bỏ chặn ${targetUser.displayName}")
                )
            }

            ProfileAction.MessageUser -> {
                val targetUser = _uiState.value.user ?: return
                val convId = SampleSocialData.conversationIdFor(targetUser.id)
                _effects.trySend(ProfileEffect.NavigateToChat(convId))
            }
        }
    }

    private fun validateDisplayName(trimmed: String): String? {
        return when {
            trimmed.length < 2 -> "Tên hiển thị phải từ 2 đến 30 ký tự"
            trimmed.length > 30 -> "Tên hiển thị tối đa 30 ký tự"
            else -> null
        }
    }
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/profile/ProfileScreen.kt
package vn.nhom2.lokap.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import coil3.compose.AsyncImage
import vn.nhom2.lokap.core.designsystem.component.AvatarRing
import vn.nhom2.lokap.core.designsystem.component.EmptyState
import vn.nhom2.lokap.core.designsystem.component.ErrorState
import vn.nhom2.lokap.core.designsystem.component.GhostButton
import vn.nhom2.lokap.core.designsystem.component.GlassChip
import vn.nhom2.lokap.core.designsystem.component.OfflineBanner
import vn.nhom2.lokap.core.designsystem.component.PinkButton
import vn.nhom2.lokap.core.designsystem.component.RegionCard
import vn.nhom2.lokap.core.designsystem.component.ShimmerBox
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.core.ui.theme.LokapTheme
import vn.nhom2.lokap.core.ui.theme.regionGradient
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.feature.map.LokapMap

fun NavGraphBuilder.profileScreen(navController: NavController) {
    composable<Route.Profile> {
        ProfileRoute(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToSettings = {
                navController.navigate(Route.Settings) {
                    launchSingleTop = true
                }
            },
            onNavigateToPostDetail = { postId ->
                navController.navigate(Route.PostDetail(postId)) {
                    launchSingleTop = true
                }
            },
            onNavigateToChat = { conversationId ->
                navController.navigate(Route.Chat(conversationId)) {
                    launchSingleTop = true
                }
            },
            onNavigateToRegionAlbum = { provinceCode ->
                navController.navigate(
                    Route.RegionAlbum(
                        level = LocationLevel.PROVINCE,
                        code = provinceCode
                    )
                ) {
                    launchSingleTop = true
                }
            }
        )
    }
}

@Composable
fun ProfileRoute(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToRegionAlbum: (String) -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProfileEffect.NavigateBack -> onNavigateBack()
                ProfileEffect.NavigateToSettings -> onNavigateToSettings()
                is ProfileEffect.NavigateToPostDetail -> onNavigateToPostDetail(effect.postId)
                is ProfileEffect.NavigateToChat -> onNavigateToChat(effect.conversationId)
                is ProfileEffect.NavigateToRegionAlbum -> onNavigateToRegionAlbum(effect.provinceCode)
                is ProfileEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    ProfileScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(LokapTheme.colors.background)
            .systemBarsPadding(),
        containerColor = LokapTheme.colors.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ProfileTopBar(
                username = uiState.user?.username,
                isMe = uiState.isMe,
                onBackClick = { onAction(ProfileAction.NavigateBack) },
                onSettingsClick = { onAction(ProfileAction.OpenSettings) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isOffline) {
                OfflineBanner(modifier = Modifier.fillMaxWidth())
            }

            when {
                uiState.isLoading -> {
                    ProfileLoadingContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = LokapTheme.spacing.md)
                    )
                }

                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        ErrorState(
                            message = uiState.errorMessage,
                            onRetry = { onAction(ProfileAction.Retry) },
                            modifier = Modifier.padding(LokapTheme.spacing.lg)
                        )
                    }
                }

                uiState.isUserNotFound || uiState.user == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            title = "Người dùng không tồn tại",
                            subtitle = "Tài khoản này có thể đã đổi tên hoặc không còn hoạt động trên Lokap.",
                            actionLabel = "Quay lại",
                            onActionClick = { onAction(ProfileAction.NavigateBack) },
                            modifier = Modifier.padding(LokapTheme.spacing.lg)
                        )
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = LokapTheme.spacing.md,
                            end = LokapTheme.spacing.md,
                            bottom = LokapTheme.spacing.xxl
                        ),
                        horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.xs)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            ProfileHeaderSection(
                                uiState = uiState,
                                onAction = onAction
                            )
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            ProfileTabSelector(
                                selectedTab = uiState.selectedTab,
                                onSelectTab = { onAction(ProfileAction.SelectTab(it)) },
                                modifier = Modifier.padding(vertical = LokapTheme.spacing.sm)
                            )
                        }

                        when (uiState.selectedTab) {
                            ProfileTab.PHOTOS -> {
                                if (uiState.visiblePosts.isEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        EmptyState(
                                            title = "Chưa có khoảnh khắc nào",
                                            subtitle = if (uiState.isMe) {
                                                "Mở camera lên và thả ngay chiếc ảnh đầu tiên lên bản đồ nhé!"
                                            } else {
                                                "Người này chưa chia sẻ khoảnh khắc nào ở chế độ hiển thị với bạn."
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = LokapTheme.spacing.xxl)
                                        )
                                    }
                                } else {
                                    items(
                                        items = uiState.visiblePosts,
                                        key = { it.id }
                                    ) { post ->
                                        ProfilePhotoGridItem(
                                            post = post,
                                            onClick = { onAction(ProfileAction.ClickPost(post.id)) }
                                        )
                                    }
                                }
                            }

                            ProfileTab.FOOTPRINTS -> {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    ProfileFootprintsSection(
                                        uiState = uiState,
                                        onAction = onAction
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.isEditSheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onAction(ProfileAction.DismissEditProfileSheet) },
            sheetState = sheetState,
            shape = LokapTheme.shapes.sheet,
            containerColor = LokapTheme.colors.surface,
            contentColor = LokapTheme.colors.textPrimary
        ) {
            EditProfileBottomSheetContent(
                displayName = uiState.editDisplayNameInput,
                errorText = uiState.editDisplayNameError,
                isSaving = uiState.isSavingProfile,
                onNameChange = { onAction(ProfileAction.ChangeEditDisplayName(it)) },
                onCancel = { onAction(ProfileAction.DismissEditProfileSheet) },
                onSave = { onAction(ProfileAction.SaveEditProfile) }
            )
        }
    }
}

@Composable
private fun ProfileTopBar(
    username: String?,
    isMe: Boolean,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = LokapTheme.spacing.sm,
                vertical = LokapTheme.spacing.xs
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Quay lại",
                tint = LokapTheme.colors.textPrimary
            )
        }

        Text(
            text = if (username != null) "@$username" else "Hồ sơ",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = LokapTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (isMe) {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Cài đặt",
                    tint = LokapTheme.colors.textPrimary
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun ProfileHeaderSection(
    uiState: ProfileUiState,
    onAction: (ProfileAction) -> Unit
) {
    val user = uiState.user ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = LokapTheme.spacing.xs, bottom = LokapTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AvatarRing(
            avatarUrl = user.avatarUrl,
            displayName = user.displayName,
            size = 96.dp,
            hasNewPost = uiState.visiblePosts.isNotEmpty()
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

        Text(
            text = user.displayName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = LokapTheme.colors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))

        Text(
            text = "@${user.username}",
            style = MaterialTheme.typography.bodyMedium,
            color = LokapTheme.colors.primary,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.md))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(LokapTheme.shapes.card)
                .background(LokapTheme.colors.surface)
                .border(
                    width = 1.dp,
                    color = LokapTheme.colors.outline,
                    shape = LokapTheme.shapes.card
                )
                .padding(vertical = LokapTheme.spacing.md, horizontal = LokapTheme.spacing.sm),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileStatItem(
                value = "${uiState.visitedProvincesCount}/${uiState.totalProvincesCount}",
                label = "Đã đến ${uiState.visitedProvincesCount}/${uiState.totalProvincesCount} tỉnh",
                highlight = true,
                modifier = Modifier.weight(1.3f)
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(LokapTheme.colors.outline)
            )

            ProfileStatItem(
                value = uiState.postCount.toString(),
                label = "Bài đăng",
                highlight = false,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(LokapTheme.colors.outline)
            )

            ProfileStatItem(
                value = uiState.friendCount.toString(),
                label = "Bạn bè",
                highlight = false,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(LokapTheme.spacing.md))

        if (uiState.isMe) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
            ) {
                GhostButton(
                    text = "Sửa hồ sơ",
                    leadingIcon = Icons.Default.Edit,
                    onClick = { onAction(ProfileAction.OpenEditProfileSheet) },
                    modifier = Modifier.weight(1f)
                )
                GhostButton(
                    text = "Cài đặt",
                    leadingIcon = Icons.Default.Settings,
                    onClick = { onAction(ProfileAction.OpenSettings) },
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            OtherUserRelationshipBar(
                friendStatus = uiState.friendStatus,
                isFriendLimitReached = uiState.isFriendLimitReached,
                onAction = onAction
            )
        }
    }
}

@Composable
private fun ProfileStatItem(
    value: String,
    label: String,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = if (highlight) LokapTheme.colors.primary else LokapTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LokapTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun OtherUserRelationshipBar(
    friendStatus: FriendStatus,
    isFriendLimitReached: Boolean,
    onAction: (ProfileAction) -> Unit
) {
    var friendMenuExpanded by remember { mutableStateOf(false) }

    when (friendStatus) {
        FriendStatus.NONE -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PinkButton(
                    text = "Kết bạn",
                    leadingIcon = Icons.Default.Add,
                    enabled = !isFriendLimitReached,
                    onClick = { onAction(ProfileAction.SendFriendRequest) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (isFriendLimitReached) {
                    Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))
                    Text(
                        text = "Bạn đã đạt giới hạn tối đa 20 bạn bè như Locket",
                        style = MaterialTheme.typography.labelSmall,
                        color = LokapTheme.colors.accentYellow
                    )
                }
            }
        }

        FriendStatus.REQUEST_SENT -> {
            GhostButton(
                text = "Đã gửi lời mời · Huỷ",
                leadingIcon = Icons.Default.Close,
                onClick = { onAction(ProfileAction.CancelFriendRequest) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        FriendStatus.REQUEST_RECEIVED -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
                ) {
                    PinkButton(
                        text = "Chấp nhận",
                        leadingIcon = Icons.Default.Check,
                        enabled = !isFriendLimitReached,
                        onClick = { onAction(ProfileAction.AcceptFriendRequest) },
                        modifier = Modifier.weight(1f)
                    )
                    GhostButton(
                        text = "Từ chối",
                        leadingIcon = Icons.Default.Close,
                        onClick = { onAction(ProfileAction.DeclineFriendRequest) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (isFriendLimitReached) {
                    Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))
                    Text(
                        text = "Danh sách bạn bè đã đầy (20/20). Không thể chấp nhận thêm.",
                        style = MaterialTheme.typography.labelSmall,
                        color = LokapTheme.colors.accentYellow
                    )
                }
            }
        }

        FriendStatus.FRIENDS -> {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
                ) {
                    PinkButton(
                        text = "Nhắn tin",
                        onClick = { onAction(ProfileAction.MessageUser) },
                        modifier = Modifier.weight(1f)
                    )
                    GhostButton(
                        text = "Bạn bè",
                        leadingIcon = Icons.Default.Person,
                        onClick = { friendMenuExpanded = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                DropdownMenu(
                    expanded = friendMenuExpanded,
                    onDismissRequest = { friendMenuExpanded = false },
                    modifier = Modifier.background(LokapTheme.colors.surfaceHigh)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Nhắn tin",
                                color = LokapTheme.colors.textPrimary
                            )
                        },
                        onClick = {
                            friendMenuExpanded = false
                            onAction(ProfileAction.MessageUser)
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Huỷ kết bạn",
                                color = LokapTheme.colors.error
                            )
                        },
                        onClick = {
                            friendMenuExpanded = false
                            onAction(ProfileAction.Unfriend)
                        }
                    )
                }
            }
        }

        FriendStatus.BLOCKED -> {
            GhostButton(
                text = "Đã chặn · Bỏ chặn",
                onClick = { onAction(ProfileAction.Unblock) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ProfileTabSelector(
    selectedTab: ProfileTab,
    onSelectTab: (ProfileTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
    ) {
        ProfileTab.entries.forEach { tab ->
            GlassChip(
                label = tab.titleVi,
                leadingIcon = if (tab == ProfileTab.FOOTPRINTS) Icons.Default.LocationOn else null,
                selected = selectedTab == tab,
                onClick = { onSelectTab(tab) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ProfilePhotoGridItem(
    post: Post,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(LokapTheme.shapes.card)
            .background(LokapTheme.colors.surfaceHigh)
            .border(
                width = 1.dp,
                brush = regionGradient(post.location.provinceCode),
                shape = LokapTheme.shapes.card
            )
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = post.imageUrl,
            contentDescription = post.caption.ifBlank { post.location.provinceName },
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(LokapTheme.spacing.xs)
                .clip(LokapTheme.shapes.pill)
                .background(LokapTheme.colors.background.copy(alpha = 0.72f))
                .padding(
                    horizontal = LokapTheme.spacing.xs,
                    vertical = 2.dp
                )
        ) {
            Text(
                text = post.location.provinceName,
                style = MaterialTheme.typography.labelSmall,
                color = LokapTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProfileFootprintsSection(
    uiState: ProfileUiState,
    onAction: (ProfileAction) -> Unit
) {
    if (uiState.visitedProvinces.isEmpty()) {
        EmptyState(
            title = "Chưa có khoảnh khắc nào",
            subtitle = "Chưa có dấu chân nào được ghi nhận trên bản đồ 34 tỉnh thành.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = LokapTheme.spacing.xxl)
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = LokapTheme.spacing.xs)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(LokapTheme.shapes.cameraFrame)
                .border(
                    width = 1.dp,
                    color = LokapTheme.colors.outline,
                    shape = LokapTheme.shapes.cameraFrame
                )
        ) {
            LokapMap(
                bubbles = uiState.mapBubbles,
                landmarks = uiState.landmarks,
                camera = uiState.mapCamera,
                onCameraChange = { onAction(ProfileAction.UpdateMapCamera(it)) },
                onBubbleClick = { onAction(ProfileAction.ClickMapBubble(it)) },
                onLandmarkClick = {},
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(LokapTheme.spacing.sm)
                    .clip(LokapTheme.shapes.pill)
                    .background(LokapTheme.colors.surface.copy(alpha = 0.85f))
                    .padding(
                        horizontal = LokapTheme.spacing.sm,
                        vertical = LokapTheme.spacing.xs
                    )
            ) {
                Text(
                    text = "Đã mở khoá ${uiState.visitedProvincesCount}/${uiState.totalProvincesCount} tỉnh",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LokapTheme.colors.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(LokapTheme.spacing.md))

        Text(
            text = "Hộ chiếu vùng miền (${uiState.visitedProvinces.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = LokapTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm),
            contentPadding = PaddingValues(vertical = LokapTheme.spacing.xs)
        ) {
            items(
                items = uiState.visitedProvinces,
                key = { it.code }
            ) { province ->
                RegionCard(
                    code = province.code,
                    regionName = province.name,
                    photoUrls = province.photoUrls,
                    postCount = province.postCount,
                    visited = true,
                    onClick = { onAction(ProfileAction.ClickProvinceCard(province.code)) },
                    modifier = Modifier.width(210.dp)
                )
            }
        }
    }
}

@Composable
private fun EditProfileBottomSheetContent(
    displayName: String,
    errorText: String?,
    isSaving: Boolean,
    onNameChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                horizontal = LokapTheme.spacing.lg,
                vertical = LokapTheme.spacing.md
            )
    ) {
        Text(
            text = "Đổi tên hiển thị",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = LokapTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))

        Text(
            text = "Tên này sẽ hiện trên ảnh Locket và bản đồ cùng hội bạn thân (2–30 ký tự).",
            style = MaterialTheme.typography.bodySmall,
            color = LokapTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.md))

        OutlinedTextField(
            value = displayName,
            onValueChange = onNameChange,
            singleLine = true,
            label = { Text("Tên hiển thị") },
            isError = errorText != null,
            supportingText = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = errorText ?: "Nhập từ 2 đến 30 ký tự",
                        color = if (errorText != null) {
                            LokapTheme.colors.error
                        } else {
                            LokapTheme.colors.textTertiary
                        }
                    )
                    Text(
                        text = "${displayName.trim().length}/30",
                        color = LokapTheme.colors.textTertiary
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LokapTheme.colors.primary,
                unfocusedBorderColor = LokapTheme.colors.outline,
                errorBorderColor = LokapTheme.colors.error,
                focusedLabelColor = LokapTheme.colors.primary,
                unfocusedLabelColor = LokapTheme.colors.textSecondary,
                cursorColor = LokapTheme.colors.primary,
                focusedTextColor = LokapTheme.colors.textPrimary,
                unfocusedTextColor = LokapTheme.colors.textPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.lg))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
        ) {
            GhostButton(
                text = "Huỷ",
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            )
            PinkButton(
                text = "Lưu thay đổi",
                loading = isSaving,
                enabled = errorText == null && displayName.trim().length in 2..30,
                onClick = onSave,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(LokapTheme.spacing.md))
    }
}

@Composable
private fun ProfileLoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(LokapTheme.spacing.md))
        ShimmerBox(
            modifier = Modifier.size(96.dp),
            shape = LokapTheme.shapes.pill
        )
        Spacer(modifier = Modifier.height(LokapTheme.spacing.md))
        ShimmerBox(
            modifier = Modifier
                .width(160.dp)
                .height(24.dp),
            shape = LokapTheme.shapes.pill
        )
        Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))
        ShimmerBox(
            modifier = Modifier
                .width(100.dp)
                .height(16.dp),
            shape = LokapTheme.shapes.pill
        )
        Spacer(modifier = Modifier.height(LokapTheme.spacing.lg))
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp),
            shape = LokapTheme.shapes.card
        )
        Spacer(modifier = Modifier.height(LokapTheme.spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.xs)
        ) {
            repeat(3) {
                ShimmerBox(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    shape = LokapTheme.shapes.card
                )
            }
        }
    }
}

@Preview(name = "Profile - Hồ sơ của tôi", showBackground = true)
@Composable
private fun ProfileMyScreenPreview() {
    LokapTheme {
        ProfileScreen(
            uiState = ProfileUiState(
                isLoading = false,
                isMe = true,
                user = SampleData.currentUser,
                visitedProvincesCount = 5,
                totalProvincesCount = 34,
                postCount = SampleData.posts.size,
                friendCount = 12,
                visiblePosts = SampleData.posts.take(6)
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Profile - Hồ sơ người khác (Lời mời)", showBackground = true)
@Composable
private fun ProfileOtherUserRequestPreview() {
    LokapTheme {
        ProfileScreen(
            uiState = ProfileUiState(
                isLoading = false,
                isMe = false,
                user = SampleData.users.firstOrNull() ?: SampleData.currentUser,
                friendStatus = FriendStatus.REQUEST_RECEIVED,
                visitedProvincesCount = 2,
                totalProvincesCount = 34,
                postCount = 0,
                friendCount = 8,
                visiblePosts = emptyList()
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Profile - Đang tải", showBackground = true)
@Composable
private fun ProfileLoadingPreview() {
    LokapTheme {
        ProfileScreen(
            uiState = ProfileUiState(isLoading = true),
            onAction = {}
        )
    }
}

@Preview(name = "Profile - Người dùng không tồn tại", showBackground = true)
@Composable
private fun ProfileNotFoundPreview() {
    LokapTheme {
        ProfileScreen(
            uiState = ProfileUiState(
                isLoading = false,
                isUserNotFound = true,
                user = null
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Profile - Lỗi mạng", showBackground = true)
@Composable
private fun ProfileErrorPreview() {
    LokapTheme {
        ProfileScreen(
            uiState = ProfileUiState(
                isLoading = false,
                errorMessage = "Không thể tải hồ sơ. Kiểm tra kết nối mạng nhé!"
            ),
            onAction = {}
        )
    }
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/friends/FriendsContract.kt
package vn.nhom2.lokap.feature.friends

import androidx.compose.runtime.Immutable
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.model.User

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
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/friends/FriendsViewModel.kt
package vn.nhom2.lokap.feature.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.data.fake.SampleSocialData
import vn.nhom2.lokap.domain.model.FriendStatus

@OptIn(FlowPreview::class)
class FriendsViewModel : ViewModel() {

    private val rawSearchQuery = MutableStateFlow("")

    private val _uiState = MutableStateFlow(FriendsUiState(isLoading = true))
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<FriendsEffect>(Channel.BUFFERED)
    val effects: Flow<FriendsEffect> = _effects.receiveAsFlow()

    init {
        observeFriendsAndSearch()
    }

    private fun observeFriendsAndSearch() {
        val debouncedQueryFlow = rawSearchQuery
            .map { it.trim() }
            .debounce(300L)
            .distinctUntilChanged()

        viewModelScope.launch {
            combine(
                SampleSocialData.friendStatuses,
                debouncedQueryFlow
            ) { statuses, query ->
                statuses to query
            }.collect { (statuses, query) ->
                val currentUserId = SampleSocialData.getCurrentUser().id
                val allOthers = SampleSocialData.directoryUsers.filter { it.id != currentUserId }

                val incoming = allOthers.filter { statuses[it.id] == FriendStatus.REQUEST_RECEIVED }
                val sent = allOthers.filter { statuses[it.id] == FriendStatus.REQUEST_SENT }
                val friends = allOthers.filter { statuses[it.id] == FriendStatus.FRIENDS }

                val normalizedQuery = query.removePrefix("@").lowercase()
                val searchResults = if (normalizedQuery.isBlank()) {
                    emptyList()
                } else {
                    allOthers.filter { user ->
                        user.username.lowercase().contains(normalizedQuery) ||
                            user.displayName.lowercase().contains(normalizedQuery)
                    }.map { user ->
                        FriendUserItemUi(
                            user = user,
                            status = statuses[user.id] ?: FriendStatus.NONE
                        )
                    }
                }

                _uiState.update { prev ->
                    prev.copy(
                        isLoading = false,
                        errorMessage = null,
                        debouncedQuery = query,
                        searchResults = searchResults,
                        incomingRequests = incoming,
                        sentRequests = sent,
                        friends = friends,
                        maxFriends = SampleSocialData.MAX_FRIENDS
                    )
                }
            }
        }
    }

    fun onAction(action: FriendsAction) {
        when (action) {
            FriendsAction.Retry -> {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                observeFriendsAndSearch()
            }

            FriendsAction.NavigateBack -> {
                _effects.trySend(FriendsEffect.NavigateBack)
            }

            is FriendsAction.ChangeSearchQuery -> {
                rawSearchQuery.value = action.query
                _uiState.update { it.copy(searchQuery = action.query) }
            }

            FriendsAction.ClearSearch -> {
                rawSearchQuery.value = ""
                _uiState.update {
                    it.copy(
                        searchQuery = "",
                        debouncedQuery = "",
                        searchResults = emptyList()
                    )
                }
            }

            is FriendsAction.OpenProfile -> {
                _effects.trySend(FriendsEffect.NavigateToProfile(action.userId))
            }

            is FriendsAction.SendRequest -> {
                val ok = SampleSocialData.sendFriendRequest(action.userId)
                if (!ok) {
                    _effects.trySend(
                        FriendsEffect.ShowMessage("Đã chạm mốc 20 bạn bè! Hãy dọn danh sách nếu muốn thêm bạn mới.")
                    )
                } else {
                    _effects.trySend(FriendsEffect.ShowMessage("Đã gửi lời mời kết bạn"))
                }
            }

            is FriendsAction.CancelSentRequest -> {
                SampleSocialData.cancelFriendRequest(action.userId)
                _effects.trySend(FriendsEffect.ShowMessage("Đã thu hồi lời mời kết bạn"))
            }

            is FriendsAction.AcceptRequest -> {
                val ok = SampleSocialData.acceptFriendRequest(action.userId)
                if (!ok) {
                    _effects.trySend(
                        FriendsEffect.ShowMessage("Đã đủ 20 người bạn, không thể chấp nhận thêm")
                    )
                } else {
                    _effects.trySend(FriendsEffect.ShowMessage("Đã thêm vào hội bạn thân!"))
                }
            }

            is FriendsAction.DeclineRequest -> {
                SampleSocialData.declineFriendRequest(action.userId)
                _effects.trySend(FriendsEffect.ShowMessage("Đã từ chối lời mời"))
            }

            is FriendsAction.UnblockUser -> {
                SampleSocialData.unblockUser(action.userId)
                _effects.trySend(FriendsEffect.ShowMessage("Đã bỏ chặn người dùng"))
            }

            is FriendsAction.RequestUnfriendConfirm -> {
                _uiState.update {
                    it.copy(
                        pendingConfirmation = PendingFriendConfirmation(
                            user = action.user,
                            actionType = FriendConfirmActionType.UNFRIEND
                        )
                    )
                }
            }

            is FriendsAction.RequestBlockConfirm -> {
                _uiState.update {
                    it.copy(
                        pendingConfirmation = PendingFriendConfirmation(
                            user = action.user,
                            actionType = FriendConfirmActionType.BLOCK
                        )
                    )
                }
            }

            FriendsAction.DismissConfirmDialog -> {
                _uiState.update { it.copy(pendingConfirmation = null) }
            }

            FriendsAction.ConfirmPendingAction -> {
                val pending = _uiState.value.pendingConfirmation ?: return
                when (pending.actionType) {
                    FriendConfirmActionType.UNFRIEND -> {
                        SampleSocialData.unfriend(pending.user.id)
                        _effects.trySend(
                            FriendsEffect.ShowMessage("Đã huỷ kết bạn với ${pending.user.displayName}")
                        )
                    }

                    FriendConfirmActionType.BLOCK -> {
                        SampleSocialData.blockUser(pending.user.id)
                        _effects.trySend(
                            FriendsEffect.ShowMessage("Đã chặn ${pending.user.displayName}")
                        )
                    }
                }
                _uiState.update { it.copy(pendingConfirmation = null) }
            }
        }
    }
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/friends/FriendsScreen.kt
package vn.nhom2.lokap.feature.friends

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import vn.nhom2.lokap.core.designsystem.component.AvatarRing
import vn.nhom2.lokap.core.designsystem.component.BadgeDot
import vn.nhom2.lokap.core.designsystem.component.EmptyState
import vn.nhom2.lokap.core.designsystem.component.ErrorState
import vn.nhom2.lokap.core.designsystem.component.GhostButton
import vn.nhom2.lokap.core.designsystem.component.OfflineBanner
import vn.nhom2.lokap.core.designsystem.component.PinkButton
import vn.nhom2.lokap.core.designsystem.component.ShimmerBox
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.core.ui.theme.LokapTheme
import vn.nhom2.lokap.data.fake.SampleSocialData
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.model.User

fun NavGraphBuilder.friendsScreen(navController: NavController) {
    composable<Route.Friends> {
        FriendsRoute(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToProfile = { userId ->
                navController.navigate(Route.Profile(userId = userId)) {
                    launchSingleTop = true
                }
            }
        )
    }
}

@Composable
fun FriendsRoute(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    viewModel: FriendsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                FriendsEffect.NavigateBack -> onNavigateBack()
                is FriendsEffect.NavigateToProfile -> onNavigateToProfile(effect.userId)
                is FriendsEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    FriendsScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState
    )
}

@Composable
fun FriendsScreen(
    uiState: FriendsUiState,
    onAction: (FriendsAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(LokapTheme.colors.background)
            .systemBarsPadding(),
        containerColor = LokapTheme.colors.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            FriendsTopBar(
                friendCount = uiState.friends.size,
                maxFriends = uiState.maxFriends,
                onBackClick = { onAction(FriendsAction.NavigateBack) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isOffline) {
                OfflineBanner(modifier = Modifier.fillMaxWidth())
            }

            FriendsSearchBar(
                query = uiState.searchQuery,
                onQueryChange = { onAction(FriendsAction.ChangeSearchQuery(it)) },
                onClear = { onAction(FriendsAction.ClearSearch) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = LokapTheme.spacing.md,
                        vertical = LokapTheme.spacing.xs
                    )
            )

            if (uiState.isFriendLimitReached) {
                FriendLimitWarningBanner(
                    maxFriends = uiState.maxFriends,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = LokapTheme.spacing.md,
                            vertical = LokapTheme.spacing.xs
                        )
                )
            }

            when {
                uiState.isLoading -> {
                    FriendsLoadingList(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = LokapTheme.spacing.md)
                    )
                }

                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        ErrorState(
                            message = uiState.errorMessage,
                            onRetry = { onAction(FriendsAction.Retry) },
                            modifier = Modifier.padding(LokapTheme.spacing.lg)
                        )
                    }
                }

                uiState.isSearching -> {
                    if (uiState.searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyState(
                                title = "Không có ai tên như vậy",
                                subtitle = "Thử kiểm tra lại @username xem có gõ nhầm ký tự nào không nhé.",
                                actionLabel = "Xoá tìm kiếm",
                                onActionClick = { onAction(FriendsAction.ClearSearch) },
                                modifier = Modifier.padding(LokapTheme.spacing.lg)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = LokapTheme.spacing.md,
                                vertical = LokapTheme.spacing.sm
                            ),
                            verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
                        ) {
                            item {
                                SectionHeaderRow(
                                    title = "Kết quả tìm kiếm",
                                    badgeCount = uiState.searchResults.size
                                )
                            }
                            items(
                                items = uiState.searchResults,
                                key = { "search_${it.user.id}" }
                            ) { item ->
                                SearchResultUserRow(
                                    item = item,
                                    isFriendLimitReached = uiState.isFriendLimitReached,
                                    onAction = onAction
                                )
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = LokapTheme.spacing.md,
                            end = LokapTheme.spacing.md,
                            top = LokapTheme.spacing.sm,
                            bottom = LokapTheme.spacing.xxl
                        ),
                        verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
                    ) {
                        if (uiState.incomingRequests.isNotEmpty()) {
                            item(key = "header_incoming") {
                                SectionHeaderRow(
                                    title = "Lời mời kết bạn",
                                    badgeCount = uiState.incomingRequests.size
                                )
                            }
                            items(
                                items = uiState.incomingRequests,
                                key = { "incoming_${it.id}" }
                            ) { user ->
                                IncomingRequestRow(
                                    user = user,
                                    isFriendLimitReached = uiState.isFriendLimitReached,
                                    onOpenProfile = { onAction(FriendsAction.OpenProfile(user.id)) },
                                    onAccept = { onAction(FriendsAction.AcceptRequest(user.id)) },
                                    onDecline = { onAction(FriendsAction.DeclineRequest(user.id)) }
                                )
                            }
                            item(key = "spacer_incoming") {
                                Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))
                            }
                        }

                        if (uiState.sentRequests.isNotEmpty()) {
                            item(key = "header_sent") {
                                SectionHeaderRow(
                                    title = "Đã gửi",
                                    badgeCount = uiState.sentRequests.size
                                )
                            }
                            items(
                                items = uiState.sentRequests,
                                key = { "sent_${it.id}" }
                            ) { user ->
                                SentRequestRow(
                                    user = user,
                                    onOpenProfile = { onAction(FriendsAction.OpenProfile(user.id)) },
                                    onCancel = { onAction(FriendsAction.CancelSentRequest(user.id)) }
                                )
                            }
                            item(key = "spacer_sent") {
                                Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))
                            }
                        }

                        item(key = "header_friends") {
                            SectionHeaderRow(
                                title = "Bạn bè (${uiState.friends.size})",
                                badgeCount = 0
                            )
                        }

                        if (uiState.friends.isEmpty()) {
                            item(key = "empty_friends") {
                                EmptyState(
                                    title = "Rủ bạn bè cài Lokap nhé",
                                    subtitle = "Kết nối tối đa 20 người bạn thân nhất để bắn ảnh trực tiếp và gom dấu chân khắp 34 tỉnh thành!",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = LokapTheme.spacing.xl)
                                )
                            }
                        } else {
                            items(
                                items = uiState.friends,
                                key = { "friend_${it.id}" }
                            ) { friend ->
                                FriendRowItem(
                                    user = friend,
                                    onClick = { onAction(FriendsAction.OpenProfile(friend.id)) },
                                    onRequestUnfriend = { onAction(FriendsAction.RequestUnfriendConfirm(friend)) },
                                    onRequestBlock = { onAction(FriendsAction.RequestBlockConfirm(friend)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    uiState.pendingConfirmation?.let { pending ->
        FriendConfirmationDialog(
            pending = pending,
            onDismiss = { onAction(FriendsAction.DismissConfirmDialog) },
            onConfirm = { onAction(FriendsAction.ConfirmPendingAction) }
        )
    }
}

@Composable
private fun FriendsTopBar(
    friendCount: Int,
    maxFriends: Int,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = LokapTheme.spacing.sm,
                vertical = LokapTheme.spacing.xs
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Quay lại",
                tint = LokapTheme.colors.textPrimary
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Hội bạn thân",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LokapTheme.colors.textPrimary
            )
            Text(
                text = "$friendCount/$maxFriends người bạn trên Lokap",
                style = MaterialTheme.typography.labelSmall,
                color = LokapTheme.colors.textSecondary
            )
        }

        Box(
            modifier = Modifier
                .padding(end = LokapTheme.spacing.sm)
                .clip(LokapTheme.shapes.pill)
                .background(
                    if (friendCount >= maxFriends) {
                        LokapTheme.colors.accentYellow.copy(alpha = 0.2f)
                    } else {
                        LokapTheme.colors.primarySoft
                    }
                )
                .padding(
                    horizontal = LokapTheme.spacing.sm,
                    vertical = LokapTheme.spacing.xs
                )
        ) {
            Text(
                text = "$friendCount/$maxFriends",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (friendCount >= maxFriends) {
                    LokapTheme.colors.accentYellow
                } else {
                    LokapTheme.colors.primary
                }
            )
        }
    }
}

@Composable
private fun FriendsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        shape = LokapTheme.shapes.pill,
        placeholder = {
            Text(
                text = "Tìm bạn bè theo @username...",
                color = LokapTheme.colors.textTertiary
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = LokapTheme.colors.textSecondary
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Xoá",
                        tint = LokapTheme.colors.textSecondary
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = LokapTheme.colors.surface,
            unfocusedContainerColor = LokapTheme.colors.surface,
            focusedBorderColor = LokapTheme.colors.primary,
            unfocusedBorderColor = LokapTheme.colors.outline,
            cursorColor = LokapTheme.colors.primary,
            focusedTextColor = LokapTheme.colors.textPrimary,
            unfocusedTextColor = LokapTheme.colors.textPrimary
        ),
        modifier = modifier
    )
}

@Composable
private fun FriendLimitWarningBanner(
    maxFriends: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(LokapTheme.shapes.card)
            .background(LokapTheme.colors.surfaceHigh)
            .border(
                width = 1.dp,
                color = LokapTheme.colors.accentYellow,
                shape = LokapTheme.shapes.card
            )
            .padding(LokapTheme.spacing.sm)
    ) {
        Text(
            text = "Bạn đã đạt tối đa $maxFriends/$maxFriends người bạn như Locket. Các nút Kết bạn và Chấp nhận tạm khoá cho tới khi bạn bớt danh sách.",
            style = MaterialTheme.typography.bodySmall,
            color = LokapTheme.colors.accentYellow
        )
    }
}

@Composable
private fun SectionHeaderRow(
    title: String,
    badgeCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = LokapTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = LokapTheme.colors.textPrimary
        )
        if (badgeCount > 0) {
            Spacer(modifier = Modifier.width(LokapTheme.spacing.xs))
            BadgeDot(count = badgeCount)
        }
    }
}

@Composable
private fun SearchResultUserRow(
    item: FriendUserItemUi,
    isFriendLimitReached: Boolean,
    onAction: (FriendsAction) -> Unit
) {
    val user = item.user
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LokapTheme.shapes.card)
            .background(LokapTheme.colors.surface)
            .border(1.dp, LokapTheme.colors.outline, LokapTheme.shapes.card)
            .padding(LokapTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarRing(
            avatarUrl = user.avatarUrl,
            displayName = user.displayName,
            size = 44.dp,
            onClick = { onAction(FriendsAction.OpenProfile(user.id)) }
        )

        Spacer(modifier = Modifier.width(LokapTheme.spacing.sm))

        Column(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(
                    onClick = { onAction(FriendsAction.OpenProfile(user.id)) }
                )
        ) {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = LokapTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.bodySmall,
                color = LokapTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(LokapTheme.spacing.xs))

        when (item.status) {
            FriendStatus.NONE -> {
                PinkButton(
                    text = "Kết bạn",
                    leadingIcon = Icons.Default.Add,
                    enabled = !isFriendLimitReached,
                    onClick = { onAction(FriendsAction.SendRequest(user.id)) }
                )
            }

            FriendStatus.REQUEST_SENT -> {
                GhostButton(
                    text = "Huỷ",
                    onClick = { onAction(FriendsAction.CancelSentRequest(user.id)) }
                )
            }

            FriendStatus.REQUEST_RECEIVED -> {
                Row(horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.xs)) {
                    PinkButton(
                        text = "Chấp nhận",
                        enabled = !isFriendLimitReached,
                        onClick = { onAction(FriendsAction.AcceptRequest(user.id)) }
                    )
                    GhostButton(
                        text = "Từ chối",
                        onClick = { onAction(FriendsAction.DeclineRequest(user.id)) }
                    )
                }
            }

            FriendStatus.FRIENDS -> {
                GhostButton(
                    text = "Bạn bè",
                    onClick = { onAction(FriendsAction.OpenProfile(user.id)) }
                )
            }

            FriendStatus.BLOCKED -> {
                GhostButton(
                    text = "Bỏ chặn",
                    onClick = { onAction(FriendsAction.UnblockUser(user.id)) }
                )
            }
        }
    }
}

@Composable
private fun IncomingRequestRow(
    user: User,
    isFriendLimitReached: Boolean,
    onOpenProfile: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LokapTheme.shapes.card)
            .background(LokapTheme.colors.surface)
            .border(1.dp, LokapTheme.colors.primarySoft, LokapTheme.shapes.card)
            .padding(LokapTheme.spacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarRing(
                avatarUrl = user.avatarUrl,
                displayName = user.displayName,
                size = 44.dp,
                hasNewPost = true,
                onClick = onOpenProfile
            )

            Spacer(modifier = Modifier.width(LokapTheme.spacing.sm))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(onClick = onOpenProfile)
            ) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = LokapTheme.colors.textPrimary
                )
                Text(
                    text = "@${user.username} muốn kết bạn",
                    style = MaterialTheme.typography.bodySmall,
                    color = LokapTheme.colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
        ) {
            PinkButton(
                text = "Chấp nhận",
                leadingIcon = Icons.Default.Check,
                enabled = !isFriendLimitReached,
                onClick = onAccept,
                modifier = Modifier.weight(1f)
            )
            GhostButton(
                text = "Từ chối",
                leadingIcon = Icons.Default.Close,
                onClick = onDecline,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SentRequestRow(
    user: User,
    onOpenProfile: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LokapTheme.shapes.card)
            .background(LokapTheme.colors.surface)
            .border(1.dp, LokapTheme.colors.outline, LokapTheme.shapes.card)
            .padding(LokapTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarRing(
            avatarUrl = user.avatarUrl,
            displayName = user.displayName,
            size = 44.dp,
            onClick = onOpenProfile
        )

        Spacer(modifier = Modifier.width(LokapTheme.spacing.sm))

        Column(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(onClick = onOpenProfile)
        ) {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = LokapTheme.colors.textPrimary
            )
            Text(
                text = "@${user.username} · Đang chờ phản hồi",
                style = MaterialTheme.typography.bodySmall,
                color = LokapTheme.colors.textSecondary
            )
        }

        GhostButton(
            text = "Huỷ",
            onClick = onCancel
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FriendRowItem(
    user: User,
    onClick: () -> Unit,
    onRequestUnfriend: () -> Unit,
    onRequestBlock: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(LokapTheme.shapes.card)
                .background(LokapTheme.colors.surface)
                .border(1.dp, LokapTheme.colors.outline, LokapTheme.shapes.card)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true }
                )
                .padding(LokapTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarRing(
                avatarUrl = user.avatarUrl,
                displayName = user.displayName,
                size = 44.dp,
                onClick = onClick
            )

            Spacer(modifier = Modifier.width(LokapTheme.spacing.sm))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = LokapTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${user.username} · Giữ lâu để tuỳ chọn",
                    style = MaterialTheme.typography.bodySmall,
                    color = LokapTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Tuỳ chọn bạn bè",
                    tint = LokapTheme.colors.textSecondary
                )
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            modifier = Modifier.background(LokapTheme.colors.surfaceHigh)
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Huỷ kết bạn",
                        color = LokapTheme.colors.textPrimary
                    )
                },
                onClick = {
                    menuExpanded = false
                    onRequestUnfriend()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Chặn người này",
                        color = LokapTheme.colors.error
                    )
                },
                onClick = {
                    menuExpanded = false
                    onRequestBlock()
                }
            )
        }
    }
}

@Composable
private fun FriendConfirmationDialog(
    pending: PendingFriendConfirmation,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val isBlock = pending.actionType == FriendConfirmActionType.BLOCK
    val title = if (isBlock) {
        "Chặn ${pending.user.displayName}?"
    } else {
        "Huỷ kết bạn với ${pending.user.displayName}?"
    }
    val description = if (isBlock) {
        "Người này sẽ không thể gửi ảnh Locket, nhắn tin hay tìm thấy hồ sơ của bạn nữa. Bạn có thể bỏ chặn bất cứ lúc nào trong phần Cài đặt."
    } else {
        "Hai bạn sẽ không còn nhận được ảnh mới của nhau trên màn hình chính nữa."
    }
    val confirmLabel = if (isBlock) "Chặn ngay" else "Huỷ kết bạn"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LokapTheme.colors.surface,
        titleContentColor = LokapTheme.colors.textPrimary,
        textContentColor = LokapTheme.colors.textSecondary,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Giữ lại",
                    color = LokapTheme.colors.textSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    color = LokapTheme.colors.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
private fun FriendsLoadingList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
    ) {
        Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))
        repeat(6) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                shape = LokapTheme.shapes.card
            )
        }
    }
}

@Preview(name = "Friends - Đầy đủ danh sách", showBackground = true)
@Composable
private fun FriendsPopulatedPreview() {
    val sampleUsers = SampleSocialData.directoryUsers
    LokapTheme {
        FriendsScreen(
            uiState = FriendsUiState(
                isLoading = false,
                incomingRequests = sampleUsers.take(2),
                sentRequests = sampleUsers.drop(2).take(1),
                friends = sampleUsers.drop(3).take(4)
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Friends - Đủ 20/20 bạn bè", showBackground = true)
@Composable
private fun FriendsMaxLimitPreview() {
    val twentyFriends = (1..20).map { idx ->
        User(
            id = "u_$idx",
            username = "banthen_$idx",
            displayName = "Bạn Thân #$idx",
            avatarUrl = null
        )
    }
    LokapTheme {
        FriendsScreen(
            uiState = FriendsUiState(
                isLoading = false,
                incomingRequests = SampleSocialData.directoryUsers.take(1),
                friends = twentyFriends,
                maxFriends = 20
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Friends - Không tìm thấy username", showBackground = true)
@Composable
private fun FriendsEmptySearchPreview() {
    LokapTheme {
        FriendsScreen(
            uiState = FriendsUiState(
                isLoading = false,
                searchQuery = "khong_ton_tai_99",
                debouncedQuery = "khong_ton_tai_99",
                searchResults = emptyList()
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Friends - Chưa có bạn", showBackground = true)
@Composable
private fun FriendsNoFriendsPreview() {
    LokapTheme {
        FriendsScreen(
            uiState = FriendsUiState(
                isLoading = false,
                incomingRequests = emptyList(),
                sentRequests = emptyList(),
                friends = emptyList()
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Friends - Đang tải & Lỗi", showBackground = true)
@Composable
private fun FriendsErrorPreview() {
    LokapTheme {
        FriendsScreen(
            uiState = FriendsUiState(
                isLoading = false,
                errorMessage = "Mất kết nối tới máy chủ Lokap."
            ),
            onAction = {}
        )
    }
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/settings/SettingsContract.kt
package vn.nhom2.lokap.feature.settings

import androidx.compose.runtime.Immutable
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.User

@Immutable
data class SettingsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val defaultLocationLevel: LocationLevel = LocationLevel.PROVINCE,
    val blockedUsers: List<User> = emptyList(),
    val notifyNewPhotos: Boolean = true,
    val notifyReactions: Boolean = true,
    val notifyMessages: Boolean = true,
    val notifyFriendRequests: Boolean = true,
    val hasNotificationPermission: Boolean = true,
    val showLogoutDialog: Boolean = false,
    val showDeleteAccountDialog: Boolean = false,
    val deleteConfirmTextInput: String = "",
    val appVersionLabel: String = "1.0.0"
) {
    val isDeleteConfirmEnabled: Boolean
        get() = deleteConfirmTextInput.trim() == "XOÁ"
}

sealed interface SettingsAction {
    data object NavigateBack : SettingsAction
    data class SelectLocationLevel(val level: LocationLevel) : SettingsAction
    data class UnblockUser(val userId: String) : SettingsAction

    data class ToggleNotifyNewPhotos(val enabled: Boolean) : SettingsAction
    data class ToggleNotifyReactions(val enabled: Boolean) : SettingsAction
    data class ToggleNotifyMessages(val enabled: Boolean) : SettingsAction
    data class ToggleNotifyFriendRequests(val enabled: Boolean) : SettingsAction
    data class UpdateNotificationPermissionState(val granted: Boolean) : SettingsAction
    data object ClickGrantNotificationPermission : SettingsAction

    data object OpenLogoutDialog : SettingsAction
    data object DismissLogoutDialog : SettingsAction
    data object ConfirmLogout : SettingsAction

    data object OpenDeleteAccountDialog : SettingsAction
    data object DismissDeleteAccountDialog : SettingsAction
    data class ChangeDeleteConfirmText(val text: String) : SettingsAction
    data object ConfirmDeleteAccount : SettingsAction
}

sealed interface SettingsEffect {
    data object NavigateBack : SettingsEffect
    data object RequestNotificationPermissionOrOpenSettings : SettingsEffect
    data class ShowMessage(val message: String) : SettingsEffect
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/settings/SettingsViewModel.kt
package vn.nhom2.lokap.feature.settings

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
import vn.nhom2.lokap.data.fake.SampleSocialData
import vn.nhom2.lokap.domain.model.FriendStatus

class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects: Flow<SettingsEffect> = _effects.receiveAsFlow()

    init {
        observeSettingsAndBlockedUsers()
    }

    private fun observeSettingsAndBlockedUsers() {
        viewModelScope.launch {
            combine(
                SampleSocialData.settings,
                SampleSocialData.friendStatuses
            ) { settings, statuses ->
                settings to statuses
            }.collect { (settings, statuses) ->
                val blocked = SampleSocialData.directoryUsers.filter { user ->
                    statuses[user.id] == FriendStatus.BLOCKED
                }
                _uiState.update { prev ->
                    prev.copy(
                        isLoading = false,
                        defaultLocationLevel = settings.defaultLocationLevel,
                        notifyNewPhotos = settings.notifyNewPhotos,
                        notifyReactions = settings.notifyReactions,
                        notifyMessages = settings.notifyMessages,
                        notifyFriendRequests = settings.notifyFriendRequests,
                        blockedUsers = blocked
                    )
                }
            }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.NavigateBack -> {
                _effects.trySend(SettingsEffect.NavigateBack)
            }

            is SettingsAction.SelectLocationLevel -> {
                SampleSocialData.updateSettings {
                    it.copy(defaultLocationLevel = action.level)
                }
                _effects.trySend(
                    SettingsEffect.ShowMessage("Đã đặt mức vị trí mặc định: ${action.level.labelVi}")
                )
            }

            is SettingsAction.UnblockUser -> {
                val target = _uiState.value.blockedUsers.find { it.id == action.userId }
                SampleSocialData.unblockUser(action.userId)
                _effects.trySend(
                    SettingsEffect.ShowMessage(
                        "Đã bỏ chặn ${target?.displayName ?: "người dùng"}"
                    )
                )
            }

            is SettingsAction.ToggleNotifyNewPhotos -> {
                SampleSocialData.updateSettings { it.copy(notifyNewPhotos = action.enabled) }
            }

            is SettingsAction.ToggleNotifyReactions -> {
                SampleSocialData.updateSettings { it.copy(notifyReactions = action.enabled) }
            }

            is SettingsAction.ToggleNotifyMessages -> {
                SampleSocialData.updateSettings { it.copy(notifyMessages = action.enabled) }
            }

            is SettingsAction.ToggleNotifyFriendRequests -> {
                SampleSocialData.updateSettings { it.copy(notifyFriendRequests = action.enabled) }
            }

            is SettingsAction.UpdateNotificationPermissionState -> {
                _uiState.update { it.copy(hasNotificationPermission = action.granted) }
            }

            SettingsAction.ClickGrantNotificationPermission -> {
                _effects.trySend(SettingsEffect.RequestNotificationPermissionOrOpenSettings)
            }

            SettingsAction.OpenLogoutDialog -> {
                _uiState.update { it.copy(showLogoutDialog = true) }
            }

            SettingsAction.DismissLogoutDialog -> {
                _uiState.update { it.copy(showLogoutDialog = false) }
            }

            SettingsAction.ConfirmLogout -> {
                _uiState.update { it.copy(showLogoutDialog = false) }
                _effects.trySend(SettingsEffect.ShowMessage("Đã đăng xuất khỏi tài khoản"))
                _effects.trySend(SettingsEffect.NavigateBack)
            }

            SettingsAction.OpenDeleteAccountDialog -> {
                _uiState.update {
                    it.copy(
                        showDeleteAccountDialog = true,
                        deleteConfirmTextInput = ""
                    )
                }
            }

            SettingsAction.DismissDeleteAccountDialog -> {
                _uiState.update {
                    it.copy(
                        showDeleteAccountDialog = false,
                        deleteConfirmTextInput = ""
                    )
                }
            }

            is SettingsAction.ChangeDeleteConfirmText -> {
                _uiState.update { it.copy(deleteConfirmTextInput = action.text) }
            }

            SettingsAction.ConfirmDeleteAccount -> {
                if (!_uiState.value.isDeleteConfirmEnabled) return
                SampleSocialData.resetAccountForDemo()
                _uiState.update {
                    it.copy(
                        showDeleteAccountDialog = false,
                        deleteConfirmTextInput = ""
                    )
                }
                _effects.trySend(
                    SettingsEffect.ShowMessage("Tài khoản, ảnh và tin nhắn của bạn đã được xoá vĩnh viễn")
                )
                _effects.trySend(SettingsEffect.NavigateBack)
            }
        }
    }
}
```

```kotlin
// FILE: app/src/main/java/vn/nhom2/lokap/feature/settings/SettingsScreen.kt
package vn.nhom2.lokap.feature.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import vn.nhom2.lokap.core.designsystem.component.AvatarRing
import vn.nhom2.lokap.core.designsystem.component.GhostButton
import vn.nhom2.lokap.core.designsystem.component.PinkButton
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.core.ui.theme.LokapTheme
import vn.nhom2.lokap.data.fake.SampleSocialData
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.User

fun NavGraphBuilder.settingsScreen(navController: NavController) {
    composable<Route.Settings> {
        SettingsRoute(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}

@Composable
fun SettingsRoute(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    val appVersionLabel = remember(context) { resolveAppVersion(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onAction(SettingsAction.UpdateNotificationPermissionState(granted))
        if (!granted) {
            openAppNotificationSettings(context)
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onAction(
                    SettingsAction.UpdateNotificationPermissionState(
                        hasPostNotificationsPermission(context)
                    )
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SettingsEffect.NavigateBack -> onNavigateBack()
                SettingsEffect.RequestNotificationPermissionOrOpenSettings -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        openAppNotificationSettings(context)
                    }
                }
                is SettingsEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    SettingsScreen(
        uiState = uiState.copy(appVersionLabel = appVersionLabel),
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(LokapTheme.colors.background)
            .systemBarsPadding(),
        containerColor = LokapTheme.colors.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = LokapTheme.spacing.sm,
                        vertical = LokapTheme.spacing.xs
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onAction(SettingsAction.NavigateBack) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = LokapTheme.colors.textPrimary
                    )
                }
                Text(
                    text = "Cài đặt",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LokapTheme.colors.textPrimary
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = LokapTheme.spacing.md,
                end = LokapTheme.spacing.md,
                top = LokapTheme.spacing.sm,
                bottom = LokapTheme.spacing.xxl
            ),
            verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.md)
        ) {
            // Nhóm 1: Riêng tư
            item {
                SettingsGroupCard(title = "Riêng tư") {
                    Text(
                        text = "Mức vị trí mặc định khi chụp ảnh",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = LokapTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))
                    Text(
                        text = "Chọn độ chi tiết địa điểm gắn lên ảnh công khai trên bản đồ 34 tỉnh thành.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LokapTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

                    LocationLevel.entries.forEach { level ->
                        val selected = uiState.defaultLocationLevel == level
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(LokapTheme.shapes.card)
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = { onAction(SettingsAction.SelectLocationLevel(level)) }
                                )
                                .padding(vertical = LokapTheme.spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = LokapTheme.colors.primary,
                                    unselectedColor = LokapTheme.colors.textSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(LokapTheme.spacing.sm))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = level.labelVi,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) {
                                        LokapTheme.colors.primary
                                    } else {
                                        LokapTheme.colors.textPrimary
                                    }
                                )
                                Text(
                                    text = locationLevelDescriptionVi(level),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LokapTheme.colors.textSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(LokapTheme.spacing.md))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(LokapTheme.colors.outline)
                    )

                    Spacer(modifier = Modifier.height(LokapTheme.spacing.md))

                    Text(
                        text = "Người đã chặn (${uiState.blockedUsers.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = LokapTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))

                    if (uiState.blockedUsers.isEmpty()) {
                        Text(
                            text = "Bạn chưa chặn ai trên Lokap.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LokapTheme.colors.textSecondary
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
                        ) {
                            uiState.blockedUsers.forEach { blockedUser ->
                                BlockedUserItemRow(
                                    user = blockedUser,
                                    onUnblock = { onAction(SettingsAction.UnblockUser(blockedUser.id)) }
                                )
                            }
                        }
                    }
                }
            }

            // Nhóm 2: Thông báo
            item {
                SettingsGroupCard(title = "Thông báo") {
                    if (!uiState.hasNotificationPermission) {
                        NotificationPermissionWarningCard(
                            onGrantClick = {
                                onAction(SettingsAction.ClickGrantNotificationPermission)
                            }
                        )
                        Spacer(modifier = Modifier.height(LokapTheme.spacing.md))
                    }

                    NotificationSwitchRow(
                        title = "Ảnh mới từ bạn bè",
                        subtitle = "Báo ngay khi hội bạn thân vừa thả ảnh mới",
                        checked = uiState.notifyNewPhotos,
                        onCheckedChange = { onAction(SettingsAction.ToggleNotifyNewPhotos(it)) }
                    )

                    NotificationSwitchRow(
                        title = "Cảm xúc",
                        subtitle = "Khi có người thả tim hoặc reaction vào ảnh của bạn",
                        checked = uiState.notifyReactions,
                        onCheckedChange = { onAction(SettingsAction.ToggleNotifyReactions(it)) }
                    )

                    NotificationSwitchRow(
                        title = "Tin nhắn",
                        subtitle = "Tin nhắn phản hồi trực tiếp từ khoảnh khắc",
                        checked = uiState.notifyMessages,
                        onCheckedChange = { onAction(SettingsAction.ToggleNotifyMessages(it)) }
                    )

                    NotificationSwitchRow(
                        title = "Lời mời kết bạn",
                        subtitle = "Khi có người tìm thấy bạn và gửi lời mời kết bạn",
                        checked = uiState.notifyFriendRequests,
                        onCheckedChange = { onAction(SettingsAction.ToggleNotifyFriendRequests(it)) }
                    )
                }
            }

            // Nhóm 3: Tài khoản
            item {
                SettingsGroupCard(title = "Tài khoản") {
                    GhostButton(
                        text = "Đăng xuất",
                        onClick = { onAction(SettingsAction.OpenLogoutDialog) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(LokapTheme.shapes.pill)
                            .border(
                                width = 1.dp,
                                color = LokapTheme.colors.error,
                                shape = LokapTheme.shapes.pill
                            )
                            .clickable { onAction(SettingsAction.OpenDeleteAccountDialog) }
                            .padding(vertical = LokapTheme.spacing.sm),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Xoá tài khoản vĩnh viễn",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = LokapTheme.colors.error
                        )
                    }
                }
            }

            // Phiên bản App cuối trang
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = LokapTheme.spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Lokap · Bản đồ khoảnh khắc Việt Nam",
                        style = MaterialTheme.typography.labelMedium,
                        color = LokapTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Phiên bản ${uiState.appVersionLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = LokapTheme.colors.textTertiary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (uiState.showLogoutDialog) {
        LogoutConfirmDialog(
            onDismiss = { onAction(SettingsAction.DismissLogoutDialog) },
            onConfirm = { onAction(SettingsAction.ConfirmLogout) }
        )
    }

    if (uiState.showDeleteAccountDialog) {
        DeleteAccountConfirmDialog(
            confirmInput = uiState.deleteConfirmTextInput,
            isConfirmEnabled = uiState.isDeleteConfirmEnabled,
            onInputChange = { onAction(SettingsAction.ChangeDeleteConfirmText(it)) },
            onDismiss = { onAction(SettingsAction.DismissDeleteAccountDialog) },
            onConfirm = { onAction(SettingsAction.ConfirmDeleteAccount) }
        )
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LokapTheme.shapes.card)
            .background(LokapTheme.colors.surface)
            .border(
                width = 1.dp,
                color = LokapTheme.colors.outline,
                shape = LokapTheme.shapes.card
            )
            .padding(LokapTheme.spacing.md)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = LokapTheme.colors.primary
        )
        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))
        content()
    }
}

@Composable
private fun BlockedUserItemRow(
    user: User,
    onUnblock: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LokapTheme.shapes.card)
            .background(LokapTheme.colors.surfaceHigh)
            .padding(LokapTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarRing(
            avatarUrl = user.avatarUrl,
            displayName = user.displayName,
            size = 40.dp
        )
        Spacer(modifier = Modifier.width(LokapTheme.spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = LokapTheme.colors.textPrimary
            )
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.labelSmall,
                color = LokapTheme.colors.textSecondary
            )
        }
        GhostButton(
            text = "Bỏ chặn",
            onClick = onUnblock
        )
    }
}

@Composable
private fun NotificationPermissionWarningCard(
    onGrantClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LokapTheme.shapes.card)
            .background(LokapTheme.colors.surfaceHigh)
            .border(
                width = 1.dp,
                color = LokapTheme.colors.accentYellow,
                shape = LokapTheme.shapes.card
            )
            .padding(LokapTheme.spacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = LokapTheme.colors.accentYellow
            )
            Spacer(modifier = Modifier.width(LokapTheme.spacing.sm))
            Text(
                text = "Chưa bật quyền thông báo",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = LokapTheme.colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))

        Text(
            text = "Cấp quyền thông báo để không bỏ lỡ khoảnh khắc mới và lời nhắn từ bạn bè trên Lokap.",
            style = MaterialTheme.typography.bodySmall,
            color = LokapTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(LokapTheme.spacing.sm))

        PinkButton(
            text = "Cấp quyền",
            leadingIcon = Icons.Default.Notifications,
            onClick = onGrantClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun NotificationSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = LokapTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = LokapTheme.colors.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = LokapTheme.colors.textSecondary
            )
        }
        Spacer(modifier = Modifier.width(LokapTheme.spacing.sm))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LokapTheme.colors.onPrimary,
                checkedTrackColor = LokapTheme.colors.primary,
                uncheckedThumbColor = LokapTheme.colors.textSecondary,
                uncheckedTrackColor = LokapTheme.colors.surfaceHigh,
                uncheckedBorderColor = LokapTheme.colors.outline
            )
        )
    }
}

@Composable
private fun LogoutConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LokapTheme.colors.surface,
        titleContentColor = LokapTheme.colors.textPrimary,
        textContentColor = LokapTheme.colors.textSecondary,
        title = {
            Text(
                text = "Đăng xuất khỏi Lokap?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Bạn vẫn có thể đăng nhập lại bất cứ lúc nào để xem lại hành trình 34 tỉnh thành của mình.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Ở lại",
                    color = LokapTheme.colors.textSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Đăng xuất",
                    color = LokapTheme.colors.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
private fun DeleteAccountConfirmDialog(
    confirmInput: String,
    isConfirmEnabled: Boolean,
    onInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LokapTheme.colors.surface,
        titleContentColor = LokapTheme.colors.textPrimary,
        textContentColor = LokapTheme.colors.textSecondary,
        title = {
            Text(
                text = "Xoá tài khoản vĩnh viễn?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LokapTheme.colors.error
            )
        },
        text = {
            Column {
                Text(
                    text = "Hành động này không thể hoàn tác. Toàn bộ ảnh đã chụp, dấu chân bản đồ và tin nhắn của bạn sẽ bị xoá vĩnh viễn khỏi máy chủ Lokap.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LokapTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(LokapTheme.spacing.md))

                Text(
                    text = "Nhập chính xác chữ XOÁ (viết hoa) để xác nhận:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LokapTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(LokapTheme.spacing.xs))

                OutlinedTextField(
                    value = confirmInput,
                    onValueChange = onInputChange,
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "XOÁ",
                            color = LokapTheme.colors.textTertiary
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LokapTheme.colors.error,
                        unfocusedBorderColor = LokapTheme.colors.outline,
                        cursorColor = LokapTheme.colors.error,
                        focusedTextColor = LokapTheme.colors.textPrimary,
                        unfocusedTextColor = LokapTheme.colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Huỷ",
                    color = LokapTheme.colors.textSecondary
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = isConfirmEnabled
            ) {
                Text(
                    text = "Xoá vĩnh viễn",
                    color = if (isConfirmEnabled) {
                        LokapTheme.colors.error
                    } else {
                        LokapTheme.colors.textTertiary
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

private fun locationLevelDescriptionVi(level: LocationLevel): String {
    return when (level) {
        LocationLevel.PLACE -> "Hiện tên quán, địa danh cụ thể nơi bạn đứng"
        LocationLevel.WARD -> "Chỉ hiện cấp Phường / Xã"
        LocationLevel.PROVINCE -> "Chỉ gom vào bong bóng Tỉnh / Thành phố"
        LocationLevel.COUNTRY -> "Ẩn chi tiết tỉnh, chỉ hiện Việt Nam"
    }
}

private fun hasPostNotificationsPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun openAppNotificationSettings(context: Context) {
    val intent = Intent().apply {
        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        val fallback = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(fallback) }
    }
}

private fun resolveAppVersion(context: Context): String {
    val fromBuildConfig = runCatching {
        val clazz = Class.forName("vn.nhom2.lokap.BuildConfig")
        val vName = clazz.getField("VERSION_NAME").get(null) as? String
        val vCode = clazz.getField("VERSION_CODE").get(null) as? Number
        if (!vName.isNullOrBlank()) {
            if (vCode != null) "$vName ($vCode)" else vName
        } else {
            null
        }
    }.getOrNull()

    if (fromBuildConfig != null) return fromBuildConfig

    return runCatching {
        val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        val versionName = pkgInfo.versionName ?: "1.0.0"
        val versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo)
        "$versionName ($versionCode)"
    }.getOrDefault("1.0.0 (1)")
}

@Preview(name = "Settings - Mặc định (Chưa cấp quyền TB)", showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    LokapTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                defaultLocationLevel = LocationLevel.PROVINCE,
                blockedUsers = SampleSocialData.directoryUsers.take(1),
                hasNotificationPermission = false,
                appVersionLabel = "1.2.0 (24)"
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Settings - Hộp thoại Xoá tài khoản", showBackground = true)
@Composable
private fun SettingsDeleteAccountDialogPreview() {
    LokapTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                hasNotificationPermission = true,
                showDeleteAccountDialog = true,
                deleteConfirmTextInput = "XOÁ"
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Settings - Hộp thoại Đăng xuất", showBackground = true)
@Composable
private fun SettingsLogoutDialogPreview() {
    LokapTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                hasNotificationPermission = true,
                showLogoutDialog = true
            ),
            onAction = {}
        )
    }
}
```

### 1. Import và dòng gọi cần thêm vào `AppNavHost.kt`

```kotlin
// Thêm các dòng import vào đầu file AppNavHost.kt:
import vn.nhom2.lokap.feature.friends.friendsScreen
import vn.nhom2.lokap.feature.profile.profileScreen
import vn.nhom2.lokap.feature.settings.settingsScreen

// Bên trong khối NavHost(navController = navController, startDestination = ...) { ... }:
profileScreen(navController)
friendsScreen(navController)
settingsScreen(navController)
```

### 2. Dependency mới
- **Không cần thêm dependency mới** (toàn bộ 3 màn hình sử dụng trực tiếp các thư viện đã có sẵn trong project: `androidx.navigation:navigation-compose`, `androidx.lifecycle:lifecycle-viewmodel-compose`, `androidx.lifecycle:lifecycle-runtime-compose`, `io.coil-kt.coil3:coil-compose`, `androidx.compose.material:material-icons-core`, và `androidx.compose.material3:material3`).