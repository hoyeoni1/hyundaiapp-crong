package com.hyundaiapp.cruisebreaker.data

import com.hyundaiapp.cruisebreaker.domain.CruisePass
import com.hyundaiapp.cruisebreaker.domain.DisengageEvent
import java.time.Instant
import java.time.ZoneId

/**
 * 서버로 보내는 해제 1건. 좌표·차속은 없다 — [DisengageEvent.point]·`speedKph`가 실수로 섞이지 않게
 * 업로드는 이 형태로만 받는다(PR #3 리뷰). 시각은 시간 단위로 내림(PRD 5.6).
 */
data class DisengageUpload(
    val segmentId: String,
    val heading8: Int,
    /** 해제 시각을 시간 단위로 내린 값(epoch ms) */
    val hourEpochMs: Long,
    val vehicleId: String,
)

/** 서버로 보내는 크루즈 통과 1건. 날짜만 보낸다(한국 시간 기준 yyyy-MM-dd) */
data class PassUpload(
    val segmentId: String,
    val heading8: Int,
    val date: String,
    val vehicleId: String,
)

private const val HOUR_MS = 3_600_000L
private val KST: ZoneId = ZoneId.of("Asia/Seoul")

fun DisengageEvent.toUpload(vehicleId: String) =
    DisengageUpload(segment.geohash, segment.heading8, atMs - Math.floorMod(atMs, HOUR_MS), vehicleId)

fun CruisePass.toUpload(vehicleId: String) =
    PassUpload(segment.geohash, segment.heading8, Instant.ofEpochMilli(atMs).atZone(KST).toLocalDate().toString(), vehicleId)

/**
 * 해제 이벤트·통과 기록 업로드. 실제 구현(서버)은 황준용, 그 전에는 [FakeCruiseServer].
 * 규칙(docs/disengage-rules.md "업로드와 차량 ID"): 생길 때마다 실시간으로 부르고, 실패하면 구현이 쌓아 두었다가 다시 보낸다.
 */
interface CruiseEventRepository {
    suspend fun upload(event: DisengageUpload)
    suspend fun upload(pass: PassUpload)
}
