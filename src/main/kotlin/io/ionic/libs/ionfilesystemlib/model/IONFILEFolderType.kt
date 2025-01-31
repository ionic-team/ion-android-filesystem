package io.ionic.libs.ionfilesystemlib.model

import androidx.annotation.VisibleForTesting

/**
 * Alias for possible folders in which to search for files
 *
 * These folders are specific to local files, cannot be used with content:// scheme
 */
enum class IONFILEFolderType(
    val requiresPermission: Boolean = false,
    internal val alternateNames: List<String> = emptyList()
) {
    INTERNAL_CACHE(alternateNames = listOf("CACHE", "TEMPORARY")),
    INTERNAL_FILES(alternateNames = listOf("DATA", "LIBRARY", "FILES", "LIBRARY_NO_CLOUD")),
    EXTERNAL_CACHE(alternateNames = listOf("CACHE_EXTERNAL")),
    EXTERNAL_FILES(alternateNames = listOf("EXTERNAL", "FILES_EXTERNAL")),
    EXTERNAL_STORAGE(requiresPermission = true, alternateNames = listOf("sdcard")),
    DOCUMENTS(requiresPermission = true);

    companion object {
        fun fromStringAlias(alias: String?): IONFILEFolderType? = if (alias.isNullOrBlank()) {
            null
        } else {
            IONFILEFolderType.entries.firstOrNull {
                aliasMatches(alias, it.name) || it.alternateNames.any { alternateName ->
                    aliasMatches(alias, alternateName)
                }
            }
        }

        private fun aliasMatches(alias: String, compare: String): Boolean =
            coerceFolderAlias(alias) == coerceFolderAlias(compare)

        @VisibleForTesting
        internal fun coerceFolderAlias(alias: String): String =
            alias.filter { it.isLetter() }.lowercase()
    }
}
