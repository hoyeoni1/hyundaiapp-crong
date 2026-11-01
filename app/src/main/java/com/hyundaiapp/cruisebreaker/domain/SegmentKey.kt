package com.hyundaiapp.cruisebreaker.domain

import com.hyundaiapp.core.GeoPoint

/**
 * 구간 ID = geohash 7자리(약 150m 격자) + 진행 방향 8방위(0 = 북, 1 = 북동, …). 담당: 이민하.
 * 서버 `cruise_events`·`cruise_passes`·`segment_risk`의 (segment_id, heading8)와 같다.
 * 규칙: docs/disengage-rules.md — 서버는 같은 방향의 [neighbors] 8칸까지 합쳐서 위험 구간을 판단한다.
 */
data class SegmentKey(val geohash: String, val heading8: Int) {
    /** 같은 방향의 둘레 8칸(북서부터 행 순서: 북서·북·북동·서·동·남서·남·남동) */
    fun neighbors(): List<SegmentKey> = geohashNeighbors(geohash).map { SegmentKey(it, heading8) }

    companion object {
        const val GEOHASH_PRECISION = 7
        private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"

        fun of(point: GeoPoint, headingDeg: Double): SegmentKey =
            SegmentKey(geohash(point, GEOHASH_PRECISION), heading8(headingDeg))

        /** 0~360도를 8방위로. 337.5~22.5 → 0 */
        fun heading8(headingDeg: Double): Int = (((headingDeg % 360 + 360) % 360 + 22.5) / 45).toInt() % 8

        /** 표준 geohash. 경도·위도 비트를 번갈아(경도 먼저) 5비트씩 base32 한 글자로 */
        fun geohash(point: GeoPoint, precision: Int): String {
            val lat = doubleArrayOf(-90.0, 90.0)
            val lon = doubleArrayOf(-180.0, 180.0)
            val sb = StringBuilder(precision)
            var isLon = true
            var bits = 0
            var ch = 0
            while (sb.length < precision) {
                val range = if (isLon) lon else lat
                val value = if (isLon) point.longitude else point.latitude
                val mid = (range[0] + range[1]) / 2
                ch = ch shl 1
                if (value >= mid) {
                    ch = ch or 1
                    range[0] = mid
                } else {
                    range[1] = mid
                }
                isLon = !isLon
                if (++bits == 5) {
                    sb.append(BASE32[ch])
                    bits = 0
                    ch = 0
                }
            }
            return sb.toString()
        }

        /** geohash 칸의 범위 [남, 북, 서, 동] */
        fun bounds(hash: String): DoubleArray {
            val lat = doubleArrayOf(-90.0, 90.0)
            val lon = doubleArrayOf(-180.0, 180.0)
            var isLon = true
            for (c in hash) {
                val d = BASE32.indexOf(c)
                require(d >= 0) { "geohash가 아닌 글자: $c" }
                for (i in 4 downTo 0) {
                    val range = if (isLon) lon else lat
                    val mid = (range[0] + range[1]) / 2
                    if ((d shr i) and 1 == 1) range[0] = mid else range[1] = mid
                    isLon = !isLon
                }
            }
            return doubleArrayOf(lat[0], lat[1], lon[0], lon[1])
        }

        /** 칸 중심에서 한 칸씩 옮긴 점을 다시 인코딩한다(극·날짜변경선은 한국에서 안 만나므로 무시) */
        private fun geohashNeighbors(hash: String): List<String> {
            val (s, n, w, e) = bounds(hash).toList()
            val cLat = (s + n) / 2
            val cLon = (w + e) / 2
            val dLat = n - s
            val dLon = e - w
            return buildList {
                for (dy in intArrayOf(1, 0, -1)) {
                    for (dx in intArrayOf(-1, 0, 1)) {
                        if (dx == 0 && dy == 0) continue
                        add(geohash(GeoPoint(cLat + dy * dLat, cLon + dx * dLon), hash.length))
                    }
                }
            }
        }
    }
}
