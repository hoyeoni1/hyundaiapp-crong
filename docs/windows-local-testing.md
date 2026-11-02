# Windows 로컬 SDK 테스트

서버·계정·유료 서비스 없이 debug 앱, Pleos AVD, Virtual Console을 사용한다. 진단 화면은 core/src/debug에 있어 release APK에 포함되지 않는다. 실제 차량의 도로 주행용 화면이 아니다.

## 2026-11-02 원격 작업 통합 후 구분

- 제품 앱은 `com.hyundaiapp.core.VehicleSignals`의 evBattery/drive/cruise/location/route 계약을 사용한다. 원격의 UI·감지기·안내·동의/서비스 로직을 유지했다.
- 기존 로컬 snapshot 진단은 `com.hyundaiapp.core.probe` 패키지의 debug 전용 SDK probe로 보존했다. 스크립트는 새 Activity 경로를 사용하므로 기존 수동 adb 명령도 `com.hyundaiapp.core.probe.SdkDiagnosticsActivity`로 바꾼다.
- 이 문서의 diagnostics/smoke는 독립 probe의 SDK 수신 검사다. 제품 앱의 동의·감지·업로드 흐름을 검증한 것으로 해석하지 않는다. 제품 앱은 개발 메뉴의 신호 모니터와 앱 단위 테스트로 별도 검증한다.
- UI 라이브러리 요구사항 때문에 compileSdk는 35다. 설치 패키지에 platforms;android-35가 추가된다. 실행용 Pleos 이미지는 기존 API 34 그대로다.
- 통합 브랜치: 두 저장소 모두 codex/integrate-sdk-20261102. 원격 push/PR 머지는 별도이며 이번 작업에서 수행하지 않는다.
## 이 PC의 개발 환경

- 도구 위치: `%LOCALAPPDATA%/hyundai-dev`
- Java: `java/` 아래 Temurin JDK 17
- SDK: `android-sdk/`
- 가상 기기 이름: `Pleos_Local_Test`
- 각 저장소 local.properties가 이 SDK를 가리킨다. 이 파일은 Git에 포함되지 않는다.
- 기존 Android Studio로 각 저장소를 열어도 된다. SDK 경로는 위 경로, Gradle JDK는 위 JDK 17을 선택한다. 별도 계정 로그인은 필요 없다.
- 프로젝트 Gradle Wrapper를 사용한다. PowerShell 스크립트는 실행하는 프로세스 안에서만 JAVA_HOME/ANDROID_HOME을 설정한다.

## 새 Windows 11 PC에서 처음 구축하기

아래 설치 명령은 아직 개발 도구를 설치하지 않은 PC 기준이다. 기존 설치가 있으면 같은 폴더에 덮어쓰지 말고 기존 경로를 확인한다. 현재 tools/windows-test.ps1은 위의 전용 폴더 구조를 사용하므로, 다른 설치 경로를 사용할 때는 스크립트도 조정해야 한다.

### 1. PC 및 저장소 준비

