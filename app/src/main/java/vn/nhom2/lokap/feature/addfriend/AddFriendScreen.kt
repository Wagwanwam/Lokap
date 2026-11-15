package vn.nhom2.lokap.feature.addfriend

import android.content.ClipData
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import kotlinx.coroutines.delay
import vn.nhom2.lokap.core.designsystem.component.AvatarRing
import vn.nhom2.lokap.core.designsystem.component.GhostButton
import vn.nhom2.lokap.core.designsystem.component.PinkButton
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.Route
import vn.nhom2.lokap.core.navigation.navigateSingleTop
import vn.nhom2.lokap.core.navigation.popBackStackSafely
import vn.nhom2.lokap.core.navigation.rememberEntrySettled
import vn.nhom2.lokap.domain.model.FriendCode
import vn.nhom2.lokap.domain.model.FriendStatus
import vn.nhom2.lokap.domain.model.User

/** Khoá trong savedStateHandle của màn Thêm bạn: màn Quét mã ghi username vừa đọc được vào đây. */
private const val SCANNED_USERNAME_KEY = "scannedFriendUsername"

fun NavGraphBuilder.addFriendScreen(navController: NavController) {
    composable<Route.AddFriend>(
        deepLinks = listOf(
            navDeepLink {
                uriPattern = "${FriendCode.SCHEME}://${FriendCode.HOST}/{${AddFriendViewModel.ARG_USERNAME}}"
            }
        )
    ) { entry ->
        val scanned by entry.savedStateHandle
            .getStateFlow<String?>(SCANNED_USERNAME_KEY, null)
            .collectAsStateWithLifecycle()
        AddFriendRoute(
            scannedUsername = scanned,
            onScannedUsernameConsumed = { entry.savedStateHandle[SCANNED_USERNAME_KEY] = null },
            onNavigateBack = { navController.popBackStackSafely() },
            onOpenScanner = { navController.navigateSingleTop(Route.ScanFriendCode) },
            onNavigateToProfile = { userId -> navController.navigateSingleTop(Route.Profile(userId)) }
        )
    }

    composable<Route.ScanFriendCode> {
        ScanFriendCodeRoute(
            onCodeScanned = { username ->
                navController.previousBackStackEntry?.savedStateHandle?.set(SCANNED_USERNAME_KEY, username)
                navController.popBackStackSafely()
            },
            onClose = { navController.popBackStackSafely() }
        )
    }
}

@Composable
fun AddFriendRoute(
    scannedUsername: String?,
    onScannedUsernameConsumed: () -> Unit,
    onNavigateBack: () -> Unit,
    onOpenScanner: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    viewModel: AddFriendViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val clipboard = LocalClipboard.current

    LaunchedEffect(scannedUsername) {
        if (scannedUsername != null) {
            viewModel.onAction(AddFriendAction.LookUp(scannedUsername))
            onScannedUsernameConsumed()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AddFriendEffect.NavigateBack -> onNavigateBack()
                AddFriendEffect.OpenScanner -> onOpenScanner()
                is AddFriendEffect.NavigateToProfile -> onNavigateToProfile(effect.userId)
                is AddFriendEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                is AddFriendEffect.ShareInvite -> {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, effect.text)
                    }
                    context.startActivity(Intent.createChooser(send, "Chia sẻ mã kết bạn"))
                }
                is AddFriendEffect.CopyLink -> {
                    clipboard.setClipEntry(ClipData.newPlainText("Link kết bạn", effect.link).toClipEntry())
                    // Android 13+ tự hiện thông báo đã sao chép, máy cũ hơn thì báo bằng snackbar
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        snackbarHostState.showSnackbar("Đã sao chép link")
                    }
                }
            }
        }
    }

    AddFriendScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState
    )
}

