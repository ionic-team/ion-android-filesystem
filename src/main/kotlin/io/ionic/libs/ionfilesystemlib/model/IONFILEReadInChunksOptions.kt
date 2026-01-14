package io.ionic.libs.ionfilesystemlib.model

import io.ionic.libs.ionfilesystemlib.helper.common.LENGTH_DEFAULT_VALUE

/**
 * Parameters for reading a file in chunks
 *
 * @param encoding how the file data to return should be encoded; see [IONFILEEncoding]
 * @param chunkSize the desired amount of chunks to read from a file at a time.
 *  The actual chunkSize that the library uses may be different than what was supplied:
 *  1. If the file is smaller than chunkSize, it will only allocate enough bytes to read the file.
 *  2. [kotlin.io.DEFAULT_BUFFER_SIZE] is used if chunkSize supplied is smaller than it.
 *      This is to make sure the read operations do not take too long.
 *  3. When encoding is [IONFILEEncoding.Base64], chunkSize is set to a multiple of 3.
 *      This is because text that has a size that is a multiple of 3 does not get
 *      This means multiple chunks can be concatenated and decoded together without losing data.
 *  There is no check on a maximum value for chunkSize, meaning that if you provide a very large value,
 *      an OutOfMemoryError may be thrown. Avoid using chunkSize larger than a few MB.
 * @param offset An optional number of bytes to skip from the start of the file.
 * Default is 0 (read from beginning of file).
 * @param length The maximum number of bytes to read after the [offset].
 * If fewer bytes are available before the end of the file, only the available bytes are returned.
 * By default, will read the entirety of contents after [offset].
 */
data class IONFILEReadInChunksOptions(
    val encoding: IONFILEEncoding,
    val chunkSize: Int,
    val offset: Int = 0,
    val length: Int = LENGTH_DEFAULT_VALUE
)