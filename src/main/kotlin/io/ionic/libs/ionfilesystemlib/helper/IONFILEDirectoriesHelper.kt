package io.ionic.libs.ionfilesystemlib.helper

import io.ionic.libs.ionfilesystemlib.helper.common.createDirOrFile
import io.ionic.libs.ionfilesystemlib.helper.common.deleteDirOrFile
import io.ionic.libs.ionfilesystemlib.helper.common.getMetadata
import io.ionic.libs.ionfilesystemlib.helper.common.prepareForCopyOrRename
import io.ionic.libs.ionfilesystemlib.model.IONFILECreateOptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEDeleteOptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEExceptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEMetadataResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal class IONFILEDirectoriesHelper {

    /**
     * List the contents of a directory
     *
     * @param fullPath full path to the directory
     * @return success with list of metadata information for each file / sub-directory, error otherwise
     */
    suspend fun listDirectory(fullPath: String): Result<List<IONFILEMetadataResult>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(fullPath)
                if (!file.exists()) {
                    throw IONFILEExceptions.DoesNotExist()
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
        options: IONFILECreateOptions
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
    suspend fun deleteDirectory(fullPath: String, options: IONFILEDeleteOptions): Result<Unit> =
        withContext(Dispatchers.IO) { deleteDirOrFile(fullPath, options) }

    /**
     * Recursively copies a directory's contents to another location
     *
     * @param sourcePath the full path to the source directory
     * @param destinationPath the full path to the destination directory
     * @return success if the directory was copied successfully, error otherwise
     */
    suspend fun copyDirectory(
        sourcePath: String,
        destinationPath: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            prepareForCopyOrRename(
                sourcePath, destinationPath, forDirectories = true
            ) { sourceFileObj: File, destinationFileObj: File ->
                val copySuccess =
                    sourceFileObj.copyRecursively(destinationFileObj, overwrite = false)
                if (!copySuccess) {
                    throw IONFILEExceptions.UnknownError()
                }
            }
        }
    }

    /**
     * Rename or move a directory from one path to another
     *
     * If rename/move fails, will attempt to copy the source recursively and then delete it, as a fallback
     *
     * @param sourcePath the full path to the source directory
     * @param destinationPath the full path to the destination directory
     * @return success if the directory was renamed/moved successfully, error otherwise
     */
    suspend fun moveDirectory(
        sourcePath: String,
        destinationPath: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            prepareForCopyOrRename(
                sourcePath, destinationPath, forDirectories = true
            ) { sourceFileObj: File, destinationFileObj: File ->
                val renameSuccessful = sourceFileObj.renameTo(destinationFileObj)
                if (!renameSuccessful) {
                    copyDirectory(sourcePath, destinationPath).getOrElse {
                        throw IONFILEExceptions.UnknownError()
                    }
                    deleteDirectory(sourcePath, IONFILEDeleteOptions(recursive = true)).getOrElse {
                        throw IONFILEExceptions.UnknownError()
                    }
                }
            }
        }
    }
}