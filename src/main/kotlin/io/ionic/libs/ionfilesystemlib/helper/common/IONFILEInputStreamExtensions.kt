package io.ionic.libs.ionfilesystemlib.helper.common

import android.util.Base64
import io.ionic.libs.ionfilesystemlib.model.IONFILEEncoding
import io.ionic.libs.ionfilesystemlib.model.IONFILEReadInChunksOptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEReadOptions
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Reads the entire contents of an [InputStream] with options.
 *
 * This method is not responsible for closing the stream.
 *
 * @param options the options for configuring how to read from the stream
 * @return the full contents of the stream
 */
internal fun InputStream.readFull(options: IONFILEReadOptions): String =
    if (options.encoding is IONFILEEncoding.WithCharset) {
        InputStreamReader(this, options.encoding.charset).use { it.readText() }
    } else {
        Base64.encodeToString(this.readBytes(), Base64.NO_WRAP)
    }

/**
 * Reads the contents of an [InputStream] in chunks.
 *
 * This method is suspend, and should be called from a non-main thread (e.g. using Dispatchers.IO)
 *
 * This method is not responsible for closing the stream.
 *
 * @param options for reading from the stream, including the chunk size to return
 * @param bufferSize the size of the buffer for reading from the stream.
 *  This is different from the chunk size, and should be a value that aligns with the OS page size
 *  The buffer size may alter the chunkSize value to be used; refer to [IONFILEReadInChunksOptions]
 * @param onChunkRead suspend function in which to return each chunk that was read
 */
internal suspend fun InputStream.readByChunks(
    options: IONFILEReadInChunksOptions,
    bufferSize: Int,
    onChunkRead: suspend (String) -> Unit,
) {
    val chunkSize = calculateChunkSizeToUse(options, bufferSize)
    var bytesRead: Int
    do {
        val byteArray = ByteArray(chunkSize)
        bytesRead = readChunk(byteArray, bufferSize)
        if (bytesRead > 0) {
            val byteArrayToConvert = byteArray.take(bytesRead).toByteArray()
            val readChunk = if (options.encoding is IONFILEEncoding.WithCharset) {
                byteArrayToConvert.toString(options.encoding.charset)
            } else {
                Base64.encodeToString(byteArrayToConvert, Base64.NO_WRAP)
            }
            onChunkRead(readChunk)
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

/**
 * Calculates chunk size based on specified parameters.
 * The chunk size here refers to a max amount of file bytes to place to read.
 * Note that multiple I/O reads may take place, as governed by the [bufferSize].
 *
 * @param options the [IONFILEReadInChunksOptions], including the desired chunk size
 * @param bufferSize the buffer size, i.e. how much of a file to read into memory at once
 * @return the chunk size to be used.
 */
private fun InputStream.calculateChunkSizeToUse(
    options: IONFILEReadInChunksOptions,
    bufferSize: Int,
): Int = minOf(options.chunkSize, available())
    .coerceAtLeast(bufferSize)
    .let {
        if (options.encoding == IONFILEEncoding.Base64) {
            // make chunk size the nearest highest multiple of 3, to not add padding to the end
            //  this is so that multiple chunks can be concatenated and decoded correctly
            it - (it % 3) + 3
        } else {
            it
        }
    }