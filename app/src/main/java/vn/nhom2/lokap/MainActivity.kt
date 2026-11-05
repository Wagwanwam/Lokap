package vn.nhom2.lokap

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.navigation.AppNavHost

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
                // Nền đen phủ toàn màn hình. Mỗi màn hình tự xử lý insets (systemBarsPadding),
                // nên ở đây KHÔNG thêm safeDrawingPadding để tránh đệm hai lần.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LokapTheme.colors.background)
                ) {
                    AppNavHost()
                }
            }
        }
    }
}
