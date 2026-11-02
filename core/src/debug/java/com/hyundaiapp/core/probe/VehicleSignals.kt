package com.hyundaiapp.core.probe

import kotlinx.coroutines.flow.StateFlow

/** SDK-independent vehicle contract. See core/README.md for units and lifecycle. */
interface VehicleSignals {
    val snapshot: StateFlow<VehicleSnapshot>
    fun start()
    fun stop()
}

enum class SignalStatus { NOT_RECEIVED, AVAILABLE, UNSUPPORTED, PERMISSION_DENIED, ERROR }
enum class SignalLifecycle { STOPPED, STARTED }

/** observedAtMillis is monotonic elapsed time, never a server/event wall-clock date. */
data class SignalReading<T>(
    val value: T? = null,
    val status: SignalStatus = SignalStatus.NOT_RECEIVED,
    val observedAtMillis: Long? = null,
    val sequence: Long = 0,
    val errorMessage: String? = null,
) {
    init {
        require((status == SignalStatus.AVAILABLE) == (value != null))
        require(observedAtMillis == null || observedAtMillis >= 0)
        require(sequence >= 0)
        require(status == SignalStatus.ERROR || errorMessage == null)
    }
}

/** Enum names verified against Vehicle 2.3.2. Interpretation belongs to each app. */
enum class CruiseControlState {
    OTHER, ENABLED, ACTIVATED, USER_OVERRIDE, SUSPENDED, FORCED_DEACTIVATION_WARNING,
}

data class GeoPoint(val latitude: Double, val longitude: Double) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
    }
}

/** heading retains the SDK value until its physical convention is verified. */
data class LocationObservation(
    val point: GeoPoint,
    val heading: Double? = null,
    val routeId: String? = null,
    val currentRouteIndex: Int? = null,
) {
    init { require(heading == null || heading.isFinite()) }
}

enum class TrafficCongestion { UNKNOWN, FAST, MODERATE, SLOW, HEAVY }

/** Raw navi-data fields: do not infer meters, seconds or current-position membership. */
data class TrafficSection(
    val offset: Double,
    val distance: Double,
    val trafficCongestionTime: Long,
    val congestion: TrafficCongestion,
    val averageSpeed: Double,
    val roadName: String,
    val startCrossName: String,
    val endCrossName: String,
) {
    init {
        require(offset.isFinite() && distance.isFinite() && averageSpeed.isFinite())
    }
}

/** Empty sections means a received empty list; NOT_RECEIVED is a separate reading status. */
data class RouteTraffic(
    val sections: List<TrafficSection>,
    // Raw SDK flag; its precise semantics are not assumed by the adapter.
    val isRouting: Boolean? = null,
)

/** Each field has independent availability, allowing unsupported signals on some vehicles. */
data class VehicleSnapshot(
    val lifecycle: SignalLifecycle = SignalLifecycle.STOPPED,
    val socPercent: SignalReading<Int> = SignalReading(),
    val chargePortOpen: SignalReading<Boolean> = SignalReading(),
    val chargeSwitchOn: SignalReading<Boolean> = SignalReading(),
    val cruiseState: SignalReading<CruiseControlState> = SignalReading(),
    val cruiseTargetSpeedKph: SignalReading<Float> = SignalReading(),
    val brakePressed: SignalReading<Boolean> = SignalReading(),
    val steeringAngle: SignalReading<Float> = SignalReading(),
    val vehicleSpeedKph: SignalReading<Float> = SignalReading(),
    val location: SignalReading<LocationObservation> = SignalReading(),
    val routeTraffic: SignalReading<RouteTraffic> = SignalReading(),
)

/** Explicit unit conversion for SDK adapters, not for already-normalized Fake values. */
object SignalUnits {
    fun metersPerSecondToKph(value: Float): Float {
        require(value.isFinite())
        return (value * 3.6f).also { require(it.isFinite()) }
    }
}
