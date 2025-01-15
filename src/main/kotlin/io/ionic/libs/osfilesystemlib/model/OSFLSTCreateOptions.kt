package io.ionic.libs.osfilesystemlib.model

/**
 * Parameters for creating a file or directory
 *
 * @param fullPath the full path to create the file / directory
 * @param recursive true if meant to create any missing parent directories, false otherwise
 * @param exclusive true if does not allow touching the file/directory if it exists (returns error), false otherwise
 */
data class OSFLSTCreateOptions(
    val fullPath: String,
    val recursive: Boolean,
    val exclusive: Boolean
)
