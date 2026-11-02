package com.hyundaiapp.cruisebreaker.ui.risk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyundaiapp.core.GeoMath
import com.hyundaiapp.core.GeoPoint
import com.hyundaiapp.core.VehicleSignals
import com.hyundaiapp.cruisebreaker.data.SegmentRiskRepository
import com.hyundaiapp.cruisebreaker.ui.RiskListUiState
import com.hyundaiapp.cruisebreaker.ui.RiskRowUi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** S2 위험 구간 목록(정차 중). 화면에 들어올 때마다 [refresh]로 주변 10km를 다시 조회한다(내 해제 기록이 반영된 값) */
class RiskListViewModel(
    private val signals: VehicleSignals,
    private val risks: SegmentRiskRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(RiskListUiState())
    val state: StateFlow<RiskListUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val here = signals.location.value?.point
            val rows = try {
                risks.nearby(here ?: FALLBACK_CENTER, RADIUS_M)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyList()
            }
            val items = rows.filter { it.isRisk }.map { r ->
                RiskRowUi(
                    id = "${r.key.geohash}-${r.key.heading8}",
                    roadName = r.roadName.ifBlank { "이름 없는 구간" },
                    distanceM = here?.let { GeoMath.distanceM(it, r.center) },
                    rate = r.rate,
                    passVehicles = r.passVehicles,
                )
            }.sortedBy { it.distanceM ?: Double.MAX_VALUE }
            _state.value = RiskListUiState(loading = false, items = items)
        }
    }

    private companion object {
        const val RADIUS_M = 10_000.0

        /** 위치를 아직 못 받았으면 판교(시드 데이터 지역) 기준 */
        val FALLBACK_CENTER = GeoPoint(37.4127, 127.0947)
    }
}
