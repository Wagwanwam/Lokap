package vn.nhom2.lokap.feature.post

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.domain.model.Reaction

/** Bộ cảm xúc của bài (giống hàng emoji cũ ở màn chi tiết). Bấm lại emoji đang chọn = bỏ cảm xúc. */
val REACTION_EMOJIS = listOf("😍", "😂", "🔥", "😮", "😢")

/** 3 emoji hiện sẵn trên thanh nhắn tin dưới ảnh bạn bè. */
private val QUICK_REACTIONS = REACTION_EMOJIS.take(3)

/**
 * Số cảm xúc của bài: tối đa 3 emoji nhiều nhất + tổng. Bấm vào thì hiện bảng 5 emoji để chọn
 * (emoji đang chọn được tô, kèm số người chọn từng emoji).
 */
@Composable
fun ReactionCountButton(
    reactions: List<Reaction>,
    myReaction: String?,
    onReact: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    var pickerOpen by remember { mutableStateOf(false) }
    val total = reactions.sumOf { it.count }
    val topEmojis = remember(reactions) {
        reactions.filter { it.count > 0 }.sortedByDescending { it.count }.take(3).map { it.emoji }
    }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(LokapTheme.shapes.pill)
                .clickable(role = Role.Button, onClickLabel = "Chọn cảm xúc") { pickerOpen = true }
                .semantics(mergeDescendants = true) {
                    contentDescription = if (myReaction != null) "Cảm xúc $total, bạn đã chọn $myReaction" else "Cảm xúc $total"
                }
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (topEmojis.isEmpty()) {
                Icon(
                    imageVector = Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                    topEmojis.forEach { emoji -> Text(text = emoji, fontSize = 20.sp) }
                }
            }
            Text(
                text = total.toString(),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (myReaction != null) colors.primary else colors.textSecondary
            )
        }
        ReactionPickerMenu(
            expanded = pickerOpen,
            reactions = reactions,
            myReaction = myReaction,
            onDismiss = { pickerOpen = false },
            onPick = { emoji ->
                pickerOpen = false
                onReact(emoji)
            }
        )
    }
}

/** Nút số bình luận; bấm mở popup bình luận toàn màn hình. */
@Composable
fun CommentCountButton(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LokapTheme.colors
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(LokapTheme.shapes.pill)
            .clickable(role = Role.Button, onClickLabel = "Mở bình luận", onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "Bình luận $count" }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.ChatBubbleOutline,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textSecondary
        )
    }
}

/**
 * Nút đăng lại ↻ + số lượt. Đã đăng lại thì tô màu chính; [enabled] = false (ảnh của mình, ảnh đang hẹn giờ)
 * thì chỉ hiện số lượt, không bấm được.
 */
@Composable
fun RepostButton(
    count: Int,
    isReposted: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val tint = if (isReposted) colors.primary else colors.textSecondary
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(LokapTheme.shapes.pill)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClickLabel = if (isReposted) "Bỏ đăng lại" else "Đăng lại",
                onClick = onClick
            )
            .semantics(mergeDescendants = true) {
                selected = isReposted
                contentDescription = if (isReposted) "Đã đăng lại, $count lượt" else "Đăng lại, $count lượt"
            }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Repeat,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = tint
        )
    }
}

/** Bảng 5 emoji nổi cạnh nút vừa bấm. */
@Composable
fun ReactionPickerMenu(
    expanded: Boolean,
    reactions: List<Reaction>,
    myReaction: String?,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    val colors = LokapTheme.colors
    val counts = remember(reactions) { reactions.associate { it.emoji to it.count } }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset(0.dp, 4.dp),
        shape = LokapTheme.shapes.pill,
        containerColor = colors.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            REACTION_EMOJIS.forEach { emoji ->
                val isSelected = emoji == myReaction
                val count = counts[emoji] ?: 0
                Column(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) colors.primary.copy(alpha = 0.18f) else Color.Transparent)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = if (isSelected) "Bỏ cảm xúc" else "Chọn cảm xúc"
                        ) { onPick(emoji) }
                        .semantics {
                            selected = isSelected
                            contentDescription = if (count > 0) "$emoji, $count" else emoji
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = emoji, fontSize = 26.sp)
                    if (count > 0) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isSelected) colors.primary else colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Thanh dưới ảnh của bạn bè (kiểu Locket): gõ tin nhắn → gửi vào Chat kèm ảnh này.
 * Chưa gõ gì thì hiện 3 emoji thả nhanh + nút mở đủ 5 emoji; đang gõ thì thay bằng nút Gửi.
 */
@Composable
fun PhotoMessageBar(
    text: String,
    onTextChange: (String) -> Unit,
    isSending: Boolean,
    onSend: () -> Unit,
    reactions: List<Reaction>,
    myReaction: String?,
    onReact: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** false: ảnh công khai đã có hàng ❤ 💬 ↻ ngay trên, thanh chỉ còn ô nhập + nút Gửi. */
    showQuickReactions: Boolean = true
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val focusManager = LocalFocusManager.current
    var pickerOpen by remember { mutableStateOf(false) }
    val canSend = text.isNotBlank() && !isSending
    val submit = {
        if (canSend) {
            onSend()
            focusManager.clearFocus()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(LokapTheme.shapes.pill)
            .background(colors.surfaceHigh)
            .border(1.dp, colors.outline, LokapTheme.shapes.pill)
            .padding(start = spacing.lg, end = spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = spacing.sm),
            contentAlignment = Alignment.CenterStart
        ) {
            if (text.isEmpty()) {
                Text(
                    text = "Gửi tin nhắn…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                    maxLines = 1
                )
            }
            BasicTextField(
                value = text,
                onValueChange = { onTextChange(it.take(1000)) },
                enabled = !isSending,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.primary),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Gửi tin nhắn kèm ảnh" }
            )
        }

        if (text.isBlank() && !isSending && showQuickReactions) {
            QUICK_REACTIONS.forEach { emoji ->
                val isSelected = emoji == myReaction
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) colors.primary.copy(alpha = 0.18f) else Color.Transparent)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = if (isSelected) "Bỏ cảm xúc" else "Thả cảm xúc"
                        ) { onReact(emoji) }
                        .semantics {
                            selected = isSelected
                            contentDescription = emoji
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 26.sp)
                }
            }
            Box {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClickLabel = "Thêm cảm xúc") { pickerOpen = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddReaction,
                        contentDescription = "Thêm cảm xúc",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                ReactionPickerMenu(
                    expanded = pickerOpen,
                    reactions = reactions,
                    myReaction = myReaction,
                    onDismiss = { pickerOpen = false },
                    onPick = { emoji ->
                        pickerOpen = false
                        onReact(emoji)
                    }
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (canSend) colors.primary else colors.surface)
                    .clickable(enabled = canSend, role = Role.Button, onClickLabel = "Gửi", onClick = submit),
                contentAlignment = Alignment.Center
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = colors.primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Gửi",
                        tint = colors.buttonText,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
