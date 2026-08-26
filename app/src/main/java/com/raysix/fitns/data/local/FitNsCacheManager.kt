package com.raysix.fitns.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Centralizes the cache locations owned by FitNS so privacy operations never
 * need to wipe Android's complete cache directory.
 */
@Singleton
class FitNsCacheManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val cacheDirectory = context.cacheDir
    private val capturesDirectory = File(cacheDirectory, CapturesDirectoryName)
    private val exportsDirectory = File(cacheDirectory, ExportsDirectoryName)

    suspend fun writeExport(json: String): File = withContext(Dispatchers.IO) {
        ensureDirectory(exportsDirectory)
        File.createTempFile(ExportFilePrefix, ExportFileSuffix, exportsDirectory).apply {
            writeText(json, Charsets.UTF_8)
        }
    }

    suspend fun deleteFitNsCacheFiles() = withContext(Dispatchers.IO) {
        deleteRecursivelyIfPresent(capturesDirectory)
        deleteRecursivelyIfPresent(exportsDirectory)

        // Exports created by older FitNS builds lived directly in cacheDir.
        cacheDirectory.listFiles()
            ?.filter { file ->
                file.isFile &&
                    file.name.startsWith(ExportFilePrefix) &&
                    file.name.endsWith(ExportFileSuffix)
            }
            ?.forEach(::deleteFile)
            ?: throw IOException("FitNS cache directory could not be read")
    }

    private fun ensureDirectory(directory: File) {
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("FitNS cache directory could not be created")
        }
        if (!directory.isDirectory) {
            throw IOException("FitNS cache path is not a directory")
        }
    }

    private fun deleteRecursivelyIfPresent(file: File) {
        if (!file.exists()) return
        if (file.isDirectory) {
            val children = file.listFiles()
                ?: throw IOException("FitNS cache directory could not be read")
            children.forEach(::deleteRecursivelyIfPresent)
        }
        deleteFile(file)
    }

    private fun deleteFile(file: File) {
        if (file.exists() && !file.delete()) {
            throw IOException("FitNS cache file could not be deleted")
        }
    }

    private companion object {
        const val CapturesDirectoryName = "captures"
        const val ExportsDirectoryName = "exports"
        const val ExportFilePrefix = "fitns-export-"
        const val ExportFileSuffix = ".json"
    }
}
