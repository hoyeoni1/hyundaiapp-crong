# 인수인계 — 황준용 (공통 플랫폼)

담당 경로: `core/`(복사본 — 수정은 차슐랭에서), 서버, `app/.../data/` 실제 구현

## 현재 상태 (2026-11-10 기준, 매번 덮어쓰기)

- 서버·앱 연결과 공개 설정 공유는 main에 반영됐고 두 앱의 서명 release APK가 준비됐다. core는 양쪽 모두 v0.5.2이며 동일 소스를 확인했다.
- 약관 담당자 전달용 기술 답변은 차슐랭 server/review-20261109.md의 11-10 항목에 모았다. DB 주 리전은 호주 시드니(ap-southeast-2), 운영 로그는 공식 안내상 EU 저장이다. 11-09 실제 로그 검사에서 정상 조회 좌표는 발견되지 않았지만 잘못된 좌표 타입 4건은 PostgreSQL 오류 메시지에 남았다. Free API/DB 로그 보관 1일과 플랫폼 전체 삭제 보장은 구분한다.
- 11-09 원격 이름 migration 적용과 CLI 이력을 정합화했고 실제 조회 이름 판교IC 진입로·충전 시드325건을 확인해 크루즈 #37을 종료했다. 별점 분포 migration의 원격 적용은 마지막 확인 시 대기 중이다.
- PR #36의 이름 변경과 리허설 정리 SQL을 검토했다. 촬영 전 업로드를 모두 처리하고 기록을 중지한 뒤 정리한다. 이번 점검에서는 원격 정리 SQL을 실행하지 않았다.
- 신규 20261109000100 migration으로 원래 시드 리뷰233건을 재배분했다. 평균 별점 약3.2~4.3, st-004 별점1위/점수최하위를 유지한다. 충전325건과 실제 앱 리뷰를 유지한다. 코드/로컬 검증 단계이며 원격 적용은 PR 검토 뒤 진행한다.
- 로컬 DB20 테스트와 core debug32 테스트 통과. Android Keystore 손상 재현이나 최신 전체 UI 리허설은 이번 점검 범위에 포함하지 않았다.
- 리전·요청 로그 실측과 core v0.5.2 확인 답변은 완료했다. 남은 일은 별점 migration 원격 적용 상태 확인과 대표자·약관 담당자의 문구 및 판 번호 확정이다. 사용자 지시에 따라 terms-draft.md와 TermsText.kt는 수정하지 않는다. core 손상 초기화의 전송·철회 대기 복구 한계는 확인 기록을 참고한다.

## 작업 기록 (최신이 위, 지우지 않음)

### 2026-11-10 — 약관 담당자 기술 답변과 core 확인 인계
- 두 저장소 최신 main을 fast-forward로 반영하고 core 추적 파일31개·버전0.5.2 일치를 다시 확인했다. 기존 core debug 테스트32개 실패0 결과를 확인했으며 이번에는 재실행하지 않았다.
- 11-09 실제 로그 검증(요청10건, 정상 조회 좌표 미발견, 잘못된 타입4건 오류 메시지 기록)과 DB 리전·로그 보관·국외 처리 범위를 서버 확인 문서에 정리했다. 검사 임시 Auth 사용자는 삭제 완료, 업무 기록 생성 없음.
- 약관 담당자에게 전달할 답은 차슐랭 server/review-20261109.md의 11-10 항목이며 상세는 server/log-verification-20261109.md다. core 변경 확인과 복호화 실패 시 상태 손실·로그 표시 한계도 명시했다.
- terms-draft.md·TermsText.kt·동의 버전은 수정하지 않는다. 담당자가 팀 내 논의 후 반영한다. 저장소 문서 규칙에 따라 문서는 main으로 공유하며 코드 PR·머지는 하지 않는다.


