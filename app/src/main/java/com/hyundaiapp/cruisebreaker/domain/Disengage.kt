package com.hyundaiapp.cruisebreaker.domain

import com.hyundaiapp.core.GeoPoint

enum class DisengageType {
    /** 해제 직전 브레이크를 밟음 */
    BRAKE,
    OTHER,
}

/** 정체 필터를 통과한 해제 이벤트 1건(F1·F2). 원래 좌표는 서버로 보내지 않고 [segment]만 보낸다 */
data class DisengageEvent(
    val atMs: Long,
    val segment: SegmentKey,
    val type: DisengageType,
    val speedKph: Float,
    /** 앱 안에서만 쓴다(지도 표시 등). 업로드 금지 */
    val point: GeoPoint,
)

/** 크루즈 작동 중 구간 통과 1회(해제율의 분모) */
data class CruisePass(val atMs: Long, val segment: SegmentKey)

/** 판정 기준값. PRD 5.3 표와 같은 값 */
data class DisengageRules(
    val minEngagedMs: Long = 3_000L,
    val minSpeedKph: Float = 60f,
    val brakeWindowMs: Long = 2_000L,
    /** 해제 전 이 시간 동안 평균 차속이 [congestionAvgKph] 미만이면 정체로 본다 */
    val congestionWindowMs: Long = 30_000L,
    val congestionAvgKph: Float = 40f,
    /** 위험 구간: 해제율 ≥ riskRate 그리고 통과 차량 ≥ minPasses */
    val riskRate: Double = 0.3,
    val minPasses: Int = 20,
    /** 안내: 진행 방향 ±announceAngleDeg 안, 앞 announceMinM~announceMaxM */
    val announceMinM: Double = 800.0,
    val announceMaxM: Double = 1_200.0,
    val announceAngleDeg: Double = 30.0,
    val announceCooldownMs: Long = 3 * 60_000L,
)
