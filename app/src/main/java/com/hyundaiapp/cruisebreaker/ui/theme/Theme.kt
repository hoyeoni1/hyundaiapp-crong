package com.hyundaiapp.cruisebreaker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * 두 앱 공통 테마(차슐랭과 같은 값). 한쪽을 바꾸면 다른 앱 Theme.kt도 같이 바꾼다.
 * 와이어프레임: https://claude.ai/artifact/9vidSJtHrxzCqtEcjJjVTf ("공통 테마" 보드)
 * - 어두운 배경 + 큰 글씨, 강조색은 파랑(정보·인증)과 주황(위험·실패) 둘만
 * - 상태는 색만으로 구분하지 않고 늘 글자와 함께 보여 준다
 */
object CarColors {
    val Background = Color(0xFF0F1216)
    val Surface = Color(0xFF1A1F25)
    val SurfaceVariant = Color(0xFF252B33)
    /** 막대 그래프 바탕 */
    val Track = Color(0xFF343C46)
    /** 보조 버튼 테두리 */
    val Outline = Color(0xFF4A5562)
    val OnSurface = Color(0xFFEEF2F5)
    val OnSurfaceVariant = Color(0xFFA3AEB9)
    /** 비활성 글자. 배경 대비 4.5:1 이상 */
    val Disabled = Color(0xFF8A95A1)
    /** 정보·인증·선택 */
    val Primary = Color(0xFF4FC3F7)
    /** 위험·실패 */
    val Warning = Color(0xFFFFB547)
    /** 파랑·주황 위에 올리는 글자 */
    val OnAccent = Background
    /** 팝업 뒤 배경 */
    val Scrim = Color(0xFF080A0C)
}

object CarDimens {
    /** 주 버튼 높이. 차량 화면 가이드 2.3 "큰 터치 영역" */
    val TouchTarget = 76.dp
    /** 보조 버튼·작은 버튼 높이 */
    val TouchTargetSmall = 64.dp
    val ScreenPadding = 48.dp
    val CardRadius = 20.dp
    val ButtonRadius = 16.dp
}

private val Colors = darkColorScheme(
    primary = CarColors.Primary,
    onPrimary = CarColors.OnAccent,
    secondary = CarColors.Warning,
    onSecondary = CarColors.OnAccent,
    background = CarColors.Background,
    onBackground = CarColors.OnSurface,
    surface = CarColors.Background,
    onSurface = CarColors.OnSurface,
    surfaceVariant = CarColors.SurfaceVariant,
    onSurfaceVariant = CarColors.OnSurfaceVariant,
    surfaceContainer = CarColors.Surface,
    surfaceContainerHigh = CarColors.SurfaceVariant,
    surfaceContainerHighest = CarColors.SurfaceVariant,
    outline = CarColors.Outline,
    outlineVariant = CarColors.Track,
    scrim = CarColors.Scrim,
)

/** 글자 크기(sp). 본문 22 이상, 주행 중 배너 56 이상 */
private val CarTypography = Typography(
    displayLarge = TextStyle(fontSize = 96.sp, fontWeight = FontWeight.Bold, lineHeight = 100.sp),
    displayMedium = TextStyle(fontSize = 64.sp, fontWeight = FontWeight.Bold, lineHeight = 72.sp),
    displaySmall = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold, lineHeight = 52.sp),
    headlineMedium = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, lineHeight = 44.sp, localeList = LocaleList("ko-KR"), lineBreak = LineBreak.Heading),
    titleLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Medium, lineHeight = 36.sp, localeList = LocaleList("ko-KR"), lineBreak = LineBreak.Heading),
    titleMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Medium, lineHeight = 34.sp, localeList = LocaleList("ko-KR"), lineBreak = LineBreak.Heading),
    bodyLarge = TextStyle(fontSize = 22.sp, lineHeight = 32.sp, localeList = LocaleList("ko-KR"), lineBreak = LineBreak.Paragraph),
    bodyMedium = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, localeList = LocaleList("ko-KR"), lineBreak = LineBreak.Paragraph),
    bodySmall = TextStyle(fontSize = 18.sp, lineHeight = 26.sp, localeList = LocaleList("ko-KR"), lineBreak = LineBreak.Paragraph),
    labelLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp),
    labelMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium, lineHeight = 26.sp),
)

@Composable
fun CruiseBreakerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = CarTypography, content = content)
}
