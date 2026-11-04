# 인수인계 — 이민하 (크루즈브레이커 로직)

담당 경로: 크루즈브레이커 로직 — `app/src/main/java/com/hyundaiapp/cruisebreaker/` 아래 `ui/` 화면을 뺀 전부(`domain/`, `data/`, ViewModel, `AppContainer`·`CruiseBreakerApp`·`RecordingService`). 화면(`ui/` Screen·테마)은 길민종

## 현재 상태 (2026-11-04 기준, 매번 덮어쓰기)

- 한 일 요약: **크루즈브레이커 로직은 모두 main에 들어감. 열린 이슈·PR 없음**
  - 판정 규칙 v2 → [docs/disengage-rules.md](../disengage-rules.md). PRD·기획서·README 반영됨(장호연, 11-03 용어 결정 "해제 다발 구간" 포함)
  - 동작 흐름(에뮬레이터 확인): S5 동의 → 기록 서비스(포그라운드, 재부팅 후 자동 시작) → 크루즈 작동 중 통과·해제 기록 → 500m 앞 해제 다발 구간 안내(S1 배너 10초 + Pleos TTS) → S2 목록·S3 요약. S4 동의 철회 → 기록 즉시 중지·기기 데이터 삭제·서버 삭제를 성공할 때까지 재시도
  - 머지된 PR: #3(해제·통과 감지, geohash 구간), #4(안내기·TTS), #5(S2~S5 ViewModel), #6(기록 서비스), #10(동의 철회), #17(철회 시 서버 삭제·업로드 재시도), #23(업로드 대기열 상한·영구 거절 건너뛰기, TTS 해제 가드)
  - 실제 서버: 황준용 PR #22로 Supabase 연결(`SupabaseCruiseServer`, core outbox). 이 PC의 `local.properties`에 팀 공개 키가 없어 **실제 서버로는 아직 확인 안 함**(키가 없으면 앱 안 Fake로 동작)
  - 닫힌 이슈 정리(11-03~04): #19·#21(PR #23), #20(PR #22 서버가 철회된 ID 업로드 거절), #25(오류 알림 대신 S5 보조 안내 + 약관 책임 제한, PR #31·#32), #26(필요 없음 — 요약은 다음 D까지 남고 S2 [최근 주행 요약]으로 다시 볼 수 있음)
- 진행 중 브랜치·PR: 없음
- 다음 할 일(우선순위 순):
  1. 11-05 중간 점검: `bash tools/demo-drive.sh`로 시연 흐름 한 번 확인(장호연이 11-04에 재확인함)
  2. (선택) 팀 공개 키를 받아 `local.properties`에 `supabase.url`·`supabase.publishableKey`를 넣고 실제 서버로 기록·안내·철회 삭제 확인(`docs/server-overview.md` "팀원 빠른 연결")
  3. PT 대비: 판정·안내·기록·철회 구조를 직접 설명할 수 있게 정리. 교차 읽기 — 이상준 차슐랭 로직, 황준용 서버 구조(`docs/server-overview.md`)
- 막힌 점·질문(누구에게): 없음
  - 참고: 길민종 PR #38이 `FakeCruiseServer` 시드 이름을 "판교IC 진입로"로 바꿈(이슈 #37). 서버 시드 이름 변경은 황준용(#37 남은 부분)
  - 장호연: 최소 SDK를 34로 올릴지(지금은 28 유지, 코드로 분기) — 급하지 않음
- 주의(함정·직접 겪은 이상한 동작):
  - **기록은 S5 동의 후에만 돈다** → 새로 설치했으면 개발 메뉴 S5에서 동의 2개 + 시작하기([시작하기]는 화면 아래 고정)
  - **Pleos TTS SDK**: Android 기본 `TextToSpeech`는 에뮬레이터에 엔진이 없어 실패. SDK는 `Locale.KOREA`(ko_KR)여야 함("ko"면 엔진 오류). 해제된 객체에 `speak`를 불러도 예외 없이 **실제로 읽음** → 해제 뒤에는 SDK를 부르지 않음(`RiskSpeaker`의 `released` 가드)
  - 공개 키가 없으면 빌드가 오류 없이 Fake로 됨. 제출 빌드는 `tools/build-release.sh`가 키 없으면 멈춤(장호연 PC에서 빌드). 공개 키(`sb_publishable_...`)는 APK에 들어가도 되는 키, 관리자 키는 절대 넣지 않음
  - 철회 뒤 기기에 남는 값은 `pending_server_deletions`(서버 삭제를 못 끝낸 차량 ID)뿐. 삭제가 성공하면 빈 목록
  - 시드 "판교IC 진입로"(에뮬레이터 고정 위치 북쪽 400m)는 실제 데이터 아님 — PT에서 시드임을 밝힐 것
  - 크루즈: 콘솔 `full_autonomous` → ACTIVATED, `ready` → ENABLED. 실차 해제 값(USER_OVERRIDE 등)은 재현 불가, 해석은 `CruiseStateInterpreter`에만
  - lint `ImplicitSamInstance`(`stopService`) 경고는 오탐
  - Git Bash에서 `adb shell run-as ... cat /data/...`는 `MSYS_NO_PATHCONV=1`을 붙인다
