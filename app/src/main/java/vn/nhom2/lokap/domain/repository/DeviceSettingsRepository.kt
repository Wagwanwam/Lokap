package vn.nhom2.lokap.domain.repository

import kotlinx.coroutines.flow.StateFlow
import vn.nhom2.lokap.domain.model.DeviceSettingsData

interface DeviceSettingsRepository {
    val settings: StateFlow<DeviceSettingsData>

    fun setDataSaver(enabled: Boolean)
    fun setAutoSave(enabled: Boolean)
    fun setHighQuality(enabled: Boolean)
}
