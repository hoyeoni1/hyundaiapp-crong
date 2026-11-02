package com.hyundaiapp.cruisebreaker.domain

import com.hyundaiapp.core.CruiseMode
import com.hyundaiapp.core.FakeVehicleSignals
import com.hyundaiapp.core.Gear
import com.hyundaiapp.core.GeoPoint
import com.hyundaiapp.cruisebreaker.data.FakeCruiseServer
import com.hyundaiapp.cruisebreaker.data.SegmentRisk
import com.hyundaiapp.cruisebreaker.data.SegmentRiskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
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
class RiskAnnouncerTest {
    private val fake = FakeVehicleSignals()
    private val heard = mutableListOf<Announcement>()
    private lateinit var announcer: RiskAnnouncer

    // 출발점(에뮬레이터 위치)과 그 북쪽 위험 구간들
    private val lat0 = 37.4127
    private val lon0 = 127.0947
    private val ahead400 = risk(lat0 + 0.0036, lon0)
    private val ahead700 = risk(lat0 + 0.0063, lon0)

    private fun risk(lat: Double, lon: Double, heading8: Int = 0, isRisk: Boolean = true): SegmentRisk {
        val key = SegmentKey(SegmentKey.geohash(GeoPoint(lat, lon), 7), heading8)
        return SegmentRisk(key, GeoPoint(lat, lon), "", 40, 100, 0.4, 0.0, isRisk)
    }

    private class ListRepo(val rows: List<SegmentRisk>) : SegmentRiskRepository {
        override suspend fun nearby(center: GeoPoint, radiusM: Double) = rows
    }

    private fun TestScope.start(repo: SegmentRiskRepository) {
        val trips = TripTracker(fake)
        announcer = RiskAnnouncer(DisengageRules(), clock = { testScheduler.currentTime })
        backgroundScope.launch { announcer.announcements.collect { heard += it } }
        trips.start(backgroundScope)
        announcer.start(backgroundScope, fake, repo, trips)
    }

    private fun cruiseOn() {
        fake.setDrive { copy(gear = Gear.DRIVE, speedKph = 100f) }
        fake.setCruise { copy(mode = CruiseMode.ACTIVATED) }
    }

    /** 북쪽으로 dLat만큼 이동(위치 갱신 1회) */
    private fun at(dLat: Double, headingDeg: Double = 0.0) = fake.setLocation(lat0 + dLat, lon0, headingDeg)

    @Test
    fun `크루즈 작동 중 앞 500m 안에 위험 구간이 있으면 1번 안내하고 10초 뒤 배너를 내린다`() =
        runTest(UnconfinedTestDispatcher()) {
            cruiseOn()
            start(ListRepo(listOf(ahead400)))
            at(0.00001)

            assertEquals(1, heard.size)
            assertEquals("500m 앞 크루즈 해제 잦은 구간", announcer.current.value?.bannerText)
            assertEquals("500미터 앞, 크루즈 해제가 잦은 구간입니다.", heard[0].speechText)
            advanceTimeBy(10_001)
            assertNull(announcer.current.value)
        }

    @Test
    fun `크루즈가 꺼져 있으면 안내하지 않는다`() = runTest(UnconfinedTestDispatcher()) {
        fake.setDrive { copy(gear = Gear.DRIVE, speedKph = 100f) }
        fake.setCruise { copy(mode = CruiseMode.ENABLED) }
        start(ListRepo(listOf(ahead400)))
        at(0.00001)

        assertTrue(heard.isEmpty())
    }

    @Test
    fun `500m보다 멀면 기다렸다가 들어올 때 안내한다`() = runTest(UnconfinedTestDispatcher()) {
        cruiseOn()
        start(ListRepo(listOf(ahead700)))
        at(0.00001) // 약 700m
        assertTrue(heard.isEmpty())
        at(0.0027) // 약 400m
        assertEquals(1, heard.size)
    }

    @Test
    fun `뒤쪽·반대 방향·위험 아닌 구간은 안내하지 않는다`() = runTest(UnconfinedTestDispatcher()) {
        cruiseOn()
        start(
            ListRepo(
                listOf(
                    risk(lat0 + 0.0036, lon0, heading8 = 4), // 반대 차선
                    risk(lat0 + 0.0036, lon0, isRisk = false),
                ),
            ),
        )
        at(0.00001)
        at(0.00002, headingDeg = 180.0) // 남쪽으로 달리면 위험 구간이 뒤에 있음
        assertTrue(heard.isEmpty())
    }

    @Test
    fun `같은 구간은 한 주행에 1번, 새 주행이면 다시 안내`() = runTest(UnconfinedTestDispatcher()) {
        cruiseOn()
        start(ListRepo(listOf(ahead400)))
        at(0.00001)
        advanceTimeBy(4 * 60_000L)
        at(0.00002)
        assertEquals(1, heard.size)

        fake.setDrive { copy(gear = Gear.PARK) }
        fake.setDrive { copy(gear = Gear.DRIVE) }
        at(0.00003)
        assertEquals(2, heard.size)
    }

    @Test
    fun `안내 사이는 최소 3분`() = runTest(UnconfinedTestDispatcher()) {
        cruiseOn()
        val other = risk(lat0 + 0.0040, lon0) // 이웃 칸
        start(ListRepo(listOf(ahead400, other)))
        at(0.00001)
        at(0.00002)
        assertEquals(1, heard.size)

        advanceTimeBy(3 * 60_000L)
        at(0.00003)
        assertEquals(2, heard.size)
        assertEquals(other.key, heard[1].risk.key)
    }

    @Test
    fun `에뮬레이터 위치에서 시드의 시연 지점이 안내된다`() = runTest(UnconfinedTestDispatcher()) {
        cruiseOn()
        start(FakeCruiseServer())
        at(0.0)

        assertEquals(1, heard.size)
        assertTrue(heard[0].risk.roadName.startsWith("시연 지점"))
    }

    @Test
    fun `조회가 느려도 위치 처리를 막지 않고, 결과가 오면 다음 위치에서 안내한다`() = runTest(UnconfinedTestDispatcher()) {
        cruiseOn()
        val slow = object : SegmentRiskRepository {
            override suspend fun nearby(center: GeoPoint, radiusM: Double): List<SegmentRisk> {
                delay(3_000)
                return listOf(ahead400)
            }
        }
        start(slow)
        at(0.00001) // 조회 시작, 아직 목록 없음
        at(0.00002) // 위치 처리는 바로 끝남(막히지 않음)
        assertTrue(heard.isEmpty())

        advanceTimeBy(3_001)
        at(0.00003)
        assertEquals(1, heard.size)
    }

    @Test
    fun `조회가 5초 넘게 걸리면 포기한다`() = runTest(UnconfinedTestDispatcher()) {
        cruiseOn()
        var calls = 0
        val hang = object : SegmentRiskRepository {
            override suspend fun nearby(center: GeoPoint, radiusM: Double): List<SegmentRisk> {
                calls++
                awaitCancellation()
            }
        }
        start(hang)
        at(0.00001)
        advanceTimeBy(5_001)
        advanceTimeBy(60_000) // 1분 뒤 다시 조회
        at(0.00002)
        assertEquals(2, calls)
        assertTrue(heard.isEmpty())
    }

    @Test
    fun `reset하면 배너가 내려간다`() = runTest(UnconfinedTestDispatcher()) {
        cruiseOn()
        start(ListRepo(listOf(ahead400)))
        at(0.00001)
        assertEquals(1, heard.size)

        announcer.reset()
        assertNull(announcer.current.value)
    }
}
