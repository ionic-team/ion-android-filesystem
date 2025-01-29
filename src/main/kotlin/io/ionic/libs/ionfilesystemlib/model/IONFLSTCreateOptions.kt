package io.ionic.libs.ionfilesystemlib.model

/**
 * Parameters for creating a file or directory
 *
 * @param recursive true if meant to create any missing parent directories, false otherwise
 */
data class IONFLSTCreateOptions(
    val recursive: Boolean
)
