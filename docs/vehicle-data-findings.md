# Pleos Connect 에뮬레이터 — 실제로 받을 수 있는 차량·내비 데이터 조사 결과

> 조사일: 2026-10-22
> 환경: Pleos Connect Emulator v2.1.0 (API 34, AVD `Pleos_Connect_2_1_0`)
> SDK 버전:
> - Vehicle SDK `ai.pleos.playground:Vehicle:2.3.2`
> - NaviHelper SDK `ai.pleos.playground:NaviHelper:3.0.5`
>
> 방법: 조사용 앱 `PleosDataProbe/`가 Vehicle SDK의 조회·지원 여부 확인(`check*Capabilities`) 메서드를 리플렉션으로 **전부** 호출함(756회). NaviHelper는 모든 이벤트를 기록하면서 다음을 실행:
> 1. 조회 메서드 8개 호출
> 2. 서울역으로 경로 요청 + 예상 주행 정보(Trip) 요청
> 3. 재조회
> 4. 경로 취소
>
> 원시 결과: `PleosDataProbe/results/vehicle.tsv`, `PleosDataProbe/results/navi.tsv`
> 관련 문서: `contest-analysis-todo.md` 7.1절, `dev-env-setup-log.md`

---

## 0. 핵심 결론 (아이디어 기획에 바로 쓰는 것)

1. **차량 데이터는 대부분 "읽을 수 있음"**:
   - 대상: 기어, 도어/창문/잠금, 시트(점유·벨트·위치·열선/통풍), 공조(HVAC), 조명, EV 배터리(SoC·용량·충전 상태), 주행가능거리, 누적주행거리, 핸들 각도, 방향지시등, 와이퍼, 크루즈 컨트롤, 타이어 저압 기준.
   - 앱이 받은 값은 268개.
2. **⚠ 현재 차속(km/h)을 주는 API가 Pleos SDK에는 없음** — ~~API 없음~~ → **정정(7장)**: AAOS 표준 `CarPropertyManager`의 `PERF_VEHICLE_SPEED`로 실시간(약 7~20 Hz) 수신됨(실측). 단 `android.car.permission.CAR_SPEED`는 dangerous(런타임 동의) 퍼미션.
   - 대신 쓸 수 있는 것: NaviHelper `onCurrentRoadSpeedLimit`(도로 제한속도, 1초마다), 크루즈 설정 속도.
   - 위치·속도용 FusedLocation SDK는 "준비 중".
   - → ~~"안전운전 점수" 류처럼 실시간 속도에 의존하는 기획은 위험.~~ **정정(7.5)**: 차속은 AAOS로 받을 수 있음. 여전히 없는 것은 가속 페달·가속도·초음파.
3. **차량 값은 "Pleos Connect Virtual Console"(웹)로 바꿀 수 있고, 대부분 SDK 리스너로 즉시 들어옴** → 6장.
   - 바꿀 수 있는 것: 기어, 속도, 페달, 핸들, SoC, 도어, 창문, 공조, 조명 등 63개 항목.
   - **단, 내비 위치는 판교 부근에 고정**이고 모의 주행 기능이 없음. 속도를 올려도 지도상 위치, 남은 거리, 누적거리, SoC는 변하지 않음.
4. **미지원 항목**:
   - 연료(Fuel) 전부, 트립 거리·시간 전부, 주행 모드, 순간·누적 전비, 안개등, 오일·브레이크액·워셔액 경고, 에어백 전개, 스마트글라스, 하이패스 음량.
   - 에뮬레이터 차량이 **EV**로 구성돼 있기 때문(배터리 77.4 kWh).
5. **NaviHelper는 기대 이상으로 풍부함 (실제 서버 데이터)**:
   - 현재 위치·주소, 목적지·남은 거리·시간, 통행료, 회전 안내(TBT) 전체 목록, 경로 상 혼잡 구간(도로명·평균속도).
   - 예상 주행(Trip): 거리·ETA·**도착 시 예상 SoC**·에너지 소모량·구간별 혼잡도.
   - 충전 사업자 목록, 최근 목적지, 즐겨찾기(집·회사).
   - 앱에서 경로 요청·취소도 동작함.
6. **퍼미션**:
   - 조회용 퍼미션은 전부 `normal`이라 매니페스트에 선언만 하면 설치 시 자동 부여됨(에뮬레이터 기준).
   - 단 Playground 앱 심사에서는 **승인된 퍼미션만** 허용됨(공식 문서). 실제로 쓰는 것만 선언할 것.

---

## 1. 필요한 퍼미션

| 퍼미션 | 보호 수준 | 용도 |
|---|---|---|
| `pleos.car.permission.CAR_INFO` | normal | CarInfo, Odometer 등 |
| `pleos.car.permission.CAR_DRIVE` | normal | 기어, 핸들, 브레이크, 크루즈, 방향지시등 |
| `pleos.car.permission.CAR_ENERGY` | normal | EV 배터리, 연료 |
| `pleos.car.permission.CAR_DOORS` / `CAR_WINDOWS` / `CAR_SEATS` / `CAR_MIRRORS` | normal | 도어, 창문, 시트, 미러 |
| `pleos.car.permission.CAR_CLIMATE` | normal | 공조 |
| `pleos.car.permission.CAR_LIGHTS` | normal | 조명 |
| `pleos.car.permission.CAR_SAFETY` | normal | 안전(에어백, 벨트, 경고) |
| `pleos.car.permission.CAR_TOLL` | normal | 하이패스 |
| `pleos.car.permission.NAVI_ROUTE` | normal | NaviHelper 조회와 이벤트 수신 (`get*Info`, `requestTrip`) |
| `pleos.car.permission.NAVI_ROUTE_SEARCH` | normal | NaviHelper 경로 요청·취소 (`requestRoute`, `cancelRoute`) |
| `ai.pleos.playground.service.vehicle.VEHICLE_BINDING` | — | Vehicle SDK AAR이 자동으로 병합 |

- **어떤 CAR_\* 퍼미션을 선언해도 다음 4개는 `SecurityException ... need Permission`으로 실패**:
  - `CarInfo.getCapabilityInfo`
  - `Safety.getTelltalesInfo` / `checkTelltalesCapabilities`
  - `Tire.getTirePressure` / `checkTirePressureCapabilities`
  
  `PRIVATE_CAR_*` 계열이 필요한 것으로 추정함(일반 앱용 아님).
- 쓰기(제어)용 `CONTROL_CAR_*` 퍼미션도 존재함. 다만 Vehicle Control은 아직 "준비 중"이라 이번에는 조사하지 않음. 이번 조사는 **조회만** 했음.
- 에뮬레이터에 존재하는 그 밖의 Pleos 퍼미션:
  - `NAVI_NOTIFY`, `NAVI_CUSTOM_ROUTE/MAP/ETC`, `HID_*`, `ADAS_PARKING`
  - `FUSED_LOCATION` (dangerous, 런타임 동의 필요)
  - `TTS_SERVICE`, `STT_SERVICE`, `LLM_SERVICE`

---

## 2. Vehicle SDK — 도메인별 결과

범례:
- ✅ 값 받음
- ⚠ 조건부(영역이나 상태 조건이 맞을 때만)
- ❌ 에뮬레이터 미지원(`ERROR_NOT_SUPPORTED_PROPERTY` 또는 "not in carPropertyConfig")
- 🔒 일반 퍼미션으로 불가

"쓰기 가능"은 `check*Capabilities`의 `accessType`이 READ_WRITE인지를 뜻함. 실제 제어 API는 별도 승인이 필요함.

### 2.1 주행·차량 상태

| 데이터 | 메서드 | 결과 | 에뮬레이터 값 | 비고 |
|---|---|---|---|---|
| 기어 | `Gear.getCurrentGearSelectionState` | ✅ | `GEAR_PARK` | 리스너 `registerGearSelection` |
| 브레이크 페달 | `Brake.getCurrentBrakePedalStatus` | ✅ | `false` | READ |
| 핸들 조향각 | `SteeringWheel.getCurrentSteeringWheelAngle` | ✅ | `0.0`° | 범위 -470~500 |
| 방향지시등 | `TurnSignal.getCurrentTurnSignalState` | ✅ | `OFF` | |
| 크루즈 상태 | `DrivingMode.getCruiseControlState` | ✅ | `ENABLED` | |
| 크루즈 설정 속도 | `DrivingMode.getCruiseControlTargetSpeed` | ✅ | `13.8889` | m/s로 보임(=50 km/h) |
| 주행 모드(에코/스포츠 등) | `DrivingMode.getCurrentDrivingModeState` | ❌ | | |
| 누적 주행거리 | `Odometer.getCurrentDistanceDriven` | ✅ | `0.0` | |
| 주행 가능 거리 | `Odometer.getRangeRemainingDistance` | ✅ | `500000.0` | 단위 m(클러스터 표시 500 km와 일치) |
| 트립 거리·시간 (현재/누적/충전 후/순간) | `Odometer.getTrip*` 8종 | ❌ | | |
| **현재 차속** | — | **API 없음** | | 3장 대체 수단 참고 |

### 2.2 에너지 (EV)

| 데이터 | 메서드 | 결과 | 값 | 비고 |
|---|---|---|---|---|
| 배터리 SoC | `EvBattery.getEvBatteryStateOfCharge` | ✅ | `100` (%) | 리스너 있음 |
| 배터리 잔량 / 용량 | `getEvBatteryLevel` / `getEvBatteryCapacity` | ✅ | `77400.0` / `77400.0` | Wh(77.4 kWh) |
| 충전구 열림 | `isEvChargePortOpen` | ✅ | `false` | 쓰기 가능 |
| 충전 중 여부(스위치) | `getEvChargeSwitchState` | ✅ | `false` | 쓰기 가능 |
| 충전 전류 제한 | `getEvChargeCurrentDrawLimit` | ✅ | `12.5` | 최대 48 A |
| 충전 목표 % 선택지 | `checkEvChargePercentLimitCapabilities` | ✅ | 50~100 (5 단위) | 쓰기 가능 |
| 전비 (현재/순간/누적/충전 후) | `get*EvEfficiency` | ❌ | | |
| 연료 % / 연비 | `Fuel.*` 전부 | ❌ | | EV 구성 |

