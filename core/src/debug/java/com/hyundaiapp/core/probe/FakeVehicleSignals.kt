package com.hyundaiapp.core.probe

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manually driven fake; no SDK connection, background task or automatic road simulation.
 * Input is accepted only while started. clockMillis must return monotonic elapsed milliseconds.
 */
class FakeVehicleSignals(
    private val clockMillis: () -> Long = elapsedClock(),
) : VehicleSignals {
    private val mutableSnapshot = MutableStateFlow(VehicleSnapshot())
    override val snapshot = mutableSnapshot.asStateFlow()

    override fun start() {
        mutableSnapshot.update { current ->
            if (current.lifecycle == SignalLifecycle.STARTED) current
            else VehicleSnapshot(lifecycle = SignalLifecycle.STARTED)
        }
    }

    override fun stop() {
        // Clear observations so a new consumer cannot treat a stopped connection as live data.
        mutableSnapshot.value = VehicleSnapshot()
    }

    fun setSocPercent(value: Int) {
        require(value in 0..100)
        publish { copy(socPercent = reading(value, socPercent)) }
    }

    fun setChargePortOpen(value: Boolean) = publish {
        copy(chargePortOpen = reading(value, chargePortOpen))
    }

    fun setChargeSwitchOn(value: Boolean) = publish {
        copy(chargeSwitchOn = reading(value, chargeSwitchOn))
    }

    fun setCruiseState(value: CruiseControlState) = publish {
        copy(cruiseState = reading(value, cruiseState))
    }

    fun setCruiseTargetSpeedKph(value: Float) {
        require(value.isFinite() && value >= 0f)
        publish { copy(cruiseTargetSpeedKph = reading(value, cruiseTargetSpeedKph)) }
    }

    fun setBrakePressed(value: Boolean) = publish {
        copy(brakePressed = reading(value, brakePressed))
    }

    fun setSteeringAngle(value: Float) {
        require(value.isFinite())
        publish { copy(steeringAngle = reading(value, steeringAngle)) }
    }

    fun setVehicleSpeedKph(value: Float) {
        // Signed speed is retained for reverse movement; app rules decide whether to use it.
        require(value.isFinite())
        publish { copy(vehicleSpeedKph = reading(value, vehicleSpeedKph)) }
    }

    fun setLocation(value: LocationObservation) = publish {
        copy(location = reading(value, location))
    }

    fun setRouteTraffic(value: RouteTraffic) {
        val frozen = value.copy(sections = value.sections.toList())
        publish { copy(routeTraffic = reading(frozen, routeTraffic)) }
    }

    /** Mark one capability unavailable. This is not a cruise deactivation or a false pedal value. */
    fun setUnavailable(signal: VehicleSignal, status: SignalStatus, message: String? = null) {
        require(status != SignalStatus.AVAILABLE)
        require(status == SignalStatus.ERROR || message == null)
        publish {
            when (signal) {
                VehicleSignal.SOC -> copy(socPercent = unavailable(socPercent, status, message))
                VehicleSignal.CHARGE_PORT -> copy(chargePortOpen = unavailable(chargePortOpen, status, message))
                VehicleSignal.CHARGE_SWITCH -> copy(chargeSwitchOn = unavailable(chargeSwitchOn, status, message))
                VehicleSignal.CRUISE_STATE -> copy(cruiseState = unavailable(cruiseState, status, message))
                VehicleSignal.CRUISE_TARGET_SPEED -> copy(cruiseTargetSpeedKph = unavailable(cruiseTargetSpeedKph, status, message))
                VehicleSignal.BRAKE -> copy(brakePressed = unavailable(brakePressed, status, message))
                VehicleSignal.STEERING -> copy(steeringAngle = unavailable(steeringAngle, status, message))
                VehicleSignal.VEHICLE_SPEED -> copy(vehicleSpeedKph = unavailable(vehicleSpeedKph, status, message))
                VehicleSignal.LOCATION -> copy(location = unavailable(location, status, message))
                VehicleSignal.ROUTE_TRAFFIC -> copy(routeTraffic = unavailable(routeTraffic, status, message))
            }
        }
    }

    private fun publish(transform: VehicleSnapshot.() -> VehicleSnapshot) {
        mutableSnapshot.update { current ->
            check(current.lifecycle == SignalLifecycle.STARTED) { "Call start() before publishing fake signals" }
            current.transform()
        }
    }

    private fun <T> reading(value: T, previous: SignalReading<T>): SignalReading<T> =
        SignalReading(value, SignalStatus.AVAILABLE, clockMillis(), previous.sequence + 1)

    private fun <T> unavailable(previous: SignalReading<T>, status: SignalStatus, message: String?): SignalReading<T> =
        SignalReading(status = status, observedAtMillis = clockMillis(), sequence = previous.sequence + 1, errorMessage = message)
}

private fun elapsedClock(): () -> Long {
    val origin = System.nanoTime()
    return { (System.nanoTime() - origin) / 1_000_000L }
}

enum class VehicleSignal {
    SOC, CHARGE_PORT, CHARGE_SWITCH, CRUISE_STATE, CRUISE_TARGET_SPEED,
    BRAKE, STEERING, VEHICLE_SPEED, LOCATION, ROUTE_TRAFFIC,
}
