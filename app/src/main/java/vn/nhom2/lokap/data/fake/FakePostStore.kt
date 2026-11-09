package vn.nhom2.lokap.data.fake

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.nhom2.lokap.domain.model.*
object FakePostStore {

    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _posts = MutableStateFlow(SampleData.posts)
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    fun addPendingPostAndSimulateSend(post: Post) {
        val pendingPost = post.copy(sendState = PostSendState.PENDING)
        _posts.update { current -> listOf(pendingPost) + current }
        storeScope.launch {
            delay(1_500L)
            _posts.update { current ->
                current.map { item ->
                    if (item.id == pendingPost.id) {
                        item.copy(sendState = PostSendState.SENT)
                    } else {
                        item
                    }
                }
            }
        }
    }

    fun retrySend(postId: String) {
        _posts.update { current ->
            current.map { item ->
                if (item.id == postId) item.copy(sendState = PostSendState.PENDING) else item
            }
        }
        storeScope.launch {
            delay(1_500L)
            _posts.update { current ->
                current.map { item ->
                    if (item.id == postId) item.copy(sendState = PostSendState.SENT) else item
                }
            }
        }
    }

    fun toggleReaction(postId: String, emoji: String) {
        _posts.update { current ->
            current.map { post ->
                if (post.id != postId) return@map post

                val previousEmoji = post.myReaction
                val nextMyReaction = if (previousEmoji == emoji) null else emoji
                val countsMap = post.reactions.associate { it.emoji to it.count }.toMutableMap()

                if (previousEmoji != null) {
                    val decremented = (countsMap[previousEmoji] ?: 1) - 1
                    if (decremented <= 0) {
                        countsMap.remove(previousEmoji)
                    } else {
                        countsMap[previousEmoji] = decremented
                    }
                }

                if (nextMyReaction != null) {
                    countsMap[nextMyReaction] = (countsMap[nextMyReaction] ?: 0) + 1
                }

                val updatedReactions = countsMap.entries.map { (k, v) -> Reaction(k, v) }
                post.copy(
                    reactions = updatedReactions,
                    myReaction = nextMyReaction
                )
            }
        }
    }

    fun removePost(postId: String) {
        _posts.update { current -> current.filterNot { it.id == postId } }
    }

    fun removePostsByAuthor(authorId: String) {
        _posts.update { current -> current.filterNot { it.author.id == authorId } }
    }
}
