# 인수인계 — 길민종 (두 앱 UI)

담당 경로: `app/src/main/java/com/hyundaiapp/*/ui/`(화면·테마). 상태 계약 `ui/UiStates.kt`는 로직 담당과 합의

## 현재 상태 (2026-11-03 기준, 매번 덮어쓰기)

- 한 일 요약: 한 줄 경고의 폭별 동등 문구(최소56sp), 위험 목록 단일 열·선택 상세·집계 기준, 최근 주행 요약·단일 목록 복귀, 설정 동의 재열람 Back·시작 콜백 미호출, 차속 ON_RESUME 재조회, STOP 중 D→P 복귀 요약, 설정 그룹·동의 반응형 정리.
- 진행 중 브랜치: `fix/ui-audit-followups` (최신 origin/main 기반, 새 PR로 검토. 머지는 장호연). 기존 알림 PR은 머지 완료.
- 다음 할 일: PR 검토 후 x86 Pleos에서 실제 차속 권한·시스템 Back/Home·내비 왕복·주행 제한 및 화면 배치를 확인.
- 담당 계약/내용 대기: 이민하: S2 오류/빈 결과·위치 기준 UiState(F05), Nav 완전 재생성에도 남는 완료 주행/요약 확인 상태(F09 잔여). 장호연/이민하: 동의 저장과 최종 시작 단계(D01), 설정 맥락 복원 정책(NAV-06). 장호연: 약관 전문(B01).
- 검증: 두 앱 일반 `assembleDebug test :app:lintDebug`, 별도 임시 Robolectric UI 28검사(차슐랭 debug10/release3, 크루즈 debug15). 수정 전 main과 동일 조건 6쌍 캡처. 임시 QA 의존성·코드는 제품에 추가하지 않음.
- 자료: `/Users/gilminjong/Dev/output/hyundai-ui-fixes-2026-11-03/`의 CHECKLIST·OWNER-CONTRACTS·REPORT·before/after·qa. 기존 dirty handoff는 before 백업과 이전 작업 기록으로 보존.
- 경계: Screen/Nav/공통 UI만 수정. core·ViewModel·data/domain·집계·서비스·서버 PR은 수정/머지하지 않음. STOP 복귀 개선을 프로세스 종료/재부팅 복원으로 주장하지 않음.

## 작업 기록 (최신이 위, 지우지 않음)

### 2026-11-03 — UI 감사 후속 구현과 전후 검증
- 구현: 한 줄 경고의 폭별 동등 문구(최소56sp), 위험 목록 단일 열·선택 상세·집계 기준, 최근 주행 요약·단일 목록 복귀, 설정 동의 재열람 Back·시작 콜백 미호출, 차속 ON_RESUME 재조회, STOP 중 D→P 복귀 요약, 설정 그룹·동의 반응형 정리.
- 결정: 기존 콜백의 의미를 유지하며 Back은 보류/재진입, 평가 건너뛰기는 결과 소비로 구분. 서버 삭제는 요청·통신 지연을 설명하고 완료를 보장하지 않음. 동의 전문은 작성하지 않음.
- 검증: 일반 빌드·단위 테스트·app lint, 추가 UI 28검사 및 6개 동일 조건 main 전후 캡처. 실제 Pleos/실차 검증은 아님.
- 잔여: 이민하: S2 오류/빈 결과·위치 기준 UiState(F05), Nav 완전 재생성에도 남는 완료 주행/요약 확인 상태(F09 잔여). 장호연/이민하: 동의 저장과 최종 시작 단계(D01), 설정 맥락 복원 정책(NAV-06). 장호연: 약관 전문(B01).
- 이전 감사의 관찰 PASS와 이번 수정 인수 검사를 구분. 이전 작업 기록·증거·작업 전 handoff 백업을 보존.


