package com.hyundaiapp.core.probe

import ai.pleos.playground.vehicle.constant.control.CruiseControl
import ai.pleos.playground.navi.constants.NaviMode
import ai.pleos.playground.navi.constants.RouteOption
import ai.pleos.playground.navi.constants.TrafficCongestionType
import ai.pleos.playground.navi.data.*
import org.junit.Assert.*
import org.junit.Test

class SdkSignalMappingTest {
    @Test fun allActualSdkCruiseEnumsMapWithoutCollapsingOverrideStates() {
        val actualSdkValues = CruiseControl.values()
        assertEquals(6, actualSdkValues.size)
        actualSdkValues.forEach { assertEquals(it.name, it.toCore().name) }
        assertEquals(CruiseControlState.USER_OVERRIDE, CruiseControl.USER_OVERRIDE.toCore())
    }

    @Test fun signedSpeedConversionIsFiniteAndOnlyAppliedOnce() {
        assertEquals(100f, SignalUnits.metersPerSecondToKph(100f / 3.6f), 0.001f)
        assertEquals(-36f, SignalUnits.metersPerSecondToKph(-10f), 0.001f)
        assertThrows(IllegalArgumentException::class.java) { SignalUnits.metersPerSecondToKph(Float.POSITIVE_INFINITY) }
    }

    @Test fun locationPreservesHeadingAndRouteContextWithoutUnitGuessing() {
        val sdk = CurrentLocationInfo(37.3948, 127.1112, 270.5, "address", "route-1", 12)
        val core = sdk.toCore()
        assertEquals(GeoPoint(37.3948, 127.1112), core.point)
        assertEquals(270.5, core.heading)
        assertEquals("route-1", core.routeId)
        assertEquals(12, core.currentRouteIndex)
    }

    @Test fun routeTrafficPreservesRawValuesAndClearsOutsideGuidance() {
        val traffic = TrafficStatus(TrafficCongestionSummary(100.0, 70L, 15.0), listOf(
            TrafficCongestionInfo(25.0, 100.0, 70L, TrafficCongestionType.HEAVY, 15.0, "road", "a", "b")
        ), emptyList())
        val sdk = RouteStateInfo(true, NaviMode.ROUTE_GUIDING, false, RouteOption.RECOMMENDED, 50, emptyList(), traffic)
        val core = sdk.toCore()!!
        assertEquals(TrafficSection(25.0, 100.0, 70L, TrafficCongestion.HEAVY, 15.0, "road", "a", "b"), core.sections.single())
        assertNull(sdk.copy(naviMode = NaviMode.SAFE_GUIDING).toCore())
        assertEquals(true, sdk.copy(isRouting = true).toCore()!!.isRouting)
        assertNull(sdk.copy(termsAgreement = false).toCore())
    }
}
