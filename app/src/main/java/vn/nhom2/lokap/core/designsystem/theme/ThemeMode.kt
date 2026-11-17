package vn.nhom2.lokap.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import vn.nhom2.lokap.core.settings.DevicePreferenceStore
enum class ThemeMode(val labelVi: String) {
    SYSTEM("Theo hệ thống"),
    LIGHT("Giao diện sáng"),
    DARK("Giao diện tối")
}

object ThemeManager {
    private const val THEME_MODE_KEY = "theme_mode"
    private var store: DevicePreferenceStore? = null
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    /** Gọi từ Application trước khi Activity vẽ, tránh chớp giao diện sáng khi mở lại. */
    fun initialize(store: DevicePreferenceStore) {
        this.store = store
        val savedMode = store.getString(THEME_MODE_KEY)
        _themeMode.value = ThemeMode.entries.firstOrNull { it.name == savedMode } ?: ThemeMode.SYSTEM
    }

    fun setThemeMode(mode: ThemeMode) {
        store?.putString(THEME_MODE_KEY, mode.name)
        _themeMode.value = mode
    }

    fun toggleTheme() {
        setThemeMode(when (_themeMode.value) {
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.SYSTEM -> ThemeMode.DARK
        })
    }
}

val LocalThemeMode = staticCompositionLocalOf { ThemeMode.SYSTEM }
