package com.hyundaiapp.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** 신호 모니터(디버그) 화면용: 모든 신호를 사람이 읽는 줄 목록으로 바꾼다. */
fun VehicleSignals.debugLines(): Flow<List<String>> =
    combine(sources, evBattery, drive, cruise, combine(location, route, ::Pair)) { src, ev, dr, cr, (loc, rt) ->
        buildList {
            add("소스  vehicle=${src.vehicle}  speed=${src.speed}  navi=${src.navi}")
            src.lastError?.let { add("최근 오류  $it") }
            add("EV  SoC=${ev.socPercent ?: "-"}%  충전구=${ev.chargePortOpen.mark()}  충전스위치=${ev.chargeSwitchOn.mark()}  용량=${ev.capacityKwh ?: "-"}kWh")
            add("주행  기어=${dr.gear ?: "-"}  차속=${dr.speedKph?.let { "%.1f".format(it) } ?: "-"}km/h  브레이크=${dr.brakePressed.mark()}  조향=${dr.steeringAngleDeg ?: "-"}°")
            add("크루즈  상태=${cr.mode ?: "-"}  설정속도=${cr.targetSpeedKph?.let { "%.0f".format(it) } ?: "-"}km/h")
            add(
                "위치  " + (
                    loc?.let { "%.5f, %.5f  방향 %.0f°  %s".format(it.point.latitude, it.point.longitude, it.headingDeg, it.address.orEmpty()) }
                        ?: "-"
                    ),
            )
            add("경로  안내중=${rt.guiding}  혼잡구간=${rt.congestion.size}개")
            rt.congestion.take(3).forEach {
                add("   · ${it.roadName} ${it.level} ${"%.0f".format(it.offsetM)}m부터 ${"%.0f".format(it.lengthM)}m, 평균 ${"%.0f".format(it.avgSpeedKph)}km/h")
            }
        }
    }

private fun Boolean?.mark() = when (this) {
    true -> "켜짐"
    false -> "꺼짐"
    null -> "-"
}
