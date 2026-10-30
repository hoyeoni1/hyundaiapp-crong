package com.hyundaiapp.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.hyundaiapp.core.internal.AaosSpeedSource
import com.hyundaiapp.core.internal.Logs
import com.hyundaiapp.core.internal.NaviSource
import com.hyundaiapp.core.internal.VehicleSdkSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 실제 에뮬레이터·차량용 구현. 앱에 하나만 만들어 Application 수명 동안 쓴다.
 *
 * ```
 * val signals = PleosVehicleSignals(context, setOf(CoreFeature.EV_BATTERY, CoreFeature.NAVI))
 * signals.start()
 * ```
 * SDK 콜백과 재시도는 모두 메인 스레드 Handler에서 처리한다. Flow 값은 어느 스레드에서 읽어도 된다.
 */
class PleosVehicleSignals(
    context: Context,
    private val features: Set<CoreFeature>,
) : VehicleSignals, NaviControl {

    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())

    private val _evBattery = MutableStateFlow(EvBatteryState())
    private val _drive = MutableStateFlow(DriveState())
    private val _cruise = MutableStateFlow(CruiseState())
    private val _location = MutableStateFlow<LocationState?>(null)
    private val _route = MutableStateFlow(RouteState())
    private val _sources = MutableStateFlow(SourceStatus())
    private val _operators = MutableStateFlow<List<ChargerOperatorInfo>>(emptyList())

    override val evBattery: StateFlow<EvBatteryState> = _evBattery.asStateFlow()
    override val drive: StateFlow<DriveState> = _drive.asStateFlow()
    override val cruise: StateFlow<CruiseState> = _cruise.asStateFlow()
    override val location: StateFlow<LocationState?> = _location.asStateFlow()
    override val route: StateFlow<RouteState> = _route.asStateFlow()
    override val sources: StateFlow<SourceStatus> = _sources.asStateFlow()
    override val chargerOperators: StateFlow<List<ChargerOperatorInfo>> = _operators.asStateFlow()

    private val reportError: (String) -> Unit = { msg -> _sources.update { it.copy(lastError = msg) } }

    private val vehicleSource = VehicleSdkSource(
        appContext, features, handler, _evBattery, _drive, _cruise,
        setState = { s -> _sources.update { it.copy(vehicle = s) } },
        error = reportError,
    )
    private val speedSource = AaosSpeedSource(
        appContext, handler, _drive,
        setState = { s -> _sources.update { it.copy(speed = s) } },
        error = reportError,
    )
    private val naviSource = NaviSource(
        appContext, handler, _location, _route, _operators,
        setState = { s -> _sources.update { it.copy(navi = s) } },
        error = reportError,
    )

    private var started = false

    override fun start() = onMain {
        if (started) return@onMain
        started = true
        Logs.i("start features=$features")
        vehicleSource.start()
        if (CoreFeature.SPEED in features) speedSource.start()
        if (CoreFeature.NAVI in features) naviSource.start()
    }

    override fun stop() = onMain {
        if (!started) return@onMain
        started = false
        vehicleSource.stop()
        speedSource.stop()
        naviSource.stop()
        _sources.value = SourceStatus()
    }

    override fun onPermissionsChanged() = onMain {
        if (!started || CoreFeature.SPEED !in features) return@onMain
        if (_sources.value.speed == SourceState.READY) return@onMain
        speedSource.stop()
        speedSource.start()
    }

    override fun requestRoute(destination: RouteDestination) = onMain { naviSource.requestRoute(destination) }

    override fun cancelRoute() = onMain { naviSource.cancelRoute() }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else handler.post(block)
    }
}
