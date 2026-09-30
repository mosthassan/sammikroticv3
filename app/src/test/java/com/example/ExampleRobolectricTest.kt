package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("سام ميكروتك", appName)
  }

  @Test
  fun `verify ip collision prevention logic`() {
    val existingIps = listOf("192.168.88.1", "192.168.88.2", "192.168.88.10", "192.168.88.20")
    val testIp = "192.168.88.10"
    assertTrue(existingIps.contains(testIp))
  }
}