### 2026-11-03 — UI 통합 경계 보충 3조건 점검 종료(구현 없음)
- 기존 임시 VM/Robolectric 환경으로 3검사 수행, failures/errors0. 새 native surface·의존성·제품 코드·설계 계약·PR/merge 변경 없음. 기존 근거와 handoff 기록 유지.
- 새 AppContainer 같은 컨텍스트: 이전 Fake 세션1/리뷰1, 새 인스턴스0/0. 온보딩 true·77.4kWh·vehicleId 유지, 이전 인스턴스 기록1도 유지. 팀 handoff에 이미 명시된 Fake 메모리·서버 미연결 데모 한계다. 실제 OS process death/재부팅 실증으로 해석하지 않음. 인증 충전 완료 UI를 영속 서버 저장 완료와 혼동하지 않도록 경계 기록.
- 크루즈 위치 null: 판교37.4127/127.0947·10km로 저장소 호출, 거리 모두null/화면 '-'. 제목은 근처 위험 구간, 시연용 시드 데이터 고지는 있음. 현재 위치 미확인·판교 기준 설명은 없음. 현재 시연 범위와 조회 기준 안내의 공백으로 구분, 실차 오표시 사고로 일반화하지 않음.
- 목록 refresh A 느림/B 빠름: B→A 순 완료 시 최종 목록 A. 현재 즉시 Fake의 실사용 서버 장애가 아니라 비동기 저장소 연동 전 최신 요청 반영 위험으로 분류. 이번 S1 검사이며 이미 고친 S2 상세 요청 취소를 반복 수정한 것이 아님.
- 남은 주야간 판독·진동/도달성·OEM Back/Home/내비 왕복·실제 신호/권한/네트워크·서버 보존·프로세스 종료/재부팅/suspend는 결함 수와 분리해 NOT RUN/BLOCKED 체크리스트로 기록.
- 결과: `/Users/gilminjong/Dev/output/ui-integration-gap-audit-2026-11-03/REPORT.md`, manifest/qa/exports 보존. 세 조건에서 범위 종료. 본인 handoff 기록만 추가, 커밋·푸시 없음.

### 2026-11-03 — UI 상태 공백 8조건 추가 점검 종료(구현 없음)
- 범위 고정: 외부 차속 권한 변경, 리뷰 A→B, 동의만 한 뒤 재생성, 길안내 결과, 큰 글자·100%·65분, 모순 태그 조합, 완료 null 전력, 예전 A 위치 뒤 새 B. 이번 범위 이후 추가 탐색 없음.
- 재현: 외부 허용/회수 후 ON_RESUME에서 실제 차속 권한과 S5 표시 불일치(알림 helper는 갱신). 길안내 성공/실패/대기 Fake 모두 확인창 종료·대기/결과 UI 없음·두 확인에 두 요청. S4 avgKw=null은 완료 후에도 '측정 중'. 실제 Navi SDK/차량에서 이 경로 발생을 실증한 것은 아님.
- 기각: 동일 ReviewViewModel에서 A rating4/FAST 뒤 B를 보내면 B 입력은 rating=null/tags=[]로 초기화. 이전 입력 잔존 가설은 해당 조건에서 기각.
- 결정 필요: 두 명시 동의만 저장해도 Robolectric 테스트 Activity recreate 후 S2, UI onStart 0회. 실제 생산 Activity/서비스는 실행하지 않고 생산 코드의 동의 guard와 같은 테스트 호스트가 시작 가능 1회 기록. 동의 우회로 분류하지 않음. FAST+SLOW+NO_WAIT+WAIT 동시 선택 가능하나 상호 배제 계약 없음. receivedAtMs=1·navi FAILED인 A로 조회 후 fresh B만으로 갱신 안 됨, VM refresh는 B 사용. SDK의 실제 stale 유지와 허용 위치 나이는 미검증/합의 대상.
- 큰 글자: 1280×720dp·720×1280dp, Compose fontScale1.3/1.5, 대표 화면6종24장. 주요 버튼 표시·64dp 이상 영역과 100%·1시간5분 표시 PASS. 세로 경고 말줄임 및 세로 요약1.5 보조 설명 높이41/필요75px 잘림은 기존 반응형/보조 카드 문제의 조건 확장으로 중복 집계하지 않음. 완료 null 화면 1장 추가.
- 검사: 상태 관찰 8개 실패0, 렌더25장 완료. 관찰 검사 PASS를 제품 결함 해결로 해석하지 않음. summary caption의 추가 측정은 같은 조건에 한정. 원본 XML·로그·임시 코드·렌더·manifest 보존.
- 수정 경계: 권한 UI 재조회·완료 null 문구는 UI에서 최소 수정 가능. 요청 결과 계약은 이상준/황준용, 시작 단계는 장호연/이민하, 태그는 장호연/이상준, 위치 freshness는 이상준 합의 필요. core/SDK·제품 로직·PR·merge 수정 없음.
- 결과: `/Users/gilminjong/Dev/output/ui-state-gap-audit-2026-11-03/REPORT.md`. 실제 설정 앱 조작·Pleos/Navi SDK·기록 서비스·프로세스 종료/재부팅·실차 stale 위치·완료 null 발생·낭독/운전자 판독은 NOT RUN/BLOCKED. 기존 증거와 기존 handoff 기록 보존, 이번 기록은 커밋·푸시하지 않음.

