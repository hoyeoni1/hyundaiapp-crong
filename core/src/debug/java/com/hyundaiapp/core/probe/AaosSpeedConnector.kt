package com.hyundaiapp.core.probe

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Build
import android.util.Log

/** All potentially blocking CarPropertyManager calls run off main. */
internal class AaosSpeedConnector(private val context: Context, private val timeoutMillis: Long) : SignalConnector {
    override val signals = setOf(VehicleSignal.VEHICLE_SPEED)
    @Volatile private var closing = false
    private val thread = HandlerThread("cruisebreaker-speed")
    private var worker: Handler? = null
    private var car: Car? = null
    private var manager: CarPropertyManager? = null
    private lateinit var sink: SignalSink
    private val firstValueTimeout = Runnable {
        if (!closing) sink.unavailable(SignalKeys.speed, SignalStatus.ERROR, "Car speed connection or initial value timed out")
    }

    private val callback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            if (closing || value.propertyId != VehiclePropertyIds.PERF_VEHICLE_SPEED) return
            worker?.removeCallbacks(firstValueTimeout)
            when (value.status) {
                CarPropertyValue.STATUS_AVAILABLE -> {
                    val raw = value.value as? Float
                    if (raw == null || !raw.isFinite()) sink.unavailable(SignalKeys.speed, SignalStatus.ERROR, "Invalid AAOS speed value")
                    else try { sink.value(SignalKeys.speed, SignalUnits.metersPerSecondToKph(raw)) }
                    catch (error: Exception) { sink.unavailable(SignalKeys.speed, SignalStatus.ERROR, error.message) }
                }
                CarPropertyValue.STATUS_UNAVAILABLE -> sink.unavailable(SignalKeys.speed, SignalStatus.NOT_RECEIVED)
                else -> sink.unavailable(SignalKeys.speed, SignalStatus.ERROR, "AAOS speed status ${value.status}")
            }
        }

        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            if (!closing && propertyId == VehiclePropertyIds.PERF_VEHICLE_SPEED)
                sink.unavailable(SignalKeys.speed, SignalStatus.ERROR, "AAOS speed callback failed")
        }
    }

    override fun start(sink: SignalSink) {
        this.sink = sink
        if (Build.VERSION.SDK_INT < 29 || !context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            sink.unavailable(SignalKeys.speed, SignalStatus.UNSUPPORTED)
            return
        }
        if (context.checkSelfPermission(SignalPermissions.SPEED) != PackageManager.PERMISSION_GRANTED) {
            sink.unavailable(SignalKeys.speed, SignalStatus.PERMISSION_DENIED)
            return
        }
        thread.start()
        val handler = Handler(thread.looper)
        worker = handler
        handler.postDelayed(firstValueTimeout, timeoutMillis)
        handler.post {
            if (closing) return@post
            try {
                car = Car.createCar(context, handler, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connected, ready ->
                    handler.post {
                        if (closing) return@post
                        if (!ready) {
                            manager = null
                            sink.unavailable(SignalKeys.speed, SignalStatus.ERROR, "Car service disconnected")
                        } else attach(connected)
                    }
                }
                if (car == null) sink.unavailable(SignalKeys.speed, SignalStatus.ERROR, "Car service is unavailable")
            } catch (error: Exception) { sink.unavailable(SignalKeys.speed, error.signalStatus(), error.message) }
        }
    }

    private fun attach(connected: Car) {
        try {
            manager?.let { runCatching { it.unregisterCallback(callback) } }
            val properties = connected.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
            manager = properties
            if (properties == null) {
                sink.unavailable(SignalKeys.speed, SignalStatus.ERROR, "Car property service is unavailable")
            } else if (properties.getCarPropertyConfig(VehiclePropertyIds.PERF_VEHICLE_SPEED) == null) {
                sink.unavailable(SignalKeys.speed, SignalStatus.UNSUPPORTED)
            } else if (!properties.registerCallback(callback, VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_FAST)) {
                val status = if (context.checkSelfPermission(SignalPermissions.SPEED) != PackageManager.PERMISSION_GRANTED)
                    SignalStatus.PERMISSION_DENIED else SignalStatus.ERROR
                sink.unavailable(SignalKeys.speed, status, "AAOS speed registration failed")
            }
            // Registration emits the initial current value; no main-thread synchronous getter.
        } catch (error: Exception) { sink.unavailable(SignalKeys.speed, error.signalStatus(), error.message) }
    }

    override fun stop() {
        closing = true
        worker?.post {
            worker?.removeCallbacks(firstValueTimeout)
            manager?.let { runCatching { it.unregisterCallback(callback) }.onFailure { Log.w("VehicleSignals", "Speed unregister failed", it) } }
            manager = null
            car?.let { runCatching { it.disconnect() }.onFailure { Log.w("VehicleSignals", "Car disconnect failed", it) } }
            car = null
            thread.quitSafely()
        }
    }
}
