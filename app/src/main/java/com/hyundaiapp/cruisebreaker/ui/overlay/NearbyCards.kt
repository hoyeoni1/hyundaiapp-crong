package com.hyundaiapp.cruisebreaker.ui.overlay

import com.hyundaiapp.core.GeoMath
import com.hyundaiapp.core.GeoPoint
import com.hyundaiapp.core.LocationState
import com.hyundaiapp.core.VehicleSignals
import com.hyundaiapp.cruisebreaker.data.SegmentRiskRepository
import com.hyundaiapp.cruisebreaker.data.SegmentRisk
import com.hyundaiapp.cruisebreaker.ui.RiskRowUi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class NearbyCardState(val message: String? = "위치 확인 중", val rows: List<RiskRowUi> = emptyList())

/** 표시 전용 조회. 안내기/TTS에는 이벤트를 보내지 않는다. */
internal class NearbyCards(
    private val signals: VehicleSignals,
    private val repository: SegmentRiskRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutable = MutableStateFlow(NearbyCardState())
    val state = mutable.asStateFlow()
    private var location: LocationState? = null
    private var cached: List<SegmentRisk>? = null
    private var center: GeoPoint? = null
    private var fetchedAt = 0L
    private var attemptedAt: Long? = null
    private var failed = false
    private var request: Job? = null

    fun start(scope: CoroutineScope) {
        scope.launch { signals.location.collect { location = it; update(scope) } }
        scope.launch { while (isActive) { update(scope); delay(1_000) } }
    }

    private fun update(scope: CoroutineScope) {
        val now = clock()
        val loc = location?.takeIf {
            now - it.receivedAtMs in 0..LOCATION_MAX_AGE_MS &&
                it.point.latitude.isFinite() && it.point.latitude in -90.0..90.0 &&
                it.point.longitude.isFinite() && it.point.longitude in -180.0..180.0
        }
        if (loc == null) { mutable.value = NearbyCardState(); return }
        val valid = cached != null && now - fetchedAt in 0 until CACHE_MS &&
            center?.let { GeoMath.distanceM(it, loc.point) < REFRESH_MOVE_M } == true
        if (!valid && request?.isActive != true && (attemptedAt == null || now - attemptedAt!! >= RETRY_MS)) {
            attemptedAt = now
            failed = false
            request = scope.launch {
                try {
                    val result = withTimeout(5_000) { repository.nearby(loc.point, 10_000.0) }
                    ensureActive()
                    cached = result
                    center = loc.point
                    fetchedAt = clock()
                } catch (_: TimeoutCancellationException) { failed = true }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { failed = true }
                update(scope) // 결과/오류를 다음 위치 이벤트까지 기다리지 않고 표시한다.
            }
        }
        mutable.value = if (valid) {
            val rows = cached.orEmpty().filter { it.isRisk }.map { risk ->
                RiskRowUi(risk.key.toString(), risk.roadName, GeoMath.distanceM(loc.point, risk.center), risk.rate, risk.passVehicles)
            }.filter { it.distanceM!!.isFinite() && it.distanceM <= 10_000 }
                .sortedWith(compareBy<RiskRowUi> { it.distanceM }.thenBy { it.id }).take(3)
            NearbyCardState(if (rows.isEmpty()) "근처에 해제 다발 구간이 없어요" else null, rows)
        } else NearbyCardState(if (failed) "구간 정보를 확인할 수 없어요" else "구간 확인 중")
    }

    companion object {
        const val LOCATION_MAX_AGE_MS = 15_000L
        const val CACHE_MS = 60_000L
        const val RETRY_MS = 10_000L
        const val REFRESH_MOVE_M = 500.0
    }
}

internal data class CardArea(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width get() = right - left
    val height get() = bottom - top
    fun position(xFraction: Float, yFraction: Float, cardWidth: Int, cardHeight: Int): Pair<Int, Int> =
        left + ((width - cardWidth).coerceAtLeast(0) * xFraction.coerceIn(0f, 1f)).toInt() to
            top + ((height - cardHeight).coerceAtLeast(0) * yFraction.coerceIn(0f, 1f)).toInt()
}

internal fun overlayMovementAllowed(drive: com.hyundaiapp.core.DriveState, restricted: Boolean?, source: com.hyundaiapp.core.SourceState): Boolean =
    restricted == false && source == com.hyundaiapp.core.SourceState.READY && com.hyundaiapp.cruisebreaker.ui.canShowParkedScreens(drive)
