package com.hyundaiapp.cruisebreaker.data

import com.hyundaiapp.cruisebreaker.domain.SegmentKey
import com.hyundaiapp.core.GeoPoint

/** 서버 `segment_risk` 뷰 한 행 + 안내·지도용 구간 중심 좌표 */
data class SegmentRisk(
    val key: SegmentKey,
    val center: GeoPoint,
    val roadName: String,
    /** 같은 방향 9칸(이 칸 + 둘레 8칸) 합산 해제 수 */
    val disengages: Int,
    /** 같은 방향 9칸 합산 통과 차량 수 */
    val passVehicles: Int,
    /** 해제 수 ÷ 통과 차량 수 */
    val rate: Double,
    /** 규칙 v2에서 없앰(원인 구분 안 함). S2 화면 정리(길민종) 뒤 삭제 — 지금은 항상 0 */
    val brakeRatio: Double,
    val isRisk: Boolean,
)

/** 위험 구간 조회. 실제 구현(서버)은 황준용, 그 전에는 [FakeCruiseServer] */
interface SegmentRiskRepository {
    suspend fun nearby(center: GeoPoint, radiusM: Double): List<SegmentRisk>
}
