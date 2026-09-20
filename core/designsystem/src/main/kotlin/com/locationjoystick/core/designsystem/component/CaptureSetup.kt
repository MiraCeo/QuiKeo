package com.locationjoystick.core.designsystem.component

internal fun isCaptureReady(
    captureModeEnabled: Boolean,
    captureEnabled: Boolean,
    jumpEnabled: Boolean,
    isDefaultBrowser: Boolean,
): Boolean = captureModeEnabled && (captureEnabled || jumpEnabled) && isDefaultBrowser
