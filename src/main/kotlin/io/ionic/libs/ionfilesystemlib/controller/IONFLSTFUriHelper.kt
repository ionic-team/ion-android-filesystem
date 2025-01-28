package io.ionic.libs.ionfilesystemlib.controller

import android.content.Context
import android.net.Uri
import android.os.Environment
import io.ionic.libs.ionfilesystemlib.model.LocalUriType
import io.ionic.libs.ionfilesystemlib.model.IONFLSTExceptions
import io.ionic.libs.ionfilesystemlib.model.IONFLSTFolderType
import io.ionic.libs.ionfilesystemlib.model.IONFLSTUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val CONTENT_SCHEME_NAME = "content"
private const val CONTENT_SCHEME = "$CONTENT_SCHEME_NAME://"
private const val SYNTHETIC_URI_PREFIX = "/synthetic/"
private const val FILE_SCHEME_NAME = "file"

class IONFLSTFUriHelper(context: Context) {

    private val internalCacheDir = context.cacheDir
    private val internalFilesDir = context.filesDir
    private val externalCacheDir = context.externalCacheDir
    private val externalFilesDir = context.getExternalFilesDir(null)

    /**
     * Resolves a URI for a file.
     *
     * Identifies the URI as belonging to a local file, or a file with content:// scheme
     *
     * @param unresolvedUri the URI to resolve; see [IONFLSTUri.Unresolved]
     * @return success with the resolved URI [IONFLSTUri.Resolved], or error otherwise
     */
    suspend fun resolveUri(
        unresolvedUri: IONFLSTUri.Unresolved
    ): Result<IONFLSTUri.Resolved> = runCatching {
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

                else -> throw IONFLSTExceptions.UnresolvableUri(unresolvedUri.uriPath)
            }
        } else {
            resolveAsLocalFile(parentFolderObject, unresolvedUri.uriPath)
        }
        resolvedUri
    }

    /**
     * Resolves as a content:// URI
     *
     * @param uri the content:// URI
     * @return a [IONFLSTUri.Resolved.Content] object
     */
    private fun resolveAsContentUri(uri: Uri): IONFLSTUri.Resolved.Content {
        return IONFLSTUri.Resolved.Content(uri)
    }

    /**
     * Resolves to a local file URI
     *
     * @param parentFolderFileObject the parent folder of the file, or null if there is none
     * @param localPath the local path to the file (minus the parent folder path)
     * @return a [IONFLSTUri.Resolved.Local] object
     */
    private suspend fun resolveAsLocalFile(
        parentFolderFileObject: File?,
        localPath: String
    ): IONFLSTUri.Resolved.Local = withContext(Dispatchers.IO) {
        val localFileObject = if (parentFolderFileObject != null) {
            File(parentFolderFileObject, localPath)
        } else {
            File(localPath)
        }
        val fileUri = Uri.fromFile(localFileObject)
        return@withContext IONFLSTUri.Resolved.Local(
            fullPath = localFileObject.path,
            uri = fileUri,
            type = try {
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
            throw IONFLSTExceptions.UnresolvableUri(path)
        }
        val location = path.substring(syntheticPathEndIndex, extensionIndex)
        val contentUriPrefix: String = CONTENT_SCHEME + "media/"
        return Uri.parse(contentUriPrefix + location)
    }

    /**
     * Get the full folder object from [IONFLSTFolderType] enum
     *
     * @return file object for a folder, or null if none was provided.
     */
    private fun IONFLSTFolderType?.getFolderFileObject(): File? =
        when (this) {
            IONFLSTFolderType.INTERNAL_CACHE -> internalCacheDir
            IONFLSTFolderType.INTERNAL_FILES -> internalFilesDir
            IONFLSTFolderType.EXTERNAL_CACHE -> externalCacheDir
            IONFLSTFolderType.EXTERNAL_FILES -> externalFilesDir
            IONFLSTFolderType.EXTERNAL_STORAGE -> Environment.getExternalStorageDirectory()
            IONFLSTFolderType.DOCUMENTS -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            null -> null
        }
}
