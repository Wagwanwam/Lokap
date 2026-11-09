package vn.nhom2.lokap

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.nhom2.lokap.core.designsystem.theme.LokapTheme
import vn.nhom2.lokap.core.designsystem.theme.ThemeManager
import vn.nhom2.lokap.core.designsystem.theme.ThemeMode
import vn.nhom2.lokap.core.navigation.AppNavHost
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by ThemeManager.themeMode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemDark
            }

            DisposableEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                )
                onDispose {}
            }

            LokapTheme(themeMode = themeMode) {
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
