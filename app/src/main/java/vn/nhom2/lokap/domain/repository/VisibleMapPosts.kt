package vn.nhom2.lokap.domain.repository

import kotlinx.coroutines.flow.combine
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.LocationLevel
import vn.nhom2.lokap.domain.model.PostSendState

/** Chỉ gom bài đã được repository cho phép đọc, không đưa bài riêng lên bản đồ. */
fun PostRepository.observeMapPosts() = combine(friendFeed, publicPosts) { friends, public ->
    (friends + public).distinctBy { it.id }.filter {
        it.sendState == PostSendState.SENT && it.audience != Audience.DIRECT &&
            it.location.level != LocationLevel.COUNTRY
    }
}
