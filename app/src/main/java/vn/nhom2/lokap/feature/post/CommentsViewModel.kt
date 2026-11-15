package vn.nhom2.lokap.feature.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.domain.repository.AuthRepository
import vn.nhom2.lokap.domain.repository.CommentRepository

/**
 * Bình luận của 1 bài, theo dõi trực tiếp từ Firestore. Popup mở thì [open], đóng thì [close] để gỡ listener.
 * Dùng chung cho Feed và trang ảnh (mỗi màn 1 instance).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val comments: CommentRepository,
    private val auth: AuthRepository
) : ViewModel() {

    private val postId = MutableStateFlow<String?>(null)
    private val _uiState = MutableStateFlow(CommentsUiState(currentUserId = auth.currentUser.value?.id.orEmpty()))
    val uiState: StateFlow<CommentsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            auth.currentUser.collect { me -> _uiState.update { it.copy(currentUserId = me?.id.orEmpty()) } }
        }
        viewModelScope.launch {
            postId.flatMapLatest { id -> if (id == null) flowOf(null) else comments.observeComments(id) }
                .collect { result ->
                    if (result == null) return@collect
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            comments = result.getOrNull()?.toCommentUi() ?: it.comments,
                            errorMessage = result.exceptionOrNull()?.message
                        )
                    }
                }
        }
    }

    fun open(id: String) {
        if (postId.value == id) return
        _uiState.update { CommentsUiState(postId = id, currentUserId = it.currentUserId, isLoading = true) }
        postId.value = id
    }

    fun close() {
        postId.value = null
        _uiState.update { CommentsUiState(currentUserId = it.currentUserId) }
    }

    fun onAction(action: CommentsAction) {
        val state = _uiState.value
        val id = state.postId ?: return
        when (action) {
            is CommentsAction.OnInputChange -> _uiState.update { it.copy(input = action.text.take(1000), errorMessage = null) }
            is CommentsAction.OnReplyTo -> _uiState.update { it.copy(replyingTo = action.comment) }
            CommentsAction.OnSend -> if (state.canSend) save {
                comments.add(id, state.input, state.replyingTo?.id).onSuccess {
                    _uiState.update { it.copy(input = "", replyingTo = null) }
                }
            }
            // "Thích" = phiếu +1; bấm lại thì bỏ (repository tự bỏ khi phiếu trùng)
            is CommentsAction.OnToggleLike -> save { comments.vote(id, action.commentId, up = true) }
            is CommentsAction.OnEdit -> if (action.comment.authorId == state.currentUserId) {
                _uiState.update { it.copy(editing = action.comment, editText = action.comment.content) }
            }
            is CommentsAction.OnEditTextChange -> _uiState.update { it.copy(editText = action.text.take(1000)) }
            CommentsAction.OnDismissEdit -> if (!state.isSaving) _uiState.update { it.copy(editing = null) }
            CommentsAction.OnSaveEdit -> state.editing?.let { comment ->
                save { comments.editComment(id, comment.id, state.editText).onSuccess { _uiState.update { it.copy(editing = null) } } }
            }
            is CommentsAction.OnDelete -> _uiState.update { it.copy(deletingId = action.commentId) }
            CommentsAction.OnDismissDelete -> if (!state.isSaving) _uiState.update { it.copy(deletingId = null) }
            CommentsAction.OnConfirmDelete -> state.deletingId?.let { commentId ->
                save { comments.deleteComment(id, commentId).onSuccess { _uiState.update { it.copy(deletingId = null) } } }
            }
        }
    }

    private fun save(block: suspend () -> Result<Unit>) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val result = block()
            _uiState.update { it.copy(isSaving = false, errorMessage = result.exceptionOrNull()?.let { e -> e.message ?: "Không lưu được bình luận" }) }
        }
    }
}
