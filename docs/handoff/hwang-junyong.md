# 인수인계 — 황준용 (공통 플랫폼)

담당 경로: `core/`(복사본 — 수정은 차슐랭에서), 서버, `app/.../data/` 실제 구현

## 현재 상태 (2026-11-03 기준, 매번 덮어쓰기)

- 브랜치: `codex/supabase-foundation-20261102`. 서버 원본은 차슐랭 `server/`. 이전 SDK 진단 PR(차슐랭 #4/크루즈 #14)과 충전 PR #3은 팀장이 main에 반영한 상태다. 이번 PR은 서버·제품 backend 연결과 문서다.
- 최신 main을 양쪽 로컬 브랜치에 병합했다(차슐랭 778b7c5 / 크루즈 9f873fb 기준). 최신 화면·알림·삭제 재시도를 보존하고, 크루즈 AppContainer 충돌은 영속 철회와 PendingDeletions를 함께 유지하도록 해결했다.
- Supabase 두 migration 및 CLI 이력 정합 완료. 이번 최종 검사는 읽기/드라이런만 실행: RLS 9개, authenticated 전용 RPC 11개, 시드 10/300/400/2000, 함수 fingerprint 일치. 대기 migration 없음. SQL/시드는 다시 적용하지 않았다.
- core v0.5.1: HTTPS 익명 인증·갱신, Keystore AES-GCM/noBackup 영속 상태, FIFO outbox, 세대별 철회 재시도와 서버 ACK 대기를 제공한다. CAR_INFO 미선언 getter 제거(#9), 선택적 내비 요청/취소는 런타임 권한 확인 후 실행한다. 두 core는 동일하다.
- 두 앱 실제 저장소 연결: 공개 URL/키 설정 시 Supabase, 설정 없으면 Fake. debug는 DEMO, release는 LIVE. deleteMine은 서버 삭제 ACK 후에만 성공하여 PendingDeletions를 지우게 했다. 새 회귀 테스트로 오프라인 삭제 대기·실패 재시도·ACK 완료를 검증한다.
- 최종 통합 검증: debug/release 앱 빌드 및 앱 테스트 각68, core debug29/release15(backend12) 통과. app lint 오류0/경고19, core lint 오류0/경고3. 로컬 DB 테스트15(차슐랭 원본) 및 공통 파일 일치 검사 통과.
- PR: [차슐랭 #10](https://github.com/hoyeoni1/hyundaiapp-chachelin/pull/10) / [크루즈 #22](https://github.com/hoyeoni1/hyundaiapp-cruisebreaker/pull/22). 최종 문서·검증 결과 포함, 원격 main은 직접 머지하지 않는다.
- 상세 서버 설명은 `docs/server-overview.md`: 구조·테이블·11 RPC·인증/RLS·재전송/철회·시드·운영/검증·남은 한계. `server/README.md`와 연결/설계 문서, docs/README 및 이 인수인계를 최신화했다.
- 실제 JWT HTTP 통합 검사와 양쪽 Pleos debug fixture(HTTPS·Keystore 복원·영속 outbox·철회 ACK)는 이전 연결 단계에서 통과했다. 이번 최신 main 통합 후 전체 SDK 주입/UI 시연은 재검사 전이다.
- 남은 일: 전체 제품 시연, WorkManager, 철회 대기 UI, 영속 읽기 캐시, 환경부 API 캐시, 약관/보관 정책 확정. 원격 main 머지와 최종 배포는 팀장 장호연이 한다.

## 작업 기록 (최신이 위, 지우지 않음)

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
