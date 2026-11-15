package vn.nhom2.lokap.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.data.location.ProvinceCatalog
import vn.nhom2.lokap.domain.model.landmarksFromPosts
import vn.nhom2.lokap.domain.repository.MessagingRepository
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.model.MapBubble
import vn.nhom2.lokap.domain.model.MapBubbleType
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.model.Province
import vn.nhom2.lokap.domain.model.User
import vn.nhom2.lokap.domain.repository.AuthRepository
import vn.nhom2.lokap.domain.repository.AuthStoreException
import vn.nhom2.lokap.domain.repository.PostRepository
import vn.nhom2.lokap.domain.repository.SocialGraph
import vn.nhom2.lokap.domain.repository.SocialRepository
import vn.nhom2.lokap.domain.repository.isOffline
import vn.nhom2.lokap.feature.auth.AuthValidators
import vn.nhom2.lokap.feature.map.MapCameraState

/**
 * Hồ sơ: người dùng từ AuthRepository (mình) hoặc SocialRepository (người khác), bài qua PostRepository
 * (server đã lọc theo luật hiển thị), quan hệ bạn bè qua SocialRepository.graph.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val auth: AuthRepository,
    private val messages: MessagingRepository,
    private val social: SocialRepository,
    private val posts: PostRepository
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

    private var loadJob: Job? = null

    private data class Snapshot(
        val userPosts: List<Post>,
        val graph: SocialGraph,
        val user: User?,
        val saved: List<Post>,
        val reposted: List<Post>
    )

    init {
        load()
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val me = auth.currentUser.value
            val targetId = routeUserId ?: me?.id
            if (targetId == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Bạn cần đăng nhập lại") }
                return@launch
            }
            val isMe = targetId == me?.id

            // Người khác: tải hồ sơ + số bạn một lần; lỗi mạng thì báo và cho Thử lại
            var otherUser: User? = null
            var otherFriendCount = 0
            if (!isMe) {
                val loaded = social.getUser(targetId)
                val error = loaded.exceptionOrNull()
                if (error != null) {
                    _uiState.update {
                        it.copy(isLoading = false, isOffline = error.isOffline, errorMessage = error.message)
                    }
                    return@launch
                }
                otherUser = loaded.getOrNull()
                if (otherUser == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = null, isUserNotFound = true, user = null) }
                    return@launch
                }
                otherFriendCount = social.getFriendCount(targetId).getOrDefault(0)
            }

            // Bài trên server: làm mới nền; lỗi mạng chỉ bật cờ offline, vẫn hiện bài đã có trong máy
            launch {
                posts.refreshUserPosts(targetId).onFailure { e -> _uiState.update { it.copy(isOffline = e.isOffline) } }
            }

            val userFlow: Flow<User?> = if (isMe) auth.currentUser else flowOf(otherUser)
            combine(
                posts.observeUserPosts(targetId),
                social.graph,
                userFlow,
                posts.savedPosts,
                posts.repostedPosts
            ) { userPosts, graph, user, saved, reposted ->
                Snapshot(userPosts, graph, user, saved, reposted)
            }.collect { snap ->
                val user = snap.user ?: return@collect // đã đăng xuất
                val provinces = ProvinceCatalog.allProvinces
                val provinceByCode = provinces.associateBy { it.code }

                val postsByProvince = snap.userPosts
                    .mapNotNull { post -> post.location.provinceCode?.let(provinceByCode::get)?.let { it to post } }
                    .groupBy({ it.first }, { it.second })

                val visitedProvinces = postsByProvince.map { (province, postsInProvince) ->
                    VisitedProvinceUi(
                        code = province.code,
                        name = province.name,
                        postCount = postsInProvince.size,
                        photoUrls = postsInProvince.map { it.imageUrl }.take(4),
                        lat = province.lat,
                        lng = province.lng
                    )
                }.sortedByDescending { it.postCount }

                val initialCamera = visitedProvinces.firstOrNull()?.let { first ->
                    MapCameraState(centerLat = first.lat, centerLng = first.lng, zoom = 6.2f)
                } ?: MapCameraState(centerLat = 16.047079, centerLng = 108.206230, zoom = 5.3f)

                _uiState.update { prev ->
                    prev.copy(
                        isLoading = false,
                        errorMessage = null,
                        isUserNotFound = false,
                        isMe = isMe,
                        user = user,
                        friendStatus = if (isMe) FriendStatus.NONE else snap.graph.statusOf(user.id),
                        visitedProvincesCount = visitedProvinces.size,
                        totalProvincesCount = provinces.size,
                        postCount = snap.userPosts.size,
                        friendCount = if (isMe) snap.graph.friendCount else otherFriendCount,
                        isFriendLimitReached = snap.graph.friendCount >= social.maxFriends,
                        bio = user.bio,
                        visiblePosts = snap.userPosts,
                        savedPosts = snap.saved,
                        repostedPosts = snap.reposted,
                        visitedProvinces = visitedProvinces,
                        mapBubbles = provinceBubbles(snap.userPosts, provinces),
                        landmarks = landmarksFromPosts(snap.userPosts),
                        mapCamera = if (prev.isLoading) initialCamera else prev.mapCamera
                    )
                }
            }
        }
    }

    /** Bong bóng tỉnh trên bản đồ "Dấu chân": mỗi tỉnh có bài một bong bóng, ảnh bìa là bài mới nhất. */
    private fun provinceBubbles(userPosts: List<Post>, provinces: List<Province>): List<MapBubble> {
        val postsByProvince = userPosts.groupBy { it.location.provinceCode }
        return provinces.mapNotNull { province ->
            val provincePosts = postsByProvince[province.code].orEmpty()
            if (provincePosts.isEmpty()) return@mapNotNull null
            MapBubble(
                id = "profile_prov_${province.code}",
                type = MapBubbleType.PROVINCE,
                code = province.code,
                label = province.name,
                lat = province.lat,
                lng = province.lng,
                postCount = provincePosts.size,
                coverImageUrl = provincePosts.maxByOrNull { it.takenAt }?.imageUrl,
                hasFriendPosts = true
            )
        }
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            ProfileAction.Retry -> {
                _uiState.update { it.copy(isLoading = true, errorMessage = null, isOffline = false) }
                load()
            }

            ProfileAction.NavigateBack -> {
                _effects.trySend(ProfileEffect.NavigateBack)
            }

            ProfileAction.OpenSettings -> {
                _effects.trySend(ProfileEffect.NavigateToSettings)
            }

            ProfileAction.OpenFriends -> {
                _effects.trySend(ProfileEffect.NavigateToFriends)
            }

            ProfileAction.OpenAddFriend -> {
                _effects.trySend(ProfileEffect.NavigateToAddFriend)
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
                val provinceCode = action.bubble.code
                if (provinceCode != null) {
                    _effects.trySend(ProfileEffect.NavigateToRegionAlbum(provinceCode))
                }
            }

            is ProfileAction.UpdateMapCamera -> {
                _uiState.update { it.copy(mapCamera = action.camera) }
            }

            ProfileAction.OpenEditProfileSheet -> {
                val user = _uiState.value.user
                _uiState.update {
                    it.copy(
                        isEditSheetOpen = true,
                        editDisplayNameInput = user?.displayName.orEmpty(),
                        editUsernameInput = user?.username.orEmpty(),
                        editBioInput = user?.bio.orEmpty(),
                        editDisplayNameError = null,
                        editUsernameError = null
                    )
                }
            }

            ProfileAction.DismissEditProfileSheet -> {
                _uiState.update {
                    it.copy(
                        isEditSheetOpen = false,
                        editDisplayNameError = null,
                        editUsernameError = null,
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

            is ProfileAction.ChangeEditUsername -> {
                // Cùng luật với màn Thiết lập hồ sơ: chữ thường, 3–20 ký tự a–z 0–9 . _
                val clean = action.value.trim().removePrefix("@").lowercase()
                _uiState.update {
                    it.copy(
                        editUsernameInput = clean,
                        editUsernameError = AuthValidators.validateUsernameFormat(clean)
                    )
                }
            }

            is ProfileAction.ChangeEditBio -> {
                // Cùng giới hạn với màn Thiết lập hồ sơ và ràng buộc profiles_bio_length (≤ 60)
                _uiState.update {
                    it.copy(editBioInput = action.value.take(60))
                }
            }

            ProfileAction.SaveEditProfile -> saveProfile()

            ProfileAction.SendFriendRequest -> withTarget { user ->
                social.sendFriendRequest(user.id).map { status ->
                    if (status == FriendStatus.FRIENDS) "Hai bạn đã trở thành bạn bè!" else "Đã gửi lời mời kết bạn"
                }
            }

            ProfileAction.CancelFriendRequest -> withTarget { user ->
                social.cancelFriendRequest(user.id).map { "Đã huỷ lời mời kết bạn" }
            }

            ProfileAction.AcceptFriendRequest -> withTarget { user ->
                social.acceptFriendRequest(user.id).map { "Hai bạn đã trở thành bạn bè!" }
            }

            ProfileAction.DeclineFriendRequest -> withTarget { user ->
                social.declineFriendRequest(user.id).map { "Đã từ chối lời mời" }
            }

            ProfileAction.Unfriend -> withTarget { user ->
                social.unfriend(user.id).map { "Đã huỷ kết bạn với ${user.displayName}" }
            }

            ProfileAction.Unblock -> withTarget { user ->
                social.unblock(user.id).map { "Đã bỏ chặn ${user.displayName}" }
            }

            ProfileAction.MessageUser -> {
                val targetUser = _uiState.value.user ?: return
                viewModelScope.launch { messages.openConversation(targetUser.id).fold(
                    onSuccess = { _effects.send(ProfileEffect.NavigateToChat(it)) },
                    onFailure = { _effects.send(ProfileEffect.ShowMessage(it.message ?: "Không mở được cuộc trò chuyện")) }) }
            }
        }
    }

    /** Thao tác bạn bè với người đang xem: thành công hay lỗi đều báo một câu; trạng thái mới tự về qua graph. */
    private fun withTarget(block: suspend (User) -> Result<String>) {
        val targetUser = _uiState.value.user ?: return
        viewModelScope.launch {
            block(targetUser).onFailure { _effects.trySend(ProfileEffect.ShowMessage(it.message ?: "Không thực hiện được")) }
        }
    }

    private fun saveProfile() {
        val state = _uiState.value
        if (state.isSavingProfile) return
        val trimmedName = state.editDisplayNameInput.trim()
        val nameError = validateDisplayName(trimmedName)
        if (nameError != null) {
            _uiState.update { it.copy(editDisplayNameError = nameError) }
            return
        }
        val cleanUsername = state.editUsernameInput.trim().removePrefix("@").lowercase()
        val usernameError = AuthValidators.validateUsernameFormat(cleanUsername)
        if (usernameError != null) {
            _uiState.update { it.copy(editUsernameError = usernameError) }
            return
        }
        val bioError = AuthValidators.validateBio(state.editBioInput)
        if (bioError != null) {
            _effects.trySend(ProfileEffect.ShowMessage(bioError))
            return
        }
        _uiState.update { it.copy(isSavingProfile = true) }
        viewModelScope.launch {
            auth.updateProfile(displayName = trimmedName, username = cleanUsername, bio = state.editBioInput).fold(
                onSuccess = { updated ->
                    _uiState.update {
                        it.copy(
                            isEditSheetOpen = false,
                            isSavingProfile = false,
                            user = if (it.isMe) updated else it.user,
                            bio = updated.bio,
                            editDisplayNameError = null,
                            editUsernameError = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSavingProfile = false,
                            editUsernameError = if (error is AuthStoreException.UsernameAlreadyTaken) error.message else it.editUsernameError
                        )
                    }
                    if (error !is AuthStoreException.UsernameAlreadyTaken) {
                        _effects.trySend(ProfileEffect.ShowMessage(error.message ?: "Không lưu được, thử lại sau"))
                    }
                }
            )
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
