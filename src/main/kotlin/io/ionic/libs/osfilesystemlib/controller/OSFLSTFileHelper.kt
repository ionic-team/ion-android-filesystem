package io.ionic.libs.osfilesystemlib.controller

import io.ionic.libs.osfilesystemlib.model.OSFLSTCreateOptions
import io.ionic.libs.osfilesystemlib.model.OSFLSTDeleteOptions
import io.ionic.libs.osfilesystemlib.model.OSFLSTExceptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class OSFLSTFileHelper {

    /**
     * Create a directory
     *
     * @param options options to create the directory
     * @return success if the directory was created successfully, error otherwise
     */
    suspend fun createDirectory(options: OSFLSTCreateOptions): Result<Unit> =
        withContext(Dispatchers.IO) {
            return@withContext createDirOrFile(options, isDirectory = true)
        }

    /**
     * Create a file
     *
     * @param options options to create the file
     * @return success if the file was created successfully, error otherwise
     */
    suspend fun createFile(options: OSFLSTCreateOptions): Result<Unit> =
        withContext(Dispatchers.IO) {
            return@withContext createDirOrFile(options, isDirectory = false)
        }

    /**
     * Delete a file or directory
     *
     * @param options options to delete the file/directory
     * @return success if the file/directory was deleted successfully, error otherwise
     */
    suspend fun delete(options: OSFLSTDeleteOptions): Result<Unit> =
        withContext(Dispatchers.IO) {
            val file = File(options.fullPath)
            if (!file.exists()) {
                return@withContext Result.failure(OSFLSTExceptions.DeleteFailed.DoesNotExist())
            }
            val deleteSucceeded = if (file.isDirectory) {
                if (!file.listFiles().isNullOrEmpty() && !options.recursive) {
                    return@withContext Result.failure(OSFLSTExceptions.DeleteFailed.CannotDeleteChildren())
                }
                file.deleteRecursively()
            } else {
                file.delete()
            }
            return@withContext if(deleteSucceeded) {
                Result.success(Unit)
            } else {
                Result.failure(OSFLSTExceptions.DeleteFailed.Unknown())
            }
        }

    /**
     * Create a directory or file
     *
     * Most logic is common to both, except the actual creation
     *
     * @param options options to create the file/directory
     * @param isDirectory true if meant to create directory, false if meant to create file
     * @return success if the file/directory was created successfully, error otherwise
     */
    private fun createDirOrFile(
        options: OSFLSTCreateOptions,
        isDirectory: Boolean
    ): Result<Unit> {
        val file = File(options.fullPath)
        if (file.exists()) {
            return if (options.exclusive) {
                Result.failure(OSFLSTExceptions.CreateFailed.AlreadyExists())
            } else {
                // file/directory creation is not going to do anything if file/directory already exists
                Result.success(Unit)
            }
        }
        if (!checkParentDirectory(file, create = options.recursive)) {
            return Result.failure(OSFLSTExceptions.CreateFailed.NoParentDirectory())
        }
        val createSucceeded = if (isDirectory) {
            file.mkdir()
        } else {
            file.createNewFile()
        }
        return if (createSucceeded) {
            Result.success(Unit)
        } else {
            Result.failure(OSFLSTExceptions.CreateFailed.Unknown())
        }
    }

    /**
     * Checks the parent directory of the file
     *
     * @param file the file to retrieve the parent directory from
     * @param create whether or not should create the parent directories if missing
     *
     * @return true if parent directory exists, false otherwise
     */
    private fun checkParentDirectory(file: File, create: Boolean): Boolean = file.parentFile?.let {
        it.exists() || (create && it.mkdirs())
    } ?: false
}