### 2.3 도어·창문·잠금

| 데이터 | 메서드 | 결과 | 값 | 영역 |
|---|---|---|---|---|
| 도어 열림 정도 | `Door.getCurrentDoorPosition` | ✅ | 전부 `0` (닫힘) | 1열 좌우, 2열 좌우, HOOD, REAR(트렁크) — 각각 개별 영역, 범위 0~1 |
| 도어 잠금 | `Door.getDoorLockState` | ⚠ | `true` | **복합 영역** `ROW_1_LEFT+ROW_1_RIGHT+ROW_2_LEFT+ROW_2_RIGHT`와 `HOOD`만 됨 |
| 글로브박스 잠금 | `getCurrentGloveBoxLockState` | ✅ | `false` | |
| 창문 위치 | `Window.getCurrentWindowPosition` | ✅ | 전부 `0` | 4개 창 개별, 0~10 |
| 창문 블라인드 위치 | `getCurrentWindowBlindPosition` | ✅ | 전부 `0` | 0~10 |
| 창문 잠금 | `getCurrentWindowLockState` | ✅ | `false` | 전체 복합 영역 / 2열 좌·우 |
| 스마트글라스 | `getCurrentSmartGlass*` | ❌ | | |

### 2.4 시트·탑승자·안전

| 데이터 | 메서드 | 결과 | 값 | 비고 |
|---|---|---|---|---|
| 좌석 점유 | `Seat.getCurrentSeatOccupancy` | ✅ | 운전석 `false`, 나머지 4석 `true` | 5석(1열 2, 2열 3) |
| 안전벨트 체결 | `Seat.getCurrentSeatBeltBuckledState` | ✅ | 5석 모두 `false` | |
| 시트 위치(전후·높이·등받이·틸트) | `getCurrentSeat*Position` | ✅ | `0` | 1·2열, 0~100 |
| 1열 세부(깊이·요추·헤드레스트·벨트높이) | 〃 | ✅ | `0` (벨트높이 `10`) | 1열만, -10~10 |
| 시트 열선 / 통풍 | `getHvacSeatTemperature` / `getHvacSeatVentilationState` | ✅ | `0` | 열선 4석, 통풍 1열, 0~3단 |
| 시트 메모리 | `checkSeatMemoryCapabilities` | ✅ | 슬롯 1~2 | WRITE 전용 |
| 에어백 활성화 | `Safety.hasSeatAirbagEnabled` | ✅ | `false` | 1열 |
| 에어백 전개 여부 | `hasSeatAirbagBeenDeployed` | ❌ | | |
| 벨트 미체결 경고 | `hasSeatBeltNotBuckledWarning` | ❌ | | |
| 전원 차단 경고 | `hasPowerDownWarningIndicator` | ✅ | `false` | |
| 엔진오일·변속기오일·브레이크액·워셔액 경고 | `Safety.get*LevelState`, `hasLowWasherFluidWarning` | ❌ | | |
| 경고등(Telltale) | `getTelltalesInfo` | 🔒 | | |

### 2.5 공조(HVAC)·열선

공조는 **영역 조합이 기능마다 다름**. 반드시 `check*Capabilities`가 돌려준 영역 그대로 조회해야 함. 좌석 하나만 넣으면 `area ID not supported` 오류가 남.

| 데이터 | 메서드 | 유효 영역 | 값 |
|---|---|---|---|
| 에어컨 / 전원 / 내기순환 / 자동내기 / 공기청정 / 최대제상 | `getHvacAcState` 등 | 1·2열 전체(5석) | AC `false`, 전원 `false`, 내기 `true`, 나머지 `false` |
| 오토 / 듀얼 / 풍량 / 풍향 | `getHvacAutoState` 등 | 1열 좌+우 | 오토 `false`, 듀얼 `false`, 풍량 `3`(1~8), 풍향 `FACE` |
| 실내 현재 온도 | `getHvacTemperatureCurrent` | 1열 좌·우 각각, 또는 전체 | `19.1` ℃ |
| 설정 온도 | `getHvacTemperature` | 1열 좌·우 (15~30 ℃) | ⚠ `PropertyNotAvailableException`. 공조 전원 OFF 상태라 그런 것으로 추정 |
| 앞·뒤 유리 제상 | `getHvacDefrosterState` | FRONT / REAR_WINDSHIELD | `false` |
| 최대 냉방 | `getHvacMaxAcState` | — | ❌ |
| 핸들 열선 | `SteeringWheel.getHvacSteeringWheelHeatState` | — | `0` (0~3) |
| 사이드미러 열선 | `SideMirror.getHvacSideMirrorHeatState` | 좌/우 개별, 좌+우 | `0` (0~3) |
| 온도 표시 단위 | `Display.getCurrentTemperatureDisplayUnit` | — | `CELSIUS` |

### 2.6 조명·와이퍼·타이어·기타

| 데이터 | 메서드 | 결과 | 값 |
|---|---|---|---|
| 상향등 / 하향등 / 미등 상태 | `Light.getCurrent*LightState` | ✅ | 상향 `ON`, 하향 `OFF`, 미등 `ON` (에뮬레이터 기본값) |
| 상향등 / 하향등 / 미등 스위치 | `get*LightSwitch` | ✅ | `OFF` / `OFF` / `ON` |
| 실내등 | `getCurrentCabinLightState` / `getCabinLightSwitch` | ✅ | `OFF` |
| 독서등 (4석) | `getCurrentReadingLightState` | ✅ | `OFF` |
| 앰비언트 라이트 상태·스위치·밝기·색 | `getAmbientLight*` | ✅ (전체 영역) | `OFF`, 밝기 `70`(0~100), 색 `16777215`(흰색 0xFFFFFF) |
| 안개등(앞·뒤), 주행모드 연동 앰비언트 | | ❌ | |
| 와이퍼 상태 / 스위치 | `Wiper.getCurrentWiperState/Switch` | ✅ | 앞·뒤 `OFF` (스위치 선택지 OFF, CONTINUOUS_LEVEL_1, CONTINUOUS_LEVEL_3, AUTO) |
| 타이어 저압 기준값 | `Tire.getLowTirePressure` | ✅ | 4륜 `137.0` (kPa) |
| 타이어 현재 공기압 | `Tire.getTirePressure` | 🔒 | |
| VIN / CRN | `CarInfo.getVINInfo` / `getCRNInfo` | ✅ | `"invalid"` (에뮬레이터라 실제 값 없음) |
| 하이패스 음성 안내 음량 | `Toll.getCurrentEtcsVoiceGuidanceVolume` | ❌ | |

### 2.7 리스너 (값 변화 구독)

대부분의 조회 항목에 `register*`/`unregister*` 리스너가 있음. 예: 기어, 도어, 창문, 시트, 공조, 조명, SoC, 주행가능거리, 핸들 각도, 방향지시등, 와이퍼, 타이어 공기압.
- 이번 조사는 **1회 조회만** 했고, 리스너로 값 변화를 받는 것은 검증하지 않았음(값을 바꿀 수단이 미검증, 4.3절).

---

## 3. NaviHelper SDK — 결과

**사용법**:
- `NaviHelper(context).initialize(onSuccess, onFail)` → `addListener(NaviHelperEventListener)`.
- `get*Info()`는 반환값 없이 결과를 **리스너 콜백**으로 보냄(호출 후 0.1~1초 이내).

| 데이터 | 호출 → 콜백 | 결과 | 실제 값 예시 |
|---|---|---|---|
| 현재 위치 | `getCurrentLocationInfo` → `onCurrentLocationInfo` | ✅ | 위도 37.4127212, 경도 127.094681, heading 0.0, 주소 "수정구 시흥동"(판교 부근 고정) |
| 경로 상태 | `getRouteStateInfo` → `onRouteStateInfo` | ✅ | `naviMode`(ROUTE_GUIDING / SAFE_GUIDING), `routeOption`, 약관동의 여부, `remainDestinationChargingInt`(도착 시 잔량 추정), **trafficStatus**(경로 상 혼잡 구간 목록: 도로명, 거리, 소요시간, HEAVY/SLOW 등, 평균속도 km/h) |
| 목적지 | `getDestinationInfo` → `onDestinationInfo` | ✅ | 서울역, 남은 거리 23000 m, 남은 시간 2820 s, 통행료 |
| 회전 안내(TBT) | `getTBTInfo` → `onTBTInfo` | ✅ | 14개. 예: `TURN_RIGHT 700m "대왕판교IC 방면"`, `TURN_TOLL "대왕판교톨게이트"`, `TURN_ARRIVE` |
| 경유지 | `getWaypointInfo` → `onWayPointInfo` | ✅ | `[]` |
| 즐겨찾기 | `getBookmarkInfo` → `onBookmarkInfo` | ✅ | HOME, OFFICE 슬롯(비어 있음) |
| 최근 목적지 | `getRecentDestinationInfo` → `onRecentDestinationInfo` | ✅ | 서울역(이번 조사로 추가됨) |
| 충전 사업자 목록 | `getChargerOperatorInfo` → `onChargerOperatorInfo` | ✅ | 현대 E-pit, GS차지비, 환경부, 한국전력 등 수십 개 |
| 도로 제한속도 | (자동) `onCurrentRoadSpeedLimit` | ✅ | **1초마다** 수신. 정차 상태라 `0` |
| 경로 요청 | `requestRoute(RouteInfo)` | ✅ | 기존 경로 `onRouteCancelled` → `onRouteStarted`(소요시간 2820 s, 23000 m, 통행료 700원) → `onNavigationStatus ROUTE_DRIVE` → `onDrivingInfo` → `onTBTInfo` |
| 예상 주행 정보 | `requestTrip(TripRequest)` → `onTripInfo` | ✅ | 거리 23799 m, ETA 2885 s, **도착 시 예상 SoC 96%**, 에너지 소모 3023 (Wh 추정), 구간별 혼잡도 비율 목록 |
| 주행 정보 | (자동) `onDrivingInfo` | ✅ | 목적지 이름, 소요시간, 거리, 충전소 여부, 목표 SoC, 충전 계획 |
| 경로 취소 | `cancelRoute()` | ✅ | `onRouteCancelled`, `onNavigationStatus FREE_DRIVE` |
| 경로 옵션 | `changeRouteOption` | (미호출) | 옵션: RECOMMENDED, SHORT_DISTANCE, SHORT_TIME, FREE, HIGHWAY, COMFORTABLE, SCHOOL_ZONE |
| 그 외 이벤트 | `onCameraAlert`, `onCautionPointInfo`, `onWayPointArrived`, `onDestinationArrived`, `onError` 등 | 미수신 | 실제 주행 시뮬레이션이 있어야 발생할 것으로 보임 |
| 기타 API | `requestRouteWithWaypoints`, `addWaypoint`, `removeWaypoint`, `requestReRoute`, `requestNavigationScreen` | (미호출) | |

