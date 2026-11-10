# 크롱 (CRONG)

> Cruise Risk Observation & Navigation Guide. 11-10 크루즈브레이커(Cruise Breaker)에서 이름을 바꿨다.

다른 차량들이 크루즈 컨트롤을 해제하는 지점을 모아, 크루즈 해제가 반복되는 구간(해제 다발 구간)을 진입 전에 알려 주는 Pleos Connect 앱. 팀 삼각고래 · 일반 부문.

- 기획: [PRD.md](PRD.md)
- 서버·인증·데이터 흐름·운영: [공통 서버 설명서](docs/server-overview.md)
- 핵심 신호: Vehicle SDK `DrivingMode` 크루즈 상태 + AAOS 차속(해제 조건 60km/h) + NaviHelper 위치·진행 방향. 안내 음성은 Pleos TTS SDK
- 판정 규칙 v2(11-01, 정체 필터·해제 유형 없앰, 안내 500m 고정): [docs/disengage-rules.md](docs/disengage-rules.md)
- 실측 메모: [docs/vehicle-data-findings.md](docs/vehicle-data-findings.md) 6.2.1 (크루즈 해제 값은 에뮬레이터로 재현 불가, 실차 값 문의 필요)

## 심사용 안내 (GitHub 제출물)

| 항목 | 내용 |
|---|---|
| 앱 이름 | 크롱 (CRONG) — 팀 삼각고래, 일반 부문 |
| 한 줄 소개 | 다른 차량들이 크루즈 컨트롤을 해제한 지점을 모아, 해제가 반복되는 구간을 진입 500m 전에 배너와 음성으로 알려 주는 Pleos Connect 앱 |
| APK 위치 | `apk/SamgakGorae_CRONG_release.apk` (서명된 release 빌드, 시연 데이터 모드·시연 판정 기준, 11-09 빌드) |
| 패키지 | `com.hyundaiapp.crong` |
| 기획서 | [docs/proposal.md](docs/proposal.md) (접수양식 02 항목과 같은 내용), 제품 요구사항 [PRD.md](PRD.md), 판정 규칙 [docs/disengage-rules.md](docs/disengage-rules.md) |
| 시연 순서 | [docs/demo-scenario.md](docs/demo-scenario.md) |

### 실행 방법

1. Pleos Connect 에뮬레이터 v2.1.0을 켠다(시스템 이미지 `Pleos Connect v2.1.0 APIs`, AVD 프로필 `Pleos_Connect_v2_0` 2560×1440). 에뮬레이터 사용자 ID는 10이다.
2. APK를 설치한다: `adb install -r apk/SamgakGorae_CRONG_release.apk`
3. 차속 권한을 준다: `adb shell pm grant --user 10 com.hyundaiapp.crong android.car.permission.CAR_SPEED` (앱이 처음 실행될 때 묻기도 한다 → 허용. 아래 시연 스크립트가 같은 명령을 실행한다)
4. 앱을 켜면 **S5 첫 실행 동의** → [동의] 2개·[시작하기] → **S2 근처 해제 다발 구간**. 처음부터 다시 보려면 `adb shell pm clear --user 10 com.hyundaiapp.crong`
5. 주행 장면은 에뮬레이터 Virtual Console로 차량 값을 주입해 재현한다(Git Bash, 저장소 루트):
   - `bash tools/vconsole.sh up` (포트 포워딩·하트비트 시작)
   - `bash tools/demo-drive.sh` → 출발(D, 100 km/h) → 크루즈 작동 → 400m 앞 시드 구간 "판교IC 진입로" **진입 전 안내(S1 배너 + 음성)** → 크루즈 해제 → 해제 기록·업로드 → 저속(40 km/h) 해제는 기록하지 않음 → 정차 후 **S3 주행 요약**
   - 끝나면 `bash tools/vconsole.sh down`. 기록 확인은 `adb logcat -s CruiseRecord`
6. 시연 고지: 크루즈 작동·해제, 차속, 기어는 콘솔로 주입하고(에뮬레이터는 실차 크루즈 해제 값을 내지 않는다), 해제 다발 구간은 시연용 시드 데이터다. 영상·PT에서도 같은 내용을 밝힌다.

### 테스트 환경 및 필요한 설정

- 확인한 환경: Windows 11, Android Studio 2025.3.2(번들 JDK 21; JDK 17 이상), Android Gradle Plugin 8.9.2, Kotlin 2.1.20, Android Emulator 36.4, Pleos Connect 에뮬레이터 v2.1.0(API 34 `pleos_car_emulator`)
- 서버: Supabase 시연 프로젝트(차슐랭과 같은 프로젝트). 설정값은 APK에 들어 있어 **추가 설정 없이** 실행되며, 인터넷 연결이 필요하다. 서버에 닿지 않으면 기록은 앱 안의 대기열에 남았다가 다시 보낸다. 설정값·동작은 [docs/server-overview.md](docs/server-overview.md)
- 권한: `pleos.car.permission.CAR_DRIVE`(크루즈 상태), `android.car.permission.CAR_SPEED`(차속, 위 3번처럼 부여), `pleos.car.permission.NAVI_ROUTE`(위치·진행 방향), `pleos.car.permission.TTS_SERVICE`(안내 음성), `INTERNET`, `FOREGROUND_SERVICE`(`specialUse`, 해제 기록 서비스), `RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS`
- 주행 중 화면: DO(`distractionOptimized`)를 선언해 주행 중에도 S1 안내 화면을 보여 준다. S1은 터치 요소·애니메이션·여러 줄 텍스트 없이 만든다(PRD 5.5)
- 소스에서 빌드: 저장소 루트의 [`supabase-public.properties`](supabase-public.properties) 두 줄을 `local.properties`에 복사한 뒤 `./gradlew assembleDebug` / `./gradlew installDebug`. 복사하지 않으면 서버 없이 Fake 저장소로 돌아가며 화면 흐름은 같다. 단위 테스트는 `./gradlew test`(core·app). release 빌드는 `bash tools/build-release.sh`(서명 키 필요)
- debug 빌드와 제출 release 빌드는 화면 흐름·판정 기준이 같다(둘 다 시연 데이터 모드). debug에만 S2 제목을 길게 누르면 열리는 개발 메뉴(신호 모니터)가 있다

### 오픈소스 고지

| 구성 요소 | 라이선스 |
|---|---|
| AndroidX (Core KTX, Jetpack Compose BOM·Material 3, Activity Compose, Lifecycle) | Apache License 2.0 |
| Kotlin 2.1.20, kotlinx-coroutines 1.9.0 | Apache License 2.0 |
| Pleos Playground SDK (Vehicle 2.3.2, NaviHelper 3.0.5, TextToSpeech 3.3.1) | 현대자동차그룹 Pleos Connect 개발 환경 제공, 공모전·개발자 약관에 따름 |
| 테스트 전용: JUnit 4.13.2 (EPL 1.0), org.json 20240303 (Public Domain) | — |

앱 소스 코드와 문서의 권리는 팀 삼각고래에 있으며 공모전 약관(지식재산권 조항)을 따른다. 위 구성 요소 외에 다른 서드파티 코드는 포함하지 않았다.

## 팀 개발 안내

팀 내부 개발 절차(처음 시작하기, 코드 구조, 담당별 첫 작업, 규칙)는 [docs/dev-guide.md](docs/dev-guide.md)에 있다.
