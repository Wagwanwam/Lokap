package vn.nhom2.lokap.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.Collator
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.domain.repository.CommentRepository
import vn.nhom2.lokap.domain.repository.MessagingRepository
import vn.nhom2.lokap.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.model.User
import vn.nhom2.lokap.domain.model.toRegionAlbumTarget
import vn.nhom2.lokap.domain.repository.AuthRepository
import vn.nhom2.lokap.domain.repository.PostRepository
import vn.nhom2.lokap.domain.repository.SocialRepository
import vn.nhom2.lokap.domain.repository.isOffline
import vn.nhom2.lokap.domain.model.RepostState
import vn.nhom2.lokap.feature.post.mergeFriendTimeline
import vn.nhom2.lokap.feature.post.sendPhotoReply

/**
 * Feed đọc bài qua PostRepository (bản Firestore: cache Room hiện ngay, làm mới từ server),
 * bạn bè qua SocialRepository. Bình luận và thông báo theo dõi trực tiếp từ Firestore.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val posts: PostRepository,
    private val social: SocialRepository,
    private val auth: AuthRepository,
    private val comments: CommentRepository,
    private val messages: MessagingRepository,
    private val notifications: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FeedUiState(now = posts.now(), currentUserId = auth.currentUser.value?.id.orEmpty())
    )
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val _effects = Channel<FeedEffect>(Channel.BUFFERED)
    val effects: Flow<FeedEffect> = _effects.receiveAsFlow()

    private var cachedPublicPosts: List<Post> = emptyList()
    /**
     * Id bài public: chỉ bài public có bình luận (Khám phá + bài public của bạn bè ở tab Bạn bè, kể cả đang hẹn giờ).
     * Set nên đổi thứ tự không dựng lại listener.
     */
    private val publicPostIds = MutableStateFlow<Set<String>>(emptySet())
    private val friendPublicPostIds = MutableStateFlow<Set<String>>(emptySet())
    private var searchJob: Job? = null
    /** Đang đăng lại / bỏ (postId → trạng thái muốn tới): bấm liên tục thì bỏ qua tới khi xong. */
    private val pendingReposts = mutableMapOf<String, Boolean>()
    private var graphFriends: List<User> = emptyList()
    private val nameCollator: Collator = Collator.getInstance(Locale.forLanguageTag("vi"))

    init {
        viewModelScope.launch {
            auth.currentUser.collect { me ->
                _uiState.update { it.copy(currentUserId = me?.id.orEmpty()) }
                updateFriendFilterOptions()
            }
        }
        viewModelScope.launch {
            // Tab Bạn bè = bài bạn bè + ảnh công khai mình / bạn bè đã đăng lại
            combine(posts.friendFeed, posts.friendReposts, ::mergeFriendTimeline).collect { list ->
                // Có bài trong máy (Room) thì hiện ngay, không đợi server
                _uiState.update { it.copy(friendPosts = list, isLoading = it.isLoading && list.isEmpty() && it.allPublicPosts.isEmpty()) }
                updateFriendFilterOptions()
                friendPublicPostIds.value = list.filter { it.audience == Audience.PUBLIC }.map { it.id }.toSet()
            }
        }
        viewModelScope.launch {
            posts.publicPosts.collect { list ->
                cachedPublicPosts = list
                applyProvinceFilter(list, _uiState.value.currentProvinceName)
                publicPostIds.value = list.map { it.id }.toSet()
            }
        }
        viewModelScope.launch {
            social.graph.collect { graph ->
                graphFriends = graph.usersWith(FriendStatus.FRIENDS)
                _uiState.update { it.copy(friendStatuses = graph.statuses) }
                updateFriendFilterOptions()
                // Gợi ý suy ra từ graph (bản Firestore): danh sách bạn về muộn thì nạp lại
                if (_uiState.value.searchQuery.isBlank()) searchUsers("")
            }
        }
        viewModelScope.launch {
            notifications.observeUnreadCount().collect { count -> _uiState.update { it.copy(unreadNotificationsCount = count) } }
        }
        viewModelScope.launch {
            // Mỗi bài public một listener chỉ đếm số bình luận; chỉ dựng lại khi tập bài đổi
            combine(publicPostIds, friendPublicPostIds) { a, b -> a + b }.distinctUntilChanged().flatMapLatest { ids ->
                if (ids.isEmpty()) flowOf(emptyMap()) else combine(ids.map { id -> comments.observeCommentCount(id).map { id to it } }) { it.toMap() }
            }.collect { counts -> _uiState.update { it.copy(commentsCountMap = counts) } }
        }
        viewModelScope.launch {
            // Mỗi ảnh công khai một listener đếm lượt đăng lại (cùng tập bài với số bình luận)
            combine(publicPostIds, friendPublicPostIds) { a, b -> a + b }.distinctUntilChanged().flatMapLatest { ids ->
                if (ids.isEmpty()) flowOf(emptyMap()) else combine(ids.map { id -> posts.observeRepostState(id).map { id to it } }) { it.toMap() }
            }.collect { states ->
                states.forEach { (id, s) -> if (pendingReposts[id] == s.isMine) pendingReposts.remove(id) }
                _uiState.update { it.copy(repostStates = states) }
            }
        }
        searchUsers("")
        // Mở màn: dùng cache có hạn (vừa tải ở Bản đồ / Album thì không tải lại)
        refresh(initial = true, force = false)
    }

    /**
     * Dropdown tab Bạn bè: bạn bè trong graph + tác giả đang có bài trong feed (gồm chính mình),
     * mình đứng đầu, còn lại xếp theo tên tiếng Việt. Người đang chọn biến mất (huỷ kết bạn, chặn)
     * thì quay về "Tất cả".
     */
    private fun updateFriendFilterOptions() {
        _uiState.update { state ->
            val me = state.currentUserId
            val options = (graphFriends + state.friendPosts.map { it.repostedBy ?: it.author })
                .distinctBy { it.id }
                .sortedWith(compareBy<User> { it.id != me }.thenComparator { a, b -> nameCollator.compare(a.displayName, b.displayName) })
            val selected = state.selectedFriendId?.takeIf { id -> options.any { it.id == id } }
            state.copy(friendFilterOptions = options, selectedFriendId = selected)
        }
    }

    private fun applyProvinceFilter(publicPosts: List<Post>, provinceName: String) {
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
                isLoading = state.isLoading && publicPosts.isEmpty() && state.friendPosts.isEmpty(),
                allPublicPosts = publicPosts,
                explorePosts = filtered,
                nearbyPosts = filtered,
                landmarkPosts = landmarks,
                currentProvinceName = if (isAll) NATIONWIDE_VIETNAM else q,
                locationSearchQuery = if (isAll) "" else q
            )
        }
    }

    /** Làm mới từ server. Có bài trong máy thì lỗi chỉ hiện banner offline, không che bằng màn lỗi. */
    private fun refresh(initial: Boolean = false, force: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { state ->
                val empty = state.friendPosts.isEmpty() && state.allPublicPosts.isEmpty()
                state.copy(isLoading = initial && empty, isRefreshing = !initial, errorMessage = null, now = posts.now())
            }
            val result = posts.refreshFeeds(force)
            _uiState.update { state ->
                val empty = state.friendPosts.isEmpty() && state.allPublicPosts.isEmpty()
                val error = result.exceptionOrNull()
                state.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isOffline = error?.isOffline == true,
                    errorMessage = if (error != null && empty && !error.isOffline) error.message else null
                )
            }
        }
    }

    fun onAction(action: FeedAction) {
        when (action) {
            is FeedAction.OnSelectTab -> {
                _uiState.update { it.copy(selectedTab = action.tab) }
            }

            is FeedAction.OnSelectFriendFilter -> {
                _uiState.update { it.copy(selectedFriendId = action.userId) }
            }

            FeedAction.OnRefresh -> refresh()

            FeedAction.OnRetry -> refresh(initial = true)

            FeedAction.OnSwipeDownToCamera -> {
                sendEffect(FeedEffect.NavigateBackToCamera)
            }

            FeedAction.OnNotificationsClick -> {
                sendEffect(FeedEffect.NavigateToNotifications)
            }

            FeedAction.OnSearchClick -> {
                sendEffect(FeedEffect.NavigateToSearch)
            }

            is FeedAction.OnLocationChipClick -> {
                val (level, code) = action.post.location.toRegionAlbumTarget() ?: return
                sendEffect(FeedEffect.NavigateToRegionAlbum(level = level, code = code))
            }

            is FeedAction.OnAuthorClick -> {
                sendEffect(FeedEffect.NavigateToProfile(action.userId))
            }

            is FeedAction.OnMessageDraftChange -> _uiState.update { state ->
                val drafts = if (action.text.isEmpty()) state.messageDrafts - action.postId
                else state.messageDrafts + (action.postId to action.text.take(1000))
                state.copy(messageDrafts = drafts)
            }

            is FeedAction.OnSendPhotoMessage -> sendPhotoMessage(action.post)

            is FeedAction.OnReact -> viewModelScope.launch {
                posts.toggleReaction(action.postId, action.emoji)
                    .onFailure { sendEffect(FeedEffect.ShowMessage(it.message ?: "Không thả cảm xúc được")) }
            }

            is FeedAction.OnRetrySendPost -> viewModelScope.launch {
                posts.retrySend(action.postId)
            }

            is FeedAction.OnReportPost -> viewModelScope.launch {
                posts.reportPost(action.postId, reason = "Nội dung không phù hợp").fold(
                    onSuccess = { },
                    onFailure = { sendEffect(FeedEffect.ShowMessage(it.message ?: "Không gửi được báo cáo")) }
                )
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
                viewModelScope.launch {
                    val result = social.block(target.userId)
                    if (result.isSuccess) {
                        // Sau khi chặn: bỏ ngay mọi bài của người đó khỏi máy
                        posts.hidePostsByAuthor(target.userId)
                    } else {
                        sendEffect(FeedEffect.ShowMessage(result.exceptionOrNull()?.message ?: "Không chặn được, thử lại sau"))
                    }
                }
            }

            is FeedAction.OnToggleRepost -> {
                val postId = action.postId
                if (postId in pendingReposts) return
                val target = !(_uiState.value.repostStates[postId] ?: RepostState()).isMine
                pendingReposts[postId] = target
                viewModelScope.launch {
                    // Listener đổi ngay (Firestore ghi trên máy trước); lỗi thì báo, số lượt tự trả lại
                    posts.setReposted(postId, target)
                        .onFailure { sendEffect(FeedEffect.ShowMessage(it.message ?: "Không đăng lại được")) }
                    pendingReposts.remove(postId)
                }
            }

            is FeedAction.OnOpenComments -> {
                _uiState.update { it.copy(activeCommentPostId = action.postId) }
            }

            FeedAction.OnDismissComments -> {
                _uiState.update { it.copy(activeCommentPostId = null) }
            }

            is FeedAction.OnSearchQueryChange -> {
                _uiState.update { it.copy(searchQuery = action.query) }
                searchUsers(action.query)
            }

            is FeedAction.OnToggleFriendRequest -> toggleFriendRequest(action.userId)

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

    /** Gửi tin dưới ảnh (kiểu Locket): ở lại Feed, xong thì báo "Đã gửi"; lỗi thì giữ nội dung đang gõ. */
    private fun sendPhotoMessage(post: Post) {
        val state = _uiState.value
        val text = state.messageDrafts[post.id]?.trim().orEmpty()
        if (text.isEmpty() || post.id in state.sendingMessagePostIds || !state.canMessage(post)) return
        _uiState.update { it.copy(sendingMessagePostIds = it.sendingMessagePostIds + post.id) }
        viewModelScope.launch {
            val result = messages.sendPhotoReply(post, text)
            _uiState.update {
                it.copy(
                    sendingMessagePostIds = it.sendingMessagePostIds - post.id,
                    messageDrafts = if (result.isSuccess) it.messageDrafts - post.id else it.messageDrafts
                )
            }
            sendEffect(FeedEffect.ShowMessage(result.fold({ "Đã gửi cho ${post.author.displayName}" }, { it.message ?: "Không gửi được tin nhắn" })))
        }
    }

    /** Ô tìm bạn trong Feed: chưa gõ thì gợi ý, gõ thì đợi 300 ms rồi hỏi server. */
    private fun searchUsers(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (query.isNotBlank()) delay(300)
            val result = if (query.isBlank()) social.suggestions() else social.search(query)
            result.onSuccess { users -> _uiState.update { it.copy(searchResults = users) } }
        }
    }

    private fun toggleFriendRequest(userId: String) {
        viewModelScope.launch {
            val message: Result<String> = when (social.graph.value.statusOf(userId)) {
                FriendStatus.NONE -> social.sendFriendRequest(userId).map { status ->
                    if (status == FriendStatus.FRIENDS) "Hai bạn đã trở thành bạn bè!" else "Đã gửi lời mời kết bạn"
                }
                FriendStatus.REQUEST_SENT -> social.cancelFriendRequest(userId).map { "Đã huỷ lời mời kết bạn" }
                FriendStatus.REQUEST_RECEIVED -> social.acceptFriendRequest(userId).map { "Hai bạn đã trở thành bạn bè!" }
                FriendStatus.FRIENDS -> social.unfriend(userId).map { "Đã huỷ kết bạn" }
                FriendStatus.BLOCKED -> return@launch
            }
            message.onFailure { sendEffect(FeedEffect.ShowMessage(it.message ?: "Không thực hiện được")) }
        }
    }

    private fun sendEffect(effect: FeedEffect) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }
}
