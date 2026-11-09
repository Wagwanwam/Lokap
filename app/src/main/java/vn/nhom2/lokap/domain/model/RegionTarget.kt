package vn.nhom2.lokap.domain.model

fun PostLocation.toRegionAlbumTarget(): Pair<String, String>? = when (level) {
    LocationLevel.PLACE, LocationLevel.WARD -> when {
        !wardCode.isNullOrBlank() -> RegionLevel.WARD.name to wardCode
        !provinceCode.isNullOrBlank() -> RegionLevel.PROVINCE.name to provinceCode
        else -> null
    }
    LocationLevel.PROVINCE -> provinceCode?.takeIf { it.isNotBlank() }?.let { RegionLevel.PROVINCE.name to it }
    LocationLevel.COUNTRY -> null
}
