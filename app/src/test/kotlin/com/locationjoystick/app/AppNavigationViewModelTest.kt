package com.locationjoystick.app

import androidx.lifecycle.ViewModelStore
import com.locationjoystick.core.data.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppNavigationViewModelTest {
    @Test
    fun `preference stays unloaded until read and updates without a UI subscriber`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val store = ViewModelStore()
            try {
                val storedPreference = MutableSharedFlow<Boolean>(replay = 1)
                val repository = mockk<SettingsRepository>()
                every { repository.getReturnHomeOnBackground() } returns storedPreference
                val viewModel = AppNavigationViewModel(repository)
                store.put("navigation", viewModel)
                assertNull(viewModel.returnHomeOnBackground.value)
                runCurrent()
                storedPreference.emit(false)
                runCurrent()
                assertEquals(false, viewModel.returnHomeOnBackground.value)
                storedPreference.emit(true)
                runCurrent()
                assertEquals(true, viewModel.returnHomeOnBackground.value)
                storedPreference.emit(false)
                runCurrent()
                assertEquals(false, viewModel.returnHomeOnBackground.value)
            } finally {
                store.clear()
                Dispatchers.resetMain()
            }
        }
}
