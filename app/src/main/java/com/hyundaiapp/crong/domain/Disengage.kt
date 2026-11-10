package com.hyundaiapp.crong.domain

import com.hyundaiapp.core.GeoPoint

/**
 * 해제 이벤트 1건(F1). 원인(브레이크·정체 등)은 나누지 않는다 — 위험 여부는 서버가 여러 차의 비율로 정한다.
 * 원래 좌표는 서버로 보내지 않고 [segment]만 보낸다.
 */
data class DisengageEvent(
    val atMs: Long,
    val segment: SegmentKey,
    /** 해제 순간 차속. 차속 권한이 없으면 null */
    val speedKph: Float?,
    /** 앱 안에서만 쓴다(지도 표시 등). 업로드 금지 */
    val point: GeoPoint,
)

/** 크루즈 작동 중 구간 통과 1회(해제율의 분모) */
data class CruisePass(val atMs: Long, val segment: SegmentKey)

/** 판정 기준값. docs/disengage-rules.md 표와 같은 값 */
data class DisengageRules(
    val minEngagedMs: Long = 3_000L,
    /** 해제 순간 차속이 이보다 낮으면 세지 않는다(오래 정차해서 자동 해제된 것 등). 차속을 모르면 건너뜀 */
    val minSpeedKph: Float = 60f,
    /** 위험 구간: 해제율 ≥ riskRate 그리고 통과 차량 ≥ minPasses (서버가 9칸 합산으로 판단) */
    val riskRate: Double = 0.3,
    val minPasses: Int = 20,
    /** 안내: 진행 방향 ±announceAngleDeg 안, 앞 (announceDistanceM - announceMarginM) ~ announceDistanceM. 500m 고정(11-01) */
    val announceDistanceM: Double = 500.0,
    val announceMarginM: Double = 200.0,
    val announceAngleDeg: Double = 30.0,
    val announceCooldownMs: Long = 3 * 60_000L,
    /** 배너를 띄워 두는 시간 */
    val bannerMs: Long = 10_000L,
)
