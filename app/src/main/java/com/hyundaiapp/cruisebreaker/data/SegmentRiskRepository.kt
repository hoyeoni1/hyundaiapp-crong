package com.hyundaiapp.cruisebreaker.data

import com.hyundaiapp.cruisebreaker.domain.SegmentKey
import com.hyundaiapp.core.GeoMath
import com.hyundaiapp.core.GeoPoint

/** 서버 `segment_risk` 뷰 한 행 + 안내·지도용 구간 중심 좌표 */
data class SegmentRisk(
    val key: SegmentKey,
    val center: GeoPoint,
    val roadName: String,
    val disengages: Int,
    val passVehicles: Int,
    /** 해제 수 ÷ 통과 차량 수 */
    val rate: Double,
    /** 해제 중 브레이크 비율 */
    val brakeRatio: Double,
    val isRisk: Boolean,
)

/** 위험 구간 조회. 실제 구현(서버)은 황준용, 그 전에는 [FakeSegmentRiskRepository] */
interface SegmentRiskRepository {
    suspend fun nearby(center: GeoPoint, radiusM: Double): List<SegmentRisk>
}

/** 시드 데이터(경부고속도로 판교~양재). PRD 5.6: 시연에서 시드 데이터임을 밝힌다 */
class FakeSegmentRiskRepository(
    private val data: List<SegmentRisk> = SEED,
) : SegmentRiskRepository {
    override suspend fun nearby(center: GeoPoint, radiusM: Double) =
        data.filter { GeoMath.distanceM(center, it.center) <= radiusM }.sortedByDescending { it.rate }

    companion object {
        // geohash는 SegmentKey 구현 전 임시 문자열. 북행(서울 방향) = heading8 0
        val SEED = listOf(
            seed("seed-a", 37.4176, 127.0991, "경부고속도로 판교JC 합류", 52, 140),
            seed("seed-b", 37.4362, 127.0640, "경부고속도로 서초IC 부근", 31, 120),
            seed("seed-c", 37.4630, 127.0420, "경부고속도로 양재IC 진출", 18, 110),
        )

        private fun seed(id: String, lat: Double, lon: Double, road: String, dis: Int, pass: Int) = SegmentRisk(
            key = SegmentKey(id, 0), center = GeoPoint(lat, lon), roadName = road,
            disengages = dis, passVehicles = pass, rate = dis.toDouble() / pass, brakeRatio = 0.7,
            isRisk = dis.toDouble() / pass >= 0.3 && pass >= 20,
        )
    }
}
