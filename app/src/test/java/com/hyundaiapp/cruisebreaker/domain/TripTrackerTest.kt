package com.hyundaiapp.cruisebreaker.domain

import com.hyundaiapp.core.FakeVehicleSignals
import com.hyundaiapp.core.Gear
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TripTrackerTest {
    private val fake = FakeVehicleSignals()

    private fun gear(g: Gear) = fake.setDrive { copy(gear = g) }

    @Test
    fun `D로 출발해서 P까지가 한 주행`() = runTest(UnconfinedTestDispatcher()) {
        val trips = TripTracker(fake)
        trips.start(backgroundScope)
        assertEquals(0, trips.tripNumber.value)

        gear(Gear.DRIVE)
        assertEquals(1, trips.tripNumber.value)
        assertTrue(trips.driving.value)

        gear(Gear.REVERSE)
        gear(Gear.DRIVE)
        assertEquals(1, trips.tripNumber.value)

        gear(Gear.PARK)
        assertFalse(trips.driving.value)
        gear(Gear.DRIVE)
        assertEquals(2, trips.tripNumber.value)
    }
}
