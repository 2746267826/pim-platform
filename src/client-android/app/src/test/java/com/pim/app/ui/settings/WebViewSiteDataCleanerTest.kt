package com.pim.app.ui.settings

import android.webkit.CookieManager
import com.pim.app.TestPimApp
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestPimApp::class)
class WebViewSiteDataCleanerTest {

    @Before
    fun setUp() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        ShadowLooper.idleMainLooper()
    }

    @After
    fun tearDown() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        ShadowLooper.idleMainLooper()
    }

    @Test
    fun clearOrigin_shouldOnlyDeleteCookiesForTargetOrigin_notOtherOrigins() {
        val cookieManager = CookieManager.getInstance()

        cookieManager.setCookie("https://old.example", "old_session=1")
        cookieManager.setCookie("https://other.example", "other_session=1")
        cookieManager.flush()
        ShadowLooper.idleMainLooper()

        // Verify preconditions
        val oldBefore = cookieManager.getCookie("https://old.example")
        assertNotNull("precondition: old_session must be set", oldBefore)
        assertTrue("precondition: old_session=1 visible for old.example", oldBefore!!.contains("old_session=1"))

        val otherBefore = cookieManager.getCookie("https://other.example")
        assertNotNull("precondition: other_session must be set", otherBefore)
        assertTrue("precondition: other_session=1 visible for other.example", otherBefore!!.contains("other_session=1"))

        // Act: clear only the old.example origin
        RealWebViewSiteDataCleaner().clearOrigin("https://old.example")
        ShadowLooper.idleMainLooper()

        // Target origin cookie should be removed
        val oldAfter = cookieManager.getCookie("https://old.example")
        assertTrue("old_session value must be cleared from old.example", oldAfter.isNullOrBlank() || !oldAfter!!.contains("old_session=1"))

        val otherAfter = cookieManager.getCookie("https://other.example")
        assertNotNull("other.example cookies must survive clearOrigin of a different origin", otherAfter)
        assertTrue("other.example must retain other_session=1", otherAfter!!.contains("other_session=1"))
    }
}
