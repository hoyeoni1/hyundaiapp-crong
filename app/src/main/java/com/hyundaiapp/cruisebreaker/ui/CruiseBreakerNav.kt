package com.hyundaiapp.cruisebreaker.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.hyundaiapp.cruisebreaker.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hyundaiapp.core.VehicleSignals
import com.hyundaiapp.cruisebreaker.AppContainer
import com.hyundaiapp.cruisebreaker.BuildConfig
import com.hyundaiapp.cruisebreaker.RecordingService
import com.hyundaiapp.cruisebreaker.data.AppPrefs
import com.hyundaiapp.cruisebreaker.data.SegmentRiskRepository
import com.hyundaiapp.cruisebreaker.domain.RiskAnnouncer
import com.hyundaiapp.cruisebreaker.domain.TripStats
import com.hyundaiapp.cruisebreaker.ui.components.CarAlertDialog
import com.hyundaiapp.cruisebreaker.ui.components.rememberNotificationPermission
import com.hyundaiapp.cruisebreaker.ui.debug.SignalMonitorScreen
import com.hyundaiapp.cruisebreaker.ui.driving.DrivingScreen
import com.hyundaiapp.cruisebreaker.ui.driving.DrivingViewModel
import com.hyundaiapp.cruisebreaker.ui.onboarding.OnboardingScreen
import com.hyundaiapp.cruisebreaker.ui.onboarding.OnboardingViewModel
import com.hyundaiapp.cruisebreaker.ui.onboarding.Terms
import com.hyundaiapp.cruisebreaker.ui.risk.RiskListScreen
import com.hyundaiapp.cruisebreaker.ui.risk.RiskListViewModel
import com.hyundaiapp.cruisebreaker.ui.settings.SettingsScreen
import com.hyundaiapp.cruisebreaker.ui.settings.SettingsViewModel
import com.hyundaiapp.cruisebreaker.ui.summary.TripSummaryScreen
import com.hyundaiapp.cruisebreaker.ui.summary.TripSummaryViewModel

/** PRD 5.5 화면 목록. 디버그 빌드는 개발 메뉴에서 시작한다 */
enum class Screen(val title: String) {
    DEV_MENU("개발 메뉴"),
    DRIVING("S1 주행 화면"),
    RISK_LIST("S2 해제 다발 구간 목록"),
    TRIP_SUMMARY("S3 주행 후 요약"),
    SETTINGS("S4 설정"),
    ONBOARDING("S5 첫 실행 동의"),
    SIGNALS("신호 모니터"),
}

/** 간단한 화면 스택. 화면이 늘면 navigation-compose로 바꿔도 된다(길민종) */
@Composable
fun CruiseBreakerNav(container: AppContainer) {
    val context = LocalContext.current
    val dependencies = remember(container, context) {
        CruiseNavigationDependencies(
            signals = container.signals,
            prefs = container.prefs,
            risks = container.risks,
            announcer = container.announcer,
            tripStats = container.tripStats,
            onStart = { RecordingService.startIfAllowed(context) },
            onWithdraw = container::withdrawConsent,
        )
    }
    CruiseBreakerNavigation(dependencies, BuildConfig.DEBUG, rememberCarUxRestricted(), context)
}

/** UI가 쓰는 기존 상태·콜백만 전달해 SDK 없이 화면 전환도 확인할 수 있게 한다. */
internal data class CruiseNavigationDependencies(
    val signals: VehicleSignals,
    val prefs: AppPrefs,
    val risks: SegmentRiskRepository,
    val announcer: RiskAnnouncer,
    val tripStats: TripStats,
    val onStart: () -> Unit,
    val onWithdraw: () -> Unit,
)

