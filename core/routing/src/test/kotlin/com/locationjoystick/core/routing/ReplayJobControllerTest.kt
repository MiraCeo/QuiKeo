package com.locationjoystick.core.routing

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class ReplayJobControllerTest {
    @Test
    fun `launch runs the block and reports isRunning`() =
        runBlocking {
            val controller = ReplayJobController("test")
            val ticks = AtomicInteger(0)

            controller.launch {
                while (isActive) {
                    ticks.incrementAndGet()
                    delay(20)
                }
            }
            delay(60)
            assertTrue("job should be running", controller.isRunning)
            assertTrue("block should have ticked", ticks.get() > 0)

            controller.cancel()
            delay(20)
            assertFalse("job should no longer be running after cancel", controller.isRunning)

            controller.close()
        }

    @Test
    fun `launch cancels and joins the previous job before starting the new one`() =
        runBlocking {
            val controller = ReplayJobController("test")
            val order = mutableListOf<String>()

            controller.launch {
                try {
                    while (isActive) delay(10)
                } finally {
                    order.add("first-cancelled")
                }
            }
            delay(20)

            controller.launch {
                order.add("second-started")
            }
            delay(50)

            assertTrue(order.indexOf("first-cancelled") < order.indexOf("second-started"))
            controller.close()
        }
}
