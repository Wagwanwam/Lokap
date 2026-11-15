plugins {
    alias(libs.plugins.android.application)
    // Đọc app/google-services.json (Firebase Console → Project settings → Your apps), không commit file này
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Khoá đọc từ file `.env` ở gốc dự án (không commit, mẫu ở .env.example). Biến môi trường cùng tên ghi đè .env.
// Firebase là nguồn dữ liệu duy nhất; thiếu cấu hình phải được bổ sung trước khi build.
val dotEnv: Map<String, String> = providers
    .fileContents(rootProject.layout.projectDirectory.file(".env"))
    .asText
    .orNull
    .orEmpty()
    .lineSequence()
    .map { it.trim() }
    .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
    .associate { line ->
        line.substringBefore("=").trim() to line.substringAfter("=").trim().removeSurrounding("\"")
    }

fun envValue(key: String): String =
    providers.environmentVariable(key).orNull?.trim()?.takeIf { it.isNotEmpty() } ?: dotEnv[key].orEmpty()

// Firebase: có thể chép thẳng app/google-services.json (Firebase Console → Project settings → Your apps)
// thay vì điền 3 dòng FIREBASE_* trong .env. Plugin google-services tạo resource cho Firebase SDK.
val googleServices: Map<String, String> = run {
    val text = providers.fileContents(layout.projectDirectory.file("google-services.json")).asText.orNull
        ?: return@run emptyMap()
    val root = groovy.json.JsonSlurper().parseText(text) as Map<*, *>
    val project = root["project_info"] as? Map<*, *>
    val client = (root["client"] as? List<*>).orEmpty().map { it as Map<*, *> }.firstOrNull { c ->
        ((c["client_info"] as? Map<*, *>)?.get("android_client_info") as? Map<*, *>)?.get("package_name") == "vn.nhom2.lokap"
    }
    // Web client ID (client_type 3) chỉ có sau khi bật đăng nhập Google trong Firebase Console rồi tải lại file.
    // Credential Manager cần ID này (không phải Android client ID) để Google cấp ID token cho Firebase.
    val otherPlatformClients = ((client?.get("services") as? Map<*, *>)
        ?.get("appinvite_service") as? Map<*, *>)?.get("other_platform_oauth_client") as? List<*>
    val webClientId = ((client?.get("oauth_client") as? List<*>).orEmpty() + otherPlatformClients.orEmpty())
        .map { it as Map<*, *> }
        .firstOrNull { it["client_type"]?.toString() == "3" }
        ?.get("client_id")?.toString().orEmpty()
    mapOf(
        "FIREBASE_PROJECT_ID" to project?.get("project_id")?.toString().orEmpty(),
        "FIREBASE_APP_ID" to (client?.get("client_info") as? Map<*, *>)?.get("mobilesdk_app_id")?.toString().orEmpty(),
        "FIREBASE_API_KEY" to ((client?.get("api_key") as? List<*>)?.firstOrNull() as? Map<*, *>)
            ?.get("current_key")?.toString().orEmpty(),
        "GOOGLE_WEB_CLIENT_ID" to webClientId
    )
}

fun firebaseValue(key: String): String = envValue(key).ifEmpty { googleServices[key].orEmpty() }

val firebaseApiKey = firebaseValue("FIREBASE_API_KEY")
val firebaseAppId = firebaseValue("FIREBASE_APP_ID")
val firebaseProjectId = firebaseValue("FIREBASE_PROJECT_ID")
check(firebaseApiKey.isNotBlank() && firebaseAppId.isNotBlank() && firebaseProjectId.isNotBlank()) {
    "Thiếu cấu hình Firebase. Điền FIREBASE_API_KEY, FIREBASE_APP_ID, FIREBASE_PROJECT_ID trong .env hoặc thêm app/google-services.json."
}

android {
    namespace = "vn.nhom2.lokap"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "vn.nhom2.lokap"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "FIREBASE_API_KEY", "\"$firebaseApiKey\"")
        buildConfigField("String", "FIREBASE_APP_ID", "\"$firebaseAppId\"")
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"$firebaseProjectId\"")
        // Trống → nút "Tiếp tục với Google" báo chưa cấu hình (đăng nhập email vẫn chạy)
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${firebaseValue("GOOGLE_WEB_CLIENT_ID")}\"")
        // Máy chạy Firebase Emulator (vd 10.0.2.2 cho emulator Android, IP LAN cho máy thật); trống = Firebase thật
        buildConfigField("String", "FIREBASE_EMULATOR_HOST", "\"${envValue("FIREBASE_EMULATOR_HOST")}\"")
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${envValue("CLOUDINARY_CLOUD_NAME")}\"")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"${envValue("CLOUDINARY_UPLOAD_PRESET")}\"")
        // VietMap tách 2 loại key: consumer API (tìm địa điểm, Place, đổi toạ độ → tỉnh/phường) và
        // consumer Tilemap (bản đồ nền). Dùng chung 1 key thì bản đồ nền ăn hết hạn mức tìm kiếm (HTTP 423).
        // VIETMAP_TILE_KEY trống → dùng tạm VIETMAP_API_KEY cho bản đồ; cả hai trống → bản đồ vẽ tay.
        buildConfigField("String", "VIETMAP_API_KEY", "\"${envValue("VIETMAP_API_KEY")}\"")
        buildConfigField(
            "String", "VIETMAP_TILE_KEY",
            "\"${envValue("VIETMAP_TILE_KEY").ifEmpty { envValue("VIETMAP_API_KEY") }}\""
        )
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    // Room: không xuất schema JSON (chưa cần migration cho cache cục bộ).
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.zxing.core)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Cache máy (feed, bài chờ gửi) + upload nền
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)

    // Firebase: đăng nhập + Firestore
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)
    // Đăng nhập Google: Credential Manager hiện hộp chọn tài khoản → ID token → Firebase Auth
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Lưu ảnh
    implementation(libs.cloudinary.android.core)

    // Bản đồ VietMap + vị trí (Fused Location Provider)
    implementation(libs.vietmap.maps.sdk)
    implementation(libs.vietmap.geojson)
    implementation(libs.vietmap.turf)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.play.services.location)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
