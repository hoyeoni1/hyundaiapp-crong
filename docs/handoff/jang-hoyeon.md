# 인수인계 — 장호연 (팀장·총괄)

담당 경로: `tools/`(원본은 차슐랭), `apk/`, `README.md`, `PRD.md`, `docs/`, 릴리스. 이 저장소 앱 뼈대도 만들었음

## 현재 상태 (2026-11-07 기준, 매번 덮어쓰기)

- 한 일 요약:
  - **11-05 내비 위 큰 배너 팝업**: 앱이 뒤에 있을 때 안내 순간 주행 화면(큰 배너)을 내비 위로 올리고 10초 뒤 내비로 복귀, 정차하면 S3 자동. PR #49 → `d6d4bd4` **머지 완료**, release APK `apk/` 재커밋(아래 작업 기록). 그 전 단계(헤드업 알림, PR #48 → `398da90`)는 머지됨
  - **11-05 시연 흐름 확정**: 확정 시나리오 `docs/demo-scenario.md`(고정 위치, 경로 재생 없음). 앱은 debug·release 모두 S5→S2로 시작, 개발 메뉴는 S2 제목 길게 누르기, UX 제한 미확인 5초 뒤 정차 화면 허용. 에뮬레이터 + 실제 서버로 처음부터 끝까지 확인. 크루즈 PR #47 → `0823124`, 차슐랭 PR #35 → `1047949` **머지 완료**, release APK `apk/` 커밋(아래 작업 기록)
  - 두 앱 기반 + 이민하의 해제·통과 감지, 500m 앞 안내(배너·Pleos TTS 음성), S2~S5 ViewModel, 기록 포그라운드 서비스·부팅 자동 시작이 `main`에 들어옴(PR #3~#6, 11-01 머지, main `ccfdcdc`). 에뮬레이터 흐름은 이민하가 확인(`docs/handoff/lee-minha.md`)
    - 11-02: PR #10(동의 철회, 이슈 #7·#8)과 황준용 core v0.4.0 debug SDK 진단(PR #14)도 머지. 머지된 main(`bdc8f88`)에서 `assembleDebug test` 통과(디버그 테스트 61개, 실패 0)
  - 문서를 판정 규칙 v2(`docs/disengage-rules.md`)에 맞춤(11-01): `PRD.md`, 기획서 원고 `docs/proposal.md`, `README.md`
    - 기획서 DOCX(`docs/submission/cruisebreaker-application.docx`)는 11-07 `docs/proposal.md`(고정 위치 시연 문구 반영)로 재생성함
    - 재생성: `python tools/build-proposal-docx.py docs/submission/application-form-template.docx docs/proposal.md "<앱명>" docs/submission/cruisebreaker-application.docx`
    - 01 참가신청서의 대표자·구성원 연락처, 참가 단위·부문 체크, 작성일, 04 서명은 아직 빈칸(손으로 기입)
  - 실제 서버는 11-04 연결 확인(이민하). 위치 재생 시연 모드는 **만들지 않기로 함**(11-05 장호연 결정, 고정 위치 시연)
- 진행 중 브랜치·PR·이슈:
  - 장호연 `fix/core-startup-resilience` — **11-04 머지 완료**(크루즈 #42 → `9ff1318`, 차슐랭 #30 → `0aa4032`). core v0.5.2 시작 크래시 방어(차슐랭 원본 동기화) + `RiskAnnouncer` 안내 3분 간격을 새 주행에서 초기화. 이민하 동의 코멘트 있음, 황준용(core) 확인 코멘트 없음 → 알릴 것
  - 열린 PR 없음(11-07 확인, 마지막 머지 #49)
  - 열린 이슈: #37(길민종·황준용, S3 빈 주행 요약·서버 시드 이름)만 남음(11-07 확인)
  - 장호연 `feat/demo-scripts`(두 저장소) — 11-02 main merge(인수인계 충돌은 main 쪽으로 해결) + 판정 규칙 v2·현재 앱에 맞게 고쳐 PR. `demo-drive.sh` 기본은 고정 위치 시연(위치 재생 없이 지금 앱으로 동작, 이민하 확인 순서와 같음), `route`는 앱 위치 재생 모드가 생긴 뒤. 가짜 콘솔·가짜 adb로 값 순서만 확인했고 **에뮬레이터 실행은 아직**(이상준이 `demo-charge.sh`는 에뮬레이터에서 확인함)
  - 기타: 문서(`PRD.md`, `README.md`, `docs/`)는 PR 없이 `main`에 바로 push(10-30 결정)
- 리뷰·머지: PR 리뷰는 Claude(정기 작업 "삼각고래 PR 리뷰", 매일 09~23시 매시), 머지는 장호연만(`docs/pr-review-guide.md`). 11-02 PR #14·차슐랭 #3·#4에 코멘트가 달려 정상 동작 확인
- 다음 할 일(우선순위 순, 11-07 점검 기준 — 코드·APK는 끝났고 남은 건 제출 준비. 서명 11-13, 발송 11-14):
  - **11-07 제출 APK 리허설에서 나온 것(시연 전 결정·수정)**: ① **두 앱 모두 런처 아이콘이 없음**(매니페스트에 `android:icon` 없음 → 독에 똑같은 기본 로봇 2개, 심사위원이 구별 못 함. 코드 PR 필요, 아이콘 그림은 길민종) ② 리허설 기록이 공유 서버에 쌓임(차슐랭 시흥동 인증 41·7일 실패 6, 최근 실패 시각이 리허설 시각) → 촬영 직전 `refresh-demo-dates.sql` 등으로 시연 데이터 정리 여부 결정 ③ 그 밖의 항목은 작업 기록 11-07 "제출 APK 리허설"
  1. 주최 측 문의 메일 발송(`docs/organizer-inquiry-email.md`, 11-07 위치기반서비스 신고 문항 12번 추가해 둠) — **장호연이 직접 발송**(발송 기록 없음). 보내기 전 사이트 FAQ·공지에 답이 이미 있는 항목은 지우기. 심사용 GitHub 계정명(4번) 답이 와야 2번 가능
  2. 저장소 접근: 두 저장소가 아직 **PRIVATE**(11-07 확인) → 심사용 계정 초대 또는 공개 전환 + 접근 확인. 새로 clone 빌드는 11-07 확인(아래 작업 기록)
  3. 시연 영상 촬영(`docs/demo-scenario.md` 순서). 촬영 전에 #37 실제 서버 시드 이름(아직 "시연 지점 (고정 위치 앞 약 400m)", 앱 Fake는 "판교IC 진입로")을 황준용에게 맞춰 달라고 할 것
  4. 기획서 7번 칸 【제출 전 기입】 2곳(GitHub 주소, 영상, `docs/proposal.md`에 2곳 남음) 채우고 DOCX 재생성(11-07 고정 위치 시연 문구는 반영·재생성 완료). 재생성 앱명 인자는 `"크루즈브레이커 (Cruise Breaker)"`
  5. 01 참가신청서(연락처·참가 단위·부문 체크·작성일)·04 서명 칸 → 5명 서명(11-13) → 11-14 메일 2통 발송(전원 CC)
  6. 서명 키 `C:/Users/H/.samgakgorae/samgakgorae-release.jks` **백업**(백업 기록 없음). APK는 11-05 커밋(`1ef69ee`, 마지막 코드 `d9e5e2e` 뒤라 최신). 다시 만들 때 `bash tools/build-release.sh`
  7. 약관(B01): 황준용 답(Supabase 리전·요청 로그)이 오면 `docs/terms-draft.md`와 `ui/TermsText.kt`의 2곳 고치기(판 번호를 바꾸면 AppContainer `termsVersion`도)
  8. 알릴 것: 황준용에게 core v0.5.2 변경(#42/차슐랭 #30) — 확인 코멘트 아직 없음
  9. 정리(급하지 않음): 로컬 머지 브랜치는 11-07 삭제함. 원격 머지 브랜치도 11-07 삭제함(장호연 지시) — 끝남
  - 11-07에 끝난 것으로 확인: 두 저장소 `local.properties`에 Supabase 키 있음(이민하 11-04 복사). Supabase 원격 migration은 이민하가 11-04 실제 서버로 시연 흐름을 확인해 적용된 것으로 보이나 `verify.sql`로 직접 확인하지는 않음
- 막힌 점·질문:
  - GitHub 아이디 확인(11-02 PR로): 이상준 `wklee316`, 황준용 `Aodlf123` → `docs/handoff/README.md`에도 적을 것
  - Organization은 만들지 않음(10-30). 심사용 계정 권한·저장소 주소의 계정명 노출은 주최 측 답변 대기(팀 문서 4절 1번, 9절 4번)
- 주의:
  - gh는 브라우저 로그인이라 계정 전체 권한이다. 두 저장소로 줄이려면 fine-grained 토큰(Contents 읽기, Pull requests 읽기·쓰기)으로 다시 로그인
  - 결정 사항: 부문 **일반**, 팀명 **삼각고래**, APK는 `apk/SamgakGorae_CruiseBreaker_release.apk`(10-30)
  - 판정 규칙의 "28일"은 집계 기간(최근 28일만 셈)이다. 서버 보관 기간·삭제 정책은 아직 정하지 않음(이슈 #7과 함께 황준용과 정할 것)
  - Virtual Console은 하트비트가 없으면 쓰기가 503 → `tools/vconsole.sh up` 먼저
  - 장호연 개발 PC는 python이 `py -3`(3.14)으로 있음(DOCX 재생성 가능, 11-03 확인)
  - 장호연 개발 PC의 CLI 빌드는 JAVA_HOME이 없음 → `export JAVA_HOME="E:\AndroidStudio\jbr"`(JDK 21, Gradle 빌드·테스트 통과 확인). 호공이(OpenClaw) PC에는 JDK·SDK·python이 없어 빌드·DOCX 재생성 불가
- 확인 방법: `./gradlew assembleDebug test` / 시연 전체는 `docs/demo-scenario.md` 순서대로 / 신호 모니터는 S2 제목 길게 누르기 → 개발 메뉴 / 기록 흐름은 `docs/handoff/lee-minha.md` "확인 방법"

## 작업 기록 (최신이 위, 지우지 않음)

### 2026-11-07 — 제출 APK 리허설(에뮬레이터 + 실제 서버, 시나리오 화면 단위 점검)
- 장호연 지시("실제 시연이라 생각하고 화면 단위로 검토"). Claude가 Pleos 에뮬레이터(36.4.10)에 **커밋된 release APK 2개**를 설치해 두 `docs/demo-scenario.md`를 1단계부터 끝까지 진행. 스크린샷: 팀 폴더 `screenshots-2026-11-07/`(ch-*, cb-*)
- 주의: 에뮬레이터에 debug 빌드가 깔려 있으면 release는 서명이 달라 `INSTALL_FAILED_UPDATE_INCOMPATIBLE` → debug를 지우고 설치해야 함
- **결과: 두 시나리오 모든 단계가 문서대로 동작**(크래시·막힘 없음, 실제 서버 읽기·쓰기 정상)
  - 차슐랭: S7 동의 → S1 10곳(약 8초) → S2 → 길안내 팝업 → 내비 → 복귀 → S3(SoC 40→80) → S4(40%→80%·35분·53 kW) → 평가 → S2 점수 3.2→3.3·인증 40→41 → 실패 S3(측정 중) → S5 → S2 7일 실패 5→6 → S6 인증·실패 각 1건
  - 크루즈: S5(동의·알림·차속 권한 창 "위치 정보") → S2(40%·500) → 내비 앞 + 스크립트 → 앱이 내비 위로 올라와 큰 배너 + 음성 → 10초 뒤 내비 복귀 → 90 km/h 해제 기록, 40 km/h 해제 기록 안 함 → 정차 시 앱이 올라와 S3(1회·1곳·1건) → S2. 앱을 앞에 둔 3'도 정상
- **발견한 것(심각한 순)**
  1. 두 앱 런처 아이콘 없음(`aapt`: `icon=''`) — 독에 똑같은 기본 로봇 아이콘 2개
  2. ~~크루즈: 내비가 앞일 때 앱 화면이 음성보다 3~5초 늦게 올라옴~~ → **정정(같은 날)**: 스크린샷 간격(2.5초)과 시각을 잘못 맞춘 것. 시스템 로그로 재측정하니 안내 20:43:57.925 → Pleos 홈 `showTaskView` 57.962 → `onStart` 58.064 → 그리기 준비 58.454, 내비 복귀 10초 뒤. 팝업(앱 올리기)은 설계대로 동작, 문제 아님
  3. 차슐랭 S4: 별을 누르면 "5점 선택" 줄이 생겨 태그 버튼이 아래로 밀림 → 별 직후 태그를 누르면 빗나감(리허설에서 실제로 빗나감). 라벨 자리를 미리 비워 두면 해결(길민종 UI)
  4. 공유 서버에 리허설 기록 누적(위 "다음 할 일" ②)
  5. 차슐랭 S1: 10곳 점수 대부분 3.4·인증 26회로 같아 "점수순"이 안 보임(PRD 5절 "시드가 비슷함"과 같은 문제). 이름도 "(시연)"·"시연 충전소 08"·"가상 충전소"가 섞임
  6. 차슐랭 길안내: 목적지가 2 m(1분)라 장면이 약하고 지도 배경이 흰색 — 영상에서는 0.9 km 충전소로 길안내를 보여 주는 편이 나음(시나리오가 허용)
  7. 크루즈 S2: 구간 이름이 서버 시드 "시연 지점 (고정 위치 앞 약 400m)"(#37). 처음 몇 초는 거리 칸이 "-"(위치 받기 전). 해제 1건을 올려도 해제율 40% 그대로라 "해제 수 +1"이 화면에 안 보임
  8. 차슐랭 최근 실패 시각이 정각(20:34 → 20:00) — 서버 `recent_failure_hour_ms` 설계대로, 시나리오 문구만 고침
- 고친 것(문서): 두 `docs/demo-scenario.md`에 3·8번 메모(차슐랭 8·10단계, 크루즈 기대 시간). 코드는 손대지 않음

### 2026-11-07 — 머지 끝난 원격 브랜치 삭제
- 장호연 지시("네, 삭제해 줘"). Claude가 진행
- `origin/main`에 모두 들어간 것(`git merge-base --is-ancestor`)만 골라 원격 브랜치 35개 삭제. 열린 PR 없음. 이제 원격은 `main`만 남음. 지운 브랜치는 GitHub PR 화면의 "Restore branch"로 되살릴 수 있음

### 2026-11-07 — 장호연 지시("할 수 있는 작업 전부")로 제출 준비 정리(두 저장소, 문서만 main 직접 push)
- **새로 clone 빌드 확인**: 두 저장소를 빈 폴더에 새로 clone(`local.properties` 없이 `ANDROID_HOME`만) → `./gradlew assembleDebug assembleRelease test` 성공. 테스트 실패 0 — 차슐랭 app debug·release 각 99, core 32 / 크루즈 app debug·release 각 83, core 32. 키가 없으면 Fake 저장소로 빌드됨(README "소스에서 빌드" 설명과 같음). `tools/sync-core.sh --check` 일치(core 0.5.2)
- 주최 측 문의 메일에 12번(위치기반서비스사업 신고 필요 여부) 추가, 팀 문서 9절 9번 추가. 두 저장소 `docs/` 사본 + 팀 폴더 원본 `organizer-inquiry-email.md` 맞춤. **발송은 장호연이 직접**
- 팀 문서 2절 담당 경로에 앱 연결 파일(`AppContainer`·`*App`·`RecordingService`·매니페스트 → 차슐랭 이상준 / 크루즈 이민하) 줄 추가, 10절 체크리스트에 APK·core 일치 체크
- `docs/handoff/README.md` 담당자 표에 GitHub 아이디 칸 추가(hoyeoni1·Aodlf123·wklee316·minalee22·giljong)
- 머지 끝난 로컬 브랜치 삭제(`git branch -d`, 머지 안 된 건 지우지 않음). 원격 브랜치는 그대로
- **PRD 정리**: 11-05 중간 점검 경고를 통과로, 위치 재생 시연 문구(5.1 표·5.6 표·7절 1~2번·PT에서 밝힐 것)를 고정 위치 시연으로, 제출 빌드 데이터 모드 이유(환경부 API → 실제 주행 기록 없음) 고침. 5.4에 앱이 뒤에 있을 때 안내(PR #48·#49) 동작 추가
- **기획서 수정·DOCX 재생성**: `docs/proposal.md`의 일정 2줄·시연 데이터 안내 1줄에서 경로 재생을 고정 위치로 고치고 DOCX 재생성(앱명 인자 `"크루즈브레이커 (Cruise Breaker)"`). 옛 DOCX와 본문을 비교해 그 3곳만 바뀐 것 확인

### 2026-11-07 — 최신 상태 점검, "다음 할 일" 갱신(두 저장소, main 직접 push)
- 장호연 요청("최신 상태 체크 후 남은 작업 보고" → "갱신해 줘"). Claude가 진행, 문서만 바꿈
- 확인: 열린 PR 없음, 열린 이슈 #37만, 저장소 PRIVATE, `local.properties` Supabase 키 있음, 서명 키 있음, release APK가 마지막 코드 커밋 뒤라 최신, `docs/proposal.md` 【제출 전 기입】 2곳 남음
- "다음 할 일"을 제출 준비 위주로 다시 씀(끝난 Supabase 키·적용 항목 빼고 #37 시드 이름·키 백업·브랜치 정리 추가). 열린 PR·이슈 줄도 고침

### 2026-11-05 — 내비 위 큰 배너 PR #49 머지(→ d6d4bd4) + release APK 재커밋
- 장호연 지시("네, 머지해줘")로 Claude가 진행. 충돌 없음, merge commit
- 머지된 main에서 `bash tools/build-release.sh` → `apk/SamgakGorae_CruiseBreaker_release.apk` 다시 커밋(앱 화면 올리기·내비 복귀·정차 시 S3 자동 포함)
- **알릴 것(장호연 → 팀원)**: 이민하 — `AppContainer`(autoRaised·summaryPending·안내 구독), `MainActivity`(scheduleReturn·returnToMaps), `RiskNotification.bringToFront`. 길민종 — `CruiseBreakerNav`(`summaryPending` 의존성·S3 자동)
- 남은 열린 PR: 없음

### 2026-11-05 — 안내를 내비 위 큰 배너로(앱 화면 올리기 + 자동 복귀, 브랜치 `feat/announce-foreground-20261105`)
- 장호연 피드백("헤드업 팝업이 너무 안 보인다"). Claude가 작업
- 동작: 앱이 뒤에 있을 때 안내가 나가면 ① `RiskNotification.bringToFront()`로 주행 화면(S1 큰 주황 배너)을 내비 위로 올림 → 배너 10초 뒤 `MainActivity.scheduleReturn()`이 지도 앱(`Intent.CATEGORY_APP_MAPS`, Pleos 내비가 선언)을 띄워 돌아감 ② 1.5초 안에 안 올라오면 헤드업 알림(기존). 정차(D→P)하면 `AppContainer.summaryPending` + 다시 올려 S3 자동 표시(`CruiseNavigationDependencies.summaryPending/onSummaryShown`)
- 실측(Pleos 에뮬레이터): 뒤에 있는 앱의 `startActivity`는 ATM이 "Background activity launch blocked"(BAL_BLOCK)를 찍지만 결과 코드 3이고 Pleos 홈(`HOME_SCREEN TaskSlotLayout`)이 `showTaskView(우리 앱)·hideTask(내비)`로 보여 줌. 단 **작업이 살아 있을 때만**(액티비티를 `finish`하면 못 올라옴). `moveTaskToBack(true)`는 true를 돌려주지만 화면은 그대로. 인텐트 extra는 전달되지 않아 복귀 신호는 `AppContainer.autoRaised`(AtomicBoolean) + `onStart`로. 알림의 BigTextStyle·색은 Pleos 헤드업이 무시, 큰 아이콘만 반영
- 확인: 내비 앞 → `demo-drive.sh` → 안내 1초 뒤 앱 S1 배너가 슬라이드 인 → 10초 뒤 내비 복귀 → P → 앱이 올라오며 S3 1·1·1. 헤드업 중복 없음(hun=0). 크래시 0. `test` 통과
- 담당 경로: `AppContainer`·`MainActivity`(이민하), `CruiseBreakerNav`(길민종)를 장호연 결정으로 고침. 머지 뒤 알릴 것
- 정책 메모: 주행 중 화면은 앱 개발 규정 2.3(애니메이션 금지)을 지켜 배너는 정적. 화면 전환 슬라이드는 플랫폼(Pleos 홈)의 것. "게임 연출(슬라이드 인 애니메이션)"은 규정과 충돌해 넣지 않음 — 장호연 판단 필요
- 문서: `docs/demo-scenario.md` 3·6단계·복구표·빌드 메모, PRD 5.5 S1

### 2026-11-05 — 내비 위 헤드업 팝업 PR #48 머지(→ 398da90) + release APK 재커밋
- 장호연 지시("네, 머지해줘")로 Claude가 진행. main에 README 정리 커밋(d583efe·df01d54)이 먼저 들어와 `docs/handoff/jang-hoyeon.md` 작업 기록이 충돌 → 양쪽 항목 모두 유지하고 merge commit으로 머지
- 머지된 main에서 `bash tools/build-release.sh` → `apk/SamgakGorae_CruiseBreaker_release.apk` 다시 커밋(헤드업 팝업 포함)
- **알릴 것(장호연 → 이민하)**: `AppContainer.appVisible`·안내 구독 알림, `MainActivity` onStart/onStop 추가. 새 파일 `RiskNotification.kt`
- 남은 열린 PR: 없음

### 2026-11-05 — 내비 위 헤드업 팝업 안내(브랜치 `feat/risk-headsup-20261105`)
- 장호연 요청("해제 다발 구간 목록만이 아니라 길 가는데 내비 위에 팝업처럼 떴으면"). Claude가 작업
- 새 파일 `RiskNotification.kt`(앱 루트): 안내가 나갈 때 앱이 앞에 없으면 헤드업 알림(채널 "해제 다발 구간 안내" 중요도 HIGH, 분류 `navigation`, 소리 없음(TTS가 읽음), 10초 뒤 자동 삭제, 누르면 앱). `AppContainer`에 `appVisible`(MainActivity onStart/onStop)과 안내 구독 추가, 기록 중지 시 알림 제거. 이민하 경로(`AppContainer`·`MainActivity`)를 장호연 결정으로 고침 → 알릴 것
- 실측: Pleos `ai.umos.notification`의 `HeadUpNotificationListenerService`가 `navigation` 분류·HIGH 알림을 내비 위 팝업으로 그림(logcat `OverlayController: HEADS_UP_NOTIFICATION`). **`car_warning` 분류는 `SEND_CATEGORY_CAR_NOTIFICATIONS` 시스템 권한이 없어 SecurityException으로 앱이 죽음** → 쓰지 않음. `notify`는 runCatching으로 감쌈
- 확인(에뮬레이터, 실제 서버): 내비를 앞에 두고 `demo-drive.sh` → 안내 1초 뒤 내비 위에 "500m 앞 크루즈 해제 잦은 구간 · 크루즈브레이커 · 해제율 40%" 팝업 + 음성, 10초 뒤 사라짐, 상단 바 종 아이콘에 남음. 앱이 앞이면 팝업 없이 S1 배너. 크래시 0
- 문서: `docs/demo-scenario.md` 3단계를 "내비 앞에 두고" 팝업으로, 복구표 추가. PRD 5.5 S1 행
- 남은 것: PR 리뷰·머지(장호연) → release 재빌드·`apk/` 커밋. 차슐랭에는 해당 없음(정차 전용)

### 2026-11-05 — README 팀 내부 안내를 `docs/dev-guide.md`로 이동(두 저장소, main 직접 push)
- 배경: 심사위원이 GitHub 링크로 들어오면 README의 팀 내부 안내(담당별 첫 작업·규칙, 팀원 실명 포함)를 먼저 보게 됨 → 장호연 "옮겨 줘"
- 한 일(Claude): README의 `## 시작하기`부터 끝(`코드 구조`·`담당별 첫 작업`·`규칙`)까지를 **`docs/dev-guide.md`**로 그대로 옮기고(링크는 `docs/` 기준으로 고침), README에는 `## 팀 개발 안내` 한 줄 링크만 남김. `docs/README.md` 목차 맨 위에 항목 추가. 차슐랭 README도 같음
- 확인: README·PRD·docs에서 README 절을 가리키는 링크 없음(grep). 문서만 바꿈. README에 남은 실명 없음(`docs/handoff/` 파일명·내용의 실명은 그대로 — 접수양식 실명 금지는 양식에만 적용)
- 바꾼 파일: `README.md`, `docs/dev-guide.md`(신설), `docs/README.md`, 이 문서

### 2026-11-05 — README 심사용 안내 추가(두 저장소, main 직접 push)
- 배경·구성은 차슐랭 `docs/handoff/jang-hoyeon.md` 같은 날짜 항목과 같음(GitHub 가산점 필수 4항목 + 오픈소스 고지). 이 저장소 README는 `## 시작하기` 앞에 "심사용 안내" 절 — 항목 표, 실행 방법(`adb install`, `pm grant --user 10 … CAR_SPEED`, S5 동의 → S2, `vconsole.sh up` → `demo-drive.sh` 장면 순서, `logcat -s CruiseRecord`, 시연 고지), 테스트 환경·필요 설정(권한 8개, DO 선언·S1 규정, 서버 설정 내장, 소스 빌드·Fake 폴백, 개발 메뉴는 debug만), 오픈소스 고지(TextToSpeech 3.3.1·core-ktx 포함)
- 확인: 코드 기준(`CruiseBreakerNav`의 `BuildConfig.DEBUG`, 매니페스트, `build.gradle.kts`, 11-05 커밋된 `apk/`). 문서만 바꿈
- 남은 결정은 차슐랭 항목 ①~⑤와 같음
- 바꾼 파일: `README.md`, 이 문서

### 2026-11-05 — 시연 흐름 PR 2개 머지(차슐랭 #35 → 1047949, 크루즈 #47 → 0823124) + release APK 커밋
- 장호연 지시(AskUserQuestion 답 "머지하고 release 커밋")로 Claude가 진행. 두 PR 모두 merge commit, 충돌 없음
- 머지된 main에서 `bash tools/build-release.sh` → `apk/SamgakGorae_<App>_release.apk` 서명 빌드·커밋(시연 데이터 모드 + 시연 판정 기준 + 실제 흐름 시작). 머지 전 브랜치 빌드로 에뮬레이터 시연 확인함(위 항목)
- **알릴 것(장호연 → 팀원)**: 길민종 — `ui/` 변경(`ChachelinNav`·`StationListScreen`·`CruiseBreakerNav`·`InitialScreen`·`ScreenPolicy`·`RiskListScreen`·공통 `CarComponents.onTitleLongPress`), 개발 메뉴는 목록 제목 길게 누르기. 이상준 — `AppContainer.chargeRules`(`DEMO_RULES`)·`StationListViewModel` 위치 타임아웃, 첫 충전 S4 20초 지연 1회 관찰(재현 안 됨) 확인 요청. 황준용 — core 0.5.2 `VehicleSdkSource.subscribeEv` SoC 재조회 한 줄(두 저장소 동일)
- 남은 열린 PR: 없음

### 2026-11-05 — 시연 흐름 확정·실제 흐름 시작 화면(브랜치 `feat/demo-flow-20261105`, 두 저장소)
- 장호연 목표("실제 앱이 시연 가능한 상태로 끊김 없이 동작, 사용자 시나리오를 픽스하고 그대로 쓰면 동작")로 Claude가 작업. 장호연 결정: 시작 화면은 debug·release 모두 실제 흐름(S5→S2), 개발 메뉴는 숨은 진입, 경로 재생 없이 고정 위치 시연
- 앱: `initialScreen`이 debug에서도 동의 전 S5·동의 후 S2(`InitialScreenTest` 수정). `resetTo`가 개발 메뉴를 밑에 깔지 않음. 개발 메뉴는 S2 제목 길게 누르기(`ScreenHeader.onTitleLongPress`, 차슐랭과 동일). **플랫폼 UX 제한 null 5초 유예**(`ScreenPolicy.resolveUxRestricted`, 11-04 검토 #5): 하트비트 없이 앱을 켜도 5초 뒤 S2가 열림(확인함). 주행 여부는 기어·차속으로 계속 막음(테스트 추가). 화면 스택 `rememberSaveable`
- core 0.5.2(차슐랭 원본 동기화): `VehicleSdkSource.subscribeEv` SoC 재조회(차슐랭 시작 SoC 문제용, 크루즈 동작 영향 없음)
- 문서: **`docs/demo-scenario.md` 신설(확정 시연 순서·기대 시간·복구표)**, PRD 7절, README, `tools/demo-drive.sh` 머리 주석
- 확인(에뮬레이터, 실제 서버 시연 데이터 모드, 앱 데이터 지우고 처음부터): S5 동의 → S2 시드 1곳(40%·500) → `demo-drive.sh` → D·100·크루즈 작동 → S1 자동 + "500m 앞 크루즈 해제 잦은 구간" 배너 + TTS → 90km/h 해제 기록(`CruiseRecord: disengage speed=90.0`, 업로드) → 40km/h 해제 기록 없음 → P → S3 1·1·1 → [목록으로] → S2. 제목 길게 → 개발 메뉴 → [뒤로] → S2. 하트비트 끄고 재실행 → 5초 뒤 S2. `assembleDebug test` 통과(app 83, core 32/18)
- 담당 경로 참고: `ui/`(길민종) `CruiseBreakerNav`·`InitialScreen`·`ScreenPolicy`·`RiskListScreen`·`CarComponents`, `core/`(황준용)를 장호연 결정으로 고침. PR 머지 뒤 두 사람에게 알릴 것. 이민하 파일은 손대지 않음
- 추가 확인(11-05 오후): `tools/build-release.sh`로 서명 release APK를 만들어 에뮬레이터에 설치(기존 debug는 `adb uninstall` 뒤) → S5→S2→S1 배너→S3→S2 같은 흐름, 크래시 0. `apk/`는 머지 뒤 main에서 다시 빌드해 커밋할 것(지금은 로컬에만 있음)
- 남은 것: PR 머지(장호연) → `bash tools/build-release.sh` → `apk/` 커밋. 서버 시드 이름 "시연 지점 (고정 위치 앞 약 400m)" → "판교IC 진입로" migration(황준용 #37). 아이콘 2종(11-04 검토 #8)

### 2026-11-05 — 길민종 Pleos 검수 후속 PR 2개 머지(크루즈 #46 → 3105a8d, 차슐랭 #34 → bdfaa3b)
- 장호연 지시로 Claude가 진행. 두 PR 모두 base가 main이라 그대로 merge commit으로 머지(충돌 없음)
- 머지 전 리뷰: 00:13 자동 리뷰 `✅` + 09:40 재검토 `✅`. PR 브랜치 `assembleDebug test --rerun-tasks` 통과(크루즈 app 82·core 32/18, 차슐랭 app 99·core 32/18, 실패 0). 두 PR의 `CarAlertDialog` `widthIn(min = 64.dp)`가 같은 내용이라 함께 머지해 두 앱 `CarComponents.kt` 동일 유지
- 내용: 크루즈 S2 선택 구간·스크롤 보존(`SaveableStateProvider`, 철회 시 `removeState`), debug 정차 S1 미리보기 헤더(`canShowDrivingPreviewBack` — release·기어 D/R/N·차속≠0·UX 미확인이면 꺼짐, `ScreenPolicyTest` 추가), 설정 "자동 기록" 설명 복원. 차슐랭은 목록 스크롤 보존·헤더 정렬
- 후속(타 담당): 황준용 — #37 서버 시드 이름 migration(실제 서버 S2 구간 이름이 아직 옛 이름). 이상준(차슐랭) — 내 충전 기록 최초 로딩 빈 상태
- 남은 열린 PR: 없음

### 2026-11-04 — 길민종 대표색 PR 2개 머지(크루즈 #45 → 633996d, 차슐랭 #33 → 683a22f)
- 장호연 지시로 Claude가 진행. 두 PR base를 `fix/car-header-back-20261104`(이미 머지됨)에서 `main`으로 바꾼 뒤(변경 파일 5개·7개 그대로) merge commit으로 머지
- 머지 전 정기 리뷰: PR 브랜치 `assembleDebug test` 통과(크루즈 204개·차슐랭 242개, 실패 0), 필수 항목 통과, `✅ 머지 가능` 코멘트. 머지된 main에서 두 저장소 core 동일, 공통 UI 파일은 Primary 값·주석만 다름
- 후속(선택, 길민종): S1 주행 화면의 "크루즈 작동"·"안내할 구간 없음"·"차량 연결됨"이 Warning 주황(#FFB547)과 거의 같은 앰버(#FFB020) → 흰색/회색으로 바꾸거나 `Theme.kt` Warning을 더 붉게(두 앱 같이). 설정 토글·"동의 철회" 구분도 같은 원인. 차슐랭 쪽은 S1 카드 "7일 실패" 0회 표기(PRD)와 S4 인증 카드 레드
- 남은 열린 PR: 없음

### 2026-11-04 — 대표색 PR 수정 요청(차슐랭 #33, 크루즈 #45)
- 장호연 결정: 길민종이 색 의미 문제를 이 PR에서 고친 뒤 머지. PR에 @giljong 요청 코멘트: 차슐랭 S4 인증 카드·S5 [닫기]를 레드 말고 흰색·회색, 크루즈 설정 "동의 철회"를 토글 앰버와 구분, S1 "차량 연결됨" 점 회색
- base 변경(`fix/car-header-back-20261104` → main)은 에이전트 권한 확인에서 막혀 못 함 → 길민종에게 요청. **머지 전에 base가 main인지 꼭 확인**
- 다음: 길민종 수정·재리뷰 요청 오면 재검토

### 2026-11-04 — 길민종 대표색 PR 검토(차슐랭 #33 레드, 크루즈 #45 앰버)
- 한 일: main에 합쳐 `assembleDebug assembleRelease test lint` 통과(248개·212개, 실패 0). 서버 연결 debug로 두 앱 주요 화면 캡처. 코드는 머지 가능, `🟡` 코멘트
- **머지 전 필수**: 두 PR base가 아직 `fix/car-header-back-20261104`(이미 머지됨) → base를 main으로 바꾼 뒤 머지(충돌 없음)
- 색 의미 확인 요청: 차슐랭 S4 "인증 충전 완료" 카드가 레드라 오류처럼 보임, S5 실패 팝업 [닫기]가 레드. 크루즈 설정 "동의 철회"가 켜진 토글과 같은 앰버라 구분 안 됨, S1 "차량 연결됨" 점 앰버. 이 PR에 넣을지 후속으로 할지는 장호연 판단
- 참고: 서버 연결 빌드의 크루즈 S2 구간 이름은 아직 옛 "시연 지점…"(서버 이름 migration #37 대기)

### 2026-11-04 — 길민종 PR 4개 머지(줄바꿈 차슐랭 #31 → d55423d·크루즈 #43 → ceaf56e, 헤더 뒤로 차슐랭 #32 → 0cb7d7b·크루즈 #44 → 554e9ee)
- 장호연 지시로 줄바꿈 PR 먼저 머지. 헤더 뒤로 브랜치에 main을 합쳐 `gil-minjong.md` 충돌 정리(현재 상태는 뒤로 PR 쪽, 작업 기록 양쪽 유지, dd6519e·f016f78) 후 머지
- 머지된 main: `assembleDebug assembleRelease test lint` 통과(248개·212개, 실패 0), core·tools 동일, 충돌 표시 없음
- 남은 열린 PR: 길민종 대표색(차슐랭 #33, 크루즈 #45) 검토 전

### 2026-11-04 — 길민종 PR 4개 검토(줄바꿈 차슐랭 #31·크루즈 #43, 헤더 뒤로 차슐랭 #32·크루즈 #44)
- 한 일: 두 PR을 함께 main에 합쳐 `assembleDebug assembleRelease test lint` 통과(248개·212개, 실패 0), core·tools 동일. 에뮬레이터: 두 앱 모든 정차 화면 헤더 [뒤로] → 이전 화면, 루트에서 [뒤로] → 앱 닫힘. 네 PR에 `✅ 머지 가능` 코멘트
- 머지 순서: 줄바꿈 PR 먼저 → 헤더 뒤로 PR은 `gil-minjong.md`만 충돌(현재 상태는 뒤로 PR 쪽, 작업 기록 양쪽 유지)
- 참고: 크루즈 S3 요약의 뒤로는 기존 설계대로 S2 목록. 에뮬레이터 주행 상태가 IDLE로 남으면 S1만 보임(기어 D→P)
- 다음: 장호연 머지 지시 대기

### 2026-11-04 — 시작 크래시 방어·안내 쿨다운 PR 머지(크루즈 #42 `9ff1318`, 차슐랭 #30 `0aa4032`)
- 한 일(Claude 에이전트, 장호연 "리뷰 후 머지" 지시): Claude 리뷰 `✅ 머지 가능`(d7821af)·이민하 동의 코멘트 확인. `docs/handoff/jang-hoyeon.md` "작업 기록"에서만 main과 충돌 → 두 항목 모두 남기는 쪽으로 origin/main을 브랜치에 합쳐 push. 합친 브랜치에서 `assembleDebug test` 통과(210개, 실패 0), 차슐랭 쪽과 `sync-core.sh --check` 일치(core 0.5.2). 차슐랭 #30을 먼저 머지한 뒤 merge commit
- 에뮬레이터: 이민하가 main+#42 합친 빌드로 `demo-drive.sh` 45초 간격 반복에서 두 번째도 안내가 나오는 것을 실제 서버로 확인함(lee-minha.md 11-04) → PR 본문의 "에뮬레이터 확인 아직"은 충족
- 리뷰의 선택 제안은 차슐랭 쪽 기록과 같음(안 고침)
- 확인: 머지 뒤 main(`9ff1318`) `assembleDebug test` 통과(210개, 실패 0), core·tools 차슐랭 main(`0aa4032`)과 `sync-core.sh --check` 일치
- 다음: `bash tools/build-release.sh` 후 `apk/` 커밋, 서버 연결 리허설. 검토 보고서 2장 #5(ScreenPolicy null 게이팅)·#8(아이콘·allowBackup)

### 2026-11-04 — core v0.5.2 시작 크래시 방어 PR 검토(차슐랭 #30, 크루즈 #42)
- 검토 중 22:35에 장호연 계정으로 두 PR이 머지됨(0aa4032, 9ff1318). 머지된 main의 코드는 Claude가 검토·시험한 것과 같음(문서만 다름)
- 확인: main에 합쳐 `assembleDebug assembleRelease test lint` 통과(246개·210개, 실패 0), core·tools 두 저장소 동일. 에뮬레이터: 구형 형식 키로 빌드 → 앱이 죽지 않고 `SupabaseConfig` 로그(키 값 없음), 정상 키로 되돌리면 정상. 크루즈 `demo-drive.sh` 40초 간격 두 번 → 두 번 다 안내 음성
- 참고 의견(PR 코멘트): 복호화 실패는 `read()`가 null을 돌려 `status`가 `local-storage-reset`이 아니라 `idle`로 시작(경고 로그로만 보임). 상태 파일 손상 시 동의 철회 삭제 대기도 사라질 수 있음 → 황준용·이상준 인지 필요
- 이 PC `local.properties`는 정상 키로 되돌려 둠(바꿨다 되돌린 것 확인)

### 2026-11-04 — 시작 크래시 방어(core v0.5.2) + 안내 쿨다운 수정, 브랜치 `fix/core-startup-resilience`
- 배경: 11-04 두 작품 전체 검토(`hyundai-app/project-review-2026-11-04.md`)에서 ① 공통 core "켜자마자 죽는" 경로 2개, ② `RiskAnnouncer`의 3분 간격이 주행 사이에도 유지돼 **리허설 직후 본 시연에서 배너·음성이 안 나오는** 문제 확인 → 장호연 "수정 시작" 지시
- 한 일:
  - core v0.5.2는 차슐랭에서 고치고 `tools/sync-core.sh`로 복사(`--check` 일치). 내용은 차슐랭 `docs/handoff/jang-hoyeon.md` 같은 날짜 항목: `BackendConfig.fromBuildConfigOrNull`, `EncryptedBackendStore.read()`·`SupabaseBackend` 상태 로드가 예외 대신 빈 상태, 테스트 2개, `build-release.sh` 키 형식 검사
  - `app/.../AppContainer.kt`(이민하 담당, 장호연 승인): `BackendConfig(...)` 직접 생성 → 팩토리 + `Log.e("SupabaseConfig", …)`
  - `app/.../domain/RiskAnnouncer.kt`(이민하 담당, 장호연 승인): `tripNumber`가 바뀔 때 `announcedThisTrip`과 함께 `lastAnnounceMs`도 null로. 규칙 문서의 "안내 사이 최소 3분"은 **한 주행 안에서만** 센다는 뜻으로 해석(정차 P 뒤 새 주행이면 바로 안내 가능). `docs/disengage-rules.md` 70행 표 문구는 이민하가 맞출 것
  - 테스트 1개 추가(`RiskAnnouncerTest` "새 주행이 시작되면 3분이 안 지났어도 다시 안내한다": 안내 → 30초 뒤 P→D → 다시 안내)
- 결정한 것과 이유: 시연 직전 리허설을 돌려도 본 시연이 묵음이 되지 않게. 실사용에서도 주차 뒤 다시 출발하면 새 맥락이라 3분 억제가 필요 없음
- 테스트 결과: `./gradlew :core:testDebugUnitTest :app:testDebugUnitTest --offline` 통과 — debug 106개(core 32 = 30 + 새 2, app 74 = 73 + 새 1), 실패 0. 차슐랭 쪽 PR: https://github.com/hoyeoni1/hyundaiapp-chachelin/pull/30. 에뮬레이터: `bash tools/demo-drive.sh`를 3분 안에 두 번 돌려 두 번째도 배너·음성이 나오는지(아직 안 함 — 에뮬레이터가 다른 세션 작업 중)
- 바꾼 파일: `core/`(동기화), `tools/build-release.sh`(동기화), `app/src/main/java/com/hyundaiapp/cruisebreaker/AppContainer.kt`, `app/src/main/java/com/hyundaiapp/cruisebreaker/domain/RiskAnnouncer.kt`, `app/src/test/.../RiskAnnouncerTest.kt`, 이 문서

### 2026-11-04 — 텍스트 후속(차슐랭 #28, 크루즈 #40)·Supabase 공개 설정(차슐랭 #29, 크루즈 #41) 머지
- 머지: 크루즈 #40 → 32ad976, 차슐랭 #29 → a874fca, 크루즈 #41 → ff29863. 차슐랭 #28은 GitHub가 PR head를 옛 커밋(a0d13d5)으로 잡아 머지 API가 계속 거절해서, 검토한 브랜치 끝(97d55cb)을 로컬에서 merge commit(9ea62b4)으로 main에 push → PR MERGED 처리됨
- 황준용 #29/#41 검토: `supabase-public.properties`는 `sb_publishable_` 공개 키(Secret 아님). 이 PC 두 저장소 `local.properties`에 두 줄 추가(Git 제외 확인). debug 빌드로 차슐랭 S1 → `SupabaseSync: connected`, 서버 시연 데이터 표시. 대왕판교IC 휴게소 2.7점·7일 실패 5회로 맨 아래 → **11-03 migration 2개 실제 서버 적용 확인**
- 최종 main(키 포함)에서 `assembleDebug assembleRelease test lint` 통과(242개·204개, 실패 0), `tools/` 동일
- 남은 것: 키가 들어왔으니 `bash tools/build-release.sh`(두 저장소) 후 `apk/` 커밋, 서버 연결 상태 전체 리허설(크루즈 포함). 황준용: 시드 구간 이름 새 migration(크루즈 #37), 본인 handoff, Supabase 리전·요청 로그 답(약관 【확인 필요】 2곳). 앱 막 켠 직후 충전 시연 시작 SoC 문제(이상준 확인 필요)

### 2026-11-04 — 길민종 텍스트·이모지 후속 PR 검토(차슐랭 #28, 크루즈 #40)
- 한 일: 두 PR을 main에 합쳐 `assembleDebug assembleRelease test lint` 통과(242개·204개, 실패 0), 충돌 없음. 에뮬레이터에서 S3 세로 가운데 배치·S4 전환, 약관 전문 줄바꿈 확인. 두 PR에 `✅ 머지 가능` 코멘트
- 참고 의견(막지 않음): 이제 한글끼리만 묶어 `ID만`·`32분`처럼 숫자·영문+한글 단어가 줄 끝에서 갈릴 수 있음 → 한글과 글자·숫자 사이도 묶도록 넓히기 제안
- **따로 발견(이 PR과 무관)**: 앱을 막 설치·실행한 직후 `demo-charge.sh success`를 돌리면 충전 시작 SoC를 이전 값(80%)으로 잡아 변화 0 → 충전 끝나도 S4/S5가 안 뜨고 개발 메뉴로 돌아감. 앱이 떠 있는 상태로 다시 돌리면 40→80 정상. 시연 영상 찍을 때는 앱을 켜고 몇 초 뒤에 스크립트 실행. 원인(기록 서비스 시작 시 SoC 초기값)은 이상준 담당 쪽에서 확인 필요
- 다음: 장호연 머지 지시 대기

### 2026-11-04 — demo-drive.sh 시드 이름 PR 머지(차슐랭 #27 → 68460a1, 크루즈 #39 → 8f02c88)
- 장호연 지시로 머지. GitHub 502·"merge already in progress"로 한 번 걸려 REST로 다시 머지함. 두 저장소 `tools/` 동일
- 확인(11-04): Supabase 키는 이 PC `local.properties`에 아직 없음(release 서명 값만). 황준용 새 서버 코드·커밋 없음(마지막 활동 11-03). 열린 PR 없음

### 2026-11-04 — demo-drive.sh 시드 이름 맞춤(브랜치 `fix/demo-drive-seed-name`)
- `tools/demo-drive.sh` 14행 설명의 시드 이름을 "시연 지점" → "판교IC 진입로"로(크루즈 #38과 맞춤). 주석 한 줄만, 동작 변경 없음. 두 저장소 `tools/` 동일 확인, `bash -n` 통과
- 남은 것: 서버 시드 이름 새 migration(황준용) 뒤 크루즈 #37 닫기

### 2026-11-04 — 길민종 화면 다듬기 PR 머지(차슐랭 #26 → 85083e7, 크루즈 #38 → 3112f02)
- 장호연 지시로 merge commit. 머지 결과는 PR 브랜치와 문서만 달라 검토 때 빌드·테스트(230개·192개)·에뮬레이터 확인이 그대로 유효
- 차슐랭 #25 자동으로 닫힘. 크루즈 #37은 열어 둠: 황준용 서버 시드 이름 새 migration, 장호연 `tools/demo-drive.sh` 14행 이름 맞추기(두 저장소)
- 열린 PR 없음

### 2026-11-04 — 길민종 화면 다듬기 PR 검토(차슐랭 #26, 크루즈 #38)
- 한 일: 두 PR을 지금 main에 합쳐 `assembleDebug assembleRelease test lint` 통과(테스트 230개·192개, 실패 0), 충돌 없음, `tools/` 동일. 에뮬레이터에서 `demo-charge.sh success`로 S3·S4 확인: S3 카드 안 빈칸 사라짐, S4 별이 열을 채우고 선택 시 주황 별·"N점 선택"·[평가 보내기] 켜짐. 두 PR에 `✅ 머지 가능` 코멘트
- 참고 의견(막지 않음): S3는 카드 아래 화면 절반이 비어 보임 → 영상에서 어색하면 가운데 정렬·SoC 숫자 키우기 검토
- 다음: 장호연 머지 지시 후 머지. #38 머지 뒤 `tools/demo-drive.sh` 14행 이름을 "판교IC 진입로"로(차슐랭 원본 → 크루즈 동기화). 서버 시드 이름은 황준용 새 migration(#37은 열어 둠)

### 2026-11-04 — 길민종 UI PR 4개 머지
- 장호연 "나머지 진행" 지시. Claude 에이전트가 리뷰한 head 고정으로 merge: 팝업 구분선 차슐랭 #22(`efb0ef8`)·크루즈 #34(`7fc8971`) 먼저 → 글자 단계 통일 #23(`a5d9144`)·#35(`7d5fbf3`)
- #23·#35는 `docs/handoff/gil-minjong.md`만 충돌 → `feat/ui-polish`에 main을 merge해 정리(현재 상태는 ui-polish 쪽, 작업 기록은 양쪽 모두 유지, `2fc2940`·`b0d3bad`) 뒤 머지
- 확인: 머지된 main 두 앱 `assembleDebug assembleRelease test lint` 통과(테스트 230, 192), `tools/` 같음, 아래 바 수정 유지(S4 버튼 y 1230). 에뮬레이터에 main debug 설치함
- 열린 PR 없음. 남은 화면 개선은 길민종 이슈 차슐랭 #25·크루즈 #37
- 바꾼 파일: 이 문서

### 2026-11-04 — 화면 검토, 개선 요청 이슈(차슐랭 #25, 크루즈 #37)
- 장호연 지시("화면 개선할 거 검토", "길민종에게 1~4 요청"). Claude 에이전트가 길민종 UI PR 4개 적용본을 에뮬레이터에 설치해 전 화면 확인: 터치 64dp 이상·글자 잘림 없음
- 요청(담당 길민종, assign): 차슐랭 #25 = S3 충전 중 카드 늘어남·빈칸, S4 별점 크게. 크루즈 #37 = S3 0·0·0 요약 상태 문구, 시드 구간 이름 "시연 지점(…)" 바꾸기(서버 새 migration은 황준용과)
- 시연 메모: 크루즈는 P를 받아야 정차 화면이 열림 → 시연 중 `tools/vconsole.sh up` 유지. 꺼진 뒤 다시 열면 P여도 주행 화면에 머묾(기어를 다시 P로 보내면 풀림)
- 에뮬레이터에는 지금 UI PR 적용본 debug가 설치돼 있음(저장소는 main)
- 바꾼 파일: 이 문서

### 2026-11-04 — 아래 바 수정 머지(차슐랭 #24, 크루즈 #36), 실제 시연 흐름 재확인
- 장호연 "Yes, merge both" 지시. Claude 에이전트가 head 고정 merge: 차슐랭 `742caa3`, 크루즈 `6e527f7`. 머지된 main 두 앱 `assembleDebug assembleRelease test lint` 통과
- 에뮬레이터(Fake)에서 머지된 main debug로 처음부터: S7·S5 동의 → [시작하기](이제 눌림) → 차슐랭 `demo-charge.sh success` → S4 별 5개·태그 → [평가 보내기] 성공 → `fail` → S5 실패 화면, 크루즈 `demo-drive.sh` → 안내 배너 → 주행 요약 1회·1곳. 스크린샷은 로컬 `screenshots-2026-11-04/`
- 알게 된 것: (1) 에뮬레이터를 새로 켜면 앱 동의 상태가 없을 수 있음 → 시연 전 개발 메뉴 S7/S5에서 동의·[시작하기] 먼저(안 하면 기록 서비스가 안 돌아 충전이 기록되지 않음). (2) `tools/vconsole.sh up`을 파이프(`| tail`)로 부르면 하트비트 때문에 끝나지 않음. (3) Git Bash에서 `MSYS_NO_PATHCONV=1`을 켠 채 시연 스크립트를 돌리면 curl `-o /dev/null`이 실패(exit 23)
- 남은 것: 길민종 UI PR 차슐랭 #22·#23, 크루즈 #34·#35 머지 결정(#22·#34 먼저)
- 바꾼 파일: 이 문서

### 2026-11-04 — 길민종 UI PR 4개 검토, 아래 시스템 바 버그 수정 PR
- 장호연 지시("지금은 어때"). Claude 에이전트가 작업
- 길민종 PR 차슐랭 #22(팝업 구분선)·#23(글자 단계 통일), 크루즈 #34·#35: 둘씩 main에 합쳐 빌드·테스트·lint 통과(230, 192), 에뮬레이터 화면 확인 → ✅ 머지 가능 코멘트. **머지 순서: #22·#34 먼저**, #23·#35는 `docs/handoff/gil-minjong.md`만 충돌하니 길민종이 main merge로 정리
- **발견(기존 버그, 시연 막힘)**: Pleos 아래 시스템 바(y 1300~1440)가 앱 창 아래를 덮어 맨 아래 고정 버튼이 안 보이고 안 눌림 — 차슐랭 S4 [평가 건너뛰기]·[완료], S7 [시작하기], S2 [길안내], 크루즈 S5 [시작하기], S3 버튼. inset은 오지 않고 `screenHeightDp`(1210dp)만 바를 뺀 값
- 수정 PR: 차슐랭 #24, 크루즈 #36(`fix/bottom-bar-inset`, `MainActivity` 루트 높이를 `screenHeightDp`로 제한). 버튼 y 1370 → 1230 확인, 빌드·테스트·lint 통과. **장호연 머지 결정 대기**
- 바꾼 파일: 이 문서(코드는 PR 브랜치)

### 2026-11-04 — 길민종 약관 검수 PR 머지, #14 닫음
- 장호연 "Yes, do all three" 지시. Claude 에이전트가 head 고정으로 merge commit: 차슐랭 #21(`675c167`), 크루즈 #33(`2dfe5e7`). 둘 다 `docs/handoff/gil-minjong.md`만 변경(약관 화면 검수 통과 기록)
- 차슐랭 #14 닫음(D01 유지 결정, D02 #19, B01 #19·#32, 화면 검수 통과)
- 남은 열린 이슈: 차슐랭 #12·#13(UI 감사 개선, 제출 필수 아님)뿐. 열린 PR 없음
- 바꾼 파일: 이 문서

### 2026-11-04 — 해결된 이슈 3개 닫음
- 장호연 지시("그거 닫아줘"). Claude 에이전트가 근거 코멘트와 함께 닫음: 차슐랭 #8(#10으로 해결), 크루즈 #20(#22로 해결), 크루즈 #25(이민하가 정한 닫는 조건 충족: #31 S5 안내, #32 약관 책임 제한)
- 남은 열린 이슈: 차슐랭 #14(길민종 화면 검수만 남음), #12·#13(UI 감사 개선, 제출 필수 아님)
- 바꾼 파일: 이 문서

### 2026-11-04 — 이상준 PR 차슐랭 #20 머지
- 장호연 "Yes, merge #20" 지시. Claude 에이전트가 리뷰한 head(`a999bfd`) 고정으로 merge commit `86d04f3`(충전소 조회 실패 재조회 + 결과 알림)
- 확인: 머지된 main에서 `assembleDebug assembleRelease test lint` 통과(테스트 184, 실패 0)
- 후속(이상준): 위치를 끝내 못 받는 세션의 재시도 끝 정하기, 서버 키 생기면 Wi-Fi 끈 충전 리허설
- 바꾼 파일: 이 문서

### 2026-11-04 — 최신 pull 뒤 재검토
- 장호연 지시("최신 pull 받아서 한번 더 검토"). Claude 에이전트가 작업
- 두 저장소 main은 머지 뒤 새 커밋 없음(차슐랭 `64a6327`, 크루즈 `82f2f09`). 새 것은 이상준 차슐랭 PR #20(충전소 조회 실패 재조회 + 결과 알림)뿐
- PR #20: 지금 main과 합쳐도 충돌 없음, `assembleDebug assembleRelease test lint` 통과(테스트 184, 실패 0) → ✅ 머지 가능 재확인 코멘트(https://github.com/hoyeoni1/hyundaiapp-chachelin/pull/20#issuecomment-6622771682). 후속: 위치를 끝내 못 받는 세션의 재시도 끝 정하기, 서버 키 생기면 오프라인 충전 리허설
- **남은 것(장호연)**: PR #20 머지 결정. 이슈 닫기(크루즈 #25·#20, 차슐랭 #14·#8)
- 바꾼 파일: 이 문서

### 2026-11-04 — PR 차슐랭 #19·크루즈 #32 머지
- 장호연 "Yes, merge both" 지시. Claude 에이전트가 리뷰한 head(`f0f8b2e`, `2a60341`) 고정으로 merge commit: 차슐랭 `f876a69`, 크루즈 `547fdc2`. 차슐랭 #17 자동으로 닫힘
- 확인: 머지된 main에서 두 앱 `assembleDebug assembleRelease test lint` 통과, `tools/` 두 저장소 같음
- 못 한 것: 크루즈 #25 닫기(에이전트 동작이 권한 확인에서 막힘) → 장호연이 닫기
- 바꾼 파일: 이 문서

### 2026-11-03 — 시연 가능 상태 마무리(PR 차슐랭 #19, 크루즈 #32)
- 장호연 지시("할 수 있는 거 다 해 줘, 두 앱이 구동·운영·시연 가능한 상태"). Claude 에이전트가 작업
- 한 일: 약관 전문 앱 연결(`ui/TermsText.kt`), 리뷰 태그 쌍 배타(#17, 차슐랭), 크루즈 S5 문구, `simulation_time_scale` 버그 수정(차슐랭), release 서명 설정 + `tools/build-release.sh`
- **서명 키 생성**: 저장소 밖 `C:/Users/H/.samgakgorae/samgakgorae-release.jks`(비밀번호는 같은 폴더 README.txt, 두 저장소 `local.properties`의 `release.*`에도 들어감). **이 폴더를 백업할 것**
- 확인: 두 앱 `assembleDebug test lint` 통과. 에뮬레이터(Fake)에서 `demo-charge.sh success/fail`, `demo-drive.sh` 전 장면 통과, 약관 창·태그 규칙 화면 확인, 서명된 release APK 설치·실행(크래시 0)
- 남은 것: PR #19·#32 머지 → 황준용에게 Supabase 키·migration 받기 → 서버 연결 리허설 → `bash tools/build-release.sh`로 제출 APK 만들어 커밋
- 바꾼 파일: 이 문서(코드는 PR)

### 2026-11-03 — 약관(B01) 값 확정
- 장호연 결정("메일만, 나머지 추천대로"): 시행일 11-14, 판 번호 유지, 서비스 종료 30일 안 삭제, 철회 표시는 서비스 종료 때 삭제, 보호책임자 장호연, 연락처 hoyhoyhoyeoni@gmail.com(전화번호는 공개되므로 안 넣음). 크루즈: 28일 지난 기록 따로 안 지움, 책임 제한에 고의·중과실 예외, S5 문구 "기록에는 좌표 대신 약 150m 구간 번호만 저장하고"로 변경(길민종)
- 한 일: 두 저장소 `docs/terms-draft.md`를 확정본으로(【확정 필요】 0개). 남은 【확인 필요】는 Supabase 리전·요청 로그(황준용). #14에 길민종 연결 요청
- 바꾼 파일: `docs/terms-draft.md`, 이 문서

### 2026-11-03 — Supabase PR #10·#22 머지, 최종 점검
- 한 일(Claude 에이전트와 함께, 장호연 "Yes, merge both", "전체 프로젝트 확인해서 최종보고"): #10(`fc9b2e7`)·#22(`eb06822`) 머지(GitHub 502 한 번 뒤 head 재확인 후 머지). 차슐랭 #9·크루즈 #12 자동 종료
- 확인: 두 main `assembleDebug assembleRelease test lint` 통과(차슐랭 208, 크루즈 192), core·tools 두 저장소 동일. 열린 PR 0건
- 확인: #8(철회 삭제 재시도)·#20(철회 뒤 늦은 업로드)은 머지된 core·서버로 해결(근거 테스트 통과). 닫기는 자동 승인에서 막혀 장호연이 닫을 것
- 발견: 두 저장소 PRIVATE, `apk/` 폴더·release 서명 설정 없음, 이 PC `local.properties`에 Supabase 키 없음 → "다음 할 일"에 반영
- 바꾼 파일: 이 문서

### 2026-11-03 — Supabase PR #10·#22 남은 수정 직접 반영(황준용 브랜치)
- 장호연 결정("다 진행해줘", 브랜치는 황준용 PR에 바로, DB 적용은 장호연이 대시보드에서). Claude 에이전트가 작업
- 반영: release 시연 데이터 모드 기본(`supabase.demo`), 점수 공식 migration `20261103000100`, 실패 잦은 시연 충전소 st-004 migration `20261103000200`, refresh SQL 갱신, `consent_version` = 약관 초안 판 번호, #10 README 충돌 해결(main merge)
- 확인: PGlite 17개, 두 앱 `assembleDebug assembleRelease test lint` 통과(208, 192). 전체 lint 통과 = #9는 #10 머지로 해결
- **남은 것(장호연)**: #10·#22 머지 → 대시보드에서 두 migration 적용(`server/deploy.md` "11-03 추가 migration", verify 기대 charge_sessions 325) → 시연 리허설
- 바꾼 파일(이 문서 외는 PR 브랜치): `app/build.gradle.kts`, `AppContainer.kt`, `core/.../BackendTransport.kt`·`SupabaseBackend.kt`·테스트, `server/` migration 2개·refresh·테스트·문서, `docs/server-overview.md`

### 2026-11-03 — 길민종 PR 머지(차슐랭 #18, 크루즈 #31)
- 한 일(Claude 에이전트와 함께, 장호연 "Yes, merge both" 지시): 차슐랭 #18(길민종 인수인계만, `8b697d1`), 크루즈 #31(S5 보조 안내, `abee7f2`) 머지. 크루즈 main `assembleDebug test :app:lintDebug` 통과
- 크루즈 #25: S5 안내는 반영됨. 책임 제한 문구는 약관 초안 4절에 있음 → 약관 확정 뒤 닫기
- 바꾼 파일: 이 문서

### 2026-11-03 — B01 약관 전문 초안, 최신 PR·이슈 점검
- 한 일(Claude 에이전트와 함께, 장호연 "초안 써 줘" 지시):
  - `docs/terms-draft.md` 두 앱 약관 초안(승인 전). 코드와 서버 PR #10 `d9fbcf6`·#22 `da25446`의 실제 처리에 맞춤: 설치마다 무작위 UUID의 해시만 서버로, 좌표 대신 충전소 ID·150m 격자 번호, 충전소 밖 충전은 기기에만, 철회 시 즉시 삭제, 철회 표시만 서버에 남음, TTS 기기 내. 크루즈는 #25 책임 제한 문구 포함
  - 확정 필요: 판 번호·시행일·보유 기간·연락처·보호책임자·책임 제한 표현. 확인 필요: Supabase 리전(국외 이전 고지), 요청 로그의 위치 보관, 위치기반서비스 신고 여부(주최 측 문의 권장)
  - 발견: 서버 `consent_version`이 `development-v1` 고정(황준용), 크루즈 S5 "정확한 좌표 대신 구간 번호를 보내고"는 근처 조회가 좌표를 보내는 것과 다름(길민종)
  - 점검: Supabase PR #10·#22는 06:20 이후 새 커밋 없음 — 시연 모드 release 설정·점수 공식·실패 잦은 시드 미반영, #10은 README 충돌. 길민종 새 PR 차슐랭 #18(인수인계만), 크루즈 #31(S5 보조 안내, 빌드·테스트 166개·app lint 통과) 리뷰
- 고침: 이 문서의 11-03 작업 기록들이 줄바꿈 대신 "역슬래시 n" 글자로 한 줄씩 들어가 있던 것을 복구(내용은 그대로)
- 바꾼 파일: `docs/terms-draft.md`, 이 문서

### 2026-11-03 — 동의 저장 방식 결정(차슐랭 이슈 #14 D01)
- 장호연 결정("Yes, both as proposed"): 동의는 각 [동의]에서 즉시 저장, 필수 두 개면 첫 실행 완료. [시작하기]는 별도 동의 단계 아님. 코드 변경 없음. PRD S5에 기록
- 남은 것(장호연): 약관 전문(B01) 작성 → 길민종에게 전달(두 앱 공통)
- 바꾼 파일: `PRD.md`, 이 문서

### 2026-11-03 — 길민종 PR #30·#27 머지
- 한 일(Claude 에이전트와 함께, 장호연 "Yes, merge all three" 지시): 리뷰 뒤 새 커밋(#30 `7e2d12c` 인수인계만, #27 `af2bc48` 띄어쓰기·통과 기록 설명) 확인 → #30 머지(`a171bf2`), #27 머지(`244c373`). 이슈 #29 닫힘. main 빌드·테스트 166개·app lint 통과, 앱 문자열에 "위험 구간" 없음
- PRD 9절 "검토 제안(미구현)"이 생김. 제출 전 구현 대상 아님
- 바꾼 파일: 이 문서

### 2026-11-03 — 길민종 PR #30·#27 리뷰
- 한 일(Claude 에이전트와 함께, 장호연 "continue"): #30(화면 문구 "해제 다발 구간", 이슈 #29) → ✅ 머지 가능, head `2727039` 빌드·테스트 166개·app lint 통과. #27 새 head `45b8de5` → ✅ 머지 가능(9절로 분리, `disengage-rules.md` 변경 빠져 이민하 승인 문제 해소)
- 바꾼 파일: 이 문서

### 2026-11-03 — 시연 스크립트 용어 PR #28 머지
- 한 일(Claude 에이전트와 함께, 장호연 "Yes, merge both" 지시): PR #28 머지(`2e0a4fe`, 차슐랭 #16과 같은 내용). 두 저장소 `tools/` 같음 확인. 앱 화면 문구는 이슈 #29(길민종) 대기
- 바꾼 파일: 이 문서

### 2026-11-03 — 용어 결정 반영(위험 구간→해제 다발 구간)
- 장호연 결정("Yes, as recommended", 길민종 PRD PR #27 검토 결과): 제품 문서 "위험 구간" → **"해제 다발 구간"**(사고 위험을 판정하지 않으므로). 코드·API 이름(`risk`/`segment_risk`)은 유지. 슬로건·한 줄 소개·기획 배경 유지
- 한 일: PRD 5.3 "용어" 항목, PRD·기획서·README 문구, 기획서 "차량 20대" → "차량-일 20" 바로잡음, DOCX 재생성(`c8f05f4`). 시연 스크립트 PR #28. 앱 화면 문구 7곳은 이슈 #29(길민종). PR #27에 결정 전달·rebase·이민하 승인 요청
- 남은 결정(장호연): 이슈 #14(차슐랭) D01·D02·B01, PR #27의 `disengage-rules.md` 변경 승인 방식
- 바꾼 파일: `PRD.md`, `README.md`, `docs/proposal.md`, `docs/submission/cruisebreaker-application.docx`, 이 문서

### 2026-11-03 — 길민종 UI PR #24 머지
- 한 일(Claude 에이전트와 함께, 장호연 "Yes, merge both" 지시): PR #24 머지(`5258afe`). main 빌드·테스트 166개·app lint 통과
- 남은 것: 길민종 PRD PR #27은 인수인계 충돌로 rebase 필요, 이민하 승인 필요. 장호연 결정 대기(용어·슬로건)
- 바꾼 파일: 이 문서

### 2026-11-03 — 최종 검토(길민종 PR 4건)
- 한 일(Claude 에이전트와 함께, 장호연 "최신 받아서 최종 검토 및 이슈 보고" 지시): 길민종 UI PR #24 새 head `ee6f702`(문서만) → ✅ 여전히 머지 가능(#27보다 먼저). PRD PR #27 → 💬 결정 필요 + 🔧(`docs/disengage-rules.md`는 이민하 승인 필요, 미구현 제안·'조회 실패 시 안내 보류'는 본문과 분리, 인수인계 충돌)
- 상태 확인: Supabase PR(차슐랭 #10 `d9fbcf6`, 크루즈 #22 `da25446`)은 07:17 재리뷰 그대로(🔧 남은 것: 제출 release 시연 모드 설정, 점수 공식). main 빌드·테스트 통과(차슐랭 176개, 크루즈 166개), `lint`는 core 3건(CAR_INFO·NAVI_ROUTE_SEARCH)으로 실패 — 차슐랭 #9, PR #10에서 해결됨
- 새 이슈는 만들지 않음(찾은 문제는 모두 열린 이슈에 있음: 차슐랭 #12·#13·#14, 크루즈 #25·#26)
- **장호연 결정 대기**: 길민종 PRD PR(차슐랭 #15, 크루즈 #27)의 슬로건·한 줄 소개 교체, "충전 실패"→"충전량 증가 미확인", "위험 구간"→"해제 다발 구간", "실측"→"계산 평균". 이슈 #14 D01(동의 저장)·D02(반대 태그)·B01(약관 전문)
- 바꾼 파일: 이 문서

### 2026-11-03 — 이민하 PR #23 머지
- 한 일(Claude 에이전트와 함께, 장호연 "Yes, merge #23" 지시): PR #23 머지(`a97f24e`) → 이슈 #19·#21 닫힘
- 남은 것: `docs/disengage-rules.md`(18b8323)는 PR #22의 서버 동작을 이미 된 일로 적어 둠 → #22 머지 전까지 내용이 앞서 있음. #22 머지 뒤 이민하 `PermanentFailureException`과 core 4xx 처리 중복 정리(이민하·황준용)
- 바꾼 파일: 이 문서

### 2026-11-03 — 이민하 PR #23 리뷰
- 한 일(Claude 에이전트와 함께, 장호연 지시): 이민하 PR #23(업로드 대기열 상한·영구 거절 건너뛰기, TTS 해제 뒤 호출 차단, 이슈 #19·#21) 리뷰 → ✅ 머지 가능. 단독 166개, PR #22와 합쳐 190개 테스트 통과, 충돌 없음
- Supabase PR(차슐랭 #10, 크루즈브레이커 #22)은 리뷰 뒤 새 커밋 없음(황준용 수정 대기)
- 바꾼 파일: 이 문서

### 2026-11-03 — 차슐랭 점수 공식 확정
- 장호연 결정(Claude 추천안 승인): `점수 = 평균 별점 × (1 − 최근 30일 실패율)`. 실패율 = 최근 30일 실패 ÷ (인증 + 실패), [실패 아님] 취소분 제외. 최근 30일 세션 5건 미만이면 감점 없음. 리뷰 5개 미만이면 "평가 중"
- 이유: 리뷰는 인증 충전 뒤에만 뜨므로 실패한 운전자는 별점을 못 남김 → 별점만 쓰면 고장 잦은 곳이 1위가 될 수 있음(예: ★4.8·실패 20% → 3.8 vs ★4.5·실패 0% → 4.5). 인증 수·kW는 가중치 근거가 약해 점수에 넣지 않고 카드에 따로 표시
- 검토했지만 안 고른 것: 별점만(지금 서버 구현, 실패가 순위에 반영 안 됨), 종합 가중 점수(기획서 옛 F6 문구, PT에서 가중치 설명이 어려움)
- 후속: 황준용 Supabase PR(차슐랭 #10)에서 `pleos_nearby_stations` 점수 계산 변경(새 migration) + 시드에 실패 잦은 시연 충전소
- 바꾼 파일: 이 문서(규칙 원문은 차슐랭 `PRD.md` 5.5)

### 2026-11-03 — 제출 빌드 데이터 모드 결정
- 장호연 결정: 공모전 제출 release APK는 시연 데이터 모드(시드 + 시뮬레이션)로 빌드. 이유: LIVE는 실제 충전소 데이터가 없어 심사위원 에뮬레이터에서 화면이 빔. PRD 7절에 기록(두 앱)
- 바꾼 파일: `PRD.md`, 이 문서

### 2026-11-03 — Supabase PR 리뷰(차슐랭 #10, 크루즈브레이커 #22)
- 한 일(Claude 에이전트와 함께, 장호연 지시): 두 PR 브랜치 빌드·테스트·lint 통과(200개, 180개, core lint 오류 0), core 동일, 비밀값 없음 확인. 스키마·RPC·core 대기열·철회 흐름 검토 후 두 PR에 리뷰 코멘트
- 결과: 🔧 수정 필요
  - 막는 것: ① 충전소 밖 충전도 서버에 올라감(11-02 결정 위반, S6는 서버 목록만 읽어 기기 저장 병합 필요) ② 시드 날짜가 11-02에 고정 → 크루즈 위험 구간 11-28쯤, 차슐랭 7일 실패 11-09쯤 만료 → 2차 PT에 시연 데이터 없음
  - 장호연 판단: ③ release = LIVE라 제출 APK에 시드가 안 보임(충전소 0개, 위험 구간 0개) → 제출 빌드를 DEMO로 할지 ④ 차슐랭 점수가 평균 별점만(PRD 초안은 실패 감점, 기획서 F6은 실패 반영) ⑤ AppContainer·Gradle·매니페스트 경로 밖 수정(받아도 된다고 봄)
  - 해결되는 것: 차슐랭 #9(CAR_INFO), 크루즈브레이커 #12·#20, 차슐랭 #8·크루즈브레이커 #19의 핵심(core 영속 대기열·철회 ACK)
- 바꾼 파일: 이 문서

### 2026-11-03 — 최종 검토
- 한 일(Claude 에이전트와 함께, 장호연 지시): 열린 PR 0건 확인. main 빌드·테스트(차슐랭 176개, 크루즈브레이커 156개, 실패 0)·앱 lint 통과, 두 저장소 core 동일
- 찾은 것: `:core:lintDebug` 오류 3건. `NAVI_ROUTE_SEARCH` 2건은 앱 매니페스트에 있어 문제 아님. `getEvBatteryCapacity`의 `CAR_INFO`는 어느 앱에도 선언 안 됨 → Pleos 문서상 심사 불통과 사유라 차슐랭 이슈 #9로 황준용에게(호출 제거 권장)
- 바꾼 파일: 이 문서

### 2026-11-03 — PR #7·#18 머지, 시연 리허설, 기획서 DOCX 재생성
- 한 일(Claude 에이전트와 함께, 장호연 "전부 검토 후 진행" 지시):
  - 길민종 알림 권한 복구 PR(차슐랭 #7, 크루즈브레이커 #18): 리뷰 뒤 커밋(설정에서 허용하면 미확인 횟수 초기화) 확인, 브랜치 빌드·테스트 통과(156개, 136개) → 머지
  - 에뮬레이터 리허설(최신 main 디버그 빌드 새로 설치, 동의부터):
    - 차슐랭 `demo-charge.sh success`: S4 "인증 충전 완료 · 시흥동 주민센터 · 40%→80% · 35분 · 53kW" + 별점. `fail`: S5 "충전 실패를 기록했어요". S1 시흥동 인증 21→22, 7일 실패 4→5, S6에 인증·실패 두 줄
    - 크루즈브레이커 `demo-drive.sh`(고정 위치): S5가 동의 전 정차 상태에서 정상 표시(PR #16 UX 제한 OK) → 출발 시 배너 "500m 앞 크루즈 해제 잦은 구간" + TTS 재생 → 90km/h 해제만 기록(`disengage ... speed=90.0`), 40km/h 해제·도착은 기록 안 됨 → P에서 S3 "지난 위험 구간 1곳 · 음성 안내 1회 · 내가 보탠 기록 1건"
  - 기획서 DOCX 재생성(두 앱, 11-03 md 기준). python은 이 PC에 `py -3`로 있었음
- 주의: 리허설 도우미가 [완료] 대신 "인증 충전 완료" 글자를 눌러 첫 리뷰는 제출되지 않았음(앱 문제 아님). `demo-*.sh` 출력을 `| tail`로 받으면 하트비트가 파이프를 잡아 명령이 안 끝남 → 파일로 받을 것
- 바꾼 파일: `docs/submission/*-application.docx`, 이 문서

### 2026-11-03 — 남은 일 점검
- 한 일(Claude 에이전트와 함께, 장호연 지시): 최신 main 받아 남은 일 점검. 오늘 머지된 PR(차슐랭 #5·#6, 크루즈브레이커 #15·#16·#17) 반영 후 main 빌드·테스트 통과(차슐랭 172개, 크루즈브레이커 152개, 실패 0). 두 저장소 core 동일
- 새로 생긴 것: 길민종 PR(차슐랭 #7, 크루즈브레이커 #18), 리뷰 이슈(차슐랭 #8, 크루즈브레이커 #19·#20·#21). Supabase 커밋은 아직 없음
- 위 "다음 할 일"을 지금 상태로 다시 씀
- 바꾼 파일: 이 문서

### 2026-11-03 — 시연 스크립트 도착 장면 수정(PR 리뷰 반영)
- 한 일(Claude 에이전트와 함께, 장호연 지시): Claude PR 리뷰 "수정 필요" 반영. `demo-drive.sh`의 `arrive()`와 `stop`이 100km/h에서 크루즈를 꺼서 도착도 해제로 기록되던 문제 → 차속 0으로 먼저 세운 뒤 해제, 그다음 P
- `wait_until`의 `date +%s%3N`이 GNU date 전용이라는 주석 추가(선택 제안)
- 확인: 가짜 콘솔·가짜 adb로 고정 모드·`stop` 순서 확인(정차 → 해제 → P). 에뮬레이터 리허설은 아직
- 바꾼 파일: `tools/demo-drive.sh`(두 저장소 같은 내용), 이 문서

### 2026-11-03 — 길민종 PR #16 에뮬레이터 확인
- 한 일(Claude 에이전트와 함께, 장호연 지시): PR #16(`fix/onboarding-and-v2-ui`, `208e175`) 릴리스 빌드를 디버그 키로 서명해 이 PC의 Pleos 에뮬레이터에 새로 설치하고 확인. 결과는 PR 코멘트(https://github.com/hoyeoni1/hyundaiapp-cruisebreaker/pull/16#issuecomment-6592746314)
- 확인한 것: 리뷰 뒤에 추가된 `CarUxRestrictionsManager` 연결이 실제로 동작함(P `DO: false` → D `DO: true` → P `DO: false`). 첫 실행 S5(권한 팝업 없음), 차속 거부·알림 허용 후 시작, P에서 S2·S4, D·80km/h S1(터치 0개), P 복귀 S3 한 번 → [확인] S2, 철회 → S5·서비스 중지, 동의 전 D는 터치 없는 S1, 재동의 → 재시작. **머지해도 됨**
- 알게 된 것: 차속 권한을 거부하면 정차 중 크루즈 해제도 `speed=null` 해제로 기록됨(규칙 v2: 차속 없으면 속도 조건 건너뜀). PR 문제는 아님. PRD 8절에 적을지 이민하와 정하기
- 함정: 에뮬레이터를 처음 띄웠을 때 시작 중 죽고 오류 보고 창에서 멈춤(adb에 안 보임) → `qemu-system-x86_64.exe` 종료, AVD 폴더의 `*.lock` 삭제 후 같은 명령으로 다시 띄우니 정상. 앱은 사용자 10으로 돌아서 `dumpsys package`의 권한은 `User 10` 아래를 봐야 함
- 못 본 것: 자동 기록 끔 + 재부팅, Car 서비스 연결이 끊겼다 돌아오는 경우, 세로 화면
- 바꾼 파일: 이 문서

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

### 2026-11-02 — 시연 스크립트 v2 정리·PR
- 한 일(Claude 에이전트와 함께, 장호연 지시): `feat/demo-scripts`에 main merge(이 문서 충돌은 main 내용으로), 스크립트를 v2와 현재 앱에 맞춤
  - `tools/demo-drive.sh`: 기본 = 고정 위치 시연(출발 → 500m 앞 안내 → 100km/h 해제 기록 → 40km/h 해제는 기록 안 함 → 정차 요약). `route` = 경로 재생(1배속). 차속 권한 `pm grant`를 스크립트가 줌
  - `tools/demo/gyeongbu-north.csv`: 표시만 다시 만듦(안내 500m, 정체 `jam` → 저속 `slow` 300m). 좌표는 그대로. 이 PC에 python이 없어 `make-route.py`를 node로 옮겨 기존 CSV와 똑같이 나오는지 확인한 뒤 생성
  - `tools/demo-charge.sh`: 전제에 S7 동의 먼저, 시연 배속 ×120 추가(이상준 요청)
- 결정한 것과 이유:
  - 정체 장면을 저속 해제로 바꿈: v2에서 정체 필터가 없어지고 "해제 순간 60km/h 이상"만 셈
  - 경로 재생은 1배속 기본: 안내가 3분에 한 번뿐이라 2배속이면 c 구간 안내가 안 나옴. 안내 장면은 a·c 둘만
  - 앱 위치 재생 모드(`DEMO_ROUTE` 브로드캐스트)는 아직 없음 → 지금 시연은 고정 위치 모드로
- 확인: 가짜 콘솔(node)·가짜 adb로 세 모드의 값 순서 확인. 에뮬레이터 실행은 아직
- 바꾼 파일: `tools/demo-drive.sh`, `tools/demo-charge.sh`, `tools/demo/make-route.py`, `tools/demo/gyeongbu-north.csv`, 이 문서

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
