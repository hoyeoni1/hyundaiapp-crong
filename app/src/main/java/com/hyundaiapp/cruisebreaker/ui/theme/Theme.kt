package com.hyundaiapp.cruisebreaker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

// 임시 테마. 공통 테마는 길민종이 교체한다. 차량 화면은 어두운 배경 + 큰 글씨가 기본
private val Colors = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    secondary = Color(0xFFFFD54F),
    background = Color(0xFF101418),
    surface = Color(0xFF1A1F24),
)

private val CarTypography = Typography(
    headlineMedium = TextStyle(fontSize = 34.sp),
    titleLarge = TextStyle(fontSize = 28.sp),
    bodyLarge = TextStyle(fontSize = 22.sp),
    bodyMedium = TextStyle(fontSize = 20.sp),
    labelLarge = TextStyle(fontSize = 20.sp),
)

@Composable
fun CruiseBreakerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = CarTypography, content = content)
}
