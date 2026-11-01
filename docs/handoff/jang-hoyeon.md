# 인수인계 — 장호연 (팀장·총괄)

담당 경로: `tools/`(원본은 차슐랭), `apk/`, `README.md`, `PRD.md`, `docs/`, 릴리스. 이 저장소 앱 뼈대도 만들었음

## 현재 상태 (2026-11-01 기준, 매번 덮어쓰기)

- 한 일 요약:
  - 두 앱 기반 완료, `main`에 반영. 에뮬레이터에서 신호 수신 확인
    - core v0.2.1(초안)
    - 앱 구조·UiState 계약
    - Fake 저장소
    - 개발 메뉴·신호 모니터
  - 기획서 제출본 완성: 원고 `docs/proposal.md` → 접수양식에 채운 `docs/submission/cruisebreaker-application.docx`(9쪽, Word로 열림 확인)
    - 재생성: `python tools/build-proposal-docx.py docs/submission/application-form-template.docx docs/proposal.md "<앱명>" docs/submission/cruisebreaker-application.docx`
    - 01 참가신청서의 대표자·구성원 연락처, 참가 단위·부문 체크, 작성일, 04 서명은 아직 빈칸(손으로 기입)
  - 아직 없는 것: 판정 로직(TODO), 서버, 포그라운드 서비스, release 서명. 화면은 길민종 PR로 올라옴(아래)
- 진행 중 브랜치·PR:
  - 길민종 `feat/ui-screens` — 차슐랭 https://github.com/hoyeoni1/hyundaiapp-chachelin/pull/1 · 크루즈브레이커 https://github.com/hoyeoni1/hyundaiapp-cruisebreaker/pull/1 — **11-01 Claude 리뷰 `✅ 머지 가능`, 장호연 머지 대기**
  - 장호연 `feat/demo-scripts`(두 저장소, 10-30) — **PR 아직 없음**. 코드라 PR로 올려야 main에 들어감
  - 기타: 기획서(`docs/proposal.md`, `docs/submission/`)는 팀장 결정으로 `main`에 바로 push
- 리뷰·머지: PR 리뷰는 Claude(정기 작업 "삼각고래 PR 리뷰", 매일 09~23시 매시), 머지는 장호연만(`docs/pr-review-guide.md`)
- 다음 할 일(우선순위 순):
  1. 기획서 7번 칸의 【제출 전 기입】 2곳(GitHub 저장소 주소, 시연 영상 링크) 채우고 재생성. Organization은 만들지 않기로 함(10-30) → 개인 계정 링크가 양식의 "계정정보 노출 금지"에 걸리는지 주최 측 답변 확인
  2. 01 참가신청서·04 서명 칸을 손으로 채우고 5명 서명 수집
  3. 주최 측 문의 메일 발송 — **초안 `docs/organizer-inquiry-email.md` 완성(10-31)**, 팀원 CC 넣고 대표자 메일로 발송(9절 1~7번 — 10-31에 1번을 "FGS·부팅 자동 시작 심사 허용"으로 바꾸고 7번 "실차 시동 off 시 완전 종료/절전" 추가)
  4. Pleos Playground 프로젝트 생성·팀원 추가
  5. 황준용에게 core 초안 검토·인계 요청
  6. 시연 스크립트: 위치 재생 시연 모드 + 크루즈 주입 동기화(PRD 5.7)
  7. release 서명 키 생성(키 파일은 저장소에 넣지 않음)
- 막힌 점·질문: 없음. "크루즈브레이커 DO 차단"은 10-31 재확인 결과 차슐랭 화면을 착각한 것(크루즈브레이커 `docs/background-freeze-experiment.md` "함께 확인한 것"에 기록, 이민하 질문 답변 완료)
- 주의:
  - gh는 브라우저 로그인이라 계정 전체 권한이다. 두 저장소로 줄이려면 fine-grained 토큰(Contents 읽기, Pull requests 읽기·쓰기)으로 다시 로그인
  - 결정 사항: 부문 **일반**, 팀명 **삼각고래**, APK는 사이트 규칙(`팀명_앱이름_release.apk`)대로 영문 팀명을 넣어 `apk/SamgakGorae_CruiseBreaker_release.apk`(10-30 변경)
  - `getEvBatteryCapacity`는 `CAR_ENERGY`만으로 권한 부족 → 차종 프리셋 사용
  - Virtual Console은 하트비트가 없으면 쓰기가 503 → `tools/vconsole.sh up` 먼저
  - 이 PC의 CLI 빌드는 JAVA_HOME이 없음 → `export JAVA_HOME="E:\AndroidStudio\jbr"`(JDK 21, Gradle 빌드·테스트 통과 확인)
