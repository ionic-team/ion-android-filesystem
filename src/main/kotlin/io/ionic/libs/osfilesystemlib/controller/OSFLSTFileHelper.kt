package io.ionic.libs.osfilesystemlib.controller

import android.util.Base64
import io.ionic.libs.osfilesystemlib.model.OSFLSTCreateOptions
import io.ionic.libs.osfilesystemlib.model.OSFLSTDeleteOptions
import io.ionic.libs.osfilesystemlib.model.OSFLSTEncoding
import io.ionic.libs.osfilesystemlib.model.OSFLSTExceptions
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

class OSFLSTFileHelper {

    /**
     * Create a directory
     *
     * @param options options to create the directory
     * @return success if the directory was created successfully, error otherwise
     */
    suspend fun createDirectory(options: OSFLSTCreateOptions): Result<Unit> =
        withContext(Dispatchers.IO) { createDirOrFile(options, isDirectory = true) }

    /**
     * Create a file
     *
     * @param options options to create the file
     * @return success if the file was created successfully, error otherwise
     */
    suspend fun createFile(options: OSFLSTCreateOptions): Result<Unit> =
        withContext(Dispatchers.IO) { createDirOrFile(options, isDirectory = false) }

    /**
     * Delete a file or directory
     *
     * @param options options to delete the file/directory
     * @return success if the file/directory was deleted successfully, error otherwise
     */
    suspend fun delete(options: OSFLSTDeleteOptions): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(options.fullPath)
            if (!file.exists()) {
                throw OSFLSTExceptions.DeleteFailed.DoesNotExist()
            }
            val deleteSucceeded = if (file.isDirectory) {
                if (!file.listFiles().isNullOrEmpty() && !options.recursive) {
                    throw OSFLSTExceptions.DeleteFailed.CannotDeleteChildren()
                }
                file.deleteRecursively()
            } else {
                file.delete()
            }
            if (!deleteSucceeded) {
                throw OSFLSTExceptions.DeleteFailed.Unknown()
            }
        }
    }

    /**
     * Saves data to a file
     *
     * @param options options to save the file
     * @return success if the file was saved successfully, error otherwise
     */
    suspend fun saveFile(options: OSFLSTSaveOptions): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(options.fullPath)
            if (!file.exists()) {
                if (options.createFileRecursive == null) {
                    throw OSFLSTExceptions.SaveFailed.DoesNotExist()
                } else {
                    val createFileResult = createFile(
                        OSFLSTCreateOptions(
                            options.fullPath,
                            recursive = options.createFileRecursive,
                            exclusive = false
                        )
                    )
                    createFileResult.exceptionOrNull()?.let { throw it }
                }
            } else if (file.isDirectory) {
                return@withContext Result.failure(OSFLSTExceptions.SaveFailed.IsDirectory())
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
     *
     * @param options options for reading the file
     * @return success with file contents string if it was read successfully, error otherwise
     */
    suspend fun readFile(options: OSFLSTReadOptions): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(options.fullPath)
            if (!file.exists()) {
                throw OSFLSTExceptions.ReadFailed.DoesNotExist()
            } else if (file.isDirectory) {
                throw OSFLSTExceptions.ReadFailed.IsDirectory()
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
     * Create a directory or file
     *
     * Most logic is common to both, except the actual creation
     *
     * @param options options to create the file/directory
     * @param isDirectory true if meant to create directory, false if meant to create file
     * @return success if the file/directory was created successfully, error otherwise
     */
    private fun createDirOrFile(
        options: OSFLSTCreateOptions,
        isDirectory: Boolean
    ): Result<Unit> = runCatching {
        val file = File(options.fullPath)
        if (file.exists()) {
            if (options.exclusive) {
                throw OSFLSTExceptions.CreateFailed.AlreadyExists()
            } else {
                // file/directory creation is not going to do anything if file/directory already exists
                return@runCatching
            }
        }
        if (!checkParentDirectory(file, create = options.recursive)) {
            throw OSFLSTExceptions.CreateFailed.NoParentDirectory()
        }
        val createSucceeded = if (isDirectory) {
            file.mkdir()
        } else {
            file.createNewFile()
        }
        if (!createSucceeded) {
            throw OSFLSTExceptions.CreateFailed.Unknown()
        }
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
}