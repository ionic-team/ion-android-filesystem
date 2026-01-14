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
internal fun InputStream.readFull(options: IONFILEReadOptions): String {
    applyOffset(options.offset)
    if (options.length in 1..<LENGTH_DEFAULT_VALUE) {
        var chunkBytesRead: Int
        var readFile = ""
        var totalBytesRead = 0
        do {
            val remainingBytesToRead = options.length - totalBytesRead
            val bufferSize = minOf(
                options.encoding.convertChunkSize(DEFAULT_BUFFER_SIZE),
                remainingBytesToRead
            )
            val byteArray = ByteArray(bufferSize)
            chunkBytesRead = readChunk(byteArray, bufferSize)
            processReadChunk(byteArray, chunkBytesRead, options.encoding)?.let {
                totalBytesRead += chunkBytesRead
                readFile += it
            }
        } while (chunkBytesRead > 0 && totalBytesRead < options.length)
        return readFile
    } else {
        // use default platform methods - can be slightly more efficient
        return if (options.encoding is IONFILEEncoding.WithCharset) {
            InputStreamReader(this, options.encoding.charset).use {
                it.readText()
            }
        } else {
            Base64.encodeToString(this.readBytes(), Base64.NO_WRAP)
        }
    }
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
    applyOffset(options.offset)
    var chunkBytesRead: Int
    var totalBytesRead = 0
    do {
        val remainingBytesToRead = options.length - totalBytesRead
        val byteArray = ByteArray(minOf(chunkSize, remainingBytesToRead))
        chunkBytesRead = readChunk(byteArray, bufferSize)
        processReadChunk(byteArray, chunkBytesRead, options.encoding)?.let {
            totalBytesRead += chunkBytesRead
            onChunkRead(it)
        }
    } while (chunkBytesRead > 0 && totalBytesRead < options.length)
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
 * Converts a chunk of bytes to a String based on encoding and invokes a handler.
 *
 * @param byteArray the byte buffer containing data
 * @param bytesRead the number of valid bytes in the buffer
 * @param encoding the encoding to use for conversion
 * @return the read chunk if anything was read, or null otherwise
 */
private fun processReadChunk(
    byteArray: ByteArray,
    bytesRead: Int,
    encoding: IONFILEEncoding,
): String? {
    if (bytesRead <= 0) return null
    val byteArrayToConvert = byteArray.copyOf(bytesRead)
    val readChunk = if (encoding is IONFILEEncoding.WithCharset) {
        byteArrayToConvert.toString(encoding.charset)
    } else {
        Base64.encodeToString(byteArrayToConvert, Base64.NO_WRAP)
    }
    return readChunk
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
): Int = minOf(options.chunkSize, minOf(available() - options.offset, options.length))
    .coerceAtLeast(bufferSize)
    .let {
        options.encoding.convertChunkSize(it)
    }

private fun IONFILEEncoding.convertChunkSize(chunkSize: Int) = if (this == IONFILEEncoding.Base64) {
    // make chunk size the nearest highest multiple of 3, to not add padding to the end
    //  this is so that multiple chunks can be concatenated and decoded correctly
    chunkSize - (chunkSize % 3) + 3
} else {
    chunkSize
}

private fun InputStream.applyOffset(offset: Int) {
    if (offset > 0) {
        skipNBytes(offset.toLong())
    }
}