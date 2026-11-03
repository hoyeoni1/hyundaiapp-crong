# 인수인계 — 길민종 (두 앱 UI)

담당 경로: `app/src/main/java/com/hyundaiapp/*/ui/`(화면·테마). 상태 계약 `ui/UiStates.kt`는 로직 담당과 합의

## 현재 상태 (2026-11-03 기준, 매번 덮어쓰기)

- 한 일 요약: 할당 이슈 #9·#13의 화면 수정. S5 차속 권한은 설명 뒤 버튼에서만 요청(MainActivity의 자동 요청 제거), 릴리스 첫 실행은 S5 → 시작하기 → 정차 중 S2(주행/신호 대기 중에는 S1). 디버그는 개발 메뉴 유지. 동의 전 신호 중지는 PR #10에 이미 반영되어 있어 그대로 사용한다.
  - S5: 알림 선택 권한 카드·명시적 요청(Android 13+, 거부해도 시작 가능), 저속 해제 필터 설명으로 변경. 시작 버튼은 스크롤 영역 밖에 고정.
  - S4: 안내 거리 선택 삭제(500m 고정 설명), `autoRecordEnabled` 토글을 기존 ViewModel에 연결. 끔은 시동 자동 시작만 중지하고 앱을 열면 기록하는 기존 의미 유지.
  - S2: 브레이크 비율 막대 삭제. 이민하 handoff·#9의 합의대로 `RiskRowUi.brakeRatio`·`SettingsUiState.announceDistanceM`와 ViewModel의 해당 투영만 제거(서버·domain 계약은 그대로).
  - #13: 철회 줄의 글자색·대비와 확인 창의 64dp 터치 영역 정리. 철회 후 화면 스택 초기화로 이전 설정 화면으로 돌아가지 않음. 디버그 신호 모니터도 동의 없이 들어갔다 나오면 수신 해제.
  - 릴리스 화면 연결: 동의한 사용자가 P이면 S2 목록에서 S4 설정·S3 요약에 접근. 플랫폼 UI 제한이 있거나 미확인이면 S1만 표시. 플랫폼이 정차 UI를 허용하면 기어가 늦어도 목록·설정 접근 가능하되, D/R/N·비정상 차속 수신은 S1 유지. 동의 전 신호를 새로 구독하지 않음.
  - D 후 P 복귀 시 S3 요약 한 번 자동 표시, [확인]/[위험 구간 보기]는 S2로 복귀. 주행 중 시스템 뒤로 가기로 정차 화면이 나타나지 않도록 내부 스택 처리 중지(시스템 뒤로 종료는 유지). 디버그 개발 메뉴도 동의 후 주행 중에는 숨김.
  - PR #5 ViewModel 연결 확인: S2·S3·S4·S5 실제 ViewModel 사용. S1 배너는 로직에서 이미 "500m 앞 크루즈 해제 잦은 구간"·10초로 나옴. 미리보기도 맞춤.
  - 줄바꿈 정리: 공통 제목·본문에 한국어 로캘과 문단 줄바꿈 적용. 좁은 동의 카드에서는 버튼을 아래로 옮겨 본문 폭 확보. 시작 버튼은 한 줄 "시작하기"로 통일하고 비활성 사유는 별도 안내. S5 소개·개인정보 설명을 의미 단위로 분리. S1 배너의 한 줄 규칙은 유지.
  - 선택 리뷰 후속: UI 제한을 `CarUxRestrictionsManager`로 관찰. 동의 전에도 주행 제한/미확인이면 S1만 표시하며 차량·위치 수신은 시작하지 않음. 플랫폼이 정차 화면을 허용하면 기어 미수신 상태에서도 설정·철회 가능. 화면 composition 수명 동안 연결 유지, 복귀(ON_START) 시 현재 제한 상태 재확인, dispose에서 연결 해제. 실제 단절·오류는 null로 차단, 오래된 연결 콜백 무시. Nav의 Context 중복 조회 제거.
  - 알림 권한 반복 거부 시 [알림 설정] 진입과 복귀 상태 갱신을 차슐랭과 동일하게 반영.
