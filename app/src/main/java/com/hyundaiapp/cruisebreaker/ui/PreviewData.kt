package com.hyundaiapp.cruisebreaker.ui

/** @Preview 와 화면 개발용 샘플. 실제 데이터 없이 화면을 먼저 만든다 */
object PreviewData {
    val driving = DrivingUiState(recording = true, cruiseEngaged = true, speedKph = 98f, banner = null)
    val drivingWithBanner = driving.copy(banner = "500m 앞 크루즈 해제 잦은 구간")
    val riskList = RiskListUiState(
        loading = false,
        items = listOf(
            RiskRowUi("seed-a", "경부고속도로 판교JC 합류", 650.0, 0.37, 140),
            RiskRowUi("seed-b", "경부고속도로 서초IC 부근", 3_900.0, 0.26, 120),
        ),
    )
    val tripSummary = TripSummaryUiState(riskSegmentsPassed = 3, announcements = 3, myContributions = 2)
}
