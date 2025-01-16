package io.ionic.libs.osfilesystemlib.controller.internal

import io.ionic.libs.osfilesystemlib.model.OSFLSTCreateOptions
import io.ionic.libs.osfilesystemlib.model.OSFLSTDeleteOptions
import io.ionic.libs.osfilesystemlib.model.OSFLSTExceptions
import java.io.File

/**
 * Create a directory or file
 *
 * Most logic is common to both, except the actual creation
 *
 * @param options options to create the file/directory
 * @param isDirectory true if meant to create directory, false if meant to create file
 * @return success if the file/directory was created successfully, error otherwise
 */
internal fun createDirOrFile(options: OSFLSTCreateOptions, isDirectory: Boolean): Result<Unit> =
    runCatching {
        val file = File(options.fullPath)
        if (file.exists()) {
            if (options.exclusive) {
                throw OSFLSTExceptions.CreateFailed.AlreadyExists()
            } else {
                // file/directory creation is not going to do anything if file/directory already exists
                return@runCatching
            }
        }
        if (!checkParentDirectory(file, create = options.recursive)) {
            throw OSFLSTExceptions.CreateFailed.NoParentDirectory()
        }
        val createSucceeded = if (isDirectory) {
            file.mkdir()
        } else {
            file.createNewFile()
        }
        if (!createSucceeded) {
            throw OSFLSTExceptions.CreateFailed.Unknown()
        }
    }

/**
 * Delete a file or directory
 *
 * @param options options to delete the file/directory
 * @return success if the file/directory was deleted successfully, error otherwise
 */
internal fun deleteDirOrFile(options: OSFLSTDeleteOptions): Result<Unit> = runCatching {
    val file = File(options.fullPath)
    if (!file.exists()) {
        throw OSFLSTExceptions.DeleteFailed.DoesNotExist()
    }
    val deleteSucceeded = if (file.isDirectory) {
        if (!file.listFiles().isNullOrEmpty() && !options.recursive) {
            throw OSFLSTExceptions.DeleteFailed.CannotDeleteChildren()
        }
        file.deleteRecursively()
    } else {
        file.delete()
    }
    if (!deleteSucceeded) {
        throw OSFLSTExceptions.DeleteFailed.Unknown()
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