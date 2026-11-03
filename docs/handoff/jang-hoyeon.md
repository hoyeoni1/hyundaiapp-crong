# 인수인계 — 장호연 (팀장·총괄)

담당 경로: `tools/`(원본은 차슐랭), `apk/`, `README.md`, `PRD.md`, `docs/`, 릴리스. 이 저장소 앱 뼈대도 만들었음

## 현재 상태 (2026-11-02 기준, 매번 덮어쓰기)

- 한 일 요약:
  - 두 앱 기반 + 이민하의 해제·통과 감지, 500m 앞 안내(배너·Pleos TTS 음성), S2~S5 ViewModel, 기록 포그라운드 서비스·부팅 자동 시작이 `main`에 들어옴(PR #3~#6, 11-01 머지, main `ccfdcdc`). 에뮬레이터 흐름은 이민하가 확인(`docs/handoff/lee-minha.md`)
    - 11-02: PR #10(동의 철회, 이슈 #7·#8)과 황준용 core v0.4.0 debug SDK 진단(PR #14)도 머지. 머지된 main(`bdc8f88`)에서 `assembleDebug test` 통과(디버그 테스트 61개, 실패 0)
  - 문서를 판정 규칙 v2(`docs/disengage-rules.md`)에 맞춤(11-01): `PRD.md`, 기획서 원고 `docs/proposal.md`, `README.md`
    - **기획서 DOCX(`docs/submission/cruisebreaker-application.docx`)는 아직 옛 원고(1km 안내·정체 필터) 기준** → 재생성 필요(호공이 PC에 python 없음)
    - 재생성: `python tools/build-proposal-docx.py docs/submission/application-form-template.docx docs/proposal.md "<앱명>" docs/submission/cruisebreaker-application.docx`
    - 01 참가신청서의 대표자·구성원 연락처, 참가 단위·부문 체크, 작성일, 04 서명은 아직 빈칸(손으로 기입)
  - 아직 없는 것: 실제 서버(황준용 — Supabase로 확정(11-02, PC 호스팅 안 함). 황준용이 이미 작업했고 곧 커밋 예정), 위치 재생 시연 모드, release 서명
- 진행 중 브랜치·PR·이슈:
  - 열린 PR 없음(PR #1~#6, #10, #14 머지)
  - 열린 이슈: #9·#13(길민종), #11(이민하, androidx.core 직접 의존), #12(황준용, 동의 철회 시 서버 기록 처리 — 제출 전). #7·#8은 PR #10으로 닫힘
  - 장호연 `feat/demo-scripts`(10-30, 커밋 2개) — **PR 없음**. main보다 27커밋 뒤처짐, main을 merge하면 이 문서(`docs/handoff/jang-hoyeon.md`)에서 충돌(11-01 확인, 강제 해결 안 함). 스크립트 내용도 옛 규칙(1km 안내, 정체 장면 `jam-*`) 기준이라 v2에 맞게 고쳐야 함
  - 기타: 문서(`PRD.md`, `README.md`, `docs/`)는 PR 없이 `main`에 바로 push(10-30 결정)
- 리뷰·머지: PR 리뷰는 Claude(정기 작업 "삼각고래 PR 리뷰", 매일 09~23시 매시), 머지는 장호연만(`docs/pr-review-guide.md`). 11-02 PR #14·차슐랭 #3·#4에 코멘트가 달려 정상 동작 확인
- 다음 할 일(우선순위 순):
  1. 황준용 Supabase 서버 커밋·PR 받으면 리뷰·머지 → 두 앱 Fake 저장소 교체 확인(서버 방식은 11-02 Supabase로 확정)
  2. 주최 측 문의 메일 발송 — 초안 `docs/organizer-inquiry-email.md` **11문항**(11-01 11번 `TTS_SERVICE` 권한 추가), 팀원 CC 넣고 대표자 메일로 발송
  3. 기획서 DOCX 재생성(원고가 11-01 v2로 바뀜) + 7번 칸 【제출 전 기입】 2곳(GitHub 주소, 시연 영상). 기능 번호가 F1·F3·F4·F5(F2 삭제)로 비어 있음 → 번호를 당길지 결정
  4. 이슈 #9·#11·#12·#13 진행 확인(11-05 중간 점검). #12는 제출 전 필수
  5. `feat/demo-scripts`: main merge 충돌(이 문서) 직접 해결 + `demo-drive.sh`를 v2(500m, 저속 해제 장면)로 고친 뒤 PR(두 저장소)
  6. (끝남) 정기 리뷰 작업 점검 — 11-02 정상
  7. 01 참가신청서·04 서명 칸 채우고 5명 서명 수집
  8. Pleos Playground 프로젝트 생성·팀원 추가, 황준용에게 core 초안 인계·서버 요청(`docs/disengage-rules.md` "팀원별 요청")
  9. release 서명 키 생성(키 파일은 저장소에 넣지 않음)
- 막힌 점·질문:
  - GitHub 아이디 확인(11-02 PR로): 이상준 `wklee316`, 황준용 `Aodlf123` → `docs/handoff/README.md`에도 적을 것
  - Organization은 만들지 않음(10-30). 심사용 계정 권한·저장소 주소의 계정명 노출은 주최 측 답변 대기(팀 문서 4절 1번, 9절 4번)
- 주의:
  - gh는 브라우저 로그인이라 계정 전체 권한이다. 두 저장소로 줄이려면 fine-grained 토큰(Contents 읽기, Pull requests 읽기·쓰기)으로 다시 로그인
  - 결정 사항: 부문 **일반**, 팀명 **삼각고래**, APK는 `apk/SamgakGorae_CruiseBreaker_release.apk`(10-30)
  - 판정 규칙의 "28일"은 집계 기간(최근 28일만 셈)이다. 서버 보관 기간·삭제 정책은 아직 정하지 않음(이슈 #7과 함께 황준용과 정할 것)
  - Virtual Console은 하트비트가 없으면 쓰기가 503 → `tools/vconsole.sh up` 먼저
  - 장호연 개발 PC의 CLI 빌드는 JAVA_HOME이 없음 → `export JAVA_HOME="E:\AndroidStudio\jbr"`(JDK 21, Gradle 빌드·테스트 통과 확인). 호공이(OpenClaw) PC에는 JDK·SDK·python이 없어 빌드·DOCX 재생성 불가
- 확인 방법: `./gradlew assembleDebug test` / 에뮬레이터 실행 → 개발 메뉴 → 신호 모니터 / 기록 흐름은 `docs/handoff/lee-minha.md` "확인 방법"

## 작업 기록 (최신이 위, 지우지 않음)

### 2026-11-03 — 기획서 md 초안 갱신(DOCX 재생성 전)
- 한 일(Claude 에이전트와 함께, 장호연 지시): 두 앱 `docs/proposal.md`에 10-30·11-01 이후 결정(11-02 PRD 반영분, 길민종 PR #6·#16 내용)을 넣음. 양식 칸 구조(## 1.~8.)는 그대로
  - 크루즈브레이커: 개인정보·안전에 동의 전 신호 안 받음, 권한은 설명 후 요청(거부해도 사용), 자동 기록 끄기, 철회 시 서버 즉시 삭제, `CarUxRestrictions`로 정차 화면 허용 판단(PR #16), 7번 칸에 고정 위치 시연·시연용 위험 구간
- 결정한 것과 이유: 기능 번호 F2 빈칸은 PRD·PR·문서가 모두 같은 번호를 써서 기획서만 당기지 않음
- 남은 것: DOCX 재생성(`tools/build-proposal-docx.py`, python 필요), 7번 칸 【제출 전 기입】 2곳(GitHub 주소·시연 영상)
- 바꾼 파일: 두 저장소 `docs/proposal.md`, 이 문서

### 2026-11-02 — 결정 3개 PRD 반영(이상준 질문·시연 배속·서버)
- 한 일(Claude 에이전트와 함께, 장호연 결정): Claude 추천안을 그대로 승인하고 PRD에 반영. 팀원 메시지 초안은 채팅으로 받음(발송은 장호연)
- 결정한 것과 이유:
  1. 차슐랭 PRD 5.3에 없는 경우는 기록하지 않음(이상준 안 승인): 성공도 실패도 아닌 세션이 점수를 흐리지 않게. 단 충전소 **조회 실패**는 "충전소 아님"으로 단정하지 않고 업로드 전에 다시 조회(서버 연결 후 인증 충전 누락 방지)
  2. 앱이 뒤에 있을 때 결과 알림 넣기(누르면 S4/S5, S7에서 알림 권한 요청). 리뷰가 핵심인데 지금은 앱을 열어야만 평가 화면이 보임. 서버 다음 순서(이상준)
  3. 시연 배속 ×120 유지, PT·영상 자막에서 밝힘(실제 속도로는 30분)
  4. 동의 철회 → 가명 차량 ID로 서버 삭제 함수 호출, 그 차량 기록 즉시 완전 삭제, 앱은 성공할 때까지 재시도(두 앱 같은 방식, 크루즈브레이커 이슈 #12)
  5. 충전소 밖 충전(`NOT_AT_STATION`)은 서버에 안 올리고 기기에만(S6 표시). 필요한 데이터만 수집, PRD 5.5 스키마와 일치
- 바꾼 파일: `PRD.md`(5.6 동의 철회 삭제, 7절 고정 위치 시연), 이 문서. 차슐랭 `PRD.md`도 같은 날 반영(차슐랭 인수인계 참고)

### 2026-11-02 — 황준용 core v0.4.0 PR #14 머지
- 한 일(Claude 에이전트와 함께, 장호연 지시): 차슐랭 #4(core 원본) 머지 직후 head가 리뷰한 `43ddd76`인지 확인하고 merge commit으로 머지(`bdc8f88`). 이어 차슐랭 #3 머지(차슐랭 `docs/handoff/jang-hoyeon.md`)
- 확인: 머지된 main에서 `assembleDebug test` 통과(디버그 테스트 61개, 실패 0). `core/`가 차슐랭과 같음. 11-01 "머지된 main 빌드 미검증"도 이걸로 해소
- 결정: `tools/windows-*.ps1`·`app/build.gradle.kts` 한 줄·`docs/chachelin-pr-3-review.md` 모두 받아들임
- 바꾼 파일: 이 문서

### 2026-11-01 저녁 — PR #3~#6 머지 후속: 이슈 등록, 판정 규칙 v2 문서 반영
- 한 일(Claude 에이전트 호공이와 함께, 장호연 지시):
  - 이민하 PR #3~#6 리뷰 후 장호연 지시로 merge commit 머지: #3 `dc658e2`, #4 `b484809`, #5 `af008f9`, #6 `ccfdcdc`. **빌드 미검증**(이 PC에 JDK 없음) → 개발 PC에서 `assembleDebug test` 필요
  - 정기 리뷰 작업 확인: #3에는 `✅ 머지 가능` 코멘트(11-01 18:10)가 있으나 #4~#6에는 코멘트·리뷰가 없음 → 점검 필요
  - 리뷰 결과를 이슈로 등록: #7 동의 철회 경로 없음(이민하, 제출 전 필수), #8 minSdk 28에서 API 29+ 호출·TTS ready 가시성·주변 조회 지연(이민하), #9 S5 첫 실행 흐름·S2/S4/S5 정리(길민종, 이민하가 handoff에 남긴 길민종 요청과 합침)
  - 문서를 `docs/disengage-rules.md`(판정 규칙 v2)에 맞춤: `PRD.md` 1·3.1·4·5.1·5.2·5.3·5.4·5.5·5.6·5.7·7·8절·부록, `docs/proposal.md`, `README.md`
  - 주최 측 문의 메일 11번(`pleos.car.permission.TTS_SERVICE` 승인·신청 절차, 이민하 요청) 추가, 팀 문서 9절 8번 추가
  - 팀 문서 4절 1번 "Organization으로 이전(황준용, 11-01까지)"을 10-30 결정(만들지 않음, 개인 계정 유지 — 심사용 계정 권한은 주최 측 문의 4번 답변 후 결정)으로 정정, 2절·8절 Organization 문구도 맞춤. 공통 문서라 차슐랭에서 고치고 같은 내용으로 복사
  - `feat/demo-scripts`(두 저장소): main을 merge해 보니(`git merge-tree`) 이 문서에서 충돌 → 규칙대로 멈춤, push·PR 안 함
  - 조사(읽기만): 황준용·이상준의 원격 브랜치·커밋·PR 없음. 커밋 작성자는 장호연·이민하·길민종(giljong)뿐. `core/`·`tools/`·공통 문서는 두 저장소가 같음(파일 해시 비교, git tree `core` `6ac11fa`, `tools` `773e9c2`)
- 결정한 것과 이유:
  - 기획서·PRD의 기능 번호는 F2만 지우고 나머지 번호는 그대로(코드 주석 `F1`·`F4`와 맞춤). 제출본에서 번호를 당길지는 장호연 결정
  - PRD 7절 시연 5단계를 "60km/h 미만에서 해제하면 기록 안 함"으로 교체(`docs/disengage-rules.md` 이민하 제안 그대로)
  - "28일"은 규칙 문서대로 집계 기간으로만 적음(보관·삭제 기간은 정해진 것이 없어 쓰지 않음)
- 바꾼 파일: `PRD.md`, `README.md`, `docs/proposal.md`, `docs/organizer-inquiry-email.md`, `docs/team-collaboration-plan.md`(차슐랭과 같음), 이 문서. 코드 변경 없음, 이슈 3개

### 2026-11-01 — 팀원 작업 확인·길민종 PR 2번 머지
- 한 일(Claude 에이전트와 함께): 두 저장소 fetch. 새 작업은 길민종 `fix/ui-review-followups`(PR 2번)뿐. 이상준·이민하·황준용은 원격 브랜치·커밋 없음, 인수인계 문서도 10-30 이후 변경 없음
- PR 2번: 자동 리뷰 `✅` 확인, head가 리뷰한 커밋과 같은지 확인 후 merge commit으로 머지(차슐랭 `0f17abb`, 크루즈브레이커 `678d7a6`). 머지된 main에서 두 앱 `assembleDebug test` 통과
- 다음: 11-05 중간 점검 전에 이상준·이민하·황준용 진행 상황 직접 확인

### 2026-11-01 — 진행 현황 점검·길민종 UI PR 리뷰
- 한 일(Claude 에이전트와 함께): 두 저장소 fetch(main 변경 없음, 새 브랜치 `feat/ui-screens`). PRD 대비 진행 현황 점검 — 판정 로직(이상준·이민하)·서버(황준용)·포그라운드 서비스는 아직 시작 전, 화면만 PR로 올라옴
- 두 PR 리뷰: PR 브랜치에서 `assembleDebug test` 통과, 필수 6항목 통과 → 둘 다 `✅ 머지 가능` 코멘트
  - 차슐랭 후속: S2 실측 속도 분포 빠짐(UiState 필드 필요, 이상준과), 이상준에게 남긴 질문 4개, 에뮬레이터 확인 아직
  - 크루즈브레이커 후속: 10-30 실험에서 정한 **S5 자동 기록 안내 문구·S4 자동 기록 켜기/끄기가 빠짐**(SettingsUiState 필드는 이민하와), 안내 거리 선택지 500m/1km/2km가 PRD 5.3(0.8~1.2km)과 다름
- 결정한 것과 이유: 빠진 항목은 UiState 합의가 필요한 후속 작업이라 머지를 막지 않음(ViewModel 작업이 이 화면 위에서 시작해야 함)
- 머지: 장호연이 직접 지시해 Claude가 두 PR을 merge commit으로 머지(차슐랭 `b147aa1`, 크루즈브레이커 `2dd99af`). 머지 후 main에서 `assembleDebug test` 통과
- 남은 일: `feat/demo-scripts` PR 올리기. 정기 리뷰 작업이 10-31 PR을 하루 동안 리뷰하지 않았음 → 정기 작업이 실제로 도는지 확인
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
