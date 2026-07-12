package com.pim.app.status

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ConnectionProbeProductionWiringTest {
    @Test
    fun appModuleProvidesQualifiedClientsTokenAdapterAndDedicatedProbeGraph() {
        val source = repoFile(
            "src", "main", "java", "com", "pim", "app", "di", "AppModule.kt"
        ).readText(Charsets.UTF_8)

        listOf(
            "annotation class AnonymousProbeClient",
            "annotation class AuthenticatedProbeClient",
            "annotation class ConnectionProbePreferences",
            "fun provideAnonymousProbeClient",
            "fun provideAuthenticatedProbeClient",
            "fun provideProbeTokenSource",
            "fun provideConnectionProbePreferences",
            "fun provideConnectionProbeStore",
            "fun provideConnectionProbeService",
            "fun provideConnectionProbeRunner"
        ).forEach { requiredSource ->
            assertTrue("AppModule must contain $requiredSource", source.contains(requiredSource))
        }
        assertTrue(
            source.contains(
                "tokenManager.getAccessTokenForServer(serverUrl)"
            )
        )
    }

    @Test
    fun settingsEntryUsesFreshnessAndManualTestForcesProbe() {
        val source = repoFile(
            "src", "main", "java", "com", "pim", "app", "ui", "settings", "SettingsViewModel.kt"
        ).readText(Charsets.UTF_8)

        assertTrue(source.contains("private val connectionProbeRunner: ConnectionProbeRunner"))
        assertTrue(source.contains("runConnectionProbe(force = false)"))
        assertTrue(source.contains("runConnectionProbe(force = true"))
        assertTrue(source.contains("connectionProbeRunner.run(force = force)"))
        assertTrue(source.contains("probeRequestGeneration.incrementAndGet()"))
        assertTrue(source.contains("probeRequestGeneration.get() == requestGeneration"))
        assertTrue(source.contains("activeManualProbeGeneration.compareAndSet"))
        assertFalse(source.contains("API 地址格式可用。登录后会使用该地址连接服务器。"))
    }

    @Test
    fun statusEntryUsesFreshnessAwareProbeRunner() {
        val source = repoFile(
            "src", "main", "java", "com", "pim", "app", "ui", "status", "StatusCenterViewModel.kt"
        ).readText(Charsets.UTF_8)

        assertTrue(source.contains("private val connectionProbeRunner: ConnectionProbeRunner"))
        assertTrue(source.contains("init {"))
        assertTrue(source.contains("connectionProbeRunner.run(force = false)"))
    }

    @Test
    fun visibleSettingsAndStatusScreensScheduleDynamicFreshnessRechecks() {
        val settings = repoFile(
            "src", "main", "java", "com", "pim", "app", "ui", "settings", "SettingsScreen.kt"
        ).readText(Charsets.UTF_8)
        val status = repoFile(
            "src", "main", "java", "com", "pim", "app", "ui", "status", "StatusCenterScreen.kt"
        ).readText(Charsets.UTF_8)

        listOf(settings, status).forEach { source ->
            assertTrue(source.contains("while (isActive)"))
            assertTrue(source.contains("refreshConnectionForVisibleScreen()"))
        }
    }

    private fun repoFile(vararg parts: String): File {
        var current: File? = File("").canonicalFile
        while (current != null) {
            val candidate = parts.fold(current) { directory, part -> directory.resolve(part) }
            if (candidate.exists()) return candidate
            current = current.parentFile
        }
        error("Could not find ${parts.joinToString(File.separator)}")
    }
}
