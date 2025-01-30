package io.ionic.libs.ionfilesystemlib.model

/**
 * Parameters for reading a file all at once
 *
 * @param encoding how the file data to return should be encoded; see [IONFLSTEncoding]
 */
data class IONFLSTReadOptions(
    val encoding: IONFLSTEncoding
)