### 2026-11-03 — 차량 환경의 화면 이동·복귀·상태 전환 감사(구현 없음)
- 전체 제품 화면 12개의 진입·화면 내 뒤로/닫기 위치·콜백 목적지·Android Back을 Nav/ScreenPolicy와 대조. 디버그 개발 메뉴를 실제 제품 동선으로 오인하지 않음. 각 앱 최신 app/PRD와 현재 head 사이 변경 없음.
- 임시 검사 21개 PASS(차슐랭 release Nav 7, 크루즈 실제 Nav debug=false 주입 경로 14). 차슐랭은 실제 AppContainer의 location/live/pending 상태만 합성 주입했고 차량 SDK는 시작하지 않음. 실제 차량 충전 재현이 아님.
- NAV01: 크루즈 Activity가 STOP(CREATED)인 동안 D→P가 모두 지나가면 복귀 시 S2 유지, 자동 S3 없음. Nav 재생성 시에도 P만 알면 이전 D 기억이 없음. D를 UI에서 먼저 본 뒤 STOP→P→복귀하면 요약이 뜨는 대조 조건도 확인. 이민하와 주행 종료/미확인 요약 상태를 UI 밖에서 관리하는 계약 합의 제안.
- NAV02: 차슐랭 S3 클릭 요소 0개·화면 내 뒤로 없음. 시스템 Back으로 원래 화면에 나가도 live는 유지되며 S1에 충전 화면 재진입 경로가 없음. S4도 Back은 pending을 남기고 pop하므로 앱 내 재개 경로가 없고 Nav 재생성 때 재등장. 정차 전용 명시적 복귀 + 활성/미처리 세션 진입점 제안(이상준 합의).
- NAV03: 크루즈 S4 동의 내용→S5에는 뒤로/닫기 없음. [시작하기]는 서비스 시작 콜백을 호출한 뒤 S4로 돌아가고, 시스템 Back은 호출 없이 S4 복귀. 초기 동의와 재열람 모드 분리 제안.
- NAV04: 크루즈 수동 S3의 Back은 S2, D→P 자동 S3는 root reset이라 Back이 Activity 종료. 실제 UI 검사로 확인. 정차 요약의 논리적 부모를 일관되게 정의할 것을 제안. 주행 중 내부 pop 금지·터치 요소 없음은 유지.
- 정상/예외: S2·S6 헤더 Back, 차슐랭 결과 명시적 완료/나중에의 원래 상세 복귀, STOP 중 충전 완료 후 S4 복귀, 약관창 Back=창 닫기, 철회창 Back=취소, 미동의 D→P=동의 화면, UX 미확인=터치0/허용 후 이전 설정 회복 확인. 목록 root·첫 동의 root·DO 화면에 뒤로 아이콘이 없다는 이유만으로 결함 판정하지 않음.
- 기획 합의: 크루즈 설정에서 출발해 완전한 D→P를 거치면 요약 후 목록으로 reset되어 이전 설정 맥락이 사라짐. 자동 요약 자체는 PRD 요구이므로 확정 결함과 구분. 같은 Activity의 단순 정지/복귀는 설정을 유지하지만 remember stack의 Nav 재생성 복원은 별도 계약 필요.
- 실제 Pleos 시스템 Back/Home·차량 내비 왕복·실제 UXR 단절·프로세스 종료/시동 suspend/재부팅은 NOT RUN/BLOCKED. Android Back dispatcher와 ActivityScenario 결과를 실제 차량 조작 증거로 주장하지 않음.
- 자료: `/Users/gilminjong/Dev/output/vehicle-navigation-audit-2026-11-03/`의 navigation-matrix.json, manifest.json, exports/, qa/. 제품 코드·타 담당 파일·PR·merge 변경 없음. 본인 handoff 기록만 추가(커밋·푸시 없음).

