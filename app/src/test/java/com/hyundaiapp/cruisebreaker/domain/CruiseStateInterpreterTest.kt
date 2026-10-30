package com.hyundaiapp.cruisebreaker.domain

import com.hyundaiapp.core.CruiseMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CruiseStateInterpreterTest {
    @Test
    fun `작동에서 대기로 바뀌면 해제`() {
        assertTrue(CruiseStateInterpreter.isDisengage(CruiseMode.ACTIVATED, CruiseMode.ENABLED))
        assertTrue(CruiseStateInterpreter.isDisengage(CruiseMode.ACTIVATED, CruiseMode.USER_OVERRIDE))
        assertTrue(CruiseStateInterpreter.isDisengage(CruiseMode.ACTIVATED, null))
    }

    @Test
    fun `대기 상태끼리 바뀌거나 켜지는 것은 해제가 아니다`() {
        assertFalse(CruiseStateInterpreter.isDisengage(CruiseMode.ENABLED, CruiseMode.ACTIVATED))
        assertFalse(CruiseStateInterpreter.isDisengage(null, CruiseMode.ENABLED))
    }

    @Test
    fun `8방위는 북을 0으로 시계 방향`() {
        assertEquals(0, SegmentKey.heading8(350.0))
        assertEquals(2, SegmentKey.heading8(90.0))
        assertEquals(4, SegmentKey.heading8(180.0))
        assertEquals(0, SegmentKey.heading8(-10.0))
    }
}
