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

    // ---------- Rà soát tương phản 15/11/2026 ----------

    @Test
    fun accentYellowStrong_isReadableOnEveryBackgroundInBothThemes() {
        // accentYellow (FACC15) làm chữ trên nền sáng chỉ 1,3–1,5:1: "Đã đủ 20 bạn", icon "Mất mạng" gần như vô hình
        for (c in listOf(LightLokapColors(), DarkLokapColors())) {
            for ((name, bg) in listOf("background" to c.background, "surface" to c.surface, "surfaceHigh" to c.surfaceHigh)) {
                val ratio = contrastRatio(c.accentYellowStrong, bg)
                assertTrue("accentYellowStrong trên $name (isDark=${c.isDark}) = $ratio", ratio >= 4.5f)
            }
        }
    }

    @Test
    fun onPrimarySoft_isReadableOnPrimarySoft() {
        // Hồng primary trên nền primarySoft của giao diện tối (FBCFE8) chỉ còn 1,92:1
        for (c in listOf(LightLokapColors(), DarkLokapColors())) {
            val ratio = contrastRatio(c.onPrimarySoft, c.primarySoft)
            assertTrue("onPrimarySoft trên primarySoft (isDark=${c.isDark}) = $ratio", ratio >= 4.5f)
        }
    }

    @Test
    fun readableOn_keepsReadableColorAndFallsBackWhenTooFaint() {
        val light = LightLokapColors()
        val dark = DarkLokapColors()
        val oceanBlue = Color(0xFF007AFF)   // chủ đề chat "Xanh Dương": 4,0:1 trên nền trắng
        val midnight = Color(0xFF232731)    // chủ đề "Đen Than": gần trùng nền tối
        val goldenHour = Color(0xFFF5AF19)  // chủ đề "Nắng Vàng": chưa tới 2:1 trên nền trắng
        assertEquals(oceanBlue, oceanBlue.readableOn(light.surface, light.textSecondary))
        assertEquals(dark.textSecondary, midnight.readableOn(dark.surface, dark.textSecondary))
        assertEquals(light.textPrimary, goldenHour.readableOn(light.surface, light.textPrimary))
        assertTrue(contrastRatio(Color.Black, Color.White) > 20.9f)
        assertEquals(1f, contrastRatio(oceanBlue, oceanBlue), 0.0001f)
    }
}
