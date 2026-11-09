package com.hyundaiapp.cruisebreaker.ui.overlay

import com.hyundaiapp.core.*
import com.hyundaiapp.cruisebreaker.data.*
import com.hyundaiapp.cruisebreaker.domain.SegmentKey
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NearbyCardsTest {
    private val origin = GeoPoint(37.4, 127.0)
    private fun risk(offset: Double, name: String, risky: Boolean = true) =
        SegmentRisk(SegmentKey.of(GeoPoint(origin.latitude + offset, origin.longitude), 0.0),
            GeoPoint(origin.latitude + offset, origin.longitude), name, 40, 100, .4, 0.0, risky)
    private class Repo(var result: List<SegmentRisk> = emptyList()) : SegmentRiskRepository {
        var calls = 0
        var fail = false
        var waitMs = 0L
        override suspend fun nearby(center: GeoPoint, radiusM: Double): List<SegmentRisk> {
            calls++
            delay(waitMs)
            if (fail) error("offline")
            return result
        }
    }

    @Test fun `closest three rerank as location moves without fetching per fix`() = runTest {
        val signals = FakeVehicleSignals()
        signals.location.value = LocationState(origin, 0.0, receivedAtMs = 0)
        val repo = Repo(listOf(risk(.004, "four"), risk(.002, "two"), risk(.001, "one"), risk(.003, "three"), risk(.0001, "safe", false)))
        val model = NearbyCards(signals, repo) { testScheduler.currentTime }
        model.start(backgroundScope)
        advanceTimeBy(1_001)
        assertEquals(listOf("one", "two", "three"), model.state.value.rows.map { it.roadName })
        signals.location.value = LocationState(GeoPoint(37.4039, 127.0), 180.0, receivedAtMs = testScheduler.currentTime)
        runCurrent()
        assertEquals(listOf("four", "three", "two"), model.state.value.rows.map { it.roadName })
        assertEquals(1, repo.calls)
    }

    @Test fun `missing or expired location hides numeric distances and does not use fallback`() = runTest {
        val signals = FakeVehicleSignals().apply { location.value = null }
        val repo = Repo(listOf(risk(.001, "one")))
        val model = NearbyCards(signals, repo) { testScheduler.currentTime }
        model.start(backgroundScope); advanceTimeBy(1_001)
        assertEquals(0, repo.calls)
        assertEquals("위치 확인 중", model.state.value.message)
        signals.location.value = LocationState(origin, 0.0, receivedAtMs = testScheduler.currentTime)
        advanceTimeBy(1_001)
        assertEquals(1, model.state.value.rows.size)
        advanceTimeBy(16_000)
        assertTrue(model.state.value.rows.isEmpty())
        assertEquals("위치 확인 중", model.state.value.message)
    }

    @Test fun `failed expired cache is not reported as empty success and retry is throttled`() = runTest {
        val signals = FakeVehicleSignals()
        signals.location.value = LocationState(origin, 0.0, receivedAtMs = 0)
        val repo = Repo().apply { fail = true }
        val model = NearbyCards(signals, repo) { testScheduler.currentTime }
        model.start(backgroundScope); advanceTimeBy(1_001)
        assertEquals("구간 정보를 확인할 수 없어요", model.state.value.message)
        repeat(5) {
            signals.location.value = LocationState(origin, 0.0, receivedAtMs = testScheduler.currentTime)
            advanceTimeBy(1_000)
        }
        assertEquals(1, repo.calls)
        repo.fail = false
        advanceTimeBy(5_000)
        assertEquals(2, repo.calls)
        assertEquals("근처에 해제 다발 구간이 없어요", model.state.value.message)
    }

    @Test fun `moving beyond query cache hides old rows while new request is pending`() = runTest {
        val signals = FakeVehicleSignals()
        signals.location.value = LocationState(origin, 0.0, receivedAtMs = 0)
        val repo = Repo(listOf(risk(.001, "one")))
        val model = NearbyCards(signals, repo) { testScheduler.currentTime }
        model.start(backgroundScope); advanceTimeBy(1_001)
        assertEquals(1, model.state.value.rows.size)
        repo.waitMs = 20_000
        signals.location.value = LocationState(GeoPoint(37.42, 127.0), 0.0, receivedAtMs = testScheduler.currentTime)
        runCurrent()
        assertTrue(model.state.value.rows.isEmpty())
        assertEquals("구간 확인 중", model.state.value.message)
        advanceTimeBy(14_500)
        assertEquals("구간 정보를 확인할 수 없어요", model.state.value.message)
    }

    @Test fun `cancelling owner cancels pending network work`() = runTest {
        val signals = FakeVehicleSignals()
        signals.location.value = LocationState(origin, 0.0, receivedAtMs = 0)
        var cancelled = false
        val repo = object : SegmentRiskRepository {
            override suspend fun nearby(center: GeoPoint, radiusM: Double): List<SegmentRisk> {
                try { awaitCancellation() } finally { cancelled = true }
            }
        }
        val owner = CoroutineScope(backgroundScope.coroutineContext + Job())
        val model = NearbyCards(signals, repo) { testScheduler.currentTime }
        model.start(owner); runCurrent(); owner.cancel(); runCurrent()
        assertTrue(cancelled)
        assertTrue(model.state.value.rows.isEmpty())
    }

    @Test fun `movement locks for driving unknown gear disconnected signals and unknown UX`() {
        val parked = DriveState(gear = Gear.PARK, speedKph = 0f)
        assertTrue(overlayMovementAllowed(parked, false, SourceState.READY))
        for (gear in listOf(Gear.DRIVE, Gear.REVERSE, Gear.NEUTRAL, Gear.UNKNOWN, null)) {
            assertFalse(overlayMovementAllowed(parked.copy(gear = gear), false, SourceState.READY))
        }
        assertFalse(overlayMovementAllowed(parked.copy(speedKph = 1f), false, SourceState.READY))
        assertFalse(overlayMovementAllowed(parked, null, SourceState.READY))
        assertFalse(overlayMovementAllowed(parked, true, SourceState.READY))
        assertFalse(overlayMovementAllowed(parked, false, SourceState.FAILED))
    }

    @Test fun `relative placement remains inside changed bounds including edge positions`() {
        val large = CardArea(853, 90, 2535, 1300)
        assertEquals(1935 to 1032, large.position(1f, 1f, 600, 268))
        val small = CardArea(100, 80, 700, 600)
        assertEquals(100 to 332, small.position(1f, 1f, 600, 268))
        assertEquals(100 to 80, small.position(-1f, -1f, 600, 268))
    }
}