- 확인 방법: `./gradlew assembleDebug test` / 에뮬레이터 실행 → 개발 메뉴 → 신호 모니터

## 작업 기록 (최신이 위, 지우지 않음)

### 2026-11-01 — 진행 현황 점검·길민종 UI PR 리뷰
- 한 일(Claude 에이전트와 함께): 두 저장소 fetch(main 변경 없음, 새 브랜치 `feat/ui-screens`). PRD 대비 진행 현황 점검 — 판정 로직(이상준·이민하)·서버(황준용)·포그라운드 서비스는 아직 시작 전, 화면만 PR로 올라옴
- 두 PR 리뷰: PR 브랜치에서 `assembleDebug test` 통과, 필수 6항목 통과 → 둘 다 `✅ 머지 가능` 코멘트
  - 차슐랭 후속: S2 실측 속도 분포 빠짐(UiState 필드 필요, 이상준과), 이상준에게 남긴 질문 4개, 에뮬레이터 확인 아직
  - 크루즈브레이커 후속: 10-30 실험에서 정한 **S5 자동 기록 안내 문구·S4 자동 기록 켜기/끄기가 빠짐**(SettingsUiState 필드는 이민하와), 안내 거리 선택지 500m/1km/2km가 PRD 5.3(0.8~1.2km)과 다름
- 결정한 것과 이유: 빠진 항목은 UiState 합의가 필요한 후속 작업이라 머지를 막지 않음(ViewModel 작업이 이 화면 위에서 시작해야 함)
- 남은 일: 장호연 PR 머지, `feat/demo-scripts` PR 올리기. 정기 리뷰 작업이 10-31 PR을 하루 동안 리뷰하지 않았음 → 정기 작업이 실제로 도는지 확인
- 바꾼 파일: 이 문서(코드 변경 없음, PR 코멘트 2개)

### 2026-10-31 — 주최 측 문의 메일 초안
- 한 일(Claude 에이전트와 함께): 9절 1~7번과 `contest-analysis-todo.md` 8절 미확정 항목(심사용 계정, 저장소 주소의 계정명 노출, 시연 영상 형식, 접수 메일 제목, Vehicle Control SDK)을 묶어 메일 초안 10문항 작성
- 결정한 것과 이유: 두 작품 질문을 메일 한 통으로 보냄(문의 창구 하나). 발송은 대표자가 직접 함
- 바꾼 파일: `docs/organizer-inquiry-email.md`(새 파일, 원본은 팀 폴더), `docs/team-collaboration-plan.md` 9절, 이 문서

### 2026-10-31 — "주행 중 DO 차단" 재확인
- 한 일(Claude 에이전트와 함께): 이민하가 물은 "DO 차단 조건"을 에뮬레이터에서 재현. D·80km/h에서 크루즈브레이커는 정차 중 실행 후 주행·주행 중 콜드 스타트·다시 앞으로 세 경우 모두 차단 안 됨(화면 캡처로 확인). DO가 없는 차슐랭은 주행 중 실행하면 "주행 중에는 사용할 수 없습니다" 화면
- 결정한 것과 이유: 10-30 관찰은 차슐랭을 크루즈브레이커로 착각한 것으로 정리(두 앱 다 기본 아이콘이라 독에서 구분이 안 됨). 시연에 S1 주행 화면 포함. 주최 측 문의 6번은 "배포본에서도 DO만으로 허용되는지"로 낮춤
- 주의: 차단 화면이 떠도 `topResumedActivity`는 그 앱으로 나오므로 차단 여부는 화면 캡처로만 판단 가능
- 바꾼 파일: 크루즈브레이커 `docs/background-freeze-experiment.md`·`PRD.md`, 두 저장소 `docs/team-collaboration-plan.md`, 이 문서

### 2026-10-31 — 백그라운드 동결 실험 결과 반영
- 한 일(Claude 에이전트와 함께): 두 저장소 최신 pull, 열린 PR 확인(두 저장소 모두 0개, 리뷰할 것 없음). 이민하 실험 문서(크루즈브레이커 `docs/background-freeze-experiment.md`)의 "장호연 확인 요청" 중 문서 몫 반영
  - 크루즈브레이커 PRD 4절에 "첫 실행 동의 이후"·시동 시 자동 기록, 5.4를 결과(포그라운드 서비스 `specialUse` + 부팅 수신기)로 다시 씀, 8절 동결 행을 남은 위험 중심으로, DO 행에 "재현 안 됨" 메모
  - 두 저장소 `docs/team-collaboration-plan.md` 9절: 1번을 심사 허용 여부로 바꾸고 7번(실차 시동 off 방식) 추가
  - 차슐랭 PRD 8절 "충전 중 생존" 행에 같은 원인·해법 메모(이상준 확인)