### 2026-11-03 — 시각 통일성·디자인 마감 재감사(구현 없음)
- 사용자 정정에 따라 기능 오류 목록을 반복하지 않고 두 앱 전체 화면을 나란히 비교. 최신 app/PRD는 origin/main과 같으며 기존 PR 후속 head 유지.
- 실제 코드 Robolectric 렌더 검사 31개 완료(차슐랭 21+모달 1, 크루즈 7+모달 2), 사용 가능한 화면/모달 PNG 32개 보존. Activity 창과 별도인 AlertDialog는 해당 창을 직접 캡처해 비교. 실제 Pleos 화면은 아님.
- 선정 1: 차슐랭 S1/S2 모두 데이터 없는 오류인데 S1은 흰 28sp 제목+파란 Primary 재시도, S2는 회색 28sp 안내+Secondary 재시도이며 세로 배치도 다름. 공통 EmptyState(제목28/설명20/간격16/Primary76)를 제안. 캐시 상세의 갱신 재시도는 Secondary 예외 유지. 최소 범위: 두 Screen과 공통 부품, VM 변경 없음.
- 선정 2: 길안내 모달 취소는 22sp Regular·49dp, 확인은 24sp Bold·52dp. 철회 모달은 취소/확인 모두 24sp Bold·64dp. 약관 닫기는 24sp Bold·52dp이며 배경도 SurfaceVariant로 다름(길안내/철회는 Surface). 공통 모달 frame과 액션64, 주 행동24 Bold/보조22 Regular 규칙 제안. 파괴 확인 Warning 색, 내용 길이에 따른 너비 차이는 유지.
- 선정 3: 차슐랭 S6 철회는 흰 일반 Secondary 헤더 버튼, 크루즈 S4는 Warning 제목+결과 설명 카드. 위치는 각 정보 구조에 맞게 유지하되 파괴 행동의 색·설명 문법을 공통화 제안. 최소 범위: HistoryScreen·SettingsScreen·공통 위험 액션 표현, 철회 로직 변경 없음.
- 공통 토큰(Theme/CarComponents/CarIcons)은 패키지·테마명·주석 제외 실제 코드 동일. 여백32/40, 카드 안쪽20/24/32, 상태 pill/별점 색, 주행 경고 크기 차이는 역할을 구분해 결함 수에 넣지 않음. 화면 frame/카드/버튼 variant를 명명하는 작업은 재발 방지 제안으로 구분.
- 첫 수정 제안: 동일 역할이 가장 명확하고 UI만으로 가능한 S1/S2 빈 오류 화면부터 맞추기 → 공통 모달 → 철회 진입 표현. 구현·merge·PR 변경은 하지 않음. 이 기록만 본인 handoff에 추가(커밋·푸시 없음).
- 결과: `/Users/gilminjong/Dev/output/ui-consistency-audit-2026-11-03/`의 앱별 contact sheet, `role-comparison.png`, `dialog-comparison.png`, exports/qa/manifest. 기존 기능 감사·렌더 파일은 보존.

