package com.hyundaiapp.cruisebreaker.ui.onboarding

import androidx.lifecycle.ViewModel
import com.hyundaiapp.cruisebreaker.data.AppPrefs
import com.hyundaiapp.cruisebreaker.ui.OnboardingUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * S5 첫 실행 동의. [동의] 버튼을 눌렀을 때만 동의로 저장한다(약관 창 닫기·뒤로 가기는 동의 아님).
 * 차속 권한은 거부해도 시작할 수 있다(차속 없으면 60km/h 조건만 건너뜀, docs/disengage-rules.md).
 * [onStarted]는 필수 동의를 마치고 [시작하기]를 누르면 불린다(기록 서비스 시작 등).
 */
class OnboardingViewModel(
    private val prefs: AppPrefs,
    speedPermissionGranted: () -> Boolean,
    private val onStarted: () -> Unit = {},
) : ViewModel() {
    private val _state = MutableStateFlow(
        OnboardingUiState(
            locationAgreed = prefs.locationAgreed,
            privacyAgreed = prefs.privacyAgreed,
            speedPermissionGranted = speedPermissionGranted(),
        ),
    )
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun agreeLocation() {
        prefs.locationAgreed = true
        _state.update { it.copy(locationAgreed = true) }
    }

    fun agreePrivacy() {
        prefs.privacyAgreed = true
        _state.update { it.copy(privacyAgreed = true) }
    }

    fun onSpeedPermissionResult(granted: Boolean) {
        _state.update { it.copy(speedPermissionGranted = granted) }
    }

    /** @return 시작했으면 true(필수 동의 2개가 있어야 함) */
    fun start(): Boolean {
        if (!prefs.onboarded) return false
        onStarted()
        return true
    }
}
