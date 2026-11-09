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

                val baseUserPosts = allPosts
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

                val visibleUserPosts = if (isMe && baseUserPosts.size < 6) {
                    val extra = SampleData.posts.filter { it.author.id != targetUser.id }
                        .take(9 - baseUserPosts.size)
                        .mapIndexed { idx, p ->
                            p.copy(id = "my_profile_photo_$idx", author = targetUser)
                        }
                    baseUserPosts + extra
                } else {
                    baseUserPosts
                }

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

                val savedPosts = allPosts.filter { post ->
                    post.imageUrl.isNotBlank() && (post.id.hashCode() % 3 == 0)
                }.take(6)

                val repostedPosts = allPosts.filter { post ->
                    post.imageUrl.isNotBlank() && (post.id.hashCode() % 2 != 0)
                }.take(4)

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
                        savedPosts = savedPosts,
                        repostedPosts = repostedPosts,
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
                        editBioInput = user?.bio?.ifBlank { it.bio }.orEmpty(),
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
                val clean = action.value.trim().removePrefix("@")
                val error = if (clean.length < 3) "Username tối thiểu 3 ký tự" else null
                _uiState.update {
                    it.copy(
                        editUsernameInput = clean,
                        editUsernameError = error
                    )
                }
            }

            is ProfileAction.ChangeEditBio -> {
                _uiState.update {
                    it.copy(editBioInput = action.value)
                }
            }

            ProfileAction.SaveEditProfile -> {
                val state = _uiState.value
                val trimmedName = state.editDisplayNameInput.trim()
                val nameError = validateDisplayName(trimmedName)
                if (nameError != null) {
                    _uiState.update { it.copy(editDisplayNameError = nameError) }
                    return
                }
                val cleanUsername = state.editUsernameInput.trim().removePrefix("@")
                if (cleanUsername.length < 3) {
                    _uiState.update { it.copy(editUsernameError = "Username tối thiểu 3 ký tự") }
                    return
                }
                SampleSocialData.updateMyProfile(
                    displayName = trimmedName,
                    username = cleanUsername,
                    bio = state.editBioInput
                )
                _uiState.update {
                    it.copy(
                        isEditSheetOpen = false,
                        bio = state.editBioInput,
                        editDisplayNameError = null,
                        editUsernameError = null,
                        isSavingProfile = false
                    )
                }
                _effects.trySend(ProfileEffect.ShowMessage("Đã lưu thông tin hồ sơ"))
            }

            ProfileAction.ToggleFollow -> {
                _uiState.update { current ->
                    val nowFollowing = !current.isFollowing
                    val delta = if (nowFollowing) 1 else -1
                    current.copy(
                        isFollowing = nowFollowing,
                        followersCount = (current.followersCount + delta).coerceAtLeast(0)
                    )
                }
                val msg = if (_uiState.value.isFollowing) "Đã theo dõi" else "Đã huỷ theo dõi"
                _effects.trySend(ProfileEffect.ShowMessage(msg))
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