### 2026-11-03 — 사용자 과업 기준 UI/UX 감사·수정 계획(구현 없음)
- 범위: 최신 PRD·UiState·담당표·본인 handoff·실제 UI/VM/서비스 코드 대조. 현재 app/PRD는 origin/main과 같고, PR #7/#18 후속 head도 보존. 코드·운영 설정·PR·merge 작업 없음. 문서는 본인 handoff에만 기록(커밋·푸시 없음).
- 실행: 임시 Robolectric SDK34/mdpi 화면 16개 렌더, 기존 권한/동의/화면 흐름 UI 검사 19개 PASS. 관찰 검사 2개 PASS로 결함 재현(차슐랭 위치 미수신 60초 뒤에도 loading, 크루즈 IOException 뒤 빈 목록). 추가 사용자 인수 검사 3개 FAIL로 재현([나중에] 높이 1dp, 1024dp 경고 말줄임, 600dp 위험 구간 통과 수 4줄). 의도한 인수 기준 실패이며 제품 코드 빌드 오류는 아님.
- 우선 계획: ① 크루즈 경고 배너의 거리·경고 의미 보존(한 줄·현재 테마 배너 최소 56sp 원칙 유지, 아주 좁은 창 축약은 장호연과 문구 합의) ② 이민하와 S2 오류/빈 상태/재시도 계약 합의 ③ 이상준과 S1 위치 대기 기한·오류/재시도 합의 ④ 차슐랭 S4 입력 스크롤+완료/나중에 고정 및 별점 폭 계산 ⑤ 크루즈 S2 좁은 폭 단일 열/선택 상세 재배치. 상태 계약·VM·core는 담당 합의 전 수정하지 않음.
- 제외: 자동 기록은 현재 S4 설명("꺼도 앱을 열면 기록")과 MainActivity/BootReceiver 동작이 일치하므로 확정 결함으로 보고하지 않음. 기존 알림·S2 상세 재시도·주행/정차 전환 수정도 새 결함으로 반복 보고하지 않음.
- 한계: 실제 Pleos, 실제 Car 서비스 단절/재연결, 자동 기록 끔/재부팅, 실제 차속 권한 시스템 창, 글자 배율 확대, 실제 서버 통합은 NOT RUN/BLOCKED. Mac Apple Silicon에서 대상 x86 Pleos 실행 불가.
- 증거: `/Users/gilminjong/Dev/output/ui-ux-audit-2026-11-03/exports/`(PNG·텍스트 배치), `qa/`(검사 결과 XML·로그·임시 감사 코드), `manifest.json`(검증 범위·소스 head). 모든 화면은 실제 앱 코드를 합성 상태로 그린 Robolectric 렌더이며 실제 Pleos 스크린샷이 아님.

### 2026-11-03 — 설정에서 알림 허용 후 미확인 횟수 초기화
- PR #7/#18 최신 리뷰의 선택 제안 반영: 설정에서 허용하고 돌아와도 미확인 횟수 2가 남던 동작을 재현(임시 Robolectric 검사에서 기대 0, 실제 2로 실패).
- `refresh()`에서 허용을 확인하면 횟수를 메모리·UI prefs 모두 0으로 초기화. 기존 실제 선택 이력은 유지하고, 자동 설정 실행·기록 동작·주행 제한은 바꾸지 않음.
- 검증: 두 앱 `assembleDebug test :app:lintDebug` 통과. 임시 Robolectric 권한 UI 검사 각 3개 통과. 미확인 2회 → 설정 허용 → 복귀 시 저장 횟수 0 → 다시 끄고 복귀 → 새 권한 요청 호출 및 횟수 1부터 재시작을 확인. 실제 Pleos 확인은 남음.
- 추가 PR·이슈 점검: 시연 도구 #5/#15는 머지 완료. 새 차슐랭 #8은 이상준 삭제 재시도, 크루즈 #19~#21은 이민하/황준용 업로드·철회 순서·TTS 수명 이슈이며 UI 요청은 없음.

