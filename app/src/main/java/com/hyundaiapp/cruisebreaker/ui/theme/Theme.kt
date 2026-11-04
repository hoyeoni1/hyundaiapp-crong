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
 * 두 앱 공통 테마(차슐랭과 같은 값, Primary만 다름). 한쪽을 바꾸면 다른 앱 Theme.kt도 같이 바꾼다.
 * 와이어프레임: https://claude.ai/artifact/9vidSJtHrxzCqtEcjJjVTf ("공통 테마" 보드)
 * - 어두운 배경 + 큰 글씨, 강조색은 앱 대표색(Primary)과 주황(위험·실패) 둘만
 * - Primary만 앱마다 다르다: 크루즈브레이커 앰버, 차슐랭 레드
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
    /** 앱 대표색(앰버). 주인공 숫자·선택·주 버튼 */
    val Primary = Color(0xFFFFB020)
    /** 위험·실패 */
    val Warning = Color(0xFFFFB547)
    /** 대표색·주황 위에 올리는 글자 */
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

/**
 * 글자 단계(sp). 두 단계가 한눈에 구분되게 크기 차이를 4sp 이상, 제목은 굵게 둔다.
 * - displayLarge 112: 화면마다 하나뿐인 주인공 숫자(충전 SoC, 차슐랭 점수, 주행 요약 횟수)
 * - displayMedium 64: 주행 중 배너, 큰 숫자 칸 / displaySmall 44: 카드 안 핵심 숫자
 * - headlineMedium 36: 화면 제목 / titleLarge 30: 카드·팝업 제목 / titleMedium 26: 목록 항목 이름
 * - bodyLarge 22: 본문 / bodyMedium 20: 회색 보조 설명·숫자 라벨 / bodySmall 18: 출처 같은 작은 주석만
 * - labelLarge 24: 버튼 / labelMedium 20: 상태 표시·구역 이름
 * 강조 규칙: 숫자는 굵게 + 흰색, 단위는 숫자보다 작은 회색. 대표색·주황은 의미(주인공·위험)가 있을 때만.
 */
private val Korean = LocaleList("ko-KR")
private val CarTypography = Typography(
    displayLarge = TextStyle(fontSize = 112.sp, fontWeight = FontWeight.Bold, lineHeight = 120.sp),
    displayMedium = TextStyle(fontSize = 64.sp, fontWeight = FontWeight.Bold, lineHeight = 72.sp),
    displaySmall = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold, lineHeight = 52.sp),
    headlineMedium = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold, lineHeight = 46.sp, localeList = Korean, lineBreak = LineBreak.Heading),
    titleLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp, localeList = Korean, lineBreak = LineBreak.Heading),
    titleMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, lineHeight = 34.sp, localeList = Korean, lineBreak = LineBreak.Heading),
    bodyLarge = TextStyle(fontSize = 22.sp, lineHeight = 32.sp, localeList = Korean, lineBreak = LineBreak.Paragraph),
    bodyMedium = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, localeList = Korean, lineBreak = LineBreak.Paragraph),
    bodySmall = TextStyle(fontSize = 18.sp, lineHeight = 26.sp, localeList = Korean, lineBreak = LineBreak.Paragraph),
    labelLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp, localeList = Korean),
    labelMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp, localeList = Korean),
)

@Composable
fun CruiseBreakerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = CarTypography, content = content)
}
