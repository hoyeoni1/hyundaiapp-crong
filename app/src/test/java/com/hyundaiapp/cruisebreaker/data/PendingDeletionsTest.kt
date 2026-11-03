package com.hyundaiapp.cruisebreaker.data

import com.hyundaiapp.core.GeoPoint
import com.hyundaiapp.cruisebreaker.domain.CruisePass
import com.hyundaiapp.cruisebreaker.domain.DisengageEvent
import com.hyundaiapp.cruisebreaker.domain.SegmentKey
import com.hyundaiapp.cruisebreaker.domain.retrying
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PendingDeletionsTest {
    /** 처음 [failures]번은 실패하는 저장소 */
    private class FlakyRepo(var failures: Int) : CruiseEventRepository {
        val deleted = mutableListOf<String>()
        var calls = 0
        override suspend fun upload(event: DisengageUpload) = Unit
        override suspend fun upload(pass: PassUpload) = Unit
        override suspend fun deleteMine(vehicleId: String) {
            calls++
            if (failures-- > 0) throw IOException("offline")
            deleted += vehicleId
        }
    }

    @Test
    fun `철회하면 차량 ID가 삭제 대기로 옮겨지고 성공할 때까지 다시 보낸다`() = runTest(UnconfinedTestDispatcher()) {
        val prefs = InMemoryAppPrefs(locationAgreed = true, privacyAgreed = true)
        val id = prefs.vehicleId()
        val repo = FlakyRepo(failures = 2)
        val deletions = PendingDeletions(prefs, repo)

        prefs.clearConsentAndData()
        assertEquals(setOf(id), prefs.pendingDeletions)
        deletions.resume(backgroundScope)
        assertEquals(1, repo.calls)

        advanceTimeBy(5_001) // 5초 뒤 두 번째
        assertEquals(2, repo.calls)
        advanceTimeBy(15_001) // 15초 뒤 세 번째 → 성공
        assertEquals(listOf(id), repo.deleted)
        assertTrue(prefs.pendingDeletions.isEmpty())
    }

    @Test
    fun `앱이 다시 켜지면 남은 삭제를 이어서 보내고, 보내는 중이면 겹치지 않는다`() = runTest(UnconfinedTestDispatcher()) {
        val prefs = InMemoryAppPrefs()
        prefs.vehicleId()
        prefs.clearConsentAndData() // 지난번 철회, 삭제는 못 끝냄
        val repo = FlakyRepo(failures = 1)
        val deletions = PendingDeletions(prefs, repo)

        deletions.resume(backgroundScope)
        deletions.resume(backgroundScope) // 앱 시작과 철회가 겹쳐도
        assertEquals(1, repo.calls)
        advanceTimeBy(5_001)
        assertEquals(2, repo.calls)
        assertTrue(prefs.pendingDeletions.isEmpty())
    }

    @Test
    fun `가짜 서버는 그 차량이 올린 만큼만 지운다`() = runTest {
        val here = GeoPoint(37.4127, 127.0947)
        val key = SegmentKey.of(here, 0.0)
        val server = FakeCruiseServer(seed = listOf(SeedCell(here, 0.0, "A", 5, 20)))
        server.upload(DisengageEvent(0L, key, 100f, here).toUpload("me"))
        server.upload(CruisePass(0L, key).toUpload("me"))
        server.upload(CruisePass(0L, key).toUpload("other"))

        server.deleteMine("me")
        server.deleteMine("me") // 다시 보내도 오류 아님

        val row = server.nearby(here, 100.0).single()
        assertEquals(5, row.disengages)
        assertEquals(21, row.passVehicles)
    }

    @Test
    fun `retrying은 간격을 끝까지 쓰면 마지막 간격으로 계속한다`() = runTest {
        var calls = 0
        retrying(delaysMs = listOf(10L, 20L)) {
            calls++
            if (calls < 4) throw IOException()
        }
        assertEquals(4, calls)
        assertEquals(10L + 20L + 20L, testScheduler.currentTime)
    }
}
