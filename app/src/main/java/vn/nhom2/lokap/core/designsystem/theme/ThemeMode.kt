package vn.nhom2.lokap.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
enum class ThemeMode(val labelVi: String) {
    SYSTEM("Theo hệ thống"),
    LIGHT("Giao diện sáng"),
    DARK("Giao diện tối")
}

object ThemeManager {
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun toggleTheme() {
        _themeMode.value = when (_themeMode.value) {
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.SYSTEM -> ThemeMode.DARK
        }
    }
}

val LocalThemeMode = staticCompositionLocalOf { ThemeMode.SYSTEM }
