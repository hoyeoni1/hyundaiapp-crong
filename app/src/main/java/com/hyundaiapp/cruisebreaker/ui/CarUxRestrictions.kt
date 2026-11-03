package com.hyundaiapp.cruisebreaker.ui

import android.car.Car
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** UI 사용 제한만 읽는다. 동의 전 차량·위치 기록 파이프라인은 시작하지 않는다. null은 미확인. */
@Composable
internal fun rememberCarUxRestricted(): Boolean? {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var restricted by remember { mutableStateOf<Boolean?>(null) }
    DisposableEffect(context, lifecycleOwner) {
        val source = CarUxRestrictionSource(context) { restricted = it }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> source.start()
                Lifecycle.Event.ON_STOP -> source.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) source.start()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            source.stop()
        }
    }
    return restricted
}

/** minSdk 28에서도 연결 대기로 UI를 막지 않도록 비동기 ServiceConnection을 쓴다. */
@Suppress("DEPRECATION")
private class CarUxRestrictionSource(private val context: Context, private val onChanged: (Boolean?) -> Unit) {
    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null
    private var started = false
    private var connected = false
    private var generation = 0
    private var connectionGeneration = 0

    fun start() {
        if (started) return
        started = true
        val current = ++generation
        onChanged(null)
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) return
        try {
            car = Car.createCar(context, object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    if (current != generation) return
                    val currentConnection = ++connectionGeneration
                    try {
                        connected = true
                        runCatching { manager?.unregisterListener() }
                        val restrictions = car?.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager
                        manager = restrictions
                        restrictions?.registerListener { value ->
                            if (current == generation && currentConnection == connectionGeneration && connected) {
                                onChanged(value.isRequiresDistractionOptimization)
                            }
                        }
                        onChanged(restrictions?.currentCarUxRestrictions?.isRequiresDistractionOptimization)
                    } catch (_: Exception) {
                        connected = false
                        connectionGeneration++
                        runCatching { manager?.unregisterListener() }
                        manager = null
                        onChanged(null)
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    if (current == generation) {
                        connected = false
                        connectionGeneration++
                        runCatching { manager?.unregisterListener() }
                        manager = null
                        onChanged(null)
                    }
                }
            })
            car?.connect()
        } catch (_: Exception) {
            stop()
        }
    }

    fun stop() {
        generation++
        connectionGeneration++
        started = false
        connected = false
        runCatching { manager?.unregisterListener() }
        manager = null
        runCatching { car?.disconnect() }
        car = null
        onChanged(null)
    }
}
