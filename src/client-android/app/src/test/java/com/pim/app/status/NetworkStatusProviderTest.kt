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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkCapabilities
import org.robolectric.shadows.ShadowNetworkInfo

// --- availabilityFor helper tests (dual API) ---

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 34])
class NetworkStatusProviderTest {

    @Test
    fun availabilityFor_noNetwork_isUnavailable() {
        assertEquals(NetworkAvailability.Unavailable, NetworkStatusProvider.availabilityFor(false, null))
    }

    @Test
    fun availabilityFor_hasNetwork_capsNull_isRestricted() {
        assertEquals(NetworkAvailability.Restricted, NetworkStatusProvider.availabilityFor(true, null))
    }

    @Test
    fun availabilityFor_internetOnly_isRestricted() {
        val caps = ShadowNetworkCapabilities.newInstance()
        shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        assertEquals(NetworkAvailability.Restricted, NetworkStatusProvider.availabilityFor(true, caps))
    }

    @Test
    fun availabilityFor_validatedOnly_isRestricted() {
        val caps = ShadowNetworkCapabilities.newInstance()
        shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        assertEquals(NetworkAvailability.Restricted, NetworkStatusProvider.availabilityFor(true, caps))
    }

    @Test
    fun availabilityFor_both_isValidated() {
        val caps = ShadowNetworkCapabilities.newInstance()
        shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        assertEquals(NetworkAvailability.Validated, NetworkStatusProvider.availabilityFor(true, caps))
    }

    @Test
    fun safeNetworkRead_returnsValueOnSuccess() {
        assertEquals(NetworkAvailability.Validated, NetworkStatusProvider.safeNetworkRead { NetworkAvailability.Validated })
    }

    @Test
    fun safeNetworkRead_returnsUnavailableOnSecurityException() {
        assertEquals(NetworkAvailability.Unavailable, NetworkStatusProvider.safeNetworkRead { throw SecurityException() })
    }
}

