param([ValidateSet('cruise','charging')][string]$Profile = 'cruise', [string]$Device = '')
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot
$environmentRoot = Join-Path $env:LOCALAPPDATA 'hyundai-dev'
$adb = Join-Path $environmentRoot 'android-sdk/platform-tools/adb.exe'
$argsAdb = @()
if ($Device) { $argsAdb = @('-s', $Device) }
$package = if ((Get-Content "$root/settings.gradle.kts" -Raw) -match '"Chachelin"') { 'com.hyundaiapp.chachelin' } else { 'com.hyundaiapp.crong' }
$results = [System.Collections.Generic.List[object]]::new()
function Read-Report {
    $userId = (& $adb @argsAdb shell am get-current-user).Trim()
    $raw = & $adb @argsAdb shell run-as $package --user $userId cat files/sdk-diagnostics.json 2>$null
    if ($LASTEXITCODE -eq 0) { try { return ($raw -join "`n" | ConvertFrom-Json) } catch {} }
    return $null
}
function Set-Console([string]$Path) {
    Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:8088/browser/session?client=windows-smoke' | Out-Null
    Invoke-RestMethod -Method Put -Uri "http://127.0.0.1:8088/vehicle/$Path" | Out-Null
}
function Assert-Reading([string]$Name, [scriptblock]$Condition) {
    $deadline = [DateTime]::UtcNow.AddSeconds(25)
    do {
        $report = Read-Report
        if ($report -and $report.mode -eq 'live' -and $report.profile -eq $Profile -and [math]::Abs([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() - $report.updatedAt) -lt 5000 -and (& $Condition $report)) {
            $results.Add([pscustomobject]@{name=$Name; passed=$true; report=$report})
            Write-Output "PASS: $Name"
            return
        }
        Start-Sleep -Milliseconds 750
    } while ([DateTime]::UtcNow -lt $deadline)
    $results.Add([pscustomobject]@{name=$Name; passed=$false; report=$report})
    throw "SDK smoke test failed: $Name"
}
try {
    & $adb @argsAdb forward tcp:8088 tcp:8088
    if ($LASTEXITCODE) { throw 'ADB console forwarding failed' }
    if ($Profile -eq 'cruise') {
        $userId = (& $adb @argsAdb shell am get-current-user).Trim()
        & $adb @argsAdb shell pm grant --user $userId $package android.car.permission.CAR_SPEED
        if ($LASTEXITCODE) { throw 'Speed permission grant failed' }
    }
    & $adb @argsAdb shell am start -W -n "$package/com.hyundaiapp.core.probe.SdkDiagnosticsActivity" --es profile $Profile --ez autostart true
    if ($LASTEXITCODE) { throw 'Diagnostics launch failed' }
    if ($Profile -eq 'cruise') {
        Set-Console 'controls?speed=60&brake=0'
        Assert-Reading 'actual speed 60 km/h' { param($r) $r.speed.status -eq 'AVAILABLE' -and [math]::Abs([double]$r.speed.value - 60) -lt 0.2 }
        Set-Console 'driving-mode?ad_mode_state=full_autonomous'
        Assert-Reading 'cruise ACTIVATED' { param($r) $r.cruise.status -eq 'AVAILABLE' -and $r.cruise.value -eq 'ACTIVATED' }
        Set-Console 'controls?brake=50'
        Assert-Reading 'brake pressed' { param($r) $r.brake.status -eq 'AVAILABLE' -and $r.brake.value -eq 'true' }
        Set-Console 'driving-mode?ad_mode_state=ready'
        Assert-Reading 'cruise ENABLED' { param($r) $r.cruise.status -eq 'AVAILABLE' -and $r.cruise.value -eq 'ENABLED' }
        Set-Console 'driving-mode?target_speed_mps=25'
        Assert-Reading 'target speed m/s to 90 km/h' { param($r) $r.targetSpeed.status -eq 'AVAILABLE' -and [math]::Abs([double]$r.targetSpeed.value - 90) -lt 0.2 }
        Set-Console 'controls?steering=30'
        Assert-Reading 'steering callback' { param($r) $r.steering.status -eq 'AVAILABLE' -and [math]::Abs([double]$r.steering.value + 30) -lt 0.2 }
    } else {
        Set-Console 'ev-battery?field=soc&value=55'
        Assert-Reading 'SoC 55 percent' { param($r) $r.soc.status -eq 'AVAILABLE' -and $r.soc.value -eq '55' }
        Set-Console 'ev-battery?field=charge_port_open&value=true'
        Assert-Reading 'charge port open' { param($r) $r.port.status -eq 'AVAILABLE' -and $r.port.value -eq 'true' }
        Set-Console 'ev-battery?field=charge_switch&value=true'
        Assert-Reading 'charge switch polled' { param($r) $r.chargeSwitch.status -eq 'AVAILABLE' -and $r.chargeSwitch.value -eq 'true' }
    }
    Assert-Reading 'NaviHelper location received' { param($r) $r.location.status -eq 'AVAILABLE' }
} finally {
    try {
        if ($Profile -eq 'cruise') { Set-Console 'controls?speed=0&brake=0&steering=0'; Set-Console 'driving-mode?ad_mode_state=ready&target_speed_mps=13.8889' }
        else { Set-Console 'ev-battery?field=charge_switch&value=false'; Set-Console 'ev-battery?field=charge_port_open&value=false' }
    } catch { Write-Warning "Console reset failed: $_" }
    $logRoot = Join-Path $environmentRoot 'logs'
    New-Item -ItemType Directory -Force $logRoot | Out-Null
    $path = Join-Path $logRoot "smoke-$package-$Profile.json"
    [pscustomobject]@{timestamp=[DateTimeOffset]::UtcNow.ToString('o'); package=$package; profile=$Profile; cases=$results.ToArray()} | ConvertTo-Json -Depth 12 | Set-Content -Encoding UTF8 $path
    Write-Output "Evidence: $path"
}
