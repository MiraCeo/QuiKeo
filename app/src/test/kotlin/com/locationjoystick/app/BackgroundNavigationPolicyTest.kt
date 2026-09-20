package com.locationjoystick.app

import com.locationjoystick.feature.favorites.api.FAVORITES_ROUTE
import com.locationjoystick.feature.group.api.GROUP_ROUTE
import com.locationjoystick.feature.map.api.MAP_ROUTE
import com.locationjoystick.feature.onboarding.api.ONBOARDING_ROUTE
import com.locationjoystick.feature.routes.api.ROUTES_ROUTE
import com.locationjoystick.feature.settings.api.SETTINGS_ROUTE
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundNavigationPolicyTest {
    private val contentRoutes = listOf(MAP_ROUTE, FAVORITES_ROUTE, ROUTES_ROUTE, GROUP_ROUTE, "route_creator/STRAIGHT")

    @Test
    fun `enabled preserves the legacy redirect for content pages`() {
        contentRoutes.forEach { assertTrue(shouldReturnHomeOnBackground(it, true)) }
    }

    @Test
    fun `disabled preserves content pages`() {
        contentRoutes.forEach { assertFalse(shouldReturnHomeOnBackground(it, false)) }
    }

    @Test
    fun `home setup settings and uninitialized destinations are never redirected`() {
        listOf(IDLE_ROUTE, ONBOARDING_ROUTE, SETTINGS_ROUTE, null).forEach { route ->
            assertFalse(shouldReturnHomeOnBackground(route, true))
            assertFalse(shouldReturnHomeOnBackground(route, false))
        }
    }

    @Test
    fun `unloaded preference never discards a restored page`() {
        contentRoutes.forEach { assertFalse(shouldReturnHomeOnBackground(it, null)) }
    }
}
