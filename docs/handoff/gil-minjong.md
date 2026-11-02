# 인수인계 — 길민종 (두 앱 UI)

담당 경로: `app/src/main/java/com/hyundaiapp/*/ui/`(화면·테마). 상태 계약 `ui/UiStates.kt`는 로직 담당과 합의

## 현재 상태 (2026-11-03 기준, 매번 덮어쓰기)

- 한 일 요약: 할당 이슈 #9·#13의 화면 수정. S5 차속 권한은 설명 뒤 버튼에서만 요청(MainActivity의 자동 요청 제거), 릴리스 첫 실행은 S5 → 시작하기 → S1. 디버그는 개발 메뉴 유지. 동의 전 신호 중지는 PR #10에 이미 반영되어 있어 그대로 사용한다.
  - S5: 알림 선택 권한 카드·명시적 요청(Android 13+, 거부해도 시작 가능), 저속 해제 필터 설명으로 변경. 시작 버튼은 스크롤 영역 밖에 고정.
  - S4: 안내 거리 선택 삭제(500m 고정 설명), `autoRecordEnabled` 토글을 기존 ViewModel에 연결. 끔은 시동 자동 시작만 중지하고 앱을 열면 기록하는 기존 의미 유지.
  - S2: 브레이크 비율 막대 삭제. 이민하 handoff·#9의 합의대로 `RiskRowUi.brakeRatio`·`SettingsUiState.announceDistanceM`와 ViewModel의 해당 투영만 제거(서버·domain 계약은 그대로).
  - #13: 철회 줄의 글자색·대비와 확인 창의 64dp 터치 영역 정리. 철회 후 화면 스택 초기화로 이전 설정 화면으로 돌아가지 않음. 디버그 신호 모니터도 동의 없이 들어갔다 나오면 수신 해제.
  - PR #5 ViewModel 연결 확인: S2·S3·S4·S5 실제 ViewModel 사용. S1 배너는 로직에서 이미 "500m 앞 크루즈 해제 잦은 구간"·10초로 나옴. 미리보기도 맞춤.
- 진행 중 브랜치·PR: `fix/onboarding-and-v2-ui` (이번 PR로 리뷰 요청, 머지 대기).
- 다음 할 일:
  1. x86_64 Pleos PC에서 새 설치 릴리스 첫 실행·알림 허용/거부·차속 허용/거부·동의 철회/재동의 확인.
  2. 에뮬레이터에서 S4 자동 기록 끈 뒤 재부팅, 앱을 다시 열 때 기록되는지 확인. S1 D·80km/h 배너도 확인.
  3. 장호연이 약관 전문을 확정하면 `TermsDialog`의 준비 중 문구 교체.
