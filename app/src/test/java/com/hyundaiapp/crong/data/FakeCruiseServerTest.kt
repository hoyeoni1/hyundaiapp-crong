package com.hyundaiapp.crong.data

import com.hyundaiapp.core.GeoPoint
import com.hyundaiapp.crong.domain.DisengageEvent
import com.hyundaiapp.crong.domain.SegmentKey
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeCruiseServerTest {
    private val here = GeoPoint(37.4127, 127.0947)
    private val key = SegmentKey.of(here, 0.0)
    private val north = key.neighbors()[1] // 북쪽 이웃 칸

    @Test
    fun `칸 경계에 나뉜 해제도 9칸을 합쳐 위험으로 잡는다`() = runTest {
        // 칸마다 따로 보면 통과 12대라 20대 기준 미달. 합치면 8/24 = 33%, 24대
        val server = FakeCruiseServer(
            seed = listOf(
                SeedCell(here, 0.0, "A", 4, 12),
                SeedCell(north.center(), 0.0, "B", 4, 12),
            ),
        )
        val rows = server.nearby(here, 1_000.0)

        assertEquals(2, rows.size) // 기록이 있는 칸만
        assertTrue(rows.all { it.isRisk && it.disengages == 8 && it.passVehicles == 24 })
    }

    @Test
    fun `반대 방향 기록은 합치지 않는다`() = runTest {
        val server = FakeCruiseServer(
            seed = listOf(
                SeedCell(here, 0.0, "A", 4, 12),
                SeedCell(north.center(), 180.0, "B", 4, 12),
            ),
        )
        assertFalse(server.nearby(here, 1_000.0).any { it.isRisk })
    }

    @Test
    fun `업로드한 해제가 바로 반영된다`() = runTest {
        val server = FakeCruiseServer(seed = listOf(SeedCell(here, 0.0, "A", 5, 20)))
        server.upload(DisengageEvent(0L, key, 100f, here).toUpload("v1"))

        val row = server.nearby(here, 100.0).single()
        assertEquals(6, row.disengages)
        assertTrue(row.isRisk)
    }
}
