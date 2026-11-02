# 인수인계 — 이민하 (크루즈브레이커 로직)

담당 경로: 크루즈브레이커 로직 — `app/src/main/java/com/hyundaiapp/cruisebreaker/` 아래 `ui/` 화면을 뺀 전부(`domain/`, `data/`, ViewModel, `AppContainer`·`CruiseBreakerApp`·`RecordingService`). 화면(`ui/` Screen·테마)은 길민종

## 현재 상태 (2026-11-01 기준, 매번 덮어쓰기)

- 한 일 요약:
  - 판정 규칙 v2 → [docs/disengage-rules.md](../disengage-rules.md). PRD·기획서·README에도 반영됨(장호연, 11-01)
  - **main(PR #3~#6 머지)에서 에뮬레이터 흐름 동작**: S5 동의 → 기록 서비스 → 크루즈 작동 중 통과·해제 기록 → 500m 앞 안내(S1 배너 10초 + Pleos TTS) → S2 목록·S3 요약. 앱을 뒤로 보내도·재부팅 후에도 동작
  - **리뷰 후속 `fix/consent-and-review-followups`**(이슈 #7·#8, #9의 신호 수신 부분, PR #3 선택 제안):
    - 동의 철회: S4 [동의 철회] → 확인 창 → 기록 서비스 정지·신호 수신 끊음·배너 내림·기기의 동의·가명 차량 ID·주행 요약 삭제. 서비스가 다시 살아나도 동의가 없으면 바로 멈춤
    - 동의 전에는 차량·위치 신호를 받지 않음(신호 모니터만 화면에 들어갈 때 켬)
    - API 28~33 호환(`ServiceCompat`, specialUse 타입은 34+에서만), TTS 준비 값 `@Volatile`, 주변 조회를 따로 띄우고 5초 시간 초과
    - 업로드는 좌표 없는 형태(`DisengageUpload`·`PassUpload`)로만 받음, 큐를 거쳐 감지기가 막히지 않음, 같은 작동 중 같은 구간은 통과 1번
- 진행 중 브랜치·PR: `fix/consent-and-review-followups` (리뷰·머지 대기)
- 다음 할 일(우선순위 순):
  1. 11-05 중간 점검 시연 리허설(아래 "확인 방법" 순서대로)
  2. 장호연 위치 재생 시연 모드가 나오면 실제 경로에서 안내 확인
  3. 이상준 차슐랭 로직 PR 교차 읽기(PT 대비)
- 막힌 점·질문(누구에게):
  - 황준용: **업로드 인터페이스가 바뀜** — `CruiseEventRepository.upload(DisengageUpload / PassUpload)`(좌표·차속 없음, 해제 시각은 시간 단위, 통과는 한국 날짜, `vehicleId` 포함). 실패 시 쌓아 두고 재전송은 저장소 구현에서. **동의 철회 때 서버에 이미 올라간 기록을 어떻게 지울지**(가명 ID로 삭제 요청 API 등) 정해서 PRD 5.6에
  - 길민종(이슈 #9): S4에 [동의 철회] 줄을 내가 넣었음(`SettingsScreen`의 `onWithdrawConsent`, 모양은 바꿔도 됨) — 안내 거리 삭제·자동 기록 토글과 같이 정리. S5 차속 권한 문구, S5 알림 권한 요청, S2 브레이크 막대 삭제(`UiStates.kt`는 같은 PR에서 합의). `MainActivity`의 차속 권한 즉시 요청 삭제
  - 장호연: 최소 SDK를 34로 올릴지(지금은 28 유지하고 코드로 분기함)
- 주의(함정·직접 겪은 이상한 동작):
  - **기록은 S5 동의 후에만 돈다** → 새로 설치했으면 개발 메뉴 S5에서 동의 2개 + 시작하기
  - 동의를 철회한 뒤 같은 프로세스에서 다시 동의해도 정상 재시작됨(감지기·안내기가 시작할 때 이전 상태를 지움)
  - TTS: Android 기본 `TextToSpeech`는 에뮬레이터에 엔진이 없어 실패. Pleos TTS SDK는 `Locale.KOREA`(ko_KR)여야 함("ko"면 엔진 오류)
  - 시드 "시연 지점(에뮬레이터 위치 앞 400m)"은 실제 데이터 아님(PT에서 밝힐 것). 가짜 서버는 차량-일 중복 제거를 안 함
  - lint `ImplicitSamInstance`(`stopService`) 경고는 오탐 — `Intent`로 멈추는 정상 호출
  - 부팅 중 `ai.pleos.service.car`(시스템) 충돌 로그는 우리 앱과 무관
  - 크루즈: 콘솔 `full_autonomous` → ACTIVATED, `ready` → ENABLED. 해제 값(USER_OVERRIDE 등)은 재현 불가. 해석은 `CruiseStateInterpreter`에만
  - Git Bash에서 `adb shell run-as ... cat /data/...`는 경로가 바뀌므로 `MSYS_NO_PATHCONV=1`을 붙인다
- 확인 방법:
  - 단위 테스트·lint: `./gradlew :app:testDebugUnitTest :app:lintDebug` (JDK 17, 44개)
  - 시연 순서: 앱 설치 → `adb shell pm grant --user 10 com.hyundaiapp.cruisebreaker android.car.permission.CAR_SPEED` → 개발 메뉴 S5 동의 2개 + 시작하기 → `bash tools/vconsole.sh up` → `put 'gear?gear=Drive' 'controls?speed=100' 'driving-mode?ad_mode_state=full_autonomous'` → S1 배너·음성 → 5초 뒤 `put 'driving-mode?ad_mode_state=ready'`(해제 기록) → `put 'gear?gear=Park'` → S3 요약·S2 목록. 로그 `adb logcat -s CruiseRecord`
  - 철회: S4 [동의 철회] → [철회] → `adb shell dumpsys activity services com.hyundaiapp.cruisebreaker | grep RecordingService` 없음, `MSYS_NO_PATHCONV=1 adb shell run-as com.hyundaiapp.cruisebreaker --user 10 cat /data/user/10/com.hyundaiapp.cruisebreaker/shared_prefs/cruisebreaker.xml` → `<map />`
  - PR 올리기: `gh`(`C:\Program Files\GitHub CLI\gh.exe`, minalee22)

## 작업 기록 (최신이 위, 지우지 않음)

### 2026-11-01 밤 (이민하, Claude 에이전트와 작업 — 리뷰 후속)
- 한 일: `fix/consent-and-review-followups` — 이슈 #7(동의 철회), #8(API 28 호환·TTS 가시성·주변 조회), #9 중 동의 전 신호 수신, PR #3 선택 제안 4개 반영. 테스트 34 → 44개, lint `NewApi` 없음
- 에뮬레이터 확인: 새로 설치 → 서비스·신호 수신 없음 → S5 동의 → 기록·안내·음성 → S4 철회 → 서비스 없음·prefs 빔·주행해도 기록 없음·S5 빈 동의 → 다시 동의 → 같은 프로세스에서 기록 재시작, 새 차량 ID
- 결정한 것과 이유:
  - 철회 = 기기 데이터 전부 삭제(동의·차량 ID·음성·자동 기록 설정·주행 요약). 다시 동의하면 새 차량 ID라 이전 기록과 이어지지 않음
  - 최소 SDK는 28 그대로 두고 코드로 분기(올릴지는 장호연 결정)
  - 조회 결과가 오면 마지막 위치로 바로 다시 판단(따로 띄운 조회 때문에 첫 안내가 1초 늦어지지 않게)
- 바꾼 파일: `AppContainer.kt`, `CruiseBreakerApp.kt`, `RecordingService.kt`, `data/{AppPrefs,CruiseEventRepository,FakeCruiseServer}.kt`, `data/VehicleId.kt`(삭제, `AppPrefs.vehicleId()`로), `domain/{DisengageDetector,RiskAnnouncer,RiskSpeaker,TripStats,TripTracker}.kt`, `ui/CruiseBreakerNav.kt`, `ui/settings/{SettingsScreen,SettingsViewModel}.kt`, `ui/onboarding/OnboardingViewModel.kt`, 테스트 4개 + `UploadRecordTest`(새 파일), `docs/handoff/lee-minha.md`

### 2026-11-01 밤 (이민하, Claude 에이전트와 작업 — 기록 서비스)
- 한 일: `feat/recording-service` — `RecordingService`(specialUse, 알림 LOW "주행 기록 중"), `BootReceiver`, 기록 파이프라인을 `CruiseBreakerApp.onCreate`에서 `AppContainer.startRecording()`으로 옮김(서비스가 부름, 한 번만 시작). 퍼미션 4개(`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS`)
- 확인: 동의 전 서비스 없음 → S5 동의 → 서비스 시작 → HOME으로 뒤로 보낸 뒤 15초 `isFrozen=false`, 안내·음성·해제 기록 → `adb reboot` 후 앱을 열지 않고 서비스 자동 시작, 안내·음성·해제 기록
- 결정한 것과 이유: 기록·안내는 S5 동의 후에만(동의 없이 위치 기반 기록 금지). S4 자동 기록 끔 = 시동 때 자동 시작만 끔(앱을 열면 기록함). 앱 시작 시 서비스는 `MainActivity`에서 켬(백그라운드에서는 포그라운드 서비스를 시작할 수 없음)
- 주의: 부팅 중 `ai.pleos.service.car`(시스템) 충돌 로그가 보이지만 우리 앱과 무관
- 바꾼 파일: `RecordingService.kt`(새 파일), `AppContainer.kt`, `CruiseBreakerApp.kt`, `MainActivity.kt`, `ui/CruiseBreakerNav.kt`, `AndroidManifest.xml`, `docs/handoff/lee-minha.md`

### 2026-11-01 저녁 (이민하, Claude 에이전트와 작업 — 화면 ViewModel)
- 한 일: `feat/screen-viewmodels` — `RiskListViewModel`(S2, 들어올 때마다 주변 10km 조회, 위험 구간만 가까운 순), `TripSummaryViewModel`(S3)+`TripStats`(안내받은 구간·안내 수·내 해제 수, 새 주행이면 0), `SettingsViewModel`(S4), `OnboardingViewModel`(S5), `AppPrefs`(SharedPreferences). `SettingsUiState.autoRecordEnabled` 추가. 음성 끔이면 `RiskSpeaker`가 읽지 않음. 에뮬레이터에서 S2 목록·S3 요약(1곳·1회·1건) 확인
- 결정한 것과 이유: "내가 보탠 기록"은 해제 수만 셈(통과는 150m마다 생겨 숫자가 커서 의미가 흐려짐). "지난 위험 구간"은 안내받은 구간 수(같은 구간은 한 주행 1회 안내라 서로 다른 구간 수와 같음)
- 바꾼 파일: `ui/{risk,summary,settings,onboarding}/*ViewModel.kt`(새 파일), `domain/TripStats.kt`, `data/AppPrefs.kt`, `ui/CruiseBreakerNav.kt`, `ui/UiStates.kt`, `AppContainer.kt`, `CruiseBreakerApp.kt`, 테스트 `ScreenViewModelsTest`

### 2026-11-01 오후 (이민하, Claude 에이전트와 작업 — 안내기)
- 한 일: `feat/risk-announcer`
  - `RiskAnnouncer`: 크루즈 작동 중, 진행 방향 ±30°, 앞 300~500m에 위험 구간 중심, 위험 구간 방향도 45° 안 → 1회. 같은 구간 한 주행 1회, 3분 간격, 배너 10초
  - `TripTracker`: 한 주행 = D ~ P
  - `FakeCruiseServer`: 가짜 저장소 2개를 합침. 업로드가 바로 반영되고 같은 방향 9칸 합산으로 위험 판단. 시드 ID를 실제 geohash로
  - `RiskSpeaker`: Pleos TTS SDK 3.3.1 기기 내 모드. 퍼미션 `pleos.car.permission.TTS_SERVICE` 추가
  - `VehicleId`: 설치마다 UUID
  - 에뮬레이터에서 S1 배너·음성·10초 뒤 배너 사라짐 확인
- 결정한 것과 이유(이민하 11-01): 안내 500m 고정, 크루즈 작동 중에만, 한 주행 D~P, 실시간 업로드, 설치마다 UUID, 28일. 문서: `docs/disengage-rules.md`
- 에이전트가 정한 것: 안내 범위 300~500m(처음 들어올 때 "500미터 앞"이 맞게), 배너 10초, 주변 조회 2km·500m 이동 또는 1분마다 갱신
- 바꾼 파일: `app/.../domain/{RiskAnnouncer,TripTracker,RiskSpeaker,Disengage,SegmentKey}.kt`, `app/.../data/{FakeCruiseServer,VehicleId,CruiseEventRepository,SegmentRiskRepository}.kt`, `AppContainer.kt`, `CruiseBreakerApp.kt`, `AndroidManifest.xml`, `app/build.gradle.kts`, 테스트 3개(새 파일), `docs/handoff/lee-minha.md`

### 2026-11-01 (이민하, Claude 에이전트와 작업)
- 한 일:
  - 크루즈 해제 상황을 조사해 판정 규칙을 처음부터 다시 정함 → `docs/disengage-rules.md`(main에 바로 push)
  - NaviHelper 실측(에뮬레이터): 현재 도로 이름은 없음(주소는 동 단위), TBT는 경로 안내 중 내 경로의 진출·톨게이트만, 혼잡 구간에만 도로 이름 → 도로 정보로 상황을 나누는 방식은 버림
  - `feat/disengage-detection`: `SegmentKey`(geohash 인코딩·범위·둘레 8칸), `DisengageDetector`(해제·통과), `CruiseStateInterpreter`(`USER_OVERRIDE`·`FORCED_DEACTIVATION_WARNING`은 작동 중), `CruiseBreakerApp`에서 Fake 저장소로 업로드 + 로그
- 결정한 것과 이유:
  - 원인(브레이크·정체·커브)을 나누지 않음: 위치에 묶인 원인은 같은 곳에 쌓이고 우연한 원인은 흩어져서 비율만으로 걸러짐. 운전자 행동도 같아 안내도 한 가지
  - 정체를 빼지 않음: 상습 정체가 시작되는 곳도 속도가 급히 줄어드는 위험 지점
  - 출구(IC 진출)는 구분하지 않음: 도로 정보 없이 구분하려면 경로 기록이 필요해 개인정보 부담이 큼. 한계로 문서에 적음
  - 칸 경계는 좌표·동심원 대신 서버 9칸 합산: 좌표 방식은 분모(통과 수)를 내려면 이동 경로 전체가 필요해 PRD의 "원래 좌표 안 보냄"과 충돌. 9칸 합산은 차 쪽 변경 없음
  - 60km/h 조건은 유지(오래 정차해서 자동 해제된 것을 뺌). 차속을 모르면 건너뜀
- 바꾼 파일: `docs/disengage-rules.md`(새 파일), `app/.../domain/{SegmentKey,DisengageDetector,Disengage,CruiseStateInterpreter,RiskAnnouncer}.kt`, `app/.../CruiseBreakerApp.kt`, 테스트 `SegmentKeyTest`·`DisengageDetectorTest`(새 파일)·`CruiseStateInterpreterTest`, `docs/handoff/lee-minha.md`


### 2026-10-30 (이민하, Claude 에이전트와 작업)
- 한 일:
  - 이 PC 개발 환경 세팅(Android Studio, JDK 17, SDK, Pleos 이미지, AVD). 함정: Studio 내장 JDK는 25라 Gradle 8.11.1이 안 돎 → JDK 17 별도 설치, 새 `sdkmanager`는 Pleos 저장소를 무시함 → cmdline-tools 19.0으로 설치
  - 백그라운드 동결 실험(PRD 5.4): 기본 구조 / 포그라운드 서비스 / 멀티 윈도우, 트리거는 경로 안내와 주행(D·80km/h)
  - 추가 검증: 재부팅 자동 시작(부팅 수신기, 동의 후에만), 알림 모양·위치, 7개 상황 연속 유지
- 결정한 것과 이유:
  - 실험 앱은 저장소에 넣지 않음(담당 경로 밖, 일회성). 결과만 `docs/background-freeze-experiment.md`에 정리
  - 권장안은 포그라운드 서비스. 멀티 윈도우는 시스템에서 꺼져 있어 선택지가 아님
  - 서비스 타입 `specialUse`, 구현은 이민하 PR, 자동 기록은 S5 동의 후 기본 켜짐·S4에서 끔 — 실험 문서 "결정 사항"·"팀원별 확인 요청"에 정리
- 바꾼 파일: `docs/background-freeze-experiment.md`(새 파일), `docs/handoff/lee-minha.md`

### 2026-10-30
- 장호연: 파일 생성. 첫 작업은 README "담당별 첫 작업" 기준으로 적어 둠.