- 막힌 점·질문: Mac Apple Silicon에서는 Pleos x86_64 이미지 실행 불가. 서버 기록 철회 정책(#12, 황준용)과 core 직접 의존성(#11, 이민하)은 각 담당 이슈에 남아 있음.
- 주의: 화면은 UiState·콜백만 받는다. `MainActivity` 수정은 #9의 명시 요청으로 차속 자동 요청 제거만 포함. ViewModel 수정은 합의된 UiState 필드 삭제에 따른 연결 정리뿐이다. 머지는 장호연 담당.
- 확인 방법: JDK 17로 `bash ./gradlew assembleDebug test :app:lintDebug`. `gradlew`에 실행 비트가 없어 bash로 실행. 첫 화면 라우팅 회귀 테스트 3개, 기존 설정 테스트에 자동 기록 저장·재생성 확인 추가. 화면 렌더 결과는 작업 기록 참조.

## 작업 기록 (최신이 위, 지우지 않음)

### 2026-11-03
- 검증: `assembleDebug test :app:lintDebug` 통과. 앱 단위 테스트 47개(debug/release 각각), core debug 17개/release 3개. 앱 lint 오류 0(기존 경고 12개). 임시 Gradle init+Robolectric 화면 확인 4개 통과: S4 자동 기록 토글·철회 클릭, S5 선택 권한 없이 시작/알림 콜백, S2 브레이크 막대 없음, S1 배너 한 줄·말줄임 없음. 1280×720 PNG를 직접 확인. 임시 테스트/의존성은 커밋하지 않음. Pleos 에뮬레이터 실행은 하지 않음.
- 한 일: GitHub 열린 PR·이슈 및 PR #2·#5·#10 리뷰/인수인계 확인. 길민종 할당 #9·#13 구현. `core/`·서버·판정 규칙은 수정하지 않음.
- 결정: 알림 권한은 선택이며 S5 설명 카드 버튼에서만 요청(API 33 미만은 추가 요청 없음). 철회 후 릴리스는 S5, 디버그는 개발 메뉴. 안내 거리·브레이크 비율은 화면 계약에서 제거(이민하의 기존 요청에 근거).
- 바꾼 파일: `MainActivity.kt`, `ui/{CruiseBreakerNav,InitialScreen,UiStates,PreviewData}.kt`, `ui/components/NotificationPermission.kt`, `ui/{onboarding/OnboardingScreen,risk/RiskListScreen,risk/RiskListViewModel,settings/SettingsScreen,settings/SettingsViewModel}.kt`, UI 단위 테스트, 본 문서.


### 2026-11-01
- 한 일: 화면 PR #1 머지 후 리뷰 후속(브랜치 `fix/ui-review-followups`). S5 왼쪽에 자동 기록 안내 "시동을 걸면 앱을 열지 않아도 주행을 자동으로 기록합니다." 추가. `SelectableButton`에 `role` 파라미터를 두고 S4 안내 거리는 `Role.RadioButton`(차슐랭과 공통 부품 같이 수정). Robolectric 렌더로 S4·S5 확인(설정은 커밋 안 함). `assembleDebug`·`test` 통과.
- 결정한 것과 이유: S4 자동 기록 토글은 `SettingsUiState` 필드가 없어 이번에 안 넣음(`UiStates.kt`는 이민하와 합의 대상)
- 바꾼 파일: `ui/onboarding/OnboardingScreen.kt`, `ui/settings/SettingsScreen.kt`, `ui/components/CarComponents.kt`, `docs/handoff/gil-minjong.md`


### 2026-10-31
- 한 일(2): 와이어프레임대로 코드 구현(브랜치 `feat/ui-screens`). 공통 테마(`CarColors`·`CarDimens`·글자 크기), 공통 부품(`CarComponents.kt`·`CarIcons.kt`), 크루즈브레이커 S1~S5 화면, `*Nav.kt` Placeholder 교체. `assembleDebug`·`test` 통과, Robolectric 렌더로 화면 확인(렌더 설정은 커밋 안 함).
- 결정한 것과 이유(2):
  - 두 앱이 같은 부품 파일을 복사해 쓴다: `core/`는 황준용 담당이라 공통 UI 모듈을 만들지 않음
  - ViewModel이 없는 화면은 `*Nav.kt`에서 임시 상태를 들고 콜백을 연결: 개발 메뉴에서 바로 눌러 볼 수 있게
- 바꾼 파일(2): `ui/theme/Theme.kt`, `ui/components/`(새로), `ui/*Nav.kt`, 각 화면 폴더(`ui/<화면>/`), `docs/handoff/gil-minjong.md`
- 한 일: 두 앱 공통 테마 + 전체 화면 와이어프레임 16장(크루즈브레이커 6장, 차슐랭 9장, 테마 1장)을 Claude Design 캔버스로 만듦(https://claude.ai/artifact/9vidSJtHrxzCqtEcjJjVTf). 코드 변경 없음.
- 결정한 것과 이유:
  - 1280×720 가로 기준, 어두운 배경 + 큰 글씨(차량 화면 가이드 2.3: 큰 터치 영역·높은 대비·텍스트 최소화)
  - 강조색은 파랑·주황 둘만: 색약에서도 밝기로 구분되고, 두 앱이 같은 의미(파랑=정보·인증, 주황=위험·실패)로 쓴다
  - 크루즈브레이커 S1 배너는 주황 바탕 한 줄 64sp, 아이콘 1개. 평소에는 "기록 중" 작은 표시만
  - 차슐랭 S4는 별 96px·태그 72px 버튼, 키보드 없음. 세로 화면은 좌우 2단 → 위아래 1단 규칙
- 바꾼 파일: `docs/handoff/gil-minjong.md`

### 2026-10-30
- 장호연: 파일 생성. 첫 작업은 README "담당별 첫 작업" 기준으로 적어 둠.
