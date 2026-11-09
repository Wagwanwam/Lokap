package vn.nhom2.lokap.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ThemeColorTest {

    @Before
    fun setup() {
        ThemeManager.setThemeMode(ThemeMode.SYSTEM)
    }

    @Test
    fun themeManager_defaultIsSystem() {
        assertEquals(ThemeMode.SYSTEM, ThemeManager.themeMode.value)
    }

    @Test
    fun themeManager_setThemeModeWorks() {
        ThemeManager.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, ThemeManager.themeMode.value)

        ThemeManager.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, ThemeManager.themeMode.value)
    }

    @Test
    fun themeManager_toggleThemeSwitchesBetweenDarkAndLight() {
        ThemeManager.setThemeMode(ThemeMode.DARK)
        ThemeManager.toggleTheme()
        assertEquals(ThemeMode.LIGHT, ThemeManager.themeMode.value)

        ThemeManager.toggleTheme()
        assertEquals(ThemeMode.DARK, ThemeManager.themeMode.value)
    }

    @Test
    fun darkLokapColors_hasMonochromeDarkBackgroundAndSoftPinkPrimary() {
        val dark = DarkLokapColors()
        assertEquals(Color(0xFF121214), dark.background)
        assertEquals(Color(0xFFF472B6), dark.primary)
        assertEquals(Color(0xFFF4F4F5), dark.textPrimary)
    }

    @Test
    fun lightLokapColors_hasMonochromeLightBackgroundAndSoftPinkPrimary() {
        val light = LightLokapColors()
        assertEquals(Color(0xFFF8F9FA), light.background)
        assertEquals(Color(0xFFF472B6), light.primary)
        assertEquals(Color(0xFF18181B), light.textPrimary)
    }
}
