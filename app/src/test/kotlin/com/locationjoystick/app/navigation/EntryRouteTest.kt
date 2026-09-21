package com.locationjoystick.app.navigation

import com.locationjoystick.app.IDLE_ROUTE
import com.locationjoystick.core.model.MockLocationState
import com.locationjoystick.feature.map.api.MAP_ROUTE
import org.junit.Assert.assertEquals
import org.junit.Test

class EntryRouteTest {
    @Test
    fun `running opens map`() = assertEquals(MAP_ROUTE, entryRouteFor(MockLocationState.RUNNING))

    @Test
    fun `paused opens map`() = assertEquals(MAP_ROUTE, entryRouteFor(MockLocationState.PAUSED))

    @Test
    fun `idle opens home`() = assertEquals(IDLE_ROUTE, entryRouteFor(MockLocationState.IDLE))

    @Test
    fun `error opens home`() = assertEquals(IDLE_ROUTE, entryRouteFor(MockLocationState.ERROR))
}
