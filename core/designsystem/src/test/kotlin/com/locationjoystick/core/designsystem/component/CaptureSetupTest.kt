package com.locationjoystick.core.designsystem.component

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureSetupTest {
    @Test
    fun `ready when capture or jump is on and this app is the default browser`() {
        assertFalse(isCaptureReady(false, captureEnabled = false, jumpEnabled = false, isDefaultBrowser = false))
        assertFalse(isCaptureReady(true, captureEnabled = true, jumpEnabled = false, isDefaultBrowser = false))
        assertFalse(isCaptureReady(true, captureEnabled = false, jumpEnabled = false, isDefaultBrowser = true))
        assertFalse(isCaptureReady(false, captureEnabled = true, jumpEnabled = true, isDefaultBrowser = true))
        assertTrue(isCaptureReady(true, captureEnabled = true, jumpEnabled = false, isDefaultBrowser = true))
        assertTrue(isCaptureReady(true, captureEnabled = false, jumpEnabled = true, isDefaultBrowser = true))
    }
}
