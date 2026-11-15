package vn.nhom2.lokap.feature.postdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.domain.repository.CommentRepository
import vn.nhom2.lokap.domain.repository.MessagingRepository
import vn.nhom2.lokap.domain.model.toRegionAlbumTarget
import vn.nhom2.lokap.domain.repository.AuthRepository
import vn.nhom2.lokap.domain.repository.PostRepository
import vn.nhom2.lokap.domain.repository.SocialRepository
import vn.nhom2.lokap.domain.repository.isOffline
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.feature.post.sendPhotoReply

/**
 * Trang 1 ảnh: theo dõi bài trong PostRepository (bản Firestore: Room + làm mới 1 lần từ server).
 * Bài của mình có thêm Sửa lời nhắn / Xoá bài. Số bình luận theo dõi từ Firestore; danh sách nằm ở popup (CommentsViewModel).
 */
@HiltViewModel
class PostDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val posts: PostRepository,
    private val social: SocialRepository,
    auth: AuthRepository,
    private val comments: CommentRepository,
    private val messages: MessagingRepository
) : ViewModel() {

    private val postId: String = runCatching { savedStateHandle.toRoute<Route.PostDetail>().id }
        .getOrElse { savedStateHandle.get<String>("id").orEmpty() }

    private val _uiState = MutableStateFlow(
        PostDetailUiState(
            isLoading = true,
            now = posts.now(),
            currentUserId = auth.currentUser.value?.id.orEmpty()
        )
    )
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    private val _effects = Channel<PostDetailEffect>(Channel.BUFFERED)
    val effects: Flow<PostDetailEffect> = _effects.receiveAsFlow()

    /** Đã hỏi server xong chưa: trước đó bài chưa có trong máy chỉ là "đang tải", chưa phải "đã xoá". */
    private var refreshed = false

    /** Đang tự rời màn (xoá / báo cáo / chặn): bài biến khỏi máy không phải là "đã bị xoá hoặc ẩn". */
    private var leaving = false

    /** Đang đăng lại / bỏ (trạng thái muốn tới): bấm liên tục thì bỏ qua tới khi xong. */
    private var repostTarget: Boolean? = null

    init {
        viewModelScope.launch {
            auth.currentUser.collect { me -> _uiState.update { it.copy(currentUserId = me?.id.orEmpty()) } }
        }
        viewModelScope.launch {
            posts.observePost(postId).collect { found ->
                _uiState.update {
                    it.copy(
                        post = found,
                        isLoading = found == null && !refreshed,
                        isNotFound = found == null && refreshed && !leaving
                    )
                }
            }
        }
        viewModelScope.launch {
            comments.observeCommentCount(postId).collect { count -> _uiState.update { it.copy(commentsCount = count) } }
        }
        viewModelScope.launch {
            posts.observeRepostState(postId).collect { repost ->
                if (repostTarget == repost.isMine) repostTarget = null
                _uiState.update { it.copy(repostState = repost) }
            }
        }
        viewModelScope.launch {
            // Trạng thái bạn bè với tác giả: quyết định có thanh "Gửi tin nhắn…" hay không
            combine(social.graph, _uiState.map { it.post?.author?.id }.distinctUntilChanged()) { graph, authorId ->
                authorId?.let(graph::statusOf) ?: FriendStatus.NONE
            }.distinctUntilChanged().collect { status -> _uiState.update { it.copy(authorStatus = status) } }
        }
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null, isLoading = it.post == null, now = posts.now()) }
            val error = posts.refreshPost(postId).exceptionOrNull()
            refreshed = true
            _uiState.update { state ->
                if (error == null) {
                    state.copy(isLoading = false, isNotFound = state.post == null, isOffline = false)
                } else {
                    state.copy(
                        isLoading = false,
                        isOffline = error.isOffline,
                        // Bài đã có trong máy thì vẫn xem được; chưa có thì báo lỗi + Thử lại
                        errorMessage = if (state.post == null) error.message else null
                    )
                }
            }
        }
    }

    fun onAction(action: PostDetailAction) {
        val state = _uiState.value
        val currentPost = state.post

        when (action) {
            PostDetailAction.OnBackClick -> {
                sendEffect(PostDetailEffect.NavigateBack)
            }

            PostDetailAction.OnRetry -> refresh()

            is PostDetailAction.OnReact -> {
                val id = currentPost?.id ?: return
                viewModelScope.launch {
                    posts.toggleReaction(id, action.emoji)
                        .onFailure { sendEffect(PostDetailEffect.ShowSnackbar(it.message ?: "Không thả cảm xúc được")) }
                }
            }

            PostDetailAction.OnOpenComments -> _uiState.update { it.copy(isCommentsOpen = true) }

            PostDetailAction.OnDismissComments -> _uiState.update { it.copy(isCommentsOpen = false) }

            PostDetailAction.OnToggleRepost -> {
                val id = currentPost?.id ?: return
                if (repostTarget != null || !state.canRepost) return
                val target = !state.repostState.isMine
                repostTarget = target
                viewModelScope.launch {
                    posts.setReposted(id, target)
                        .onFailure { sendEffect(PostDetailEffect.ShowSnackbar(it.message ?: "Không đăng lại được")) }
                    repostTarget = null
                }
            }

            is PostDetailAction.OnMessageDraftChange -> _uiState.update { it.copy(messageDraft = action.text.take(1000)) }

            PostDetailAction.OnSendMessage -> {
                val post = currentPost ?: return
                val text = state.messageDraft.trim()
                if (text.isEmpty() || state.isSendingMessage || !state.canMessage) return
                _uiState.update { it.copy(isSendingMessage = true) }
                viewModelScope.launch {
                    val result = messages.sendPhotoReply(post, text)
                    _uiState.update {
                        it.copy(isSendingMessage = false, messageDraft = if (result.isSuccess) "" else it.messageDraft)
                    }
                    sendEffect(PostDetailEffect.ShowSnackbar(result.fold({ "Đã gửi cho ${post.author.displayName}" }, { it.message ?: "Không gửi được tin nhắn" })))
                }
            }

            PostDetailAction.OnAuthorClick -> {
                val authorId = currentPost?.author?.id ?: return
                sendEffect(PostDetailEffect.NavigateToProfile(authorId))
            }

            PostDetailAction.OnLocationClick -> {
                val (level, code) = currentPost?.location?.toRegionAlbumTarget() ?: return
                sendEffect(PostDetailEffect.NavigateToRegionAlbum(level, code))
            }

            PostDetailAction.OnOpenReportSheet -> {
                _uiState.update { it.copy(isReportSheetOpen = true) }
            }

            PostDetailAction.OnDismissReportSheet -> {
                _uiState.update { it.copy(isReportSheetOpen = false) }
            }

            is PostDetailAction.OnSubmitReport -> {
                val id = currentPost?.id ?: return
                _uiState.update { it.copy(isReportSheetOpen = false) }
                viewModelScope.launch {
                    leaving = true
                    posts.reportPost(id, action.reason).fold(
                        onSuccess = { sendEffect(PostDetailEffect.NavigateBack) },
                        onFailure = {
                            leaving = false
                            sendEffect(PostDetailEffect.ShowSnackbar(it.message ?: "Không gửi được báo cáo"))
                        }
                    )
                }
            }

            PostDetailAction.OnOpenBlockDialog -> {
                _uiState.update { it.copy(showBlockConfirmDialog = true) }
            }

            PostDetailAction.OnDismissBlockDialog -> {
                _uiState.update { it.copy(showBlockConfirmDialog = false) }
            }

            PostDetailAction.OnConfirmBlock -> {
                val authorId = currentPost?.author?.id ?: return
                _uiState.update { it.copy(showBlockConfirmDialog = false) }
                viewModelScope.launch {
                    leaving = true
                    val result = social.block(authorId)
                    if (result.isSuccess) {
                        posts.hidePostsByAuthor(authorId)
                        sendEffect(PostDetailEffect.NavigateBack)
                    } else {
                        leaving = false
                        sendEffect(PostDetailEffect.ShowSnackbar(result.exceptionOrNull()?.message ?: "Không chặn được, thử lại sau"))
                    }
                }
            }

            // ---------- Bài của mình ----------

            PostDetailAction.OnOpenEditCaption -> {
                if (!state.isOwnPost) return
                _uiState.update { it.copy(isEditCaptionOpen = true, editCaptionInput = currentPost?.caption.orEmpty()) }
            }

            PostDetailAction.OnDismissEditCaption -> {
                _uiState.update { it.copy(isEditCaptionOpen = false, isSavingCaption = false) }
            }

            is PostDetailAction.OnChangeCaptionInput -> {
                _uiState.update { it.copy(editCaptionInput = action.text.take(vn.nhom2.lokap.domain.model.POST_CAPTION_MAX_LENGTH)) }
            }

            PostDetailAction.OnSaveCaption -> {
                val id = currentPost?.id ?: return
                if (!state.isOwnPost || state.isSavingCaption) return
                _uiState.update { it.copy(isSavingCaption = true) }
                viewModelScope.launch {
                    posts.updateCaption(id, state.editCaptionInput.trim().ifEmpty { null }).fold(
                        onSuccess = {
                            _uiState.update { it.copy(isEditCaptionOpen = false, isSavingCaption = false) }
                        },
                        onFailure = { error ->
                            _uiState.update { it.copy(isSavingCaption = false) }
                            sendEffect(PostDetailEffect.ShowSnackbar(error.message ?: "Không lưu được, thử lại sau"))
                        }
                    )
                }
            }

            PostDetailAction.OnOpenDeleteDialog -> {
                if (!state.isOwnPost) return
                _uiState.update { it.copy(showDeleteConfirmDialog = true) }
            }

            PostDetailAction.OnDismissDeleteDialog -> {
                _uiState.update { it.copy(showDeleteConfirmDialog = false) }
            }

            PostDetailAction.OnConfirmDelete -> {
                val id = currentPost?.id ?: return
                if (!state.isOwnPost || state.isDeleting) return
                _uiState.update { it.copy(isDeleting = true) }
                viewModelScope.launch {
                    leaving = true
                    posts.deletePost(id).fold(
                        onSuccess = {
                            _uiState.update { it.copy(showDeleteConfirmDialog = false, isDeleting = false) }
                            sendEffect(PostDetailEffect.NavigateBack)
                        },
                        onFailure = { error ->
                            leaving = false
                            _uiState.update { it.copy(isDeleting = false) }
                            sendEffect(PostDetailEffect.ShowSnackbar(error.message ?: "Không xoá được, thử lại sau"))
                        }
                    )
                }
            }
        }
    }

    private fun sendEffect(effect: PostDetailEffect) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }
}
