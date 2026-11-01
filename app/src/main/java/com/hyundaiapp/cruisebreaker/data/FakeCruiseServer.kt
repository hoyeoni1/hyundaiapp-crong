package com.hyundaiapp.cruisebreaker.data

import com.hyundaiapp.core.GeoMath
import com.hyundaiapp.core.GeoPoint
import com.hyundaiapp.cruisebreaker.domain.CruisePass
import com.hyundaiapp.cruisebreaker.domain.DisengageEvent
import com.hyundaiapp.cruisebreaker.domain.DisengageRules
import com.hyundaiapp.cruisebreaker.domain.SegmentKey

/** 시드 데이터 한 칸. 좌표·방향은 칸(geohash 7 + 8방위)으로 바뀐다 */
data class SeedCell(
    val point: GeoPoint,
    val headingDeg: Double,
    val roadName: String,
    val disengages: Int,
    val passes: Int,
)

/**
 * 서버(황준용)가 준비되기 전에 앱 안에서 쓰는 가짜 서버. 업로드한 해제·통과가 바로 위험 구간 계산에 반영된다(시연: 해제 → +1).
 *
 * 위험 판단은 서버 규칙(docs/disengage-rules.md "서버 판단")처럼 같은 방향 9칸을 합산한다.
 * 다른 점: 차가 하나뿐이라 차량-일 중복 제거와 28일 기간은 하지 않는다. 행은 기록이 있는 칸만 돌려준다.
 */
class FakeCruiseServer(
    seed: List<SeedCell> = SEED,
    private val rules: DisengageRules = DisengageRules(),
) : CruiseEventRepository, SegmentRiskRepository {
    private class Cell(val roadName: String, var disengages: Int = 0, var passes: Int = 0)

    private val cells = HashMap<SegmentKey, Cell>()

    init {
        for (s in seed) {
            val cell = cellAt(SegmentKey.of(s.point, s.headingDeg), s.roadName)
            cell.disengages += s.disengages
            cell.passes += s.passes
        }
    }

    override suspend fun upload(event: DisengageEvent) {
        synchronized(cells) { cellAt(event.segment).disengages += 1 }
    }

    override suspend fun upload(pass: CruisePass) {
        synchronized(cells) { cellAt(pass.segment).passes += 1 }
    }

    override suspend fun nearby(center: GeoPoint, radiusM: Double): List<SegmentRisk> = synchronized(cells) {
        cells.keys.map { riskOf(it) }
            .filter { GeoMath.distanceM(center, it.center) <= radiusM }
            .sortedByDescending { it.rate }
    }

    private fun cellAt(key: SegmentKey, roadName: String = ""): Cell = cells.getOrPut(key) { Cell(roadName) }

    private fun riskOf(key: SegmentKey): SegmentRisk {
        val around = (listOf(key) + key.neighbors()).mapNotNull { cells[it] }
        val disengages = around.sumOf { it.disengages }
        val passes = around.sumOf { it.passes }
        val rate = if (passes > 0) disengages.toDouble() / passes else 0.0
        return SegmentRisk(
            key = key, center = key.center(), roadName = cells.getValue(key).roadName,
            disengages = disengages, passVehicles = passes, rate = rate, brakeRatio = 0.0,
            isRisk = rate >= rules.riskRate && passes >= rules.minPasses,
        )
    }

    companion object {
        /** 경부고속도로 판교~양재, 북행(서울 방향). PRD 5.6: 시연에서 시드 데이터임을 밝힌다 */
        val SEED = listOf(
            SeedCell(GeoPoint(37.4176, 127.0991), 0.0, "경부고속도로 판교JC 합류", 52, 140),
            SeedCell(GeoPoint(37.4362, 127.0640), 0.0, "경부고속도로 서초IC 부근", 31, 120),
            SeedCell(GeoPoint(37.4630, 127.0420), 0.0, "경부고속도로 양재IC 진출", 18, 110),
            // 에뮬레이터 위치는 판교(37.4127, 127.0947)에 고정이라, 위치 재생 없이도 안내를 볼 수 있게 북쪽 약 400m에 둔다
            SeedCell(GeoPoint(37.4163, 127.0947), 0.0, "시연 지점(에뮬레이터 위치 앞 400m)", 45, 120),
        )
    }
}
