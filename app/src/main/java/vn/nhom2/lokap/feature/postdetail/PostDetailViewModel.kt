package vn.nhom2.lokap.feature.postdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.data.fake.FakeCommentStore
import vn.nhom2.lokap.data.fake.FakePostStore
import vn.nhom2.lokap.data.fake.SampleData
import vn.nhom2.lokap.data.fake.SampleData2
import vn.nhom2.lokap.domain.model.toRegionAlbumTarget
class PostDetailViewModel : ViewModel() {

    private var targetPostId: String? = null

    private val _uiState = MutableStateFlow(PostDetailUiState(isLoading = true))
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    private val _effects = Channel<PostDetailEffect>(Channel.BUFFERED)
    val effects: Flow<PostDetailEffect> = _effects.receiveAsFlow()

    fun loadPost(postId: String) {
        if (targetPostId == postId) return
        targetPostId = postId

        viewModelScope.launch {
            FakePostStore.posts.collect { list ->
                val found = list.firstOrNull { it.id == postId }
                _uiState.update { state ->
                    state.copy(
                        post = found,
                        isLoading = false,
                        isNotFound = (found == null)
                    )
                }
            }
        }

        viewModelScope.launch {
            FakeCommentStore.comments.collect { map ->
                val list = map[postId] ?: emptyList()
                _uiState.update { state ->
                    state.copy(comments = list)
                }
            }
        }
    }

    fun onAction(action: PostDetailAction) {
        val currentPost = _uiState.value.post

        when (action) {
            PostDetailAction.OnBackClick -> {
                sendEffect(PostDetailEffect.NavigateBack)
            }

            is PostDetailAction.OnReact -> {
                val id = currentPost?.id ?: return
                FakePostStore.toggleReaction(id, action.emoji)
            }

            PostDetailAction.OnReplyClick -> {
                val authorId = currentPost?.author?.id ?: return
                val convId = SampleData2.resolveConversationIdForUser(authorId)
                sendEffect(PostDetailEffect.NavigateToChat(convId))
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

                val id = currentPost?.id
                _uiState.update { it.copy(isReportSheetOpen = false) }
                sendEffect(PostDetailEffect.NavigateBack)
                if (id != null) FakePostStore.removePost(id)
            }

            PostDetailAction.OnOpenBlockDialog -> {
                _uiState.update { it.copy(showBlockConfirmDialog = true) }
            }

            PostDetailAction.OnDismissBlockDialog -> {
                _uiState.update { it.copy(showBlockConfirmDialog = false) }
            }

            PostDetailAction.OnConfirmBlock -> {

                val authorId = currentPost?.author?.id
                _uiState.update { it.copy(showBlockConfirmDialog = false) }
                sendEffect(PostDetailEffect.NavigateBack)
                if (authorId != null) FakePostStore.removePostsByAuthor(authorId)
            }

            is PostDetailAction.OnVoteComment -> {
                val postId = targetPostId ?: return
                FakeCommentStore.voteComment(postId, action.commentId, action.isUpvote)
            }

            is PostDetailAction.OnReplyToComment -> {
                _uiState.update { it.copy(replyingToComment = action.comment) }
            }

            is PostDetailAction.OnChangeCommentInput -> {
                _uiState.update { it.copy(commentInput = action.text) }
            }

            PostDetailAction.OnSendComment -> {
                val text = _uiState.value.commentInput.trim()
                val postId = targetPostId ?: return
                if (text.isNotBlank()) {
                    val replyTarget = _uiState.value.replyingToComment
                    FakeCommentStore.addComment(
                        postId = postId,
                        content = text,
                        replyTargetId = replyTarget?.id,
                        replyToAuthorName = replyTarget?.authorName
                    )
                    _uiState.update {
                        it.copy(
                            commentInput = "",
                            replyingToComment = null
                        )
                    }
                    sendEffect(PostDetailEffect.ShowSnackbar("Đã gửi bình luận"))
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