- 진행 중 브랜치·PR: `fix/onboarding-and-v2-ui` — https://github.com/hoyeoni1/hyundaiapp-cruisebreaker/pull/16 (리뷰·머지 대기).
- 다음 할 일:
  1. x86_64 Pleos PC에서 첫 P 신호와 UX 제한 상태가 도착하는 시점·연결 지연/단절 시 S1 유지·복귀 시 회복을 확인. 새 설치 릴리스 첫 실행·알림 허용/거부·차속 허용/거부·동의 철회/재동의와 P 목록 → D 주행 → P 요약 → S2 복귀 확인.
  2. 에뮬레이터에서 S4 자동 기록 끈 뒤 재부팅, 앱을 다시 열 때 기록되는지 확인. S1 D·80km/h 배너도 확인.
  3. 장호연이 약관 전문을 확정하면 `TermsDialog`의 준비 중 문구 교체.
- 막힌 점·질문: Mac Apple Silicon에서는 Pleos x86_64 이미지 실행 불가. 팀원이 x86 Pleos 릴리스 `208e175`에서 첫 P의 DO=false, S5 권한, D 배너·P 요약, 철회·동의 전 D 차단·재동의까지 확인함(PR #16 댓글). 이번 연결 유지 변경의 실제 PC 재확인과 자동 기록 끔/재부팅·Car 서비스 단절은 남음. 서버 기록 철회 정책(#12, 황준용)과 core 직접 의존성(#11, 이민하)은 각 담당 이슈에 남아 있음.
- 주의: 화면은 UiState·콜백만 받는다. `MainActivity` 수정은 #9의 명시 요청으로 차속 자동 요청 제거만 포함. ViewModel 수정은 합의된 UiState 필드 삭제에 따른 연결 정리뿐이다. 머지는 장호연 담당.
- 주의: 비자동차 기기나 Car 서비스 연결 실패에서는 UX 제한이 미확인이므로 S1에 머문다. UI 전용 래퍼이며 다른 앱도 공통 사용이 필요해지면 황준용과 core 이관을 협의한다. 차속 권한 거부 시 정차 해제가 기록될 수 있는 동작은 기존 판정 규칙이며 이민하/기획 확인 사항.
- 확인 방법: JDK 17로 `bash ./gradlew assembleDebug test :app:lintDebug`. `gradlew`에 실행 비트가 없어 bash로 실행. 첫 화면 라우팅 회귀 테스트 3개, 기존 설정 테스트에 자동 기록 저장·재생성 확인 추가. 화면 렌더 결과는 작업 기록 참조.

## 작업 기록 (최신이 위, 지우지 않음)

### 2026-11-03 — 추가 선택 리뷰 반영
- 새 리뷰: 두 PR 모두 머지 가능. 크루즈 `208e175`는 팀원이 실제 x86 Pleos 릴리스 시연 흐름 확인까지 통과(PR #16 댓글 참조).
- 알림: 요청 전에 이력을 저장하지 않고, 허용 또는 rationale로 선택 결과가 확인된 뒤 기록. 첫 창 미선택 닫기는 다시 요청 가능. 요청 진행 상태를 저장해 시스템 창 뒤의 버튼이 "알림 설정"으로 바뀌지 않게 함. 최초 취소·실제 반복 거부·진행 중 라벨 회귀 테스트 각 3개 추가(두 앱 동일).
- UX 연결: ON_STOP에서 연결을 끊고 null로 만들던 처리를 제거. composition 수명 동안 구독을 유지하고 ON_START에 플랫폼 현재 값 재조회. 실제 단절·조회 예외는 여전히 null로 S1 차단, dispose에서 리소스 해제. 기어·주행 제한을 완화하지 않음.
- 검증: 임시 Robolectric에서 설정 화면 왕복 시 연결 해제 0회·복귀 시 주행 제한 재조회·실제 미확인 신호 S1·dispose 해제 1회 확인. app debug/release 각각 62개 테스트, 빌드·앱 lint 확인. 자동 기록 끔/재부팅·실제 Car 단절·이번 연결 유지의 Pleos 재확인은 남음.


### 2026-11-03 — 선택 리뷰 반영
- 최종 확인: `assembleDebug test :app:lintDebug` 통과. app debug/release 각각 59개, core debug 17/release 3개, 실패 0. 임시 UI 검사 8개 통과.
- 한 일: 리뷰의 선택 제안도 반영. 알림 영구 거부/재요청 불가 시 명시적 버튼으로 설정 진입, 요청 이력과 rationale로 최초 요청 구분, 복귀 시 실제 권한 재확인. UI 설정 이력은 단순 요청 여부만 저장하며 서비스·동의 계약은 바꾸지 않음.
- S5/정차 화면: 플랫폼 UX 제한을 동의 전부터 확인해 제한 중 터치 화면을 숨김. 정차 허용이 확인되면 기어가 없어도 설정·철회 접근. 비동기 Car 연결, 리스너 해제, 생명주기 복귀, 지난 연결 콜백 차단. 새 퍼미션·core 변경 없음.
- 검증: 권한 분기 4개·플랫폼 제한 정책 2개 회귀 테스트 추가. 임시 Robolectric에서 기어 미수신+플랫폼 정차 허용 → 설정/철회, 동의 전 플랫폼 제한/미확인 → 클릭 요소 0개 확인. 실제 Car 서비스 연결은 Pleos PC 확인 필요.


### 2026-11-03 — 텍스트 줄바꿈 정리
- 문제: 차슐랭 실패 안내의 "요." 한 줄 고립, 좁은 동의 카드의 "필수"·"선택"·권한 설명 및 긴 시작 버튼 문구 잘림. 크루즈 S5 제목도 좁은 폭에서 "곳을"만 한 줄에 남음.
- 한 일: 동의 카드의 500dp 미만 본문 폭에서 버튼을 아래로 배치, 공통 시작 버튼/비활성 사유 분리. 공통 테마의 한국어 로캘·제목/본문 줄바꿈 설정을 두 앱에 동일 반영. 앱별 소개·실패 설명을 의미 단위로 나눔.
- 검증: 기본 화면 22개와 추가 좁은 화면 3개(가로 1024×600, 세로 600×1024)를 다시 렌더해 실제 줄 배치 확인. 가로·세로 PDF 23쪽도 줄바꿈 정리본으로 재생성. 임시 렌더 설정·샘플 데이터는 커밋하지 않음.
- 바꾼 파일: `ui/components/CarComponents.kt`, `ui/theme/Theme.kt`, `ui/onboarding/OnboardingScreen.kt`, 본 문서.


### 2026-11-03 — 릴리스 화면 흐름 연결
- 문제: 디버그 개발 메뉴 외에는 S2·S4 진입 경로가 없어 릴리스 동의 후 S1에 갇힘. PRD 시나리오·5.5의 정차 후 S3 요약도 자동 표시되지 않음.
- 한 일: 정차용 기본 화면을 S2로 연결, D 후 P에서 S3 한 번 자동 표시, 요약에서 S2로 돌아가기. S1은 기존처럼 UiState만 받으며 터치 요소 없음. UI Nav에서 기존 `VehicleSignals.drive`만 읽어 주행/기어 미수신 시 정차 화면과 열린 약관·철회 창을 숨김. 기록·판정·서비스·core는 변경하지 않음.
- 검증: `assembleDebug test :app:lintDebug` 통과. 앱 테스트 53개(debug/release 각각), core debug 17/release 3. 회귀 테스트 6개(정차/권한 없음, D·R·N·미수신, P·차속 모순, 동의 전 첫 화면, D→N→P 한 번 요약, 철회 초기화). 임시 Robolectric 실제 Nav 테스트 3개 통과(릴리스 전체 흐름·철회 창 도중 주행·디버그 메뉴 중 주행), 주행 중 클릭 요소 0개 확인. 요약 렌더 직접 확인. Pleos 에뮬레이터·실차·CarUxRestrictions 연동 검증을 대신하지 않음.
- 바꾼 파일: `ui/{CruiseBreakerNav,InitialScreen,ScreenPolicy}.kt`, `ui/{InitialScreenTest,ScreenPolicyTest}`, 본 문서. Nav는 상태·콜백 의존성을 분리하여 가짜 신호로도 전체 흐름을 검증할 수 있게 함.

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