**주의**:
- `requestRoute`는 **현재 경로를 덮어씀**(사용자 경로가 취소됨).
- 경로 안내가 시작되면 내비 앱이 화면 앞으로 나오고, 우리 앱은 백그라운드로 밀려 Android가 프로세스를 얼림(이벤트가 멈춤). 실제 앱은 화면 설계와 서비스 구조를 이 점을 고려해 정해야 함.

---

## 4. 아이디어 기획 관점 시사점

### 4.1 지금 확실히 실동작하는 재료
- **EV + 내비 결합**: SoC, 용량, 주행가능거리, 충전 상태(차량) + 도착 시 예상 SoC, 에너지 소모, 충전 사업자, 경로 혼잡도(내비).
  - → 충전·주행 계획, 장거리 여행 보조 등.
- **탑승자·실내 상태**: 좌석 점유, 벨트 체결(5석), 도어·창문·잠금, 공조, 열선·통풍, 조명.
  - → 탑승자 인식 기반 실내 경험, 하차 전 점검(창문·도어·조명), 뒷좌석 승객 알림 등.
- **경로 맥락**: 목적지, 남은 거리·시간, TBT, 통행료, 도로 제한속도, 혼잡 구간.
  - → 도착 시각 기반 알림, 경로 상 정보 큐레이션 등.

### 4.2 피해야 하거나 대안이 필요한 것
- **실시간 차속**: ~~API 없음~~ → **정정(7.5)** AAOS `PERF_VEHICLE_SPEED`로 가능(CAR_SPEED 런타임 권한). **급가속·급감속**: 전용 값은 없고 차속 미분으로 추정해야 함. 가속 페달 값은 어느 API에도 없음(7.4).
- **연료차 데이터**(연비, 주유량, 오일 경고): 에뮬레이터가 EV라 미지원.
- **트립 거리·시간, 전비 통계**: 미지원 → 앱이 시간·거리를 직접 계산해야 함.
- **차량 제어**(공조 켜기, 창문 열기 등): `accessType`은 READ_WRITE로 나오지만 Vehicle Control SDK가 "준비 중"이라 앱 심사 통과가 불확실함.
- **TTS / STT / LLM / FusedLocation**: 아티팩트(`TextToSpeech`, `SpeechToText`, `LLM`, `fused-location-*`)와 퍼미션이 이미 저장소와 에뮬레이터에 있음. 하지만 공식 상태는 "준비 중" → 설명회(10.28)에서 공개 일정 확인 필요.

### 4.3 추가로 확인할 것 (다음 단계)
1. ~~값을 바꾸는 방법~~ → 6장에서 해결(Virtual Console).
2. ~~모의 주행~~ → 없음(6.4). 설명회에서 "경로 주행 시뮬레이션 수단"이 있는지 질문할 것.
   - 도착, 과속 카메라, 제한속도 변화 이벤트를 시연하려면 필요함.
3. `PRIVATE_CAR_*` 퍼미션(타이어 공기압, 경고등, capability 전체 목록) 승인 가능 여부 → Playground 또는 설명회 문의.
4. 좌석 점유 반전, 시트 위치 미반영이 에뮬레이터 버그인지 → Playground 문의 대상.

---

## 5. 재현 방법

```bash
export JAVA_HOME="E:\\AndroidStudio\\jbr"
cd /c/Users/H/Desktop/hyundai-app/PleosDataProbe && ./gradlew assembleDebug
A=/e/Android/Sdk/platform-tools/adb.exe
$A install -r app/build/outputs/apk/debug/app-debug.apk
$A shell am start -n com.example.pleosprobe/.MainActivity
# 약 100초 소요. 경로 안내 중 내비가 앞으로 나오면 앱이 얼므로, 위 am start를 몇 초마다 반복해서 앞으로 유지
$A shell run-as com.example.pleosprobe --user 10 cat files/vehicle.tsv > results/vehicle.tsv
$A shell run-as com.example.pleosprobe --user 10 cat files/navi.tsv   > results/navi.tsv
```

- 에뮬레이터 앱은 **사용자 10**(드라이버 프로필)에서 실행되므로 `run-as`에 `--user 10`이 필요함.
- 조사 앱은 마지막에 `cancelRoute()`로 경로를 지움. 다만 "최근 목적지"에 서울역이 남음.

---

## 6. 차량 값 바꾸기와 리스너 검증 (2026-10-22 추가 조사)

### 6.1 값을 바꾸는 수단: Pleos Connect Virtual Console
- 에뮬레이터 안의 VHAL 서비스(`ai.pleos.hardware.vehicle@V2-sdv-service`)가 **포트 8088**에서 웹 콘솔을 띄우고 있음. 시스템 앱 `ai.pleos.hardware.vehicle.emulator`(VehicleEmulatorApp)는 이 페이지를 보여주는 WebView임.
- PC에서 여는 방법:
  ```bash
  /e/Android/Sdk/platform-tools/adb.exe forward tcp:8088 tcp:8088
  ```
  그다음 브라우저로 `http://localhost:8088/` 접속.
- 화면 구성: 3D 고속도로 주행 화면, ACCEL/BRAKE 패드, 핸들, 기어(P/R/N/D), Systems(하이패스, 와이퍼, 앰비언트), Climate, Body(도어, 창문, 트렁크), "Emulation Controls"(63개 항목).
- **HTTP API로 자동화 가능**: `PUT /vehicle/<endpoint>?<파라미터>`
  - 상태 조회: `GET /vehicle/state`
  - 항목 목록: `GET /vehicle/catalog`(사본은 `PleosDataProbe/results/virtual_console_catalog.json`)
- ⚠ **브라우저 세션이 살아 있어야 함**: 콘솔 페이지는 `POST /browser/session?client=<id>`를 1초마다 보냄. 끊기면 모든 쓰기가 `503 {"error":"emulator_inactive"}`가 됨.
  - 브라우저 탭이 백그라운드로 가서 멈춰도 같은 증상.
  - 스크립트만으로 쓰려면 아래 하트비트를 따로 돌릴 것:
    ```bash
    while true; do curl -s -X POST "http://localhost:8088/browser/session?client=cli" >/dev/null; sleep 1; done
    ```
- `car_service` 셸 명령(`inject-vhal-event` 등)은 user 빌드라 막혀 있음(`requires non-user build`). 콘솔이 유일한 공식 수단으로 보임.
- 콘솔에서 바꾼 값은 에뮬레이터 화면에도 그대로 반영됨.
  - 클러스터에 속도·기어·주행가능거리, "도어 열림" 경고, 하단 공조 온도 등.
  - 캡처: `PleosDataProbe/results/sim_cluster.png`

### 6.2 리스너 검증 결과
- 방법: 조사 앱을 리스너 모드로 실행하고, 값을 하나씩 PUT한 뒤 2.5초 안에 들어온 콜백을 확인함.
  - 실행: `am start -n com.example.pleosprobe/.MainActivity --ez listen true`
  - 이 모드는 모든 `register*` 리스너를 영역 조합별로 등록함(517개 성공, 실패 10개는 🔒 TirePressure·Telltales).
  - 스크립트: `PleosDataProbe/tools/vtest.sh`

