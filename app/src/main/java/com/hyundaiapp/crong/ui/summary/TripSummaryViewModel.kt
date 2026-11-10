package com.hyundaiapp.crong.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyundaiapp.crong.domain.TripStats
import com.hyundaiapp.crong.ui.TripSummaryUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** S3 주행 후 요약(정차 중). 마지막 주행 값 */
class TripSummaryViewModel(stats: TripStats) : ViewModel() {
    val state: StateFlow<TripSummaryUiState> = stats.current.map {
        TripSummaryUiState(
            riskSegmentsPassed = it.riskSegments,
            announcements = it.announcements,
            myContributions = it.disengages,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripSummaryUiState())
}
