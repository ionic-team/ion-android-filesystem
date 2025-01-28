package io.ionic.libs.ionfilesystemlib.model

/**
 * Parameters for creating a file or directory
 *
 * @param encoding how the file data to return should be encoded; see [IONFLSTEncoding]
 */
data class IONFLSTReadOptions(
    val encoding: IONFLSTEncoding
)