### 2026-11-09 — 서버 피드백 검토와 별점 분포 조정
- PR #36의 시드 이름 변경/리허설 정리 SQL을 읽고 기존 회귀 테스트20개를 실행했다. 이름 migration은 원격 대기 상태다.
- 새 migration으로 시드 리뷰 평균을 10개 값으로 분산했다. 같은 날 재실행·날짜 갱신, 실제 리뷰/세션 보존, 불완전 시드 중단을 확인했다.
- CLI 프로젝트 메타데이터에서 ap-southeast-2(시드니)를 확인하고 관리 세션 DB 로그 설정을 조회했다. 공식 로그 필드·보관·리전 외 처리 문서와 코드의 POST 본문 전송을 대조했다.
- core v0.5.2 변경과 양쪽 소스 일치를 확인하고 core debug32 테스트 통과. 복호화 실패 시 상태 손실/로그 표시의 한계를 확인 기록에 적었다.
- 상세는 차슐랭 server/review-20261109.md다.

### 2026-11-03 — 장호연이 이 PR 브랜치에 남은 수정을 이어서 반영(황준용 작업 대신)
- 장호연 지시("다 진행해줘, 내가 팀장이니 내가 진행할게", Claude 에이전트가 작업). 황준용은 이 브랜치를 **pull한 뒤** 이어서 작업할 것
- 한 일: release 기본 SIMULATED(`local.properties` `supabase.demo=false`일 때만 LIVE, 두 앱 `app/build.gradle.kts`), `BackendConfig.consentVersion`(core, 두 앱 동일) + core 테스트 1개, 새 migration `20261103000100_score_failure_penalty.sql`(점수 = 별점 × (1 − 30일 실패율)), `20261103000200_demo_failure_station.sql`(st-004 실패 잦은 시드), `refresh-demo-dates.sql`이 새 시드(소유자 42, 충전 325)를 인식, PGlite 테스트 2개 추가(17개 통과), main merge(README 충돌 해결)
- **원격 미적용**: 두 migration은 장호연이 대시보드로 적용 예정(`server/deploy.md` "11-03 추가 migration"). CLI 이력은 적용 뒤 `migration repair --status applied`로 맞추기
- 확인: 두 앱 `assembleDebug assembleRelease test lint` 통과(차슐랭 208, 크루즈 192), release BuildConfig `SUPABASE_SIMULATED = true`

### 2026-11-03 — 서버 PR 피드백 1·2 반영
- 차슐랭 신규 충전소 밖 충전을 서버 outbox 대신 암호화/noBackup 로컬 저장소로 분기. 재실행/오프라인 내 기록 병합, 중복 방지, 철회 즉시 삭제·늦은 저장 차단을 구현했다. 이전 서버/outbox 기록은 소급 이전하지 않는다.
- 시드 날짜 갱신 SQL·상태 조회·운영 절차 추가. 고정 SEED 소유자/건수를 확인하고 한국 날짜를 기준으로 원래 행만 갱신한다. 같은 날 재실행 가능, 변형 시드 중단, LIVE/SIMULATED 보존. 원격 실행과 무료 플랜 운영 대책은 이번 범위 밖이다.
- 차슐랭 앱 debug/release 테스트 각81, 빌드·lint 오류0/경고14. DB15 테스트에 60일 노후 시드 복구·반복 실행·앱 데이터 보존을 추가 검증했다. 공통 core 변경 없음. 기존 PR #10/#22에 반영.

### 2026-11-03 — 팀원 서버 연결 안내 보강
- 공통 서버 설명서에 PR 적용·공개 URL/키 설정·debug 재빌드·동의·현재 Android 사용자별 연결 검사·문제 해결 순서를 추가했다. 앱 개발과 관리자 CLI 작업의 준비물을 구분했다. 문서 변경만 있으며 기존 PR에 반영한다.

