package com.hyundaiapp.core.internal

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.control.CruiseControl
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.CruiseControlStateListener
import ai.pleos.playground.vehicle.listener.CruiseControlTargetSpeedListener
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import ai.pleos.playground.vehicle.listener.EvChargePortOpenListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.SteeringWheelAngleListener
import android.content.Context
import android.os.Handler
import com.hyundaiapp.core.CoreFeature
import com.hyundaiapp.core.CruiseMode
import com.hyundaiapp.core.CruiseState
import com.hyundaiapp.core.DriveState
import com.hyundaiapp.core.EvBatteryState
import com.hyundaiapp.core.Gear
import com.hyundaiapp.core.SourceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Pleos Vehicle SDK → 상태 Flow.
 *
 * `Vehicle.initialize()` 직후에는 서비스 연결 전이라 호출이 실패할 수 있다. 그래서 묶음마다 조회를 한 번 해 보고,
 * 성공하면 그때 리스너를 등록한다(실패하면 1초 뒤 다시, 최대 [MAX_ATTEMPTS]번).
 */
internal class VehicleSdkSource(
    private val context: Context,
    private val features: Set<CoreFeature>,
    private val handler: Handler,
    private val ev: MutableStateFlow<EvBatteryState>,
    private val drive: MutableStateFlow<DriveState>,
    private val cruise: MutableStateFlow<CruiseState>,
    private val setState: (SourceState) -> Unit,
    private val error: (String) -> Unit,
) {
    private var vehicle: Vehicle? = null
    private val pending = mutableSetOf<CoreFeature>()
    private val unregister = mutableListOf<() -> Unit>()
    @Volatile private var running = false

    fun start() {
        val wanted = features.intersect(setOf(CoreFeature.EV_BATTERY, CoreFeature.DRIVE))
        if (wanted.isEmpty()) return
        running = true
        setState(SourceState.CONNECTING)
        val v = Vehicle(context).also { it.initialize() }
        vehicle = v
        pending += wanted
        wanted.forEach { connect(v, it, attempt = 1) }
    }

    fun stop() {
        running = false
        handler.removeCallbacksAndMessages(TOKEN)
        unregister.forEach { runCatching(it) }
        unregister.clear()
        pending.clear()
        runCatching { vehicle?.release() }
        vehicle = null
    }

    // 조회 한 번으로 연결 여부를 확인한 뒤 리스너를 건다
    private fun connect(v: Vehicle, feature: CoreFeature, attempt: Int) {
        val onOk: () -> Unit = { post { if (running) onConnected(v, feature) } }
        val onErr: (Exception) -> Unit = { e ->
            post {
                if (!running) return@post
                if (attempt >= MAX_ATTEMPTS) {
                    error("$feature 연결 실패: ${e.message}")
                    setState(SourceState.FAILED)
                } else {
                    handler.postDelayed({ connect(v, feature, attempt + 1) }, TOKEN, RETRY_MS)
                }
            }
        }
        runCatching {
            when (feature) {
                CoreFeature.EV_BATTERY -> v.getEvBattery().getEvBatteryStateOfCharge({ soc ->
                    ev.update { it.copy(socPercent = soc) }
                    onOk()
                }, onErr)
                CoreFeature.DRIVE -> v.getGear().getCurrentGearSelectionState({ g ->
                    drive.update { it.copy(gear = g.toGear()) }
                    onOk()
                }, onErr)
                else -> Unit
            }
        }.onFailure { onErr(it as? Exception ?: Exception(it)) }
    }

    private fun onConnected(v: Vehicle, feature: CoreFeature) {
        if (!pending.remove(feature)) return
        when (feature) {
            CoreFeature.EV_BATTERY -> subscribeEv(v)
            CoreFeature.DRIVE -> subscribeDrive(v)
            else -> Unit
        }
        Logs.i("vehicle $feature ready")
        if (pending.isEmpty()) setState(SourceState.READY)
    }

    private fun subscribeEv(v: Vehicle) {
        val battery = v.getEvBattery()
        val soc = object : EvBatteryStateOfChargeListener {
            override fun onEvBatteryStateOfChargeUpdated(value: Int?) {
                ev.update { it.copy(socPercent = value) }
            }
            override fun onFailed(e: Exception) = fail("SoC", e)
        }
        val port = object : EvChargePortOpenListener {
            override fun onEvChargePortOpenUpdated(value: Boolean?) {
                ev.update { it.copy(chargePortOpen = value) }
                // 충전구가 열리면 충전 스위치를 바로 한 번 조회
                if (value == true) post { pollChargeSwitch(v) }
            }
            override fun onFailed(e: Exception) = fail("충전구", e)
        }
        battery.registerEvBatteryStateOfCharge(soc)
        battery.registerEvChargePortOpen(port)
        unregister += { battery.unregisterEvBatteryStateOfCharge(soc) }
        unregister += { battery.unregisterEvChargePortOpen(port) }

        battery.isEvChargePortOpen({ open -> ev.update { it.copy(chargePortOpen = open) } }, { fail("충전구 조회", it) })
        // 연결 확인 조회와 리스너 등록 사이에 바뀐 SoC(시연 스크립트가 앱 시작 직후 넣는 값)를 놓치지 않게 한 번 더 조회한다
        battery.getEvBatteryStateOfCharge({ soc -> ev.update { it.copy(socPercent = soc) } }, { fail("SoC 조회", it) })
        // getEvBatteryCapacity requires CAR_INFO, which neither host app declares.
        // Keep capacity unavailable so the app's selected vehicle preset remains the fallback.
        ev.update { it.copy(capacityKwh = null) }
        pollChargeSwitch(v)
    }

    // 충전 스위치는 리스너가 없다 → 충전구가 열려 있는 동안 주기 조회
    private fun pollChargeSwitch(v: Vehicle) {
        if (!running) return
        handler.removeCallbacksAndMessages(SWITCH_TOKEN)
        v.getEvBattery().getEvChargeSwitchState({ on -> ev.update { it.copy(chargeSwitchOn = on) } }, { fail("충전 스위치", it) })
        if (ev.value.chargePortOpen == true) {
            handler.postDelayed({ pollChargeSwitch(v) }, SWITCH_TOKEN, CHARGE_SWITCH_POLL_MS)
        }
    }

    private fun subscribeDrive(v: Vehicle) {
        val gear = object : GearSelectionListener {
            override fun onSelected(value: VehicleGear?) {
                drive.update { it.copy(gear = value.toGear()) }
            }
            override fun onFailed(e: Exception) = fail("기어", e)
        }
        val brake = object : BrakePedalStatusListener {
            override fun onBrakePedalStatusUpdated(value: Boolean?) {
                drive.update { it.copy(brakePressed = value) }
            }
            override fun onFailed(e: Exception) = fail("브레이크", e)
        }
        val steering = object : SteeringWheelAngleListener {
            override fun onSteeringWheelAngleUpdated(value: Float?) {
                drive.update { it.copy(steeringAngleDeg = value) }
            }
            override fun onFailed(e: Exception) = fail("조향각", e)
        }
        val cruiseState = object : CruiseControlStateListener {
            override fun onCruiseControlStateUpdated(value: CruiseControl?) {
                cruise.update { it.copy(mode = value.toMode()) }
            }
            override fun onFailed(e: Exception) = fail("크루즈 상태", e)
        }
        val cruiseSpeed = object : CruiseControlTargetSpeedListener {
            override fun onCruiseControlTargetSpeedUpdated(value: Float?) {
                cruise.update { it.copy(targetSpeedKph = value?.mpsToKph()) }
            }
            override fun onFailed(e: Exception) = fail("크루즈 속도", e)
        }
        v.getGear().registerGearSelection(gear)
        v.getBrake().registerBrakePedalStatus(brake)
        v.getSteeringWheel().registerSteeringWheelAngle(steering)
        v.getDrivingMode().registerCruiseControlState(cruiseState)
        v.getDrivingMode().registerCruiseControlTargetSpeed(cruiseSpeed)
        unregister += { v.getGear().unregisterGearSelection(gear) }
        unregister += { v.getBrake().unregisterBrakePedalStatus(brake) }
        unregister += { v.getSteeringWheel().unregisterSteeringWheelAngle(steering) }
        unregister += { v.getDrivingMode().unregisterCruiseControlState(cruiseState) }
        unregister += { v.getDrivingMode().unregisterCruiseControlTargetSpeed(cruiseSpeed) }

        // 리스너는 값이 바뀔 때만 오므로 현재 값을 한 번 채운다
        v.getBrake().getCurrentBrakePedalStatus({ b -> drive.update { it.copy(brakePressed = b) } }, { fail("브레이크 조회", it) })
        v.getSteeringWheel().getCurrentSteeringWheelAngle({ a -> drive.update { it.copy(steeringAngleDeg = a) } }, { fail("조향각 조회", it) })
        v.getDrivingMode().getCruiseControlState({ c -> cruise.update { it.copy(mode = c.toMode()) } }, { fail("크루즈 조회", it) })
        v.getDrivingMode().getCruiseControlTargetSpeed({ s -> cruise.update { it.copy(targetSpeedKph = s?.mpsToKph()) } }, { fail("크루즈 속도 조회", it) })
    }

    private fun fail(what: String, e: Exception?) {
        Logs.w("vehicle $what: ${e?.message}")
        error("$what: ${e?.message}")
    }

    private fun post(block: () -> Unit) {
        handler.post(block)
    }

    private companion object {
        val TOKEN = Any()
        val SWITCH_TOKEN = Any()
        const val MAX_ATTEMPTS = 15
        const val RETRY_MS = 1_000L
        const val CHARGE_SWITCH_POLL_MS = 10_000L
    }
}

internal fun VehicleGear?.toGear(): Gear? = when (this) {
    null -> null
    VehicleGear.GEAR_PARK -> Gear.PARK
    VehicleGear.GEAR_REVERSE -> Gear.REVERSE
    VehicleGear.GEAR_NEUTRAL -> Gear.NEUTRAL
    VehicleGear.GEAR_UNKNOWN -> Gear.UNKNOWN
    // DRIVE와 수동 단수(1~9)는 모두 전진
    else -> Gear.DRIVE
}

internal fun CruiseControl?.toMode(): CruiseMode? =
    this?.let { c -> CruiseMode.entries.firstOrNull { it.name == c.name } ?: CruiseMode.OTHER }

internal fun Float.mpsToKph(): Float = this * 3.6f
