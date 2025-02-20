package io.ionic.libs.ionfilesystemlib.model

import android.net.Uri
import java.io.File

sealed class IONFILEUri {
    /**
     * Uri that is unresolved, and it is unknown what kind of uri it can be.
     *
     * @param parentFolder type of parent folder; indicates that the uri is for a  local file inside that folde.
     *  For folder types refer to [IONFILEFolderType].
     *  If null, will assume that the entire uri to resolve appears inside uriPath
     * @param uriPath the actual full uri path (or partial, to be combined with parentFolder if not null)
     */
    data class Unresolved(
        val parentFolder: IONFILEFolderType?,
        val uriPath: String
    ) : IONFILEUri()

    /**
     * Uri that was resolved and is now of a known type.
     *
     * Note that just because it is resolved, does not mean that the underlying file exists.
     *
     * @param uri the resolved android uri
     * @param inExternalStorage true if the file is external storage, false otherwise
     *  (may requires permissions relevant for Android 10 and below)
     */
    sealed class Resolved(open val uri: Uri, open val inExternalStorage: Boolean) : IONFILEUri() {
        /**
         * Uri of content scheme (e.g. content://media...)
         */
        data class Content(override val uri: Uri) : Resolved(uri, false)

        /**
         * Complete file path pointing to local file
         *
         * With the Uri being of type file://
         */
        data class Local(
            val fullPath: String,
            override val uri: Uri,
            val type: LocalUriType,
            override val inExternalStorage: Boolean
        ) : Resolved(uri, inExternalStorage) {

            constructor(fullPath: String) : this(
                fullPath,
                Uri.fromFile(File(fullPath)),
                LocalUriType.UNKNOWN,
                inExternalStorage = true
            )
        }
    }
}

enum class LocalUriType {
    FILE,
    DIRECTORY,
    UNKNOWN // e.g. the file at specified path does not exist
}