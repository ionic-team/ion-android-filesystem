package io.ionic.libs.osfilesystemlib.model

/**
 * Parameters for creating a file or directory
 *
 * @param recursive true if meant to remove any sub-directories or files, false otherwise.
 *  This only applies to directories; will fail if recursive=false and directory is not empty
 */
data class OSFLSTDeleteOptions(
    val recursive: Boolean
)