// --- Flow tests (SDK 34 only) ---

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NetworkStatusProviderFlowTest {

    @Test
    fun availabilityFlow_noActive_emitsUnavailable() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val shadow = shadowOf(cm)
        shadow.clearAllNetworks()
        shadow.setDefaultNetworkActive(false)

        val events = mutableListOf<NetworkAvailability>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            NetworkStatusProvider(ctx).availability.collect { events.add(it) }
        }
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(NetworkAvailability.Unavailable), events)
        job.cancel()
    }

    @Test
    fun availabilityFlow_activeValidated_emitsValidated() = runTest {
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
        shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        shadow.setNetworkCapabilities(net, caps)

        val events = mutableListOf<NetworkAvailability>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            NetworkStatusProvider(ctx).availability.collect { events.add(it) }
        }
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(NetworkAvailability.Validated), events)
        job.cancel()
    }

    @Test
    fun availabilityFlow_activeRestricted_emitsRestricted() = runTest {
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

        val events = mutableListOf<NetworkAvailability>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            NetworkStatusProvider(ctx).availability.collect { events.add(it) }
        }
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(NetworkAvailability.Restricted), events)
        job.cancel()
    }

    @Test
    fun onAvailable_emitsValidatedWhenCapsHaveBoth() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val shadow = shadowOf(cm)
        shadow.clearAllNetworks()
        shadow.setDefaultNetworkActive(false)

        val events = mutableListOf<NetworkAvailability>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            NetworkStatusProvider(ctx).availability.collect { events.add(it) }
        }
        testScheduler.advanceUntilIdle()
        assertEquals(listOf(NetworkAvailability.Unavailable), events)

        val net = ShadowNetwork.newInstance(2)
        val info = ShadowNetworkInfo.newInstance(
            NetworkInfo.DetailedState.CONNECTED, ConnectivityManager.TYPE_WIFI, 0, true, true
        )
        shadow.addNetwork(net, info)
        shadow.setActiveNetworkInfo(info)
        val caps = ShadowNetworkCapabilities.newInstance()
        shadowOf(caps).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        shadow.setNetworkCapabilities(net, caps)

        val callback = shadow.networkCallbacks.single()
        callback.onAvailable(net)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(NetworkAvailability.Unavailable, NetworkAvailability.Validated), events)
        job.cancel()
    }

    @Test
    fun onAvailable_noValidated_emitsRestricted() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val shadow = shadowOf(cm)
        shadow.clearAllNetworks()
        shadow.setDefaultNetworkActive(false)

        val events = mutableListOf<NetworkAvailability>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            NetworkStatusProvider(ctx).availability.collect { events.add(it) }
        }
        testScheduler.advanceUntilIdle()
        assertEquals(listOf(NetworkAvailability.Unavailable), events)

        val net = ShadowNetwork.newInstance(2)
        val info = ShadowNetworkInfo.newInstance(
            NetworkInfo.DetailedState.CONNECTED, ConnectivityManager.TYPE_WIFI, 0, true, true
        )
        shadow.addNetwork(net, info)
        shadow.setActiveNetworkInfo(info)
        val caps = ShadowNetworkCapabilities.newInstance()
        shadowOf(caps).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        shadow.setNetworkCapabilities(net, caps)

        val callback = shadow.networkCallbacks.single()
        callback.onAvailable(net)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(NetworkAvailability.Unavailable, NetworkAvailability.Restricted), events)
        job.cancel()
    }

    @Test
    fun onAvailable_noCaps_emitsRestricted() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val shadow = shadowOf(cm)
        shadow.clearAllNetworks()
        shadow.setDefaultNetworkActive(false)

        val events = mutableListOf<NetworkAvailability>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            NetworkStatusProvider(ctx).availability.collect { events.add(it) }
        }
        testScheduler.advanceUntilIdle()
        assertEquals(listOf(NetworkAvailability.Unavailable), events)

        val net = ShadowNetwork.newInstance(2)
        val info = ShadowNetworkInfo.newInstance(
            NetworkInfo.DetailedState.CONNECTED, ConnectivityManager.TYPE_WIFI, 0, true, true
        )
        shadow.addNetwork(net, info)
        shadow.setActiveNetworkInfo(info)

        val callback = shadow.networkCallbacks.single()
        callback.onAvailable(net)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(NetworkAvailability.Unavailable, NetworkAvailability.Restricted), events)
        job.cancel()
    }

    @Test
    fun onCapabilitiesChanged_upgradesToValidated() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val shadow = shadowOf(cm)
        shadow.clearAllNetworks()
        shadow.setDefaultNetworkActive(false)

        val events = mutableListOf<NetworkAvailability>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            NetworkStatusProvider(ctx).availability.collect { events.add(it) }
        }
        testScheduler.advanceUntilIdle()
        assertEquals(listOf(NetworkAvailability.Unavailable), events)

        val net = ShadowNetwork.newInstance(3)
        val info = ShadowNetworkInfo.newInstance(
            NetworkInfo.DetailedState.CONNECTED, ConnectivityManager.TYPE_WIFI, 0, true, true
        )
        shadow.addNetwork(net, info)
        shadow.setActiveNetworkInfo(info)
        val earlyCaps = ShadowNetworkCapabilities.newInstance()
        shadowOf(earlyCaps).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        shadowOf(earlyCaps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        shadow.setNetworkCapabilities(net, earlyCaps)

        val callback = shadow.networkCallbacks.single()
        callback.onAvailable(net)
        testScheduler.advanceUntilIdle()
        assertEquals(listOf(NetworkAvailability.Unavailable, NetworkAvailability.Restricted), events)

        val upgradedCaps = ShadowNetworkCapabilities.newInstance()
        shadowOf(upgradedCaps).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        shadowOf(upgradedCaps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        shadowOf(upgradedCaps).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        callback.onCapabilitiesChanged(net, upgradedCaps)
        testScheduler.advanceUntilIdle()

        assertEquals(
            listOf(NetworkAvailability.Unavailable, NetworkAvailability.Restricted, NetworkAvailability.Validated),
            events
        )
        job.cancel()
    }

    @Test
    fun onLost_switchesToMobile() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
        shadowOf(wifiCaps).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        shadow.setNetworkCapabilities(wifiNet, wifiCaps)

        val events = mutableListOf<NetworkAvailability>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            NetworkStatusProvider(ctx).availability.collect { events.add(it) }
        }
        testScheduler.advanceUntilIdle()
        assertEquals(listOf(NetworkAvailability.Validated), events)

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

        val callback = shadow.networkCallbacks.single()
        callback.onLost(wifiNet)
        testScheduler.advanceUntilIdle()

        assertEquals(
            listOf(NetworkAvailability.Validated, NetworkAvailability.Restricted),
            events
        )
        job.cancel()
    }

    @Test
    fun onLost_noFallback_emitsUnavailableAndUnregisters() = runTest {
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
        shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        shadow.setNetworkCapabilities(net, caps)

        val events = mutableListOf<NetworkAvailability>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            NetworkStatusProvider(ctx).availability.collect { events.add(it) }
        }
        testScheduler.advanceUntilIdle()
        assertEquals(listOf(NetworkAvailability.Validated), events)

        shadow.clearAllNetworks()
        shadow.setDefaultNetworkActive(false)

        val callback = shadow.networkCallbacks.single()
        callback.onLost(net)
        testScheduler.advanceUntilIdle()
        assertEquals(listOf(NetworkAvailability.Validated, NetworkAvailability.Unavailable), events)

        job.cancel()
        testScheduler.advanceUntilIdle()
        assertTrue(shadow.networkCallbacks.isEmpty())
    }

    @Test
    fun cancellation_unregistersCallback() = runTest {
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
        shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        shadow.setNetworkCapabilities(net, caps)

        val events = mutableListOf<NetworkAvailability>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            NetworkStatusProvider(ctx).availability.collect { events.add(it) }
        }
        testScheduler.advanceUntilIdle()
        assertEquals(listOf(NetworkAvailability.Validated), events)
        assertNotNull(shadow.networkCallbacks.singleOrNull())

        job.cancel()
        testScheduler.advanceUntilIdle()
        assertTrue(shadow.networkCallbacks.isEmpty())
    }
}
