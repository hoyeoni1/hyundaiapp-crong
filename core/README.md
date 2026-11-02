# core v0.3.1 — 차량 신호와 실제 SDK 연결

원본은 차슐랭 저장소다. 두 앱은 `VehicleSignals.snapshot`을 구독한다. `FakeVehicleSignals`는 수동 입력, `PleosVehicleSignals`는 실제 읽기 전용 SDK 어댑터다.

v0.3.1은 release에 포함되지 않는 debug 전용 `SdkDiagnosticsActivity`를 추가한다. 반복 실행·콘솔 자동 검증은 [Windows 로컬 테스트](../docs/windows-local-testing.md)를 따른다.

## 앱에서 연결하기

개인정보 동의 화면을 완료한 뒤 앱 수명 관리 코드에서 생성한다. Activity 화면 전환마다 stop하면 그 사이 기록이 끊긴다. 주행 중 수집을 유지할 서비스의 소유권과 백그라운드 동결 해결은 앱 측 작업이다.

```kotlin
val options = PleosSignalOptions.cruiseBreaker() // 차슐랭: .chachelin()
val signals: VehicleSignals = PleosVehicleSignals(applicationContext, options)
val missing = SignalPermissions.missing(applicationContext, options)
// 앱에서 필요한 동의/런타임 권한을 처리한 뒤:
signals.start()
// 기존 CoroutineScope에서 signals.snapshot.collect { ... }
// 수집을 실제로 종료할 때:
signals.stop()
```

모듈은 권한 대화상자를 띄우지 않는다. 권한을 나중에 허용했거나 연결이 실패한 경우 앱에서 stop → start하여 새 세션으로 재시도한다. 앱 설정·권한을 자동 변경하지 않는다.

| 프로필 | 요청 신호 | 앱에 선언할 권한 |
|---|---|---|
| chachelin | SoC·충전구·충전 스위치·위치 | CAR_ENERGY, NAVI_ROUTE |
| cruiseBreaker | 크루즈 상태·목표속도·브레이크·조향·실제 차속·위치·경로 혼잡 | CAR_DRIVE, NAVI_ROUTE, android.car.permission.CAR_SPEED |

Pleos 권한의 전체 접두사는 `pleos.car.permission.`이다. CAR_SPEED는 런타임 동의도 필요하다. 선택하지 않은 신호는 접근하지 않고 NOT_RECEIVED로 남긴다. 직접 `PleosSignalOptions(signals = setOf(...))`로 범위를 좁힐 수 있다. 공통 모듈은 android.car 컴파일 라이브러리와 런타임 uses-library 선언을 제공하며, 신호 권한은 각 앱 매니페스트에 둔다.

## 관측 계약

각 필드는 `SignalReading<T>`다. AVAILABLE일 때만 value가 존재한다. NOT_RECEIVED, UNSUPPORTED, PERMISSION_DENIED, ERROR는 정상 false/0/크루즈 해제와 다르다. ERROR에만 errorMessage를 담는다.

observedAtMillis는 단조 증가 경과 시간이며 서버 저장용 날짜가 아니다. sequence는 신호별·세션별로 증가한다. 동일 값의 재수신도 구분한다. StateFlow는 빠른 중간 상태를 생략할 수 있으므로 모든 원본 이벤트를 보존하는 큐가 아니다. 해제 감지 소비자는 lifecycle과 AVAILABLE 여부를 확인하고 오래된 관측값을 사용하지 않아야 한다.

start/stop은 반복 호출에 안전하다. stop은 관측값을 미수신으로 비우며 새 start는 sequence를 초기화한다. 이전 세션의 늦은 콜백은 새 상태를 덮어쓰지 않는다. STARTED는 수집 요청이 시작되었다는 뜻이며 모든 서비스가 연결됐다는 뜻은 아니다.

## 실제 연결 동작

- Vehicle: initialize → 권한 확인 → 신호별 capability의 READ/READ_WRITE 확인 → 리스너 등록 + 초기 getter. 리스너에서 더 최신 값/실패를 받은 뒤 도착한 초기 getter 응답은 무시한다.
- 충전 스위치: 변경 리스너가 없어 기본 10초 주기 조회. 동일 신호의 조회 요청은 겹치지 않는다.
- Vehicle 종료: 폴링 중단 → unregister → 진행 중인 비동기 요청 완료 후 release. 응답이 없으면 기본 5초 요청 제한시간 이후 정리한다. SDK 요청 자체를 취소하는 공개 API는 없다.
- NaviHelper: 연결 성공 callback 뒤 리스너 등록. 위치·경로 상태를 기본 1초 주기로 요청한다. 요청마다 기본 5초 제한시간이 있고 동일 종류 요청은 겹치지 않는다. 경로 종료/취소/변경은 기존 혼잡값을 초기화한다.
- 혼잡은 약관 동의 + ROUTE_GUIDING 상태에서만 제공한다. 다른 모드는 NOT_RECEIVED로 내려 차속 대체 판정이 가능하게 한다. 빈 목록 수신과 미수신을 구분한다. isRouting 플래그는 의미를 추측해 필터링하지 않고 원본 그대로 전달한다.
- 실제 차속: AAOS Car 서비스의 PERF_VEHICLE_SPEED를 10Hz로 구독한다. 차량 서비스 연결·속성 조회·리스너 등록은 별도 HandlerThread에서 수행한다. 초기값은 등록 callback으로 받고, CarPropertyValue.status가 AVAILABLE일 때만 값을 사용한다. API 29 미만/비차량 환경은 해당 신호를 UNSUPPORTED로 둔다.
- 권한 확인 실패와 SecurityException은 PERMISSION_DENIED, 읽기 capability 미지원은 UNSUPPORTED, 불투명한 SDK 예외·연결 실패·요청 시간 초과는 ERROR로 보존한다. 예외 메시지를 추측해 SDK 오류 코드를 분류하지 않는다.

