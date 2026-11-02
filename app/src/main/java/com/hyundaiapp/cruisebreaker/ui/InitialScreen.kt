package com.hyundaiapp.cruisebreaker.ui

/** 디버그 개발 메뉴를 유지하고, 릴리스 첫 실행은 반드시 동의 화면으로 보낸다. */
internal fun initialScreen(debug: Boolean, onboarded: Boolean): Screen = when {
    debug -> Screen.DEV_MENU
    onboarded -> Screen.DRIVING
    else -> Screen.ONBOARDING
}
