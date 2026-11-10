package com.hyundaiapp.crong.domain

import com.hyundaiapp.core.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SegmentKeyTest {
    @Test
    fun `8방위는 북을 0으로 시계 방향`() {
        assertEquals(0, SegmentKey.heading8(350.0))
        assertEquals(2, SegmentKey.heading8(90.0))
        assertEquals(4, SegmentKey.heading8(180.0))
        assertEquals(0, SegmentKey.heading8(-10.0))
    }

    @Test
    fun `geohash 표준 예시와 같다`() {
        // 위키백과 Geohash 예시
        assertEquals("u4pruydqqvj", SegmentKey.geohash(GeoPoint(57.64911, 10.40744), 11))
        // 에뮬레이터 기본 위치(판교 부근)
        assertEquals("wydku4g", SegmentKey.geohash(GeoPoint(37.4127, 127.0947), 7))
    }

    @Test
    fun `구간 ID는 7자리와 8방위`() {
        assertEquals(SegmentKey("wydku4g", 2), SegmentKey.of(GeoPoint(37.4127, 127.0947), 90.0))
    }

    @Test
    fun `칸 범위 안에 원래 점이 들어 있다`() {
        val (s, n, w, e) = SegmentKey.bounds("wydku4g").toList()
        assertTrue(37.4127 in s..n && 127.0947 in w..e)
    }

    @Test
    fun `둘레 8칸은 알려진 값과 같다`() {
        // 북서·북·북동·서·동·남서·남·남동
        assertEquals(
            listOf("ezefx", "ezs48", "ezs49", "ezefr", "ezs43", "ezefp", "ezs40", "ezs41"),
            SegmentKey("ezs42", 0).neighbors().map { it.geohash },
        )
    }

    @Test
    fun `둘레 칸은 방향이 같고 서로 이웃이다`() {
        val center = SegmentKey("wydku4g", 3)
        val around = center.neighbors()
        assertEquals(8, around.toSet().size)
        assertTrue(around.all { it.heading8 == 3 && it != center })
        assertTrue(around.all { center in it.neighbors() })
    }
}
