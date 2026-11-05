package vn.nhom2.lokap.feature.postdetail

import vn.nhom2.lokap.domain.model.toRegionAlbumTarget
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
import vn.nhom2.lokap.data.fake.FakePostStore
import vn.nhom2.lokap.data.fake.SampleData2

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
                // Giống Feed: ẩn bài với người báo cáo rồi quay lại
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
                // Giống Feed: ẩn mọi bài của người bị chặn rồi quay lại
                val authorId = currentPost?.author?.id
                _uiState.update { it.copy(showBlockConfirmDialog = false) }
                sendEffect(PostDetailEffect.NavigateBack)
                if (authorId != null) FakePostStore.removePostsByAuthor(authorId)
            }
        }
    }

    private fun sendEffect(effect: PostDetailEffect) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }
}
