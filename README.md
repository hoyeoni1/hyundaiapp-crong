# 크루즈브레이커 (Cruise Breaker)

다른 차량들이 크루즈 컨트롤을 해제하는 지점을 모아, 운전자 개입이 반복되는 위험 구간을 진입 전에 알려 주는 Pleos Connect 앱.

- 핵심 신호: Vehicle SDK `DrivingMode` 크루즈 상태(`CruiseControlState`), 브레이크, 조향각
- 실측 메모: [docs/vehicle-data-findings.md](docs/vehicle-data-findings.md) 6.2.1 (크루즈 해제 값은 에뮬레이터로 재현 불가, 실차 값 문의 필요)

## 개발

- 팀 역할·일정·규칙: [docs/team-collaboration-plan.md](docs/team-collaboration-plan.md)
- 공통 모듈 `core/`·`tools/`·팀 문서는 두 앱 저장소에 똑같이 들어 있다. 원본은 차슐랭 저장소이며 황준용만 수정한다. 동기화: 차슐랭에서 `tools/sync-core.sh ../CruiseBreaker` (확인만: `--check`)
- 빌드: `./gradlew assembleDebug` (JDK 17, `local.properties`에 `sdk.dir`)
