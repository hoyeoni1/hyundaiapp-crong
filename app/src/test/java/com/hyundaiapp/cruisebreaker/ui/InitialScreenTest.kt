package com.hyundaiapp.cruisebreaker.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class InitialScreenTest {
    @Test
    fun `release first launch and consent withdrawal lead to onboarding`() {
        assertEquals(Screen.ONBOARDING, initialScreen(debug = false, onboarded = false))
    }

    @Test
    fun `release returning user can reach parked risk list and settings`() {
        assertEquals(Screen.RISK_LIST, initialScreen(debug = false, onboarded = true))
    }

    @Test
    fun `debug starts with the same real flow as release`() {
        assertEquals(Screen.ONBOARDING, initialScreen(debug = true, onboarded = false))
        assertEquals(Screen.RISK_LIST, initialScreen(debug = true, onboarded = true))
    }
}
