package io.ionic.libs.ionfilesystemlib.helper

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import io.ionic.libs.ionfilesystemlib.helper.common.FILE_MIME_TYPE_FALLBACK
import io.ionic.libs.ionfilesystemlib.helper.common.readByChunks
import io.ionic.libs.ionfilesystemlib.helper.common.readFull
import io.ionic.libs.ionfilesystemlib.model.IONFILEExceptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEFileType
import io.ionic.libs.ionfilesystemlib.model.IONFILEMetadataResult
import io.ionic.libs.ionfilesystemlib.model.IONFILEReadInChunksOptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEReadOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

internal class IONFILEContentHelper(private val contentResolver: ContentResolver) {

    /**
     * Reads contents of a file using content resolver
     *
     * @param uri the content uri to read the file from
     * @param options options for reading the file
     * @return success with file contents string if it was read successfully, error otherwise
     */
    suspend fun readFile(
        uri: Uri,
        options: IONFILEReadOptions
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val fileContents: String = contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readFull(options)
            } ?: throw IONFILEExceptions.UnknownError()
            return@runCatching fileContents
        }.mapError(uri)
    }

    /**
     * Reads the contents of a file in chunks
     *
     * Useful when the file does not fit entirely in memory.
     *
     * @param uri the content uri to read the file from
     * @param options options for reading the file in chunks; refer to [IONFILEReadInChunksOptions]
     * @return a (cold) flow in which the chunks are emitted;
     * the flow completes after all chunks are emitted (unless an error occurs somewhere in-between)
     */
    fun readFileInChunks(
        uri: Uri,
        options: IONFILEReadInChunksOptions
    ): Flow<String> = flow {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readByChunks(
                options,
                DEFAULT_BUFFER_SIZE,
                onChunkRead = { chunk -> emit(chunk) }
            )
        } ?: throw IONFILEExceptions.UnknownError()
    }.flowOn(Dispatchers.IO)
        .catch { throw it.mapError(uri) }

    /**
     * Gets information about a file using content resolver
     *
     * @param uri the content uri to get the metadata from
     * @return success with result containing relevant file information, error otherwise
     */
    suspend fun getFileMetadata(uri: Uri): Result<IONFILEMetadataResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cursor = contentResolver.query(uri, null, null, null, null)
                    ?: throw IONFILEExceptions.UnknownError()
                cursor.use {
                    if (!cursor.moveToFirst()) {
                        throw IONFILEExceptions.DoesNotExist(path = uri.toString())
                    }
                    contentResolver.persistedUriPermissions
                    val name = getNameForContentUri(cursor)
                    val size: Long = getSizeForContentUri(cursor, uri)
                    val lastModified: Long = getLastModifiedTimestampForContentUri(cursor)
                    val created: Long = getCreatedTimestampForContentUri(cursor)
                    val mimeType = contentResolver.getType(uri) ?: FILE_MIME_TYPE_FALLBACK
                    IONFILEMetadataResult(
                        fullPath = uri.path ?: "",
                        name = name,
                        uri = uri,
                        size = size,
                        type = IONFILEFileType.File(mimeType),
                        createdTimestamp = created,
                        lastModifiedTimestamp = lastModified
                    )
                }
            }.mapError(uri)
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
            if (rowsDeleted <= 0) {
                throw IONFILEExceptions.UnknownError()
            }
        }.mapError(uri)
    }

    /**
     * Copy a file from one uri to a local file.
     *
     * Copying files from a content:// uri to another is not supported,
     * as we cannot create new files with content:// scheme
     *
     * @param sourceUri the full content:// uri to the source file
     * @param destinationPath a local file path to copy to
     * @return success if the file was copied successfully, error otherwise
     */
    suspend fun copyFile(
        sourceUri: Uri,
        destinationPath: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val destinationFileObj = File(destinationPath)
            when {
                destinationFileObj.isDirectory -> throw IONFILEExceptions.CopyRenameFailed.MixingFilesAndDirectories()
                destinationFileObj.parentFile?.exists() == false -> throw IONFILEExceptions.CopyRenameFailed.NoParentDirectory()
            }
            contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(destinationFileObj).use { outputStream ->
                    val bytesWritten = inputStream.copyTo(outputStream)
                    if (bytesWritten <= 0) {
                        throw IONFILEExceptions.UnknownError()
                    }
                }
            } ?: throw IONFILEExceptions.UnknownError()
        }.mapError(sourceUri)
    }

    /**
     * Gets the name of a file in content uri
     *
     * @param cursor the android [Cursor] containing information about the uri
     * @return the name of the file, or exception if not found
     */
    private fun getNameForContentUri(cursor: Cursor): String {
        val columnIndex = cursor.getColumnIndexForNames(
            columnNames = listOf(
                OpenableColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            )
        )
        return columnIndex?.let { cursor.getString(columnIndex) }
            ?: throw IONFILEExceptions.UnknownError()
    }

    /**
     * Gets the size of the that the content uri is pointing to.
     *
     * Will try to open the file and get its size if the android [Cursor] does not have the necessary column.
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
        val columnIndex = cursor.getColumnIndexForNames(
            columnNames = listOf(
                MediaStore.MediaColumns.DATE_MODIFIED,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )
        )
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
        val columnIndex = cursor.getColumnIndexForNames(
            columnNames = listOf(
                MediaStore.MediaColumns.DATE_TAKEN,
                MediaStore.MediaColumns.DATE_ADDED
            )
        )
        return columnIndex?.let { cursor.getString(columnIndex).toLongOrNull() } ?: 0L
    }

    private fun Cursor.getColumnIndexForNames(
        columnNames: List<String>
    ): Int? = columnNames.firstNotNullOfOrNull { getColumnIndex(it).takeIf { index -> index >= 0 } }

    private fun <T> Result<T>.mapError(uri: Uri): Result<T> =
        exceptionOrNull()?.let { Result.failure(it.mapError(uri)) } ?: this

    private fun Throwable.mapError(uri: Uri): Throwable = when (this) {
        is FileNotFoundException -> IONFILEExceptions.DoesNotExist(path = uri.toString(), cause = this)
        is UnsupportedOperationException -> IONFILEExceptions.UnknownError(cause = this)
        else -> this
    }
}