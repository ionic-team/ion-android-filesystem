package io.ionic.libs.ionfilesystemlib.model

/**
 * Parameters for reading a file all at once
 *
 * @param encoding how the file data to return should be encoded; see [IONFILEEncoding]
 */
data class IONFILEReadOptions(
    val encoding: IONFILEEncoding
)