package com.hyundaiapp.cruisebreaker.ui.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import com.hyundaiapp.cruisebreaker.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.hyundaiapp.core.VehicleSignals
import com.hyundaiapp.core.debugLines

/**
 * 신호 모니터: core가 받는 모든 값을 실시간으로 보여 준다.
 * Virtual Console(localhost:8088)에서 값을 바꾸고 여기서 바로 들어오는지 확인한다.
 */
@Composable
fun SignalMonitorScreen(signals: VehicleSignals) {
    val lines by remember(signals) { signals.debugLines() }.collectAsState(initial = emptyList())
    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("신호 모니터", style = MaterialTheme.typography.headlineMedium)
        lines.forEach { Text(it, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 8.dp)) }
    }
}
