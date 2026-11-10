package com.hyundaiapp.crong.ui

import com.hyundaiapp.crong.ui.components.NotificationAction
import com.hyundaiapp.crong.ui.components.notificationAction
import com.hyundaiapp.crong.ui.components.notificationDecisionSeen
import com.hyundaiapp.crong.ui.components.notificationUnresolvedResults
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPermissionTest {
    @Test fun `first request must open dialog rather than settings`() {
        assertEquals(NotificationAction.REQUEST, notificationAction(34, false, false, false))
    }
    @Test fun `denial with rationale permits a second request`() {
        assertEquals(NotificationAction.REQUEST, notificationAction(34, false, true, true))
    }
    @Test fun `denial without rationale routes explicit tap to settings`() {
        assertEquals(NotificationAction.SETTINGS, notificationAction(34, false, true, false))
    }
    @Test fun `granted and pre Android 13 need no request`() {
        assertEquals(NotificationAction.NONE, notificationAction(34, true, true, false))
        assertEquals(NotificationAction.NONE, notificationAction(32, false, false, false))
    }
    @Test fun `dismissed first dialog can be requested again`() {
        val seen = notificationDecisionSeen(false, granted = false, showRationale = false)
        assertEquals(false, seen)
        assertEquals(NotificationAction.REQUEST, notificationAction(34, false, seen, false))
    }
    @Test fun `actual denial followed by permanent denial still offers settings`() {
        val seen = notificationDecisionSeen(false, granted = false, showRationale = true)
        assertEquals(NotificationAction.REQUEST, notificationAction(34, false, seen, true))
        val deniedAgain = notificationDecisionSeen(seen, granted = false, showRationale = false)
        assertEquals(NotificationAction.SETTINGS, notificationAction(34, false, deniedAgain, false))
    }
    @Test fun `pending dialog keeps request label until a result arrives`() {
        assertEquals(NotificationAction.REQUEST, notificationAction(34, false, true, false, inFlight = true))
    }

    @Test fun `missing denial history falls back to settings after two unresolved requests`() {
        val first = notificationUnresolvedResults(0, granted = false, showRationale = false)
        assertEquals(NotificationAction.REQUEST, notificationAction(34, false, false, false, unresolvedResults = first))
        val second = notificationUnresolvedResults(first, granted = false, showRationale = false)
        assertEquals(NotificationAction.SETTINGS, notificationAction(34, false, false, false, unresolvedResults = second))
        assertEquals(NotificationAction.REQUEST, notificationAction(34, false, false, false, inFlight = true, unresolvedResults = second))
    }
    @Test fun `confirmed permission result clears unresolved requests`() {
        assertEquals(0, notificationUnresolvedResults(2, granted = true, showRationale = false))
        val count = notificationUnresolvedResults(2, granted = false, showRationale = true)
        assertEquals(0, count)
        assertEquals(NotificationAction.REQUEST, notificationAction(34, false, true, true, unresolvedResults = count))
    }

}
