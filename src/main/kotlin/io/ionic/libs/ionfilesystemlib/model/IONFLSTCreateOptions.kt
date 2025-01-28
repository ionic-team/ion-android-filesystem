package io.ionic.libs.ionfilesystemlib.model

/**
 * Parameters for creating a file or directory
 *
 * @param recursive true if meant to create any missing parent directories, false otherwise
 * @param exclusive true if does not allow touching the file/directory if it exists (returns error), false otherwise
 */
data class IONFLSTCreateOptions(
    val recursive: Boolean,
    val exclusive: Boolean
)
