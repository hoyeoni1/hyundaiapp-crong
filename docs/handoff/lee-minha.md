# 인수인계 — 이민하 (크루즈브레이커 로직)

담당 경로: `app/src/main/java/com/hyundaiapp/cruisebreaker/domain/`, ViewModel

## 현재 상태 (2026-11-01 기준, 매번 덮어쓰기)

- 한 일 요약:
  - **판정 규칙 v2 확정** → [docs/disengage-rules.md](../disengage-rules.md). PRD 5.3을 대체(PRD 반영은 장호연 요청)
    - 원인 구분 없음: `BRAKE/OTHER` 유형, 정체 제외 삭제. 차는 사실만 기록, 위험은 서버가 여러 차의 비율로 판단
    - 칸 경계: 서버가 같은 방향 둘레 8칸까지 9칸 합산(차량-일 중복 제거), 최근 28일
    - 안내 문구 한 가지: "1킬로미터 앞, 크루즈 해제가 잦은 구간입니다."
  - **해제 감지(F1)·구간 ID 구현**(`feat/disengage-detection`): `SegmentKey` geohash·둘레 8칸, `DisengageDetector`, `USER_OVERRIDE`를 작동 중으로 고침. 단위 테스트 18개 통과, 에뮬레이터에서 해제·통과 기록 확인(logcat `CruiseRecord`)
  - 업로드는 아직 Fake 저장소(서버 없음). 포그라운드 서비스 없음 → 앱이 뒤로 가면 기록이 멈춤
- 진행 중 브랜치·PR: `feat/disengage-detection` (리뷰·머지 대기)
- 다음 할 일(우선순위 순):
  1. `feat/recording-service`: 포그라운드 서비스 + 부팅 수신기, 서비스가 `signals.start()`·감지기·업로드 수집을 소유(지금은 `CruiseBreakerApp`)
  2. `RiskAnnouncer`: 진행 방향 ±30°, 앞 (설정 거리 ±200m), 같은 구간 1회·3분 간격 + TTS SDK 확인
  3. ViewModel: S2·S3·S4·S5(길민종 화면 콜백 이름 그대로). S4 `autoRecordEnabled`
  4. 11-05 시연 흐름: 해제 → 기록 → 안내 화면(Fake 저장소에 해제가 반영되게)
- 막힌 점·질문(누구에게): `docs/disengage-rules.md` "팀원별 요청" 표
  - 장호연: PRD 3.1·4·5.1·5.2·5.3·5.5·5.6·5.7·7·8절, 기획서 `docs/proposal.md`, README 반영
  - 황준용: `segment_risk` 뷰 9칸 합산·차량-일 중복 제거·28일, 유형·브레이크 비율 컬럼 삭제
  - 길민종: S2 "브레이크로 해제" 막대 삭제(`RiskRowUi.brakeRatio`), 질문 4개 답은 표에 있음
  - 장호연: 주최 측 문의(FGS·부팅 자동 시작 심사, 실차 해제 값, 시동 off 절전 여부)
- 주의(함정·직접 겪은 이상한 동작):
  - 동결 중 이벤트는 버려지지 않고 쌓였다가 한꺼번에 옴 → 콜백 시각을 이벤트 시각으로 믿으면 안 됨(서비스로 동결을 막는 게 전제)
  - 크루즈: 콘솔 `full_autonomous` → ACTIVATED, `ready` → ENABLED. 해제 값(USER_OVERRIDE 등)은 재현 불가. 해석은 `CruiseStateInterpreter`에만
  - 판교 에뮬레이터 위치의 geohash는 `wydku4g`(옛 TODO 주석의 "wydm…"는 틀린 추정)
  - `FakeSegmentRiskRepository` 시드의 구간 ID는 아직 임시 문자열(`seed-a` 등) → 안내기 만들 때 실제 geohash로 바꿀 것
  - 차속 권한 대화상자가 "위치 정보"로 뜸(AAOS가 CAR_SPEED를 위치 그룹으로 묶음). 차속이 없으면 60km/h 조건만 건너뛰고 기록은 함
  - 조향 리스너는 값이 그대로여도 약 3초마다 `-0.0`을 보냄. 조향각은 콘솔 값과 부호 반대
- 확인 방법:
  - 단위 테스트: `./gradlew :app:testDebugUnitTest` (JDK 17)
  - 해제 기록: 앱 설치 → `adb shell pm grant --user 10 com.hyundaiapp.cruisebreaker android.car.permission.CAR_SPEED` → 앱 실행 → `bash tools/vconsole.sh up` → `put 'gear?gear=Drive' 'controls?speed=100' 'driving-mode?ad_mode_state=full_autonomous'` → 5초 뒤 `put 'driving-mode?ad_mode_state=ready'` → `adb logcat -s CruiseRecord`에 `pass`·`disengage`
  - 동결 여부: `adb shell dumpsys activity processes | grep -A3 <패키지>` 의 `isFrozen`

## 작업 기록 (최신이 위, 지우지 않음)

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
