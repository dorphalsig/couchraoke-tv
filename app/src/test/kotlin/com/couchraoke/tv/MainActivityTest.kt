package com.couchraoke.tv

import android.net.wifi.WifiManager
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainActivityTest {
    @Test(timeout = 30_000)
    fun activityStartAcquiresAndStopReleasesMulticastLock() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        val multicastLockField = MainActivity::class.java.getDeclaredField("multicastLock").apply {
            isAccessible = true
        }

        assertNotNull(multicastLockField.get(activity) as WifiManager.MulticastLock?)

        controller.stop()

        assertNull(multicastLockField.get(activity))
    }
}
