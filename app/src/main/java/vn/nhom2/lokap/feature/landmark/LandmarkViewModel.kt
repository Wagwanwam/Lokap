package vn.nhom2.lokap.feature.landmark

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.model.Landmark
import vn.nhom2.lokap.domain.model.Post
import vn.nhom2.lokap.domain.model.PostSendState
import vn.nhom2.lokap.domain.model.User
import vn.nhom2.lokap.domain.model.arrangeForLandmark
import vn.nhom2.lokap.domain.model.landmarksFromPosts
import vn.nhom2.lokap.domain.model.withPhotos
import vn.nhom2.lokap.domain.repository.AuthRepository
import vn.nhom2.lokap.domain.repository.ImageImporter
import vn.nhom2.lokap.domain.repository.LandmarkRepository
import vn.nhom2.lokap.domain.repository.PostRepository
import vn.nhom2.lokap.domain.repository.SocialRepository

/**
 * Màn Địa danh: ảnh bìa = bài public có nhiều cảm xúc nhất (cùng một ảnh với mọi người), lưới ảnh xếp theo
 * Nổi bật / Mới nhất, lọc Bạn bè / Của tôi. Người dùng đưa ảnh lên bằng camera, thư viện hoặc gắn bài đã đăng.
 */
@HiltViewModel
class LandmarkViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val posts: PostRepository,
    private val landmarks: LandmarkRepository,
    private val social: SocialRepository,
    private val auth: AuthRepository,
    private val images: ImageImporter
) : ViewModel() {

    private val landmarkId: String = runCatching {
        savedStateHandle.toRoute<Route.Landmark>().id
    }.getOrElse {
        savedStateHandle["id"] ?: ""
    }

    private val _uiState = MutableStateFlow(LandmarkUiState(isLoading = true))
    val uiState: StateFlow<LandmarkUiState> = _uiState.asStateFlow()

    private val _effect = Channel<LandmarkEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    // Dữ liệu gốc; UiState được dựng lại từ đây mỗi khi một nguồn đổi
    private var catalogLandmark: Landmark? = null
    private var landmarkPosts: List<Post> = emptyList()
    private var friendIds: Set<String> = emptySet()
    private var me: User? = null
    private var refreshed = false
    private var refreshError: Throwable? = null

    private var refreshJob: Job? = null
    private var attachJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                landmarks.observeLandmark(landmarkId),
                posts.observeLandmarkPosts(landmarkId),
                social.graph,
                auth.currentUser
            ) { landmark, list, graph, user ->
                catalogLandmark = landmark
                landmarkPosts = list
                friendIds = graph.statuses.filterValues { it == FriendStatus.FRIENDS }.keys
                me = user
            }.collect { rebuild() }
        }
        refresh()
    }

    private fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            refreshed = false
            refreshError = null
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val landmark = async { landmarks.refreshLandmark(landmarkId) }
            val photos = async { posts.refreshLandmarkPosts(landmarkId) }
            refreshError = landmark.await().exceptionOrNull() ?: photos.await().exceptionOrNull()
            refreshed = true
            rebuild()
        }
    }

    private fun rebuild() {
        val now = posts.now()
        // Chưa có trong danh mục (offline, danh mục chưa tải) thì dựng tạm từ bài đã gắn địa danh
        val base = catalogLandmark ?: landmarksFromPosts(landmarkPosts, now).firstOrNull()
        val landmark = base?.withPhotos(landmarkPosts, now)
        _uiState.update { state ->
            state.copy(
                isLoading = landmark == null && !refreshed,
                errorMessage = when {
                    landmark != null || !refreshed -> null
                    else -> refreshError?.message ?: "Không tìm thấy địa danh này"
                },
                landmark = landmark,
                provinceName = landmark?.provinceName?.ifBlank { null }
                    ?: landmarkPosts.firstNotNullOfOrNull { it.location.provinceName }.orEmpty(),
                posts = landmarkPosts.arrangeForLandmark(state.tab, friendIds, me?.id),
                totalPostCount = landmarkPosts.size,
                meId = me?.id
            )
        }
    }

    fun onAction(action: LandmarkAction) {
        when (action) {
            is LandmarkAction.OnPostClick -> send(LandmarkEffect.NavigateToPostDetail(action.postId))

            is LandmarkAction.OnPostLongClick -> {
                // Chỉ ảnh của mình mới gỡ được khỏi địa danh
                if (action.post.author.id == me?.id && action.post.sendState == PostSendState.SENT) {
                    _uiState.update { it.copy(detachCandidate = action.post) }
                }
            }

            is LandmarkAction.OnSelectTab -> {
                _uiState.update { it.copy(tab = action.tab) }
                rebuild()
            }

            LandmarkAction.OnAddPhotoClick -> _uiState.update { it.copy(isAddSheetOpen = true) }

            LandmarkAction.OnDismissAddSheet -> _uiState.update { it.copy(isAddSheetOpen = false) }

            LandmarkAction.OnTakePhotoClick -> {
                _uiState.update { it.copy(isAddSheetOpen = false) }
                send(LandmarkEffect.NavigateToCamera(landmarkId))
            }

            LandmarkAction.OnPickFromGalleryClick -> {
                _uiState.update { it.copy(isAddSheetOpen = false) }
                send(LandmarkEffect.LaunchGalleryPicker)
            }

            is LandmarkAction.OnGalleryResult -> importFromGallery(action.uri)

            LandmarkAction.OnAttachExistingClick -> openAttachSheet()

            LandmarkAction.OnDismissAttachSheet -> {
                attachJob?.cancel()
                _uiState.update { it.copy(isAttachSheetOpen = false) }
            }

            is LandmarkAction.OnAttachPost -> attach(action.postId)

            LandmarkAction.OnConfirmDetach -> detach()

            LandmarkAction.OnDismissDetach -> _uiState.update { it.copy(detachCandidate = null) }

            LandmarkAction.OnReportClick -> _uiState.update { it.copy(isReportSheetOpen = true) }

            LandmarkAction.OnDismissReport -> _uiState.update { it.copy(isReportSheetOpen = false) }

            is LandmarkAction.OnSubmitReport -> report(action.reason)

            LandmarkAction.OnRetry -> refresh()

            LandmarkAction.OnBackClick -> send(LandmarkEffect.NavigateBack)
        }
    }

    private fun importFromGallery(uri: String?) {
        if (uri == null || _uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            images.importImage(uri).fold(
                onSuccess = { path -> send(LandmarkEffect.NavigateToComposer(path, landmarkId)) },
                onFailure = { send(LandmarkEffect.ShowMessage(it.message ?: "Không đọc được ảnh này")) }
            )
            _uiState.update { it.copy(isBusy = false) }
        }
    }

    /** Bài public đã gửi của mình, chưa gắn vào địa danh này. */
    private fun openAttachSheet() {
        val meId = me?.id ?: return send(LandmarkEffect.ShowMessage("Bạn cần đăng nhập lại"))
        _uiState.update { it.copy(isAddSheetOpen = false, isAttachSheetOpen = true) }
        attachJob?.cancel()
        attachJob = viewModelScope.launch {
            launch { posts.refreshUserPosts(meId) }
            posts.observeUserPosts(meId).collect { mine ->
                val attachable = mine.filter {
                    it.audience == Audience.PUBLIC && it.sendState == PostSendState.SENT &&
                        it.location.landmarkId != landmarkId && it.imageUrl.isNotBlank()
                }
                _uiState.update { it.copy(attachablePosts = attachable) }
            }
        }
    }

    private fun attach(postId: String) {
        val landmark = _uiState.value.landmark ?: return
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            posts.setPostLandmark(postId, landmark).fold(
                onSuccess = {
                    attachJob?.cancel()
                    _uiState.update { it.copy(isAttachSheetOpen = false) }
                },
                onFailure = { send(LandmarkEffect.ShowMessage(it.message ?: "Không gắn được ảnh, thử lại sau")) }
            )
            _uiState.update { it.copy(isBusy = false) }
        }
    }

    private fun detach() {
        val post = _uiState.value.detachCandidate ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(detachCandidate = null, isBusy = true) }
            posts.setPostLandmark(post.id, null).onFailure {
                send(LandmarkEffect.ShowMessage(it.message ?: "Không gỡ được ảnh, thử lại sau"))
            }
            _uiState.update { it.copy(isBusy = false) }
        }
    }

    private fun report(reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isReportSheetOpen = false) }
            landmarks.reportLandmark(landmarkId, reason).fold(
                onSuccess = { send(LandmarkEffect.NavigateBack) },
                onFailure = { send(LandmarkEffect.ShowMessage(it.message ?: "Không báo cáo được, thử lại sau")) }
            )
        }
    }

    private fun send(effect: LandmarkEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }
}
