package io.ionic.libs.ionfilesystemlib.helper

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import io.ionic.libs.ionfilesystemlib.helper.internal.FILE_MIME_TYPE_FALLBACK
import io.ionic.libs.ionfilesystemlib.helper.internal.readByChunks
import io.ionic.libs.ionfilesystemlib.helper.internal.readFull
import io.ionic.libs.ionfilesystemlib.model.IONFLSTExceptions
import io.ionic.libs.ionfilesystemlib.model.IONFLSTFileType
import io.ionic.libs.ionfilesystemlib.model.IONFLSTMetadataResult
import io.ionic.libs.ionfilesystemlib.model.IONFLSTReadByChunksOptions
import io.ionic.libs.ionfilesystemlib.model.IONFLSTReadOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

class IONFLSTContentHelper(private val contentResolver: ContentResolver) {

    /**
     * Reads contents of a file using content resolver
     *
     * @param uri the content uri to read the file from
     * @param options options for reading the file
     * @return success with file contents string if it was read successfully, error otherwise
     */
    suspend fun readFile(
        uri: Uri,
        options: IONFLSTReadOptions
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val fileContents: String = contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readFull(options)
            } ?: throw IONFLSTExceptions.UnknownError()
            return@runCatching fileContents
        }.mapError()
    }

    /**
     * Reads the contents of a file in chunks
     *
     * Useful when the file does not fit in entirely memory.
     *
     * @param options options for reading the file in chunks; refer to [IONFLSTReadByChunksOptions]
     * @return a (cold) flow in which the chunks are emitted;
     * the flow completes after all chunks are emitted (unless an error occurs somewhere in-between)
     */
    fun readFileByChunks(
        uri: Uri,
        options: IONFLSTReadByChunksOptions
    ): Flow<String> = flow {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readByChunks(
                options,
                DEFAULT_BUFFER_SIZE,
                onChunkRead = { chunk -> emit(chunk) }
            )
        } ?: throw IONFLSTExceptions.UnknownError()
    }.catch { throw it.mapError() }
        .flowOn(Dispatchers.IO)

    /**
     * Gets information about a file using content resolver
     *
     * @param uri the content uri to get the metadata from
     * @return success with result containing relevant file information, error otherwise
     */
    suspend fun getFileMetadata(uri: Uri): Result<IONFLSTMetadataResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cursor = contentResolver.query(uri, null, null, null, null)
                    ?: throw IONFLSTExceptions.UnknownError()
                cursor.use {
                    if (!cursor.moveToFirst()) {
                        throw IONFLSTExceptions.DoesNotExist()
                    }
                    contentResolver.persistedUriPermissions
                    val name = getNameForContentUri(cursor)
                    val size: Long = getSizeForContentUri(cursor, uri)
                    val lastModified: Long = getLastModifiedTimestampForContentUri(cursor)
                    val created: Long = getCreatedTimestampForContentUri(cursor)
                    val mimeType = contentResolver.getType(uri) ?: FILE_MIME_TYPE_FALLBACK
                    IONFLSTMetadataResult(
                        fullPath = uri.path ?: "",
                        name = name,
                        uri = uri,
                        size = size,
                        type = IONFLSTFileType.File(mimeType),
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
                throw IONFLSTExceptions.DeleteFailed.Unknown()
            }
        }.mapError()
    }

    /**
     * Copy a file from one uri to a local file.
     *
     * Copying files from a content:// uri to another is not supported,
     * as we cannot create new files with content:// scheme
     *
     * @param sourceUri the full content:// uri to the source file
     * @param destinationPath a local file path to copy to
     * @return success if the file was copied successfully, false otherwise
     */
    suspend fun copyFile(
        sourceUri: Uri,
        destinationPath: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val destinationFileObj = File(destinationPath)
            when {
                destinationFileObj.isDirectory -> throw IONFLSTExceptions.CopyRenameFailed.MixingFilesAndDirectories()
                destinationFileObj.parentFile?.exists() == false -> throw IONFLSTExceptions.CopyRenameFailed.NoParentDirectory()
            }
            contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(destinationFileObj).use { outputStream ->
                    val bytesWritten = inputStream.copyTo(outputStream)
                    if (bytesWritten <= 0) {
                        throw IONFLSTExceptions.UnknownError()
                    }
                }
            } ?: throw IONFLSTExceptions.UnknownError()
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
            ?: throw IONFLSTExceptions.UnknownError()
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
        exceptionOrNull()?.let { Result.failure(it.mapError()) } ?: this

    private fun Throwable.mapError(): Throwable = when (this) {
        is FileNotFoundException -> IONFLSTExceptions.DoesNotExist()
        is UnsupportedOperationException -> IONFLSTExceptions.UnknownError()
        else -> this
    }
}