| 바꾼 값 (PUT 요청) | SDK 리스너 수신 | 비고 |
|---|---|---|
| 기어 `gear?gear=Drive` | ✅ `Gear.onSelected GEAR_DRIVE` | |
| SoC `ev-battery?field=soc&value=55` | ✅ `onEvBatteryStateOfChargeUpdated 55` | 배터리 잔량(Wh)은 77400 그대로 |
| 속도·가속 `controls?speed=60&accelerator=30` | — (Pleos SDK에 속도 API 없음) → **AAOS `PERF_VEHICLE_SPEED` 16.67 m/s 수신(7.5)** | 클러스터에는 64 km/h 표시됨. 가속 페달은 AAOS에도 없음 |
| 핸들 `controls?...&steering=30` | ✅ `onSteeringWheelAngleUpdated -30.0` | **부호 반대** |
| 브레이크 `controls?brake=50` | ✅ `onBrakePedalStatusUpdated true` | 밟음 여부만 받음 |
| 방향지시등 `turn-signal?mode=BlinkLeft` | ✅ `onTurnSignalStateUpdated LEFT` | |
| 공조 전원 / A/C `hvac?power=…` / `ac=…` | ✅ `onHvacPowerStateUpdated`, `onHvacAcStateUpdated` | 영역: 1·2열 전체 |
| 운전석 온도 `hvac?driver_temp=24` | ✅ `onHvacTemperatureUpdated 24.0` (화씨 76.0도 함께) | ROW_1_LEFT |
| 풍량 `hvac?fan_speed=5` | ✅ `onHvacFanSpeedUpdated 5` | 1열 좌+우 |
| 운전석 열선 `hvac?driver_seat=2` | ✅ `onHvacSeatTemperatureUpdated 2` | |
| 핸들 열선 `steering-wheel-heat?level=2` | ✅ `onHvacSteeringWheelHeatStateUpdated 2` | |
| 앞 좌 도어 열기 `body?door_fl=true` | ✅ `Door.onPositionUpdated 1` | 클러스터 "도어 열림" 경고 |
| 전체 잠금 해제 `body?doors_locked=false` | ✅ `onDoorLockStateUpdated false` | 도어 4개 복합 영역 |
| 앞 좌 창문 50% `body?window_fl=50` | ✅ `onWindowPositionUpdated 5` | 0~100이 0~10으로 변환됨 |
| 창문 잠금 `window-lock?locked=true&area=all` | ✅ `onWindowLockStateUpdated true` | |
| 하향등 / 상향등 `lights?feature=…` | ✅ LowBeam·TailLamp·HighBeam `ON` | |
| 실내등 `body?cabin_light=true` | ✅ `CabinLightState ON` | 독서등 4개도 함께 ON으로 옴 |
| 와이퍼 `body?front_wiper=Low` | ✅ `onWiperStateUpdated ON` | |
| 앰비언트 색 `body?ambient_color=#aa00ff` | ✅ `onBrightnessColorUpdated 11141375` (=0xAA00FF) | |
| 앰비언트 밝기 `body?ambient_brightness=80` | ❌ 수신 없음 | |
| 누적거리 `odometer?traveled_distance_km=1234` | ✅ `onDistanceDrivenUpdated 1234.0` | **줄일 수 없음**(`traveled_distance_must_not_decrease`) |
| 주행가능거리 `odometer?remaining_range_m=300000` | ✅ `onRangeRemainingChanged 300000.0` | 클러스터 300 km |
| 충전구 `ev-battery?field=charge_port_open&value=true` | ✅ `onEvChargePortOpenUpdated true` | |
| 충전 시작 `ev-battery?field=charge_switch&value=true` | (해당 리스너 없음) | 충전 중이어도 SoC는 자동으로 오르지 않음 |
| 크루즈 목표 `driving-mode?target_speed_mps=25` | ✅ `onCruiseControlTargetSpeedUpdated 25.0` | |
| 크루즈 상태 `driving-mode?ad_mode_state=…` (2026-10-29 추가) | ⚠ `onCruiseControlStateUpdated` 수신. 매핑: `ready`·`longitudinal`·`lateral` → `ENABLED`, `full_autonomous` → `ACTIVATED`, `not_ready` → `null` | 값이 같은 상태끼리 바꾸면 콜백 없음(ready→longitudinal 등). 아래 6.2.1 참고 |
| 좌석 점유 `seat?kind=occupied&value=true&area=driver` | ⚠ `onSeatOccupancyUpdated false` | **값이 반대로 옴**(콘솔 true → SDK false) |
| 시트 슬라이드 `seat?kind=position…` / `kind=move…` | ❌ SDK 값 0 그대로 | 콘솔 값만 바뀜 |

#### 6.2.1 크루즈 해제 감지 실측 (2026-10-29)
- 목적: '크롱'(크루즈 해제 지점 집계) 아이디어의 핵심 신호 확인.
- 콘솔 크루즈 상태는 실제로는 자율주행 제어 모드(`SDV_ADAS_AD_CONTROL…AD_MODE_STATE`)이고, 선택지는 `not_ready / ready / longitudinal / lateral / full_autonomous` 5개뿐(`virtual_console_catalog.json`).
- SDK `CruiseControl` 열거형(Vehicle 2.3.2 `javap` 확인): `OTHER, ENABLED, ACTIVATED, USER_OVERRIDE, SUSPENDED, FORCED_DEACTIVATION_WARNING`.
- 결과:
  - 상태 변경 리스너는 동작함(`ENABLED`↔`ACTIVATED`↔`null` 전환 시 콜백).
  - **해제 관련 값(`USER_OVERRIDE`, `SUSPENDED`, `FORCED_DEACTIVATION_WARNING`)은 콘솔로 만들 수 없음** → 실차에서 크루즈 해제 시 어떤 값이 오는지 미확인.
  - 일반 크루즈에 해당할 것으로 보이는 `longitudinal`(종방향 제어)이 `ACTIVATED`가 아니라 `ENABLED`로 옴(추정: 에뮬레이터 매핑 문제일 수 있음).
  - `ACTIVATED` 상태에서 `controls?brake=50`을 넣어도 크루즈 상태는 바뀌지 않음(브레이크 콜백만 옴). 콘솔은 "브레이크로 해제"를 시뮬레이션하지 않음.
- 시연 의미: `full_autonomous`→`ready`(=`ACTIVATED`→`ENABLED`)를 "해제"로 간주하면 시연 화면은 만들 수 있음. 단 실차 의미와 같은지는 문의 필요.

### 6.3 시연·개발에 주는 의미
- **시연 영상 가능**: 기어 D → 속도 표시 → 문 열림·창문·공조 변화 같은 시나리오를 콘솔 HTTP API 스크립트로 재현 가능하게 만들 수 있음(`vtest.sh` 참고).
- **차속은 Pleos SDK로는 못 받지만 AAOS로 받음**(7.5 정정): 콘솔에서 속도를 바꾸면 `PERF_VEHICLE_SPEED` 콜백이 들어옴. 기어(D), 브레이크, CarUxRestrictions는 보조 수단.
- 누적·잔량 값은 자동으로 시뮬레이션되지 않음. 주행거리, SoC, 주행가능거리는 시나리오에 맞게 직접 값을 넣어야 함.

### 6.4 모의 주행(내비 위치 이동) 확인 결과 — 없음
- **내비 설정**: 차량 설정 → 내비게이션 끝까지 확인. 충전소, 경로(EV 경로 계획, 유료도로·어린이보호구역 우회), 과속 경고, 버전 정보(3.3.2-rc5-hotfix.3), 데이터 관리만 있음. **모의 주행 메뉴 없음**.
- **GPS 주입**: `adb emu geo fix`로 Android GPS는 서울시청으로 바뀜(`dumpsys location`과 Pleos FusedLocation 서비스도 수신). 하지만 **NaviHelper 현재 위치는 판교 부근 좌표에 고정**됨.
- **콘솔 주행**: D + 80 km/h로 1분 이상 경로 안내를 해도 위치와 남은 거리(23000 m)가 변하지 않음.
- → 도착, 경유지 도착, 과속 카메라, 제한속도 변화 이벤트는 현재 에뮬레이터에서 재현 불가.
- 참고: FusedLocation SDK는 GPS 주입을 따르므로, 그 SDK가 공개되면 위치 기반 시연은 `geo fix`로 가능할 수 있음(추정).
- 지도 타일이 표시되지 않음(빈 배경). 경로·TBT 데이터는 정상.

### 6.5 원복 상태
조사 후 콘솔 값을 기본값으로 되돌렸고 경로를 취소했으며(FREE_DRIVE), GPS를 원래 좌표로 되돌림. 예외는 다음 두 가지:
- **누적거리 1234 km**: 감소가 금지돼 되돌릴 수 없음. 콜드 부팅 시 초기화되는지는 미확인.
- 콘솔 첫 접속 시 콘솔 기본값이 적용되면서 일부 값이 처음 조사(2장) 때와 달라짐. 예: 공조 전원·A/C ON, 운전석 22℃.

### 6.6 실습 퀘스트용 추가 실측 (2026-10-23)
방법은 6.2와 같음(리스너 모드 517개 등록 → 콘솔 PUT → 2.5초 내 콜백 확인). 리스너가 안 온 항목은 값을 바꿔 둔 채 조회 모드(getter)로 다시 읽음.

| 바꾼 값 (PUT 요청) | 리스너 | 조회(getter) | 비고 |
|---|---|---|---|
| 앞유리 제상 `hvac?front_defroster=true` | ✅ `HvacDefrosterState[FRONT_WINDSHIELD] true` | — | |
| 뒷유리 제상 `hvac?rear_defroster=true` | ✅ `HvacDefrosterState[REAR_WINDSHIELD] true` | — | |
| 내기순환 `hvac?recirc=true/false` | ✅ `onHvacAutoRecirculationStateUpdated true/false` | — | 콜백 이름은 "Auto"지만 내기/외기 전환에 반응함. 영역 1·2열 전체 |
| A/C `hvac?ac=true` | ✅ `onHvacAcStateUpdated true` | — | |
| 풍향 `hvac?driver_direction=4&passenger_direction=4` | ❌ 콜백 없음 | ✅ `getHvacFanDirection[ROW_1_LEFT+ROW_1_RIGHT] = DEFROST` | 리스너 등록은 성공. 값은 SDK에 반영됨 → **조회로 확인해야 함** |
| 와이퍼 모드 `body?front_wiper=Auto/Low/High/Off` | ⚠ `WiperState`만 ON/OFF로 옴(Auto·Low·High 모두 ON) | ✅ `getCurrentWiperSwitch[FRONT_WINDSHIELD] = AUTO` | 모드(AUTO 여부)는 **조회로 확인해야 함** |
| 비상등 `lights?feature=hazard&value=true` | ❌ 콜백 없음 | ❌ 조회 API 없음 | 방향지시등 상태도 `OFF` 그대로. **앱에서 비상등은 확인 불가** |