### 2026-11-03 — 전체 PR·이슈 재점검 및 알림 예외 후속
- 두 저장소 PR 17개(열림·머지 전체), 이슈 6개와 리뷰·인수인계를 재확인. 담당 #9·#13과 이전 UI PR은 완료. 현재 남은 타 담당: 시연 도구 PR #5/#15 handoff 충돌·리허설(장호연), #12 실제 서버 삭제(황준용), 차슐랭 삭제 재시도(이상준), 약관 전문 확정 후 UI 문구 교체.
- 이전 UI PR 최종 리뷰의 알림 이력 소실 예외를 두 앱에 동일 보완. 미확인 결과가 반복되면 명시적 설정 버튼 제공, 첫 창 미선택 후 재요청과 진행 중 라벨은 유지. 카운트 영속화·화면 재진입·설정 자동 실행 없음도 확인 대상.
- `UxRestrictionSource` 주입 경계에 임시 Robolectric 생명주기 검증 용도 주석 추가(설정 복귀·단절·dispose). 동작 변경 없음.
- 검증: JDK 17 `assembleDebug test :app:lintDebug` 통과. 새 회귀 테스트 2개(누락 이력→2회 결과 후 설정, 확정 결과 초기화)를 각 앱에 추가. 임시 Robolectric UI 검사 각 3개 통과(기존 거부→설정·복귀, 첫 창 닫기→재요청, 미확인 결과 횟수 저장·화면 재진입 후 설정 버튼·탭 전 설정 실행 없음). 실제 Pleos 검증은 수행하지 않음.