- 확인 방법:
  - 단위 테스트·lint: `./gradlew :app:testDebugUnitTest :app:lintDebug` (JDK 17)
  - 시연: `bash tools/demo-drive.sh` 또는 수동 — 앱 설치 → `adb shell pm grant --user 10 com.hyundaiapp.cruisebreaker android.car.permission.CAR_SPEED` → 개발 메뉴 S5 동의 2개 + 시작하기 → `bash tools/vconsole.sh up` → `put 'gear?gear=Drive' 'controls?speed=100' 'driving-mode?ad_mode_state=full_autonomous'` → S1 배너·음성 → 5초 뒤 `put 'driving-mode?ad_mode_state=ready'`(해제 기록) → `put 'controls?speed=0' 'gear?gear=Park'` → S3·S2. 로그 `adb logcat -s CruiseRecord`
  - 철회: S4 [동의 철회] → [철회] → 로그 `consent withdrawn, server deletions pending=N`
  - PR 올리기: `gh`(`C:\Program Files\GitHub CLI\gh.exe`, minalee22)

## 작업 기록 (최신이 위, 지우지 않음)

### 2026-11-04 (이민하, Claude 에이전트와 작업 — 상태 정리)
- 한 일: 두 저장소 최신화 후 "현재 상태"를 머지 결과에 맞춰 다시 씀(PR #22·#23·#32 머지, #20·#25 닫힘)
- 결정한 것과 이유(11-03, 이민하):
  - 이슈 #25: S2 조회 실패 알림은 넣지 않음 — 운전자가 해결할 수 없는 문제라 알려도 할 일이 없음. 대신 S5 보조 안내(길민종 PR #31)와 약관 책임 제한(장호연 PR #32)
  - 이슈 #26: 구현 안 함(not planned) — 마지막 주행 요약은 `TripStats`에 다음 D까지 남고 S2 [최근 주행 요약]으로 다시 볼 수 있어 "한 번 보이면 충분"
- 바꾼 파일: `docs/handoff/lee-minha.md`

### 2026-11-03 오후 (이민하, Claude 에이전트와 작업 — 이슈 #19·#21)
- 한 일: `fix/upload-queue-and-tts-release` — `data/UploadQueue.kt`(새 파일), `PermanentFailureException`·`isPermanentFailure`, `retrying(isPermanent)`, `RiskSpeaker` 해제 락. 테스트 `UploadQueueTest`(새 파일, 5개)
- 확인: TTS SDK 해제 동작을 임시 시험 빌드로 실측(위 "주의"), 고친 빌드로 동의→즉시 철회 5회 반복 크래시 없음. PR #22와 3-way 합치기 충돌 없음·테스트 통과
- 결정한 것과 이유:
  - 업로드 대기열 상한 500건, 넘치면 오래된 것부터 버림(이슈 제안대로). 영구 실패 판정은 저장소가 `PermanentFailureException`을 던지는 계약으로(core 예외 타입에 기대지 않게)
  - 삭제는 영구 실패여도 계속 재시도(규정·지울 ID 보존)
  - 이슈 #20은 서버 방식(PR #22, 철회된 ID 업로드 거절)으로 정리, 앱은 그대로
- 바꾼 파일: `AppContainer.kt`, `data/{CruiseEventRepository,PendingDeletions,UploadQueue}.kt`, `domain/{Retry,RiskSpeaker}.kt`, `UploadQueueTest.kt`, `docs/handoff/lee-minha.md`

### 2026-11-03 (이민하, Claude 에이전트와 작업 — 철회 시 서버 삭제)
- 한 일: `fix/withdraw-server-delete` — `PendingDeletions`(삭제 대기 저장·재시도·앱 시작 때 이어서), `CruiseEventRepository.deleteMine`, `AppPrefs.pendingDeletions`(철회와 같은 commit으로 옮김), `retrying`(5초·15초·1분·5분, 차슐랭과 같은 간격)으로 업로드 재시도, androidx.core 직접 의존(이슈 #11), TTS 연결 놓기, 가짜 서버 차량별 삭제. 테스트 44 → 48개(+core는 따로)
- 에뮬레이터 확인: 동의 → 주행(안내·기록) → 철회 → `pending=1` → 가짜 서버 삭제 성공 → 대기 목록 빔, 서비스 없음 → 다시 동의 → TTS 새로 연결, 안내·기록 정상
- 결정한 것과 이유: 삭제 대기 ID는 동의·차량 ID를 지우는 같은 저장(commit)에서 옮김 — 사이에 앱이 죽어도 지울 ID를 잃지 않게. 업로드 재시도는 순서를 지키려고 한 줄로(앞 기록이 성공해야 다음)
- 바꾼 파일: `app/build.gradle.kts`, `AppContainer.kt`, `CruiseBreakerApp.kt`, `data/{AppPrefs,CruiseEventRepository,FakeCruiseServer}.kt`, `data/PendingDeletions.kt`·`domain/Retry.kt`(새 파일), `domain/RiskSpeaker.kt`, 테스트 `PendingDeletionsTest`(새 파일), `docs/handoff/lee-minha.md`

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
