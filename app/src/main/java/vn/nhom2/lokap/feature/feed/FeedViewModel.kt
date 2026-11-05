package vn.nhom2.lokap.feature.feed

import vn.nhom2.lokap.data.fake.FakeNotificationStore
import vn.nhom2.lokap.domain.model.toRegionAlbumTarget
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
import vn.nhom2.lokap.data.fake.FakePostStore
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleData2
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.model.PostSendState

class FeedViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val _effects = Channel<FeedEffect>(Channel.BUFFERED)
    val effects: Flow<FeedEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            FakeNotificationStore.unreadCount.collect { n -> _uiState.update { it.copy(unreadNotificationsCount = n) } }
        }
        viewModelScope.launch {
            FakePostStore.posts.collect { allPosts ->
                updatePostsState(allPosts)
            }
        }
    }

    private fun updatePostsState(allPosts: List<Post>) {
        val now = SampleData.NOW
        val meId = SampleData.currentUser.id

        // Tab Bạn bè: mọi bài của tôi và của bạn bè, gồm cả bài Public đang hẹn giờ
        // (CLAUDE.md: bạn bè thấy bài public ngay). Dữ liệu giả: mọi user khác đều là bạn,
        // bài DIRECT trong SampleData đều do tôi gửi hoặc gửi cho tôi.
        val friendIds = SampleData.users.map { it.id }.toSet()
        val friendFeed = allPosts
            .filter { post -> post.author.id == meId || post.author.id in friendIds }
            .sortedByDescending { it.takenAt }

        // Tab Khám phá: chỉ lấy bài PUBLIC đã gửi thành công và KHÔNG đang hẹn giờ (publicVisibleAt <= now)
        val visiblePublicPosts = allPosts
            .filter { post ->
                post.audience == Audience.PUBLIC &&
                    post.sendState == PostSendState.SENT &&
                    (post.publicVisibleAt == null || !post.publicVisibleAt.isAfter(now))
            }
            .sortedByDescending { it.takenAt }

        val nearby = visiblePublicPosts.filter { post ->
            post.location.provinceCode == SampleData2.DEFAULT_PROVINCE_CODE
        }

        val landmarks = visiblePublicPosts.filter { post ->
            !post.location.landmarkId.isNullOrBlank()
        }

        _uiState.update { state ->
            state.copy(
                friendPosts = friendFeed,
                nearbyPosts = nearby,
                landmarkPosts = landmarks
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
        }
    }

    private fun sendEffect(effect: FeedEffect) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }
}