## 타입과 단위

- SoC: Int 0~100%. 충전구/스위치/브레이크: Boolean.
- 크루즈: SDK CruiseControl의 6개 값을 명시적으로 core enum에 매핑. OTHER와 미수신은 다르며 작동/해제 의미 판정은 앱 담당이다.
- 크루즈 목표속도·차속: Float km/h. 어댑터에서 SDK/AAOS m/s에 ×3.6을 한 번 적용한다. Fake에는 이미 km/h인 값을 넣는다. 실제 차속은 후진 음수를 보존한다.
- 위치: 위도·경도 Double, heading Double, routeId/currentRouteIndex 보존. 기본 판교 좌표를 자동 주입하지 않는다.
- 조향각은 Float 원본 값이다. 단위와 부호가 추가 확인 대상이라 이름에 Deg를 붙이지 않는다. heading 기준·혼잡 offset/거리/시간/평균속도 단위도 추정해 변환하지 않는다.
- 경로 혼잡 목록으로 현재 차량의 정체 여부를 단정하지 않는다. offset과 현재 경로 진행 위치의 대응 검증은 남아 있다.

충전구 getter·리스너는 Vehicle 2.3.2에서 deprecated다. 기존 에뮬레이터 실측과 CAR_ENERGY 권한 계약을 유지하기 위해 현재 연결하며 향후 AAOS EV_CHARGE_PORT_OPEN으로 교체할 수 있다. 이 PC의 Pleos 에뮬레이터에서 충전구 변경 콜백 수신을 검증했다. 실차는 미검증이다.

## Fake

```kotlin
var elapsed = 0L
val fake = FakeVehicleSignals { elapsed }
fake.start()
fake.setVehicleSpeedKph(100f)
fake.setCruiseState(CruiseControlState.ACTIVATED)
elapsed += 5_000
fake.setBrakePressed(true)
fake.setCruiseState(CruiseControlState.ENABLED) // 에뮬레이터 시연 매핑. 실차 해제 의미 미확정
fake.setUnavailable(VehicleSignal.VEHICLE_SPEED, SignalStatus.PERMISSION_DENIED)
fake.stop()
```

Fake 입력은 start 후에만 가능하다. 충전은 setChargePortOpen → setSocPercent → setChargeSwitchOn → setSocPercent → setChargePortOpen으로 재현한다. 충전 성공·크루즈 해제 자동 판정, 자동 위치 재생·콘솔 동기화는 포함하지 않는다.

## 검증 및 남은 실행 확인

표준 명령: `gradlew.bat :core:testDebugUnitTest` (JDK·Android SDK 필요). 단위 테스트는 세션 분리·독립적인 실패/복구·입력 검증·실제 SDK enum/내비 모델 변환·속도 변환을 확인한다.

v0.3.0에서는 공식 JAR로 독립 컴파일·JUnit을 실행했다. v0.3.1에서는 Windows 로컬 SDK 환경을 설치하고 실제 Gradle 단위 테스트 14개와 두 앱 debug APK 빌드·설치를 검증했다. Pleos 2.1.0에서 차속·크루즈·브레이크·조향·목표속도·배터리·충전구·충전 스위치·위치의 실제 SDK 수신, 진단 화면의 Stop/재시작/Fake 전환이 통과했다. 실차·백그라운드 동결 해결·경로 혼잡 membership 판정은 검증 범위 밖이다. 반복 실행 방법과 근거 파일은 Windows 로컬 테스트 문서를 참조한다.

에뮬레이터에서는 두 프로필별 권한 누락/허용, SoC·충전구 변경, 크루즈 ACTIVATED ↔ ENABLED, 브레이크, 차속 60km/h → 약 60 반환, 경로 안내 유무, stop/start 후 과거 콜백 무시와 리스너 정리를 확인해야 한다.

공식 근거: [Vehicle 연결](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/obtain-domain-object), [NaviHelper 초기화](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/initialization-and-release), [AAOS CarPropertyManager](https://developer.android.com/reference/android/car/hardware/property/CarPropertyManager).

v0.1.0 → v0.2.0에서는 개별 evBattery/cruise/driverInput/location Flow를 snapshot으로 통합했다. v0.3.0은 해당 인터페이스를 유지하고 실제 어댑터와 앱별 프로필을 추가한다.