### 2026-11-03 — 최신 main 통합 및 서버 인수인계 정리
- 원격 main을 양쪽 브랜치에 병합. 기존 알림·화면·기록 큐·삭제 재시도를 유지하고, 서버 철회가 로컬 큐 등록에서 끝나지 않도록 awaitWithdrawal ACK 계약을 연결했다.
- core v0.5.1로 동기화. 미선언 CAR_INFO getter 제거 및 내비 검색/취소 권한 guard 추가. lambda 경계를 lint가 추적하지 못하는 두 메서드에만 근거를 적어 MissingPermission을 제한적으로 억제한다. 런타임 권한 검사는 유지한다.
- 서버 개요 문서를 신설하고 기존 설계/연결/운영 문서의 과거 미구현 표시를 수정했다. 원본 migration은 차슐랭 한 곳에서 관리한다.
- DB 검증 및 db push --dry-run 정상. 원격 스키마/시드 변경 없음. 검증 결과와 PR 링크는 아래 현재 상태에서 확인한다.
### 2026-11-03 — Supabase 실제 Android 연결·검증
- Anonymous Sign-In=true 확인 후 HTTP 원격 통합 테스트를 추가하고 통과. 그 run의 테스트 계정/업무 기록을 UUID marker로 한정해 정리했다.
- core.backend, 앱 실제 저장소/Gradle 공개 설정/INTERNET 권한 연결. 차슐랭 PR #3은 로컬 merge만 했다. 두 core는 0.5.0으로 동일하게 동기화.
- Keystore 암호화 상태·FIFO outbox·영구 오류 보관·재시도·철회 세대 보존을 구현하고 신규 테스트11개 통과. 앱/코어 양 variant 테스트와 빌드·lint 통과.
- user10 Pleos emulator에서 두 앱의 shell 전용 debug fixture가 실제 제품 backend로 익명 인증, 세션 재로드, 영속 작업 업로드, 서버 철회 및 로컬 인증 제거를 통과. 릴리스에는 fixture/probe Activity가 없다.
- 식별된 Android 테스트 설치의 서버 차단 행/익명 계정을 CLI로 정리. 실패 검사에서 식별하지 못한 최소 REVOKED 차단 정보는 남아 있을 수 있으며 집계에 포함되지 않는다. 기존 시드는 변경하지 않음.
- Android 검사 첫 시도에서 INTERNET 신규 권한 추가 직후 EPERM 발생 → force-stop 후 정상. 이후 검사 대기열에 이전 정리 작업이 먼저 처리되는 점을 반영해 fixture drain을 고침.


### 2026-11-02 — CLI 원격 검증 및 표준 migration 이력 정합 완료
- 사용자 login/link 완료 후 CLI db query로 자체 적용 버전 2건, 시드 충전소 10/충전 300/해제 400/통과 2000, RLS 9개 모두 true, RPC 11개 anon=false/authenticated=true 확인. 원격 함수 fingerprint f0b69344326a5769b916cafeb7698bbe는 로컬 SQL 적용 결과와 일치.
- migration repair로 20261102000100·20261102000200을 applied 등록. LOCAL/REMOTE 두 행 일치 및 db push --dry-run upToDate=true·대기 migration 없음. SQL 본문·시드 재실행과 기존 업무 데이터 변경 없음.
- verify.sql을 단일 JSON 응답으로 바꿔 마지막 SELECT만 노출되는 문제 해결. 변경 후 로컬 테스트 15개 통과. CLI 사용법·배포 상태 문서 최신화.
- 공개 API로 Anonymous Sign-In 비활성 재확인. 실제 가입/사용자 생성·Auth 설정 변경·Android 앱 서버 전환은 하지 않음. CLI 캐시·자격 증명·개인 환경값은 Git 제외.

### 2026-11-02 — 사용자 PowerShell CLI 경로 문제 확인
- 사용자는 안내한 supabase.exe 경로에서 CommandNotFoundException. 에이전트 셸에서는 같은 절대 경로의 파일 존재·CLI 2.122.0 실행을 재확인. 사용자 셸에서 보이지 않는 원인은 아직 확정하지 않음.
- 사용자 PowerShell에서 공식 ZIP을 직접 받아 설치 후 실행하도록 안내. 인증 및 원격 DB 작업은 진행하지 않음.

