package vn.nhom2.lokap.data.local

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import vn.nhom2.lokap.core.designsystem.theme.ThemeManager
import vn.nhom2.lokap.core.designsystem.theme.ThemeMode
import vn.nhom2.lokap.core.settings.DevicePreferenceStore
import vn.nhom2.lokap.domain.model.DeviceSettingsData

class DeviceSettingsPersistenceTest {
    @After
    fun resetTheme() {
        ThemeManager.initialize(MemoryPreferences())
    }

    @Test
    fun `first launch uses device defaults`() {
        val settings = PersistentDeviceSettingsRepository(MemoryPreferences())
        assertEquals(DeviceSettingsData(), settings.settings.value)
    }

    @Test
    fun `data saver and gallery choice survive repository recreation`() {
        val disk = MemoryPreferences()
        val firstSession = PersistentDeviceSettingsRepository(disk)
        firstSession.setDataSaver(true)
        firstSession.setAutoSave(true)
        firstSession.setHighQuality(false)

        val reopened = PersistentDeviceSettingsRepository(disk)
        assertEquals(
            DeviceSettingsData(dataSaverEnabled = true, autoSaveToGallery = true, highQualityUpload = false),
            reopened.settings.value
        )

        // Tắt một công tắc sau khi mở lại không làm mất lựa chọn của công tắc khác.
        reopened.setDataSaver(false)
        assertEquals(
            DeviceSettingsData(dataSaverEnabled = false, autoSaveToGallery = true, highQualityUpload = false),
            PersistentDeviceSettingsRepository(disk).settings.value
        )
        reopened.setAutoSave(false)
        reopened.setHighQuality(true)
        assertEquals(DeviceSettingsData(), PersistentDeviceSettingsRepository(disk).settings.value)
    }

    @Test
    fun `theme restores before drawing after fresh application initialization`() {
        val disk = MemoryPreferences()
        ThemeManager.initialize(disk)
        ThemeManager.setThemeMode(ThemeMode.DARK)

        // Xoá trạng thái trong bộ nhớ bằng một phiên khác, rồi nạp lại kho đã lưu.
        ThemeManager.initialize(MemoryPreferences())
        assertEquals(ThemeMode.SYSTEM, ThemeManager.themeMode.value)
        ThemeManager.initialize(disk)
        assertEquals(ThemeMode.DARK, ThemeManager.themeMode.value)

        ThemeManager.toggleTheme()
        ThemeManager.initialize(disk)
        assertEquals(ThemeMode.LIGHT, ThemeManager.themeMode.value)

        ThemeManager.setThemeMode(ThemeMode.SYSTEM)
        ThemeManager.initialize(disk)
        assertEquals(ThemeMode.SYSTEM, ThemeManager.themeMode.value)
    }

    @Test
    fun `unknown saved theme falls back to system without losing other choices`() {
        val disk = MemoryPreferences()
        val settings = PersistentDeviceSettingsRepository(disk)
        settings.setAutoSave(true)
        disk.putString("theme_mode", "theme_removed_in_new_version")

        ThemeManager.initialize(disk)
        assertEquals(ThemeMode.SYSTEM, ThemeManager.themeMode.value)
        assertEquals(true, PersistentDeviceSettingsRepository(disk).settings.value.autoSaveToGallery)
    }

    private class MemoryPreferences : DevicePreferenceStore {
        private val values = mutableMapOf<String, Any>()
        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = values[key] as? Boolean ?: defaultValue
        override fun putBoolean(key: String, value: Boolean) { values[key] = value }
        override fun getString(key: String): String? = values[key] as? String
        override fun putString(key: String, value: String) { values[key] = value }
    }
}
