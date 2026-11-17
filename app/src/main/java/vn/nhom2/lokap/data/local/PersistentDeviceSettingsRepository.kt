package vn.nhom2.lokap.data.local

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import vn.nhom2.lokap.core.settings.DevicePreferenceStore
import vn.nhom2.lokap.domain.model.DeviceSettingsData
import vn.nhom2.lokap.domain.repository.DeviceSettingsRepository

@Singleton
class PersistentDeviceSettingsRepository @Inject constructor(
    private val store: DevicePreferenceStore
) : DeviceSettingsRepository {
    private val _settings = MutableStateFlow(
        DeviceSettingsData(
            dataSaverEnabled = store.getBoolean(DATA_SAVER, false),
            autoSaveToGallery = store.getBoolean(AUTO_SAVE, false),
            highQualityUpload = store.getBoolean(HIGH_QUALITY, true)
        )
    )
    override val settings: StateFlow<DeviceSettingsData> = _settings.asStateFlow()

    override fun setDataSaver(enabled: Boolean) {
        store.putBoolean(DATA_SAVER, enabled)
        _settings.update { it.copy(dataSaverEnabled = enabled) }
    }

    override fun setAutoSave(enabled: Boolean) {
        store.putBoolean(AUTO_SAVE, enabled)
        _settings.update { it.copy(autoSaveToGallery = enabled) }
    }

    override fun setHighQuality(enabled: Boolean) {
        store.putBoolean(HIGH_QUALITY, enabled)
        _settings.update { it.copy(highQualityUpload = enabled) }
    }

    private companion object {
        const val DATA_SAVER = "data_saver"
        const val AUTO_SAVE = "auto_save_to_gallery"
        const val HIGH_QUALITY = "high_quality_upload"
    }
}
