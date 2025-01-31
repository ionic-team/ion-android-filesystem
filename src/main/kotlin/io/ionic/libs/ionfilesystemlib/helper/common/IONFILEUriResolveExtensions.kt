package io.ionic.libs.ionfilesystemlib.helper.common

import io.ionic.libs.ionfilesystemlib.helper.IONFILEUriHelper
import io.ionic.libs.ionfilesystemlib.model.IONFILEExceptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEUri
import io.ionic.libs.ionfilesystemlib.model.LocalUriType

internal suspend inline fun <reified T> IONFILEUriHelper.useUriIfResolvedAsLocalDirectory(
    uri: IONFILEUri,
    onResolved: (IONFILEUri.Resolved.Local) -> Result<T>
): Result<T> = useUriIfResolvedAsLocal(uri) { resolvedUri ->
    if (resolvedUri.type != LocalUriType.FILE) {
        onResolved(resolvedUri)
    } else {
        Result.failure(IONFILEExceptions.NotSupportedForFiles())
    }
}

internal suspend inline fun <reified T> IONFILEUriHelper.useUriIfResolvedAsLocalFile(
    uri: IONFILEUri,
    onResolved: (IONFILEUri.Resolved.Local) -> Result<T>
): Result<T> = useUriIfResolvedAsLocal(uri) { resolvedUri ->
    if (resolvedUri.type != LocalUriType.DIRECTORY) {
        onResolved(resolvedUri)
    } else {
        Result.failure(IONFILEExceptions.NotSupportedForDirectory())
    }
}

internal suspend inline fun <reified T> IONFILEUriHelper.useUriIfResolvedAsLocal(
    uri: IONFILEUri,
    onResolved: (IONFILEUri.Resolved.Local) -> Result<T>
): Result<T> = useUriIfResolved(uri) { resolvedUri ->
    if (resolvedUri is IONFILEUri.Resolved.Local) {
        onResolved(resolvedUri)
    } else {
        Result.failure(IONFILEExceptions.NotSupportedForContentScheme())
    }
}

internal suspend inline fun <reified T> IONFILEUriHelper.useUriIfResolvedAsNonDirectory(
    uri: IONFILEUri,
    onResolved: (IONFILEUri.Resolved) -> Result<T>
): Result<T> = useUriIfResolved(uri) { resolvedUri ->
    if (resolvedUri !is IONFILEUri.Resolved.Local || resolvedUri.type != LocalUriType.DIRECTORY) {
        onResolved(resolvedUri)
    } else {
        Result.failure(IONFILEExceptions.NotSupportedForDirectory())
    }
}

internal suspend inline fun <reified T> IONFILEUriHelper.useUriIfResolved(
    uri: IONFILEUri,
    onResolved: (IONFILEUri.Resolved) -> Result<T>
): Result<T> {
    val resolvedUri: IONFILEUri.Resolved = if (uri is IONFILEUri.Resolved) {
        uri
    } else {
        resolveUri(uri as IONFILEUri.Unresolved).let {
            it.getOrNull()
                ?: return@useUriIfResolved Result.failure(
                    it.exceptionOrNull() ?: NullPointerException()
                )
        }
    }
    return onResolved(resolvedUri)
}