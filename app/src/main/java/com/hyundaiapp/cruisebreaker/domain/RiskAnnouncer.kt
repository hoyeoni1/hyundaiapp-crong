package com.hyundaiapp.cruisebreaker.domain

import com.hyundaiapp.core.GeoMath
import com.hyundaiapp.core.GeoPoint
import com.hyundaiapp.core.LocationState
import com.hyundaiapp.core.VehicleSignals
import com.hyundaiapp.cruisebreaker.data.SegmentRisk
import com.hyundaiapp.cruisebreaker.data.SegmentRiskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/** 지금 화면에 띄울 안내(F4). 배너 한 줄 + TTS 한 문장 */
data class Announcement(val bannerText: String, val speechText: String, val risk: SegmentRisk)

/**
 * 진입 전 음성 안내(F4). 담당: 이민하. 규칙: docs/disengage-rules.md "안내".
 * - 크루즈 작동 중일 때만
 * - 진행 방향 ±30° 안, 앞 300~500m에 위험 구간 중심이 들어오면 1회. 위험 구간의 방향도 내 진행 방향과 45° 안
 * - 같은 구간은 한 주행([TripTracker])에 1회, 안내 사이 최소 3분. 붙어 있는 위험 칸들은 이 3분 간격으로 한 번만 안내된다
 * - 문구는 원인 구분 없이 한 가지. 음성은 [announcements]를 [RiskSpeaker]가 받아 읽는다
 */
class RiskAnnouncer(
    private val rules: DisengageRules,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _current = MutableStateFlow<Announcement?>(null)
    private val _announcements = MutableSharedFlow<Announcement>(extraBufferCapacity = 4)

    /** S1 배너. 안내 뒤 [DisengageRules.bannerMs]가 지나면 null */
    val current: StateFlow<Announcement?> = _current.asStateFlow()

    /** 안내가 나갈 때마다 1번(음성용) */
    val announcements: SharedFlow<Announcement> = _announcements.asSharedFlow()

    private val announcedThisTrip = mutableSetOf<SegmentKey>()
    private var lastAnnounceMs: Long? = null
    private var clearJob: Job? = null

    private var nearby: List<SegmentRisk> = emptyList()
    private var nearbyAt: GeoPoint? = null
    private var nearbyAtMs = 0L

    fun start(scope: CoroutineScope, signals: VehicleSignals, risks: SegmentRiskRepository, trips: TripTracker) {
        scope.launch { trips.tripNumber.collect { announcedThisTrip.clear() } }
        scope.launch {
            signals.location.filterNotNull().collect { location ->
                if (CruiseStateInterpreter.isEngaged(signals.cruise.value.mode)) onLocation(scope, location, risks)
            }
        }
    }

    private suspend fun onLocation(scope: CoroutineScope, location: LocationState, risks: SegmentRiskRepository) {
        val now = clock()
        val last = lastAnnounceMs
        if (last != null && now - last < rules.announceCooldownMs) return
        refreshNearby(location.point, now, risks)
        val hit = nearby.firstOrNull { it.key !in announcedThisTrip && isAhead(location, it) } ?: return

        announcedThisTrip += hit.key
        lastAnnounceMs = now
        val announcement = announcementFor(hit)
        _current.value = announcement
        _announcements.tryEmit(announcement)
        clearJob?.cancel()
        clearJob = scope.launch {
            delay(rules.bannerMs)
            if (_current.value === announcement) _current.value = null
        }
    }

    private fun isAhead(location: LocationState, risk: SegmentRisk): Boolean {
        if (!risk.isRisk) return false
        val distance = GeoMath.distanceM(location.point, risk.center)
        if (distance > rules.announceDistanceM || distance < rules.announceDistanceM - rules.announceMarginM) return false
        val bearing = GeoMath.bearingDeg(location.point, risk.center)
        if (GeoMath.angleDiffDeg(location.headingDeg, bearing) > rules.announceAngleDeg) return false
        // 반대 차선(반대 방향) 위험 구간은 안내하지 않는다
        return GeoMath.angleDiffDeg(location.headingDeg, risk.key.heading8 * 45.0) <= 45.0
    }

    /** 2km 안을 조회해 두고, 500m 넘게 움직였거나 1분이 지나면 다시 조회한다. 실패하면 이전 목록을 쓴다 */
    private suspend fun refreshNearby(point: GeoPoint, now: Long, risks: SegmentRiskRepository) {
        val at = nearbyAt
        if (at != null && GeoMath.distanceM(at, point) < REFRESH_MOVE_M && now - nearbyAtMs < REFRESH_MS) return
        try {
            nearby = risks.nearby(point, NEARBY_RADIUS_M)
            nearbyAt = point
            nearbyAtMs = now
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // 네트워크 오류 등: 이전 목록 유지
        }
    }

    private fun announcementFor(risk: SegmentRisk): Announcement {
        val m = rules.announceDistanceM.toInt()
        return Announcement(
            bannerText = "${m}m 앞 크루즈 해제 잦은 구간",
            speechText = "${m}미터 앞, 크루즈 해제가 잦은 구간입니다.",
            risk = risk,
        )
    }

    private companion object {
        const val NEARBY_RADIUS_M = 2_000.0
        const val REFRESH_MOVE_M = 500.0
        const val REFRESH_MS = 60_000L
    }
}
