package com.locationjoystick.core.datastore

import com.locationjoystick.core.testing.FakePreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppPreferencesDataSourceTest {
    private lateinit var fakeDataStore: FakePreferencesDataStore
    private lateinit var dataSource: AppPreferencesDataSource

    @Before
    fun setUp() {
        fakeDataStore = FakePreferencesDataStore()
        dataSource = AppPreferencesDataSource(fakeDataStore)
    }

    @Test
    fun `returnHomeOnBackground defaults to true and survives data source recreation`() =
        runTest {
            assertTrue(dataSource.getReturnHomeOnBackground().first())
            dataSource.setReturnHomeOnBackground(false)
            val recreated = AppPreferencesDataSource(fakeDataStore)
            assertFalse(recreated.getReturnHomeOnBackground().first())
            recreated.setReturnHomeOnBackground(true)
            assertTrue(dataSource.getReturnHomeOnBackground().first())
        }

    @Test
    fun `snapshot applies background navigation and reset restores default`() =
        runTest {
            assertTrue(dataSource.getSettingsSnapshot().first().returnHomeOnBackground)
            dataSource.applySnapshot(dataSource.getSettingsSnapshot().first().copy(returnHomeOnBackground = false))
            assertFalse(dataSource.getReturnHomeOnBackground().first())
            assertFalse(dataSource.getSettingsSnapshot().first().returnHomeOnBackground)
            dataSource.clearAllExceptOnboarding()
            assertTrue(dataSource.getReturnHomeOnBackground().first())
        }

    @Test
    fun `hideTeleportFeatures defaults to false and round-trips true`() =
        runTest {
            assertFalse(dataSource.getHideTeleportFeatures().first())
            dataSource.setHideTeleportFeatures(true)
            assertTrue(dataSource.getHideTeleportFeatures().first())
        }

    @Test
    fun `hideWidgetOverlay defaults to false and round-trips true`() =
        runTest {
            assertFalse(dataSource.getHideWidgetOverlay().first())
            dataSource.setHideWidgetOverlay(true)
            assertTrue(dataSource.getHideWidgetOverlay().first())
        }

    @Test
    fun `hideForegroundNotification defaults to false and round-trips true`() =
        runTest {
            assertFalse(dataSource.getHideForegroundNotification().first())
            dataSource.setHideForegroundNotification(true)
            assertTrue(dataSource.getHideForegroundNotification().first())
        }

    @Test
    fun `showRouteJumpButtons defaults to false and round-trips true`() =
        runTest {
            assertFalse(dataSource.getShowRouteJumpButtons().first())
            dataSource.setShowRouteJumpButtons(true)
            assertTrue(dataSource.getShowRouteJumpButtons().first())
        }

    @Test
    fun `bypassMockLocationCheck defaults to false and round-trips true`() =
        runTest {
            assertFalse(dataSource.getBypassMockLocationCheck().first())
            dataSource.setBypassMockLocationCheck(true)
            assertTrue(dataSource.getBypassMockLocationCheck().first())
        }
}
