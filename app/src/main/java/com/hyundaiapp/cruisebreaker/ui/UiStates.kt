package com.hyundaiapp.cruisebreaker.ui

/*
 * 화면 상태 계약(UiState). PRD 5.5 화면 S1~S5.
 * - ViewModel(이민하)이 이 상태를 내보내고, 화면(길민종)은 이 상태만 보고 그린다.
 * - 바꿀 때는 두 사람이 같은 PR에서 합의한다. 미리보기 데이터는 PreviewData.kt.
 * - 주행 중 화면(S1)은 애니메이션·자동 스크롤·여러 줄 텍스트·터치 요소 금지(앱 개발 규정 2.3).
 */

/** S1 주행 화면(주행 중, DO) */
data class DrivingUiState(
    /** 차량 신호를 받고 있어 기록 중인지 */
    val recording: Boolean = false,
    val cruiseEngaged: Boolean = false,
    val speedKph: Float? = null,
    /** 안내 배너 한 줄. 없으면 null */
    val banner: String? = null,
)

/** S2 위험 구간 목록(정차 중) */
data class RiskRowUi(
    val id: String,
    val roadName: String,
    val distanceM: Double?,
    val rate: Double,
    val passVehicles: Int,
)

data class RiskListUiState(
    val loading: Boolean = true,
    val items: List<RiskRowUi> = emptyList(),
)

/** S3 주행 후 요약(정차 중) */
data class TripSummaryUiState(
    val riskSegmentsPassed: Int = 0,
    val announcements: Int = 0,
    val myContributions: Int = 0,
)

/** S4 설정 */
data class SettingsUiState(
    val voiceEnabled: Boolean = true,
    /** 시동 때 자동 기록. S5 동의 후 기본 켜짐(PRD 5.4). 토글 화면은 길민종 */
    val autoRecordEnabled: Boolean = true,
)

/** S5 첫 실행 동의(동의 버튼 외 동작은 동의 아님) + 차속 런타임 권한 */
data class OnboardingUiState(
    val locationAgreed: Boolean = false,
    val privacyAgreed: Boolean = false,
    val speedPermissionGranted: Boolean = false,
)
