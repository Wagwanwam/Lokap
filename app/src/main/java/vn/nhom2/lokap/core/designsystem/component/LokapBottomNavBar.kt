package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
enum class MainTab(val title: String) {
    FEED("Khám phá"),
    MAP("Bản đồ"),
    CAMERA("Chụp"),
    INBOX("Hộp thư"),
    PROFILE("Hồ sơ")
}

/** Chiều cao hàng tab (chưa tính thanh điều hướng hệ thống); màn nằm dưới thanh này dùng để chừa chỗ. */
val LokapBottomNavBarHeight = 68.dp

@Composable
fun LokapBottomNavBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = colors.surface,
        border = BorderStroke(
            width = 0.8.dp,
            color = colors.outline.copy(alpha = 0.25f)
        ),
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LokapBottomNavBarHeight)
                    .padding(horizontal = spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {

                BottomNavItem(
                    icon = if (currentTab == MainTab.FEED) Icons.Filled.Explore else Icons.Outlined.Explore,
                    label = MainTab.FEED.title,
                    isSelected = currentTab == MainTab.FEED,
                    onClick = { onTabSelected(MainTab.FEED) },
                    modifier = Modifier.weight(1f)
                )

                BottomNavItem(
                    icon = if (currentTab == MainTab.MAP) Icons.Filled.Map else Icons.Outlined.Map,
                    label = MainTab.MAP.title,
                    isSelected = currentTab == MainTab.MAP,
                    onClick = { onTabSelected(MainTab.MAP) },
                    modifier = Modifier.weight(1f)
                )

                CameraCenterButton(
                    isSelected = currentTab == MainTab.CAMERA,
                    onClick = { onTabSelected(MainTab.CAMERA) }
                )

                BottomNavItem(
                    icon = if (currentTab == MainTab.INBOX) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                    label = MainTab.INBOX.title,
                    isSelected = currentTab == MainTab.INBOX,
                    onClick = { onTabSelected(MainTab.INBOX) },
                    modifier = Modifier.weight(1f)
                )

                BottomNavItem(
                    icon = if (currentTab == MainTab.PROFILE) Icons.Filled.Person else Icons.Outlined.PersonOutline,
                    label = MainTab.PROFILE.title,
                    isSelected = currentTab == MainTab.PROFILE,
                    onClick = { onTabSelected(MainTab.PROFILE) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CameraCenterButton(
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LokapTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.88f
            isSelected -> 1.05f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    Box(
        modifier = Modifier
            .size(58.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (isSelected) colors.primary.copy(alpha = 0.25f)
                else colors.primary.copy(alpha = 0.12f)
            )
            .border(
                width = 1.5.dp,
                color = if (isSelected) colors.primary else colors.primary.copy(alpha = 0.4f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PhotoCamera,
                contentDescription = "Chụp ảnh",
                tint = colors.onPrimary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LokapTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.88f
            isSelected -> 1.08f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    val itemColor by animateColorAsState(
        targetValue = if (isSelected) colors.primary else colors.textTertiary,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = itemColor,
            modifier = Modifier
                .size(32.dp)
                .scale(scale)
        )
        // Nhãn lấp gần hết bề ngang ô tab; cùng cỡ tối đa để các tab đều nhau
        FitText(
            text = label,
            color = itemColor,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
            widthFraction = 0.9f,
            heightFraction = 1f,
            maxFontSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
        )
    }
}
