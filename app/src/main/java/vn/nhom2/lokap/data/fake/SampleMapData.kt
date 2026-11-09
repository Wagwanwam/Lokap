package vn.nhom2.lokap.data.fake

import vn.nhom2.lokap.domain.model.*
object SampleMapData {

    private val extraProvinceBubbles = listOf(
        MapBubble(
            id = "prov_hue",
            type = MapBubbleType.PROVINCE,
            code = "hue",
            label = "TP. Huế",
            lat = 16.4637,
            lng = 107.5909,
            postCount = 48,
            coverImageUrl = "https://images.unsplash.com/photo-1583417319070-4a69db38a482?w=400",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "prov_hn",
            type = MapBubbleType.PROVINCE,
            code = "ha-noi",
            label = "Hà Nội",
            lat = 21.0285,
            lng = 105.8542,
            postCount = 124,
            coverImageUrl = "https://images.unsplash.com/photo-1509030450996-dd1a26dda07a?w=400",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "prov_hcm",
            type = MapBubbleType.PROVINCE,
            code = "tp-ho-chi-minh",
            label = "TP. Hồ Chí Minh",
            lat = 10.7769,
            lng = 106.7009,
            postCount = 215,
            coverImageUrl = "https://images.unsplash.com/photo-1583417267826-aebc4d1542e1?w=400",
            hasFriendPosts = true
        )
    )

    val allProvinceBubbles: List<MapBubble>
        get() {
            val handMade = (SampleData.provinceMapBubbles + extraProvinceBubbles).associateBy { it.code }
            return ProvinceCatalog.allProvinces.map { p ->
                handMade[p.code]?.copy(label = p.name, lat = p.lat, lng = p.lng)
                    ?: MapBubble(
                        id = "prov_${p.code}",
                        type = MapBubbleType.PROVINCE,
                        code = p.code,
                        label = p.name,
                        lat = p.lat,
                        lng = p.lng,
                        postCount = 0,
                        coverImageUrl = null,
                        hasFriendPosts = false
                    )
            }
        }

    private val provinceWideBubbles: List<MapBubble>
        get() = ProvinceCatalog.allProvinces.map { p ->
            MapBubble(
                id = "pw_${p.code}",
                type = MapBubbleType.PROVINCE_WIDE,
                code = p.code,
                label = "Khắp ${p.name}",
                lat = p.lat + 0.02,
                lng = p.lng - 0.03,
                postCount = 0,
                coverImageUrl = null,
                hasFriendPosts = false
            )
        }

    private val extraWardBubbles = listOf(

        MapBubble(
            id = "pw_dn",
            type = MapBubbleType.PROVINCE_WIDE,
            code = "da-nang",
            label = "Khắp Đà Nẵng",
            lat = 16.0750,
            lng = 108.1800,
            postCount = 19,
            coverImageUrl = "https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?w=400",
            hasFriendPosts = true
        ),

        MapBubble(
            id = "pw_hue",
            type = MapBubbleType.PROVINCE_WIDE,
            code = "hue",
            label = "Khắp TP. Huế",
            lat = 16.4800,
            lng = 107.5600,
            postCount = 12,
            coverImageUrl = "https://images.unsplash.com/photo-1583417319070-4a69db38a482?w=400",
            hasFriendPosts = false
        ),
        MapBubble(
            id = "ward_hue_thuanhoa",
            type = MapBubbleType.WARD,
            code = "hue-thuan-hoa",
            label = "P. Thuận Hóa",
            lat = 16.4698,
            lng = 107.5786,
            postCount = 21,
            coverImageUrl = "https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?w=400",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "ward_hue_vy_da",
            type = MapBubbleType.WARD,
            code = "hue-vy-da",
            label = "P. Vỹ Dạ",
            lat = 16.4765,
            lng = 107.6052,
            postCount = 15,
            coverImageUrl = "https://images.unsplash.com/photo-1528127269322-539801943592?w=400",
            hasFriendPosts = true
        ),

        MapBubble(
            id = "pw_hn",
            type = MapBubbleType.PROVINCE_WIDE,
            code = "ha-noi",
            label = "Khắp Hà Nội",
            lat = 21.0450,
            lng = 105.8200,
            postCount = 34,
            coverImageUrl = "https://images.unsplash.com/photo-1509030450996-dd1a26dda07a?w=400",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "ward_hn_hoankiem",
            type = MapBubbleType.WARD,
            code = "hn-hoan-kiem",
            label = "P. Hoàn Kiếm",
            lat = 21.0328,
            lng = 105.8525,
            postCount = 52,
            coverImageUrl = "https://images.unsplash.com/photo-1555921015-5532091f6026?w=400",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "ward_hn_tayho",
            type = MapBubbleType.WARD,
            code = "hn-tay-ho",
            label = "P. Tây Hồ",
            lat = 21.0645,
            lng = 105.8235,
            postCount = 38,
            coverImageUrl = "https://images.unsplash.com/photo-1509030450996-dd1a26dda07a?w=400",
            hasFriendPosts = false
        ),

        MapBubble(
            id = "pw_hcm",
            type = MapBubbleType.PROVINCE_WIDE,
            code = "tp-ho-chi-minh",
            label = "Khắp TP.HCM",
            lat = 10.7950,
            lng = 106.6750,
            postCount = 45,
            coverImageUrl = "https://images.unsplash.com/photo-1583417267826-aebc4d1542e1?w=400",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "ward_hcm_saigon",
            type = MapBubbleType.WARD,
            code = "hcm-sai-gon",
            label = "P. Sài Gòn",
            lat = 10.7797,
            lng = 106.7019,
            postCount = 96,
            coverImageUrl = "https://images.unsplash.com/photo-1583417267826-aebc4d1542e1?w=400",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "ward_hcm_ankhanh",
            type = MapBubbleType.WARD,
            code = "hcm-an-khanh",
            label = "P. An Khánh",
            lat = 10.8035,
            lng = 106.7369,
            postCount = 74,
            coverImageUrl = "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=400",
            hasFriendPosts = true
        )
    )

