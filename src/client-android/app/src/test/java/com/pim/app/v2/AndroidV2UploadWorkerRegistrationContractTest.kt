package com.pim.app.v2

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidV2UploadWorkerRegistrationContractTest {
    @Test
    fun appStartupRegistersPeriodicUploadWorkerAndLogsSchedulingFailures() {
        val app = repoFile(
            "src",
            "main",
            "java",
            "com",
            "pim",
            "app",
            "PimApp.kt"
        ).readText(Charsets.UTF_8)

        assertTrue(app.contains("override fun onCreate()"))
        assertTrue(app.contains("scheduleUploadWorker(this)"))
        assertTrue(app.contains("runCatching"))
        assertTrue("startup upload scheduling failures must be observable", app.contains(".onFailure"))
        assertTrue("startup upload scheduling failures must be logged", app.contains("Timber.e"))
    }

    @Test
    fun periodicUploadWorkerUsesStableUniqueWorkRegistration() {
        val worker = repoFile(
            "src",
            "main",
            "java",
            "com",
            "pim",
            "app",
            "daemon",
            "UploadWorker.kt"
        ).readText(Charsets.UTF_8)

        assertTrue(worker.contains("const val WORK_NAME = \"pim_upload\""))
        assertTrue(worker.contains("PeriodicWorkRequestBuilder<UploadWorker>(15, TimeUnit.MINUTES)"))
        assertTrue(worker.contains("NetworkType.CONNECTED"))
        assertTrue(worker.contains("enqueueUniquePeriodicWork("))
        assertTrue(worker.contains("UploadWorker.WORK_NAME"))
        assertTrue(worker.contains("ExistingPeriodicWorkPolicy.KEEP"))
    }

    private fun repoFile(vararg parts: String): File {
        var current: File? = File("").canonicalFile
        while (current != null) {
            val candidate = parts.fold(current) { dir, part -> dir.resolve(part) }
            if (candidate.exists() || parts.last().endsWith(".kt")) return candidate
            current = current.parentFile
        }
        error("Could not find ${parts.joinToString(File.separator)}")
    }
}
