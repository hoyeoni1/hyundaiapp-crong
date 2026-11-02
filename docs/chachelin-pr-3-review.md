# 차슐랭 PR #3 리뷰 — 2026-11-02

- 대상: https://github.com/hoyeoni1/hyundaiapp-chachelin/pull/3
- 작성자: 이상준(`wklee316`)
- 검토 HEAD: `737fb38e645f8f12a1deddce3f14378f6b7dc7bb`
- 결론: **수정 후 머지 권장**. 원격 댓글·승인·머지는 실행하지 않음.

## 수정 필요

### P2: Fake 업로드가 새 멱등성 계약을 지키지 않음

`app/src/main/java/com/hyundaiapp/chachelin/data/ChargeRecordRepository.kt:38–40`

인터페이스와 PR 설명은 같은 세션 ID 재전송 시 한 건만 남도록 요구하지만, fake는 매번 목록 앞에 추가한다. 별도 임시 체크아웃에서 동일한 `ChargeSessionRecord`를 같은 차량 ID로 두 번 업로드한 뒤 목록 크기 1을 기대하는 테스트가 실패했다(실제 2건). 충전소 인증 건수와 리뷰 집계가 중복될 수 있고, 내 기록 화면의 세션 ID 기반 LazyColumn 키도 중복된다.

세션 ID 기준으로 기존 항목을 유지하거나 교체하고 중복 업로드 회귀 테스트를 추가해야 한다. 현재 fake 자체는 네트워크 오류를 만들지 않으므로 평상시 한 번 업로드에서는 드러나지 않는다. 새 재시도 계약을 검증하는 fake로 사용하려면 먼저 맞춰야 한다.

## 머지 전 담당자 확인

PR 본문에서 `ChachelinNav.kt`와 `HistoryScreen.kt`의 담당 경로 밖 변경을 명시하고 길민종 확인을 요청했다. 확인 후 장호연이 머지할 수 있다.

## 서버 연결 전 보완

`AppContainer.withdrawConsent()`는 기존 차량 ID를 지운 뒤 삭제 요청을 한 번 실행하고 실패를 무시한다. 현재 메모리 fake는 실패하지 않지만 실제 서버 구현으로 바꾸기 전에 삭제 실패 시 재시도할 ID·작업을 보존해야 한다. 현 fake 데모의 별도 머지 차단 사유로 계산하지 않았다.

## 검증 범위

- 별도 detached worktree에서 검토. 두 제품 작업 브랜치의 소스는 변경하지 않음.
- PR 기본 테스트: 앱 debug/release 각각 63개, core debug/release 각각 3개 통과.
- 앱 debug lint: 오류 0, 경고 9.
- 별도 리뷰 회귀 테스트: 동일 세션 2회 업로드 시 한 건 기대 → 실패. 테스트는 PR 원본 검증 전에 소스 밖으로 이동.
- `git merge-tree --write-tree codex/integrate-sdk-20261102 origin/pr/3`: 충돌 없음. 실제 브랜치 머지는 하지 않음.
- 원격 PR HEAD를 재확인했으며 검토 HEAD와 일치.
- 실차·서비스 재시작·실제 서버 통합은 이번 리뷰에서 실행하지 않음. 기존 core lint 문제는 이 PR의 신규 결함으로 계산하지 않음.
