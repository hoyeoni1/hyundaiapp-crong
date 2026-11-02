package com.hyundaiapp.core.probe

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log

internal class NaviSdkConnector(
    private val context: Context,
    private val options: PleosSignalOptions,
) : SignalConnector {
    override val signals = options.signals.intersect(navigationSignals)
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var closing = false
    private lateinit var sink: SignalSink
    private var navi: NaviHelper? = null
    private var connected = false
    private var locationPending = false
    private var trafficPending = false
    private val locationTimeout = Runnable {
        locationPending = false
        if (!closing) sink.unavailable(SignalKeys.location, SignalStatus.ERROR, "Navigation location request timed out")
    }
    private val trafficTimeout = Runnable {
        trafficPending = false
        if (!closing) sink.unavailable(SignalKeys.traffic, SignalStatus.ERROR, "Navigation route request timed out")
    }
    private val connectionTimeout = Runnable {
        if (!closing) sink.fail(signals, SignalStatus.ERROR, "Navigation initialization timed out")
    }
    private val poll = object : Runnable {
        override fun run() {
            if (closing) return
            query()
            main.postDelayed(this, options.navigationPollMillis)
        }
    }

    private val listener = object : NaviHelperEventListener {
        override fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo) {
            main.post {
                if (closing || VehicleSignal.LOCATION !in signals) return@post
                main.removeCallbacks(locationTimeout)
                locationPending = false
                try { sink.value(SignalKeys.location, currentLocationInfo.toCore()) }
                catch (error: Exception) { sink.unavailable(SignalKeys.location, error.signalStatus(), error.message) }
            }
        }

        override fun onRouteStateInfo(routeStateInfo: RouteStateInfo) {
            main.post {
                if (closing || VehicleSignal.ROUTE_TRAFFIC !in signals) return@post
                main.removeCallbacks(trafficTimeout)
                trafficPending = false
                try { sink.value(SignalKeys.traffic, routeStateInfo.toCore()) }
                catch (error: Exception) { sink.unavailable(SignalKeys.traffic, error.signalStatus(), error.message) }
            }
        }

        override fun onRouteCancelled() = invalidateRoute()
        override fun onRouteEnded() = invalidateRoute()
        override fun onRouteStarted(routeStartInfo: RouteStartInfo) = invalidateRoute()

        override fun onError(errorCode: NaviErrorCode) {
            main.post {
                if (!closing && VehicleSignal.ROUTE_TRAFFIC in signals)
                    sink.unavailable(SignalKeys.traffic, SignalStatus.ERROR, "Navigation: ${errorCode.name}")
            }
        }
    }

    private fun invalidateRoute() {
        main.post {
            if (!closing && VehicleSignal.ROUTE_TRAFFIC in signals) {
                main.removeCallbacks(trafficTimeout)
                trafficPending = false
                sink.value(SignalKeys.traffic, null)
            }
        }
    }

    override fun start(sink: SignalSink) {
        this.sink = sink
        main.post {
            if (closing) return@post
            if (context.checkSelfPermission(SignalPermissions.NAVIGATION) != PackageManager.PERMISSION_GRANTED) {
                sink.fail(signals, SignalStatus.PERMISSION_DENIED)
                return@post
            }
            try {
                val sdk = NaviHelper(context)
                navi = sdk
                main.postDelayed(connectionTimeout, options.requestTimeoutMillis)
                sdk.initialize({ main.post {
                    if (!closing && !connected) {
                        main.removeCallbacks(connectionTimeout)
                        try {
                            sdk.addListener(listener)
                            connected = true
                            poll.run()
                        } catch (error: Exception) { sink.fail(signals, error.signalStatus(), error.message) }
                    }
                } }, { error -> main.post {
                    main.removeCallbacks(connectionTimeout)
                    if (!closing) sink.fail(signals, error.signalStatus(), error.message)
                } })
            } catch (error: Exception) {
                main.removeCallbacks(connectionTimeout)
                sink.fail(signals, error.signalStatus(), error.message)
            }
        }
    }

    private fun query() {
        val sdk = navi ?: return
        if (VehicleSignal.LOCATION in signals && !locationPending) {
            locationPending = true
            main.postDelayed(locationTimeout, options.requestTimeoutMillis)
            try { sdk.getCurrentLocationInfo() }
            catch (error: Exception) {
                locationPending = false
                main.removeCallbacks(locationTimeout)
                sink.unavailable(SignalKeys.location, error.signalStatus(), error.message)
            }
        }
        if (VehicleSignal.ROUTE_TRAFFIC in signals && !trafficPending) {
            trafficPending = true
            main.postDelayed(trafficTimeout, options.requestTimeoutMillis)
            try { sdk.getRouteStateInfo() }
            catch (error: Exception) {
                trafficPending = false
                main.removeCallbacks(trafficTimeout)
                sink.unavailable(SignalKeys.traffic, error.signalStatus(), error.message)
            }
        }
    }

    override fun stop() {
        closing = true
        main.post {
            main.removeCallbacks(poll)
            main.removeCallbacks(connectionTimeout)
            main.removeCallbacks(locationTimeout)
            main.removeCallbacks(trafficTimeout)
            navi?.let { sdk ->
                runCatching { sdk.removeListener(listener) }.onFailure { Log.w("VehicleSignals", "Navi listener cleanup failed", it) }
                runCatching { sdk.release() }.onFailure { Log.w("VehicleSignals", "Navi release failed", it) }
            }
            navi = null
            connected = false
        }
    }
}
