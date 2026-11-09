package vn.nhom2.lokap.domain.model

import androidx.compose.runtime.Immutable
enum class RegionLevel {
    PROVINCE,
    WARD
}

@Immutable
data class Province(
    val code: String,
    val name: String,
    val isCity: Boolean,
    val lat: Double,
    val lng: Double
)

@Immutable
data class Ward(
    val code: String,
    val provinceCode: String,
    val name: String,
    val lat: Double,
    val lng: Double
)
