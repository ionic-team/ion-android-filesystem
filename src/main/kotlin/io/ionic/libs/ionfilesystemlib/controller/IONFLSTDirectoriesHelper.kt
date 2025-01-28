package io.ionic.libs.ionfilesystemlib.controller

import io.ionic.libs.ionfilesystemlib.controller.internal.createDirOrFile
import io.ionic.libs.ionfilesystemlib.controller.internal.deleteDirOrFile
import io.ionic.libs.ionfilesystemlib.controller.internal.getMetadata
import io.ionic.libs.ionfilesystemlib.model.IONFLSTCreateOptions
import io.ionic.libs.ionfilesystemlib.model.IONFLSTDeleteOptions
import io.ionic.libs.ionfilesystemlib.model.IONFLSTExceptions
import io.ionic.libs.ionfilesystemlib.model.IONFLSTMetadataResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class IONFLSTDirectoriesHelper {

    /**
     * List the contents of a directory
     *
     * @param fullPath full path to the directory
     * @return success with list of metadata information for each file / sub-directory, error otherwise
     */
    suspend fun listDirectory(fullPath: String): Result<List<IONFLSTMetadataResult>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(fullPath)
                if (!file.exists()) {
                    throw IONFLSTExceptions.DoesNotExist()
                }
                val directoryEntries = file.listFiles()?.toList() ?: emptyList()
                directoryEntries.filterNotNull().map { getMetadata(it) }
            }
        }

    /**
     * Create a directory
     *
     * @param fullPath the full path to the directory to create
     * @param options options to create the directory
     * @return success if the directory was created successfully, error otherwise
     */
    suspend fun createDirectory(
        fullPath: String,
        options: IONFLSTCreateOptions
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
    suspend fun deleteDirectory(fullPath: String, options: IONFLSTDeleteOptions): Result<Unit> =
        withContext(Dispatchers.IO) { deleteDirOrFile(fullPath, options) }
}