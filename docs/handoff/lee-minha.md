# 인수인계 — 이민하 (크루즈브레이커 로직)

담당 경로: `app/src/main/java/com/hyundaiapp/cruisebreaker/domain/`, ViewModel

## 현재 상태 (2026-10-30 기준, 매번 덮어쓰기)

- 한 일 요약: 아직 시작 전. 시작점은 장호연이 만들어 둔 뼈대(아래).
- 진행 중 브랜치·PR: 없음
- 다음 할 일(우선순위 순):
  1. **백그라운드 동결 실험**(PRD 5.4) — 11-01까지 결과를 이 파일에 기록
  2. `domain/SegmentKey.kt` geohash 구현 + 테스트
  3. `domain/DisengageDetector.kt` TODO — PRD 5.3. `FakeVehicleSignals`로 단위 테스트 먼저
  4. TTS SDK 동작 확인 → `RiskAnnouncer`
- 막힌 점·질문(누구에게): 없음
- 주의(함정·직접 겪은 이상한 동작):
  - 크루즈: 콘솔 `full_autonomous` → ACTIVATED, `ready` → ENABLED. 해제 값(USER_OVERRIDE 등)은 재현 불가. 해석은 `CruiseStateInterpreter`에만
  - **DO를 선언했는데 adb 설치본은 주행 중(D·80km/h) 차단됨**(2026-10-30 실측). 신호 수신은 계속됨. 주최 측 문의 중
  - 차속 권한 대화상자가 "위치 정보"로 뜸(AAOS가 CAR_SPEED를 위치 그룹으로 묶음)
  - 내비 서비스가 재시작되면 core가 10초 안에 자동 재연결함(core 0.2.1)
  - 조향각은 콘솔 값과 부호 반대
- 확인 방법: 앱 실행 → 개발 메뉴 → 신호 모니터. 값 바꾸기는 `bash tools/vconsole.sh up` 후 `put '...'`(README "시작하기")

## 작업 기록 (최신이 위, 지우지 않음)

### 2026-10-30
- 장호연: 파일 생성. 첫 작업은 README "담당별 첫 작업" 기준으로 적어 둠.
