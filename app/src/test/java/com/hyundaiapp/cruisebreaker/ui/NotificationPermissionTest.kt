package com.hyundaiapp.cruisebreaker.ui

import com.hyundaiapp.cruisebreaker.ui.components.NotificationAction
import com.hyundaiapp.cruisebreaker.ui.components.notificationAction
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
}
