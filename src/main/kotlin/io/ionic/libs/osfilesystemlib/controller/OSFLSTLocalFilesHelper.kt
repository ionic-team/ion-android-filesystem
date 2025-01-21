package io.ionic.libs.osfilesystemlib.controller

import android.util.Base64
import io.ionic.libs.osfilesystemlib.controller.internal.createDirOrFile
import io.ionic.libs.osfilesystemlib.controller.internal.deleteDirOrFile
import io.ionic.libs.osfilesystemlib.controller.internal.getMetadata
import io.ionic.libs.osfilesystemlib.model.OSFLSTCreateOptions
import io.ionic.libs.osfilesystemlib.model.OSFLSTDeleteOptions
import io.ionic.libs.osfilesystemlib.model.OSFLSTEncoding
import io.ionic.libs.osfilesystemlib.model.OSFLSTExceptions
import io.ionic.libs.osfilesystemlib.model.OSFLSTMetadataResult
import io.ionic.libs.osfilesystemlib.model.OSFLSTReadOptions
import io.ionic.libs.osfilesystemlib.model.OSFLSTSaveMode
import io.ionic.libs.osfilesystemlib.model.OSFLSTSaveOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class OSFLSTLocalFilesHelper {

    /**
     * Create a file
     *
     * @param fullPath full path to the file to create
     * @param options options to create the file
     * @return success if the file was created successfully, error otherwise
     */
    suspend fun createFile(fullPath: String, options: OSFLSTCreateOptions): Result<Unit> =
        withContext(Dispatchers.IO) { createDirOrFile(fullPath, options, isDirectory = false) }

    /**
     * Delete a file

     * @param fullPath the full path to the file
     * @return success if the file was deleted successfully, error otherwise
     */
    suspend fun deleteFile(fullPath: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            deleteDirOrFile(fullPath, OSFLSTDeleteOptions(recursive = false))
        }

    /**
     * Saves data to a file
     *
     * @param fullPath full path to the file to save
     * @param options options to save the file
     * @return success if the file was saved successfully, error otherwise
     */
    suspend fun saveFile(
        fullPath: String,
        options: OSFLSTSaveOptions
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(fullPath)
            if (!file.exists()) {
                if (options.createFileRecursive == null) {
                    throw OSFLSTExceptions.DoesNotExist()
                } else {
                    val createFileResult = createFile(
                        fullPath,
                        OSFLSTCreateOptions(
                            recursive = options.createFileRecursive,
                            exclusive = false
                        )
                    )
                    createFileResult.exceptionOrNull()?.let { throw it }
                }
            }
            val fileStream = FileOutputStream(file, options.mode == OSFLSTSaveMode.APPEND)
            if (options.encoding is OSFLSTEncoding.WithCharset) {
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
     * Reads contents of a file

     * @param fullPath full path of the file to read from
     * @param options options for reading the file
     * @return success with file contents string if it was read successfully, error otherwise
     */
    suspend fun readFile(
        fullPath: String,
        options: OSFLSTReadOptions
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(fullPath)
            if (!file.exists()) {
                throw OSFLSTExceptions.DoesNotExist()
            }
            val inputStream = FileInputStream(file)
            val fileContents: String = if (options.encoding is OSFLSTEncoding.WithCharset) {
                val reader =
                    InputStreamReader(inputStream, options.encoding.charset)
                reader.use { reader.readText() }
            } else {
                val byteArray = inputStream.readBytes()
                Base64.encodeToString(byteArray, Base64.NO_WRAP)
            }
            return@runCatching fileContents
        }
    }

    /**
     * Gets information about a file or directory
     *
     * @param fullPath the full path to the file or directory
     * @return success with result containing relevant file information, error otherwise
     */
    suspend fun getFileMetadata(fullPath: String): Result<OSFLSTMetadataResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(fullPath)
                if (!file.exists()) {
                    throw OSFLSTExceptions.DoesNotExist()
                }
                getMetadata(fileObject = file)
            }
        }
}