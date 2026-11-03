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
 * 서버가 그 요청을 영영 받지 않을 때(형식 오류, 408·429를 뺀 4xx, 직렬화 오류) 저장소 구현이 던진다.
 * 앱은 이 기록을 다시 보내지 않고 버린 뒤 다음 기록으로 넘어간다(이슈 #19). 그 밖의 예외(네트워크 끊김·5xx·408·429)는 다시 보낸다
 */
class PermanentFailureException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** 다시 보내도 안 될 실패인가. [IllegalArgumentException]은 직렬화·형식 오류로 본다 */
fun isPermanentFailure(e: Throwable): Boolean = e is PermanentFailureException || e is IllegalArgumentException

/**
 * 해제 이벤트·통과 기록 업로드. 실제 구현(서버)은 황준용, 그 전에는 [FakeCruiseServer].
 * 규칙(docs/disengage-rules.md "업로드와 차량 ID"): 생길 때마다 실시간으로 부른다. 실패(예외)하면 앱이 같은 기록을
 * 5초·15초·1분·5분 간격으로 다시 보낸다([UploadQueue], 메모리 대기열 최대 500건, 앱이 죽으면 못 보낸 기록은 사라짐).
 * 다시 보내도 안 될 실패는 [PermanentFailureException]으로 던진다.
 */
interface CruiseEventRepository {
    suspend fun upload(event: DisengageUpload)
    suspend fun upload(pass: PassUpload)

    /**
     * 동의 철회(PRD 5.6, 11-02 결정): 이 가명 차량 ID의 `cruise_events`·`cruise_passes`를 서버에서 즉시 완전 삭제.
     * 지울 것이 없어도 오류가 아니다(같은 요청을 다시 보내도 됨). 실패하면 예외 → 앱이 [PendingDeletions]로 다시 보낸다
     */
    suspend fun deleteMine(vehicleId: String)
}
