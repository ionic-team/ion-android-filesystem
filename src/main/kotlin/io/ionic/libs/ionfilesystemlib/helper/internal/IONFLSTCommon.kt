package io.ionic.libs.ionfilesystemlib.helper.internal

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.MimeTypeMap
import io.ionic.libs.ionfilesystemlib.model.IONFLSTCreateOptions
import io.ionic.libs.ionfilesystemlib.model.IONFLSTDeleteOptions
import io.ionic.libs.ionfilesystemlib.model.IONFLSTExceptions
import io.ionic.libs.ionfilesystemlib.model.IONFLSTFileType
import io.ionic.libs.ionfilesystemlib.model.IONFLSTMetadataResult
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import kotlin.math.min

// common methods and variables to different IONFLST helpers

internal const val FILE_MIME_TYPE_FALLBACK = "application/octet-binary"

/**
 * Create a directory or file
 *
 * Most logic is common to both, except the actual creation
 *
 * @param fullPath full path to the file/directory to create
 * @param options options to create the file/directory
 * @param isDirectory true if meant to create directory, false if meant to create file
 * @return success if the file/directory was created successfully, error otherwise
 */
internal fun createDirOrFile(
    fullPath: String,
    options: IONFLSTCreateOptions,
    isDirectory: Boolean
): Result<Unit> =
    runCatching {
        val file = File(fullPath)
        if (file.exists()) {
            throw IONFLSTExceptions.CreateFailed.AlreadyExists()
        }
        if (!checkParentDirectory(file, create = options.recursive)) {
            throw IONFLSTExceptions.CreateFailed.NoParentDirectory()
        }
        val createSucceeded = if (isDirectory) {
            file.mkdir()
        } else {
            file.createNewFile()
        }
        if (!createSucceeded) {
            throw IONFLSTExceptions.CreateFailed.Unknown()
        }
    }

/**
 * Delete a file or directory
 *
 * @param fullPath full path to the file/directory to delete
 * @param options options to delete the file/directory
 * @return success if the file/directory was deleted successfully, error otherwise
 */
internal fun deleteDirOrFile(
    fullPath: String,
    options: IONFLSTDeleteOptions
): Result<Unit> = runCatching {
    val file = File(fullPath)
    if (!file.exists()) {
        throw IONFLSTExceptions.DoesNotExist()
    }
    val deleteSucceeded = if (file.isDirectory) {
        if (!file.listFiles().isNullOrEmpty() && !options.recursive) {
            throw IONFLSTExceptions.DeleteFailed.CannotDeleteChildren()
        }
        file.deleteRecursively()
    } else {
        file.delete()
    }
    if (!deleteSucceeded) {
        throw IONFLSTExceptions.DeleteFailed.Unknown()
    }
}

/**
 * get metadata from a file object (that can represent an actual file or a directory)
 *
 * @param fileObject the [File] representing a file or directory
 * @return metadata information on the file or directory
 */
@SuppressLint("NewApi") // lint not detecting version check in IONFLSTBuildConfig
internal fun getMetadata(fileObject: File): IONFLSTMetadataResult = IONFLSTMetadataResult(
    fullPath = fileObject.absolutePath,
    name = fileObject.name,
    size = fileObject.length(),
    type = if (fileObject.isDirectory) {
        IONFLSTFileType.Directory
    } else {
        IONFLSTFileType.File(mimeType = getMimeType(fileObject))
    },
    createdTimestamp = if (IONFLSTBuildConfig.getAndroidSdkVersionCode() > Build.VERSION_CODES.O) {
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
 * Check preconditions for copying or renaming local files/directories
 *
 * @param sourcePath path to the source file / directory
 * @param destinationPath path to the destination file / directory
 * @param forDirectories true if meant to be copying/renaming directories, false otherwise
 * @return the file objects in an inline lambda, or throws error in case one of the checks fails
 */
internal inline fun prepareForCopyOrRename(
    sourcePath: String,
    destinationPath: String,
    forDirectories: Boolean,
    block: (sourceFileObj: File, destinationFileObj: File) -> Unit
) {
    val sourceFileObj = File(sourcePath)
    val destinationFileObj = File(destinationPath)
    when {
        sourceFileObj == destinationFileObj -> return
        !sourceFileObj.exists() -> throw IONFLSTExceptions.DoesNotExist()
        !forDirectories && (sourceFileObj.isDirectory || destinationFileObj.isDirectory) -> throw IONFLSTExceptions.CopyRenameFailed.MixingFilesAndDirectories()
        forDirectories && (sourceFileObj.isFile || destinationFileObj.isFile) -> throw IONFLSTExceptions.CopyRenameFailed.MixingFilesAndDirectories()
        destinationFileObj.parentFile?.exists() == false -> throw IONFLSTExceptions.CopyRenameFailed.NoParentDirectory()
        forDirectories && destinationFileObj.isDirectory -> throw IONFLSTExceptions.CopyRenameFailed.DestinationDirectoryExists()
        else -> block(sourceFileObj, destinationFileObj)
    }
}

/**
 * Gets the mime type from a file object
 *
 * @param fileObject the file object, that should represent an actual file (not a directory)
 * @return the mime type retrieved from the file extension, or a binary fallback if none was found
 */
private fun getMimeType(fileObject: File): String {
    val extension = fileObject.extension.ifBlank { fileObject.path }
    var resolvedExtension: String? = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    if (resolvedExtension == null) {
        // consider a few extensions that may be missing from android; otherwise return fallback
        resolvedExtension = when (extension) {
            "3ga" -> "audio/3gpp"
            "js" -> "text/javascript"
            else -> FILE_MIME_TYPE_FALLBACK
        }
    }
    return resolvedExtension
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