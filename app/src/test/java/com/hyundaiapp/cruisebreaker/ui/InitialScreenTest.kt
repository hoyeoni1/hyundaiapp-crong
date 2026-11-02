package com.hyundaiapp.cruisebreaker.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class InitialScreenTest {
    @Test
    fun `release first launch and consent withdrawal lead to onboarding`() {
        assertEquals(Screen.ONBOARDING, initialScreen(debug = false, onboarded = false))
    }

    @Test
    fun `release returning user enters driving screen`() {
        assertEquals(Screen.DRIVING, initialScreen(debug = false, onboarded = true))
    }

    @Test
    fun `debug keeps development menu regardless of consent`() {
        assertEquals(Screen.DEV_MENU, initialScreen(debug = true, onboarded = false))
        assertEquals(Screen.DEV_MENU, initialScreen(debug = true, onboarded = true))
    }
}
