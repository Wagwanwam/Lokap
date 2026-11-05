package vn.nhom2.lokap.data.fake

import vn.nhom2.lokap.domain.model.Place

object SampleData2 {

    const val DEFAULT_PROVINCE_CODE = "da-nang"
    const val DEFAULT_PROVINCE_NAME = "Đà Nẵng"
    const val DEFAULT_WARD_CODE = "dn-hai-chau"
    const val DEFAULT_WARD_NAME = "Phường Hải Châu"

    /**
     * 8 địa điểm mẫu quanh Đà Nẵng để chọn trong Composer khi chọn LocationLevel.PLACE
     */
    val daNangPlaces: List<Place> = listOf(
        Place(
            id = "pl_dn_1",
            name = "Trình Cà Phê",
            address = "25 Phạm Hồng Thái, Phường Hải Châu, Đà Nẵng",
            lat = 16.0682,
            lng = 108.2215
        ),
        Place(
            id = "pl_dn_2",
            name = "Bãi biển Mỹ Khê",
            address = "Đường Võ Nguyên Giáp, Phường Sơn Trà, Đà Nẵng",
            lat = 16.0598,
            lng = 108.2465
        ),
        Place(
            id = "pl_dn_3",
            name = "Trường Đại học Bách khoa - ĐH Đà Nẵng",
            address = "54 Nguyễn Lương Bằng, Đà Nẵng",
            lat = 16.0738,
            lng = 108.1499
        ),
        Place(
            id = "pl_dn_4",
            name = "Cầu Rồng",
            address = "Đường Nguyễn Văn Linh, Phường Hải Châu, Đà Nẵng",
            lat = 16.0611,
            lng = 108.2275
        ),
        Place(
            id = "pl_dn_5",
            name = "Chợ Cồn",
            address = "290 Hùng Vương, Phường Hải Châu, Đà Nẵng",
            lat = 16.0679,
            lng = 108.2144
        ),
        Place(
            id = "pl_dn_6",
            name = "Bán đảo Sơn Trà - Đỉnh Bàn Cờ",
            address = "Phường Sơn Trà, Đà Nẵng",
            lat = 16.1189,
            lng = 108.2736
        ),
        Place(
            id = "pl_dn_7",
            name = "Phố cổ Hội An",
            address = "Đường Trần Phú, Phường Hội An, Đà Nẵng",
            lat = 15.8794,
            lng = 108.3350
        ),
        Place(
            id = "pl_dn_8",
            name = "Reply 1988 Cafe",
            address = "20 Lê Hồng Phong, Phường Hải Châu, Đà Nẵng",
            lat = 16.0635,
            lng = 108.2192
        )
    )

    val placeToLandmarkMap: Map<String, String> = mapOf(
        "pl_dn_4" to "lm_cau_rong",
        "pl_dn_6" to "lm_son_tra",
        "pl_dn_7" to "lm_hoi_an"
    )

    val reportReasons: List<String> = listOf(
        "Spam",
        "Nhạy cảm",
        "Quấy rối",
        "Sai vị trí",
        "Khác"
    )

    fun resolveConversationIdForUser(userId: String): String {
        return SampleData.conversations
            .firstOrNull { it.friend.id == userId }
            ?.id ?: "conv_${userId}"
    }
}
