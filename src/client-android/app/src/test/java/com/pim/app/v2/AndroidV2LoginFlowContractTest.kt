package com.pim.app.v2

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidV2LoginFlowContractTest {
    @Test
    fun settingsLoginUsesConfiguredApiAndStoresReturnedTokens() {
        val source = repoFile(
            "src",
            "main",
            "java",
            "com",
            "pim",
            "app",
            "ui",
            "settings",
            "SettingsViewModel.kt"
        ).readText(Charsets.UTF_8)

        val validationIndex = source.indexOf("ServerUrlValidator.validate(state.value.apiAddress)")
        val saveIndex = source.indexOf("saveApiAddress()", validationIndex)
        val loginIndex = source.indexOf(
            "serverBoundLoginCoordinator.login(username, password)",
            saveIndex
        )
        val staleServerIndex = source.indexOf("ServerBoundLoginResult.StaleServer", loginIndex)
        val saveFailureIndex = source.indexOf("ServerBoundLoginResult.SessionSaveFailed", loginIndex)
        val failureBranchIndex = source.indexOf("onFailure = { error ->", loginIndex)
        val cancellationIndex = source.indexOf(
            "if (error is CancellationException) throw error",
            failureBranchIndex
        )
        val failureUiIndex = source.indexOf("_state.update", failureBranchIndex)

        assertTrue("login must validate the current API address", validationIndex >= 0)
        assertTrue("login must save the validated API address before creating the API client", saveIndex > validationIndex)
        assertTrue("login must use the shared server-bound coordinator", loginIndex > saveIndex)
        assertTrue(
            "login must reject a response from a stale captured server",
            staleServerIndex > loginIndex
        )
        assertTrue("login must surface secure-storage save failure", saveFailureIndex > loginIndex)
        assertTrue(
            "settings login cancellation guard must be the first failure-side effect",
            cancellationIndex > failureBranchIndex && cancellationIndex < failureUiIndex
        )
        assertTrue("ephemeral auth storage must be visible to the user", source.contains("SecureStorageStatus.Ephemeral"))
        assertTrue(
            "login failure must reflect the session still valid for the current server",
            source.contains("isLoggedIn = hasCurrentServerSession()")
        )
        assertTrue(
            "settings logout must surface secure-storage invalidation failure",
            source.contains("if (!tokenManager.clear())")
        )
        assertTrue(
            "settings login must not duplicate the security-critical token commit flow",
            !source.contains("tokenManager.saveTokens(")
        )
    }

    @Test
    fun testConnectionInvalidatesServerASessionBeforeSavingAndProbingServerB() {
        val source = repoFile(
            "src",
            "main",
            "java",
            "com",
            "pim",
            "app",
            "ui",
            "settings",
            "SettingsViewModel.kt"
        ).readText(Charsets.UTF_8)

        val saveFunction = source.indexOf("fun saveApiAddress()")
        val persistIndex = source.indexOf("serverSettingsStore.setBaseUrl", saveFunction)
        val testFunction = source.indexOf("fun testConnection()")
        val saveBeforeProbe = source.indexOf("saveApiAddress()", testFunction)
        val probeIndex = source.indexOf("runConnectionProbe(force = true", testFunction)

        assertTrue("server switch must pass through the central settings boundary", persistIndex > saveFunction)
        assertTrue("testConnection must save the switched server before probing", saveBeforeProbe > testFunction)
        assertTrue("testConnection must not probe until the switched server is saved", probeIndex > saveBeforeProbe)
    }

    @Test
    fun legacyMobileLoginRejectsInvalidReturnedTokenSession() {
        val source = repoFile(
            "src",
            "main",
            "java",
            "com",
            "pim",
            "app",
            "ui",
            "PimAppScaffold.kt"
        ).readText(Charsets.UTF_8)

        val loginIndex = source.indexOf("serverBoundLoginCoordinator.login(username, password)")
        val staleServerIndex = source.indexOf("ServerBoundLoginResult.StaleServer", loginIndex)
        val saveFailureIndex = source.indexOf("ServerBoundLoginResult.SessionSaveFailed", loginIndex)
        val failureBranchIndex = source.indexOf("onFailure = { error ->", loginIndex)
        val cancellationIndex = source.indexOf(
            "if (error is CancellationException) throw error",
            failureBranchIndex
        )
        val failureLogIndex = source.indexOf("logs.error(", failureBranchIndex)
        val failureUiIndex = source.indexOf("_state.update", failureBranchIndex)

        assertTrue("legacy login must use the shared server-bound coordinator", loginIndex >= 0)
        assertTrue(
            "legacy login must reject a response from a stale captured server",
            staleServerIndex > loginIndex
        )
        assertTrue("legacy login must surface secure-storage save failure", saveFailureIndex > loginIndex)
        assertTrue(
            "legacy login cancellation guard must run before failure logging",
            cancellationIndex > failureBranchIndex && cancellationIndex < failureLogIndex
        )
        assertTrue(
            "legacy login cancellation guard must run before failure UI",
            cancellationIndex > failureBranchIndex && cancellationIndex < failureUiIndex
        )
        assertTrue("legacy login must warn about ephemeral auth storage", source.contains("SecureStorageStatus.Ephemeral"))
        assertTrue(
            "legacy login failure must reflect the current server session",
            source.contains("isLoggedIn = hasCurrentServerSession()")
        )
        assertTrue(
            "legacy clear login must surface secure-storage invalidation failure",
            source.contains("if (!tokenManager.clear())")
        )
        assertTrue(
            "legacy login must not duplicate the security-critical token commit flow",
            !source.contains("tokenManager.saveTokens(")
        )
    }

    @Test
    fun sharedLoginCoordinatorPinsTransportAndUsesAtomicServerCommit() {
        val coordinator = repoFile(
            "..",
            "core",
            "src",
            "main",
            "java",
            "com",
            "pim",
            "core",
            "auth",
            "ServerBoundLoginCoordinator.kt"
        ).readText(Charsets.UTF_8)
        val module = repoFile(
            "..",
            "core",
            "src",
            "main",
            "java",
            "com",
            "pim",
            "core",
            "di",
            "CoreModule.kt"
        ).readText(Charsets.UTF_8)

        assertTrue(
            "coordinator must atomically commit only to the captured current server",
            coordinator.contains("serverSettingsStore.commitSessionIfCurrentServer(serverIdentity)")
        )
        assertTrue(
            "production transport must use the service pinned to the captured server",
            module.contains("refreshApiServiceForServer(serverIdentity)")
        )
        assertTrue(
            "production transport must submit the coordinator request",
            module.contains(".login(request)")
        )
    }

    @Test
    fun serverUrlMutationEntrypointsReloadPersistedUrlAndCurrentServerSession() {
        val settings = repoFile(
            "src",
            "main",
            "java",
            "com",
            "pim",
            "app",
            "ui",
            "settings",
            "SettingsViewModel.kt"
        ).readText(Charsets.UTF_8)
        val legacy = repoFile(
            "src",
            "main",
            "java",
            "com",
            "pim",
            "app",
            "ui",
            "PimAppScaffold.kt"
        ).readText(Charsets.UTF_8)

        val settingsSave = settings.sectionBetween(
            "fun saveApiAddress(): Boolean",
            "fun testConnection()"
        )
        val collectionSave = settings.sectionBetween(
            "fun setContinuousCollectionEnabled(enabled: Boolean)",
            "private fun missingCollectionPermissions()"
        )
        val legacySave = legacy.sectionBetween(
            "fun saveServerUrl(value: String)",
            "fun syncNow()"
        )
        val legacyReload = legacy.substringAfter("private fun reloadPersistedServerState(")

        assertTrue(
            "settings save must reload persisted truth after success and failure",
            settingsSave.countOccurrences("reloadPersistedServerState(") >= 2
        )
        assertTrue(
            "collection URL save must reload persisted truth after success and failure",
            collectionSave.countOccurrences("reloadPersistedServerState(") >= 2
        )
        assertTrue(
            "legacy save must reload persisted truth after success and failure",
            legacySave.countOccurrences("reloadPersistedServerState(") >= 2
        )
        assertTrue(
            "legacy reload must read the actual persisted URL",
            legacyReload.contains("serverSettingsStore.getBaseUrl()")
        )
        assertTrue(
            "legacy reload must derive login from the current server-bound session",
            legacyReload.contains("hasCurrentServerSession()")
        )
    }

    private fun repoFile(vararg parts: String): File {
        var current: File? = File("").canonicalFile
        while (current != null) {
            val candidate = parts.fold(current) { dir, part -> dir.resolve(part) }
            if (candidate.exists()) return candidate
            current = current.parentFile
        }
        error("Could not find ${parts.joinToString(File.separator)}")
    }

    private fun String.countOccurrences(needle: String): Int {
        return windowed(needle.length).count { it == needle }
    }

    private fun String.sectionBetween(start: String, end: String): String {
        return substringAfter(start).substringBefore(end)
    }
}