### 2026-11-03 — 추가 선택 리뷰 반영
- 새 리뷰: 두 PR 모두 머지 가능. 크루즈 `208e175`는 팀원이 실제 x86 Pleos 릴리스 시연 흐름 확인까지 통과(PR #16 댓글 참조).
- 알림: 요청 전에 이력을 저장하지 않고, 허용 또는 rationale로 선택 결과가 확인된 뒤 기록. 첫 창 미선택 닫기는 다시 요청 가능. 요청 진행 상태를 저장해 시스템 창 뒤의 버튼이 "알림 설정"으로 바뀌지 않게 함. 최초 취소·실제 반복 거부·진행 중 라벨 회귀 테스트 각 3개 추가(두 앱 동일).
- UX 연결: ON_STOP에서 연결을 끊고 null로 만들던 처리를 제거. composition 수명 동안 구독을 유지하고 ON_START에 플랫폼 현재 값 재조회. 실제 단절·조회 예외는 여전히 null로 S1 차단, dispose에서 리소스 해제. 기어·주행 제한을 완화하지 않음.
- 검증: 임시 Robolectric에서 설정 화면 왕복 시 연결 해제 0회·복귀 시 주행 제한 재조회·실제 미확인 신호 S1·dispose 해제 1회 확인. app debug/release 각각 62개 테스트, 빌드·앱 lint 확인. 자동 기록 끔/재부팅·실제 Car 단절·이번 연결 유지의 Pleos 재확인은 남음.


### 2026-11-03 — 선택 리뷰 반영
- 최종 확인: `assembleDebug test :app:lintDebug` 통과. app debug/release 각각 59개, core debug 17/release 3개, 실패 0. 임시 UI 검사 8개 통과.
- 한 일: 리뷰의 선택 제안도 반영. 알림 영구 거부/재요청 불가 시 명시적 버튼으로 설정 진입, 요청 이력과 rationale로 최초 요청 구분, 복귀 시 실제 권한 재확인. UI 설정 이력은 단순 요청 여부만 저장하며 서비스·동의 계약은 바꾸지 않음.
- S5/정차 화면: 플랫폼 UX 제한을 동의 전부터 확인해 제한 중 터치 화면을 숨김. 정차 허용이 확인되면 기어가 없어도 설정·철회 접근. 비동기 Car 연결, 리스너 해제, 생명주기 복귀, 지난 연결 콜백 차단. 새 퍼미션·core 변경 없음.
- 검증: 권한 분기 4개·플랫폼 제한 정책 2개 회귀 테스트 추가. 임시 Robolectric에서 기어 미수신+플랫폼 정차 허용 → 설정/철회, 동의 전 플랫폼 제한/미확인 → 클릭 요소 0개 확인. 실제 Car 서비스 연결은 Pleos PC 확인 필요.


### 2026-11-03 — 텍스트 줄바꿈 정리
- 문제: 차슐랭 실패 안내의 "요." 한 줄 고립, 좁은 동의 카드의 "필수"·"선택"·권한 설명 및 긴 시작 버튼 문구 잘림. 크루즈 S5 제목도 좁은 폭에서 "곳을"만 한 줄에 남음.
- 한 일: 동의 카드의 500dp 미만 본문 폭에서 버튼을 아래로 배치, 공통 시작 버튼/비활성 사유 분리. 공통 테마의 한국어 로캘·제목/본문 줄바꿈 설정을 두 앱에 동일 반영. 앱별 소개·실패 설명을 의미 단위로 나눔.
- 검증: 기본 화면 22개와 추가 좁은 화면 3개(가로 1024×600, 세로 600×1024)를 다시 렌더해 실제 줄 배치 확인. 가로·세로 PDF 23쪽도 줄바꿈 정리본으로 재생성. 임시 렌더 설정·샘플 데이터는 커밋하지 않음.
- 바꾼 파일: `ui/components/CarComponents.kt`, `ui/theme/Theme.kt`, `ui/onboarding/OnboardingScreen.kt`, 본 문서.


### 2026-11-03 — 릴리스 화면 흐름 연결
- 문제: 디버그 개발 메뉴 외에는 S2·S4 진입 경로가 없어 릴리스 동의 후 S1에 갇힘. PRD 시나리오·5.5의 정차 후 S3 요약도 자동 표시되지 않음.
- 한 일: 정차용 기본 화면을 S2로 연결, D 후 P에서 S3 한 번 자동 표시, 요약에서 S2로 돌아가기. S1은 기존처럼 UiState만 받으며 터치 요소 없음. UI Nav에서 기존 `VehicleSignals.drive`만 읽어 주행/기어 미수신 시 정차 화면과 열린 약관·철회 창을 숨김. 기록·판정·서비스·core는 변경하지 않음.
- 검증: `assembleDebug test :app:lintDebug` 통과. 앱 테스트 53개(debug/release 각각), core debug 17/release 3. 회귀 테스트 6개(정차/권한 없음, D·R·N·미수신, P·차속 모순, 동의 전 첫 화면, D→N→P 한 번 요약, 철회 초기화). 임시 Robolectric 실제 Nav 테스트 3개 통과(릴리스 전체 흐름·철회 창 도중 주행·디버그 메뉴 중 주행), 주행 중 클릭 요소 0개 확인. 요약 렌더 직접 확인. Pleos 에뮬레이터·실차·CarUxRestrictions 연동 검증을 대신하지 않음.
- 바꾼 파일: `ui/{CruiseBreakerNav,InitialScreen,ScreenPolicy}.kt`, `ui/{InitialScreenTest,ScreenPolicyTest}`, 본 문서. Nav는 상태·콜백 의존성을 분리하여 가짜 신호로도 전체 흐름을 검증할 수 있게 함.

### 2026-11-03
- 검증: `assembleDebug test :app:lintDebug` 통과. 앱 단위 테스트 47개(debug/release 각각), core debug 17개/release 3개. 앱 lint 오류 0(기존 경고 12개). 임시 Gradle init+Robolectric 화면 확인 4개 통과: S4 자동 기록 토글·철회 클릭, S5 선택 권한 없이 시작/알림 콜백, S2 브레이크 막대 없음, S1 배너 한 줄·말줄임 없음. 1280×720 PNG를 직접 확인. 임시 테스트/의존성은 커밋하지 않음. Pleos 에뮬레이터 실행은 하지 않음.
- 한 일: GitHub 열린 PR·이슈 및 PR #2·#5·#10 리뷰/인수인계 확인. 길민종 할당 #9·#13 구현. `core/`·서버·판정 규칙은 수정하지 않음.
- 결정: 알림 권한은 선택이며 S5 설명 카드 버튼에서만 요청(API 33 미만은 추가 요청 없음). 철회 후 릴리스는 S5, 디버그는 개발 메뉴. 안내 거리·브레이크 비율은 화면 계약에서 제거(이민하의 기존 요청에 근거).
- 바꾼 파일: `MainActivity.kt`, `ui/{CruiseBreakerNav,InitialScreen,UiStates,PreviewData}.kt`, `ui/components/NotificationPermission.kt`, `ui/{onboarding/OnboardingScreen,risk/RiskListScreen,risk/RiskListViewModel,settings/SettingsScreen,settings/SettingsViewModel}.kt`, UI 단위 테스트, 본 문서.


### 2026-11-01
- 한 일: 화면 PR #1 머지 후 리뷰 후속(브랜치 `fix/ui-review-followups`). S5 왼쪽에 자동 기록 안내 "시동을 걸면 앱을 열지 않아도 주행을 자동으로 기록합니다." 추가. `SelectableButton`에 `role` 파라미터를 두고 S4 안내 거리는 `Role.RadioButton`(차슐랭과 공통 부품 같이 수정). Robolectric 렌더로 S4·S5 확인(설정은 커밋 안 함). `assembleDebug`·`test` 통과.
- 결정한 것과 이유: S4 자동 기록 토글은 `SettingsUiState` 필드가 없어 이번에 안 넣음(`UiStates.kt`는 이민하와 합의 대상)
- 바꾼 파일: `ui/onboarding/OnboardingScreen.kt`, `ui/settings/SettingsScreen.kt`, `ui/components/CarComponents.kt`, `docs/handoff/gil-minjong.md`


### 2026-10-31
- 한 일(2): 와이어프레임대로 코드 구현(브랜치 `feat/ui-screens`). 공통 테마(`CarColors`·`CarDimens`·글자 크기), 공통 부품(`CarComponents.kt`·`CarIcons.kt`), 크루즈브레이커 S1~S5 화면, `*Nav.kt` Placeholder 교체. `assembleDebug`·`test` 통과, Robolectric 렌더로 화면 확인(렌더 설정은 커밋 안 함).
- 결정한 것과 이유(2):
  - 두 앱이 같은 부품 파일을 복사해 쓴다: `core/`는 황준용 담당이라 공통 UI 모듈을 만들지 않음
  - ViewModel이 없는 화면은 `*Nav.kt`에서 임시 상태를 들고 콜백을 연결: 개발 메뉴에서 바로 눌러 볼 수 있게
- 바꾼 파일(2): `ui/theme/Theme.kt`, `ui/components/`(새로), `ui/*Nav.kt`, 각 화면 폴더(`ui/<화면>/`), `docs/handoff/gil-minjong.md`
- 한 일: 두 앱 공통 테마 + 전체 화면 와이어프레임 16장(크루즈브레이커 6장, 차슐랭 9장, 테마 1장)을 Claude Design 캔버스로 만듦(https://claude.ai/artifact/9vidSJtHrxzCqtEcjJjVTf). 코드 변경 없음.
- 결정한 것과 이유:
  - 1280×720 가로 기준, 어두운 배경 + 큰 글씨(차량 화면 가이드 2.3: 큰 터치 영역·높은 대비·텍스트 최소화)
  - 강조색은 파랑·주황 둘만: 색약에서도 밝기로 구분되고, 두 앱이 같은 의미(파랑=정보·인증, 주황=위험·실패)로 쓴다
  - 크루즈브레이커 S1 배너는 주황 바탕 한 줄 64sp, 아이콘 1개. 평소에는 "기록 중" 작은 표시만
  - 차슐랭 S4는 별 96px·태그 72px 버튼, 키보드 없음. 세로 화면은 좌우 2단 → 위아래 1단 규칙
- 바꾼 파일: `docs/handoff/gil-minjong.md`

### 2026-10-30
- 장호연: 파일 생성. 첫 작업은 README "담당별 첫 작업" 기준으로 적어 둠.
