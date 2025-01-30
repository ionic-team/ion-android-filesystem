package io.ionic.libs.ionfilesystemlib.controller.internal

import android.util.Base64
import io.ionic.libs.ionfilesystemlib.model.IONFLSTEncoding
import io.ionic.libs.ionfilesystemlib.model.IONFLSTReadByChunksOptions
import io.ionic.libs.ionfilesystemlib.model.IONFLSTReadOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Reads the entire contents of an [InputStream] with options
 *
 * @param options the options for configuring how to read from the stream
 * @return the full contents of the stream
 */
internal fun InputStream.readFull(options: IONFLSTReadOptions): String =
    if (options.encoding is IONFLSTEncoding.WithCharset) {
        val reader =
            InputStreamReader(this, options.encoding.charset)
        reader.use { reader.readText() }
    } else {
        val byteArray = this.readBytes()
        Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

/**
 * Reads the contents of an [InputStream] in chunks,
 *
 * @param options for reading from the stream, including the chunk size to return
 * @param bufferSize the size of the buffer for reading from the stream.
 *  This is different from the chunk size, and should be a value that aligns with the OS page size
 *  The buffer size may alter the chunkSize value to be used; refer to [IONFLSTReadByChunksOptions]
 * @param onChunkRead suspend function in which to return each chunk that was read
 */
internal suspend fun InputStream.readByChunks(
    options: IONFLSTReadByChunksOptions,
    bufferSize: Int,
    onChunkRead: suspend (String) -> Unit,
) = withContext(Dispatchers.IO) {
    val chunkSize = minOf(options.chunkSize, available())
        .coerceAtLeast(bufferSize)
        .let {
            if (options.encoding == IONFLSTEncoding.Base64) {
                // make chunk size a multiple of 3 to not add padding to the end
                //  this is so that multiple chunks can be concatenated and decoded correctly
                (it / 3) * 3
            } else {
                it
            }
        }
    var bytesRead: Int
    var readCount = 0
    do {
        val byteArray = ByteArray(chunkSize)
        bytesRead = readChunk(byteArray, bufferSize)
        if (bytesRead > 0) {
            val byteArrayToConvert = byteArray.take(bytesRead).toByteArray()
            val readChunk = if (options.encoding is IONFLSTEncoding.WithCharset) {
                byteArrayToConvert.toString(options.encoding.charset)
            } else {
                Base64.encodeToString(byteArrayToConvert, Base64.NO_WRAP)
            }
            onChunkRead(readChunk)
            readCount += bytesRead
        } else if (readCount == 0) {
            // read 0 bytes overall, meaning file is empty;
            // will emmit an empty string to indicate empty file contents
            onChunkRead("")
        }
    } while (bytesRead > 0)
}

/**
 * Reads a single chunk from an [InputStream] to a byte array
 *
 * @param byteArray the array with the desired chunk size to read the
 * @param bufferSize the size of the buffer in which to perform each separate I/O read.
 *  Will use a smaller size if the byteArray is smaller than buffer size,
 *  or if the inputStream has less than bufferSize bytes left to read.
 * @return number of bytes read; if or less bytes were read, it means that the end of file has been reached
 */
private fun InputStream.readChunk(byteArray: ByteArray, bufferSize: Int): Int {
    var totalBytesRead = 0
    do {
        val len = minOf(byteArray.size - totalBytesRead, bufferSize)
        val bytesRead = read(byteArray, totalBytesRead, len)
        if (bytesRead > 0) {
            totalBytesRead += bytesRead
        }
    } while (bytesRead > 0 && totalBytesRead < byteArray.size)
    return totalBytesRead
}