@Composable
fun AddFriendScreen(
    uiState: AddFriendUiState,
    onAction: (AddFriendAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    // Quét mã xong quay lại: sheet (cửa sổ Dialog mới) chỉ mở khi animation Back chạy xong,
    // việc tra cứu username vẫn chạy song song với animation
    val entrySettled = rememberEntrySettled()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding(),
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            AddFriendTopBar(
                friendCount = uiState.friendCount,
                maxFriends = uiState.maxFriends,
                onBackClick = { onAction(AddFriendAction.NavigateBack) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(spacing.xl))

            MyCodeCard(me = uiState.me)

            Spacer(modifier = Modifier.height(spacing.lg))

            Text(
                text = uiState.me?.displayName.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (uiState.hasCode) {
                val username = uiState.me?.username.orEmpty()
                Text(
                    text = "@$username",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(spacing.md))
                FriendLinkRow(
                    link = FriendCode.linkFor(username),
                    onCopy = { onAction(AddFriendAction.CopyLink) }
                )
            }

            Spacer(modifier = Modifier.height(spacing.xl))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                AddFriendActionButton(
                    icon = Icons.Filled.QrCodeScanner,
                    label = "Quét mã",
                    filled = true,
                    onClick = { onAction(AddFriendAction.OpenScanner) },
                    modifier = Modifier.weight(1f)
                )
                AddFriendActionButton(
                    icon = Icons.Filled.Share,
                    label = "Chia sẻ",
                    filled = false,
                    enabled = uiState.hasCode,
                    onClick = { onAction(AddFriendAction.ShareInvite) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (uiState.isFriendLimitReached) {
                Spacer(modifier = Modifier.height(spacing.lg))
                FriendLimitNote(maxFriends = uiState.maxFriends)
            }

            Spacer(modifier = Modifier.height(spacing.xxl))
        }
    }

    uiState.lookup?.takeIf { entrySettled }?.let { lookup ->
        FriendLookupSheet(
            lookup = lookup,
            isFriendLimitReached = uiState.isFriendLimitReached,
            maxFriends = uiState.maxFriends,
            onAction = onAction
        )
    }
}

@Composable
private fun AddFriendTopBar(
    friendCount: Int,
    maxFriends: Int,
    onBackClick: () -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val isFull = friendCount >= maxFriends

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.sm, vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Quay lại",
                tint = colors.textPrimary
            )
        }
        Text(
            text = "Thêm bạn",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .padding(end = spacing.sm)
                .clip(LokapTheme.shapes.pill)
                .background(if (isFull) colors.accentYellow.copy(alpha = 0.2f) else colors.primarySoft)
                .padding(horizontal = spacing.sm, vertical = spacing.xs)
        ) {
            Text(
                text = "$friendCount/$maxFriends",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isFull) colors.accentYellow else colors.primary
            )
        }
    }
}

