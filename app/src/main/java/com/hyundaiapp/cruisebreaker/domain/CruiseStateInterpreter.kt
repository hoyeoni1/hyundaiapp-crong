package com.hyundaiapp.cruisebreaker.domain

import com.hyundaiapp.core.CruiseMode

/**
 * 크루즈 상태 해석을 한 곳에 모은다(PRD 5.3). 실차 해제 값이 확인되면 **여기만** 고친다.
 *
 * 에뮬레이터 실측(findings 6.2.1): 콘솔 full_autonomous → ACTIVATED, ready·longitudinal·lateral → ENABLED,
 * not_ready → null. 해제 값(USER_OVERRIDE 등)은 콘솔로 만들 수 없어 시연은 ACTIVATED → ENABLED를 해제로 본다.
 */
object CruiseStateInterpreter {
    /** 크루즈가 지금 차를 제어하고 있는가 */
    fun isEngaged(mode: CruiseMode?): Boolean = mode == CruiseMode.ACTIVATED

    /** 작동 → 비작동으로 바뀐 순간인가 */
    fun isDisengage(previous: CruiseMode?, current: CruiseMode?): Boolean =
        isEngaged(previous) && !isEngaged(current)
}
