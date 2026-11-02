package com.hyundaiapp.core

import kotlinx.coroutines.flow.StateFlow

/**
 * 두 앱이 공통으로 쓰는 차량·내비 신호. 앱은 이 인터페이스만 보고, SDK는 직접 부르지 않는다.
 *
 * - 값을 아직 못 받았거나 해당 [CoreFeature]를 켜지 않았으면 null.
 * - 단위는 이름에 붙인다(Kph, Deg, M, Kwh). SDK 원래 단위(m/s, Wh)는 구현에서 바꾼다.
 * - 근거 실측: docs/vehicle-data-findings.md 2·3·6·7장.
 */
interface VehicleSignals {
    val evBattery: StateFlow<EvBatteryState>
    val drive: StateFlow<DriveState>
    val cruise: StateFlow<CruiseState>
    val location: StateFlow<LocationState?>
    val route: StateFlow<RouteState>

    /** 각 데이터 소스의 연결 상태. 신호 모니터 화면과 오류 안내용 */
    val sources: StateFlow<SourceStatus>

    /** 여러 번 불러도 된다(이미 시작했으면 무시). */
    fun start()
    fun stop()

    /** 런타임 퍼미션(차속 CAR_SPEED)을 사용자가 허용한 뒤 부른다. 차속 소스를 다시 연결한다. */
    fun onPermissionsChanged()
}

/** 내비 조작. 신호(읽기)와 분리해 둔다. */
interface NaviControl {
    /** 충전 사업자 목록(현대 E-pit, 환경부 등). 시작 시 한 번 받아 온다. */
    val chargerOperators: StateFlow<List<ChargerOperatorInfo>>

    /**
     * 목적지로 경로 안내를 시작한다. **현재 경로를 덮어쓰므로** 누르기 전에 사용자에게 확인받을 것.
     * 매니페스트에 `pleos.car.permission.NAVI_ROUTE_SEARCH` 필요.
     */
    fun requestRoute(destination: RouteDestination)
    fun cancelRoute()
}

/** 앱이 쓰는 데이터 묶음. 매니페스트에 선언한 퍼미션과 맞춘다(선언 안 한 묶음을 켜면 FAILED). */
enum class CoreFeature {
    /** SoC·충전구·충전 스위치·배터리 용량. `pleos.car.permission.CAR_ENERGY` */
    EV_BATTERY,

    /** 기어·브레이크·조향각·크루즈. `pleos.car.permission.CAR_DRIVE` */
    DRIVE,

    /** AAOS 차속. `android.car.permission.CAR_SPEED`(dangerous → 런타임 동의 필요) */
    SPEED,

    /** 위치·경로·혼잡 구간·충전 사업자. `pleos.car.permission.NAVI_ROUTE` */
    NAVI,
}

// ---------------- 상태 모델 ----------------

data class EvBatteryState(
    /** 0~100 */
    val socPercent: Int? = null,
    val chargePortOpen: Boolean? = null,
    /** 리스너가 없어 충전구가 열려 있는 동안만 10초마다 조회한다. 에뮬레이터는 켜도 SoC가 오르지 않음 */
    val chargeSwitchOn: Boolean? = null,
    /** 에뮬레이터 77.4 */
    val capacityKwh: Float? = null,
)

data class DriveState(
    val gear: Gear? = null,
    /** AAOS PERF_VEHICLE_SPEED(m/s)를 km/h로 바꾼 값. 퍼미션이 없으면 null */
    val speedKph: Float? = null,
    /** 밟음 여부만 온다(세기 없음) */
    val brakePressed: Boolean? = null,
    /** SDK 값 그대로. 에뮬레이터 콘솔 값과 부호가 반대로 들어온다(콘솔 30 → -30) */
    val steeringAngleDeg: Float? = null,
)

enum class Gear { PARK, REVERSE, NEUTRAL, DRIVE, UNKNOWN }

data class CruiseState(
    /** SDK 값 그대로. 크루즈 준비 안 됨(not_ready)이면 null. 해석(작동/해제)은 앱에서 한다 */
    val mode: CruiseMode? = null,
    /** SDK 값(m/s로 보임)을 km/h로 바꾼 값 */
    val targetSpeedKph: Float? = null,
)

/**
 * SDK `CruiseControl` 열거형과 같은 이름.
 * 에뮬레이터에서는 ENABLED·ACTIVATED만 만들 수 있다. 실차 해제 값(USER_OVERRIDE 등)은 미확인(findings 6.2.1).
 */
enum class CruiseMode { OTHER, ENABLED, ACTIVATED, USER_OVERRIDE, SUSPENDED, FORCED_DEACTIVATION_WARNING }

data class GeoPoint(val latitude: Double, val longitude: Double)

data class LocationState(
    val point: GeoPoint,
    /** 진행 방향. 0 = 북, 시계 방향 */
    val headingDeg: Double,
    val address: String? = null,
    /** 이 값을 받은 시각(System.currentTimeMillis) */
    val receivedAtMs: Long = 0L,
)

data class RouteState(
    /** 목적지 경로 안내 중인지 */
    val guiding: Boolean = false,
    /** 경로 상 혼잡 구간. 경로 안내 중에만 채워진다 */
    val congestion: List<CongestionSegment> = emptyList(),
)

data class CongestionSegment(
    /** 경로 상 시작 위치(m). ⚠ 기준점(경로 출발점인지 현재 위치인지)은 실차 주행으로 확인 필요 */
    val offsetM: Double,
    val lengthM: Double,
    val level: CongestionLevel,
    val avgSpeedKph: Double,
    val roadName: String,
)

enum class CongestionLevel { UNKNOWN, FAST, MODERATE, SLOW, HEAVY }

data class ChargerOperatorInfo(val id: String, val name: String)

data class RouteDestination(
    val name: String,
    val point: GeoPoint,
    val address: String = "",
)

// ---------------- 소스 상태 ----------------

enum class SourceState {
    /** 앱이 이 묶음을 켜지 않음 */
    OFF,
    CONNECTING,
    READY,

    /** 런타임 퍼미션 미허용(차속) */
    NO_PERMISSION,
    FAILED,
}

data class SourceStatus(
    val vehicle: SourceState = SourceState.OFF,
    val speed: SourceState = SourceState.OFF,
    val navi: SourceState = SourceState.OFF,
    /** 가장 최근 오류 메시지(디버그용) */
    val lastError: String? = null,
)