/** Thẻ trắng chứa QR + avatar ở giữa, quầng hồng phía sau. */
@Composable
private fun MyCodeCard(me: User?) {
    val colors = LokapTheme.colors
    val username = me?.username.orEmpty()
    val cardShape = RoundedCornerShape(32.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth(0.84f)
            .widthIn(max = 340.dp)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        // Quầng sáng hồng phía sau thẻ
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        listOf(colors.primary.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize(0.9f)
                .clip(cardShape)
                .background(Color.White)
                .border(3.dp, colors.primary, cardShape)
                .padding(26.dp),
            contentAlignment = Alignment.Center
        ) {
            if (username.isNotBlank()) {
                FriendQrCode(
                    content = FriendCode.linkFor(username),
                    contentDescription = "Mã QR kết bạn của @$username",
                    modifier = Modifier.fillMaxSize()
                ) { size ->
                    AvatarRing(
                        avatarUrl = me?.avatarUrl,
                        displayName = me?.displayName.orEmpty(),
                        size = size,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(4.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Filled.QrCode2,
                    contentDescription = "Chưa có mã kết bạn",
                    tint = Color(0xFFBDBDC4),
                    modifier = Modifier.size(96.dp)
                )
            }
        }
    }
}

/** Link kết bạn (cùng nội dung với QR) và nút sao chép. Bấm xong icon đổi thành dấu tích 2 giây. */
@Composable
private fun FriendLinkRow(link: String, onCopy: () -> Unit) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val shape = LokapTheme.shapes.pill
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(2_000)
            copied = false
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceHigh)
            .border(1.dp, colors.outline, shape)
            .padding(start = spacing.md, end = spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Link,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(spacing.sm))
        Text(
            text = link,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = {
                onCopy()
                copied = true
            }
        ) {
            Icon(
                imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                contentDescription = if (copied) "Đã sao chép link" else "Sao chép link kết bạn",
                tint = colors.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun AddFriendActionButton(
    icon: ImageVector,
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = LokapTheme.colors
    val shape = RoundedCornerShape(20.dp)
    val container = when {
        !enabled -> colors.surface
        filled -> colors.primary
        else -> colors.surfaceHigh
    }
    val content = when {
        !enabled -> colors.textTertiary
        filled -> colors.onPrimary
        else -> colors.textPrimary
    }

    Column(
        modifier = modifier
            .clip(shape)
            .background(container)
            .then(if (filled) Modifier else Modifier.border(1.dp, colors.outline, shape))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = LokapTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.xs)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(30.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = content
        )
    }
}

@Composable
private fun FriendLimitNote(maxFriends: Int) {
    val colors = LokapTheme.colors
    Row(
        modifier = Modifier
            .clip(LokapTheme.shapes.pill)
            .background(colors.accentYellow.copy(alpha = 0.15f))
            .padding(horizontal = LokapTheme.spacing.md, vertical = LokapTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.sm)
    ) {
        Icon(
            imageVector = Icons.Filled.WarningAmber,
            contentDescription = null,
            tint = colors.accentYellow,
            modifier = Modifier.size(30.dp)
        )
        Text(
            text = "Đã đủ $maxFriends bạn",
            style = MaterialTheme.typography.labelLarge,
            color = colors.accentYellow
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendLookupSheet(
    lookup: FriendLookup,
    isFriendLimitReached: Boolean,
    maxFriends: Int,
    onAction: (AddFriendAction) -> Unit
) {
    val colors = LokapTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { onAction(AddFriendAction.DismissLookup) },
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LokapTheme.spacing.xl)
                .padding(bottom = LokapTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (lookup) {
                is FriendLookup.Loading -> LookupLoading(username = lookup.username)
                is FriendLookup.Found -> LookupFound(
                    lookup = lookup,
                    isFriendLimitReached = isFriendLimitReached,
                    maxFriends = maxFriends,
                    onAction = onAction
                )
                is FriendLookup.NotFound -> LookupMessage(
                    icon = Icons.Filled.PersonSearch,
                    title = "Không tìm thấy @${lookup.username}",
                    buttonLabel = "Đóng",
                    primaryButton = false,
                    onClick = { onAction(AddFriendAction.DismissLookup) }
                )
                is FriendLookup.Failed -> LookupMessage(
                    icon = Icons.Filled.ErrorOutline,
                    title = lookup.message,
                    buttonLabel = "Thử lại",
                    primaryButton = true,
                    onClick = { onAction(AddFriendAction.RetryLookup) }
                )
            }
        }
    }
}

@Composable
private fun LookupLoading(username: String) {
    Column(
        modifier = Modifier.padding(vertical = LokapTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LokapTheme.spacing.md)
    ) {
        CircularProgressIndicator(color = LokapTheme.colors.primary)
        Text(
            text = "@$username",
            style = MaterialTheme.typography.bodyLarge,
            color = LokapTheme.colors.textSecondary
        )
    }
}

@Composable
private fun LookupFound(
    lookup: FriendLookup.Found,
    isFriendLimitReached: Boolean,
    maxFriends: Int,
    onAction: (AddFriendAction) -> Unit
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val user = lookup.user
    val openProfile = { onAction(AddFriendAction.OpenProfile(user.id)) }

    AvatarRing(
        avatarUrl = user.avatarUrl,
        displayName = user.displayName,
        size = 96.dp,
        hasNewPost = true,
        onClick = if (lookup.isSelf) null else openProfile
    )
    Spacer(modifier = Modifier.height(spacing.md))
    Text(
        text = user.displayName,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = if (lookup.isSelf) Modifier else Modifier.clickable(onClick = openProfile)
    )
    Text(
        text = "@${user.username}",
        style = MaterialTheme.typography.bodyLarge,
        color = colors.textSecondary
    )
    if (user.bio.isNotBlank()) {
        Spacer(modifier = Modifier.height(spacing.xs))
        Text(
            text = user.bio,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }

    statusChip(lookup)?.let { (icon, label) ->
        Spacer(modifier = Modifier.height(spacing.md))
        StatusChip(icon = icon, label = label)
    }

    Spacer(modifier = Modifier.height(spacing.xl))

    val buttonModifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
    when {
        lookup.isSelf -> GhostButton(
            text = "Đóng",
            onClick = { onAction(AddFriendAction.DismissLookup) },
            modifier = buttonModifier
        )
        lookup.status == FriendStatus.NONE -> {
            PinkButton(
                text = "Kết bạn",
                leadingIcon = Icons.Filled.PersonAdd,
                enabled = !isFriendLimitReached,
                loading = lookup.isWorking,
                onClick = { onAction(AddFriendAction.SendRequest) },
                modifier = buttonModifier
            )
            if (isFriendLimitReached) {
                Spacer(modifier = Modifier.height(spacing.md))
                FriendLimitNote(maxFriends = maxFriends)
            }
        }
        lookup.status == FriendStatus.REQUEST_RECEIVED -> PinkButton(
            text = "Chấp nhận",
            leadingIcon = Icons.Filled.Check,
            enabled = !isFriendLimitReached,
            loading = lookup.isWorking,
            onClick = { onAction(AddFriendAction.AcceptRequest) },
            modifier = buttonModifier
        )
        lookup.status == FriendStatus.REQUEST_SENT -> GhostButton(
            text = "Huỷ lời mời",
            loading = lookup.isWorking,
            onClick = { onAction(AddFriendAction.CancelRequest) },
            modifier = buttonModifier
        )
        else -> GhostButton(
            text = "Xem hồ sơ",
            leadingIcon = Icons.Filled.Person,
            onClick = openProfile,
            modifier = buttonModifier
        )
    }
}

private fun statusChip(lookup: FriendLookup.Found): Pair<ImageVector, String>? = when {
    lookup.isSelf -> Icons.Filled.QrCode2 to "Mã của bạn"
    lookup.status == FriendStatus.FRIENDS -> Icons.Filled.Check to "Bạn bè"
    lookup.status == FriendStatus.REQUEST_SENT -> Icons.Filled.Schedule to "Đã gửi lời mời"
    lookup.status == FriendStatus.REQUEST_RECEIVED -> Icons.Filled.PersonAdd to "Muốn kết bạn"
    else -> null
}

@Composable
private fun StatusChip(icon: ImageVector, label: String) {
    val colors = LokapTheme.colors
    Row(
        modifier = Modifier
            .clip(LokapTheme.shapes.pill)
            .background(colors.primarySoft)
            .padding(horizontal = LokapTheme.spacing.md, vertical = LokapTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LokapTheme.spacing.xs)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(30.dp))
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = colors.primary)
    }
}

@Composable
private fun LookupMessage(
    icon: ImageVector,
    title: String,
    buttonLabel: String,
    primaryButton: Boolean,
    onClick: () -> Unit
) {
    val colors = LokapTheme.colors
    Box(
        modifier = Modifier
            .padding(top = LokapTheme.spacing.md)
            .size(72.dp)
            .clip(CircleShape)
            .background(colors.surfaceHigh),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(32.dp))
    }
    Spacer(modifier = Modifier.height(LokapTheme.spacing.md))
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(LokapTheme.spacing.xl))
    val buttonModifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
    if (primaryButton) {
        PinkButton(text = buttonLabel, onClick = onClick, modifier = buttonModifier)
    } else {
        GhostButton(text = buttonLabel, onClick = onClick, modifier = buttonModifier)
    }
}

private val previewMe = User(
    id = "me",
    username = "minhkhoi",
    displayName = "Minh Khôi",
    avatarUrl = null
)

@Preview(name = "Thêm bạn", showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun AddFriendPreview() {
    LokapTheme {
        AddFriendScreen(
            uiState = AddFriendUiState(me = previewMe, friendCount = 5),
            onAction = {}
        )
    }
}

@Preview(name = "Thêm bạn - Đủ 20 bạn", showBackground = true, backgroundColor = 0xFF0B0B0D)
@Composable
private fun AddFriendFullPreview() {
    LokapTheme {
        AddFriendScreen(
            uiState = AddFriendUiState(me = previewMe, friendCount = 20),
            onAction = {}
        )
    }
}