### 2026-11-02 — Supabase CLI 연결 준비
- 반복 SQL 복사 대신 CLI를 쓰기로 함. 공식 supabase/cli 2.122.0 Windows amd64를 로컬 개발 도구 경로에 설치하고 공식 SHA-256 검증. 차슐랭 server/supabase/config.toml 생성(project_id=pleos-apps-dev), 원격 설정은 변경하지 않음.
- server/cli-setup.md에 사용자 login·link 절차 작성. 인증 완료 후 db query로 기존 적용 상태를 검증한 뒤 표준 migration 이력에 두 수동 적용 버전을 repair할 예정. 이력 수정·db push는 아직 하지 않음.
- 사용자 공유 RPC 실행 권한은 정상. 시드 건수와 버전·RLS는 로그인 이후 확인. 토큰·비밀번호는 채팅으로 받지 않음.

### 2026-11-02 — 실행 가능한 Supabase migration과 테스트 준비
- 사용자 요청으로 초기 SQL과 시연 데이터 migration 작성. private 원본/RLS 및 authenticated 전용 RPC 11개, 다른 소유자 접근 차단, 등록/세션/리뷰 중복 처리, 정정 영수증, 철회 행 잠금·원본 삭제, 9칸 차량-일 집계를 구현했다.
- 로컬 PostgreSQL 테스트 15개 통과. Supabase 서비스 전체·동시 다중 연결 테스트와 원격 적용은 아직 아님. SDK 타입·앱 UI/도메인 코드 변경 없음.
- Chrome 제어 도구가 접근을 거부(Computer Use was not approved to use Google Chrome). 사용자가 대시보드 직접 SQL 실행을 선택해 `server/deploy.md`에 4단계 파일과 기대 결과를 정리했다.
- 기존 기록 이전 대상은 없으며 새 스키마+가상 데이터 초기 입력이다. 실패 감점은 넣지 않았고 평균 별점과 실패 정보를 별도로 반환한다. 보관·약관 정책은 확정하지 않았다.


### 2026-11-02 — 공통 서버 스키마·서비스 흐름 설계
- 차슐랭 PR #3과 크루즈 main의 저장소 계약·PRD·규칙 문서를 대조해 `server/schema-design.md`를 작성. 앱 설치 소유권·충전소/세션/후기·구간/해제/통과 구조와 API 접근 경계를 제안했다.
- 재전송 중복 방지, 차량-일 중복 제거, 해제-통과 원자 저장, 실패 정정, 철회-업로드 경쟁 처리, 오프라인 삭제 재시도까지 설명했다.
- 보관·삭제 기간과 점수 감점 계수는 미정으로 구분. SQL 실행·Supabase 설정 변경·새 PR/push는 하지 않았다. 문서 변경만이라 런타임 테스트는 실행하지 않았다.


### 2026-11-02 — Supabase 프로젝트 연결 확인
- 공개 키로 Auth health/settings 200, stations 조회는 PGRST205(미생성), anonymous_users=false 확인. 데이터 쓰기·가입·관리자 키 사용 없음.
- 기존 SDK PR과 분리한 새 로컬 브랜치 생성. 차슐랭 server/에 공개 연결값 로컬 설정(git 제외)·예제·인증/서버 작업 안내 작성. 이메일 없는 인증과 auth.uid 소유권, 영속 재전송/철회 삭제를 설계 방향으로 기록.
- 사용자 준비: 채팅에 제공한 Secret key 폐기, Anonymous Sign-In 활성화. migration과 앱 저장소 구현은 후속 작업. Asia Pacific의 구체적 AWS 리전은 미확인.

