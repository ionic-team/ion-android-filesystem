package io.ionic.libs.osfilesystemlib.model

/**
 * Result containing relevant metadata about a file (or directory)
 *
 * @param fullPath the full path to a file
 * @param name the file name
 * @param size the size in bytes
 * @param type type of file (directory or an actual file with a certain mimeType); see [OSFLSTFileType]
 * @param createdTimestamp local timestamp for file creation;
 *  available only on Android 26 and above; 0 is returned below Android 26
 * @param lastModifiedTimestamp local timestamp for last time file was modified
 */
data class OSFLSTMetadataResult(
    val fullPath: String,
    val name: String,
    val size: Long,
    val type: OSFLSTFileType,
    val createdTimestamp: Long,
    val lastModifiedTimestamp: Long
)

sealed class OSFLSTFileType {
    data object Directory : OSFLSTFileType()
    data class File(val mimeType: String) : OSFLSTFileType()
}