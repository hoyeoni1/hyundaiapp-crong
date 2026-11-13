# 인수인계 — 이민하 (크루즈브레이커 로직)

담당 경로: 크루즈브레이커 로직 — `app/src/main/java/com/hyundaiapp/cruisebreaker/` 아래 `ui/` 화면을 뺀 전부(`domain/`, `data/`, ViewModel, `AppContainer`·`CruiseBreakerApp`·`RecordingService`). 화면(`ui/` Screen·테마)은 길민종

## 현재 상태 (2026-11-13 기준, 매번 덮어쓰기)

- 한 일 요약: **크롱 로직은 모두 main에 들어가 있고 열린 이슈는 없다. 11-13에 시연 영상용 녹화 스크립트와 영상 시나리오를 브랜치에 올렸다**
  - 판정 규칙 v2 → [docs/disengage-rules.md](../disengage-rules.md). 동작 흐름: S5 동의 → 기록 서비스 → 크루즈 작동 중 통과·해제 기록 → 500m 앞 해제 다발 구간 안내(배너 10초 + Pleos TTS, 내비 위로 올라옴) → S2 목록·S3 요약. S4 동의 철회 → 기록 중지·기기 데이터 삭제·서버 삭제 재시도
  - 시연 영상: [docs/demo-video-scenario.md](../demo-video-scenario.md)(블렌더 상황 영상 → 인포테인먼트 진입 → 실제 구동 녹화, 편당 3번 전환). 녹화는 `bash tools/demo-video.sh crong`(크롱), `bash tools/demo-video.sh chachelin`(차슐랭) — 화면 터치·차량 값 주입·`adb screenrecord`·주입 시각 기록(`events.tsv`)·블렌더에 붙일 화면 캡처를 한 번에 한다
  - 11-13 확인: 에뮬레이터 + 제출 release APK(`apk/SamgakGorae_CRONG_release.apk`) + 실제 서버(시연 데이터 모드)에서 크롱 전체 녹화가 끝까지 통과. 내비 위 배너 → 90 km/h 해제 기록 → 40 km/h 해제 기록 안 함 → S3 "1회 · 1곳 · 1건" → S2 4곳(판교JC 합류부 0.4 km 40%) → 철회 로그 `consent withdrawn, server deletions pending=1`
- 진행 중 브랜치·PR: `feat/demo-video-script-20261113`(tools/demo-video.sh, docs/demo-video-scenario.md, 이 문서). `tools/`는 장호연 담당·차슐랭이 원본이라 차슐랭 저장소에도 같은 이름의 브랜치로 같은 파일을 올렸다 → 장호연 확인 필요
- 다음 할 일(우선순위 순):
  1. 시연 영상 마무리: 블렌더 컷 렌더, 녹화와 이어 붙이기(저장소 밖 `demo-video/` 폴더), 음성 안내 소리 넣기
  2. PT 대비: 판정·안내·기록·철회 구조를 직접 설명할 수 있게 정리. 교차 읽기 — 이상준 차슐랭 로직, 황준용 서버 구조(`docs/server-overview.md`)
- 막힌 점·질문(누구에게):
  - 장호연: `tools/demo-video.sh`를 두 저장소 브랜치에 올림(main 아님). 머지할지, 영상 제작용으로만 둘지 결정
- 주의(함정·직접 겪은 이상한 동작):
  - **기록은 S5 동의 후에만 돈다.** 차속 권한은 앱을 켜기 전에 줘야 한다(`pm grant ... android.car.permission.CAR_SPEED`). 켠 뒤에 주면 다시 켜기 전까지 차속이 비어 40 km/h 해제도 기록된다
  - **동의 직후 S2 목록은 "거리 확인 중"으로 뜬다**(첫 조회 때 위치를 아직 못 받음. `RiskListViewModel.refresh`는 화면에 들어올 때만 다시 조회). 설정에 다녀오면 가까운 순 + 거리로 바뀐다. 녹화 스크립트는 이렇게 우회한다 — 앱을 고칠지는 길민종과 정할 것
  - **pm clear로 앱 데이터를 지우면 그 설치가 서버에 올린 기록이 남는다**(삭제를 요청할 가명 ID가 사라짐). 리허설은 앱의 [동의 철회]로 끝낸다. `demo-video.sh`는 시작·끝에 [동의 철회]를 누른다
  - Git Bash에서 `MSYS_NO_PATHCONV=1`을 스크립트 전체에 걸면 `vconsole.sh`의 `curl -o /dev/null`이 깨진다(curl exit 23). adb 호출에만 건다
  - `adb screenrecord`는 소리를 못 담는다(음성 안내는 PC 스피커로만). 에뮬레이터 화면은 2560×1440, 녹화는 약 26 fps
  - 차슐랭에서 길안내를 시작하면 내비에 경로가 남는다 → 내비의 [안내 종료]로 끈다(스크립트가 한다)
  - 에뮬레이터 시계는 PC와 다르다(11-13 14:50 KST에 에뮬레이터는 11-12 21:50) → 앱의 날짜·시각 표시가 그 기준
  - Pleos TTS SDK: `Locale.KOREA`여야 함. 해제된 객체에 `speak`를 불러도 실제로 읽으므로 해제 뒤에는 부르지 않는다(`RiskSpeaker`의 `released` 가드)
  - 빌드는 `local.properties`에서만 서버 키를 읽는다(없으면 오류 없이 Fake). 제출 빌드는 `tools/build-release.sh`
  - 크루즈: 콘솔 `full_autonomous` → ACTIVATED, `ready` → ENABLED. 실차 해제 값은 재현 불가, 해석은 `CruiseStateInterpreter`에만
  - `demo-drive.sh`·`demo-video.sh`를 파이프(`| grep` 등)로 돌리면 하트비트가 출력을 붙잡아 끝나지 않는다 → 파일로 출력(`> log 2>&1`). 끝나면 `bash tools/vconsole.sh down`
