package vn.nhom2.lokap.feature

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import vn.nhom2.lokap.domain.model.DeviceSettingsData
import vn.nhom2.lokap.domain.repository.AppCacheRepository
import vn.nhom2.lokap.domain.repository.DeviceSettingsRepository
import vn.nhom2.lokap.core.settings.DevicePreferenceStore

class TestDeviceSettingsRepository : DeviceSettingsRepository {
    override val settings = MutableStateFlow(DeviceSettingsData())
    override fun setDataSaver(enabled: Boolean) { settings.update { it.copy(dataSaverEnabled = enabled) } }
    override fun setAutoSave(enabled: Boolean) { settings.update { it.copy(autoSaveToGallery = enabled) } }
    override fun setHighQuality(enabled: Boolean) { settings.update { it.copy(highQualityUpload = enabled) } }
}

class TestAppCacheRepository : AppCacheRepository {
    var calls = 0
    var result: Result<Unit> = Result.success(Unit)
    override suspend fun clearCache(): Result<Unit> { calls++; return result }
}

/** Shares the persisted bytes between repository instances without retaining a ViewModel. */
class TestContactPreferences : DevicePreferenceStore {
    private val values = mutableMapOf<String, Any>()
    var failWrites = false
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = values[key] as? Boolean ?: defaultValue
    override fun putBoolean(key: String, value: Boolean) { values[key] = value }
    override fun getString(key: String): String? = values[key] as? String
    override fun putString(key: String, value: String) {
        check(!failWrites) { "Disk is unavailable" }
        values[key] = value
    }
}
