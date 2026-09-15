package com.locationjoystick.core.common.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.locationjoystick.core.common.constants.AppConstants.LocaleConstants
import java.util.Locale

/**
 * Wraps a base [Context] with the user's chosen per-app language on API 28-32, where the
 * framework's own LocaleManager only applies to API 33+. A no-op passthrough on API 33+ (the
 * framework already handles every context there) and when no language has been chosen (falls
 * back to the base context, i.e. system language).
 */
object LocaleContextWrapper {
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base

        val tag =
            base
                .getSharedPreferences(LocaleConstants.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(LocaleConstants.KEY_LANGUAGE_TAG, null)
                ?: return base

        val locale = Locale.forLanguageTag(tag)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
