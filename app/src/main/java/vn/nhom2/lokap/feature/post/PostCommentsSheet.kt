package vn.nhom2.lokap.feature.post

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.nhom2.lokap.core.designsystem.component.AvatarRing
import vn.nhom2.lokap.core.designsystem.component.ContentDeleteDialog
import vn.nhom2.lokap.core.designsystem.component.ContentEditDialog
import vn.nhom2.lokap.core.designsystem.component.LokapPulseLoader
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme

/**
 * Popup bình luận phủ 100% màn hình (kiểu Facebook): danh sách ở trên, ô nhập ở dưới bám bàn phím.
 * Chỉ nằm trong cây giao diện khi đang mở; rời đi thì gỡ listener Firestore.
 */
@Composable
fun PostCommentsSheet(
    postId: String,
    onDismiss: () -> Unit,
    viewModel: CommentsViewModel = hiltViewModel(key = "post_comments")
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(postId) { viewModel.open(postId) }
    DisposableEffect(viewModel) { onDispose { viewModel.close() } }
    CommentsDialog(state = state, onAction = viewModel::onAction, onDismiss = onDismiss)
}

/** 1 dòng trong danh sách: bình luận gốc (depth 0) hoặc trả lời (depth 1, mọi cấp trả lời dồn về 1 cấp như Facebook). */
private data class CommentRow(val comment: CommentUi, val depth: Int, val rootAuthorName: String)

private fun List<CommentUi>.toRows(): List<CommentRow> = flatMap { root ->
    val out = mutableListOf(CommentRow(root, 0, root.authorName))
    collectReplies(root.replies, root.authorName, out)
    out
}

/** Cây đã giới hạn độ sâu ở toCommentUi nên đệ quy không chạy vô hạn. */
private fun collectReplies(list: List<CommentUi>, rootName: String, out: MutableList<CommentRow>) {
    list.forEach { c ->
        out += CommentRow(c, 1, rootName)
        collectReplies(c.replies, rootName, out)
    }
}

@Composable
private fun CommentsDialog(
    state: CommentsUiState,
    onAction: (CommentsAction) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LokapTheme.colors
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        // Cửa sổ dialog vẽ tràn dưới thanh trạng thái: chỉnh màu icon cho hợp nền sáng/tối
        val view = LocalView.current
        val isDark = colors.isDark
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }
        }
        val visible = remember { MutableTransitionState(false) }.apply { targetState = true }
        AnimatedVisibility(
            visibleState = visible,
            enter = slideInVertically(tween(260)) { it / 3 } + fadeIn(tween(200))
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .imePadding()
                ) {
                    CommentsHeader(count = state.totalCount, onClose = onDismiss)
                    HorizontalDivider(color = colors.outline)
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        CommentsList(state = state, onAction = onAction)
                    }
                    CommentInputBar(state = state, onAction = onAction)
                }
            }
        }

        state.editing?.let {
            ContentEditDialog("Sửa bình luận", state.editText, state.isSaving,
                { text -> onAction(CommentsAction.OnEditTextChange(text)) },
                { onAction(CommentsAction.OnSaveEdit) },
                { onAction(CommentsAction.OnDismissEdit) })
        }
        if (state.deletingId != null) {
            ContentDeleteDialog("Xoá bình luận?", state.isSaving,
                { onAction(CommentsAction.OnConfirmDelete) },
                { onAction(CommentsAction.OnDismissDelete) })
        }
    }
}

@Composable
private fun CommentsHeader(count: Int, onClose: () -> Unit) {
    val colors = LokapTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = LokapTheme.spacing.lg, end = LokapTheme.spacing.xs, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (count > 0) "Bình luận · $count" else "Bình luận",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Đóng bình luận",
                tint = colors.textPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun CommentsList(state: CommentsUiState, onAction: (CommentsAction) -> Unit) {
    val colors = LokapTheme.colors
    val rows = remember(state.comments) { state.comments.toRows() }
    val listState = rememberLazyListState()

    // Vừa có bình luận gốc mới (thường là của mình): cuộn xuống cuối để thấy ngay
    var lastRootCount by remember { mutableIntStateOf(-1) }
    LaunchedEffect(state.comments.size, state.isLoading) {
        if (state.isLoading) return@LaunchedEffect
        if (lastRootCount in 0 until state.comments.size && rows.isNotEmpty()) listState.animateScrollToItem(rows.lastIndex)
        lastRootCount = state.comments.size
    }

    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LokapPulseLoader() }
        rows.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = state.errorMessage ?: "Chưa có bình luận",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary
            )
        }
        else -> LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = LokapTheme.spacing.md, vertical = LokapTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(rows, key = { it.comment.id }) { row ->
                CommentItem(
                    row = row,
                    isMine = row.comment.authorId == state.currentUserId,
                    onAction = onAction
                )
            }
        }
    }
}

