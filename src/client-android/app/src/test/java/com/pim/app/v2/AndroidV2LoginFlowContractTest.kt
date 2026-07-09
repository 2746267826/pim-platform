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
        val loginIndex = source.indexOf("apiClientProvider.refreshApiService().login", saveIndex)
        val requestIndex = source.indexOf("LoginRequest(username.trim(), password)", loginIndex)
        val tokenIndex = source.indexOf("tokenManager.saveTokens(auth.accessToken, auth.refreshToken)", requestIndex)

        assertTrue("login must validate the current API address", validationIndex >= 0)
        assertTrue("login must save the validated API address before creating the API client", saveIndex > validationIndex)
        assertTrue("login must use the refreshed API service for the configured address", loginIndex > saveIndex)
        assertTrue("login must submit trimmed credentials through LoginRequest", requestIndex > loginIndex)
        assertTrue("login success must persist returned access and refresh tokens", tokenIndex > requestIndex)
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
}
