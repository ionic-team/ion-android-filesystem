package io.ionic.libs.ionfilesystemlib.model

/**
 * Parameters for reading a file by chunks
 *
 * @param encoding how the file data to return should be encoded; see [IONFILEEncoding]
 * @param chunkSize the desired amount of chunks to store in memory at a time.
 *  The actual chunkSize that the library uses may be different than what was supplied:
 *  1. If the file is smaller than chunkSize, it will only allocate enough bytes to read the file.
 *  2. [kotlin.io.DEFAULT_BUFFER_SIZE] is used if chunkSize supplied is smaller than it.
 *      This is to make sure the read operations do not take too long.
 *  3. When encoding is [IONFILEEncoding.Base64], chunkSize is set to a multiple of 3.
 *      This is because text that has a size that is a multiple of 3 does not get
 *      This means multiple chunks can be concatenated and decoded together without losing data.
 *  There is no check on a maximum value for chunkSize, meaning that if you provide a very large value,
 *      an OutOfMemoryError may be thrown. Avoid using chunkSize larger than a few MB.
 */
data class IONFILEReadByChunksOptions(
    val encoding: IONFILEEncoding,
    val chunkSize: Int,
)