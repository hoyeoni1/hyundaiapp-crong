package com.hyundaiapp.crong.data

import com.hyundaiapp.core.GeoPoint
import com.hyundaiapp.crong.domain.CruisePass
import com.hyundaiapp.crong.domain.DisengageEvent
import com.hyundaiapp.crong.domain.SegmentKey
import org.junit.Assert.assertEquals
import java.lang.reflect.Modifier
import org.junit.Test

class UploadRecordTest {
    private val key = SegmentKey("wydku4g", 2)

    // 2026-11-01 10:23:45.678 KST = 01:23:45.678 UTC
    private val at = 1_793_496_225_678L

    @Test
    fun `서버로 보내는 형태에는 좌표·차속이 없다`() {
        val fields = listOf(DisengageUpload::class.java, PassUpload::class.java)
            .flatMap { c -> c.declaredFields.filterNot { it.isSynthetic || Modifier.isStatic(it.modifiers) }.map { it.name } }
            .toSet()
        assertEquals(setOf("segmentId", "heading8", "hourEpochMs", "date", "vehicleId"), fields)
    }

    @Test
    fun `해제 시각은 시간 단위로 내린다`() {
        val up = DisengageEvent(at, key, 100f, GeoPoint(37.4127, 127.0947)).toUpload("v1")
        assertEquals(DisengageUpload("wydku4g", 2, at - (23 * 60_000L + 45_678L), "v1"), up)
    }

    @Test
    fun `통과는 한국 시간 날짜만 보낸다`() {
        assertEquals(PassUpload("wydku4g", 2, "2026-11-01", "v1"), CruisePass(at, key).toUpload("v1"))
        // 자정 직전 UTC 15:30 = 다음 날 00:30 KST
        val utc1530 = at + (14 * 3_600_000L + 6 * 60_000L + 14_322L)
        assertEquals("2026-11-02", CruisePass(utc1530, key).toUpload("v1").date)
    }

    @Test
    fun `동의 철회하면 동의와 차량 ID가 지워진다`() {
        val prefs = InMemoryAppPrefs(locationAgreed = true, privacyAgreed = true, autoRecordEnabled = false)
        val id = prefs.vehicleId()
        assertEquals(id, prefs.vehicleId())

        prefs.clearConsentAndData()
        assertEquals(false, prefs.onboarded)
        assertEquals(true, prefs.autoRecordEnabled)
        assertEquals(false, id == prefs.vehicleId())
    }
}
