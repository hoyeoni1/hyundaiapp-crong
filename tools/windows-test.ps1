param(
    [ValidateSet('doctor','test','build','start','diagnostics','read','console','stop')]
    [string]$Action = 'doctor',
    [ValidateSet('cruise','charging')][string]$Profile = 'cruise',
    [string]$Avd = 'Pleos_Local_Test',
    [string]$Device = ''
)
$ErrorActionPreference = 'Stop'
$repository = Split-Path $PSScriptRoot
$environmentRoot = Join-Path $env:LOCALAPPDATA 'hyundai-dev'
$env:JAVA_HOME = (Get-ChildItem (Join-Path $environmentRoot 'java') -Directory | Select-Object -First 1).FullName
$env:ANDROID_HOME = Join-Path $environmentRoot 'android-sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$adb = Join-Path $env:ANDROID_HOME 'platform-tools/adb.exe'
$adbArgs = @()
if ($Device) { $adbArgs = @('-s', $Device) }
$package = if ((Get-Content (Join-Path $repository 'settings.gradle.kts') -Raw) -match '"Chachelin"') { 'com.hyundaiapp.chachelin' } else { 'com.hyundaiapp.cruisebreaker' }
Push-Location $repository
try {
    switch ($Action) {
        doctor {
            & "$env:JAVA_HOME/bin/java.exe" -version
            if (Test-Path $adb) { & $adb devices -l } else { Write-Output 'Android Platform Tools not installed yet' }
            $emulator = Join-Path $env:ANDROID_HOME 'emulator/emulator.exe'
            if (Test-Path $emulator) { & $emulator -accel-check; & $emulator -list-avds }
        }
        test { & ./gradlew.bat :core:testDebugUnitTest --max-workers=2; if ($LASTEXITCODE) { throw 'Tests failed' } }
        build { & ./gradlew.bat assembleDebug --max-workers=2; if ($LASTEXITCODE) { throw 'Build failed' } }
        start {
            $running = Get-CimInstance Win32_Process -Filter "name='qemu-system-x86_64.exe'" -ErrorAction SilentlyContinue |
                Where-Object { $_.CommandLine -match ('-avd\s+"?' + [regex]::Escape($Avd) + '(?:"|\s|$)') }
            if ($running) { Write-Output "AVD $Avd is already running"; break }
            $logRoot = Join-Path $environmentRoot 'logs'
            New-Item -ItemType Directory -Force $logRoot | Out-Null
            Start-Process -FilePath (Join-Path $env:ANDROID_HOME 'emulator/emulator.exe') -ArgumentList @('-avd', $Avd, '-no-snapshot-load', '-gpu', 'auto') -WindowStyle Hidden -RedirectStandardOutput "$logRoot/emulator-out.log" -RedirectStandardError "$logRoot/emulator-error.log"
        }
        diagnostics {
            & $adb @adbArgs install -r app/build/outputs/apk/debug/app-debug.apk
            if ($LASTEXITCODE) { throw 'APK installation failed' }
            & $adb @adbArgs shell am start -W -n "$package/com.hyundaiapp.core.SdkDiagnosticsActivity" --es profile $Profile --ez autostart true
            if ($LASTEXITCODE) { throw 'Diagnostics launch failed' }
        }
        read {
            $userId = (& $adb @adbArgs shell am get-current-user).Trim()
            & $adb @adbArgs shell run-as $package --user $userId cat files/sdk-diagnostics.json
            if ($LASTEXITCODE) { throw 'No diagnostic report: launch diagnostics first' }
        }
        console { & $adb @adbArgs forward tcp:8088 tcp:8088; Write-Output 'Open http://127.0.0.1:8088/ in a browser' }
        stop { & $adb @adbArgs shell am force-stop $package }
    }
} finally { Pop-Location }
