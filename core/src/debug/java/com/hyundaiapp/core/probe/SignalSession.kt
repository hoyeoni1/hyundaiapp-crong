package com.hyundaiapp.core.probe

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class SignalKey<T>(
    val id: VehicleSignal,
    val read: (VehicleSnapshot) -> SignalReading<T>,
    val write: (VehicleSnapshot, SignalReading<T>) -> VehicleSnapshot,
    val validate: (T) -> Unit = {},
)

internal object SignalKeys {
    val soc = SignalKey(VehicleSignal.SOC, VehicleSnapshot::socPercent, { s, v -> s.copy(socPercent = v) }) { require(it in 0..100) }
    val port = SignalKey(VehicleSignal.CHARGE_PORT, VehicleSnapshot::chargePortOpen, { s, v -> s.copy(chargePortOpen = v) })
    val chargeSwitch = SignalKey(VehicleSignal.CHARGE_SWITCH, VehicleSnapshot::chargeSwitchOn, { s, v -> s.copy(chargeSwitchOn = v) })
    val cruise = SignalKey(VehicleSignal.CRUISE_STATE, VehicleSnapshot::cruiseState, { s, v -> s.copy(cruiseState = v) })
    val targetSpeed = SignalKey(VehicleSignal.CRUISE_TARGET_SPEED, VehicleSnapshot::cruiseTargetSpeedKph, { s, v -> s.copy(cruiseTargetSpeedKph = v) }) { require(it.isFinite() && it >= 0f) }
    val brake = SignalKey(VehicleSignal.BRAKE, VehicleSnapshot::brakePressed, { s, v -> s.copy(brakePressed = v) })
    val steering = SignalKey(VehicleSignal.STEERING, VehicleSnapshot::steeringAngle, { s, v -> s.copy(steeringAngle = v) }) { require(it.isFinite()) }
    val speed = SignalKey(VehicleSignal.VEHICLE_SPEED, VehicleSnapshot::vehicleSpeedKph, { s, v -> s.copy(vehicleSpeedKph = v) }) { require(it.isFinite()) }
    val location = SignalKey(VehicleSignal.LOCATION, VehicleSnapshot::location, { s, v -> s.copy(location = v) })
    val traffic = SignalKey(VehicleSignal.ROUTE_TRAFFIC, VehicleSnapshot::routeTraffic, { s, v -> s.copy(routeTraffic = v) })
    val all = listOf(soc, port, chargeSwitch, cruise, targetSpeed, brake, steering, speed, location, traffic)
}

internal interface SignalConnector {
    val signals: Set<VehicleSignal>
    fun start(sink: SignalSink)
    fun stop()
}

/** A sink belongs to one connection generation; old callbacks cannot update a newer session. */
internal class SignalSink(
    private val owner: ManagedVehicleSignals,
    private val generation: Long,
) {
    fun <T> value(key: SignalKey<T>, value: T?) {
        try {
            if (value != null) key.validate(value)
            owner.publish(generation, key, value, if (value == null) SignalStatus.NOT_RECEIVED else SignalStatus.AVAILABLE)
        } catch (error: IllegalArgumentException) {
            unavailable(key, SignalStatus.ERROR, "Invalid ${key.id} value: ${error.message}")
        }
    }

    fun <T> unavailable(key: SignalKey<T>, status: SignalStatus, message: String? = null) {
        require(status != SignalStatus.AVAILABLE)
        owner.publish(generation, key, null, status, message)
    }

    fun fail(signals: Set<VehicleSignal>, status: SignalStatus, message: String? = null) {
        SignalKeys.all.filter { it.id in signals }.forEach { unavailable(it, status, message) }
    }
}

internal class ManagedVehicleSignals(
    private val connectorFactory: () -> List<SignalConnector>,
    private val clockMillis: () -> Long,
) : VehicleSignals {
    private val lock = Any()
    private val mutableSnapshot = MutableStateFlow(VehicleSnapshot())
    override val snapshot = mutableSnapshot.asStateFlow()
    private var generation = 0L
    private var connectors = emptyList<SignalConnector>()

    override fun start() = synchronized(lock) {
        if (mutableSnapshot.value.lifecycle == SignalLifecycle.STARTED) return@synchronized
        generation++
        mutableSnapshot.value = VehicleSnapshot(lifecycle = SignalLifecycle.STARTED)
        val sink = SignalSink(this, generation)
        connectors = try { connectorFactory() } catch (error: Exception) {
            sink.fail(VehicleSignal.entries.toSet(), error.signalStatus(), error.message)
            return@synchronized
        }
        connectors.forEach { connector ->
            try {
                connector.start(sink)
            } catch (error: Exception) {
                sink.fail(connector.signals, if (error is SecurityException) SignalStatus.PERMISSION_DENIED else SignalStatus.ERROR, error.message)
            }
        }
    }

    override fun stop() = synchronized(lock) {
        if (mutableSnapshot.value.lifecycle == SignalLifecycle.STOPPED) return@synchronized
        generation++
        mutableSnapshot.value = VehicleSnapshot()
        connectors.forEach { runCatching { it.stop() } }
        connectors = emptyList()
    }

    internal fun <T> publish(generation: Long, key: SignalKey<T>, value: T?, status: SignalStatus, message: String? = null) = synchronized(lock) {
        if (generation != this.generation || mutableSnapshot.value.lifecycle != SignalLifecycle.STARTED) return@synchronized
        val current = mutableSnapshot.value
        val reading = SignalReading(value, status, clockMillis(), key.read(current).sequence + 1, message.takeIf { status == SignalStatus.ERROR })
        mutableSnapshot.value = key.write(current, reading)
    }
}
