package com.hyundaiapp.cruisebreaker.domain

import com.hyundaiapp.core.CruiseMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CruiseStateInterpreterTest {
    @Test
    fun `작동에서 대기·일시정지·없음으로 바뀌면 해제`() {
        assertTrue(CruiseStateInterpreter.isDisengage(CruiseMode.ACTIVATED, CruiseMode.ENABLED))
        assertTrue(CruiseStateInterpreter.isDisengage(CruiseMode.ACTIVATED, CruiseMode.SUSPENDED))
        assertTrue(CruiseStateInterpreter.isDisengage(CruiseMode.ACTIVATED, null))
        assertTrue(CruiseStateInterpreter.isDisengage(CruiseMode.USER_OVERRIDE, CruiseMode.SUSPENDED))
    }

    @Test
    fun `가속 페달 덮어쓰기와 해제 경고는 아직 작동 중`() {
        assertFalse(CruiseStateInterpreter.isDisengage(CruiseMode.ACTIVATED, CruiseMode.USER_OVERRIDE))
        assertFalse(CruiseStateInterpreter.isDisengage(CruiseMode.USER_OVERRIDE, CruiseMode.ACTIVATED))
        assertFalse(CruiseStateInterpreter.isDisengage(CruiseMode.ACTIVATED, CruiseMode.FORCED_DEACTIVATION_WARNING))
    }

    @Test
    fun `대기 상태끼리 바뀌거나 켜지는 것은 해제가 아니다`() {
        assertFalse(CruiseStateInterpreter.isDisengage(CruiseMode.ENABLED, CruiseMode.ACTIVATED))
        assertFalse(CruiseStateInterpreter.isDisengage(null, CruiseMode.ENABLED))
    }
}
