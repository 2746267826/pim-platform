package com.pim.app.v2

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AndroidLiveUpdateBuildContractTest {

    private val projectRoot: File by lazy {
        var dir = File(System.getProperty("user.dir"))
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile
        }
        dir ?: throw IllegalStateException("Could not find project root from ${System.getProperty("user.dir")}")
    }

    private val repoRoot: File by lazy {
        generateSequence(projectRoot) { it.parentFile }
            .firstOrNull { File(it, ".github/workflows/build-android.yml").isFile }
            ?: throw IllegalStateException(
                "Could not find repository root (no .github/workflows/build-android.yml found) " +
                    "starting from ${projectRoot.absolutePath}"
            )
    }

    @Test
    fun rootBuildGradleContainsAgp8_13_2() {
        val content = File(projectRoot, "build.gradle.kts").readText()
        assertTrue(
            "Root build.gradle.kts must contain AGP version \"8.13.2\"",
            content.contains("version \"8.13.2\"")
        )
    }

    @Test
    fun appBuildGradleContainsCompileSdk36() {
        val content = File(projectRoot, "app/build.gradle.kts").readText()
        assertTrue(
            "App build.gradle.kts must have compileSdk = 36",
            content.contains("compileSdk = 36")
        )
    }

    @Test
    fun appBuildGradleContainsCompileSdkMinor1() {
        val content = File(projectRoot, "app/build.gradle.kts").readText()
        assertTrue(
            "App build.gradle.kts must have compileSdkMinor = 1",
            content.contains("compileSdkMinor = 1")
        )
    }

    @Test
    fun appBuildGradleStillContainsTargetSdk34() {
        val content = File(projectRoot, "app/build.gradle.kts").readText()
        assertTrue(
            "App build.gradle.kts must still contain targetSdk = 34",
            content.contains("targetSdk = 34")
        )
    }

    @Test
    fun appBuildGradleStillContainsMinSdk26() {
        val content = File(projectRoot, "app/build.gradle.kts").readText()
        assertTrue(
            "App build.gradle.kts must still contain minSdk = 26",
            content.contains("minSdk = 26")
        )
    }

    @Test
    fun workflowContainsPlatformsAndroid36_1() {
        val content = File(repoRoot, ".github/workflows/build-android.yml").readText()
        assertTrue(
            "Workflow must contain platforms;android-36.1",
            content.contains("platforms;android-36.1")
        )
    }

    @Test
    fun workflowContainsBuildTools36_1_0() {
        val content = File(repoRoot, ".github/workflows/build-android.yml").readText()
        assertTrue(
            "Workflow must contain build-tools;36.1.0",
            content.contains("build-tools;36.1.0")
        )
    }
}