@Composable
internal fun CruiseBreakerNavigation(
    dependencies: CruiseNavigationDependencies,
    debug: Boolean,
    uxRestricted: Boolean?,
    context: Context = LocalContext.current,
) {
    val stack = remember { mutableStateListOf(initialScreen(debug, dependencies.prefs.onboarded)) }
    var sessionStarted by remember { mutableStateOf(dependencies.prefs.onboarded) }
    val drive by dependencies.signals.drive.collectAsState()
    val currentScreen = visibleScreen(stack.last(), drive, uxRestricted)
    val go: (Screen) -> Unit = { stack.add(it) }
    val back: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    val resetTo: (Screen) -> Unit = {
        stack.clear()
        if (debug && it != Screen.DEV_MENU) stack.add(Screen.DEV_MENU)
        stack.add(it)
    }
    BackHandler(enabled = currentScreen == Screen.TRIP_SUMMARY || (currentScreen != Screen.DRIVING && stack.size > 1)) {
        if (currentScreen == Screen.TRIP_SUMMARY) resetTo(Screen.RISK_LIST) else back()
    }

    var terms by remember { mutableStateOf<Terms?>(null) }
    var confirmWithdraw by remember { mutableStateOf(false) }
    val tripNavigation = remember { TripEndNavigation() }
    LaunchedEffect(currentScreen) {
        if (currentScreen == Screen.DRIVING) {
            terms = null
            confirmWithdraw = false
        }
    }
    LaunchedEffect(sessionStarted, dependencies.signals) {
        dependencies.signals.drive.collect { observed ->
            if (tripNavigation.shouldShowSummary(sessionStarted, observed)) resetTo(Screen.TRIP_SUMMARY)
        }
    }
    val notifications = rememberNotificationPermission()
    val onboardingVm = viewModel {
        OnboardingViewModel(
            dependencies.prefs,
            speedPermissionGranted = { context.checkSelfPermission(SPEED_PERMISSION) == PackageManager.PERMISSION_GRANTED },
            onStarted = dependencies.onStart,
        )
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, onboardingVm, dependencies.signals) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onboardingVm.reload()
                dependencies.signals.onPermissionsChanged()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val speedPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onboardingVm.onSpeedPermissionResult(granted)
        dependencies.signals.onPermissionsChanged()
    }

    Surface(Modifier.fillMaxSize()) {
        when (currentScreen) {
            Screen.DEV_MENU -> DevMenu(go)
            Screen.DRIVING -> {
                val vm = viewModel { DrivingViewModel(dependencies.signals, dependencies.announcer) }
                val state by vm.state.collectAsState()
                DrivingScreen(state)
            }
            Screen.SIGNALS -> {
                // 신호 수신은 동의 후 기록 서비스가 시작한다. 개발 메뉴의 신호 모니터만 들어올 때 따로 켠다
                DisposableEffect(dependencies) {
                    dependencies.signals.start()
                    onDispose { if (!dependencies.prefs.onboarded) dependencies.signals.stop() }
                }
                SignalMonitorScreen(dependencies.signals)
            }
            Screen.RISK_LIST -> {
                val vm = viewModel { RiskListViewModel(dependencies.signals, dependencies.risks) }
                // 화면에 들어올 때마다 새로 조회한다(방금 남긴 해제가 반영되게)
                LaunchedEffect(Unit) { vm.refresh() }
                val state by vm.state.collectAsState()
                RiskListScreen(
                    state,
                    onSummaryClick = { go(Screen.TRIP_SUMMARY) },
                    onSettingsClick = { go(Screen.SETTINGS) },
                )
            }
            Screen.TRIP_SUMMARY -> {
                val vm = viewModel { TripSummaryViewModel(dependencies.tripStats) }
                val state by vm.state.collectAsState()
                TripSummaryScreen(state, onShowRisks = { resetTo(Screen.RISK_LIST) }, onDone = { resetTo(Screen.RISK_LIST) })
            }
            Screen.SETTINGS -> {
                val vm = viewModel { SettingsViewModel(dependencies.prefs, onWithdrawConsent = dependencies.onWithdraw) }
                val state by vm.state.collectAsState()
                SettingsScreen(
                    state,
                    onVoiceEnabledChange = vm::setVoiceEnabled,
                    onAutoRecordEnabledChange = vm::setAutoRecordEnabled,
                    onConsentClick = { go(Screen.ONBOARDING) },
                    onBack = back,
                    onWithdrawConsent = { confirmWithdraw = true },
                )
                if (confirmWithdraw) {
                    WithdrawDialog(
                        onConfirm = {
                            confirmWithdraw = false
                            vm.withdrawConsent()
                            sessionStarted = false
                            resetTo(initialScreen(debug, dependencies.prefs.onboarded))
                        },
                        onDismiss = { confirmWithdraw = false },
                    )
                }
            }
            Screen.ONBOARDING -> {
                LaunchedEffect(Unit) { onboardingVm.reload() }
                val state by onboardingVm.state.collectAsState()
                OnboardingScreen(
                    state,
                    onAgreeLocation = onboardingVm::agreeLocation,
                    onAgreePrivacy = onboardingVm::agreePrivacy,
                    onRequestSpeedPermission = { speedPermission.launch(SPEED_PERMISSION) },
                    onShowTerms = { terms = it },
                    notificationsGranted = notifications.granted,
                    onRequestNotifications = notifications.request,
                    notificationsNeedSettings = notifications.openSettings,
                    onBack = if (stack.size > 1 && stack[stack.lastIndex - 1] == Screen.SETTINGS) back else null,
                    onStart = {
                        if (onboardingVm.start()) {
                            sessionStarted = true
                            if (stack.size > 1) back() else resetTo(Screen.RISK_LIST)
                        }
                    },
                )
            }
        }
    }

    if (currentScreen != Screen.DRIVING) terms?.let { TermsDialog(it, onDismiss = { terms = null }) }
}

/** 동의 철회 확인. 취소·바깥 누르기는 철회 아님 */
@Composable
private fun WithdrawDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    CarAlertDialog("동의를 철회할까요?", "주행 기록을 멈추고 기기의 동의·기록 정보를 지워요. 서버 기록 삭제도 요청하며, 통신 상태에 따라 완료까지 걸릴 수 있어요. 다시 쓰려면 동의가 필요해요.",
        "철회", onConfirm, onDismiss, dismissLabel = "취소", destructive = true)
}

/** 약관 전문(TermsText, 원본 docs/terms-draft.md). 닫기는 동의가 아니다 */
@Composable
private fun TermsDialog(terms: Terms, onDismiss: () -> Unit) {
    val location = terms == Terms.LOCATION
    CarAlertDialog(if (location) "위치정보 이용" else "개인정보 수집·이용", if (location) TermsText.LOCATION else TermsText.PRIVACY, "닫기", onDismiss, onDismiss)
}

@Composable
private fun DevMenu(go: (Screen) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("크루즈브레이커 개발 메뉴 (디버그 빌드에만 보임)", style = MaterialTheme.typography.headlineMedium)
        Screen.entries.filter { it != Screen.DEV_MENU }.forEach { s ->
            Button(onClick = { go(s) }) { Text(s.title) }
        }
    }
}

private const val SPEED_PERMISSION = "android.car.permission.CAR_SPEED"
