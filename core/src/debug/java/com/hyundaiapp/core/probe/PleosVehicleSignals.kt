package com.hyundaiapp.core.probe

import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock

/** Only selected signals are accessed. Permission dialogs belong to the host app. */
data class PleosSignalOptions(
    val signals: Set<VehicleSignal>,
    val navigationPollMillis: Long = 1_000L,
    val chargeSwitchPollMillis: Long = 10_000L,
    val requestTimeoutMillis: Long = 5_000L,
) {
    init {
        require(navigationPollMillis >= 1_000L)
        require(chargeSwitchPollMillis >= 1_000L)
        require(requestTimeoutMillis >= 1_000L)
    }

    companion object {
        fun cruiseBreaker() = PleosSignalOptions(setOf(
            VehicleSignal.CRUISE_STATE, VehicleSignal.CRUISE_TARGET_SPEED, VehicleSignal.BRAKE,
            VehicleSignal.STEERING, VehicleSignal.VEHICLE_SPEED, VehicleSignal.LOCATION, VehicleSignal.ROUTE_TRAFFIC,
        ))
        fun chachelin() = PleosSignalOptions(setOf(
            VehicleSignal.SOC, VehicleSignal.CHARGE_PORT, VehicleSignal.CHARGE_SWITCH, VehicleSignal.LOCATION,
        ))
    }
}

object SignalPermissions {
    const val ENERGY = "pleos.car.permission.CAR_ENERGY"
    const val DRIVE = "pleos.car.permission.CAR_DRIVE"
    const val NAVIGATION = "pleos.car.permission.NAVI_ROUTE"
    const val SPEED = "android.car.permission.CAR_SPEED"

    fun required(options: PleosSignalOptions): Set<String> = buildSet {
        if (options.signals.any { it in energySignals }) add(ENERGY)
        if (options.signals.any { it in driveSignals }) add(DRIVE)
        if (options.signals.any { it in navigationSignals }) add(NAVIGATION)
        if (VehicleSignal.VEHICLE_SPEED in options.signals) add(SPEED)
    }

    fun missing(context: Context, options: PleosSignalOptions): Set<String> =
        required(options).filterTo(mutableSetOf()) { context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
}

internal val energySignals = setOf(VehicleSignal.SOC, VehicleSignal.CHARGE_PORT, VehicleSignal.CHARGE_SWITCH)
internal val driveSignals = setOf(VehicleSignal.CRUISE_STATE, VehicleSignal.CRUISE_TARGET_SPEED, VehicleSignal.BRAKE, VehicleSignal.STEERING)
internal val navigationSignals = setOf(VehicleSignal.LOCATION, VehicleSignal.ROUTE_TRAFFIC)

/** Real read-only SDK adapter. Create after explicit app consent; start/stop do not show dialogs. */
class PleosVehicleSignals(context: Context, options: PleosSignalOptions) : VehicleSignals {
    private val application = context.applicationContext
    private val frozenOptions = options.copy(signals = options.signals.toSet())
    private val delegate = ManagedVehicleSignals(
        connectorFactory = {
            buildList {
                if (frozenOptions.signals.any { it in energySignals || it in driveSignals })
                    add(VehicleSdkConnector(application, frozenOptions))
                if (frozenOptions.signals.any { it in navigationSignals })
                    add(NaviSdkConnector(application, frozenOptions))
                if (VehicleSignal.VEHICLE_SPEED in frozenOptions.signals)
                    add(AaosSpeedConnector(application, frozenOptions.requestTimeoutMillis))
            }
        },
        clockMillis = SystemClock::elapsedRealtime,
    )
    override val snapshot = delegate.snapshot
    override fun start() = delegate.start()
    override fun stop() = delegate.stop()
}
