package io.ionic.libs.ionfilesystemlib.helper

import android.util.Base64
import androidx.annotation.VisibleForTesting
import io.ionic.libs.ionfilesystemlib.helper.common.createDirOrFile
import io.ionic.libs.ionfilesystemlib.helper.common.deleteDirOrFile
import io.ionic.libs.ionfilesystemlib.helper.common.getMetadata
import io.ionic.libs.ionfilesystemlib.helper.common.prepareForCopyOrRename
import io.ionic.libs.ionfilesystemlib.helper.common.readByChunks
import io.ionic.libs.ionfilesystemlib.helper.common.readFull
import io.ionic.libs.ionfilesystemlib.model.IONFILECreateOptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEDeleteOptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEEncoding
import io.ionic.libs.ionfilesystemlib.model.IONFILEExceptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEMetadataResult
import io.ionic.libs.ionfilesystemlib.model.IONFILEReadInChunksOptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEReadOptions
import io.ionic.libs.ionfilesystemlib.model.IONFILESaveMode
import io.ionic.libs.ionfilesystemlib.model.IONFILESaveOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStreamWriter

internal class IONFILELocalFilesHelper {

    /**
     * Reads contents of a file

     * @param fullPath full path of the file to read from
     * @param options options for reading the file
     * @return success with file contents string if it was read successfully, error otherwise
     */
    suspend fun readFile(
        fullPath: String,
        options: IONFILEReadOptions
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(fullPath)
            if (!file.exists()) {
                throw IONFILEExceptions.DoesNotExist()
            }
            val inputStream = FileInputStream(file)
            val fileContents: String = inputStream.readFull(options)
            return@runCatching fileContents
        }
    }

    /**
     * Reads the contents of a file in chunks
     *
     * Useful when the file does not fit entirely in memory.
     *
     * @param fullPath full path of the file to read from
     * @param options options for reading the file in chunks; refer to [IONFILEReadInChunksOptions]
     * @return a (cold) flow in which the chunks are emitted;
     * the flow completes after all chunks are emitted (unless an error occurs somewhere in-between)
     */
    fun readFileInChunks(
        fullPath: String,
        options: IONFILEReadInChunksOptions,
    ): Flow<String> = readFileInChunks(fullPath, options, bufferSize = DEFAULT_BUFFER_SIZE)

    /**
     * Internal method for reading the contents of a file in chunks, allowing to pass a variable buffer size
     *
     * @param fullPath full path of the file to read from
     * @param options options for reading the file in chunks; refer to [IONFILEReadInChunksOptions]
     * @param bufferSize the size of the buffer for reading from the stream.
     *  This is different from the chunk size, and should be a value that aligns with the OS page size
     *  The buffer size may alter the chunkSize value to be used; refer to [IONFILEReadInChunksOptions]
     * @return a (cold) flow in which the chunks are emitted; the flow completes after emissions
     */
    @VisibleForTesting
    internal fun readFileInChunks(
        fullPath: String,
        options: IONFILEReadInChunksOptions,
        bufferSize: Int
    ): Flow<String> = flow {
        val file = File(fullPath)
        if (!file.exists()) {
            throw IONFILEExceptions.DoesNotExist()
        }
        FileInputStream(file).use { inputStream ->
            inputStream.readByChunks(
                options,
                bufferSize,
                onChunkRead = { chunk -> emit(chunk) }
            )
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Gets information about a file or directory
     *
     * @param fullPath the full path to the file or directory
     * @return success with result containing relevant file information, error otherwise
     */
    suspend fun getFileMetadata(fullPath: String): Result<IONFILEMetadataResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(fullPath)
                if (!file.exists()) {
                    throw IONFILEExceptions.DoesNotExist()
                }
                getMetadata(fileObject = file)
            }
        }

    /**
     * Create a file
     *
     * @param fullPath full path to the file to create
     * @param options options to create the file
     * @return success if the file was created successfully, error otherwise
     */
    suspend fun createFile(fullPath: String, options: IONFILECreateOptions): Result<Unit> =
        withContext(Dispatchers.IO) { createDirOrFile(fullPath, options, isDirectory = false) }

    /**
     * Saves data to a file
     *
     * @param fullPath full path to the file to save
     * @param options options to save the file
     * @return success if the file was saved successfully, error otherwise
     */
    suspend fun saveFile(
        fullPath: String,
        options: IONFILESaveOptions
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(fullPath)
            if (!file.exists()) {
                val createFileResult = createFile(
                    fullPath,
                    IONFILECreateOptions(recursive = options.createFileRecursive)
                )
                createFileResult.exceptionOrNull()?.let { throw it }
            }
            val fileStream = FileOutputStream(file, options.mode == IONFILESaveMode.APPEND)
            if (options.encoding is IONFILEEncoding.WithCharset) {
                val writer =
                    BufferedWriter(OutputStreamWriter(fileStream, options.encoding.charset))
                writer.use { writer.write(options.data) }
            } else {
                val outputStream = BufferedOutputStream(fileStream)
                val dataToDecode = if (options.data.contains(",")) {
                    // it is possible that the data comes as a data url "data:<type>;base64, <base64content>"
                    options.data.split(",")[1].trim()
                } else {
                    options.data
                }
                val base64Data = Base64.decode(dataToDecode, Base64.NO_WRAP)
                outputStream.use { outputStream.write(base64Data) }
            }
        }
    }

    /**
     * Delete a file

     * @param fullPath the full path to the file
     * @return success if the file was deleted successfully, error otherwise
     */
    suspend fun deleteFile(fullPath: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            deleteDirOrFile(fullPath, IONFILEDeleteOptions(recursive = false))
        }

    /**
     * Copy a file from one path to another
     *
     * @param sourcePath the full path to the source file
     * @param destinationPath the full path to the destination file
     * @return success if the file was copied successfully, error otherwise
     */
    suspend fun copyFile(
        sourcePath: String,
        destinationPath: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            prepareForCopyOrRename(
                sourcePath, destinationPath, forDirectories = false
            ) { sourceFileObj: File, destinationFileObj: File ->
                val copiedFile = sourceFileObj.copyTo(destinationFileObj, overwrite = true)
                if (!copiedFile.exists()) {
                    throw IONFILEExceptions.UnknownError()
                }
            }
        }
    }

    /**
     * Rename or move a file from one path to another.
     *
     * If rename/move fails, will attempt to manually copy the source and then delete it, as a fallback
     *
     * @param sourcePath the full path to the source file
     * @param destinationPath the full path to the destination file
     * @return success if the file was renamed/moved successfully, error otherwise
     */
    suspend fun renameFile(
        sourcePath: String,
        destinationPath: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            prepareForCopyOrRename(
                sourcePath, destinationPath, forDirectories = false
            ) { sourceFileObj: File, destinationFileObj: File ->
                destinationFileObj.delete()
                val renameSuccessful = sourceFileObj.renameTo(destinationFileObj)
                if (!renameSuccessful) {
                    copyFile(sourcePath, destinationPath).getOrElse {
                        throw IONFILEExceptions.UnknownError()
                    }
                    deleteFile(sourcePath).getOrElse { throw IONFILEExceptions.UnknownError() }
                }
            }
        }
    }
}