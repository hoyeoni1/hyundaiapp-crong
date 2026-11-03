package com.hyundaiapp.cruisebreaker.ui

import com.hyundaiapp.core.DriveState
import com.hyundaiapp.core.Gear
import org.junit.Assert.*
import org.junit.Test

class ScreenPolicyTest {
    @Test fun `parked returning user can reach every stationary screen without speed permission`() {
        for (screen in listOf(Screen.RISK_LIST, Screen.TRIP_SUMMARY, Screen.SETTINGS, Screen.ONBOARDING)) {
            assertEquals(screen, visibleScreen(screen, DriveState(gear = Gear.PARK), false))
        }
    }

    @Test fun `moving reversing neutral and unknown signals show touch free driving screen`() {
        for (gear in listOf(Gear.DRIVE, Gear.REVERSE, Gear.NEUTRAL, Gear.UNKNOWN, null)) {
            for (screen in Screen.entries) {
                assertEquals(Screen.DRIVING, visibleScreen(screen, DriveState(gear = gear, speedKph = 0f), null))
            }
        }
    }

    @Test fun `contradictory park with nonzero or invalid speed does not expose controls`() {
        for (speed in listOf(1f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertEquals(Screen.DRIVING, visibleScreen(Screen.SETTINGS, DriveState(Gear.PARK, speed), false))
        }
    }

    @Test fun `first consent remains reachable before recording starts`() {
        assertEquals(Screen.ONBOARDING, visibleScreen(Screen.ONBOARDING, DriveState(), false))
    }

    @Test fun `platform can allow settings with missing gear after consent`() {
        assertEquals(Screen.SETTINGS, visibleScreen(Screen.SETTINGS, DriveState(), false))
        assertEquals(Screen.SETTINGS, visibleScreen(Screen.SETTINGS, DriveState(Gear.UNKNOWN), false))
    }

    @Test fun `platform restrictions also hide consent before vehicle collection starts`() {
        assertEquals(Screen.DRIVING, visibleScreen(Screen.ONBOARDING, DriveState(), true))
        assertEquals(Screen.DRIVING, visibleScreen(Screen.ONBOARDING, DriveState(), null))
        assertEquals(Screen.DRIVING, visibleScreen(Screen.SETTINGS, DriveState(Gear.PARK, 0f), true))
    }

    @Test fun `D through neutral to park opens summary once`() {
        val trip = TripEndNavigation()
        assertFalse(trip.shouldShowSummary(true, DriveState(Gear.PARK, 0f)))
        assertFalse(trip.shouldShowSummary(true, DriveState(Gear.DRIVE, 100f)))
        assertFalse(trip.shouldShowSummary(true, DriveState(Gear.NEUTRAL, 0f)))
        assertFalse(trip.shouldShowSummary(true, DriveState()))
        assertTrue(trip.shouldShowSummary(true, DriveState(Gear.PARK, 0f)))
        assertFalse(trip.shouldShowSummary(true, DriveState(Gear.PARK, 0f)))
        assertFalse(trip.shouldShowSummary(true, DriveState(Gear.DRIVE, 80f)))
        assertTrue(trip.shouldShowSummary(true, DriveState(Gear.PARK)))
    }

    @Test fun `consent withdrawal clears pending trip summary`() {
        val trip = TripEndNavigation()
        trip.shouldShowSummary(true, DriveState(Gear.DRIVE, 100f))
        assertFalse(trip.shouldShowSummary(false, DriveState(Gear.PARK, 0f)))
        assertFalse(trip.shouldShowSummary(true, DriveState(Gear.PARK, 0f)))
    }
}
