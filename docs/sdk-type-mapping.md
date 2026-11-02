# SDK → core 타입 대응표

검증일: 2026-10-30. v0.1.0을 기준으로 대응표를 작성했으며, 타입 변경과 실제 SDK 어댑터는 이후 [core v0.3.0](../core/README.md)에 반영했다. 아래 '현재 core' 열은 검증 당시 v0.1.0이다.

## 검증 범위와 근거

- 저장소 지정 버전: `Vehicle:2.3.2`, `NaviHelper:3.0.5`.
- 공식 Maven 저장소에서 AAR을 받아 내부 `classes.jar`의 JVM descriptor와 generic Signature를 직접 읽었다.
- NaviHelper POM의 compile 의존성인 `navi-data:3.0.4`도 확인했다. 위치·혼잡 모델은 이 AAR에 있다.
- [원본 시그니처·SHA-256](sdk-type-evidence.json)에 증거를 기록했다. JVM 기본 타입 `I/F/D/Z`는 각각 Int/Float/Double/Boolean이다.
- 이 검사는 원본 값 타입을 확정한다. 참조 타입의 Kotlin nullability와 물리 단위는 descriptor만으로 확정하지 않는다.
- 단위·에뮬레이터 동작 근거는 [기존 실측](vehicle-data-findings.md) 6.2, 6.2.1, 7.5다. 이번에 에뮬레이터를 새로 실행한 것은 아니다.
- 공식 가이드: [Vehicle](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/intro), [NaviHelper](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/intro). 상세 API 웹 본문에는 시그니처가 표시되지 않아 AAR을 기준으로 했다.

## 기존 신호 대응

아래 core 타입은 다음 개편의 설계 결정이다. nullable은 SDK 원본 nullability와 별개로 아직 값이 없는 상태를 수용한다.

| 정보 | SDK getter / listener callback | 원본 값 타입 | 현재 core | 다음 core 기준 / 변환 |
|---|---|---|---|---|
| SoC | `EvBattery.getEvBatteryStateOfCharge` / `onEvBatteryStateOfChargeUpdated` | Int | `socPercent: Float?` | `Int?`, 퍼센트. 소수 SoC를 Fake에 넣지 않는다 |
| 충전구 | `EvBattery.isEvChargePortOpen` / `onEvChargePortOpenUpdated` | Boolean | `chargePortOpen: Boolean?` | 유지, true=열림 |
| 충전 스위치 | `EvBattery.getEvChargeSwitchState` | Boolean | `chargeSwitchOn: Boolean?` | 유지. 해당 변경 리스너 없음. 주기 조회; 충전 성공 판정으로 단독 사용하지 않음 |
| 크루즈 상태 | `DrivingMode.getCruiseControlState` / `onCruiseControlStateUpdated` | `CruiseControl` enum | `rawState: String?` | 같은 6개 값을 갖는 core enum으로 명시적 매핑. 원본 이름 보존. 해제 의미 해석은 앱 담당 |
| 크루즈 목표 속도 | `DrivingMode.getCruiseControlTargetSpeed` / `onCruiseControlTargetSpeedUpdated` | Float | `setSpeedKph: Float?` | 유지. 기존 실측 기준 m/s → km/h는 ×3.6. 변환은 실제 어댑터에서 한 번만 수행 |
| 브레이크 | `Brake.getCurrentBrakePedalStatus` / `onBrakePedalStatusUpdated` | Boolean | `brakePressed: Boolean?` | 유지. 페달 압력·퍼센트가 아님 |
| 조향각 | `SteeringWheel.getCurrentSteeringWheelAngle` / `onSteeringWheelAngleUpdated` | Float | `steeringAngleDeg: Float?` | Float 유지. degree 단위와 부호 규약은 추가 확인 전 확정하지 않음. 콘솔과 부호 반대라는 기존 실측만 있음 |
| 위도·경도 | `CurrentLocationInfo.latitude/longitude` | Double / Double | `GeoPoint(Double, Double)` | 타입 유지. 위치와 방향을 같은 관측값에 묶는 모델로 확장 |

