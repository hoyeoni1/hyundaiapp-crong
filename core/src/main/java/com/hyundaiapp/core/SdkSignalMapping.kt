package com.hyundaiapp.core

import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.constants.NaviMode
import ai.pleos.playground.vehicle.constant.control.CruiseControl

internal fun CruiseControl.toCore(): CruiseControlState = when (this) {
    CruiseControl.OTHER -> CruiseControlState.OTHER
    CruiseControl.ENABLED -> CruiseControlState.ENABLED
    CruiseControl.ACTIVATED -> CruiseControlState.ACTIVATED
    CruiseControl.USER_OVERRIDE -> CruiseControlState.USER_OVERRIDE
    CruiseControl.SUSPENDED -> CruiseControlState.SUSPENDED
    CruiseControl.FORCED_DEACTIVATION_WARNING -> CruiseControlState.FORCED_DEACTIVATION_WARNING
}

internal fun CurrentLocationInfo.toCore() = LocationObservation(
    GeoPoint(latitude, longitude), heading, routeId, currentRouteIndex,
)

/** No active route must not be reported as a confirmed congestion-free route. */
internal fun RouteStateInfo.toCore(): RouteTraffic? =
    if (!termsAgreement || naviMode != NaviMode.ROUTE_GUIDING) null
    else trafficStatus?.let { status ->
        RouteTraffic(status.congestionItemList.map { item ->
            TrafficSection(item.offset, item.distance, item.trafficCongestionTime,
                TrafficCongestion.valueOf(item.trafficCongestionType.name), item.avrSpeed,
                item.roadName, item.startCrossName, item.endCrossName)
        }, isRouting = isRouting)
    }
