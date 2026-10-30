package com.hyundaiapp.core.internal

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import com.hyundaiapp.core.DriveState
import com.hyundaiapp.core.SourceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * 차속: Pleos SDK에는 없고 AAOS 표준 `PERF_VEHICLE_SPEED`(m/s)로 받는다(findings 7.5).
 * `android.car.permission.CAR_SPEED`는 dangerous라 앱이 런타임 동의를 받아야 한다. 없으면 [SourceState.NO_PERMISSION].
 */
internal class AaosSpeedSource(
    private val context: Context,
    private val handler: Handler,
    private val drive: MutableStateFlow<DriveState>,
    private val setState: (SourceState) -> Unit,
    private val error: (String) -> Unit,
) {
    private var car: Car? = null
    private var manager: CarPropertyManager? = null

    private val callback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            val mps = value.value as? Float ?: return
            drive.update { it.copy(speedKph = mps.mpsToKph()) }
        }

        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            error("차속 콜백 오류(area=$areaId)")
        }
    }

    fun start() {
        if (context.checkSelfPermission(Car.PERMISSION_SPEED) != PackageManager.PERMISSION_GRANTED) {
            setState(SourceState.NO_PERMISSION)
            return
        }
        setState(SourceState.CONNECTING)
        // 메인 스레드를 막지 않는 비동기 연결
        car = Car.createCar(context, handler, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { c, ready ->
            if (ready) register(c) else {
                manager = null
                setState(SourceState.CONNECTING)
            }
        }
    }

    private fun register(c: Car) {
        runCatching {
            val m = c.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            manager = m
            check(m.registerCallback(callback, VehiclePropertyIds.PERF_VEHICLE_SPEED, SAMPLE_RATE_HZ)) {
                "registerCallback false"
            }
        }.onSuccess {
            Logs.i("speed ready")
            setState(SourceState.READY)
        }.onFailure {
            error("차속 연결 실패: ${it.message}")
            setState(SourceState.FAILED)
        }
    }

    fun stop() {
        runCatching { manager?.unregisterCallback(callback) }
        runCatching { car?.disconnect() }
        manager = null
        car = null
    }

    private companion object {
        // 에뮬레이터는 7~20Hz로 준다. 앱 판정(초 단위 평균)에는 5Hz면 충분
        const val SAMPLE_RATE_HZ = 5f
    }
}
