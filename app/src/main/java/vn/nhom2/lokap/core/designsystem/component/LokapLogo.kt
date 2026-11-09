package vn.nhom2.lokap.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import vn.nhom2.lokap.R
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
@Composable
fun LokapLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showText: Boolean = false,
    tint: Color? = null
) {
    val colors = LokapTheme.colors
    val spacing = LokapTheme.spacing
    val isDark = colors.isDark

    val logoRes = if (isDark) R.drawable.ic_lokap_logo_dark else R.drawable.ic_lokap_logo_light

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        Image(
            painter = painterResource(id = logoRes),
            contentDescription = "Lokap Logo",
            colorFilter = tint?.let { ColorFilter.tint(it) },
            modifier = Modifier.size(size)
        )

        if (showText) {
            Text(
                text = "Lokap",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        }
    }
}
