package vn.nhom2.lokap.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.data.fake.FakeNotificationStore
import vn.nhom2.lokap.data.fake.FakePostStore
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleData2
import vn.nhom2.lokap.domain.model.*
class FeedViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val _effects = Channel<FeedEffect>(Channel.BUFFERED)
    val effects: Flow<FeedEffect> = _effects.receiveAsFlow()

    init {
        _uiState.update { it.copy(searchResults = SampleData.users.filter { u -> u.id != SampleData.currentUser.id }) }
        viewModelScope.launch {
            FakeNotificationStore.unreadCount.collect { n -> _uiState.update { it.copy(unreadNotificationsCount = n) } }
        }
        viewModelScope.launch {
            FakePostStore.posts.collect { allPosts ->
                updatePostsState(allPosts)
            }
        }
        viewModelScope.launch {
            vn.nhom2.lokap.data.fake.FakeCommentStore.observeAllCounts().collect { counts ->
                _uiState.update { it.copy(commentsCountMap = counts) }
            }
        }
    }

    private var cachedPublicPosts: List<Post> = emptyList()

    private fun updatePostsState(allPosts: List<Post>) {
        val now = SampleData.NOW
        val meId = SampleData.currentUser.id

        val friendIds = SampleData.users.map { it.id }.toSet()
        val friendFeed = allPosts
            .filter { post -> post.author.id == meId || post.author.id in friendIds }
            .sortedByDescending { it.takenAt }

        val visiblePublicPosts = allPosts
            .filter { post ->
                post.audience == Audience.PUBLIC &&
                    post.sendState == PostSendState.SENT &&
                    (post.publicVisibleAt == null || !post.publicVisibleAt.isAfter(now))
            }
            .sortedByDescending { it.takenAt }

        cachedPublicPosts = visiblePublicPosts
        applyProvinceFilter(visiblePublicPosts, _uiState.value.currentProvinceName, friendFeed)
    }

    private fun applyProvinceFilter(
        publicPosts: List<Post>,
        provinceName: String,
        friendFeed: List<Post> = _uiState.value.friendPosts
    ) {
        val q = provinceName.trim()
        val isAll = q.isEmpty() || q.equals(NATIONWIDE_VIETNAM, ignoreCase = true) ||
            q.equals("Việt Nam", ignoreCase = true) || q.equals("Toàn quốc", ignoreCase = true)

        val filtered = if (isAll) {
            publicPosts
        } else {
            val lowerQ = q.lowercase()
            publicPosts.filter { post ->
                val pName = post.location.provinceName.orEmpty().lowercase()
                val place = post.location.placeName.orEmpty().lowercase()
                val ward = post.location.wardName.orEmpty().lowercase()
                val caption = post.caption.orEmpty().lowercase()
                pName.contains(lowerQ) || lowerQ.contains(pName) ||
                    place.contains(lowerQ) || lowerQ.contains(place) ||
                    ward.contains(lowerQ) || lowerQ.contains(ward) ||
                    caption.contains(lowerQ)
            }
        }

        val landmarks = filtered.filter { !it.location.landmarkId.isNullOrBlank() }

        _uiState.update { state ->
            state.copy(
                friendPosts = friendFeed,
                allPublicPosts = publicPosts,
                explorePosts = filtered,
                nearbyPosts = filtered,
                landmarkPosts = landmarks,
                currentProvinceName = if (isAll) NATIONWIDE_VIETNAM else q,
                locationSearchQuery = if (isAll) "" else q
            )
        }
    }

    fun onAction(action: FeedAction) {
        when (action) {
            is FeedAction.OnSelectTab -> {
                _uiState.update { it.copy(selectedTab = action.tab) }
            }

            FeedAction.OnRefresh -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
                    delay(600L)
                    updatePostsState(FakePostStore.posts.value)
                    _uiState.update { it.copy(isRefreshing = false) }
                }
            }

            FeedAction.OnRetry -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    delay(500L)
                    updatePostsState(FakePostStore.posts.value)
                    _uiState.update { it.copy(isLoading = false) }
                }
            }

            FeedAction.OnSwipeDownToCamera -> {
                sendEffect(FeedEffect.NavigateBackToCamera)
            }

            FeedAction.OnNotificationsClick -> {
                sendEffect(FeedEffect.NavigateToNotifications)
            }

            FeedAction.OnSearchClick -> {
                sendEffect(FeedEffect.NavigateToSearch)
            }

            is FeedAction.OnPostClick -> {
                sendEffect(FeedEffect.NavigateToPostDetail(action.postId))
            }

            is FeedAction.OnLocationChipClick -> {
                val (level, code) = action.post.location.toRegionAlbumTarget() ?: return
                sendEffect(FeedEffect.NavigateToRegionAlbum(level = level, code = code))
            }

            is FeedAction.OnAuthorClick -> {
                sendEffect(FeedEffect.NavigateToProfile(action.userId))
            }

            is FeedAction.OnReplyClick -> {
                val convId = SampleData2.resolveConversationIdForUser(action.post.author.id)
                sendEffect(FeedEffect.NavigateToChat(convId))
            }

            is FeedAction.OnReact -> {
                FakePostStore.toggleReaction(action.postId, action.emoji)
            }

            is FeedAction.OnRetrySendPost -> {
                FakePostStore.retrySend(action.postId)
            }

            is FeedAction.OnReportPost -> {
                FakePostStore.removePost(action.postId)
                sendEffect(FeedEffect.ShowMessage("Đã gửi báo cáo và ẩn bài viết"))
            }

            is FeedAction.OnBlockUser -> {
                _uiState.update { it.copy(pendingBlock = BlockTarget(action.userId, action.displayName)) }
            }

            FeedAction.OnDismissBlockDialog -> {
                _uiState.update { it.copy(pendingBlock = null) }
            }

            FeedAction.OnConfirmBlock -> {
                val target = _uiState.value.pendingBlock ?: return
                _uiState.update { it.copy(pendingBlock = null) }
                FakePostStore.removePostsByAuthor(target.userId)
                sendEffect(FeedEffect.ShowMessage("Đã chặn ${target.displayName}"))
            }

            is FeedAction.OnToggleRepost -> {
                val postId = action.postId
                val wasReposted = _uiState.value.repostedPostIds.contains(postId)
                _uiState.update { state ->
                    val next = if (wasReposted) state.repostedPostIds - postId else state.repostedPostIds + postId
                    state.copy(repostedPostIds = next)
                }
                sendEffect(
                    FeedEffect.ShowMessage(
                        if (!wasReposted) "Đã đăng lại lên trang cá nhân" else "Đã huỷ đăng lại"
                    )
                )
            }

            is FeedAction.OnOpenComments -> {
                _uiState.update { it.copy(activeCommentPostId = action.postId) }
            }

            FeedAction.OnDismissComments -> {
                _uiState.update { it.copy(activeCommentPostId = null) }
            }

            is FeedAction.OnAddComment -> {
                if (action.text.isNotBlank()) {
                    vn.nhom2.lokap.data.fake.FakeCommentStore.addComment(action.postId, action.text)
                    sendEffect(FeedEffect.ShowMessage("Đã gửi bình luận"))
                }
            }

            is FeedAction.OnShareLink -> {
                val shareUrl = "https://lokap.vn/p/${action.post.id}"
                sendEffect(FeedEffect.ShowMessage("Đã sao chép liên kết: $shareUrl"))
            }

            is FeedAction.OnShowReactionPicker -> {
                _uiState.update { it.copy(activeReactionPickerPostId = action.postId) }
            }

            FeedAction.OnDismissReactionPicker -> {
                _uiState.update { it.copy(activeReactionPickerPostId = null) }
            }

            is FeedAction.OnSearchQueryChange -> {
                val query = action.query
                val allUsers = SampleData.users.filter { it.id != SampleData.currentUser.id }
                val filtered = if (query.isBlank()) {
                    allUsers
                } else {
                    allUsers.filter { user ->
                        user.displayName.contains(query, ignoreCase = true) ||
                            user.username.contains(query, ignoreCase = true)
                    }
                }
                _uiState.update { it.copy(searchQuery = query, searchResults = filtered) }
            }

            is FeedAction.OnToggleFriendRequest -> {
                val currentStatus = _uiState.value.friendStatuses[action.userId] ?: FriendStatus.NONE
                val nextStatus = when (currentStatus) {
                    FriendStatus.NONE -> FriendStatus.REQUEST_SENT
                    FriendStatus.REQUEST_SENT -> FriendStatus.NONE
                    FriendStatus.FRIENDS -> FriendStatus.NONE
                    else -> FriendStatus.REQUEST_SENT
                }
                _uiState.update { state ->
                    state.copy(friendStatuses = state.friendStatuses + (action.userId to nextStatus))
                }
                sendEffect(
                    FeedEffect.ShowMessage(
                        if (nextStatus == FriendStatus.REQUEST_SENT) "Đã gửi lời mời kết bạn" else "Đã huỷ lời mời kết bạn"
                    )
                )
            }

            is FeedAction.OnSelectProvince -> {
                applyProvinceFilter(cachedPublicPosts.ifEmpty { _uiState.value.allPublicPosts }, action.provinceName)
                _uiState.update { it.copy(isProvincePickerOpen = false, provinceFilterSearch = "") }
            }

            is FeedAction.OnLocationSearchQueryChange -> {
                applyProvinceFilter(cachedPublicPosts.ifEmpty { _uiState.value.allPublicPosts }, action.query)
            }

            FeedAction.OnClearLocationSearch -> {
                applyProvinceFilter(cachedPublicPosts.ifEmpty { _uiState.value.allPublicPosts }, NATIONWIDE_VIETNAM)
            }

            FeedAction.OnOpenProvincePicker -> {
                _uiState.update { it.copy(isProvincePickerOpen = true, provinceFilterSearch = "") }
            }

            FeedAction.OnDismissProvincePicker -> {
                _uiState.update { it.copy(isProvincePickerOpen = false, provinceFilterSearch = "") }
            }

            is FeedAction.OnProvinceFilterSearchChange -> {
                _uiState.update { it.copy(provinceFilterSearch = action.query) }
            }
        }
    }

    private fun sendEffect(effect: FeedEffect) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }
}
