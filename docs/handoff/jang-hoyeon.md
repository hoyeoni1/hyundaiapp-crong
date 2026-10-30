# 인수인계 — 장호연 (팀장·총괄)

담당 경로: `tools/`(원본은 차슐랭), `apk/`, `README.md`, `PRD.md`, `docs/`, 릴리스. 이 저장소 앱 뼈대도 만들었음

## 현재 상태 (2026-10-30 기준, 매번 덮어쓰기)

- 한 일 요약:
  - 두 앱 기반 완료, `main`에 반영. 에뮬레이터에서 신호 수신 확인
    - core v0.2.1(초안)
    - 앱 구조·UiState 계약
    - Fake 저장소
    - 개발 메뉴·신호 모니터
  - 아직 없는 것: 판정 로직(TODO), 실제 화면 디자인, 서버, release 서명
- 진행 중 브랜치·PR: 없음(10-30은 `main`에 직접 푸시 — Organization·보호 규칙 전이라 예외)
- 다음 할 일(우선순위 순):
  1. 주최 측 문의 메일 발송(`docs/team-collaboration-plan.md` 9절 1~6번 + "APK 파일명을 영문 앱 이름만으로 써도 되는지")
  2. Pleos Playground 프로젝트 생성·팀원 추가
  3. 황준용에게 core 초안 검토·인계 요청
  4. 시연 스크립트: 위치 재생 시연 모드 + 크루즈 주입 동기화(PRD 5.7)
  5. release 서명 키 생성(키 파일은 저장소에 넣지 않음)
- 막힌 점·질문: 크루즈브레이커 주행 중 화면이 DO 선언에도 차단됨 → 주최 측 답변 대기
- 주의:
  - 결정 사항: 부문 **일반**, 팀명 **삼각고래**, APK는 팀명 없이 `apk/CruiseBreaker_release.apk`
  - `getEvBatteryCapacity`는 `CAR_ENERGY`만으로 권한 부족 → 차종 프리셋 사용
  - Virtual Console은 하트비트가 없으면 쓰기가 503 → `tools/vconsole.sh up` 먼저
- 확인 방법: `./gradlew assembleDebug test` / 에뮬레이터 실행 → 개발 메뉴 → 신호 모니터

## 작업 기록 (최신이 위, 지우지 않음)

### 2026-10-30
- 한 일(Claude 에이전트와 함께):
  - PRD에 부문·팀명 기입
  - core v0.2 설계·구현
    - Vehicle SDK·AAOS 차속·NaviHelper
    - 내비 재연결
    - Fake 신호
    - GeoMath
  - 두 앱 뼈대
    - AppContainer
    - domain/data 뼈대
    - UiStates
    - S1 예시
    - 신호 모니터
  - `tools/vconsole.sh`
  - README "시작하기"
  - 인수인계 규칙(`docs/handoff/`)
- 결정한 것과 이유:
  - 화면은 Compose(UiState로 `@Preview` 먼저 만들 수 있어 UI·로직 병렬 작업에 유리)
  - 판정 로직은 담당자 몫이라 TODO로 남기고, 기준값·모델·예시 테스트만 넣음
  - 차슐랭 DO 선언 제거(정차 전용 앱, PRD 3.2)
  - APK 파일명에서 팀명 제외(한글 파일명 금지 규칙과 맞춤)
- 에뮬레이터 실측:
  - EV·주행·크루즈·차속·위치 수신 OK
  - 내비 앱이 재시작되면 응답이 끊김 → core에서 10초 감시 후 재연결하도록 고침
  - 크루즈브레이커 DO 차단
  - 차속 권한 대화상자가 "위치 정보"로 표시됨
- 바꾼 파일: 거의 전부(커밋 `Add core v0.2 signals and app foundation...`, `Name release APK...`)
