package com.locationjoystick.app

import com.locationjoystick.feature.onboarding.api.ONBOARDING_ROUTE
import com.locationjoystick.feature.settings.api.SETTINGS_ROUTE

/** Preserves the existing exemptions for Home, setup and Settings/file-picker flows. */
internal fun shouldReturnHomeOnBackground(
    currentRoute: String?,
    returnHomeOnBackground: Boolean?,
): Boolean =
    returnHomeOnBackground == true &&
        currentRoute != null &&
        currentRoute != IDLE_ROUTE &&
        currentRoute != ONBOARDING_ROUTE &&
        currentRoute != SETTINGS_ROUTE