`CruiseControl` 실제 값: `OTHER`, `ENABLED`, `ACTIVATED`, `USER_OVERRIDE`, `SUSPENDED`, `FORCED_DEACTIVATION_WARNING`.

## 추가할 신호

| 정보 | 원본 | 확인 결과 | core 설계 기준 |
|---|---|---|---|
| 진행 방향 | `CurrentLocationInfo.heading` | Double | Double 보존. 북쪽 기준·회전 방향·범위는 실측/명세 확인 전 보류 |
| 경로 식별 | `CurrentLocationInfo.routeId`, `currentRouteIndex` | String, boxed Integer | 경로 혼잡 대응에 필요할 수 있어 어댑터에서 버리지 않음. index를 거리로 간주하지 않음 |
| 실제 차속 | AAOS `PERF_VEHICLE_SPEED` | 기존 실측상 Float m/s; Android 34 android.car.jar로 어댑터 컴파일 확인 | 크루즈 목표 속도와 별개로 제공. Float km/h, ×3.6. 실제 권한 허용·수신은 에뮬레이터에서 확인 필요 |
| 경로 혼잡 | `RouteStateInfo.trafficStatus` | `TrafficStatus` | 현재 정체 여부 Boolean으로 축약하지 않고 경로 컨텍스트와 구간 목록 제공 |
| 혼잡 목록 | `TrafficStatus.congestionItemList` | `List<TrafficCongestionInfo>` | 목록 각 항목을 보존. 미수신과 빈 목록 구분 |
| 혼잡 항목 | `offset`, `distance`, `avrSpeed` | 모두 Double | 단위·offset 기준점은 추가 검증 전 원본 의미로 보존 |
| 혼잡 시간 | `trafficCongestionTime` | Long | 초/밀리초 확정 전 변환하지 않음 |
| 혼잡 등급 | `trafficCongestionType` | `TrafficCongestionType` enum | `UNKNOWN`, `FAST`, `MODERATE`, `SLOW`, `HEAVY`를 구분 |

혼잡 목록이 존재한다는 사실만으로 현재 차량이 정체 구간 안에 있다고 판단하면 안 된다. 현재 위치/경로 진행 상태와 offset의 관계를 검증해야 한다. 경로가 없거나 미확인인 경우 정체 필터의 차속 대체 판정은 앱 로직에서 수행한다.

## Fake 및 실제 어댑터 구현 계약

1. Fake는 변환을 마친 core 단위로 입력한다. 목표속도 `100f`는 100km/h이며 SDK 원본 m/s가 아니다.
2. SDK → core 변환 테스트와 core 소비 로직 테스트를 분리한다. Fake만으로 SDK 호환성을 증명할 수 없다.
3. SDK null/미수신/권한 거부/미지원/연결 종료는 정상 해제 이벤트와 구분한다. 값과 상태를 별도로 모델링한다.
4. SDK callback은 현재 코드의 StateFlow와 동일하지 않다. StateFlow는 동일값을 합치므로 매 콜백 수신 시각이 필요하면 관측 시각도 함께 갱신한다.
5. 읽기는 비동기 성공·실패 callback, 변경 구독은 register/unregister 구조다. 초기 getter와 변경 구독을 함께 고려한다.
6. core v0.3.0은 차슐랭 원본에서 변경한 뒤 크루즈브레이커에 동기화했다. Vehicle·NaviHelper·AAOS 어댑터를 구현했고 실제 SDK 및 Android 34 JAR로 컴파일·변환 테스트를 확인했다. Binder 연결·실제 수신은 미검증이다.

## 남은 의미 검증

- 조향각 단위와 양수 방향, heading 기준 및 유효 범위.
- 혼잡 offset/distance/시간/평균속도의 정확한 단위와 현재 경로 위치 대응 방법.
- AAOS 차속 런타임 권한 허용·실제 수신. 사용한 SDK 리스너의 nullable 값과 non-null Exception 시그니처는 실제 AAR로 컴파일 확인했다.
- 실제 차량의 크루즈 해제 상태. 에뮬레이터의 ACTIVATED → ENABLED 매핑은 시연 규칙이며 실차 계약이 아니다.
