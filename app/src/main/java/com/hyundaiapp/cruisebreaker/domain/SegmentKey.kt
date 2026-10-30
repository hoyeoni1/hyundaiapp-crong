package com.hyundaiapp.cruisebreaker.domain

import com.hyundaiapp.core.GeoPoint

/**
 * 구간 ID = geohash 7자리(약 150m 격자) + 진행 방향 8방위(0 = 북, 1 = 북동, …). PRD 5.3. 담당: 이민하.
 * 서버 `cruise_events`·`cruise_passes`·`segment_risk`의 (segment_id, heading8)와 같다.
 */
data class SegmentKey(val geohash: String, val heading8: Int) {
    companion object {
        const val GEOHASH_PRECISION = 7

        fun of(point: GeoPoint, headingDeg: Double): SegmentKey =
            SegmentKey(geohash(point, GEOHASH_PRECISION), heading8(headingDeg))

        /** 0~360도를 8방위로. 337.5~22.5 → 0 */
        fun heading8(headingDeg: Double): Int = (((headingDeg % 360 + 360) % 360 + 22.5) / 45).toInt() % 8

        // TODO(이민하): 표준 geohash 인코딩 + 테스트(예: 판교 37.4127,127.0947 → "wydm…")
        fun geohash(point: GeoPoint, precision: Int): String = TODO("geohash 구현 필요")
    }
}