**실습 앱 구현에 주는 의미**
- 김서림 퀘스트의 3단계(앞유리 제상 → A/C → 외기)는 **전부 리스너로 실시간 확인 가능**.
- 풍향·와이퍼 AUTO는 퀘스트 진행 중(주차 상태)에만 1초 간격 조회로 확인. 앱 개발 규정상 백그라운드 폴링은 최소화해야 하므로 퀘스트 화면이 떠 있을 때만.
- 비상등은 퀘스트 후보에서 제외.
- 실측 후 콘솔 값은 기본값으로 되돌림(제상·A/C·내기 OFF, 풍향 Face, 와이퍼 Off, 비상등 Off).

---

## 7. AAOS 표준 Car API·제어 권한 실측 (2026-10-23)

> 실측 2회: 1차(같은 날 앞선 세션, `setHvacAc(true)`로 실제 값 변경까지 확인)와 2차(이 절의 기준. 조회 범위를 넓히고 쓰기는 현재 값 그대로만 호출). 두 결과는 서로 모순되지 않음.
> 원시 결과: `PleosDataProbe/results/aaos.tsv`(2차 전체, 488행), `PleosDataProbe/results/aaos_xref.tsv`(VHAL 덤프와 대조해 미지원/퍼미션 부족을 가른 표)

### 7.1 왜 다시 조사했나
Pleos 공식 문서 [SDK API 정책](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/pleos-connect-sdk-api) 원문:
- "표준 데이터 | AAOS Car API | 속도, 기어 상태, 외기 온도, 연료 레벨 등 / 특화 기능 | Pleos Connect SDK | 제조사 전용 시트 제어, 특화 라이팅, 고유 보안 인증 등"
- "표준 우선 정책: 동일하거나 유사한 기능의 API가 AAOS 표준에 이미 존재하는 경우, 해당 표준 API 사용을 권장하며 Pleos Connect SDK에서는 중복 기능을 제공하지 않습니다."

→ 0장의 "차속 API 없음"은 Pleos SDK만 본 결론이었으므로 AAOS 쪽을 실측함.

### 7.2 방법
- `PleosDataProbe`에 `--ez aaos true` 모드 추가(`app/src/main/java/com/example/pleosprobe/AaosProbe.kt`, `build.gradle.kts`에 `useLibrary("android.car")`).
- 조회 대상 ID: 에뮬레이터의 `android.car.VehiclePropertyIds` static int 상수 **207개**(SDK 34 stub jar는 159개이지만 기기 런타임 클래스에는 @SystemApi 상수가 더 있음) + android-36.1 jar에만 있는 상수 **91개**(초음파 `ULTRASONICS_*` 6개, `ACCELERATOR_PEDAL_COMPRESSION_PERCENTAGE` 등을 raw int로 시도) + `dumpsys car_service --hal PropertyHalService`에 나오지만 공개 상수가 없는 **벤더 ID 140개**.
- 각 ID에 `getCarPropertyConfig` → config가 있으면 영역별 `getProperty`; config가 없으면 `getProperty(id, 0)`의 예외 메시지를 그대로 기록해 **미지원(IllegalArgumentException)**과 **퍼미션 부족(SecurityException)**을 구분.
- config가 있는 47개 전부에 `registerCallback`(CONTINUOUS는 최대 10 Hz) 등록 → 콘솔 PUT으로 값 변경 → `PleosLsn` 로그 확인.
- 쓰기: AAOS `setProperty`를 **현재 값 그대로**(HVAC_AC_ON, HVAC_FAN_SPEED, HVAC_TEMPERATURE_SET, HVAC_SEAT_TEMPERATURE, SEAT_FORE_AFT_POS, RANGE_REMAINING), Pleos SDK `setHvacAc`도 현재 값(false) 그대로.
- 퍼미션: 매니페스트에 `android.car.permission.*` 35개와 `pleos.car.permission.CONTROL_CAR_CLIMATE` 선언 → `pm grant --user 10` → 앱이 `PackageInfo.requestedPermissionsFlags`로 실제 부여 상태 기록.

```bash
export JAVA_HOME="E:\\AndroidStudio\\jbr"
cd /c/Users/H/Desktop/hyundai-app/PleosDataProbe && ./gradlew assembleDebug
A=/e/Android/Sdk/platform-tools/adb.exe
$A install -r app/build/outputs/apk/debug/app-debug.apk
for p in CAR_SPEED CAR_ENERGY; do $A shell pm grant --user 10 com.example.pleosprobe android.car.permission.$p; done
$A shell am start -n com.example.pleosprobe/.MainActivity --ez aaos true --ez write true --ez pleosset true
sleep 14; $A shell run-as com.example.pleosprobe --user 10 cat files/aaos.tsv > results/aaos.tsv
# 속도 콜백 확인 (하트비트 켜 둔 상태에서)
$A logcat -c; curl -s -X PUT "http://localhost:8088/vehicle/controls?speed=60"; sleep 3; $A logcat -d -s PleosLsn:I | grep PERF_VEHICLE_SPEED
```

### 7.3 퍼미션 (에뮬레이터 실측, `pm grant --user 10` 출력과 앱의 granted 플래그)

| 퍼미션(`android.car.permission.`) | 보호 수준(기기) | `pm grant` 결과 | 앱에서 본 상태 |
|---|---|---|---|
| `CAR_SPEED`, `CAR_ENERGY` | **dangerous** | 성공(exit 0) | GRANTED |
| `CAR_INFO`, `CAR_POWERTRAIN`, `CAR_EXTERIOR_ENVIRONMENT`, `CAR_ENERGY_PORTS`, `READ_CAR_DISPLAY_UNITS`, `CONTROL_CAR_DISPLAY_UNITS` | normal | (설치 시 자동) | GRANTED |
| `CONTROL_CAR_CLIMATE`, `CONTROL_CAR_SEATS`, `CONTROL_CAR_DOORS/WINDOWS/MIRRORS`, `READ_CAR_STEERING`, `CAR_MILEAGE`, `CAR_TIRES`, `CAR_DYNAMICS_STATE`, `CAR_EXTERIOR_LIGHTS`, `READ_CAR_INTERIOR_LIGHTS`, `READ_WINDSHIELD_WIPERS`, `READ_ADAS_STATES/SETTINGS`, `CAR_DRIVING_STATE`, `CAR_IDENTIFICATION`, `CAR_EPOCH_TIME`, `CAR_VENDOR_EXTENSION`, `CAR_ENGINE_DETAILED`, `CONTROL_GLOVE_BOX`, `CONTROL_STEERING_WHEEL`, `READ_DRIVER_MONITORING_STATES`, `CAR_DIAGNOSTICS`, `PRIVILEGED_CAR_INFO` | **signature\|privileged** | `SecurityException: Permission … is not a changeable permission type` (exit 255) | DENIED |
| `READ_ULTRASONICS_SENSOR_DATA`, `READ_CAR_SEATS`, `READ_IMPACT_SENSORS` | 기기에 정의 없음 | `IllegalArgumentException: Unknown permission: android.car.permission.READ_ULTRASONICS_SENSOR_DATA` | NOT_DEFINED / DENIED |
| `pleos.car.permission.CONTROL_CAR_CLIMATE` | normal | (설치 시 자동) | GRANTED |

**실차·심사 기준과 다른 점(중요)**
- `pm grant`는 adb로 개발자가 강제 부여한 것. 실차에서는 dangerous 퍼미션을 **사용자에게 런타임 다이얼로그로 요청**해야 하고, 거부되면 못 읽음.
- signature|privileged 퍼미션은 adb로도 못 주고(위 메시지), 실차에서도 OEM 서명·privileged 앱만 가능 → **3rd-party 앱은 영구히 불가**.
- Pleos 공식 [Overview](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/): "퍼미션은 Pleos App Market과 사전 협의 및 승인된 범위 내에서만 사용 가능 … 미승인 퍼미션 선언 시 앱 심사 불통과". AAOS 퍼미션(`CAR_SPEED` 등)이 이 승인 절차의 대상인지는 문서에 명시가 없음 → **미확인**(설명회 질문 사항).

### 7.4 AAOS 프로퍼티 조회 결과

| 구분 | 개수 | 예 |
|---|---|---|
| **지원(값 읽음)** | 47 | 아래 표 |
| **퍼미션 부족**(SecurityException, VHAL에는 존재) | 301 (표준 165 + 벤더 136) | `HVAC_AC_ON → requires android.car.permission.CONTROL_CAR_CLIMATE`, `PERF_STEERING_ANGLE → requires READ_CAR_STEERING`, `PERF_ODOMETER → requires CAR_MILEAGE`, `TURN_SIGNAL_STATE`/`HAZARD_LIGHTS_STATE → requires CAR_EXTERIOR_LIGHTS`, `SEAT_FORE_AFT_POS → requires CONTROL_CAR_SEATS`, `CRUISE_CONTROL_TARGET_SPEED → requires READ_ADAS_STATES`, `INFO_VIN → requires CAR_IDENTIFICATION`, 벤더 ID 136개 전부 `→ requires CAR_VENDOR_EXTENSION` |
| **미지원**(IllegalArgumentException, VHAL에 없음) | 약 90 | `ENGINE_RPM`, `IGNITION_STATE`: "property ID is not in carPropertyConfig list, and so it is not supported" / `ULTRASONICS_SENSOR_MEASURED_DISTANCE`(0x18410c25), `ACCELERATOR_PEDAL_COMPRESSION_PERCENTAGE`(0x1160030f): "The property: … is unsupported" |

일반 앱이 받을 수 있는 퍼미션(normal + dangerous)으로 읽힌 47개:

