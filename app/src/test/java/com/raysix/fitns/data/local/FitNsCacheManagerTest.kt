package com.raysix.fitns.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FitNsCacheManagerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = FitNsCacheManager(context)

    @Test
    fun writeExportUsesDedicatedFitNsExportDirectory() = runTest {
        manager.deleteFitNsCacheFiles()

        val export = manager.writeExport("{\"version\":1}")

        assertEquals("exports", export.parentFile?.name)
        assertTrue(export.name.startsWith("fitns-export-"))
        assertEquals("{\"version\":1}", export.readText())

        manager.deleteFitNsCacheFiles()
    }

    @Test
    fun deleteFitNsCacheFilesDeletesCapturesAndAllExportFormatsOnly() = runTest {
        manager.deleteFitNsCacheFiles()
        val captures = File(context.cacheDir, "captures").apply { mkdirs() }
        File(captures, "capture.jpg").writeText("capture")
        val export = manager.writeExport("new export")
        val legacyExport = File(context.cacheDir, "fitns-export-legacy.json").apply {
            writeText("legacy export")
        }
        val unrelated = File(context.cacheDir, "keep-me.tmp").apply {
            writeText("unrelated")
        }

        manager.deleteFitNsCacheFiles()

        assertFalse(captures.exists())
        assertFalse(export.exists())
        assertFalse(export.parentFile?.exists() == true)
        assertFalse(legacyExport.exists())
        assertTrue(unrelated.exists())

        unrelated.delete()
    }
}
