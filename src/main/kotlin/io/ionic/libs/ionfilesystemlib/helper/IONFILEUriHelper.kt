package io.ionic.libs.ionfilesystemlib.helper

import android.content.Context
import android.net.Uri
import android.os.Environment
import io.ionic.libs.ionfilesystemlib.model.LocalUriType
import io.ionic.libs.ionfilesystemlib.model.IONFILEExceptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEFolderType
import io.ionic.libs.ionfilesystemlib.model.IONFILEUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val CONTENT_SCHEME_NAME = "content"
private const val CONTENT_SCHEME = "$CONTENT_SCHEME_NAME://"
private const val SYNTHETIC_URI_PREFIX = "/synthetic/"
private const val FILE_SCHEME_NAME = "file"

internal class IONFILEUriHelper(context: Context) {

    private val internalCacheDir = context.cacheDir
    private val internalFilesDir = context.filesDir
    private val externalCacheDir = context.externalCacheDir
    private val externalFilesDir = context.getExternalFilesDir(null)

    /**
     * Resolves a URI for a file.
     *
     * Identifies the URI as belonging to a local file, or a file with content:// scheme
     *
     * @param unresolvedUri the URI to resolve; see [IONFILEUri.Unresolved]
     * @return success with the resolved URI [IONFILEUri.Resolved], or error otherwise
     */
    suspend fun resolveUri(
        unresolvedUri: IONFILEUri.Unresolved
    ): Result<IONFILEUri.Resolved> = runCatching {
        val parentFolderObject = unresolvedUri.parentFolder.getFolderFileObject()
        val resolvedUri = if (parentFolderObject == null) {
            val parsedUri = Uri.parse(unresolvedUri.uriPath)
            when {
                parsedUri.scheme == CONTENT_SCHEME_NAME -> resolveAsContentUri(parsedUri)

                unresolvedUri.uriPath.contains(SYNTHETIC_URI_PREFIX) ->
                    resolveAsContentUri(convertSyntheticToContentUri(unresolvedUri.uriPath))

                parsedUri.scheme == FILE_SCHEME_NAME -> resolveAsLocalFile(
                    parentFolderFileObject = null,
                    parsedUri.path ?: ""
                )

                parsedUri.scheme == null -> resolveAsLocalFile(
                    parentFolderFileObject = null,
                    unresolvedUri.uriPath
                )

                else -> throw IONFILEExceptions.UnresolvableUri(unresolvedUri.uriPath)
            }
        } else {
            resolveAsLocalFile(
                parentFolderObject,
                unresolvedUri.uriPath,
                assumeExternalStorage = unresolvedUri.parentFolder?.inExternalStorage
            )
        }
        resolvedUri
    }

    /**
     * Resolves as a content:// URI
     *
     * @param uri the content:// URI
     * @return a [IONFILEUri.Resolved.Content] object
     */
    private fun resolveAsContentUri(uri: Uri): IONFILEUri.Resolved.Content {
        return IONFILEUri.Resolved.Content(uri)
    }

    /**
     * Resolves to a local file URI
     *
     * @param parentFolderFileObject the parent folder of the file, or null if there is none
     * @param localPath the local path to the file (minus the parent folder path)
     * @param assumeExternalStorage true if file is in external storage, false if it does not,
     *  null if unknown (will be determined from file path)
     * @return a [IONFILEUri.Resolved.Local] object
     */
    private suspend fun resolveAsLocalFile(
        parentFolderFileObject: File?,
        localPath: String,
        assumeExternalStorage: Boolean? = null
    ): IONFILEUri.Resolved.Local = withContext(Dispatchers.IO) {
        val localFileObject = getLocalFileObject(parentFolderFileObject, localPath)
        val fileUri = Uri.fromFile(localFileObject)
        val isFileInExternalStorage =
            assumeExternalStorage ?: isInExternalStorage(localFileObject.absolutePath)
        return@withContext IONFILEUri.Resolved.Local(
            fullPath = localFileObject.path,
            uri = fileUri,
            type = getLocalUriType(localFileObject),
            inExternalStorage = isFileInExternalStorage
        )
    }

    /**
     * Converts file path of type /synthetic/ to a content:// URI
     *
     * A local file path with /synthetic/ can be returned by Android's Photo Picker, for example.
     * But this file will be readable via the content resolver, hence the change to content:// URI.
     *
     * @param path the path to the file containing /synthetic/ in it
     * @return a content:// URI
     */
    private fun convertSyntheticToContentUri(path: String): Uri {
        val syntheticPathEndIndex =
            path.lastIndexOf(SYNTHETIC_URI_PREFIX) + SYNTHETIC_URI_PREFIX.length
        val extensionIndex = path.lastIndexOf('.')
        if (extensionIndex < syntheticPathEndIndex) {
            // the path has no extension, meaning it cannot really be a file mapped to content:// scheme
            throw IONFILEExceptions.UnresolvableUri(path)
        }
        val location = path.substring(syntheticPathEndIndex, extensionIndex)
        val contentUriPrefix: String = CONTENT_SCHEME + "media/"
        return Uri.parse(contentUriPrefix + location)
    }

    /**
     * Get a reference to the local file
     *
     * @param parentFolderFileObject the parent folder of the file, or null if there is none
     * @param localPath the local path to the file (minus the parent folder path)
     * @return a [File] object pointing to the local file
     */
    private fun getLocalFileObject(parentFolderFileObject: File?, localPath: String): File =
        parentFolderFileObject?.let { File(it, localPath) } ?: File(localPath)

    /**
     * Gets the type of local file uri
     *
     * @param localFileObject The [File] object pointing to the local file
     * @return the [LocalUriType]
     */
    private fun getLocalUriType(localFileObject: File): LocalUriType = try {
        when {
            !localFileObject.exists() -> LocalUriType.UNKNOWN
            localFileObject.isDirectory -> LocalUriType.DIRECTORY
            localFileObject.isFile -> LocalUriType.FILE
            else -> LocalUriType.UNKNOWN
        }
    } catch (secEx: SecurityException) {
        // could be that getting file information requires permissions that are not granted
        LocalUriType.UNKNOWN
    }

    /**
     * Checks if the provided local file path is in external storage.
     *
     * Will see if the file path is on a known parent folder of [IONFILEFolderType], and
     *  if so, will return whether that folder is in external storage;
     * Otherwise, if it's not in a known parent folder, will assume it is in external storage.
     *
     * @param localFilePath full path to the local file
     * @return true if file is in external storage, false otherwise.
     */
    private fun isInExternalStorage(
        localFilePath: String
    ): Boolean = IONFILEFolderType.entries.firstOrNull {
        it.getFolderFileObject()?.let { folderFileObject ->
            localFilePath.contains(folderFileObject.absolutePath)
        } ?: false
    }?.inExternalStorage ?: true

    /**
     * Get the full folder object from [IONFILEFolderType] enum
     *
     * @return file object for a folder, or null if none was provided.
     */
    private fun IONFILEFolderType?.getFolderFileObject(): File? =
        when (this) {
            IONFILEFolderType.INTERNAL_CACHE -> internalCacheDir
            IONFILEFolderType.INTERNAL_FILES -> internalFilesDir
            IONFILEFolderType.EXTERNAL_CACHE -> externalCacheDir
            IONFILEFolderType.EXTERNAL_FILES -> externalFilesDir
            IONFILEFolderType.EXTERNAL_STORAGE -> Environment.getExternalStorageDirectory()
            IONFILEFolderType.DOCUMENTS -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            null -> null
        }
}
