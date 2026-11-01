# 인수인계 — 이민하 (크루즈브레이커 로직)

담당 경로: `app/src/main/java/com/hyundaiapp/cruisebreaker/domain/`, ViewModel

## 현재 상태 (2026-11-01 기준, 매번 덮어쓰기)

- 한 일 요약:
  - 판정 규칙 v2 → [docs/disengage-rules.md](../disengage-rules.md)(PRD 5.3 대체, PRD 반영은 장호연 요청). 11-01 결정: 안내 500m 고정·크루즈 작동 중에만, 한 주행 = D~P, 업로드 실시간, 가명 차량 ID = 설치마다 UUID, 기간 28일
  - **에뮬레이터에서 11-05 흐름 동작**: 크루즈 작동 → 통과·해제 기록 → 가짜 서버 반영 → 500m 앞 위험 구간 안내(S1 배너 10초 + Pleos TTS 음성)
    - 해제 감지·구간 ID: `feat/disengage-detection`(PR #3)
    - 안내기·주행 구분·가짜 서버·음성·차량 ID: `feat/risk-announcer`(PR #3 위에 쌓음)
  - 아직 없는 것: 포그라운드 서비스(앱이 뒤로 가면 기록이 멈춤), S2~S5 ViewModel, 실제 서버(황준용)
- 진행 중 브랜치·PR: https://github.com/hoyeoni1/hyundaiapp-cruisebreaker/pull/3 (리뷰·머지 대기), `feat/risk-announcer` PR(#3 머지 후 main 기준으로 바뀜)
- 다음 할 일(우선순위 순):
  1. ViewModel: S2(가짜 서버 목록 → 해제 +1이 보이게), S3(주행 요약, `TripTracker` 기준), S4(`autoRecordEnabled`, 음성 켜기 → `RiskSpeaker.start`의 `enabled`), S5
  2. `feat/recording-service`: 포그라운드 서비스 + 부팅 수신기. 지금 `CruiseBreakerApp.onCreate`에 있는 시작 코드를 서비스로 옮김
  3. 장호연 위치 재생 시연 모드가 나오면 안내 확인(지금은 에뮬레이터 고정 위치 앞 400m 시드로만 확인)
- 막힌 점·질문(누구에게): `docs/disengage-rules.md` "팀원별 요청" 표
  - 장호연: PRD·기획서·README 반영, 주최 측 문의에 `pleos.car.permission.TTS_SERVICE` 승인 여부 추가
  - 황준용: 실시간 업로드(실패 시 쌓아 두고 재전송), 업로드에 `AppContainer.vehicleId` 붙이기, `segment_risk` 9칸 합산·차량-일 중복 제거·28일
  - 길민종: S2 "브레이크로 해제" 막대 삭제, S4 안내 거리 선택 삭제(500m 고정)
- 주의(함정·직접 겪은 이상한 동작):
  - **TTS**: Android 기본 `TextToSpeech`는 에뮬레이터에 엔진이 없어 초기화 실패(status -1). Pleos TTS SDK는 되지만 `Locale.KOREAN`("ko")이면 엔진이 "unsupported locale: ko-"로 실패 → `Locale.KOREA`(ko_KR)
  - 시드에 "시연 지점(에뮬레이터 위치 앞 400m)"이 있음 — 에뮬레이터 위치가 판교 고정이라 위치 재생 없이 안내를 보이려고 넣음. 실제 데이터가 아님(PT에서 밝힐 것)
  - 가짜 서버는 차량-일 중복 제거를 안 함(차가 하나). 같은 자리에서 크루즈를 껐다 켜면 통과가 계속 늘어남
  - 크루즈: 콘솔 `full_autonomous` → ACTIVATED, `ready` → ENABLED. 해제 값(USER_OVERRIDE 등)은 재현 불가. 해석은 `CruiseStateInterpreter`에만
  - 판교 에뮬레이터 위치의 geohash는 `wydku4g`
  - 동결 중 이벤트는 쌓였다가 한꺼번에 옴 → 서비스로 동결을 막는 게 전제
  - 차속 권한 대화상자가 "위치 정보"로 뜸. 차속이 없으면 60km/h 조건만 건너뜀
- 확인 방법:
  - 단위 테스트: `./gradlew :app:testDebugUnitTest` (JDK 17, 29개)
  - 에뮬레이터: 앱 설치 → `adb shell pm grant --user 10 com.hyundaiapp.cruisebreaker android.car.permission.CAR_SPEED` → 앱 실행 → 개발 메뉴 S1 → `bash tools/vconsole.sh up` → `put 'gear?gear=Drive' 'controls?speed=100' 'driving-mode?ad_mode_state=full_autonomous'` → 바로 배너·음성. 5초 뒤 `put 'driving-mode?ad_mode_state=ready'` → 해제 기록. 로그: `adb logcat -s CruiseRecord`
  - 같은 주행에서는 다시 안내 안 함 → 다시 보려면 `gear?gear=Park` 후 앱 재시작, 또는 3분 + 새 주행
  - PR 올리기: 이 PC에 `gh` 설치·로그인됨(`C:\Program Files\GitHub CLI\gh.exe`, minalee22)

## 작업 기록 (최신이 위, 지우지 않음)

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