    val allWardAndProvinceWideBubbles: List<MapBubble>
        get() = (SampleData.wardMapBubblesDaNang + extraWardBubbles + provinceWideBubbles)
            .distinctBy { it.type to (it.code ?: it.id) }

    private val extraPlaceBubbles = listOf(

        MapBubble(
            id = "place_hue_1",
            type = MapBubbleType.PLACE,
            code = null,
            label = "Cà phê Muối Chú Long",
            lat = 16.4682,
            lng = 107.5841,
            postCount = 1,
            coverImageUrl = "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=400",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "place_hue_2",
            type = MapBubbleType.PLACE,
            code = null,
            label = "Đồi Vọng Cảnh hoàng hôn",
            lat = 16.4312,
            lng = 107.5612,
            postCount = 3,
            coverImageUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=400",
            hasFriendPosts = false
        ),

        MapBubble(
            id = "place_hn_1",
            type = MapBubbleType.PLACE,
            code = null,
            label = "Trill Rooftop Cafe",
            lat = 21.0315,
            lng = 105.8512,
            postCount = 4,
            coverImageUrl = "https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=400",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "place_hn_2",
            type = MapBubbleType.PLACE,
            code = null,
            label = "Hồ Tây chiều thu",
            lat = 21.0610,
            lng = 105.8215,
            postCount = 7,
            coverImageUrl = "https://images.unsplash.com/photo-1509030450996-dd1a26dda07a?w=400",
            hasFriendPosts = true
        ),

        MapBubble(
            id = "place_hcm_1",
            type = MapBubbleType.PLACE,
            code = null,
            label = "Bến Bạch Đằng chill đêm",
            lat = 10.7745,
            lng = 106.7068,
            postCount = 9,
            coverImageUrl = "https://images.unsplash.com/photo-1583417267826-aebc4d1542e1?w=400",
            hasFriendPosts = true
        ),
        MapBubble(
            id = "place_hcm_2",
            type = MapBubbleType.PLACE,
            code = null,
            label = "Matcha Vibe Thảo Điền",
            lat = 10.8041,
            lng = 106.7340,
            postCount = 5,
            coverImageUrl = "https://images.unsplash.com/photo-1515823064-d6e0c04616a7?w=400",
            hasFriendPosts = false
        )
    )

    val allPlaceBubbles: List<MapBubble>

        get() = (SampleData.detailMapBubblesDaNang + extraPlaceBubbles)
            .filter { it.type == MapBubbleType.PLACE }
            .distinctBy { it.id }

    fun findRegionName(level: String, code: String): String {
        val all = allProvinceBubbles + allWardAndProvinceWideBubbles
        all.firstOrNull { it.code == code }?.let { bubble ->
            return bubble.label.removePrefix("Khắp ").trim()
        }
        return when (code) {
            "48" -> "Đà Nẵng"
            "46" -> "TP. Huế"
            "01" -> "Hà Nội"
            "79" -> "TP. Hồ Chí Minh"
            else -> if (level.equals("WARD", ignoreCase = true)) "Phường $code" else "Khu vực $code"
        }
    }
}
