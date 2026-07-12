package com.pim.app.ui.settings

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.pim.app.location.service.ForegroundLocationController
import com.pim.app.mobile.usage.UsageAccessChecker
import com.pim.app.permissions.PermissionStatusRepository
import com.pim.app.settings.TrackingSettingsStore
import com.pim.app.status.ConnectionProbe
import com.pim.app.status.ConnectionProbeEvidenceStore
import com.pim.app.status.ConnectionProbeOutcome
import com.pim.app.status.ConnectionProbeResult
import com.pim.app.status.ConnectionProbeRunner
import com.pim.app.status.ConnectionProbeStage
import com.pim.app.status.ServerCapabilities
import com.pim.core.auth.SecurePreferencesFactory
import com.pim.core.auth.ServerBoundLoginCoordinator
import com.pim.core.auth.ServerBoundLoginTransport
import com.pim.core.auth.TokenManager
import com.pim.core.settings.PimServerEndpoints
import com.pim.core.settings.ServerSettingsStore
import java.util.ArrayDeque
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsServerMutationTest {
    private val mainDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        val application = ApplicationProvider.getApplicationContext<Application>()
        context = application
        shadowOf(application).grantPermissions(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.ACTIVITY_RECOGNITION
        )
        listOf(SERVER_PREFS, AUTH_PREFS, TRACKING_PREFS).forEach { name ->
            check(context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit())
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun saveCommitFailureReloadsServerAAndKeepsServerASessionAndCollectionIntent() {
        val fixture = fixture()
        fixture.serverPreferences.enqueueCommitResult(false)
        fixture.viewModel.updateApiAddress(SERVER_B_URL)

        assertFalse(fixture.viewModel.saveApiAddress())

        val state = fixture.viewModel.state.value
        assertEquals(SERVER_A_URL, state.apiAddress)
        assertTrue(state.isLoggedIn)
        assertTrue(state.continuousCollectionEnabled)
        assertTrue(fixture.trackingSettings.read().continuousCollectionEnabled)
    }

    @Test
    fun successfulServerSwitchReloadsServerBAndClearedSessionWithoutChangingCollectionIntent() {
        val fixture = fixture()
        fixture.viewModel.updateApiAddress(SERVER_B_URL)

        assertTrue(fixture.viewModel.saveApiAddress())

        val state = fixture.viewModel.state.value
        assertEquals(SERVER_B_URL, state.apiAddress)
        assertFalse(state.isLoggedIn)
        assertTrue(state.continuousCollectionEnabled)
        assertTrue(fixture.trackingSettings.read().continuousCollectionEnabled)
    }

    @Test
    fun failedSessionClearAndRollbackReloadsActualServerAndSessionTruth() {
        val fixture = fixture(failSessionClear = true)
        fixture.serverPreferences.enqueueCommitResult(true)
        fixture.serverPreferences.enqueueCommitResult(false)
        fixture.viewModel.updateApiAddress(SERVER_B_URL)

        assertFalse(fixture.viewModel.saveApiAddress())

        val actualServerUrl = fixture.serverSettings.getBaseUrl()
        val state = fixture.viewModel.state.value
        assertEquals(actualServerUrl, state.apiAddress)
        assertEquals(
            !fixture.tokenManager.getAccessTokenForServer(actualServerUrl).isNullOrBlank(),
            state.isLoggedIn
        )
        assertTrue(fixture.trackingSettings.read().continuousCollectionEnabled)
    }

    @Test
    fun collectionServerSaveFailureReloadsTruthWithoutChangingCollectionIntent() {
        val fixture = fixture()
        fixture.serverPreferences.enqueueCommitResult(false)
        fixture.viewModel.updateApiAddress(SERVER_B_URL)

        fixture.viewModel.setContinuousCollectionEnabled(true)

        val state = fixture.viewModel.state.value
        assertEquals(SERVER_A_URL, state.apiAddress)
        assertTrue(state.isLoggedIn)
        assertTrue(state.continuousCollectionEnabled)
        assertTrue(fixture.trackingSettings.read().continuousCollectionEnabled)
    }

    @Test
    fun successfulCollectionServerSwitchKeepsIntentWhileNewServerSessionIsMissing() {
        val fixture = fixture()
        fixture.viewModel.updateApiAddress(SERVER_B_URL)

        fixture.viewModel.setContinuousCollectionEnabled(true)

        val state = fixture.viewModel.state.value
        assertEquals(SERVER_B_URL, state.apiAddress)
        assertFalse(state.isLoggedIn)
        assertTrue(state.continuousCollectionEnabled)
        assertTrue(fixture.trackingSettings.read().continuousCollectionEnabled)
    }

    @Test
    fun logoutClearsSessionWithoutChangingCollectionIntent() {
        val fixture = fixture()

        fixture.viewModel.logout()

        assertFalse(fixture.viewModel.state.value.isLoggedIn)
        assertTrue(fixture.viewModel.state.value.continuousCollectionEnabled)
        assertTrue(fixture.trackingSettings.read().continuousCollectionEnabled)
    }

    private fun fixture(failSessionClear: Boolean = false): Fixture {
        val serverPreferences = ScriptedCommitSharedPreferences(
            context.getSharedPreferences(SERVER_PREFS, Context.MODE_PRIVATE)
        )
        val authPreferences = ScriptedCommitSharedPreferences(
            context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
        )
        val securePreferencesFactory = TestSecurePreferencesFactory(
            preferences = authPreferences,
            durableInvalidation = !failSessionClear
        )
        val tokenManager = TokenManager(securePreferencesFactory, nowMillis = { 1_000L })
        val serverSettings = ServerSettingsStore(
            SharedPreferencesContext(context, serverPreferences),
            tokenManager
        )
        serverSettings.setBaseUrl(SERVER_A_URL)
        check(tokenManager.save("access-a", "refresh-a", Long.MAX_VALUE, SERVER_A_IDENTITY))
        if (failSessionClear) authPreferences.enqueueCommitResult(false)

        val trackingSettings = TrackingSettingsStore(
            context.getSharedPreferences(TRACKING_PREFS, Context.MODE_PRIVATE)
        )
        trackingSettings.setContinuousCollectionEnabled(true)
        val coordinator = ServerBoundLoginCoordinator(
            serverSettings,
            tokenManager,
            ServerBoundLoginTransport { _, _ -> error("login transport is not used") }
        )
        val probeStore = InMemoryProbeEvidenceStore()
        val probeRunner = ConnectionProbeRunner(
            probe = ConnectionProbe { serverUrl -> successfulProbe(serverUrl) },
            store = probeStore,
            currentServerUrl = serverSettings::getBaseUrl,
            wallClockMillis = { 1_000L }
        )
        val viewModel = SettingsViewModel(
            serverSettingsStore = serverSettings,
            tokenManager = tokenManager,
            serverBoundLoginCoordinator = coordinator,
            trackingSettingsStore = trackingSettings,
            foregroundLocationController = ForegroundLocationController(context),
            permissionStatusRepository = PermissionStatusRepository(
                context,
                UsageAccessChecker(context)
            ),
            connectionProbeRunner = probeRunner
        )
        return Fixture(
            viewModel = viewModel,
            serverSettings = serverSettings,
            tokenManager = tokenManager,
            trackingSettings = trackingSettings,
            serverPreferences = serverPreferences
        )
    }

    private fun successfulProbe(serverUrl: String): ConnectionProbeResult {
        return ConnectionProbeResult(
            outcome = ConnectionProbeOutcome.Reachable,
            checkedAtUtcMillis = 1_000L,
            serverIdentity = PimServerEndpoints.from(serverUrl).apiBaseUrl.toString(),
            lastCompletedStage = ConnectionProbeStage.EmbedBootstrap,
            latencyMillisByStage = emptyMap(),
            capabilities = ServerCapabilities(
                mobileItemResultsV1 = true,
                androidEmbedV1 = true
            )
        )
    }

    private data class Fixture(
        val viewModel: SettingsViewModel,
        val serverSettings: ServerSettingsStore,
        val tokenManager: TokenManager,
        val trackingSettings: TrackingSettingsStore,
        val serverPreferences: ScriptedCommitSharedPreferences
    )

    private companion object {
        const val SERVER_PREFS = "settings_server_mutation_server"
        const val AUTH_PREFS = "settings_server_mutation_auth"
        const val TRACKING_PREFS = "settings_server_mutation_tracking"
        const val SERVER_A_URL = "https://server-a.example/api/v1/"
        const val SERVER_B_URL = "https://server-b.example/api/v1/"
        const val SERVER_A_IDENTITY = "https://server-a.example"
    }
}

private class InMemoryProbeEvidenceStore : ConnectionProbeEvidenceStore {
    private val mutableResult = MutableStateFlow<ConnectionProbeResult?>(null)
    override val result: StateFlow<ConnectionProbeResult?> = mutableResult.asStateFlow()

    override fun save(result: ConnectionProbeResult): Boolean {
        mutableResult.value = result
        return true
    }

    override fun freshResult(serverIdentity: String, nowMillis: Long): ConnectionProbeResult? = null
}

private class SharedPreferencesContext(
    base: Context,
    private val preferences: SharedPreferences
) : ContextWrapper(base) {
    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = preferences
}

private class TestSecurePreferencesFactory(
    private val preferences: SharedPreferences,
    private val durableInvalidation: Boolean
) : SecurePreferencesFactory {
    override fun open(): SharedPreferences = preferences

    override fun reset() {
        check(durableInvalidation) { "secure storage reset failed" }
        check(preferences.edit().clear().commit())
    }

    override fun markSessionInvalidated(): Boolean = durableInvalidation
}

private class ScriptedCommitSharedPreferences(
    private val delegate: SharedPreferences
) : SharedPreferences by delegate {
    private val commitResults = ArrayDeque<Boolean>()

    fun enqueueCommitResult(result: Boolean) {
        commitResults.addLast(result)
    }

    override fun edit(): SharedPreferences.Editor = ScriptedEditor(delegate.edit())

    private inner class ScriptedEditor(
        private val delegate: SharedPreferences.Editor
    ) : SharedPreferences.Editor {
        override fun putString(key: String, value: String?): SharedPreferences.Editor = apply {
            delegate.putString(key, value)
        }

        override fun putStringSet(
            key: String,
            values: MutableSet<String>?
        ): SharedPreferences.Editor = apply {
            delegate.putStringSet(key, values)
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor = apply {
            delegate.putInt(key, value)
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply {
            delegate.putLong(key, value)
        }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor = apply {
            delegate.putFloat(key, value)
        }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = apply {
            delegate.putBoolean(key, value)
        }

        override fun remove(key: String): SharedPreferences.Editor = apply {
            delegate.remove(key)
        }

        override fun clear(): SharedPreferences.Editor = apply {
            delegate.clear()
        }

        override fun commit(): Boolean {
            val shouldCommit = if (commitResults.isEmpty()) true else commitResults.removeFirst()
            val delegateCommitted = delegate.commit()
            return shouldCommit && delegateCommitted
        }

        override fun apply() {
            delegate.apply()
        }
    }
}
