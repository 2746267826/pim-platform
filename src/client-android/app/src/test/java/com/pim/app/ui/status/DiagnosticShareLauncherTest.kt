package com.pim.app.ui.status

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DiagnosticShareLauncherTest {

    @Test
    fun buildShareIntent_hasZipShareContract() {
        val expected = contentUri()
        val intent = DiagnosticShareLauncher.buildShareIntent(expected)

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("application/zip", intent.type)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        val actual = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        assertNotNull(actual)
        assertEquals(expected, actual)
        val clipData = intent.clipData
        assertNotNull(clipData)
        assertEquals(1, clipData!!.itemCount)
        assertEquals(expected, clipData.getItemAt(0).uri)
    }

    @Test
    fun open_missingFile_returnsFalse() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val missingFile = File(context.filesDir, "diagnostics/exports/nonexistent.zip")

        assertFalse(DiagnosticShareLauncher.open(context, missingFile))
    }

    @Test
    fun fileProvider_onlyExposesDiagnosticExports() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val exportFile = File(context.filesDir, "diagnostics/exports/test-diagnostics.zip")
            .apply {
                parentFile?.mkdirs()
                writeText("test")
            }
        val outsideFile = File(context.filesDir, "outside-diagnostics.zip")
            .apply { writeText("test") }

        val uri = DiagnosticShareLauncher.resolveContentUri(context, exportFile)

        assertEquals("content", uri.scheme)
        assertEquals("${context.packageName}.fileprovider", uri.authority)
        assertThrows(IllegalArgumentException::class.java) {
            DiagnosticShareLauncher.resolveContentUri(context, outsideFile)
        }
    }

    private fun contentUri(): Uri =
        Uri.parse("content://com.pim.app.fileprovider/diagnostics_exports/test-diagnostics.zip")
}