- Windows 11 x64, BIOS/UEFI의 CPU 가상화 활성화. RAM은 16GB 이상을 기준으로 하고 여유 디스크 약 40GB 이상을 확보하는 것을 권한다. 설치한 Pleos 이미지의 system.img만 약 18GB다.
- Windows 기능의 Windows Hypervisor Platform(WHPX)을 확인한다. 비활성화 상태라면 관리자 권한으로 활성화하고 필요 시 재부팅한다. 이 PC에서는 이미 사용 가능하여 시스템 기능을 변경하지 않았다.
- Git과 [Android Studio](https://developer.android.com/studio)를 설치한다. IDE 회원가입·로그인은 필요 없다. 명령행 테스트 자체는 아래 JDK·SDK만으로 실행된다.
- 차슐랭과 크루즈브레이커 저장소를 같은 부모 폴더 아래 받는다. 각 저장소만으로도 빌드되며, 두 저장소를 나란히 두는 것은 공통 모듈 동기화를 위한 구성이다.

### 2. JDK 17과 Android 명령행 도구 설치

PowerShell에서 실행한다. JDK URL은 최신 Temurin 17을 받으며 이 PC에서 검증한 세부 버전은 아래 결과 절에 기록했다. Android 명령행 도구는 실제 설치에 사용한 19.0을 지정한다.

```powershell
$devRoot = Join-Path $env:LOCALAPPDATA 'hyundai-dev'
$sdkRoot = Join-Path $devRoot 'android-sdk'
New-Item -ItemType Directory -Force "$devRoot/downloads", "$sdkRoot/cmdline-tools" | Out-Null

Invoke-WebRequest 'https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse' -OutFile "$devRoot/downloads/jdk.zip"
Expand-Archive -LiteralPath "$devRoot/downloads/jdk.zip" -DestinationPath "$devRoot/java"

Invoke-WebRequest 'https://dl.google.com/android/repository/commandlinetools-win-13114758_latest.zip' -OutFile "$devRoot/downloads/cmdline-tools.zip"
Expand-Archive -LiteralPath "$devRoot/downloads/cmdline-tools.zip" -DestinationPath "$devRoot/downloads/cmdline-extract"
Copy-Item -LiteralPath "$devRoot/downloads/cmdline-extract/cmdline-tools" -Destination "$sdkRoot/cmdline-tools/19.0" -Recurse

$env:JAVA_HOME = (Get-ChildItem "$devRoot/java" -Directory | Select-Object -First 1).FullName
$env:ANDROID_HOME = $sdkRoot
$env:ANDROID_SDK_ROOT = $sdkRoot
& "$env:JAVA_HOME/bin/java.exe" -version
& "$sdkRoot/cmdline-tools/19.0/bin/sdkmanager.bat" --version
```

Java 17과 sdkmanager 19.0이 표시되면 다음 단계로 진행한다. 환경변수는 현재 PowerShell 세션에만 적용되며, 새 터미널에서는 다시 설정하거나 windows-test.ps1을 사용한다.

### 3. Pleos 저장소 등록과 SDK 설치

Android Studio의 Tools → SDK Manager에서 Android SDK Location을 위 sdkRoot로 지정한다. SDK Update Sites에 다음 항목을 추가한다.

- 이름: Pleos Connect System Image
- URL: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml`

IDE 없이 새 환경에서 등록하려면 다음 파일을 생성할 수 있다. 기존 repositories.cfg가 있으면 이 명령은 덮어쓰지 않으며, SDK Manager에서 기존 목록에 Pleos를 추가한다.

```powershell
$androidUserDirectory = Join-Path $env:USERPROFILE '.android'
New-Item -ItemType Directory -Force $androidUserDirectory | Out-Null
$repositoryConfig = Join-Path $androidUserDirectory 'repositories.cfg'
if (Test-Path -LiteralPath $repositoryConfig) {
    Write-Output '기존 repositories.cfg 유지: SDK Manager에서 Pleos 항목을 추가하세요.'
} else {
    @'
count=1
disp00=Pleos Connect System Image
enabled00=true
src00=https\://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml
'@ | Set-Content -Encoding ASCII -LiteralPath $repositoryConfig
}

[IO.File]::WriteAllLines("$devRoot/downloads/packages.txt", @(
    'platform-tools',
    'platforms;android-34',
    'platforms;android-35',
    'build-tools;35.0.0',
    'emulator',
    'system-images;android-34;pleos_car_emulator;x86_64'
))
& "$sdkRoot/cmdline-tools/19.0/bin/sdkmanager.bat" "--sdk_root=$sdkRoot" "--package_file=$devRoot/downloads/packages.txt"
```

표시되는 Android/42dot 라이선스 내용을 확인하고 동의하는 본인이 입력한다. 자동으로 yes를 전달하는 절차는 넣지 않았다. Pleos는 본 작업 시점에 무상이지만 향후 유료화 통지가 있다면 다시 판단한다. 회원가입·결제가 요구되는 경우에도 진행 전에 확인한다.

실제 설치에서는 최신 명령행 도구의 sdkmanager 호환 래퍼가 세미콜론을 포함한 패키지명을 잘못 분리하는 현상이 있었다. 위처럼 19.0과 package_file을 사용해 재현성을 확보했다. XML 버전 4 관련 경고가 나더라도 설치 완료 후 아래 파일이 존재하는지 확인한다.

```powershell
Test-Path "$sdkRoot/platforms/android-35/android.jar"
Test-Path "$sdkRoot/platforms/android-35/optional/android.car.jar"
Test-Path "$sdkRoot/build-tools/35.0.0/aapt2.exe"
Test-Path "$sdkRoot/system-images/android-34/pleos_car_emulator/x86_64/package.xml"
& "$sdkRoot/emulator/emulator.exe" -accel-check
```

파일 확인이 모두 True이고 가속기가 사용 가능해야 한다. 설치 중인 프로세스가 살아 있다면 출력이 잠시 없어도 바로 재시작하지 않는다. 이미지 다운로드는 약 4.1GB이며 압축 해제에 추가 시간이 걸린다.

### 4. 전용 가상 기기 생성

```powershell
& "$sdkRoot/cmdline-tools/19.0/bin/avdmanager.bat" list device
& "$sdkRoot/cmdline-tools/19.0/bin/avdmanager.bat" create avd -n Pleos_Local_Test -k 'system-images;android-34;pleos_car_emulator;x86_64' -d Pleos_Connect_v2_0
```

커스텀 하드웨어 프로필 생성 여부를 묻는 경우 no를 선택한다. 같은 이름의 AVD가 이미 있으면 강제로 덮어쓰지 않는다. Android Studio Device Manager에서 해당 AVD를 편집하거나 `%USERPROFILE%/.android/avd/Pleos_Local_Test.avd/config.ini`의 기존 키를 다음과 같이 변경한다. 동일한 키를 중복 추가하지 않는다.

```ini
hw.ramSize=4096
hw.cpu.ncore=4
hw.gpu.enabled=yes
hw.gpu.mode=auto
fastboot.forceColdBoot=yes
fastboot.forceFastBoot=no
```

Android Studio에서 생성할 경우 Automotive → Pleos Connect v2.1.0 → API 34 이미지를 선택하고 이름을 Pleos_Local_Test로 맞춘다. [공식 이미지·AVD 설치 가이드](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)를 참고한다.

### 5. 프로젝트 경로 설정과 첫 검증

각 저장소 루트에서 다음을 실행해 local.properties를 만든다. 기존 파일이 있다면 다른 설정은 유지하고 sdk.dir만 수정한다. 경로는 백슬래시 대신 슬래시로 기록한다.

```powershell
$sdkDirectoryForGradle = $sdkRoot.Replace('\', '/')
# 기존 local.properties가 없는 새 clone에서만 실행:
"sdk.dir=$sdkDirectoryForGradle" | Set-Content -Encoding ASCII local.properties
```

Android Studio에서는 Gradle JDK를 devRoot/java 아래 JDK 17로 선택한다. SDK 다운로드를 마쳤어도 최초 Gradle 실행에는 Maven 의존성 다운로드가 필요하다. 별도 Gradle 설치나 서버 키는 필요 없다. 아래 반복 실행 절의 doctor → test → build → start 순서로 진행하고, 부팅 완료 후 diagnostics와 smoke를 실행한다.

첫 부팅 확인은 SDK의 platform-tools/adb.exe로 `devices`와 `shell getprop sys.boot_completed`를 실행한다. 온라인 기기와 값 1을 확인한다. console 명령으로 포트 포워딩 후 브라우저에서 `http://127.0.0.1:8088/`에 접속한다. [공식 콘솔 가이드](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/use-vehicle-emulator)

## 반복 실행

각 저장소 루트에서 PowerShell로 실행한다. 실행 정책으로 막히면 `powershell -ExecutionPolicy Bypass -File .\tools\windows-test.ps1 doctor`처럼 해당 스크립트 실행에만 정책을 지정한다.

```powershell
.\tools\windows-test.ps1 doctor
.\tools\windows-test.ps1 test
.\tools\windows-test.ps1 build
.\tools\windows-test.ps1 start
```

start 이후 에뮬레이터 부팅이 끝나면:

```powershell
.\tools\windows-test.ps1 diagnostics -Profile cruise
.\tools\windows-test.ps1 console
.\tools\windows-test.ps1 read
```

차슐랭 저장소에서는 `diagnostics -Profile charging`으로 배터리 신호를 확인한다. 진단 화면에서도 live cruise/live charging/fake/stop 버튼으로 전환할 수 있다. 차속 권한 창이 뜨면 에뮬레이터에서 허용한다. 거부한 경우 PERMISSION_DENIED가 표시된다.

콘솔 주소는 `http://127.0.0.1:8088/`이다. SDK 진단 화면은 앱의 기본 제품 화면과 별도의 Activity이며, 일반 Run으로 제품 화면을 열었을 때 자동 시작하지 않는다.

read는 에뮬레이터의 현재 Android 사용자 번호를 조회하고 앱 내부 `sdk-diagnostics.json`을 읽는다. JSON에는 mode, profile, lifecycle, 신호별 status/value/sequence/observedAtMillis/error가 있다. 같은 결과를 진단 화면과 Logcat의 SdkDiagnostics 태그에서도 확인할 수 있다. 위치를 포함하는 개발 로그는 로컬에만 남으며 서버 업로드는 없다.

콘솔 → 실제 SDK → 어댑터 → 진단 보고서를 자동으로 검사하려면 APK 설치 후 다음을 실행한다. 이 명령은 에뮬레이터의 차속·크루즈·브레이크 또는 배터리 값을 변경한다. 검사가 끝나면 속도 0/브레이크 해제/크루즈 ready, 또는 충전 스위치와 충전구 닫힘으로 정리한다. 크루즈 검사에서는 전용 에뮬레이터 앱의 차속 권한을 adb로 허용한다.

```powershell
.\tools\windows-smoke.ps1 -Profile cruise
.\tools\windows-smoke.ps1 -Profile charging
```

검사 근거 JSON은 `%LOCALAPPDATA%/hyundai-dev/logs/smoke-<앱패키지>-<프로필>.json`에 저장된다. mode=live, 프로필, 보고서 신선도와 각 신호값을 함께 검사하여 Fake나 과거 파일을 성공으로 처리하지 않는다.

수집 종료:

```powershell
.\tools\windows-test.ps1 stop
```

이 명령은 앱을 force-stop하므로 종료 후 파일은 마지막 보고서다. 앱 내 Stop 버튼으로 정상 SDK 정리를 검사할 수 있다. 과거 보고서의 updatedAt을 보고 현재 실행 여부를 구분한다.

## 수신 확인 항목

| 조작 | 기대 결과 |
|---|---|
| 콘솔 차속 60km/h | speed AVAILABLE, value 약 60 |
| 콘솔 크루즈 full_autonomous | cruise ACTIVATED |
| 콘솔 크루즈 ready | cruise ENABLED |
| 콘솔 브레이크 조작 | brake true/false |
| 콘솔 SoC 변경 | charging 프로필 soc Int % 갱신 |
| 콘솔 충전구 변경 | port true/false |
| 충전 스위치 변경 | 최대 기본 폴링 주기 10초 후 갱신 |
| live → Stop → live | STOPPED에서 값 비움, 새 세션 sequence 재시작 |
| fake 실행 | mode=fake이며 실제 SDK 연결 검증과 구분 |

에뮬레이터 위치는 기존 실측상 고정이다. 혼잡 정보는 내비 경로 안내 상태에 의존한다. 브레이크를 밟는 것만으로 에뮬레이터 크루즈가 자동 해제되지는 않으므로 크루즈 상태를 별도로 변경한다.

## 문제 확인

- doctor에서 WHPX 가속 사용 가능 여부와 AVD 목록을 확인한다.
- 여러 기기가 연결돼 있으면 `-Device emulator-5554`처럼 대상 지정.
- start 로그: `%LOCALAPPDATA%/hyundai-dev/logs/emulator-out.log`, `emulator-error.log`.
- 초기 연결 실패는 앱에서 Stop 후 다시 Start한다. 앱 내 보고서의 error를 함께 확인한다.
- 빌드·SDK 라이브러리 다운로드가 끝나기 전에는 인터넷이 필요하다. 내비 서비스가 자체적으로 네트워크를 사용할 수 있으나 별도 서버 연동은 설정하지 않는다.
- 회원가입·결제가 필요한 화면이 나오면 자동으로 진행하지 않는다.

## 이 PC에서 확인한 결과 (2026-10-30)

- Java 17.0.20.1, Gradle 8.11.1, Android Platform 34, Build Tools 35.0.0, Platform Tools 37.0.1 설치.
- Pleos Connect 2.1.0 x86_64 이미지, AVD Pleos_Local_Test, RAM 4096MB/CPU 4개/Cold boot. WHPX 사용 가능, sys.boot_completed=1 확인.
- 두 저장소의 debug APK 빌드·설치 성공. 실제 Gradle core 단위 테스트 14개 통과.
- 크루즈 앱 smoke 7개: 실제 차속 60, ACTIVATED, 브레이크 true, ENABLED, 목표속도 25m/s→90km/h, 조향 30→-30, NaviHelper 위치 수신.
- 차슐랭 앱 smoke 4개: SoC 55, 충전구 true, 충전 스위치 true 폴링, NaviHelper 위치 수신.
- 진단 화면 Stop에서 STOPPED/NOT_RECEIVED, 기존 Activity 재시작에서 AVAILABLE 복구, Fake 버튼에서 mode=fake 확인.
- release 매니페스트에 진단 Activity와 debug 전용 확장 권한이 포함되지 않는 것을 확인.
- 회원가입·결제·Supabase/별도 서버 설정 없음. 사용자가 동의한 Android/42dot SDK 라이선스로 설치.

실제 SDK 테스트 근거: `%LOCALAPPDATA%/hyundai-dev/logs/smoke-com.hyundaiapp.cruisebreaker-cruise.json`, `smoke-com.hyundaiapp.chachelin-charging.json`, `stop-button-evidence.json`, `fake-button-evidence.json`. 기록은 해당 실행 시점의 결과이며 새 변경 후에는 smoke를 다시 실행한다.

정상 SDK 연결을 확인한 범위이며 실차, 백그라운드 동결 해결, 실제 이동 경로·혼잡 구간 포함 여부를 보장하지 않는다. 에뮬레이터·콘솔로 이 후속 작업도 반복 검증할 수 있다.

## 2026-11-02 병합 후 재검증

- 두 앱 debug APK 빌드·설치 및 lint 0 errors. 제품 SDK 소스 vehicle/navi READY, 크루즈 speed READY 확인.
- 크루즈 앱 단위 테스트 debug/release 각각 44개, 차슐랭 각각 2개. 공통 모듈 debug 17개/release 3개 통과.
- 분리된 debug probe smoke: 크루즈 7개, 충전 4개 통과. release core JAR·매니페스트에 probe 진단 코드가 포함되지 않음.
- 제품 앱의 충전 세션 로직 feature 브랜치 및 서버 연동은 이번 main 병합과 별개다. 차슐랭 배터리 용량 권한 오류는 기존 제한으로 남아 있다.