| 데이터 | 프로퍼티 | 값(에뮬레이터) | 변화 모드·주기 | 필요 퍼미션 |
|---|---|---|---|---|
| **차속** | `PERF_VEHICLE_SPEED`, `PERF_VEHICLE_SPEED_DISPLAY` | 0.0 (m/s) | CONTINUOUS 10~20 Hz | `CAR_SPEED`(dangerous) |
| 기어 | `GEAR_SELECTION`, `CURRENT_GEAR` | 4(=P), D=8 | ONCHANGE | `CAR_POWERTRAIN` |
| 주차 브레이크 | `PARKING_BRAKE_ON`, `PARKING_BRAKE_AUTO_APPLY` | false | ONCHANGE | `CAR_POWERTRAIN` |
| 외기 온도 | `ENV_OUTSIDE_TEMPERATURE` | 25.0 ℃ | CONTINUOUS 1~2 Hz | `CAR_EXTERIOR_ENVIRONMENT` |
| 휠 틱 | `WHEEL_TICK` | [0,100000,…] (속도 바꿔도 **불변**) | CONTINUOUS | `CAR_SPEED` |
| EV 배터리 | `EV_BATTERY_LEVEL` 77400 Wh, `EV_CURRENT_BATTERY_CAPACITY`, `INFO_EV_BATTERY_CAPACITY` | | | `CAR_ENERGY`(dangerous) / `CAR_INFO` |
| **순간 충·방전 전력** | `EV_BATTERY_INSTANTANEOUS_CHARGE_RATE` | 0.0 (콘솔에 바꿀 항목 없음) | CONTINUOUS 1~10 Hz | `CAR_ENERGY` |
| 충전 | `EV_CHARGE_STATE` 2, `EV_CHARGE_TIME_REMAINING` 0, `EV_CHARGE_PORT_OPEN/CONNECTED`, `EV_CHARGE_SWITCH`, `EV_CHARGE_CURRENT_DRAW_LIMIT` 12.5, `EV_CHARGE_PERCENT_LIMIT` 50 | | | `CAR_ENERGY` / `CAR_ENERGY_PORTS` |
| 회생제동 | `EV_REGENERATIVE_BRAKING_STATE` 2, `EV_BRAKE_REGENERATION_LEVEL` 0, `EV_STOPPING_MODE` 1 | | | `CAR_ENERGY` |
| 주행가능거리 | `RANGE_REMAINING` | 500000 m | CONTINUOUS | `CAR_ENERGY` (쓰기는 `ADJUST_RANGE_REMAINING` 필요) |
| 연료 | `FUEL_LEVEL` 15000, `FUEL_LEVEL_LOW` false, `INFO_FUEL_CAPACITY` 15000, `INFO_FUEL_TYPE` [10] | | | `CAR_ENERGY` / `CAR_INFO` |
| 차량 정보 | `INFO_MAKE` "Hyundai 42dot", `INFO_MODEL` "Connect", `INFO_MODEL_YEAR` 2024, `INFO_EXTERIOR_DIMENSIONS`, `INFO_DRIVER_SEAT`, 포트 위치 | | STATIC | `CAR_INFO` |
| 표시 단위 | `DISTANCE/FUEL_VOLUME/TIRE_PRESSURE/EV_BATTERY/VEHICLE_SPEED/HVAC_TEMPERATURE_DISPLAY_UNITS` | | | `READ_CAR_DISPLAY_UNITS` |
| 기타 | `NIGHT_MODE`, 하이패스 카드 `ELECTRONIC_TOLL_COLLECTION_CARD_STATUS/TYPE`, `GENERAL_SAFETY_REGULATION_COMPLIANCE` | | | `CAR_INFO` 등 |

