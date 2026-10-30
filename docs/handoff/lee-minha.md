# 인수인계 — 이민하 (크루즈브레이커 로직)

담당 경로: `app/src/main/java/com/hyundaiapp/cruisebreaker/domain/`, ViewModel

## 현재 상태 (2026-10-30 기준, 매번 덮어쓰기)

- 한 일 요약: **백그라운드 동결 실험 완료** → [docs/background-freeze-experiment.md](../background-freeze-experiment.md)
  - 원인: Android 14 캐시 앱 동결. 앱이 뒤로 간 지 약 10초 뒤 얼고, 그동안 이벤트는 앱이 다시 앞으로 올 때 한꺼번에 늦게 옴(판정에 못 씀)
  - **포그라운드 서비스로 해결됨**(얼지 않고 실시간 수신, 5분 연속 확인). 멀티 윈도우는 시스템에서 꺼져 있어 불가
  - **시동(재부팅) 자동 시작 됨**: 첫 실행 동의 후 부팅 수신기로 서비스가 켜지고 앱을 열지 않아도 실시간 수신(2회). 서비스 시작 직후 약 5초는 놓칠 수 있음
  - **알림**: 알림 센터에 카드 한 장(상단 아이콘·팝업 없음)
  - **일관성**: 다른 앱 3종·경로 안내·주행·앱 화면 닫힘·`am kill` 7개 상황 모두 같은 프로세스로 51~84ms 안에 수신
  - 아직 `app/`에는 반영 안 함(서비스 없음 → 지금 구조로는 뒤에 가면 기록이 멈춤)
- 진행 중 브랜치·PR: 없음. 실험 앱은 저장소 밖 `hyundaiapp/experiments/freeze-probe/`
- 다음 할 일(우선순위 순):
  1. 결과 공유(11-01까지): 장호연(PRD 5.4, 주최 측 문의, 서비스 구조 확인), 황준용(`core/README` 할 일), 이상준(차슐랭 충전 중 생존도 같은 원인일 가능성)
  2. 장호연 확인 후 `app/`에 주행 기록용 포그라운드 서비스 + 부팅 수신기 추가 PR(`feat/recording-service`)
  3. `domain/SegmentKey.kt` geohash 구현 + 테스트
  4. `domain/DisengageDetector.kt` TODO — PRD 5.3. `FakeVehicleSignals`로 단위 테스트 먼저
  5. TTS SDK 동작 확인 → `RiskAnnouncer`
- 막힌 점·질문(누구에게):
  - 장호연: 주최 측 문의에 추가 요청 — 포그라운드 서비스 타입(`specialUse` vs `location`)과 심사 허용 여부, 부팅 자동 시작 허용 여부, 실차가 시동 off 때 완전히 꺼지는지 절전(suspend)하는지
  - 장호연: "주행 중 DO 차단"이 이번 실험(D·80km/h)에서는 재현 안 됨 — 어떤 조건에서 봤는지
- 주의(함정·직접 겪은 이상한 동작):
  - 동결 중 이벤트는 버려지지 않고 쌓였다가 한꺼번에 옴 → 콜백 시각을 이벤트 시각으로 믿으면 안 됨(서비스로 동결을 막는 게 전제)
  - 조향 리스너는 값이 그대로여도 약 3초마다 `-0.0`을 보냄
  - 크루즈: 콘솔 `full_autonomous` → ACTIVATED, `ready` → ENABLED. 해제 값(USER_OVERRIDE 등)은 재현 불가. 해석은 `CruiseStateInterpreter`에만
  - 차속 권한 대화상자가 "위치 정보"로 뜸(AAOS가 CAR_SPEED를 위치 그룹으로 묶음)
  - 내비 서비스가 재시작되면 core가 10초 안에 자동 재연결함(core 0.2.1)
  - 조향각은 콘솔 값과 부호 반대
- 확인 방법:
  - 동결 여부: `adb shell dumpsys activity processes | grep -A3 <패키지>` 의 `isFrozen`
  - 실험 재실행: `experiments/freeze-probe/run.sh <이름> <probe|fgs> <route|drive>` → `analyze.sh <이름>`
  - 앱 신호: 앱 실행 → 개발 메뉴 → 신호 모니터. 값 바꾸기는 `bash tools/vconsole.sh up` 후 `put '...'`(README "시작하기")

## 작업 기록 (최신이 위, 지우지 않음)

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
