package io.ionic.libs.osfilesystemlib.model

import android.net.Uri

sealed class OSFLSTUri {
    /**
     * Uri that is unresolved, and it is unknown what kind of uri it can be.
     *
     * Can have a parentFolder to indicate that the uri is for a  local file inside that folder
     * For folder types refer to [OSFLSTFolderType].
     * If null, will assume that the entire uri to resolve appears inside uriPath
     */
    data class Unresolved(
        val parentFolder: OSFLSTFolderType?,
        val uriPath: String
    ) : OSFLSTUri()

    /**
     * Uri that was resolved and is now of a known type
     */
    sealed class Resolved : OSFLSTUri() {
        /**
         * Uri of content scheme (e.g. content://media...)
         */
        data class Content(val uri: Uri) : Resolved()

        /**
         * Complete file path pointing to local file
         *
         * With the Uri being of type file://
         */
        data class Local(val fullPath: String, val uri: Uri, val type: LocalUriType) : Resolved()
    }
}

enum class LocalUriType {
    FILE,
    DIRECTORY,
    UNKNOWN // e.g. the file at specified path does not exist
}