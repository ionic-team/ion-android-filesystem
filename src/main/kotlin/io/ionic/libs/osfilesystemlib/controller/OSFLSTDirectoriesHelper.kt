package io.ionic.libs.osfilesystemlib.controller

import io.ionic.libs.osfilesystemlib.controller.internal.createDirOrFile
import io.ionic.libs.osfilesystemlib.controller.internal.deleteDirOrFile
import io.ionic.libs.osfilesystemlib.controller.internal.getMetadata
import io.ionic.libs.osfilesystemlib.model.OSFLSTCreateOptions
import io.ionic.libs.osfilesystemlib.model.OSFLSTDeleteOptions
import io.ionic.libs.osfilesystemlib.model.OSFLSTExceptions
import io.ionic.libs.osfilesystemlib.model.OSFLSTMetadataResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class OSFLSTDirectoriesHelper {
    /**
     * Create a directory
     *
     * @param fullPath the full path to the directory to create
     * @param options options to create the directory
     * @return success if the directory was created successfully, error otherwise
     */
    suspend fun createDirectory(
        fullPath: String,
        options: OSFLSTCreateOptions
    ): Result<Unit> = withContext(Dispatchers.IO) {
        createDirOrFile(fullPath, options, isDirectory = true)
    }

    /**
     * Delete or directory
     *
     * @param fullPath the full path of the directory to delete
     * @param options options to delete the directory
     * @return success if the directory was deleted successfully, error otherwise
     */
    suspend fun deleteDirectory(fullPath: String, options: OSFLSTDeleteOptions): Result<Unit> =
        withContext(Dispatchers.IO) { deleteDirOrFile(fullPath, options) }

    /**
     * List the contents of a directory
     *
     * @param fullPath full path to the directory
     * @return success with list of metadata information for each file / sub-directory, error otherwise
     */
    suspend fun listDirectory(fullPath: String): Result<List<OSFLSTMetadataResult>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(fullPath)
                if (!file.exists()) {
                    throw OSFLSTExceptions.DoesNotExist()
                }
                val directoryEntries = file.listFiles()?.toList() ?: emptyList()
                directoryEntries.filterNotNull().map { getMetadata(it) }
            }
        }
}