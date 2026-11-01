package com.hyundaiapp.cruisebreaker.domain

import com.hyundaiapp.core.CruiseMode
import com.hyundaiapp.core.FakeVehicleSignals
import com.hyundaiapp.core.Gear
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DisengageDetectorTest {
    private val fake = FakeVehicleSignals()
    private val events = mutableListOf<DisengageEvent>()
    private val passes = mutableListOf<CruisePass>()

    // 판교 부근, 북쪽으로 달림
    private val here = SegmentKey("wydku4g", 0)

    private fun TestScope.startDetector() {
        val detector = DisengageDetector(fake, DisengageRules(), clock = { testScheduler.currentTime })
        backgroundScope.launch { detector.events.collect { events += it } }
        backgroundScope.launch { detector.passes.collect { passes += it } }
        detector.start(backgroundScope)
    }

    private fun drive(speedKph: Float?) = fake.setDrive { copy(gear = Gear.DRIVE, speedKph = speedKph) }
    private fun cruise(mode: CruiseMode?) = fake.setCruise { copy(mode = mode) }

    @Test
    fun `3초 넘게 작동한 뒤 고속에서 꺼지면 해제 1건, 그 구간 통과도 1건`() = runTest(UnconfinedTestDispatcher()) {
        fake.setLocation(37.4127, 127.0947, headingDeg = 0.0)
        drive(100f)
        startDetector()

        cruise(CruiseMode.ACTIVATED)
        advanceTimeBy(5_000)
        cruise(CruiseMode.ENABLED)

        assertEquals(1, events.size)
        assertEquals(here, events[0].segment)
        assertEquals(100f, events[0].speedKph)
        assertEquals(listOf(here), passes.map { it.segment })
    }

    @Test
    fun `3초 안에 꺼지면 세지 않는다`() = runTest(UnconfinedTestDispatcher()) {
        drive(100f)
        startDetector()

        cruise(CruiseMode.ACTIVATED)
        advanceTimeBy(2_000)
        cruise(CruiseMode.ENABLED)

        assertTrue(events.isEmpty())
    }

    @Test
    fun `60km per h 미만에서 꺼지면 세지 않는다`() = runTest(UnconfinedTestDispatcher()) {
        drive(100f)
        startDetector()

        cruise(CruiseMode.ACTIVATED)
        advanceTimeBy(10_000)
        drive(0f) // 정체에서 멈춰 자동 해제
        cruise(CruiseMode.ENABLED)

        assertTrue(events.isEmpty())
    }

    @Test
    fun `차속을 모르면 속도 조건 없이 센다`() = runTest(UnconfinedTestDispatcher()) {
        drive(null)
        startDetector()

        cruise(CruiseMode.ACTIVATED)
        advanceTimeBy(5_000)
        cruise(CruiseMode.SUSPENDED)

        assertEquals(1, events.size)
        assertNull(events[0].speedKph)
    }

    @Test
    fun `가속 페달로 잠깐 덮어쓴 것은 해제가 아니고 작동 시간도 이어진다`() = runTest(UnconfinedTestDispatcher()) {
        drive(100f)
        startDetector()

        cruise(CruiseMode.ACTIVATED)
        advanceTimeBy(2_000)
        cruise(CruiseMode.USER_OVERRIDE)
        advanceTimeBy(2_000)
        cruise(CruiseMode.ACTIVATED)
        assertTrue(events.isEmpty())

        cruise(CruiseMode.ENABLED) // 처음 작동부터 4초
        assertEquals(1, events.size)
    }

    @Test
    fun `연결 직후 첫 값이 대기면 해제가 아니다`() = runTest(UnconfinedTestDispatcher()) {
        cruise(CruiseMode.ENABLED)
        drive(100f)
        startDetector()

        assertTrue(events.isEmpty())
        assertTrue(passes.isEmpty())
    }

    @Test
    fun `작동 중에 새 구간에 들어설 때마다 통과 1건`() = runTest(UnconfinedTestDispatcher()) {
        drive(100f)
        fake.setLocation(37.4127, 127.0947, headingDeg = 0.0)
        startDetector()

        cruise(CruiseMode.ACTIVATED)
        fake.setLocation(37.4128, 127.0947, headingDeg = 0.0) // 같은 칸
        fake.setLocation(37.4150, 127.0947, headingDeg = 0.0) // 북쪽 칸
        fake.setLocation(37.4150, 127.0947, headingDeg = 180.0) // 같은 칸, 반대 방향

        assertEquals(
            listOf(here, SegmentKey.of(fake.location.value!!.point, 0.0), SegmentKey.of(fake.location.value!!.point, 180.0)),
            passes.map { it.segment },
        )
    }

    @Test
    fun `작동 중이 아니면 통과를 세지 않는다`() = runTest(UnconfinedTestDispatcher()) {
        drive(100f)
        startDetector()

        fake.setLocation(37.4150, 127.0947, headingDeg = 0.0)
        cruise(CruiseMode.ENABLED)
        fake.setLocation(37.4170, 127.0947, headingDeg = 0.0)

        assertTrue(passes.isEmpty())
    }

    @Test
    fun `다시 켜면 같은 구간도 새 통과로 센다`() = runTest(UnconfinedTestDispatcher()) {
        drive(100f)
        startDetector()

        cruise(CruiseMode.ACTIVATED)
        advanceTimeBy(5_000)
        cruise(CruiseMode.ENABLED)
        cruise(CruiseMode.ACTIVATED)

        assertEquals(listOf(here, here), passes.map { it.segment })
    }
}