@Composable
private fun CommentItem(row: CommentRow, isMine: Boolean, onAction: (CommentsAction) -> Unit) {
    val colors = LokapTheme.colors
    val comment = row.comment
    val haptic = LocalHapticFeedback.current
    var menuOpen by remember { mutableStateOf(false) }
    val liked = comment.userVote > 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (row.depth > 0) 44.dp else 0.dp, top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AvatarRing(
            avatarUrl = comment.authorAvatarUrl,
            displayName = comment.authorName,
            size = if (row.depth > 0) 30.dp else 38.dp
        )
        Column(modifier = Modifier.weight(1f)) {
            Box {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.surfaceHigh)
                        .combinedClickable(
                            onClick = {},
                            onLongClick = if (isMine) {
                                {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuOpen = true
                                }
                            } else null,
                            onLongClickLabel = if (isMine) "Sửa hoặc xoá bình luận" else null
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = comment.authorName,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    val mention = comment.replyToAuthorName?.takeIf { row.depth > 0 && it != row.rootAuthorName }
                    Text(
                        text = buildAnnotatedString {
                            if (mention != null) {
                                withStyle(SpanStyle(color = colors.primary, fontWeight = FontWeight.SemiBold)) { append(mention) }
                                append(" ")
                            }
                            append(comment.content)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textPrimary
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    containerColor = colors.surface,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Sửa", color = colors.textPrimary) },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = colors.textPrimary) },
                        onClick = {
                            menuOpen = false
                            onAction(CommentsAction.OnEdit(comment))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Xoá", color = colors.error) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = colors.error) },
                        onClick = {
                            menuOpen = false
                            onAction(CommentsAction.OnDelete(comment.id))
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = comment.timeAgo,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
                CommentTextAction(
                    label = "Thích",
                    highlighted = liked,
                    onClick = { onAction(CommentsAction.OnToggleLike(comment.id)) }
                )
                CommentTextAction(
                    label = "Trả lời",
                    highlighted = false,
                    onClick = { onAction(CommentsAction.OnReplyTo(comment)) }
                )
                if (comment.score > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.semantics(mergeDescendants = true) {
                            contentDescription = "${comment.score} lượt thích"
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(colors.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ThumbUp,
                                contentDescription = null,
                                tint = colors.buttonText,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        Text(
                            text = comment.score.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentTextAction(label: String, highlighted: Boolean, onClick: () -> Unit) {
    val colors = LokapTheme.colors
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = if (highlighted) colors.primary else colors.textSecondary,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp)
            .wrapContentHeight(Alignment.CenterVertically)
    )
}

@Composable
private fun CommentInputBar(state: CommentsUiState, onAction: (CommentsAction) -> Unit) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(state.replyingTo?.id) {
        if (state.replyingTo != null) runCatching { focusRequester.requestFocus() }
    }

    Column(modifier = Modifier.fillMaxWidth().background(colors.surface)) {
        HorizontalDivider(color = colors.outline)
        if (state.errorMessage != null && state.comments.isNotEmpty()) {
            Text(
                text = state.errorMessage,
                style = MaterialTheme.typography.labelMedium,
                color = colors.error,
                modifier = Modifier.padding(horizontal = spacing.lg, vertical = 4.dp)
            )
        }
        state.replyingTo?.let { target ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = spacing.lg, end = spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Đang trả lời ${target.authorName}",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                CommentTextAction(label = "Huỷ", highlighted = false, onClick = { onAction(CommentsAction.OnReplyTo(null)) })
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.surfaceHigh)
                    .padding(horizontal = spacing.lg, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (state.input.isEmpty()) {
                    Text(
                        text = if (state.replyingTo != null) "Viết câu trả lời…" else "Viết bình luận…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textSecondary
                    )
                }
                BasicTextField(
                    value = state.input,
                    onValueChange = { onAction(CommentsAction.OnInputChange(it)) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.primary),
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .semantics { contentDescription = "Ô nhập bình luận" }
                )
            }
            IconButton(
                onClick = { onAction(CommentsAction.OnSend) },
                enabled = state.canSend,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (state.canSend) colors.primary else colors.surfaceHigh)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Đăng bình luận",
                    tint = if (state.canSend) colors.buttonText else colors.textSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
