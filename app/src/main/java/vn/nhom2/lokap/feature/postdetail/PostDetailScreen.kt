package vn.nhom2.lokap.feature.postdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.coroutines.launch
import vn.nhom2.lokap.core.designsystem.component.*
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.core.navigation.navigateSingleTop
import vn.nhom2.lokap.core.navigation.popBackStackSafely
import vn.nhom2.lokap.domain.model.Audience
import vn.nhom2.lokap.domain.model.POST_CAPTION_MAX_LENGTH
import vn.nhom2.lokap.domain.model.ReportReasons
import vn.nhom2.lokap.feature.post.PhotoMessageBar
import vn.nhom2.lokap.feature.post.RepostToggle
import vn.nhom2.lokap.feature.post.PostCard
import vn.nhom2.lokap.feature.post.PostCommentsSheet

fun NavGraphBuilder.postDetailScreen(navController: NavController) {
    composable<Route.PostDetail> {
        PostDetailRoute(
            onNavigateBack = { navController.popBackStackSafely() },
            onNavigateToProfile = { userId -> navController.navigateSingleTop(Route.Profile(userId)) },
            onNavigateToRegionAlbum = { level, code ->
                navController.navigateSingleTop(Route.RegionAlbum(level, code))
            }
        )
    }
}

@Composable
fun PostDetailRoute(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToRegionAlbum: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PostDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PostDetailEffect.NavigateBack -> {
                    // Bỏ focus ô nhắn tin trước khi Back: bàn phím ẩn ngay, không che màn trước suốt animation
                    focusManager.clearFocus(force = true)
                    onNavigateBack()
                }
                is PostDetailEffect.NavigateToProfile -> onNavigateToProfile(effect.userId)
                is PostDetailEffect.NavigateToRegionAlbum -> onNavigateToRegionAlbum(effect.level, effect.code)
                is PostDetailEffect.ShowSnackbar -> {
                    scope.launch { snackbarHostState.showSnackbar(effect.message) }
                }
            }
        }
    }

    PostDetailScreen(
        state = state,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

/** Trang 1 ảnh: thanh trên (Quay lại + ⋮) và đúng thẻ ảnh như Feed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    state: PostDetailUiState,
    onAction: (PostDetailAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shapes = LokapTheme.shapes
    val post = state.post

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .imePadding(),
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.xs, vertical = spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { onAction(PostDetailAction.OnBackClick) }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                if (post != null && !state.isNotFound) {
                    PostOptionsMenu(
                        isOwnPost = state.isOwnPost,
                        authorName = post.author.displayName,
                        onAction = onAction
                    )
                }
            }
        }
    ) { innerPadding ->
        if (state.isNotFound || post == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(spacing.lg),
                contentAlignment = Alignment.Center
            ) {
                when {
                    // Chưa có bài trong máy, đang hỏi server
                    state.isLoading -> LokapPulseLoader()
                    state.errorMessage != null -> ErrorState(
                        message = state.errorMessage,
                        onRetry = { onAction(PostDetailAction.OnRetry) }
                    )
                    else -> ErrorState(
                        message = "Bài đã bị xoá hoặc ẩn",
                        retryLabel = "Quay lại",
                        onRetry = { onAction(PostDetailAction.OnBackClick) }
                    )
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            if (state.isOffline) {
                OfflineBanner()
            }

            PostCard(
                post = post,
                now = state.now,
                onReact = { emoji -> onAction(PostDetailAction.OnReact(emoji)) },
                onAuthorClick = { onAction(PostDetailAction.OnAuthorClick) },
                commentsCount = if (state.showComments) state.commentsCount else null,
                onOpenComments = { onAction(PostDetailAction.OnOpenComments) },
                onLocationClick = { onAction(PostDetailAction.OnLocationClick) },
                repost = if (state.showRepost) {
                    RepostToggle(
                        isReposted = state.repostState.isMine,
                        count = state.repostState.count,
                        enabled = state.canRepost
                    ) { onAction(PostDetailAction.OnToggleRepost) }
                } else null,
                footer = if (state.canMessage) {
                    {
                        PhotoMessageBar(
                            text = state.messageDraft,
                            onTextChange = { onAction(PostDetailAction.OnMessageDraftChange(it)) },
                            isSending = state.isSendingMessage,
                            onSend = { onAction(PostDetailAction.OnSendMessage) },
                            reactions = post.reactions,
                            myReaction = post.myReaction,
                            onReact = { emoji -> onAction(PostDetailAction.OnReact(emoji)) },
                            showQuickReactions = post.audience != Audience.PUBLIC
                        )
                    }
                } else null
            )
        }

        if (state.isCommentsOpen) {
            PostCommentsSheet(postId = post.id, onDismiss = { onAction(PostDetailAction.OnDismissComments) })
        }

        if (state.isReportSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { onAction(PostDetailAction.OnDismissReportSheet) },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                shape = shapes.sheet,
                containerColor = colors.surface,
                contentColor = colors.textPrimary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.lg, vertical = spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    Text(
                        text = "Báo cáo bài viết",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )

                    ReportReasons.forEach { reason ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shapes.card)
                                .background(colors.surfaceHigh)
                                .border(1.dp, colors.outline, shapes.card)
                                .clickable { onAction(PostDetailAction.OnSubmitReport(reason)) }
                                .padding(horizontal = spacing.lg, vertical = spacing.md)
                        ) {
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }
        }

        if (state.showBlockConfirmDialog) {
            BlockUserDialog(
                displayName = post.author.displayName,
                onConfirm = { onAction(PostDetailAction.OnConfirmBlock) },
                onDismiss = { onAction(PostDetailAction.OnDismissBlockDialog) }
            )
        }

        if (state.isEditCaptionOpen) {
            EditCaptionDialog(
                value = state.editCaptionInput,
                isSaving = state.isSavingCaption,
                onValueChange = { onAction(PostDetailAction.OnChangeCaptionInput(it)) },
                onSave = { onAction(PostDetailAction.OnSaveCaption) },
                onDismiss = { onAction(PostDetailAction.OnDismissEditCaption) }
            )
        }

        if (state.showDeleteConfirmDialog) {
            DeletePostDialog(
                isDeleting = state.isDeleting,
                onConfirm = { onAction(PostDetailAction.OnConfirmDelete) },
                onDismiss = { onAction(PostDetailAction.OnDismissDeleteDialog) }
            )
        }
    }
}

/** ⋮: bài của mình → Sửa lời nhắn / Xoá bài; bài người khác → Báo cáo / Chặn. */
@Composable
private fun PostOptionsMenu(
    isOwnPost: Boolean,
    authorName: String,
    onAction: (PostDetailAction) -> Unit
) {
    val colors = LokapTheme.colors
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Tùy chọn",
                tint = colors.textPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = colors.surfaceHigh
        ) {
            val items = if (isOwnPost) {
                listOf(
                    Triple("Sửa lời nhắn", colors.textPrimary, PostDetailAction.OnOpenEditCaption),
                    Triple("Xoá bài", colors.error, PostDetailAction.OnOpenDeleteDialog)
                )
            } else {
                listOf(
                    Triple("Báo cáo bài viết", colors.textPrimary, PostDetailAction.OnOpenReportSheet),
                    Triple("Chặn $authorName", colors.error, PostDetailAction.OnOpenBlockDialog)
                )
            }
            items.forEach { (label, color, action) ->
                DropdownMenuItem(
                    text = { Text(text = label, style = MaterialTheme.typography.bodyLarge, color = color) },
                    onClick = {
                        expanded = false
                        onAction(action)
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }
    }
}

/** Sửa lời nhắn của bài mình đăng: tối đa 60 ký tự, để trống = bỏ lời nhắn. */
@Composable
private fun EditCaptionDialog(
    value: String,
    isSaving: Boolean,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LokapTheme.colors
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = colors.surface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = { Text("Sửa lời nhắn") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    maxLines = 3,
                    placeholder = { Text("Lời nhắn…", color = colors.textTertiary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.outline,
                        cursorColor = colors.primary,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )
                Text(
                    text = "${value.length}/$POST_CAPTION_MAX_LENGTH",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (value.length >= POST_CAPTION_MAX_LENGTH) colors.primary else colors.textTertiary,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !isSaving) {
                Text(
                    text = if (isSaving) "Đang lưu…" else "Lưu",
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(text = "Huỷ", color = colors.textSecondary)
            }
        }
    )
}

/** Xoá bài của mình: xoá mềm trên server + xoá ảnh, không hoàn tác. */
@Composable
private fun DeletePostDialog(
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LokapTheme.colors
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        containerColor = colors.surface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = { Text("Xoá bài này?") },
        text = { Text("Ảnh và cảm xúc của bài sẽ bị xoá, không hoàn tác được.") },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isDeleting) {
                Text(
                    text = if (isDeleting) "Đang xoá…" else "Xoá",
                    color = colors.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text(text = "Huỷ", color = colors.textSecondary)
            }
        }
    )
}

@Preview(name = "PostDetail - Không tồn tại", showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun PostDetailNotFoundPreview() {
    LokapTheme {
        PostDetailScreen(
            state = PostDetailUiState(
                post = null,
                isNotFound = true
            ),
            onAction = {}
        )
    }
}
