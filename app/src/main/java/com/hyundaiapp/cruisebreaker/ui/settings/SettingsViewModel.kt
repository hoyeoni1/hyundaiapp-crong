package com.hyundaiapp.cruisebreaker.ui.settings

import androidx.lifecycle.ViewModel
import com.hyundaiapp.cruisebreaker.data.AppPrefs
import com.hyundaiapp.cruisebreaker.ui.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * S4 설정. 값은 [AppPrefs]에 바로 저장한다(음성 안내는 다음 안내부터 적용).
 * 안내 거리는 500m 고정(11-01 결정) → 화면에서 바꿔도 반영하지 않는다. 거리 선택은 길민종이 화면에서 지울 예정.
 */
class SettingsViewModel(
    private val prefs: AppPrefs,
    private val onWithdrawConsent: () -> Unit = {},
) : ViewModel() {
    private val _state = MutableStateFlow(fromPrefs())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun setVoiceEnabled(enabled: Boolean) {
        prefs.voiceEnabled = enabled
        _state.update { it.copy(voiceEnabled = enabled) }
    }

    fun setAutoRecordEnabled(enabled: Boolean) {
        prefs.autoRecordEnabled = enabled
        _state.update { it.copy(autoRecordEnabled = enabled) }
    }

    /** 동의 철회: 기록 중지·기기 데이터 삭제는 [onWithdrawConsent](AppContainer.withdrawConsent)가 한다. 화면 값은 기본값으로 */
    fun withdrawConsent() {
        onWithdrawConsent()
        _state.value = fromPrefs()
    }

    private fun fromPrefs() = SettingsUiState(
        voiceEnabled = prefs.voiceEnabled,
        announceDistanceM = FIXED_DISTANCE_M,
        autoRecordEnabled = prefs.autoRecordEnabled,
    )

    private companion object {
        const val FIXED_DISTANCE_M = 500
    }
}
