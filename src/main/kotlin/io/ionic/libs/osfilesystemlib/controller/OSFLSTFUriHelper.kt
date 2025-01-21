package io.ionic.libs.osfilesystemlib.controller

import android.content.Context
import android.net.Uri
import android.os.Environment
import io.ionic.libs.osfilesystemlib.model.LocalUriType
import io.ionic.libs.osfilesystemlib.model.OSFLSTExceptions
import io.ionic.libs.osfilesystemlib.model.OSFLSTFolderType
import io.ionic.libs.osfilesystemlib.model.OSFLSTUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val CONTENT_SCHEME_NAME = "content"
private const val CONTENT_SCHEME = "$CONTENT_SCHEME_NAME://"
private const val SYNTHETIC_URI_PREFIX = "/synthetic/"
private const val FILE_SCHEME_NAME = "file"

class OSFLSTFUriHelper {

    suspend fun resolveUri(
        context: Context,
        unresolvedUri: OSFLSTUri.Unresolved
    ): Result<OSFLSTUri.Resolved> = runCatching {
        val parentFolderObject = unresolvedUri.parentFolder.getFolderFileObject(context)
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

                else -> throw OSFLSTExceptions.UnresolvableUri(unresolvedUri.uriPath)
            }
        } else {
            resolveAsLocalFile(parentFolderObject, unresolvedUri.uriPath)
        }
        resolvedUri
    }

    private fun resolveAsContentUri(uri: Uri): OSFLSTUri.Resolved.Content {
        return OSFLSTUri.Resolved.Content(uri)
    }

    private suspend fun resolveAsLocalFile(
        parentFolderFileObject: File?,
        localPath: String
    ): OSFLSTUri.Resolved.Local = withContext(Dispatchers.IO) {
        val localFileObject = if (parentFolderFileObject != null) {
            File(parentFolderFileObject, localPath)
        } else {
            File(localPath)
        }
        val fileUri = Uri.fromFile(localFileObject)
        return@withContext OSFLSTUri.Resolved.Local(
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

    private fun convertSyntheticToContentUri(path: String): Uri {
        val syntheticPathEndIndex =
            path.lastIndexOf(SYNTHETIC_URI_PREFIX) + SYNTHETIC_URI_PREFIX.length
        val extensionIndex = path.lastIndexOf('.')
        if (extensionIndex < syntheticPathEndIndex) {
            // the path has no extension, meaning it cannot really be a file mapped to content:// scheme
            throw OSFLSTExceptions.UnresolvableUri(path)
        }
        val location = path.substring(syntheticPathEndIndex, extensionIndex)
        val contentUriPrefix: String = CONTENT_SCHEME + "media/"
        return Uri.parse(contentUriPrefix + location)
    }

    private fun OSFLSTFolderType?.getFolderFileObject(context: Context): File? =
        when (this) {
            OSFLSTFolderType.INTERNAL_CACHE -> context.cacheDir
            OSFLSTFolderType.INTERNAL_FILES -> context.filesDir
            OSFLSTFolderType.EXTERNAL_CACHE -> context.externalCacheDir
            OSFLSTFolderType.EXTERNAL_FILES -> context.getExternalFilesDir(null)
            OSFLSTFolderType.EXTERNAL_STORAGE -> Environment.getExternalStorageDirectory()
            OSFLSTFolderType.DOCUMENTS -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            null -> null
        }
}
