package com.hyundaiapp.cruisebreaker.ui

/**
 * 첫 실행은 동의 화면, 동의 뒤에는 S2 목록. 디버그 빌드도 같다(시연 영상·심사 모두 같은 화면, 11-05 장호연 결정).
 * 개발 메뉴는 디버그 빌드에서 S2 제목을 길게 눌러 연다.
 */
internal fun initialScreen(debug: Boolean, onboarded: Boolean): Screen = if (onboarded) Screen.RISK_LIST else Screen.ONBOARDING
