package com.hyundaiapp.core.internal

import ai.pleos.playground.navi.constants.NaviMode
import ai.pleos.playground.navi.constants.NavigationStatus
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.RouteOption
import ai.pleos.playground.navi.constants.TrafficCongestionType
import ai.pleos.playground.navi.data.ChargerOperator
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.os.Handler
import com.hyundaiapp.core.ChargerOperatorInfo
import com.hyundaiapp.core.CongestionLevel
import com.hyundaiapp.core.CongestionSegment
import com.hyundaiapp.core.GeoPoint
import com.hyundaiapp.core.LocationState
import com.hyundaiapp.core.RouteDestination
import com.hyundaiapp.core.RouteState
import com.hyundaiapp.core.SourceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * NaviHelper SDK → 위치·경로 Flow.
 *
 * `get*Info()`는 반환값 없이 리스너로 결과를 준다(0.1~1초). 위치는 리스너가 따로 없어 [LOCATION_POLL_MS]마다 조회한다.
 * 에뮬레이터는 위치가 판교 부근에 고정이고 모의 주행이 없다(findings 6.4).
 */
internal class NaviSource(
    private val context: Context,
    private val handler: Handler,
    private val location: MutableStateFlow<LocationState?>,
    private val route: MutableStateFlow<RouteState>,
    private val operators: MutableStateFlow<List<ChargerOperatorInfo>>,
    private val setState: (SourceState) -> Unit,
    private val error: (String) -> Unit,
) {
    private var navi: NaviHelper? = null
    @Volatile private var ready = false
    @Volatile private var running = false
    // 마지막 위치 응답 시각. 내비 서비스가 재시작되면 호출은 나가도 응답이 안 온다(실측) → 재연결 판단에 씀
    @Volatile private var lastReplyMs = 0L

    private val listener = object : NaviHelperEventListener {
        override fun onCurrentLocationInfo(info: CurrentLocationInfo) {
            location.value = LocationState(
                point = GeoPoint(info.latitude, info.longitude),
                headingDeg = info.heading,
                address = info.address,
                receivedAtMs = System.currentTimeMillis(),
            )
            lastReplyMs = System.currentTimeMillis()
        }

        override fun onRouteStateInfo(info: RouteStateInfo) {
            route.value = info.toRouteState()
        }

        override fun onNavigationStatus(status: NavigationStatus) {
            if (status != NavigationStatus.ROUTE_DRIVE) route.update { RouteState() }
            refreshRoute()
        }

        override fun onRouteStarted(info: RouteStartInfo) = refreshRoute()

        override fun onRouteCancelled() {
            route.value = RouteState()
        }

        override fun onRouteEnded() {
            route.value = RouteState()
        }

        override fun onChargerOperatorInfo(list: List<ChargerOperator>) {
            operators.value = list.map { ChargerOperatorInfo(it.id, it.name) }
        }

        override fun onError(code: NaviErrorCode) {
            error("내비 오류: $code")
        }
    }

    fun start() {
        running = true
        connect()
    }

    private fun connect() {
        setState(SourceState.CONNECTING)
        val n = NaviHelper(context)
        navi = n
        n.initialize({
            handler.post {
                if (navi !== n) return@post
                n.addListener(listener)
                ready = true
                lastReplyMs = System.currentTimeMillis()
                Logs.i("navi ready")
                setState(SourceState.READY)
                runCatching { n.getChargerOperatorInfo() }
                pollLocation()
                pollRoute()
            }
        }, { e ->
            handler.post {
                if (navi !== n) return@post
                error("내비 연결 실패: ${e.message} — ${RECONNECT_MS / 1000}초 뒤 다시 시도")
                setState(SourceState.FAILED)
                reconnect()
            }
        })
    }

    fun stop() {
        running = false
        handler.removeCallbacksAndMessages(RECONNECT_TOKEN)
        disconnect()
    }

    private fun disconnect() {
        ready = false
        handler.removeCallbacksAndMessages(TOKEN)
        navi?.let { n ->
            runCatching { n.removeListener(listener) }
            runCatching { n.release() }
        }
        navi = null
    }

    fun requestRoute(destination: RouteDestination) {
        val n = navi?.takeIf { ready } ?: return error("내비 준비 전이라 경로 요청을 못 함")
        runCatching {
            n.requestRoute(
                RouteInfo(
                    latitude = destination.point.latitude,
                    longitude = destination.point.longitude,
                    displayLatitude = null,
                    displayLongitude = null,
                    poiName = destination.name,
                    poiId = "",
                    address = destination.address,
                    poiSubId = "",
                    routeOption = RouteOption.RECOMMENDED,
                    poiProvider = "",
                    isMobileRequest = false,
                ),
            )
        }.onFailure { error("경로 요청 실패: ${it.message}") }
    }

    fun cancelRoute() {
        navi?.takeIf { ready }?.let { runCatching { it.cancelRoute() } }
    }

    private fun refreshRoute() {
        navi?.takeIf { ready }?.let { runCatching { it.getRouteStateInfo() } }
    }

    private fun reconnect() {
        disconnect()
        if (!running) return
        handler.removeCallbacksAndMessages(RECONNECT_TOKEN)
        handler.postDelayed({ if (running) connect() }, RECONNECT_TOKEN, RECONNECT_MS)
    }

    private fun pollLocation() {
        if (!ready) return
        if (System.currentTimeMillis() - lastReplyMs > STALE_MS) {
            Logs.w("navi 응답 없음 → 재연결")
            error("내비 응답 없음 — 재연결 중")
            setState(SourceState.CONNECTING)
            reconnect()
            return
        }
        runCatching { navi?.getCurrentLocationInfo() }
        handler.postDelayed({ pollLocation() }, TOKEN, LOCATION_POLL_MS)
    }

    private fun pollRoute() {
        if (!ready) return
        refreshRoute()
        handler.postDelayed({ pollRoute() }, TOKEN, ROUTE_POLL_MS)
    }

    private companion object {
        val TOKEN = Any()
        val RECONNECT_TOKEN = Any()
        const val STALE_MS = 10_000L
        const val RECONNECT_MS = 3_000L
        // 100km/h ≈ 28m/s → 1초면 약 28m. 크루즈브레이커 구간(약 150m) 판정에 충분
        const val LOCATION_POLL_MS = 1_000L
        const val ROUTE_POLL_MS = 5_000L
    }
}

internal fun RouteStateInfo.toRouteState(): RouteState {
    // 실측: 안내 중에도 isRouting=false, naviMode=ROUTE_GUIDING 으로 온다(findings 3장)
    val guiding = naviMode == NaviMode.ROUTE_GUIDING || naviMode == NaviMode.SIMULATION_GUIDING
    return RouteState(
        guiding = guiding,
        congestion = if (!guiding) emptyList() else trafficStatus?.congestionItemList.orEmpty().map {
            CongestionSegment(
                offsetM = it.offset,
                lengthM = it.distance,
                level = it.trafficCongestionType.toLevel(),
                avgSpeedKph = it.avrSpeed,
                roadName = it.roadName.orEmpty(),
            )
        },
    )
}

internal fun TrafficCongestionType?.toLevel(): CongestionLevel = when (this) {
    TrafficCongestionType.FAST -> CongestionLevel.FAST
    TrafficCongestionType.MODERATE -> CongestionLevel.MODERATE
    TrafficCongestionType.SLOW -> CongestionLevel.SLOW
    TrafficCongestionType.HEAVY -> CongestionLevel.HEAVY
    else -> CongestionLevel.UNKNOWN
}
