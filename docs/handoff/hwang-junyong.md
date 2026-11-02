# 인수인계 — 황준용 (공통 플랫폼)

담당 경로: `core/`(복사본 — 수정은 차슐랭에서), 서버, `app/.../data/` 실제 구현

## 현재 상태 (2026-11-02 기준, 매번 덮어쓰기)

- PR 전 기여 범위 재확인: 제품 core/src/main/java 구현은 origin/main과 동일하며 장호연의 기존 기반을 유지했다. 우리 추가분은 debug SDK probe·상태/Fake 모델·테스트·Windows 실행 도구·타입 검증·통합 검증이다. 실제 서버/영속 재전송/집계 API는 미구현. PR 발행 완료: 차슐랭 https://github.com/hoyeoni1/hyundaiapp-chachelin/pull/4 · 크루즈 https://github.com/hoyeoni1/hyundaiapp-cruisebreaker/pull/14. 리뷰·머지는 장호연 대기.

- 차슐랭 PR #3(`737fb38`) 리뷰: fake 업로드의 세션 ID 중복 방지 누락을 회귀 테스트로 재현해 수정 후 머지를 권장. UI 담당자 확인도 필요. 상세: `docs/chachelin-pr-3-review.md`. 원격 PR 조작 없음.
- 원격 main을 로컬 SDK·Windows 진단 작업과 통합 완료. 브랜치: `codex/integrate-sdk-20261102`. 브랜치 push·PR 발행 완료. 원격 main 머지는 하지 않음.
- 제품용 core는 다른 담당자의 앱이 사용하는 원격 계약과 구현을 유지. 로컬 snapshot SDK 진단은 `com.hyundaiapp.core.probe`의 debug 전용으로 분리해 충돌 없이 보존(core v0.4.0).
- 앱의 기어·길안내 API, 해제 감지·안내·기록 서비스·동의 철회 및 UI 원본은 유지. app 코드 변경은 compileSdk 35 + android.car 빌드 설정 통합뿐.
- 두 저장소의 core와 tools 동기화를 확인했고, 앱/코어 테스트·빌드·lint 및 실제 SDK 실행 확인이 통과했다. 상세 결과는 아래 작업 기록 참조.
- 다음 할 일: 팀 리뷰 후 장호연이 PR 머지. 제품 SDK와 raw probe를 장기적으로 단일 backend로 합칠지는 별도 설계 검토. 서버 연동은 사용자 요청대로 보류.
- 주의: windows-smoke PASS는 raw probe SDK 수신 결과이며 제품 기록·동의·업로드 통합 성공을 뜻하지 않는다. 실제 제품은 개발 메뉴 신호 모니터 및 앱 단위 테스트로 확인.
- 확인 방법: `gradlew.bat :core:testDebugUnitTest :core:testReleaseUnitTest :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --max-workers=2`, `tools/windows-test.ps1`, `docs/windows-local-testing.md`.

## 작업 기록 (최신이 위, 지우지 않음)

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
