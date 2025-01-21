package io.ionic.libs.osfilesystemlib.model

/**
 * Parameters for creating a file or directory
 *
 * @param encoding how the file data to return should be encoded; see [OSFLSTEncoding]
 */
data class OSFLSTReadOptions(
    val encoding: OSFLSTEncoding
)