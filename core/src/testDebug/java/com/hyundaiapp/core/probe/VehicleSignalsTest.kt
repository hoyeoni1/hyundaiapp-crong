package com.hyundaiapp.core.probe

import org.junit.Assert.*
import org.junit.Test

class VehicleSignalsTest {
    @Test fun lifecycleDoesNotProduceSyntheticDeactivation() {
        val fake = FakeVehicleSignals { 100L }
        assertEquals(SignalLifecycle.STOPPED, fake.snapshot.value.lifecycle)
        assertNull(fake.snapshot.value.location.value)
        fake.start()
        fake.setCruiseState(CruiseControlState.ACTIVATED)
        fake.start()
        assertEquals(CruiseControlState.ACTIVATED, fake.snapshot.value.cruiseState.value)
        fake.stop()
        fake.stop()
        assertEquals(SignalStatus.NOT_RECEIVED, fake.snapshot.value.cruiseState.status)
        assertNull(fake.snapshot.value.cruiseState.value)
        fake.start()
        assertEquals(0L, fake.snapshot.value.cruiseState.sequence)
    }

    @Test fun repeatedValuesStillHaveDistinctObservationIdentity() {
        val fake = FakeVehicleSignals { 42L }
        fake.start()
        fake.setBrakePressed(false)
        val first = fake.snapshot.value.brakePressed
        fake.setBrakePressed(false)
        val second = fake.snapshot.value.brakePressed
        assertEquals(false, second.value)
        assertEquals(42L, second.observedAtMillis)
        assertEquals(first.sequence + 1, second.sequence)
        assertNotEquals(first, second)
    }

    @Test fun permissionFailureIsIndependentAndRecoverable() {
        val fake = FakeVehicleSignals { 10L }
        fake.start()
        fake.setVehicleSpeedKph(100f)
        fake.setCruiseState(CruiseControlState.ACTIVATED)
        fake.setUnavailable(VehicleSignal.VEHICLE_SPEED, SignalStatus.PERMISSION_DENIED)
        assertNull(fake.snapshot.value.vehicleSpeedKph.value)
        assertEquals(SignalStatus.PERMISSION_DENIED, fake.snapshot.value.vehicleSpeedKph.status)
        assertEquals(CruiseControlState.ACTIVATED, fake.snapshot.value.cruiseState.value)
        fake.setVehicleSpeedKph(80f)
        fake.setUnavailable(VehicleSignal.CRUISE_STATE, SignalStatus.NOT_RECEIVED)
        assertNull(fake.snapshot.value.cruiseState.value)
        assertEquals(SignalStatus.NOT_RECEIVED, fake.snapshot.value.cruiseState.status)
        assertEquals(SignalStatus.AVAILABLE, fake.snapshot.value.vehicleSpeedKph.status)
        assertEquals(3L, fake.snapshot.value.vehicleSpeedKph.sequence)
    }

    @Test fun emptyTrafficIsDifferentFromMissingTraffic() {
        val fake = FakeVehicleSignals { 0L }
        fake.start()
        assertEquals(SignalStatus.NOT_RECEIVED, fake.snapshot.value.routeTraffic.status)
        val source = mutableListOf<TrafficSection>()
        fake.setRouteTraffic(RouteTraffic(source))
        source.add(TrafficSection(0.0, 100.0, 60L, TrafficCongestion.HEAVY, 10.0, "road", "a", "b"))
        assertEquals(SignalStatus.AVAILABLE, fake.snapshot.value.routeTraffic.status)
        assertTrue(fake.snapshot.value.routeTraffic.value!!.sections.isEmpty())
    }

    @Test fun chargingAndCruiseInputsKeepSdkCompatibleValues() {
        val fake = FakeVehicleSignals { 0L }
        fake.start()
        fake.setChargePortOpen(true)
        fake.setSocPercent(40)
        fake.setChargeSwitchOn(true)
        fake.setSocPercent(60)
        fake.setChargePortOpen(false)
        fake.setCruiseTargetSpeedKph(100f)
        fake.setCruiseState(CruiseControlState.USER_OVERRIDE)
        fake.setLocation(LocationObservation(GeoPoint(37.3948, 127.1112), 90.0))
        assertEquals(60, fake.snapshot.value.socPercent.value)
        assertEquals(false, fake.snapshot.value.chargePortOpen.value)
        assertEquals(100f, fake.snapshot.value.cruiseTargetSpeedKph.value)
        assertEquals(CruiseControlState.USER_OVERRIDE, fake.snapshot.value.cruiseState.value)
        assertEquals(90.0, fake.snapshot.value.location.value!!.heading)
        assertEquals(60f, SignalUnits.metersPerSecondToKph(16.666668f), 0.001f)
    }

    @Test fun invalidInputsAndStoppedWritesAreRejected() {
        val fake = FakeVehicleSignals { 0L }
        assertThrows(IllegalStateException::class.java) { fake.setBrakePressed(true) }
        fake.start()
        assertThrows(IllegalArgumentException::class.java) { fake.setSocPercent(101) }
        assertThrows(IllegalArgumentException::class.java) { fake.setVehicleSpeedKph(Float.NaN) }
        assertThrows(IllegalArgumentException::class.java) { GeoPoint(91.0, 0.0) }
        assertThrows(IllegalArgumentException::class.java) { SignalReading(value = true) }
        fake.stop()
        assertThrows(IllegalStateException::class.java) { fake.setSocPercent(50) }
    }
}
