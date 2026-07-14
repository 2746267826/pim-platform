package com.pim.app.status

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkCapabilities
import org.robolectric.shadows.ShadowNetworkInfo

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NetworkStatusProviderTest {

    @Test
    fun isConnectedForActiveNetworkFalseWhenNoActiveNetwork() {
        val cm = connectivityManager()
        val shadow = shadowOf(cm)
        shadow.clearAllNetworks()
        shadow.setDefaultNetworkActive(false)
        assertFalse(NetworkStatusProvider.isConnectedForActiveNetwork(cm))
    }

    @Test
    fun isConnectedForActiveNetworkTrueWhenActiveHasInternet() {
        val cm = connectivityManager()
        val shadow = shadowOf(cm)

        val wifiNet = ShadowNetwork.newInstance(1)
        val wifiInfo = ShadowNetworkInfo.newInstance(
            NetworkInfo.DetailedState.CONNECTED, ConnectivityManager.TYPE_WIFI, 0, true, true
        )
        shadow.addNetwork(wifiNet, wifiInfo)
        shadow.setActiveNetworkInfo(wifiInfo)

        val caps = ShadowNetworkCapabilities.newInstance()
        shadowOf(caps).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        shadow.setNetworkCapabilities(wifiNet, caps)

        assertTrue(NetworkStatusProvider.isConnectedForActiveNetwork(cm))
    }

    @Test
    fun isConnectedForActiveNetworkFalseWhenActiveHasNoInternet() {
        val cm = connectivityManager()
        val shadow = shadowOf(cm)

        val wifiNet = ShadowNetwork.newInstance(1)
        val wifiInfo = ShadowNetworkInfo.newInstance(
            NetworkInfo.DetailedState.CONNECTED, ConnectivityManager.TYPE_WIFI, 0, true, true
        )
        shadow.addNetwork(wifiNet, wifiInfo)
        shadow.setActiveNetworkInfo(wifiInfo)

        val caps = ShadowNetworkCapabilities.newInstance()
        shadowOf(caps).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        shadow.setNetworkCapabilities(wifiNet, caps)

        assertFalse(NetworkStatusProvider.isConnectedForActiveNetwork(cm))
    }

    @Test
    fun isConnectedForActiveNetworkFalseWhenCapsNull() {
        val cm = connectivityManager()
        val shadow = shadowOf(cm)

        val wifiNet = ShadowNetwork.newInstance(1)
        val wifiInfo = ShadowNetworkInfo.newInstance(
            NetworkInfo.DetailedState.CONNECTED, ConnectivityManager.TYPE_WIFI, 0, true, true
        )
        shadow.addNetwork(wifiNet, wifiInfo)
        shadow.setActiveNetworkInfo(wifiInfo)

        assertFalse(NetworkStatusProvider.isConnectedForActiveNetwork(cm))
    }

    @Test
    fun onLostReReadsActiveNetworkWhenMobileAvailable() {
        val cm = connectivityManager()
        val shadow = shadowOf(cm)

        val wifiNet = ShadowNetwork.newInstance(1)
        val wifiInfo = ShadowNetworkInfo.newInstance(
            NetworkInfo.DetailedState.CONNECTED, ConnectivityManager.TYPE_WIFI, 0, true, true
        )
        shadow.addNetwork(wifiNet, wifiInfo)
        shadow.setActiveNetworkInfo(wifiInfo)
        val wifiCaps = ShadowNetworkCapabilities.newInstance()
        shadowOf(wifiCaps).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        shadowOf(wifiCaps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        shadow.setNetworkCapabilities(wifiNet, wifiCaps)

        val mobileNet = ShadowNetwork.newInstance(0)
        val mobileInfo = ShadowNetworkInfo.newInstance(
            NetworkInfo.DetailedState.CONNECTED, ConnectivityManager.TYPE_MOBILE, 0, true, true
        )
        shadow.addNetwork(mobileNet, mobileInfo)
        val mobileCaps = ShadowNetworkCapabilities.newInstance()
        shadowOf(mobileCaps).addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        shadowOf(mobileCaps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        shadow.setNetworkCapabilities(mobileNet, mobileCaps)

        // Simulate WiFi lost: mobile becomes active
        shadow.setActiveNetworkInfo(mobileInfo)

        assertTrue(NetworkStatusProvider.isConnectedForActiveNetwork(cm))
    }

    @Test
    fun isConnectedFlowDoesNotEmitFalseWhenMobileTakesOverOnWifiLost() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val shadow = shadowOf(cm)

        // Set up Wi-Fi with internet as default
        val wifiNet = ShadowNetwork.newInstance(1)
        val wifiInfo = ShadowNetworkInfo.newInstance(
            NetworkInfo.DetailedState.CONNECTED, ConnectivityManager.TYPE_WIFI, 0, true, true
        )
        shadow.addNetwork(wifiNet, wifiInfo)
        shadow.setActiveNetworkInfo(wifiInfo)
        val wifiCaps = ShadowNetworkCapabilities.newInstance()
        shadowOf(wifiCaps).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        shadowOf(wifiCaps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        shadow.setNetworkCapabilities(wifiNet, wifiCaps)

        val events = mutableListOf<Boolean>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            NetworkStatusProvider(ctx).isConnected.collect { events.add(it) }
        }

        assertEquals(listOf(true), events)

        // Set up mobile with internet as new active
        val mobileNet = ShadowNetwork.newInstance(0)
        val mobileInfo = ShadowNetworkInfo.newInstance(
            NetworkInfo.DetailedState.CONNECTED, ConnectivityManager.TYPE_MOBILE, 0, true, true
        )
        shadow.addNetwork(mobileNet, mobileInfo)
        shadow.setActiveNetworkInfo(mobileInfo)
        val mobileCaps = ShadowNetworkCapabilities.newInstance()
        shadowOf(mobileCaps).addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        shadowOf(mobileCaps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        shadow.setNetworkCapabilities(mobileNet, mobileCaps)

        // Simulate Wi-Fi loss via registered callback
        val callback = shadow.networkCallbacks.single()
        callback.onLost(wifiNet)

        testScheduler.advanceUntilIdle()
        assertFalse(events.contains(false))
        job.cancel()
    }

    @Test
    fun onLostWithNoFallbackEmitsFalseAndUnregisters() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val shadow = shadowOf(cm)

        val net = ShadowNetwork.newInstance(1)
        val info = ShadowNetworkInfo.newInstance(
            NetworkInfo.DetailedState.CONNECTED, ConnectivityManager.TYPE_WIFI, 0, true, true
        )
        shadow.addNetwork(net, info)
        shadow.setActiveNetworkInfo(info)
        val caps = ShadowNetworkCapabilities.newInstance()
        shadowOf(caps).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        shadow.setNetworkCapabilities(net, caps)

        val events = mutableListOf<Boolean>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            NetworkStatusProvider(ctx).isConnected.collect { events.add(it) }
        }

        assertEquals(listOf(true), events)

        shadow.clearAllNetworks()
        shadow.setDefaultNetworkActive(false)

        val callback = shadow.networkCallbacks.single()
        callback.onLost(net)

        testScheduler.advanceUntilIdle()
        assertEquals(listOf(true, false), events)

        job.cancel()
        testScheduler.advanceUntilIdle()
        assertTrue(shadow.networkCallbacks.isEmpty())
    }

    private fun connectivityManager(): ConnectivityManager {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        return ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
}
