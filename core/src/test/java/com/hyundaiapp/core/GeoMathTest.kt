package com.hyundaiapp.core

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoMathTest {
    private val pangyo = GeoPoint(37.4127, 127.0947)

    @Test
    fun `위도 0_001도는 약 111m`() {
        val north = GeoPoint(pangyo.latitude + 0.001, pangyo.longitude)
        assertEquals(111.2, GeoMath.distanceM(pangyo, north), 0.5)
    }

    @Test
    fun `북쪽 지점의 방위각은 0도, 동쪽은 90도`() {
        assertEquals(0.0, GeoMath.bearingDeg(pangyo, GeoPoint(pangyo.latitude + 0.01, pangyo.longitude)), 0.1)
        assertEquals(90.0, GeoMath.bearingDeg(pangyo, GeoPoint(pangyo.latitude, pangyo.longitude + 0.01)), 0.1)
    }

    @Test
    fun `방위각 차이는 360도를 넘어가도 짧은 쪽`() {
        assertEquals(20.0, GeoMath.angleDiffDeg(350.0, 10.0), 1e-9)
        assertEquals(180.0, GeoMath.angleDiffDeg(0.0, 180.0), 1e-9)
    }
}
