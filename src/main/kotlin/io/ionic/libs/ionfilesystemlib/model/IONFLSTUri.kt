package io.ionic.libs.ionfilesystemlib.model

import android.net.Uri
import java.io.File

sealed class IONFLSTUri {
    /**
     * Uri that is unresolved, and it is unknown what kind of uri it can be.
     *
     * @param parentFolder type of parent folder; indicates that the uri is for a  local file inside that folde.
     *  For folder types refer to [IONFLSTFolderType].
     *  If null, will assume that the entire uri to resolve appears inside uriPath
     * @param uriPath the actual full uri path (or partial, to be combined with parentFolder if not null)
     */
    data class Unresolved(
        val parentFolder: IONFLSTFolderType?,
        val uriPath: String
    ) : IONFLSTUri()

    /**
     * Uri that was resolved and is now of a known type.
     *
     * Note that just because it is resolved, does not mean that the underlying file exists.
     */
    sealed class Resolved(open val uri: Uri) : IONFLSTUri() {
        /**
         * Uri of content scheme (e.g. content://media...)
         */
        data class Content(override val uri: Uri) : Resolved(uri)

        /**
         * Complete file path pointing to local file
         *
         * With the Uri being of type file://
         */
        data class Local(
            val fullPath: String,
            override val uri: Uri,
            val type: LocalUriType
        ) : Resolved(uri) {

            constructor(fullPath: String) : this(
                fullPath,
                Uri.fromFile(File(fullPath)),
                LocalUriType.UNKNOWN
            )
        }
    }
}

enum class LocalUriType {
    FILE,
    DIRECTORY,
    UNKNOWN // e.g. the file at specified path does not exist
}