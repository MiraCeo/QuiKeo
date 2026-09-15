package com.locationjoystick.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.locationjoystick.core.common.constants.AppConstants
import com.locationjoystick.core.designsystem.R

/**
 * Localized name for a speed preset, resolved from its stable id (see
 * [AppConstants.ProfileConstants]) rather than [com.locationjoystick.core.model.SpeedProfile.name],
 * which is an English identifier owned by the data layer and also written into exported settings
 * files.
 */
@Composable
fun speedProfileLabel(id: String): String =
    when (id) {
        AppConstants.ProfileConstants.PROFILE_ID_SLOW_WALK -> stringResource(R.string.speed_profile_slow_walk)
        AppConstants.ProfileConstants.PROFILE_ID_WALK -> stringResource(R.string.speed_profile_walk)
        AppConstants.ProfileConstants.PROFILE_ID_RUN -> stringResource(R.string.speed_profile_run)
        AppConstants.ProfileConstants.PROFILE_ID_BIKE -> stringResource(R.string.speed_profile_bike)
        AppConstants.ProfileConstants.PROFILE_ID_DRIVE -> stringResource(R.string.speed_profile_drive)
        else -> id
    }
