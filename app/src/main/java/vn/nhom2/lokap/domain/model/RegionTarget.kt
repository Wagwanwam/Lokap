package vn.nhom2.lokap.domain.model

/**
 * Album khu vực ứng với chip vị trí của bài: (RegionLevel.name, code).
 * Trả về null khi bài chỉ ở mức "Việt Nam" — mức này không thuộc khu vực nào trên bản đồ.
 */
fun PostLocation.toRegionAlbumTarget(): Pair<String, String>? = when (level) {
    LocationLevel.PLACE, LocationLevel.WARD -> when {
        !wardCode.isNullOrBlank() -> RegionLevel.WARD.name to wardCode
        !provinceCode.isNullOrBlank() -> RegionLevel.PROVINCE.name to provinceCode
        else -> null
    }
    LocationLevel.PROVINCE -> provinceCode?.takeIf { it.isNotBlank() }?.let { RegionLevel.PROVINCE.name to it }
    LocationLevel.COUNTRY -> null
}
