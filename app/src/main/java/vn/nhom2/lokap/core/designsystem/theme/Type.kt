package vn.nhom2.lokap.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import vn.nhom2.lokap.R

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val beVietnamProFont = GoogleFont("Be Vietnam Pro")

val BeVietnamProFamily = FontFamily(
    Font(googleFont = beVietnamProFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = beVietnamProFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = beVietnamProFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = beVietnamProFont, fontProvider = fontProvider, weight = FontWeight.ExtraBold)
)

val LokapDisplayStyle = TextStyle(
    fontFamily = BeVietnamProFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 32.sp,
    lineHeight = 40.sp
)

val LokapTitleStyle = TextStyle(
    fontFamily = BeVietnamProFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 28.sp
)

val LokapBodyStyle = TextStyle(
    fontFamily = BeVietnamProFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.sp
)

val LokapLabelStyle = TextStyle(
    fontFamily = BeVietnamProFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp
)

val LokapTypography = Typography(
    displayLarge = LokapDisplayStyle,
    displayMedium = LokapDisplayStyle,
    displaySmall = LokapDisplayStyle,
    headlineLarge = LokapDisplayStyle,
    headlineMedium = LokapTitleStyle,
    headlineSmall = LokapTitleStyle,
    titleLarge = LokapTitleStyle,
    titleMedium = LokapTitleStyle,
    titleSmall = LokapTitleStyle,
    bodyLarge = LokapBodyStyle,
    bodyMedium = LokapBodyStyle,
    bodySmall = LokapBodyStyle,
    labelLarge = LokapLabelStyle,
    labelMedium = LokapLabelStyle,
    labelSmall = LokapLabelStyle
)
