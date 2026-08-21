package com.pim.shell

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ServerSettingsStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before fun setUp() {
        ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("shell_settings", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun `saved url roundtrips normalized`() {
        val store = ServerSettingsStore(context)
        store.saveServerUrl("pim.example.com/")
        assertEquals("https://pim.example.com", store.serverUrl())
    }
    @Test fun `invalid url is not saved`() {
        val store = ServerSettingsStore(context)
        assertNull(store.saveServerUrl("ftp://x"))
        assertNull(store.serverUrl())
    }
    @Test fun `clear removes url`() {
        val store = ServerSettingsStore(context)
        store.saveServerUrl("https://pim.example.com")
        store.clear()
        assertNull(store.serverUrl())
    }
}
