package com.locationjoystick.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locationjoystick.core.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Keeps the latest saved navigation preference available to background lifecycle events. */
@HiltViewModel
class AppNavigationViewModel
    @Inject
    constructor(
        settingsRepository: SettingsRepository,
    ) : ViewModel() {
        // Null means not loaded: never discard a restored page based on a guessed default.
        // Eager collection stays current even when Compose's STARTED collectors are suspended.
        val returnHomeOnBackground: StateFlow<Boolean?> =
            settingsRepository.getReturnHomeOnBackground().stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = null,
            )
    }