**없는 것(일반 앱 기준, 근거 포함)**
- **초음파·주차 센서**: API 34 `VehiclePropertyIds`에 `ULTRASONICS_*` 상수 없음(javap로 확인). 기기 퍼미션 `READ_ULTRASONICS_SENSOR_DATA` "Unknown permission". android-36.1 상수 값(406916133 등)으로 직접 호출 → "is unsupported". VHAL 덤프 312개 프로퍼티에도 없음. 공식 AOSP 레퍼런스 [VehiclePropertyIds](https://developer.android.com/reference/android/car/VehiclePropertyIds) 원문: "Required Permission: Signature|Privileged permission Car.PERMISSION_READ_ULTRASONICS_SENSOR_DATA to read property." → 최신 OS에서도 **3rd-party 불가**.
- **가속 페달**: API 34 상수 없음. android-36 `ACCELERATOR_PEDAL_COMPRESSION_PERCENTAGE` → 에뮬레이터 "unsupported". AOSP 원문: "Dangerous permission Car.PERMISSION_READ_CAR_PEDALS to read property."(최신 OS에서는 가능성 있으나 현재 플랫폼 34.1에는 없음). 콘솔에는 `accelerator_pct`(DDS 토픽 `SDV_MOTION_ACCELERATOR_BASE_ACCELERATORBASE_ACTUAL_PEDAL`)가 있지만 앱용 API로 노출되지 않음(콘솔 `accelerator=30` PUT 시 AAOS 콜백 0건). Pleos SDK는 브레이크 on/off만(`Brake.getCurrentBrakePedalStatus`).
- **가속도(종·횡), 요레이트**: 어느 쪽에도 상수 없음. 차속(10~20 Hz) 미분으로 종가속도 추정, 조향각(Pleos `SteeringWheel`, 6.2)과 차속으로 횡가속도 추정만 가능.
- **조향각(AAOS)**: `PERF_STEERING_ANGLE → requires READ_CAR_STEERING`(signature). 단 **Pleos SDK `SteeringWheel.getCurrentSteeringWheelAngle`/리스너로는 받음**(2.1, 6.2) → 정책 문서의 "특화 기능은 Pleos SDK" 사례.
- **공조·시트·창문·도어·조명 상태(AAOS)**: 읽기조차 `CONTROL_CAR_CLIMATE`/`CONTROL_CAR_SEATS`/`CONTROL_CAR_DOORS`… 가 필요(signature) → 3rd-party는 **Pleos SDK로만** 읽을 수 있음(2장, 6장 실측과 일치).
- **트립(거리·시간)**: `PERF_ODOMETER → requires CAR_MILEAGE`(signature). 콘솔 state가 벤더 프로퍼티 `TRIP_DRIVING_DISTANCE`(0x21610451)·`TRIP_DRIVING_TIME`(0x21410450)을 노출하지만 AAOS로는 `CAR_VENDOR_EXTENSION`, Pleos `Odometer.getTrip*`은 미지원(2.1). 누적거리는 Pleos `getCurrentDistanceDriven`으로만 받음.
- **전비(km/kWh)**: Pleos `get*EvEfficiency` 미지원(2.2). AAOS에는 전비 프로퍼티 자체가 없음. 순간 전력 `EV_BATTERY_INSTANTANEOUS_CHARGE_RATE`와 차속·거리로 앱이 계산해야 하며, 에뮬레이터는 이 값이 0.0 고정이라 **시연은 미확인**.

### 7.5 차속 콜백 (실측, 2차)
`registerCallback(PERF_VEHICLE_SPEED, 10 Hz)` 등록 후 콘솔 PUT → 3초 안에 들어온 `PleosLsn` 로그:

| PUT | 수신 | 비고 |
|---|---|---|
| `gear?gear=Drive` | `GEAR_SELECTION 8` 1회 + `PERF_VEHICLE_SPEED 0.0` 21회 | 정차 시에도 약 7 Hz로 계속 옴 |
| `controls?speed=60` | `PERF_VEHICLE_SPEED 16.666668`(m/s = **60 km/h**) 20회, `PERF_VEHICLE_SPEED_DISPLAY` 동일 | 1차 실측도 같은 값 |
| `controls?speed=60&accelerator=30` | 속도만, **페달 이벤트 없음** | |
| `controls?…&steering=30` | **조향 이벤트 없음**(READ_CAR_STEERING 필요) | Pleos SDK 리스너로는 수신(6.2) |
| `controls?speed=20&brake=50` | `PERF_VEHICLE_SPEED 5.555556`(20 km/h) | 브레이크 자체 이벤트 없음(Pleos `Brake` 리스너로는 수신) |
| 원복 `speed=0`, `gear=Park` | `0.0`, `GEAR_SELECTION 4` | |

→ **차속은 AAOS API로 실시간 수신 가능.** 단위는 m/s. 급가속·급감속 판정은 앱이 미분해서 계산(콘솔은 속도를 계단식으로 바꾸므로 에뮬레이터 시연 시 "가속도"는 비현실적인 값이 나옴 → 시연 스크립트에서 속도를 0.5초 간격으로 단계적으로 올리는 식으로 보완 필요).

### 7.6 제어(쓰기) 실측

| 시도 | 결과(메시지 그대로) |
|---|---|
| AAOS `HVAC_AC_ON`, `HVAC_FAN_SPEED`, `HVAC_TEMPERATURE_SET`, `HVAC_SEAT_TEMPERATURE` | config 자체가 숨겨짐 → `getProperty`: `SecurityException: requires android.car.permission.CONTROL_CAR_CLIMATE` → **setProperty 시도 불가** |
| AAOS `SEAT_FORE_AFT_POS` | `SecurityException: requires android.car.permission.CONTROL_CAR_SEATS` |
| AAOS `RANGE_REMAINING`(READ_WRITE, 현재 값 500000.0 그대로) | `SecurityException: requires android.car.permission.ADJUST_RANGE_REMAINING` |
| Pleos SDK `checkHvacAcCapabilities` | areas = `ROW_1_LEFT+ROW_1_RIGHT+ROW_2_LEFT+ROW_2_CENTER+ROW_2_RIGHT` |
| Pleos SDK `getHvacAcState(area)` | `false` |
| Pleos SDK `setHvacAc(area, false)`(현재 값 그대로, 2차) | **성공 콜백** `(area, false)`; 콘솔 `ac_on` false 유지 |
| Pleos SDK `setHvacAc(area, true)`(1차 실측) | 성공 콜백 + 콘솔 `ac_on` False → True 실제 변경, false로 원복 확인 |

- 시그니처(AAR javap): `setHvacAc(SeatArea, boolean, Function2<SeatArea,Boolean,Unit> onSuccess, Function1<Exception,Unit> onFailed)`. 레퍼런스 상세 페이지는 본문이 비어 있으나 AAR에는 구현이 있고 동작함.
- AOSP 원문([VehiclePropertyIds#HVAC_AC_ON](https://developer.android.com/reference/android/car/VehiclePropertyIds#HVAC_AC_ON)): "Required Permission: Signature|Privileged permission Car.PERMISSION_CONTROL_CAR_CLIMATE to read and write property." → **AAOS 경로로는 3rd-party 제어 불가**가 플랫폼 설계.
- Pleos 원문([Declaring permission](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission)): "CAR_* … 도메인 데이터 조회(get) / CONTROL_CAR_* … 도메인 제어(set)", "매니페스트에는 pleos.car.permission.{상수명} 형식으로 선언합니다." `PleosPermission` 상수는 CAR_*/CONTROL_CAR_*/PRIVATE_CAR_* × 11 도메인 = 33개.
- 결론: **제어는 Pleos SDK 경로로 기술적으로 동작함(에뮬레이터, normal 퍼미션)**. 막는 것은 App Market 승인(Playground 메뉴에 Vehicle Control "준비 중", 2026-10-22)과 심사 기준([앱 심사 가이드](https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-functionality-performance-stability) "차량 상태를 변경하거나 운전 안전에 영향을 줄 수 있는 SDK·API는 별도의 심사 기준"). → 판정은 "불가"가 아니라 **"조건부(승인 필요)"**.

### 7.7 원복
콘솔 `GET /vehicle/state`를 실측 전후로 저장해 비교 → 타임스탬프 외 **차이 0건**(기어 Park, 속도·페달·조향 0, 공조 전원 ON·A/C OFF·풍량 3·22 ℃ 그대로). 누적거리는 6.5의 1234 km 그대로. 하트비트 종료.

### 7.8 Pleos ID·사용자 프로필 공식 근거 (매뉴얼 전수 확인)
[현대 Pleos Connect 매뉴얼](https://ownersmanual.hyundai.com/ivi/Connect-S/AVNT/KOR/Korean/) 68개 페이지를 "동기화 / 다른 차량 / 클라우드 / 프로필" 로 검색한 결과:

| 페이지 | 원문 | 의미 |
|---|---|---|
| [002_Initialsetup_1.html](https://ownersmanual.hyundai.com/ivi/Connect-S/AVNT/KOR/Korean/002_Initialsetup_1.html) 사용자 프로필 설정 | "프로필을 생성하고 마이현대 계정과 연동하면 차량 주행 및 환경에 관한 설정을 개인화하고, 마이현대 앱에서 차량을 제어할 수 있습니다." / "최대 3명의 사용자 프로필을 등록할 수 있습니다." | 프로필은 **마이현대 계정**과 연동. "Pleos ID"라는 용어는 매뉴얼에 없음 |
| [009_Vehiclesettings_13.html](https://ownersmanual.hyundai.com/ivi/Connect-S/AVNT/KOR/Korean/009_Vehiclesettings_13.html) 프로필 설정 | "블루링크 계정 (사양 적용 시) 프로필과 연동된 블루링크 계정을 표시합니다." / "데이터 동기화 설정 (사양 적용 시) 마지막 동기화 시점을 확인합니다. … 데이터를 동기화할 수 있습니다." | **동기화 항목은 있으나 무엇을·어디로 동기화하는지, 다른 차량에서 불러오는지는 기술 없음** |
| 〃 "설정 항목 - 프로필" | 프로필 이미지/이름, 블루링크 계정, 비밀번호, 디지털 키, NFC 카드 키, 프로필 관리, 데이터 동기화 설정 | 시트·공조·내비 즐겨찾기가 프로필에 저장된다는 문장 없음 |
| [007_Vehicleconvenience_8.html](https://ownersmanual.hyundai.com/ivi/Connect-S/AVNT/KOR/Korean/007_Vehicleconvenience_8.html) Pleos App Market | "Pleos App Market을 이용하려면 Pleos Account로 로그인해야 합니다." / "동일한 계정을 사용해 Pleos App Market에서 설치한 이력이 있는 앱은 다른 차량이나 기기에서 이미 설치했더라도 시스템에 재설치할 수 있습니다." | "다른 차량" 언급은 **앱 설치 이력**뿐. 설정·즐겨찾기 프로필 이동은 아님 |

- 개발자 문서(document.pleos.ai sitemap 4,317 URL) 중 3rd-party 차량 앱용 프로필·계정·Pleos ID API는 **없음**(매치되는 URL 0건; `connect-mobile/.../register-profile-guide`는 모바일 앱 개발자 프로필 등록).
- → "다른 차에서 내 프로필 불러오기"가 **기본 기능과 겹치는지는 미확인**(동기화 항목은 존재하나 범위 미기술). 앱이 구현하려면 자체 계정에 설정을 저장했다가 Pleos SDK `set*`/`move*`로 복원해야 하며, 이는 7.6의 제어 승인 조건에 걸림.

### 7.9 NaviHelper 경로 형상(커브·곡률) 공식 근거
[NaviHelper 레퍼런스](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/intro)(v3.0.5) `ai.pleos.playground.navi.data` 데이터 클래스 31개와 상수 enum 15개 전수 확인:
- `TBTInfo` 원문: "data class TBTInfo(val type: TurnType, val distance: Double, val description: String)" — "distance: Double Distance to the upcoming turn (in meters)"
- `CurrentLocationInfo`: "(latitude, longitude, heading, address, routeId, currentRouteIndex)" — 현재 점 하나
- `RoutePointInfo`: "ratioOnRoute: Double … Ratio along the route (0.0–100.0)" — 경로 상 비율, 좌표열 아님
- `CautionPoint`: "(id, type: CautionPointType, remainDistance: Int)" — `DOWNHILL_CAUTION_AREA`, `NARROW_ROAD`, `SPEED_BUMP` 등 **이벤트 종류+남은 거리**만
- `TurnType`에 `TURN_SHARP_L / TURN_SHARP_R`(급회전) 존재
- 곡률(curvature)·커브·경사(slope/gradient)·polyline·링크 좌표 필드: **없음**(sitemap 전체 검색 0건).
- → 멀미 케어의 "경로의 커브 정도"는 NaviHelper에서 **못 받음**. 대안은 (a) TBT 급회전·`CautionPoint`까지 남은 거리로 "곧 커브" 예고, (b) Pleos `SteeringWheel` 조향각 × AAOS 차속으로 **현재** 횡가속도 추정(예고가 아니라 사후 감지).

---

## 8. 아이디어 5개 판정표 (2026-10-23 기준)

범례: ✅ 실측으로 확인 / ⚠ 조건부 / ❌ 불가 / ？ 미확인. "공식 근거"는 7장의 URL·원문을 가리킴.

### 8.1 문콕 방지(DoorGap) — **❌ 불가**
| 필요한 데이터 | Pleos SDK | AAOS API | 실측 결과 | 공식 근거 | 판정 |
|---|---|---|---|---|---|
| 문 옆 장애물까지 거리(초음파) | 도메인 없음(19개: brake…wiper, 7.4) | API 34 상수 없음; API 36 `ULTRASONICS_SENSOR_MEASURED_DISTANCE`는 Signature\|Privileged | raw ID 호출 "is unsupported", 퍼미션 "Unknown permission", VHAL에 없음 | AOSP: "Signature\|Privileged permission Car.PERMISSION_READ_ULTRASONICS_SENSOR_DATA to read property." / Pleos api 패키지 원문에 주차·초음파 도메인 없음 | ❌ |
| 도어 열림 정도(보조) | `Door.getCurrentDoorPosition` ✅ 0~1, 리스너 ✅ | `DOOR_POS → requires CONTROL_CAR_DOORS` | 6.2 `Door.onPositionUpdated 1` | — | ✅(거리 없이는 핵심 기능 불성립) |
| 기어·정차 여부 | `Gear` ✅ | `GEAR_SELECTION` ✅ | 7.5 | — | ✅ |

### 8.2 자동차 스마트 스위치(다른 차에서 내 프로필·즐겨찾기·설정 불러오기) — **⚠ 조건부, 겹침 여부 ？**
| 필요한 데이터 | Pleos SDK | AAOS API | 실측 결과 | 공식 근거 | 판정 |
|---|---|---|---|---|---|
| 차량 사용자 프로필·계정 읽기 | API 없음(sitemap 0건) | 없음 | — | 7.8: 개발자 문서에 프로필/계정 API 없음 | ❌(앱 자체 계정으로 대체) |
| 기본 기능과의 겹침 | — | — | — | 매뉴얼: "마이현대 계정과 연동하면 차량 주행 및 환경에 관한 설정을 개인화", "데이터 동기화 설정 (사양 적용 시)" — 타차량 로드 문장 없음 | ？(부분 겹침 가능성 큼) |
| 시트·공조·설정 읽기 | `Seat.get*Position`, `HVAC.get*` ✅ | ❌(CONTROL_CAR_SEATS/CLIMATE 필요) | 2.4·2.5 ✅. 단 콘솔 시트 이동이 SDK에 미반영(6.2) → 시트 값 변화는 에뮬에서 검증 안 됨 | — | ✅/⚠ |
| 시트·공조 복원(쓰기) | `setHvac*`, `moveSeat*` 있음(AAR) | ❌(signature) | `setHvacAc` 성공(7.6). `moveSeat*`는 미실측 | 7.6: "CONTROL_CAR_* 제어(set)", App Market 승인 범위 내 | ⚠(승인 필요) |
| 내비 즐겨찾기 | `getBookmarkInfo` ✅ 읽기; 쓰기는 `NavigationScreenRequest.AddHomeFavorite`(화면 요청)뿐 | — | 3장 ✅ | 7.9 클래스 목록 | ⚠(앱이 직접 쓰기 불가, 화면 호출만) |

### 8.3 스마트 에코 드라이빙 코치 — **⚠ 조건부(차속 기반만 가능)**
| 필요한 데이터 | Pleos SDK | AAOS API | 실측 결과 | 공식 근거 | 판정 |
|---|---|---|---|---|---|
| 차속 | 없음(크루즈 목표속도만) | `PERF_VEHICLE_SPEED` ✅ `CAR_SPEED`(dangerous) | 7.5: 60 km/h → 16.67 m/s, 약 7 Hz | AOSP: "Dangerous permission Car.PERMISSION_SPEED to read property." / Pleos 정책: "표준 데이터 \| AAOS Car API \| 속도…" | ✅(런타임 동의 + 승인 여부 ？) |
| 가속·감속 | 없음 | 상수 없음 | 차속 미분으로 계산 가능; 콘솔은 계단식이라 시연 시 보정 필요 | 7.4 | ⚠ |
| 가속 페달 | `Brake` on/off만 | API 34 없음; API 36 `ACCELERATOR_PEDAL_COMPRESSION_PERCENTAGE` → 에뮬 "unsupported" | 콘솔 `accelerator=30` → 콜백 0건 | AOSP: "Dangerous permission Car.PERMISSION_READ_CAR_PEDALS"(최신 OS) / Pleos: "getCurrentBrakePedalStatus … If true, the brake pedal is being pressed." | ❌(현 플랫폼) |
| 순간 전비·연비 | `get*EvEfficiency` ❌, `Fuel.*` ❌ | 전비 프로퍼티 없음; `EV_BATTERY_INSTANTANEOUS_CHARGE_RATE` ✅(0.0 고정), `FUEL_LEVEL` ✅ | 2.2 미지원; AAOS 값은 콘솔로 못 바꿈 | 7.4 | ⚠(앱 계산, 시연 ？) |
| 트립 거리·시간 | `Odometer.getTrip*` ❌, `getCurrentDistanceDriven` ✅ | `PERF_ODOMETER → requires CAR_MILEAGE` | 6.2 누적거리 리스너 ✅ | 7.4 | ⚠(앱이 누적거리 차이로 계산) |
| 외기 온도(보조) | — | `ENV_OUTSIDE_TEMPERATURE` ✅ 25.0 | 7.4 | — | ✅ |

### 8.4 멀미 케어 — **⚠ 조건부(예고 불가, 사후 감지+제어 승인)**
| 필요한 데이터 | Pleos SDK | AAOS API | 실측 결과 | 공식 근거 | 판정 |
|---|---|---|---|---|---|
| 경로의 커브 정도(곡률) | NaviHelper에 없음 | — | 7.9: 31개 클래스 전수 확인, 기하 필드 없음 | "data class TBTInfo(val type: TurnType, val distance: Double, val description: String)" | ❌(대안: `TURN_SHARP_*`·`CautionPoint` 남은 거리로 "곧 급회전" 예고) |
| 차속·가속도 | 없음 | `PERF_VEHICLE_SPEED` ✅ | 7.5 | 8.3과 같음 | ⚠ |
| 횡가속도 | 직접 값 없음; `SteeringWheel` 조향각 ✅(리스너, 6.2) | `PERF_STEERING_ANGLE → requires READ_CAR_STEERING` | 조향각 × 차속으로 추정 가능 | 7.4 | ⚠(추정) |
| 시트 제어(쓰기) | `moveSeat*`, `setSeat*` 있음(AAR) | ❌(CONTROL_CAR_SEATS signature) | 미실측(공조만 실측) | 7.6 | ⚠(승인 필요, 미실측) |
| 공조 제어(쓰기) | `setHvacAc` ✅ 동작 | ❌(CONTROL_CAR_CLIMATE signature) | 7.6 성공 | "CONTROL_CAR_* 제어(set)" + App Market 승인 | ⚠(승인 필요) |
| 주행 중 조작 제한 | — | CarUxRestrictions | — | 앱 심사 가이드 "운전 안전에 영향을 줄 수 있는 SDK·API는 별도의 심사 기준" | ⚠ |

### 8.5 내 차 실습실 — **✅ 가능**
| 필요한 데이터 | Pleos SDK | AAOS API | 실측 결과 | 공식 근거 | 판정 |
|---|---|---|---|---|---|
| 공조(전원·A/C·온도·풍량·풍향) 읽기·변화 감지 | ✅ getter+리스너(풍향은 조회로) | ❌(CONTROL_CAR_CLIMATE 필요, 읽기도 막힘) | 6.2·6.6 | Pleos: "HVAC → CAR_CLIMATE 조회(get)" | ✅ |
| 앞·뒤 제상 | ✅ `HvacDefrosterState` 리스너 | ❌ | 6.6 | — | ✅ |
| 내기순환 | ✅ `onHvacAutoRecirculationStateUpdated` | ❌ | 6.6 | — | ✅ |
| 와이퍼 상태·모드 | ✅ 상태 리스너 / 모드는 조회 | ❌(READ_WINDSHIELD_WIPERS signature) | 6.6 | Pleos: "Window, Wiper → CAR_WINDOWS" | ✅ |
| 열선(시트·핸들·미러) | ✅ 리스너 | ❌ | 6.2 | — | ✅ |
| 비상등 | ❌ 조회 API 없음 | `HAZARD_LIGHTS_STATE → requires CAR_EXTERIOR_LIGHTS` | 6.6 | 7.4 | ❌(퀘스트 제외 유지) |
- 모든 입력이 **조회(CAR_*) normal 퍼미션**이고 제어가 없어 승인 리스크가 가장 낮음. AAOS 경로는 전부 signature 퍼미션이라 **Pleos SDK가 유일한 경로**라는 점을 제안서에 쓸 수 있음.

---

## 9. 앞선 판정 정정 목록
| 어디 | 기존 판정 | 정정 | 근거 |
|---|---|---|---|
| 이 문서 0장 2번, 4.2, 6.3 | "현재 차속 API 없음" | **Pleos SDK에는 없지만 AAOS `PERF_VEHICLE_SPEED`로 가능**(dangerous `CAR_SPEED`) | 7.5 실측, AOSP 원문 |
| 이 문서 4.2 | "차량 제어는 심사 통과 불확실" | 기술적으로 **동작함**(`setHvacAc` 실측). 불확실한 건 App Market 승인뿐 → "조건부" | 7.6 |
| 이 문서 2.1 "현재 차속 — API 없음" | 〃 | 표 자체는 Pleos SDK 기준이라 유지하되 7.5 참조 | |
| `idea-candidates-review.md` 51·73행 "멀미 완화: 횡가속도 데이터가 SDK에 없어서 핵심 입력이 빔" | 불가 사유 | 직접 값은 없음(유지). 단 조향각(Pleos)+차속(AAOS)으로 **추정은 가능** → 사유는 "커브 예고 데이터 없음(7.9)"이 더 정확 | 7.4, 7.9 |
| `idea-candidates-review.md` 157행 "안전운전 점수: 차속 API 없음" | 불가 | **차속은 있음**. 가속 페달·급조향 점수는 조향각으로 대체 가능 → 재검토 대상 | 7.5 |
| `idea-candidates-review.md` 177행 "로드스터 사운드: 차속·가속 페달 값이 SDK에 없음" | 불가 | 차속은 있음, 가속 페달은 없음(유지) | 7.4·7.5 |
| `idea-candidates-review.md` 182행 "초보 주차 코치: 카메라·초음파 없음" | 불가 | **유지(확정)** | 7.4, 8.1 |
| `idea-candidates-review.md` 239·260행 "사고 과실: 차속 API 없음" | 치명 | 차속 기록 가능으로 정정 | 7.5 |
| `top-2-candidates.md` 104·132행 "차속 API 없음 → 긴장 감지 정확도 낮음 / 기술 리스크 높음" | | 차속 가능으로 정정(백그라운드 기록·CAR_SPEED 런타임 동의 리스크는 유지) | 7.5 |
| `contest-analysis-todo.md` 240행 "현재 차속 API 없음" | | 정정 | 7.5 |
| `final-recommendation.md` 97행 "Pleos ID 운전자 프로필" | 표현 | 매뉴얼 용어는 "사용자 프로필 + 마이현대/블루링크 계정 연동", 앱 마켓은 "Pleos Account". "Pleos ID"는 매뉴얼에 없음 | 7.8 |

### 미확인으로 남는 것(설명회·Playground 문의)
1. AAOS 퍼미션(`CAR_SPEED`, `CAR_ENERGY`)도 Pleos App Market 승인 대상인지.
2. 실차에서 `CAR_SPEED` 런타임 동의 다이얼로그가 Pleos UI에서 어떻게 뜨는지.
3. "데이터 동기화 설정"이 동기화하는 항목과 다른 차량 로드 가능 여부(8.2).
4. 가속 페달 프로퍼티(`ACCELERATOR_PEDAL_COMPRESSION_PERCENTAGE`)가 Pleos 실차 플랫폼에 올라올 계획이 있는지.
5. `EV_BATTERY_INSTANTANEOUS_CHARGE_RATE`를 콘솔에서 바꿀 수 없어 전비 시연 불가 — 시뮬레이션 수단.
