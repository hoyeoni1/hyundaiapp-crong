# 크루즈브레이커 (Cruise Breaker)

다른 차량들이 크루즈 컨트롤을 해제하는 지점을 모아, 크루즈 해제가 반복되는 구간(해제 다발 구간)을 진입 전에 알려 주는 Pleos Connect 앱. 팀 삼각고래 · 일반 부문.

- 기획: [PRD.md](PRD.md)
- 핵심 신호: Vehicle SDK `DrivingMode` 크루즈 상태 + AAOS 차속(해제 조건 60km/h) + NaviHelper 위치·진행 방향. 안내 음성은 Pleos TTS SDK
- 판정 규칙 v2(11-01, 정체 필터·해제 유형 없앰, 안내 500m 고정): [docs/disengage-rules.md](docs/disengage-rules.md)
- 실측 메모: [docs/vehicle-data-findings.md](docs/vehicle-data-findings.md) 6.2.1 (크루즈 해제 값은 에뮬레이터로 재현 불가, 실차 값 문의 필요)

## 시작하기 (처음 한 번)

1. 개발 환경: [docs/dev-env-setup-log.md](docs/dev-env-setup-log.md) — Android Studio, JDK 17, Pleos 에뮬레이터(`Pleos_Connect_2_1_0`)
2. 두 저장소를 **같은 폴더 아래 나란히** clone (`.../Chachelin`, `.../CruiseBreaker`)
3. Android Studio로 `CruiseBreaker` 폴더 열기 → Gradle Sync → 에뮬레이터 켜고 ▶ Run `app`
   - CLI: `./gradlew assembleDebug` / `./gradlew installDebug` / 테스트 `./gradlew test`
4. 처음 실행하면 **차속 권한**을 묻는다 → 허용
5. **개발 메뉴**(디버그 빌드에만 보임) → **신호 모니터**에서 `vehicle=READY`, `speed=READY`, `navi=READY`이면 정상
6. 값 바꿔 보기: `bash tools/vconsole.sh up` 후
   - `bash tools/vconsole.sh put 'gear?gear=Drive' 'controls?speed=100'` → 차속 100
   - `bash tools/vconsole.sh put 'driving-mode?ad_mode_state=full_autonomous'` → 크루즈 `ACTIVATED`
   - `bash tools/vconsole.sh put 'driving-mode?ad_mode_state=ready'` → `ENABLED`(시연에서 "해제"로 봄)

## 코드 구조

```
app/src/main/java/com/hyundaiapp/cruisebreaker/
├─ AppContainer.kt      객체 모음(수동 DI). 신호·저장소·감지기를 여기서 만든다
├─ domain/              판정 로직 (이민하)
│  ├─ CruiseStateInterpreter.kt  크루즈 작동/해제 해석 — 실차 값이 오면 여기만 고친다(테스트 있음)
│  ├─ DisengageDetector.kt       F1 해제·통과 감지
│  ├─ SegmentKey.kt              구간 ID(geohash 7 + 8방위, 둘레 8칸)
│  ├─ RiskAnnouncer.kt           F4 진입 전 안내(크루즈 작동 중, 500m 앞)
│  ├─ RiskSpeaker.kt             안내 음성(Pleos TTS SDK)
│  ├─ TripTracker.kt·TripStats.kt  한 주행(D~P) 구분·주행 요약
│  └─ Disengage.kt               이벤트 모델, DisengageRules(docs/disengage-rules.md 기준값)
├─ data/                저장소 인터페이스 + Fake(시드 데이터). 서버 구현은 황준용
├─ ui/                  화면 (길민종) — 화면은 UiState만 보고 그린다
│  ├─ UiStates.kt       S1~S5 화면 상태 계약 (이민하 ↔ 길민종 합의)
│  ├─ PreviewData.kt    미리보기 샘플
│  ├─ CruiseBreakerNav.kt  화면 목록·이동, 개발 메뉴
│  ├─ driving/          S1 예시: 신호 → ViewModel → UiState → Screen
│  └─ debug/            신호 모니터
core/                   공통 차량·내비 신호 (황준용) — core/README.md
tools/                  vconsole.sh(콘솔 조작), demo-charge.sh·demo-drive.sh(시연 시나리오), build-proposal-docx.py(기획서), sync-core.sh, vtest.sh (장호연)
```

## 담당별 첫 작업

| 담당 | 시작 지점 |
|---|---|
| 이민하 | **백그라운드 동결 실험**(PRD 5.4, 11-01 공유) → `SegmentKey.geohash` → `DisengageDetector` TODO. `FakeVehicleSignals`로 단위 테스트부터 |
| 길민종 | `ui/UiStates.kt` + `PreviewData.kt`로 S1~S5 화면을 `@Preview`에서 먼저 만들기. S1은 주행 중 규정(터치·애니메이션·여러 줄 금지) |
| 황준용 | `data/`의 인터페이스로 서버 구현(차슐랭과 같은 Supabase 프로젝트) |
| 장호연 | 위치 재생 시연 모드 + 크루즈 주입 스크립트(PRD 5.7) |

## 규칙

- **작업을 마칠 때마다 인수인계 문서 필수**: `docs/handoff/<영문-이름>.md` (양식·규칙: [docs/handoff/README.md](docs/handoff/README.md)). PR에 없으면 머지하지 않는다
- **PR 리뷰는 Claude, 머지는 장호연만** 한다(작성자·에이전트는 머지 금지). 리뷰 기준: [docs/pr-review-guide.md](docs/pr-review-guide.md)
- 각자 자기 담당 경로·브랜치에서만 작업한다
- 팀 역할·일정·규칙: [docs/team-collaboration-plan.md](docs/team-collaboration-plan.md)
- `main` 직접 push 금지 → 브랜치 `feat/...`, `fix/...`, `docs/...`(영문 소문자·하이픈) + PR
- 공통 모듈 `core/`·`tools/`·팀 문서는 두 저장소에 똑같이 들어 있다. 원본은 차슐랭 저장소이며 `core/`는 황준용만 수정한다. 동기화: 차슐랭에서 `tools/sync-core.sh ../CruiseBreaker` (확인만: `--check`)
- 퍼미션을 추가하면 `AndroidManifest.xml`과 `AppContainer`의 `CoreFeature`를 함께 맞춘다
- 이 앱은 DO를 선언한다 → 주행 중 화면 규정(PRD 5.5)을 지킨다
