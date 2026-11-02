package com.hyundaiapp.core.probe

import org.junit.Assert.*
import org.junit.Test

class ManagedVehicleSignalsTest {
    private class Connector(
        override val signals: Set<VehicleSignal> = setOf(VehicleSignal.CRUISE_STATE),
        private val startError: Exception? = null,
        private val stopError: Exception? = null,
    ) : SignalConnector {
        lateinit var sink: SignalSink
        var starts = 0
        var stops = 0
        override fun start(sink: SignalSink) {
            this.sink = sink
            starts++
            startError?.let { throw it }
        }
        override fun stop() { stops++; stopError?.let { throw it } }
    }

    @Test fun oldCallbacksCannotResurrectStoppedOrRestartedSessions() {
        val generations = mutableListOf<Connector>()
        val real = ManagedVehicleSignals({ listOf(Connector().also(generations::add)) }, { 100L })
        real.start()
        val first = generations.single()
        first.sink.value(SignalKeys.cruise, CruiseControlState.ACTIVATED)
        real.start()
        assertEquals(1, first.starts)
        real.stop()
        first.sink.value(SignalKeys.cruise, CruiseControlState.USER_OVERRIDE)
        assertEquals(SignalLifecycle.STOPPED, real.snapshot.value.lifecycle)
        assertNull(real.snapshot.value.cruiseState.value)
        real.start()
        generations.last().sink.value(SignalKeys.cruise, CruiseControlState.ACTIVATED)
        first.sink.fail(first.signals, SignalStatus.ERROR, "late failure")
        first.sink.value(SignalKeys.cruise, CruiseControlState.ENABLED)
        assertEquals(CruiseControlState.ACTIVATED, real.snapshot.value.cruiseState.value)
        assertEquals(1L, real.snapshot.value.cruiseState.sequence)
        real.stop()
        real.stop()
        assertEquals(1, first.stops)
        assertEquals(1, generations.last().stops)
    }

    @Test fun oneSourceFailureDoesNotPreventOtherSourcesOrCleanup() {
        val failed = Connector(startError = SecurityException("denied"), stopError = IllegalStateException("release"))
        val healthy = Connector(setOf(VehicleSignal.LOCATION))
        val real = ManagedVehicleSignals({ listOf(failed, healthy) }, { 0L })
        real.start()
        healthy.sink.value(SignalKeys.location, LocationObservation(GeoPoint(37.0, 127.0)))
        assertEquals(SignalStatus.PERMISSION_DENIED, real.snapshot.value.cruiseState.status)
        assertNull(real.snapshot.value.cruiseState.errorMessage)
        assertEquals(SignalStatus.AVAILABLE, real.snapshot.value.location.status)
        real.stop()
        assertEquals(1, healthy.stops)
        assertEquals(SignalLifecycle.STOPPED, real.snapshot.value.lifecycle)
    }

    @Test fun malformedRawValuesBecomeErrorsAndNullIsNotADeactivation() {
        val source = Connector()
        val real = ManagedVehicleSignals({ listOf(source) }, { 25L })
        real.start()
        source.sink.value(SignalKeys.soc, 110)
        source.sink.value(SignalKeys.speed, Float.NaN)
        assertEquals(SignalStatus.ERROR, real.snapshot.value.socPercent.status)
        assertEquals(SignalStatus.ERROR, real.snapshot.value.vehicleSpeedKph.status)
        source.sink.value(SignalKeys.cruise, CruiseControlState.ACTIVATED)
        source.sink.value(SignalKeys.cruise, null)
        assertEquals(SignalStatus.NOT_RECEIVED, real.snapshot.value.cruiseState.status)
        assertNull(real.snapshot.value.cruiseState.value)
        source.sink.value(SignalKeys.soc, 50)
        source.sink.value(SignalKeys.soc, 50)
        assertEquals(3L, real.snapshot.value.socPercent.sequence)
        assertEquals(25L, real.snapshot.value.socPercent.observedAtMillis)
        assertNull(real.snapshot.value.socPercent.errorMessage)
    }

    @Test fun permissionProfilesOnlyRequestEachAppsSignals() {
        assertEquals(setOf(SignalPermissions.ENERGY, SignalPermissions.NAVIGATION), SignalPermissions.required(PleosSignalOptions.chachelin()))
        assertEquals(setOf(SignalPermissions.DRIVE, SignalPermissions.NAVIGATION, SignalPermissions.SPEED), SignalPermissions.required(PleosSignalOptions.cruiseBreaker()))
    }
}
