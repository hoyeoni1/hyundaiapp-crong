package com.hyundaiapp.core

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig
import ai.pleos.playground.vehicle.constant.control.CruiseControl
import ai.pleos.playground.vehicle.listener.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.TimeoutException

/** SDK operations and bookkeeping are serialized on main, callbacks are generation-guarded. */
internal class VehicleSdkConnector(
    private val context: Context,
    private val options: PleosSignalOptions,
) : SignalConnector {
    override val signals = options.signals.intersect(energySignals + driveSignals)
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var closing = false
    private var cleanupStarted = false
    private lateinit var sink: SignalSink
    private var vehicle: Vehicle? = null
    private val unregister = mutableListOf<() -> Unit>()
    private val polls = mutableListOf<Runnable>()
    private val pending = mutableMapOf<Long, Runnable>()
    private var requestId = 0L

    override fun start(sink: SignalSink) {
        this.sink = sink
        main.post {
            if (closing) return@post
            try {
                val sdk = Vehicle(context)
                vehicle = sdk
                sdk.initialize()
                bindSignals(sdk)
            } catch (error: Exception) {
                sink.fail(signals, error.signalStatus(), error.message)
            }
        }
    }

    private fun bindSignals(sdk: Vehicle) {
        val battery by lazy { sdk.getEvBattery() }
        val cruise by lazy { sdk.getDrivingMode() }
        val brake by lazy { sdk.getBrake() }
        val steering by lazy { sdk.getSteeringWheel() }

        if (VehicleSignal.SOC in signals) bind<Int, Int>(SignalKeys.soc, SignalPermissions.ENERGY,
            { ok, fail -> battery.checkEvBatteryStateOfChargeCapabilities(ok, fail) },
            { ok, fail -> battery.getEvBatteryStateOfCharge(ok, fail) }, { it },
            { emit, fail ->
                val listener = object : EvBatteryStateOfChargeListener {
                    override fun onEvBatteryStateOfChargeUpdated(value: Int?) = emit(value)
                    override fun onFailed(exception: Exception) = fail(exception)
                }
                battery.registerEvBatteryStateOfCharge(listener)
                ({ battery.unregisterEvBatteryStateOfCharge(listener) })
            })

        if (VehicleSignal.CHARGE_PORT in signals) bind<Boolean, Boolean>(SignalKeys.port, SignalPermissions.ENERGY,
            { ok, fail -> battery.checkEvChargePortOpenCapabilities(ok, fail) },
            { ok, fail -> battery.isEvChargePortOpen(ok, fail) }, { it },
            { emit, fail ->
                val listener = object : EvChargePortOpenListener {
                    override fun onEvChargePortOpenUpdated(value: Boolean?) = emit(value)
                    override fun onFailed(exception: Exception) = fail(exception)
                }
                battery.registerEvChargePortOpen(listener)
                ({ battery.unregisterEvChargePortOpen(listener) })
            })

        if (VehicleSignal.CHARGE_SWITCH in signals) bind<Boolean, Boolean>(SignalKeys.chargeSwitch, SignalPermissions.ENERGY,
            { ok, fail -> battery.checkEvChargeSwitchCapabilities(ok, fail) },
            { ok, fail -> battery.getEvChargeSwitchState(ok, fail) }, { it },
            pollMillis = options.chargeSwitchPollMillis)

        if (VehicleSignal.CRUISE_STATE in signals) bind<CruiseControl, CruiseControlState>(SignalKeys.cruise, SignalPermissions.DRIVE,
            { ok, fail -> cruise.checkCruiseControlStateCapabilities(ok, fail) },
            { ok, fail -> cruise.getCruiseControlState(ok, fail) }, { it.toCore() },
            { emit, fail ->
                val listener = object : CruiseControlStateListener {
                    override fun onCruiseControlStateUpdated(value: CruiseControl?) = emit(value)
                    override fun onFailed(exception: Exception) = fail(exception)
                }
                cruise.registerCruiseControlState(listener)
                ({ cruise.unregisterCruiseControlState(listener) })
            })

        if (VehicleSignal.CRUISE_TARGET_SPEED in signals) bind<Float, Float>(SignalKeys.targetSpeed, SignalPermissions.DRIVE,
            { ok, fail -> cruise.checkCruiseControlTargetSpeedCapabilities(ok, fail) },
            { ok, fail -> cruise.getCruiseControlTargetSpeed(ok, fail) }, SignalUnits::metersPerSecondToKph,
            { emit, fail ->
                val listener = object : CruiseControlTargetSpeedListener {
                    override fun onCruiseControlTargetSpeedUpdated(value: Float?) = emit(value)
                    override fun onFailed(exception: Exception) = fail(exception)
                }
                cruise.registerCruiseControlTargetSpeed(listener)
                ({ cruise.unregisterCruiseControlTargetSpeed(listener) })
            })

        if (VehicleSignal.BRAKE in signals) bind<Boolean, Boolean>(SignalKeys.brake, SignalPermissions.DRIVE,
            { ok, fail -> brake.checkBrakePedalStatusCapabilities(ok, fail) },
            { ok, fail -> brake.getCurrentBrakePedalStatus(ok, fail) }, { it },
            { emit, fail ->
                val listener = object : BrakePedalStatusListener {
                    override fun onBrakePedalStatusUpdated(value: Boolean?) = emit(value)
                    override fun onFailed(exception: Exception) = fail(exception)
                }
                brake.registerBrakePedalStatus(listener)
                ({ brake.unregisterBrakePedalStatus(listener) })
            })

        if (VehicleSignal.STEERING in signals) bind<Float, Float>(SignalKeys.steering, SignalPermissions.DRIVE,
            { ok, fail -> steering.checkSteeringWheelAngleCapabilities({ config, _ -> ok(config) }, fail) },
            { ok, fail -> steering.getCurrentSteeringWheelAngle(ok, fail) }, { it },
            { emit, fail ->
                val listener = object : SteeringWheelAngleListener {
                    override fun onSteeringWheelAngleUpdated(value: Float?) = emit(value)
                    override fun onFailed(exception: Exception) = fail(exception)
                }
                steering.registerSteeringWheelAngle(listener)
                ({ steering.unregisterSteeringWheelAngle(listener) })
            })
    }

    private fun <R, T> bind(
        key: SignalKey<T>, permission: String,
        capability: ((CapabilityConfig?) -> Unit, (Exception) -> Unit) -> Unit,
        get: ((R?) -> Unit, (Exception) -> Unit) -> Unit,
        convert: (R) -> T,
        register: (((R?) -> Unit, (Exception) -> Unit) -> (() -> Unit))? = null,
        pollMillis: Long? = null,
    ) {
        if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            sink.unavailable(key, SignalStatus.PERMISSION_DENIED)
            return
        }
        request(key, capability, { config ->
            if (config == null || config.accessType !in setOf(AccessType.READ, AccessType.READ_WRITE)) {
                sink.unavailable(key, SignalStatus.UNSUPPORTED)
                return@request
            }
            var revision = 0L
            val emit: (R?) -> Unit = { raw ->
                try { sink.value(key, raw?.let(convert)) }
                catch (error: Exception) { report(key, error) }
            }
            try {
                register?.let { subscribe ->
                    unregister += subscribe({ raw -> main.post { if (!closing) { revision++; emit(raw) } } },
                        { error -> main.post { if (!closing) { revision++; report(key, error) } } })
                }
                var readPending = false
                fun read() {
                    if (readPending) return
                    readPending = true
                    val beforeRead = revision
                    request(key, get, { raw ->
                        readPending = false
                        if (revision == beforeRead) emit(raw)
                    }, { error ->
                        readPending = false
                        if (revision == beforeRead) report(key, error)
                    })
                }
                read()
                if (pollMillis != null) {
                    val poll = object : Runnable {
                        override fun run() {
                            if (closing) return
                            read()
                            main.postDelayed(this, pollMillis)
                        }
                    }
                    polls += poll
                    main.postDelayed(poll, pollMillis)
                }
            } catch (error: Exception) { report(key, error) }
        })
    }

    private fun <R> request(
        key: SignalKey<*>,
        call: ((R?) -> Unit, (Exception) -> Unit) -> Unit,
        success: (R?) -> Unit,
        failure: (Exception) -> Unit = { report(key, it) },
    ) {
        if (closing) return
        val id = ++requestId
        val timeout = Runnable {
            if (pending.remove(id) != null) {
                if (!closing) failure(TimeoutException("${key.id} SDK request timed out"))
                releaseWhenDrained()
            }
        }
        pending[id] = timeout
        main.postDelayed(timeout, options.requestTimeoutMillis)
        fun finish(action: () -> Unit) {
            main.post {
                pending.remove(id)?.let {
                    main.removeCallbacks(it)
                    if (!closing) action()
                    releaseWhenDrained()
                }
            }
        }
        try { call({ value -> finish { success(value) } }, { error -> finish { failure(error) } }) }
        catch (error: Exception) { finish { failure(error) } }
    }

    private fun report(key: SignalKey<*>, error: Exception) = sink.unavailable(key, error.signalStatus(), error.message)

    override fun stop() {
        closing = true
        main.post {
            cleanupStarted = true
            polls.forEach(main::removeCallbacks)
            polls.clear()
            unregister.forEach { cleanup -> runCatching(cleanup).onFailure { Log.w("VehicleSignals", "Unregister failed", it) } }
            unregister.clear()
            releaseWhenDrained()
        }
    }

    private fun releaseWhenDrained() {
        if (closing && cleanupStarted && pending.isEmpty()) {
            vehicle?.let { runCatching { it.release() }.onFailure { error -> Log.w("VehicleSignals", "Release failed", error) } }
            vehicle = null
        }
    }
}

internal fun Throwable.signalStatus(): SignalStatus =
    if (generateSequence(this) { it.cause }.take(16).any { it is SecurityException }) SignalStatus.PERMISSION_DENIED
    else if (this is UnsupportedOperationException) SignalStatus.UNSUPPORTED
    else SignalStatus.ERROR