- 결정한 것과 이유: 이민하 결정 사항 5개(서비스 타입 `specialUse`, 구현 이민하 PR, S5 동의 후 기본 켜짐·S4에서 끔, S5 안내 문구, PRD 4절 문구)에 이견 없음 → 그대로 진행. 서비스가 `signals.start()`·`disengageDetector.start()`를 소유하는 구조 변경도 동의(코드는 `feat/recording-service` PR에서 리뷰)
- 남은 일: 주최 측 메일 발송(9절 1~7번), DO 차단 조건을 이민하에게 전달
- 바꾼 파일: 크루즈브레이커 `PRD.md`, 차슐랭 `PRD.md`, 두 저장소 `docs/team-collaboration-plan.md`, 이 문서

### 2026-10-30 — 문서는 main에 바로
- 한 일(Claude 에이전트와 함께): CLAUDE.md "작업 방식"에 규칙 추가 — 코드는 브랜치+PR, 문서(PRD·README·CLAUDE.md·docs/)는 PR 없이 main에 바로 push
- 결정한 것과 이유: 문서는 리뷰 대기 없이 바로 반영(장호연 결정). 담당 경로 규칙은 그대로
- 바꾼 파일: `CLAUDE.md`(두 저장소 동일), 이 문서

### 2026-10-30 — APK 파일명에 영문 팀명
- 한 일(Claude 에이전트와 함께): 대회 사이트 지원·제출 방법 페이지 원문 확인 → APK 파일명 규칙이 `팀명_앱이름_release.apk`라 팀명 생략 결정을 되돌림. 기획서 7번 칸·PRD·이 문서를 `SamgakGorae_<앱>_release.apk`로 고치고 DOCX 재생성
- 결정한 것과 이유: 팀명은 로마자 `SamgakGorae`(공지 규칙 + 한글 파일명 금지 둘 다 지킴). GitHub Organization은 만들지 않음(장호연 결정)
- 바꾼 파일: `docs/proposal.md`, `docs/submission/*.docx`, `PRD.md`, 이 문서

### 2026-10-30 — PR 자동 리뷰 켬
- 한 일: 이 PC에 GitHub CLI 설치, `hoyeoni1`로 gh 로그인(브라우저 방식, 계정 전체 권한), 정기 작업 "삼각고래 PR 리뷰" 생성(매일 09~23시 매시 정각)
- 동작: 새 PR·새 커밋만 리뷰 → `C:\Users\H\.claude\pr-review\`의 전용 클론에서 빌드·테스트 → `docs/pr-review-guide.md` 기준으로 PR 코멘트(판정 첫 줄) → 결과를 앱 알림으로 보고. 머지·승인·push는 하지 않음
- 확인: 두 저장소 PR 조회 OK(열린 PR 0개), 전용 클론 빌드·테스트 통과. 실제 PR에 대한 첫 리뷰는 아직 없음

### 2026-10-30 — PR 리뷰·머지 규칙
- 결정: 모든 변경은 PR. 리뷰는 Claude(장호연 계정 에이전트), 머지는 장호연만. 교차 리뷰는 "교차 읽기"(PT 대비, 승인 권한 없음)로 바꿈
- 바꾼 파일: `docs/team-collaboration-plan.md` 2·4·7절, `docs/pr-review-guide.md`(새로 만듦, 리뷰 기준), PR 템플릿, `CLAUDE.md`, README, `docs/handoff/README.md`
- 남은 일: PC에 `gh`(GitHub CLI) 설치·로그인 → 정기 PR 리뷰 작업 설정. 머지 권한을 장호연으로 제한하는 것은 Organization 이전 후 브랜치 보호에서 설정

### 2026-10-30 — 기획서 제출본
- 한 일(Claude 에이전트와 함께): 접수양식 02 파트 1~8번 원고 `docs/proposal.md` 작성, 양식에 자동으로 채우는 `tools/build-proposal-docx.py` 작성, 크루즈브레이커 접수양식 DOCX 생성
- 결정한 것과 이유:
  - 사업성 숫자: 국내 Pleos Connect 탑재 2027년 말 약 23만 대로 추정(그랜저 71,775 + 아반떼 79,335대/년, 2025 현대차 판매 실적). 설치율·사용 비율·Retention 목표는 가정이라고 명시
  - 원고는 마크다운 하나로 관리하고 DOCX는 스크립트로 다시 만든다(손으로 고친 DOCX와 원고가 어긋나지 않게)
  - 두 작품 공동 초안(비교·검토용)은 Claude 문서 https://claude.ai/artifact/MwikFg1WTHiANoDykTUKGc
- 바꾼 파일: `docs/proposal.md`, `docs/submission/`, `tools/build-proposal-docx.py`, 이 문서

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
