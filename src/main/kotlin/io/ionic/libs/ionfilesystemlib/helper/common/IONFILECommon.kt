package io.ionic.libs.ionfilesystemlib.helper.common

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import io.ionic.libs.ionfilesystemlib.model.IONFILECreateOptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEDeleteOptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEExceptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEFileType
import io.ionic.libs.ionfilesystemlib.model.IONFILEMetadataResult
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import kotlin.math.min

// common methods and variables to different IONFILE helpers
internal const val FILE_MIME_TYPE_FALLBACK = "application/octet-binary"
private const val FILE_3GA_EXTENSION = "3ga"
private const val FILE_3GA_MIME_TYPE = "audio/3gpp"
private const val FILE_JAVASCRIPT_EXTENSION = "js"
private const val FILE_JAVASCRIPT_MIME_TYPE = "text/javascript"

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
    options: IONFILECreateOptions,
    isDirectory: Boolean
): Result<Unit> =
    runCatching {
        val file = File(fullPath)
        when {
            file.exists() -> throw IONFILEExceptions.CreateFailed.AlreadyExists(fullPath)

            !checkParentDirectory(file, create = options.recursive) ->
                throw IONFILEExceptions.CreateFailed.NoParentDirectory()

            isDirectory && !file.mkdir() -> throw IONFILEExceptions.UnknownError()

            !isDirectory && !file.createNewFile() -> throw IONFILEExceptions.UnknownError()
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
    options: IONFILEDeleteOptions
): Result<Unit> = runCatching {
    val file = File(fullPath)
    if (!file.exists()) {
        throw IONFILEExceptions.DoesNotExist(fullPath)
    }
    val deleteSucceeded = if (file.isDirectory) {
        if (!file.listFiles().isNullOrEmpty() && !options.recursive) {
            throw IONFILEExceptions.DeleteFailed.CannotDeleteChildren()
        }
        file.deleteRecursively()
    } else {
        file.delete()
    }
    if (!deleteSucceeded) {
        throw IONFILEExceptions.UnknownError()
    }
}

/**
 * get metadata from a file object (that can represent an actual file or a directory)
 *
 * @param fileObject the [File] representing a file or directory
 * @return metadata information on the file or directory
 */
@SuppressLint("NewApi") // lint not detecting version check in IONFILEBuildConfig
internal fun getMetadata(fileObject: File): IONFILEMetadataResult = IONFILEMetadataResult(
    fullPath = fileObject.absolutePath,
    name = fileObject.name,
    size = fileObject.length(),
    uri = Uri.fromFile(fileObject),
    type = if (fileObject.isDirectory) {
        IONFILEFileType.Directory
    } else {
        IONFILEFileType.File(mimeType = getMimeType(fileObject))
    },
    createdTimestamp = if (IONFILEBuildConfig.getAndroidSdkVersionCode() > Build.VERSION_CODES.O) {
        Files.readAttributes(fileObject.toPath(), BasicFileAttributes::class.java).let { attr ->
            // use the oldest of the two attributes
            min(attr.creationTime().toMillis(), attr.lastAccessTime().toMillis())
        }
    } else {
        null
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
        !sourceFileObj.exists() -> throw IONFILEExceptions.DoesNotExist(sourcePath)
        !forDirectories && (sourceFileObj.isDirectory || destinationFileObj.isDirectory) -> throw IONFILEExceptions.CopyRenameFailed.MixingFilesAndDirectories()
        forDirectories && (sourceFileObj.isFile || destinationFileObj.isFile) -> throw IONFILEExceptions.CopyRenameFailed.MixingFilesAndDirectories()
        destinationFileObj.parentFile?.exists() == false -> throw IONFILEExceptions.CopyRenameFailed.NoParentDirectory()
        forDirectories && destinationFileObj.isDirectory -> throw IONFILEExceptions.CopyRenameFailed.DestinationDirectoryExists(destinationPath)
        else -> block(sourceFileObj, destinationFileObj)
    }
}

/**
 * Validates offset and length parameters for file reading operations.
 * @param offset the number of bytes to skip before reading; must be >= 0
 * @param length the maximum number of bytes to read; must be > 0
 */
internal fun validateOffsetAndLength(offset: Int, length: Int) {
    require(offset >= 0) { "offset must be >= 0, but was $offset" }
    require(length > 0) { "length must be > 0, but was $length" }
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
            FILE_3GA_EXTENSION -> FILE_3GA_MIME_TYPE
            FILE_JAVASCRIPT_EXTENSION -> FILE_JAVASCRIPT_MIME_TYPE
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