### 2026-11-02 — SDK 진단 및 Windows 테스트 기반 PR 발행
- 사용자 요청으로 두 저장소의 core v0.4.0 변경을 각각 PR로 제출 완료(차슐랭 #4 / 크루즈 #14). debug 전용 probe와 제품 core의 차이, 기존 검증 결과, 서버 미구현 상태를 PR 본문에 명시한다.
- 진행 문서 최신화 및 변경 파일 검사. 기존 PR #3의 로직은 포함하지 않으며 원격 main 머지는 장호연에게 맡긴다.

### 2026-11-02 — PR 전 팀 진행·기여 범위 점검
- 두 저장소 fetch 및 열린 PR 조회. 크루즈 main은 76253c5, 차슐랭 main은 4bfa6a2(최신 추가는 팀장 인수인계 문서뿐). 차슐랭 PR #3은 737fb38로 열려 있으며 크루즈 열린 PR은 없음.
- 오래된 handoff의 미완료 표시보다 실제 머지·커밋 상태를 우선해 정리. 크루즈 앱 핵심 로직은 이민하, 두 앱 화면은 길민종, 초기 제품 core·앱 기반은 장호연의 기여로 구분.
- 우리 목표 중 SDK 타입 검증·debug 진단/Fake·Windows 환경과 문서·팀 코드 보존 통합은 완료. 제품 backend 통합 및 서버 저장·재전송·삭제·집계는 후속 작업. 이번 점검에서 코드·원격 브랜치·PR은 변경하지 않음.

### 2026-11-02 — 이상준 차슐랭 PR #3 리뷰
- PR HEAD를 별도 임시 체크아웃에서 검토. 동일 세션 두 번 업로드 시 기록이 두 건 남아 새 멱등성 계약을 위반함을 확인.
- PR 기본 앱 테스트 debug/release 각 63개, core 각 3개 통과. 앱 lint 오류 0·경고 9. 로컬 통합 브랜치와 merge-tree 충돌 없음.
- 원격 댓글·승인·머지 및 두 제품 브랜치 소스 변경 없음. 서버 삭제 실패 처리 보완은 실제 서버 연결 전 후속 과제로 기록.

### 2026-11-02 — 로컬 SDK 작업과 팀 원격 main 통합
- 병합 전 로컬 작업을 보존 커밋한 뒤 origin/main을 병합. 기존 차슐랭 pull은 진행 중 병합이 아니라 로컬 수정으로 업데이트가 막힌 상태였음.
- 제품 API를 원격 버전으로 유지하고 로컬 진단 코드를 debug/probe 및 testDebug/probe로 이동. 기존 동작은 보존 커밋에서 다시 확인할 수 있음.
- app/build.gradle.kts와 core/build.gradle.kts는 원격 compileSdk 35·Compose·의존성을 유지하고 android.car 설정을 결합. 매니페스트는 원격 서비스·TTS·권한을 보존하고 중복 CAR_SPEED 제거.
- core/README, Windows 문서·스크립트에 제품과 raw probe 구분을 기록. 작성자 미설정으로 보존·통합 커밋에만 Codex <codex@local.invalid> 임시 작성자를 사용하며 전역 Git 설정은 변경하지 않음.
- 검증 완료: 앱 단위 테스트 debug/release 각각 44개, core debug 17개(제품 GeoMath 3 + probe 14)/release 3개 통과. debug APK 빌드 및 lint 통과(0 errors, 12 warnings).
- 실제 Pleos: 두 앱 제품 신호 모니터 vehicle/navi READY, 크루즈 제품 speed READY. raw probe smoke 크루즈 7개/충전 4개 통과. release core JAR와 매니페스트에 probe/진단 Activity가 없음을 확인.
- 통합 기준 origin/main: 76253c5. 원래 로컬 SDK 작업 보존 커밋: 3d4f492. 이 작업은 원격 PR 머지가 아니라 로컬 통합 브랜치 병합이다.

### 2026-10-30
- 장호연: 파일 생성. 첫 작업은 README "담당별 첫 작업" 기준으로 적어 둠.
