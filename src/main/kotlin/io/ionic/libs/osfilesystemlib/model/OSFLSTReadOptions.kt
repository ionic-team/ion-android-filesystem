package io.ionic.libs.osfilesystemlib.model

/**
 * Parameters for creating a file or directory
 *
 * @param fullPath the full path of the file / directory to remove
 * @param encoding how the file data to return should be encoded; see [OSFLSTEncoding]
 */
data class OSFLSTReadOptions(
    val fullPath: String,
    val encoding: OSFLSTEncoding
)