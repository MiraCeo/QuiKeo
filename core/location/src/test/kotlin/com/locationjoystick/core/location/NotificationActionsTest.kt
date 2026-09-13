package com.locationjoystick.core.location

import com.locationjoystick.core.common.constants.AppConstants
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationActionsTest {
    @Test
    fun `not replay active - shows Stop Map Favorites`() {
        val actions = selectNotificationActions(replayActive = false, replayPaused = false)
        assertEquals(3, actions.size)
        assertEquals(R.string.notification_action_stop, actions[0].labelRes)
        assertEquals(R.string.notification_action_map, actions[1].labelRes)
        assertEquals(R.string.notification_action_favorites, actions[2].labelRes)
    }

    @Test
    fun `not replay active with replayPaused true - shows Stop Map Favorites`() {
        // replayPaused=true is meaningless when replayActive=false, but must not crash
        val actions = selectNotificationActions(replayActive = false, replayPaused = true)
        assertEquals(3, actions.size)
        assertEquals(R.string.notification_action_stop, actions[0].labelRes)
        assertEquals(R.string.notification_action_map, actions[1].labelRes)
        assertEquals(R.string.notification_action_favorites, actions[2].labelRes)
    }

    @Test
    fun `replay active not paused - shows Stop Pause Map`() {
        val actions = selectNotificationActions(replayActive = true, replayPaused = false)
        assertEquals(3, actions.size)
        assertEquals(R.string.notification_action_stop, actions[0].labelRes)
        assertEquals(R.string.notification_action_pause, actions[1].labelRes)
        assertEquals(R.string.notification_action_map, actions[2].labelRes)
    }

    @Test
    fun `replay active and paused - shows Stop Resume Map`() {
        val actions = selectNotificationActions(replayActive = true, replayPaused = true)
        assertEquals(3, actions.size)
        assertEquals(R.string.notification_action_stop, actions[0].labelRes)
        assertEquals(R.string.notification_action_resume, actions[1].labelRes)
        assertEquals(R.string.notification_action_map, actions[2].labelRes)
    }

    @Test
    fun `action enums are correct for non-replay`() {
        val actions = selectNotificationActions(replayActive = false, replayPaused = false)
        assertEquals(NotificationAction.STOP, actions[0].action)
        assertEquals(NotificationAction.NAV_MAP, actions[1].action)
        assertEquals(NotificationAction.NAV_FAVORITES, actions[2].action)
    }

    @Test
    fun `action enums are correct for replay paused`() {
        val actions = selectNotificationActions(replayActive = true, replayPaused = true)
        assertEquals(NotificationAction.STOP, actions[0].action)
        assertEquals(NotificationAction.RESUME, actions[1].action)
        assertEquals(NotificationAction.NAV_MAP, actions[2].action)
    }

    @Test
    fun `action enums are correct for replay running`() {
        val actions = selectNotificationActions(replayActive = true, replayPaused = false)
        assertEquals(NotificationAction.STOP, actions[0].action)
        assertEquals(NotificationAction.PAUSE, actions[1].action)
        assertEquals(NotificationAction.NAV_MAP, actions[2].action)
    }

    @Test
    fun `notificationChannelId - not hidden uses active channel`() {
        assertEquals(AppConstants.NotificationConstants.CHANNEL_ID_ACTIVE, notificationChannelId(hideNotification = false))
    }

    @Test
    fun `notificationChannelId - hidden uses minimized channel`() {
        assertEquals(
            AppConstants.NotificationConstants.CHANNEL_ID_ACTIVE_MINIMIZED,
            notificationChannelId(hideNotification = true),
        )
    }
}
