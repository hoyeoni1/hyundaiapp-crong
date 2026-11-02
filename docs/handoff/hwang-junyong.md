# 인수인계 — 황준용 (공통 플랫폼)

담당 경로: `core/`(복사본 — 수정은 차슐랭에서), 서버, `app/.../data/` 실제 구현

## 현재 상태 (2026-11-02 기준, 매번 덮어쓰기)

- SDK 진단·Windows 테스트 기반은 차슐랭 PR #4 / 크루즈 PR #14 제출 완료. 제품 core 계약을 유지했으며 원격 main 머지는 장호연 대기.
- 서버 작업 브랜치: `codex/supabase-foundation-20261102`. 원본은 차슐랭 `server/`; SDK PR과 분리했다.
- 실행 가능한 SQL migration 2개(initial schema / demo data), preflight·verify SQL 및 `server/deploy.md` 준비 완료. 앱용 7개+관리용 2개 테이블, RLS, 인증 RPC 11개, 소유권·중복 방지·실패 정정·철회·집계 포함.
- PGlite 0.5.14 PostgreSQL에서 15개 테스트 통과. 충전소 10·충전 기록 300·해제 400·통과 2,000 시드와 재적용 중복 방지, 원본 접근 차단, KST·28일·9칸/차량-일 집계를 검증했다. 실제 Supabase Auth/JWT·PostgREST·다중 연결 동시성은 미검증.
- 원격 DB에는 아직 적용하지 않음. 로그인된 Chrome 접근을 도구가 거부하여 사용자가 SQL Editor 직접 실행을 선택했다. 적용 후 verify 결과를 받아 확인해야 한다. 관리자 Secret key는 저장·사용하지 않았다.
- 차슐랭 점수는 우선 평균 별점(5개 미만 null), 실패 횟수는 별도다. 실패 감점·자동 보관 만료·Auth 정리는 미구현이며 정책 확정 후 적용한다.
- 다음 작업: 사용자 preflight → schema → demo → verify 실행 결과 확인 → 실제 인증/API/동시성 확인 → Android 인증·영속 outbox·실제 저장소 연결. 원격 생성·앱 전환이 끝났다고 해석하지 않는다.

## 작업 기록 (최신이 위, 지우지 않음)

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
