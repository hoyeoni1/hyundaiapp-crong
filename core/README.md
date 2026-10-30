# core — 공통 차량·내비 신호 모듈

두 앱(차슐랭·크루즈브레이커)이 함께 쓰는 모듈입니다. **앱 코드는 Pleos SDK를 직접 부르지 않고 이 모듈만 씁니다.**

- 담당: 황준용 (수정은 황준용만, 다른 사람은 요청)
- 원본: 차슐랭 저장소. 크루즈브레이커에는 `tools/sync-core.sh`로 복사
- 버전: `core/VERSION` (고치면 올리고, PR 제목에 `core vX.Y.Z`)

## 쓰는 법

```kotlin
// 1) 앱에서 하나만 만든다(AppContainer). 매니페스트 퍼미션과 CoreFeature를 맞춘다
val signals = PleosVehicleSignals(context, setOf(CoreFeature.EV_BATTERY, CoreFeature.NAVI))
signals.start()

// 2) StateFlow로 읽는다. 값을 아직 못 받았으면 null
signals.evBattery.collect { ev -> ev.socPercent; ev.chargePortOpen }

// 3) 테스트·미리보기에서는 가짜를 쓴다
val fake = FakeVehicleSignals()
fake.setEv { copy(chargePortOpen = true, socPercent = 40) }
```

## 제공하는 값

| Flow | 내용 | 필요한 CoreFeature / 퍼미션 | 출처 |
|---|---|---|---|
| `evBattery` | SoC(%), 충전구 열림, 충전 스위치, 배터리 용량(kWh) | `EV_BATTERY` / `pleos.car.permission.CAR_ENERGY` | Vehicle SDK `EvBattery` |
| `drive` | 기어, 차속(km/h), 브레이크, 조향각 | `DRIVE` / `pleos.car.permission.CAR_DRIVE` (차속은 `SPEED`) | Vehicle SDK `Gear`·`Brake`·`SteeringWheel` |
| `drive.speedKph` | 차속 | `SPEED` / `android.car.permission.CAR_SPEED` (**런타임 동의**) | AAOS `PERF_VEHICLE_SPEED` |
| `cruise` | 크루즈 상태(SDK 이름 그대로), 설정 속도(km/h) | `DRIVE` | Vehicle SDK `DrivingMode` |
| `location` | 위도·경도, 진행 방향(°), 주소 | `NAVI` / `pleos.car.permission.NAVI_ROUTE` | NaviHelper(1초마다 조회) |
| `route` | 안내 중 여부, 혼잡 구간 목록 | `NAVI` | NaviHelper(5초마다 + 이벤트) |
| `sources` | 소스별 연결 상태(OFF/CONNECTING/READY/NO_PERMISSION/FAILED), 최근 오류 | — | — |

`NaviControl`(같은 객체): `chargerOperators`(충전 사업자 목록), `requestRoute()`(**기존 경로를 덮어씀**, `NAVI_ROUTE_SEARCH` 필요), `cancelRoute()`.

유틸: `GeoMath.distanceM / bearingDeg / angleDiffDeg`, 디버그용 `signals.debugLines()`.

## 알아 둘 것 (에뮬레이터 실측, `docs/vehicle-data-findings.md`)

- SoC는 충전 중에도 자동으로 오르지 않는다 → Virtual Console로 주입.
- 조향각은 콘솔 값과 부호가 반대(콘솔 30 → -30).
- 크루즈: 콘솔 `full_autonomous` → `ACTIVATED`, `ready/longitudinal/lateral` → `ENABLED`, `not_ready` → `null`. 해제 값(`USER_OVERRIDE` 등)은 재현 불가.
- 위치는 판교 부근 고정, 모의 주행 없음.
- 경로 안내가 시작되면 내비가 앞으로 나오고 앱 프로세스가 얼 수 있다(이벤트 멈춤).
- 로그: `adb logcat -s HyCore`

## 다음에 할 일 (황준용)

- [ ] 실기기·장시간 동작 확인(재연결, 서비스 끊김 시 재시도)
- [ ] Pleos SDK가 `getCurrentGearSelectionState`, `isEvChargePortOpen`, `getEvBatteryCapacity` 등을 deprecated로 표시함("Google 프로퍼티를 쓰라") → AAOS `GEAR_SELECTION`·`EV_CHARGE_PORT_OPEN`·`INFO_EV_BATTERY_CAPACITY`로 바꿀지 결정(퍼미션이 달라짐)
- [ ] 백그라운드 수집 구조(포그라운드 서비스 등) — 이민하 실험 결과 반영
- [ ] 단위 테스트 보강(SDK 값 → 상태 변환 함수)
