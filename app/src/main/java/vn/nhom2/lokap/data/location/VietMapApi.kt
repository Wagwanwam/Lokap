package vn.nhom2.lokap.data.location

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import vn.nhom2.lokap.BuildConfig

/*
 * REST API của VietMap (maps.vietmap.vn/docs/map-api). Dùng chung key với bản đồ.
 * Không có server riêng (gói Firebase miễn phí) nên app gọi thẳng; nhớ giới hạn key theo package
 * vn.nhom2.lokap + SHA-1 trong VietMap Console để người khác không lấy key dùng được.
 */

@Serializable
data class VmBoundary(
    /** 0 = tỉnh/thành, 2 = phường/xã (định dạng mới không còn 1 = quận/huyện). */
    val type: Int,
    val id: Long = 0,
    val name: String = "",
    @SerialName("full_name") val fullName: String = ""
)

@Serializable
data class VmResult(
    @SerialName("ref_id") val refId: String = "",
    val name: String = "",
    val address: String = "",
    val display: String = "",
    val boundaries: List<VmBoundary> = emptyList()
)

@Serializable
data class VmPlace(
    val display: String = "",
    val name: String = "",
    val address: String = "",
    val lat: Double = Double.NaN,
    val lng: Double = Double.NaN
)

@Singleton
class VietMapApi @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    val isConfigured: Boolean get() = BuildConfig.VIETMAP_API_KEY.isNotBlank()

    /** Reverse v4, định dạng mới (phường, tỉnh). */
    suspend fun reverse(lat: Double, lng: Double): VmResult? =
        call<List<VmResult>>("reverse/v4", listOf("lat" to lat.toString(), "lng" to lng.toString(), "display_type" to "1"))
            .firstOrNull()

    /** Autocomplete v4: tối đa 10 kết quả, KHÔNG kèm toạ độ. */
    suspend fun autocomplete(text: String, focusLat: Double?, focusLng: Double?): List<VmResult> {
        val params = mutableListOf("text" to text, "display_type" to "1")
        if (focusLat != null && focusLng != null) params += "focus" to "$focusLat,$focusLng"
        return call("autocomplete/v4", params)
    }

    /** Place v4: toạ độ của 1 kết quả autocomplete (refid giữ nguyên tiền tố "auto:"). */
    suspend fun place(refId: String): VmPlace = call("place/v4", listOf("refid" to refId))

    private suspend inline fun <reified T> call(path: String, params: List<Pair<String, String>>): T =
        withContext(Dispatchers.IO) {
            val url = "$BASE_URL/$path".toHttpUrl().newBuilder()
                .addQueryParameter("apikey", BuildConfig.VIETMAP_API_KEY)
                .apply { params.forEach { (k, v) -> addQueryParameter(k, v) } }
                .build()
            // Lỗi mạng → IOException (app báo mất mạng); sai key / hết lượt → VietMapException
            client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
                if (!response.isSuccessful) throw VietMapException(response.code)
                json.decodeFromString<T>(response.body?.string().orEmpty())
            }
        }

    private companion object {
        const val BASE_URL = "https://maps.vietmap.vn/api"
    }
}

/** 401: sai key · 423: hết lượt gọi hoặc key chưa bật API này. */
class VietMapException(val httpCode: Int) : Exception("VietMap trả lỗi HTTP $httpCode")
