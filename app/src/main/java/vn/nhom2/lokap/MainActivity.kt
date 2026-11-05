package vn.nhom2.lokap

import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import vn.nhom2.lokap.core.designsystem.theme.DesignSystemPreview
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // App chỉ có dark mode: luôn dùng icon sáng trên system bar, không theo chế độ của máy.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            LokapTheme {
                // Nền đen phủ cả vùng status/nav bar; nội dung tránh đè lên system bars.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LokapTheme.colors.background)
                        .safeDrawingPadding()
                ) {
                    // Tạm thời: màn hình xem design system.
                    DesignSystemPreview()
                }
            }
        }
    }
}