- 확인 방법:
  - 단위 테스트·lint: `./gradlew :app:testDebugUnitTest :app:lintDebug` (JDK 17)
  - 시연 녹화: 에뮬레이터 실행 → `adb install -r apk/SamgakGorae_CRONG_release.apk` → `bash tools/demo-video.sh crong > log 2>&1` → `build/demo-video/crong/kr1.mp4`·`drive.mp4`·`events.tsv`, `build/demo-video/assets/crong_*.png`. 기록 확인 `adb logcat -s CruiseRecord`
  - 손으로 시연: `bash tools/demo-drive.sh`(확정 순서는 [docs/demo-scenario.md](../demo-scenario.md))
  - PR 올리기: `gh`(`C:/Program Files/GitHub CLI/gh.exe`, minalee22)

## 작업 기록 (최신이 위, 지우지 않음)

### 2026-11-13 (이민하, Claude 에이전트와 작업 — 시연 영상 시나리오·녹화 스크립트)
- 한 일: 영상 시나리오 작성(`docs/demo-video-scenario.md`), 녹화 스크립트 `tools/demo-video.sh` 작성(크롱·차슐랭 공통), 에뮬레이터에서 두 앱 전체 녹화 통과. 에뮬레이터에서 예전 패키지 `com.hyundaiapp.cruisebreaker`를 지우고 제출 release APK 두 개를 설치함
- 결정한 것과 이유(이민하):
  - 블렌더 상황 영상과 녹화는 겹치지 않고 번갈아 나온다. 블렌더 → 녹화는 항상 인포테인먼트 진입 애니메이션을 거치고, 편당 3번으로 줄인다(5번은 너무 잦음)
  - 블렌더 영상에는 자막을 넣지 않는다('크루즈 해제', '충전 중' 같은 장면 안 글씨만)
  - 해제 순간 앱의 "크루즈 대기/작동" 글자 변화는 영상에서 빼고, 기록 여부는 S3 요약으로 보여 준다 → 주행 녹화는 내비를 앞에 둔 한 번이면 된다
  - 스크립트는 pm clear 대신 앱의 [동의 철회]로 시작·끝을 맞춘다 — 실제 서버에 리허설 기록이 쌓이지 않게
  - (같은 날 영상 검토 뒤) 자막은 블렌더·녹화 모두 넣지 않고 고지는 엔드카드에만 둔다. 크롱 영상에서 해제 조건 시연(90 km/h는 기록, 40 km/h는 기록 안 함)은 뺀다 — 앱이 하는 일은 해제를 기록하고 안내하는 것이고 조건까지 넣으면 복잡하다. 대신 인트로에서 커브 구간 해제 → 합류 구간 해제를 차례로 보여 준다
  - 녹화에서 담는 영역: 계기판 패널 + 앱 창 + 독을 잘림 없이, 오른쪽 가는 띠만 자르고 상태 표시줄 자리는 앱 창 색으로 덮는다. 빨리 감는 곳은 기다리는 구간뿐이고 화면을 넘기는 부분은 원래 속도. 블렌더 → 녹화는 한 직선으로 들어가고 크기가 튀지 않게 화면 폭 = 프레임 폭에서 멈춘다
- 리허설이 서버에 남긴 것: 스크립트를 고치기 전 pm clear로 지운 설치(크롱 1, 차슐랭 1)의 기록(크롱 해제 1·통과, 차슐랭 시흥동 주민센터 인증 1·실패 1)은 서버에 남았다 → 촬영 전 `server/supabase/clear-rehearsal-data.sql`(차슐랭 저장소) 필요. 그 뒤 실행분은 동의 철회로 지움
- 바꾼 파일: `tools/demo-video.sh`(새 파일), `docs/demo-video-scenario.md`(새 파일), `docs/handoff/lee-minha.md`

### 2026-11-04 (이민하, Claude 에이전트와 작업 — PR #42 확인·실제 서버 리허설)
- 한 일: PR #42 이민하 파일 변경 검토·동의 코멘트, 규칙 문서 3분 문구(main), main+PR #42를 합친 임시 빌드(커밋 안 함)로 실제 서버 리허설 — 앱 80·core 32 단위 테스트 통과, `demo-drive.sh` 3회(두 번째·세 번째는 45초 간격에도 안내), S2·S3 확인, 철회 시 서버 삭제 확인
- 결정한 것과 이유: 안내 3분 간격은 한 주행 안에서만 센다(장호연 PR #42 해석에 동의) — 원래 붙어 있는 칸들이 한 주행에서 연달아 안내되지 않게 하려던 규칙
- 바꾼 파일: `docs/disengage-rules.md`, `docs/handoff/lee-minha.md`. 이 PC `local.properties`(커밋 안 됨)에 팀 공개 서버 키 추가

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
