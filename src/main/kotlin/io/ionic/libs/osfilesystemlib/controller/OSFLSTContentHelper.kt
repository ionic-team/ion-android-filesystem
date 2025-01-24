package io.ionic.libs.osfilesystemlib.controller

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import io.ionic.libs.osfilesystemlib.controller.internal.FILE_MIME_TYPE_FALLBACK
import io.ionic.libs.osfilesystemlib.model.OSFLSTEncoding
import io.ionic.libs.osfilesystemlib.model.OSFLSTExceptions
import io.ionic.libs.osfilesystemlib.model.OSFLSTFileType
import io.ionic.libs.osfilesystemlib.model.OSFLSTMetadataResult
import io.ionic.libs.osfilesystemlib.model.OSFLSTReadOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.InputStreamReader

class OSFLSTContentHelper(private val contentResolver: ContentResolver) {

    /**
     * Reads contents of a file using content resolver
     *
     * @param uri the content uri to read the file from
     * @param options options for reading the file
     * @return success with file contents string if it was read successfully, error otherwise
     */
    suspend fun readFile(
        uri: Uri,
        options: OSFLSTReadOptions
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val inputStream =
                contentResolver.openInputStream(uri) ?: throw OSFLSTExceptions.UnknownError()
            val fileContents: String = if (options.encoding is OSFLSTEncoding.WithCharset) {
                val reader =
                    InputStreamReader(inputStream, options.encoding.charset)
                reader.use { reader.readText() }
            } else {
                val byteArray = inputStream.readBytes()
                Base64.encodeToString(byteArray, Base64.NO_WRAP)
            }
            return@runCatching fileContents
        }.mapError()
    }

    /**
     * Gets information about a file using content resolver
     *
     * @param uri the content uri to get the metadata from
     * @return success with result containing relevant file information, error otherwise
     */
    suspend fun getFileMetadata(uri: Uri): Result<OSFLSTMetadataResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cursor = contentResolver.query(uri, null, null, null, null)
                    ?: throw OSFLSTExceptions.UnknownError()
                cursor.use {
                    if (!cursor.moveToFirst()) {
                        throw OSFLSTExceptions.DoesNotExist()
                    }
                    contentResolver.persistedUriPermissions
                    val name = getNameForContentUri(cursor)
                    val size: Long = getSizeForContentUri(cursor, uri)
                    val lastModified: Long = getLastModifiedTimestampForContentUri(cursor)
                    val created: Long = getCreatedTimestampForContentUri(cursor)
                    val mimeType = contentResolver.getType(uri) ?: FILE_MIME_TYPE_FALLBACK
                    OSFLSTMetadataResult(
                        fullPath = uri.path ?: "",
                        name = name,
                        size = size,
                        type = OSFLSTFileType.File(mimeType),
                        createdTimestamp = created,
                        lastModifiedTimestamp = lastModified
                    )
                }
            }.mapError()
        }

    /**
     * Deletes a file using content resolver
     *
     * @param uri the content uri of the file to delete
     * @return success if file was deleted, error otherwise
     */
    suspend fun deleteFile(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val rowsDeleted = contentResolver.delete(uri, null, null)
            if (rowsDeleted > 0) {
                Unit
            } else {
                throw OSFLSTExceptions.DeleteFailed.Unknown()
            }
        }.mapError()
    }

    /**
     * Gets the name of a file in content uri
     *
     * @param cursor the android [Cursor] containing information about the uri
     * @return the name of the file, or exception if not found
     */
    private fun getNameForContentUri(cursor: Cursor): String {
        val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            .takeIf { it >= 0 }
            ?: cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                .takeIf { it >= 0 }
            ?: cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                .takeIf { it >= 0 }
        return columnIndex?.let { cursor.getString(columnIndex) }
            ?: throw OSFLSTExceptions.UnknownError()
    }

    /**
     * Gets the size of the that the content uri is pointing to
     *
     * @param cursor the android [Cursor] containing information about the uri
     * @param uri the content uri of the file, to try to open the file as a fallback if the cursor has no information
     * @return the size of the file, or 0 if it cannot be retrieved; throws exceptions in case file cannot be opened
     */
    private fun getSizeForContentUri(cursor: Cursor, uri: Uri): Long =
        cursor.getColumnIndex(OpenableColumns.SIZE).let { index ->
            if (index >= 0) {
                cursor.getString(index).toLongOrNull()
            } else {
                null
            }
        } ?: contentResolver.openAssetFileDescriptor(uri, "r")?.use {
            it.length
        } ?: 0L

    /**
     * Gets the last modified timestamp for a file in content uri
     *
     * @param cursor the android [Cursor] containing information about the uri
     * @return the timestamp of last modification for the file, or 0 if not found
     */
    private fun getLastModifiedTimestampForContentUri(cursor: Cursor): Long {
        val columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
            .takeIf { it >= 0 }
            ?: cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                .takeIf { it >= 0 }
        return columnIndex?.let { cursor.getString(columnIndex).toLongOrNull() }
        // Images from photoPicker in MediaStore may not have modification date; fallback to date of creation if available
            ?: getCreatedTimestampForContentUri(cursor)
    }

    /**
     * Gets the created timestamp for a file in content uri
     *
     * @param cursor the android [Cursor] containing information about the uri
     * @return the timestamp of creation for file, or 0 if not found
     */
    private fun getCreatedTimestampForContentUri(cursor: Cursor): Long {
        val columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
            .takeIf { it >= 0 }
            ?: cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
                .takeIf { it >= 0 }
        return columnIndex?.let { cursor.getString(columnIndex).toLongOrNull() } ?: 0L
    }

    private fun <T> Result<T>.mapError(): Result<T> =
        when (val ex = exceptionOrNull()) {
            is FileNotFoundException -> Result.failure(OSFLSTExceptions.DoesNotExist())
            is UnsupportedOperationException -> Result.failure(OSFLSTExceptions.NotAllowed())
            null -> this
            else -> Result.failure(ex)
        }
}