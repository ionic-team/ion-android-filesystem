package io.ionic.libs.osfilesystemlib.controller.internal

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.MimeTypeMap
import io.ionic.libs.osfilesystemlib.model.OSFLSTCreateOptions
import io.ionic.libs.osfilesystemlib.model.OSFLSTDeleteOptions
import io.ionic.libs.osfilesystemlib.model.OSFLSTExceptions
import io.ionic.libs.osfilesystemlib.model.OSFLSTFileType
import io.ionic.libs.osfilesystemlib.model.OSFLSTMetadataResult
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import kotlin.math.min


private const val FILE_MIME_TYPE_FALLBACK = "application/octet-binary"

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
        throw OSFLSTExceptions.DoesNotExist()
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
 * get metadata from a file object (that can represent an actual file or a directory)
 *
 * @param fileObject the [File] representing a file or directory
 * @return metadata information on the file or directory
 */
@SuppressLint("NewApi") // lint not detecting version check in OSFLSTBuildConfig
internal fun getMetadata(fileObject: File): OSFLSTMetadataResult = OSFLSTMetadataResult(
    fullPath = fileObject.absolutePath,
    name = fileObject.name,
    size = fileObject.length(),
    type = if (fileObject.isDirectory) {
        OSFLSTFileType.Directory
    } else {
        OSFLSTFileType.File(mimeType = getMimeType(fileObject))
    },
    createdTimestamp = if (OSFLSTBuildConfig.getAndroidSdkVersionCode() > Build.VERSION_CODES.O) {
        Files.readAttributes(fileObject.toPath(), BasicFileAttributes::class.java).let { attr ->
            // use the oldest of the two attributes
            min(attr.creationTime().toMillis(), attr.lastAccessTime().toMillis())
        }
    } else {
        0
    },
    lastModifiedTimestamp = fileObject.lastModified()
)

/**
 * Gets the mime type from a file object
 *
 * @param fileObject the file object, that should represent an actual file (not a directory)
 * @return the mime type retrieved from the file extension, or a binary fallback if none was found
 */
private fun getMimeType(fileObject: File): String {
    val extension = fileObject.extension.ifBlank { fileObject.path }
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: FILE_MIME_TYPE_FALLBACK
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