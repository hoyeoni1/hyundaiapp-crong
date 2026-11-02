package com.hyundaiapp.cruisebreaker.ui.driving

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyundaiapp.cruisebreaker.domain.CruiseStateInterpreter
import com.hyundaiapp.cruisebreaker.domain.RiskAnnouncer
import com.hyundaiapp.cruisebreaker.ui.DrivingUiState
import com.hyundaiapp.core.SourceState
import com.hyundaiapp.core.VehicleSignals
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * S1 주행 화면. 신호 → ViewModel → UiState → 화면 흐름의 예시(다른 화면도 이 모양으로 만든다).
 */
class DrivingViewModel(
    signals: VehicleSignals,
    announcer: RiskAnnouncer,
) : ViewModel() {
    val state: StateFlow<DrivingUiState> = combine(
        signals.sources, signals.cruise, signals.drive, announcer.current,
    ) { src, cruise, drive, announcement ->
        DrivingUiState(
            recording = src.vehicle == SourceState.READY,
            cruiseEngaged = CruiseStateInterpreter.isEngaged(cruise.mode),
            speedKph = drive.speedKph,
            banner = announcement?.bannerText,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DrivingUiState())
}
