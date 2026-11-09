package vn.nhom2.lokap.data.fake

import vn.nhom2.lokap.domain.model.Province
object ProvinceCatalog {
    val allProvinces: List<Province> = listOf(

        Province(code = "ha-noi", name = "Hà Nội", isCity = true, lat = 21.0285, lng = 105.8542),
        Province(code = "hue", name = "Huế", isCity = true, lat = 16.4637, lng = 107.5909),
        Province(code = "hai-phong", name = "Hải Phòng", isCity = true, lat = 20.8449, lng = 106.6881),
        Province(code = "da-nang", name = "Đà Nẵng", isCity = true, lat = 16.0544, lng = 108.2022),
        Province(code = "tp-ho-chi-minh", name = "Thành phố Hồ Chí Minh", isCity = true, lat = 10.7769, lng = 106.7009),
        Province(code = "can-tho", name = "Cần Thơ", isCity = true, lat = 10.0452, lng = 105.7469),

        Province(code = "lai-chau", name = "Lai Châu", isCity = false, lat = 22.3864, lng = 103.4703),
        Province(code = "dien-bien", name = "Điện Biên", isCity = false, lat = 21.3860, lng = 103.0230),
        Province(code = "son-la", name = "Sơn La", isCity = false, lat = 21.3256, lng = 103.9188),
        Province(code = "lang-son", name = "Lạng Sơn", isCity = false, lat = 21.8537, lng = 106.7615),
        Province(code = "quang-ninh", name = "Quảng Ninh", isCity = false, lat = 20.9599, lng = 107.0425),
        Province(code = "thanh-hoa", name = "Thanh Hóa", isCity = false, lat = 19.8067, lng = 105.7852),
        Province(code = "nghe-an", name = "Nghệ An", isCity = false, lat = 18.6796, lng = 105.6813),
        Province(code = "ha-tinh", name = "Hà Tĩnh", isCity = false, lat = 18.3559, lng = 105.8877),
        Province(code = "cao-bang", name = "Cao Bằng", isCity = false, lat = 22.6663, lng = 106.2522),
        Province(code = "tuyen-quang", name = "Tuyên Quang", isCity = false, lat = 21.8236, lng = 105.2142),
        Province(code = "lao-cai", name = "Lào Cai", isCity = false, lat = 22.4856, lng = 103.9707),
        Province(code = "thai-nguyen", name = "Thái Nguyên", isCity = false, lat = 21.5942, lng = 105.8482),
        Province(code = "phu-tho", name = "Phú Thọ", isCity = false, lat = 21.3227, lng = 105.4019),
        Province(code = "bac-ninh", name = "Bắc Ninh", isCity = false, lat = 21.1861, lng = 106.0763),
        Province(code = "hung-yen", name = "Hưng Yên", isCity = false, lat = 20.6464, lng = 106.0511),
        Province(code = "ninh-binh", name = "Ninh Bình", isCity = false, lat = 20.2506, lng = 105.9745),
        Province(code = "quang-tri", name = "Quảng Trị", isCity = false, lat = 16.8163, lng = 107.1003),
        Province(code = "quang-ngai", name = "Quảng Ngãi", isCity = false, lat = 15.1214, lng = 108.8044),
        Province(code = "gia-lai", name = "Gia Lai", isCity = false, lat = 13.9833, lng = 108.0000),
        Province(code = "khanh-hoa", name = "Khánh Hòa", isCity = false, lat = 12.2388, lng = 109.1967),
        Province(code = "lam-dong", name = "Lâm Đồng", isCity = false, lat = 11.9404, lng = 108.4583),
        Province(code = "dak-lak", name = "Đắk Lắk", isCity = false, lat = 12.6667, lng = 108.0500),
        Province(code = "dong-nai", name = "Đồng Nai", isCity = false, lat = 10.9574, lng = 106.8427),
        Province(code = "tay-ninh", name = "Tây Ninh", isCity = false, lat = 11.3352, lng = 106.1099),
        Province(code = "vinh-long", name = "Vĩnh Long", isCity = false, lat = 10.2537, lng = 105.9722),
        Province(code = "dong-thap", name = "Đồng Tháp", isCity = false, lat = 10.4598, lng = 105.6326),
        Province(code = "ca-mau", name = "Cà Mau", isCity = false, lat = 9.1768, lng = 105.1524),
        Province(code = "an-giang", name = "An Giang", isCity = false, lat = 10.3864, lng = 105.4352)
    )

    private val byCode: Map<String, Province> = allProvinces.associateBy { it.code }

    fun findByCode(code: String): Province? = byCode[code]
}
