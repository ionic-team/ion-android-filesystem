package io.ionic.libs.ionfilesystemlib.model

import android.net.Uri

/**
 * Result containing relevant metadata about a file (or directory)
 *
 * @param fullPath the full path to a file
 * @param name the file name
 * @param uri the complete uri to the file
 * @param size the size in bytes
 * @param type type of file (directory or an actual file with a certain mimeType); see [IONFLSTFileType]
 * @param createdTimestamp local timestamp for file creation;
 *  for some files may be available only on Android 26 and above; 0 returned below Android 26
 * @param lastModifiedTimestamp local timestamp for last time file was modified
 */
data class IONFLSTMetadataResult(
    val fullPath: String,
    val name: String,
    val uri: Uri,
    val size: Long,
    val type: IONFLSTFileType,
    val createdTimestamp: Long,
    val lastModifiedTimestamp: Long
)

sealed class IONFLSTFileType {
    data object Directory : IONFLSTFileType()
    data class File(val mimeType: String) : IONFLSTFileType()
}