package com.hyundaiapp.cruisebreaker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hyundaiapp.cruisebreaker.AppContainer
import com.hyundaiapp.cruisebreaker.BuildConfig
import com.hyundaiapp.cruisebreaker.ui.debug.SignalMonitorScreen
import com.hyundaiapp.cruisebreaker.ui.driving.DrivingScreen
import com.hyundaiapp.cruisebreaker.ui.driving.DrivingViewModel

/** PRD 5.5 화면 목록. 디버그 빌드는 개발 메뉴에서 시작한다 */
enum class Screen(val title: String) {
    DEV_MENU("개발 메뉴"),
    DRIVING("S1 주행 화면"),
    RISK_LIST("S2 위험 구간 목록"),
    TRIP_SUMMARY("S3 주행 후 요약"),
    SETTINGS("S4 설정"),
    ONBOARDING("S5 첫 실행 동의"),
    SIGNALS("신호 모니터"),
}

/** 간단한 화면 스택. 화면이 늘면 navigation-compose로 바꿔도 된다(길민종) */
@Composable
fun CruiseBreakerNav(container: AppContainer) {
    val stack = remember { mutableStateListOf(if (BuildConfig.DEBUG) Screen.DEV_MENU else Screen.DRIVING) }
    val go: (Screen) -> Unit = { stack.add(it) }
    BackHandler(enabled = stack.size > 1) { stack.removeAt(stack.lastIndex) }

    Surface(Modifier.fillMaxSize()) {
        when (stack.last()) {
            Screen.DEV_MENU -> DevMenu(go)
            Screen.DRIVING -> {
                val vm = viewModel { DrivingViewModel(container.signals, container.announcer) }
                val state by vm.state.collectAsState()
                DrivingScreen(state)
            }
            Screen.SIGNALS -> SignalMonitorScreen(container.signals)
            // 아래는 아직 ViewModel이 없다 → 미리보기 데이터로 상태만 보여 준다
            Screen.RISK_LIST -> Placeholder(Screen.RISK_LIST, PreviewData.riskList)
            Screen.TRIP_SUMMARY -> Placeholder(Screen.TRIP_SUMMARY, PreviewData.tripSummary)
            Screen.SETTINGS -> Placeholder(Screen.SETTINGS, SettingsUiState())
            Screen.ONBOARDING -> Placeholder(Screen.ONBOARDING, OnboardingUiState())
        }
    }
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

@Composable
private fun Placeholder(screen: Screen, state: Any) {
    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text(screen.title, style = MaterialTheme.typography.headlineMedium)
        Text("화면 미구현 — 아래 UiState로 그린다", modifier = Modifier.padding(vertical = 12.dp))
        Text(state.toString())
    }
}
