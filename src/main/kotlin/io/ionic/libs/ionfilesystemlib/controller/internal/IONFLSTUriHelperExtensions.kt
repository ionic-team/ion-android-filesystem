package io.ionic.libs.ionfilesystemlib.controller.internal

import io.ionic.libs.ionfilesystemlib.controller.IONFLSTFUriHelper
import io.ionic.libs.ionfilesystemlib.model.IONFLSTExceptions
import io.ionic.libs.ionfilesystemlib.model.IONFLSTUri
import io.ionic.libs.ionfilesystemlib.model.LocalUriType

internal suspend inline fun <reified T> IONFLSTFUriHelper.useUriIfResolvedAsLocalDirectory(
    uri: IONFLSTUri,
    onResolved: (IONFLSTUri.Resolved.Local) -> Result<T>
): Result<T> = useUriIfResolvedAsLocal(uri) { resolvedUri ->
    if (resolvedUri.type != LocalUriType.FILE) {
        onResolved(resolvedUri)
    } else {
        Result.failure(IONFLSTExceptions.NotSupportedForFiles())
    }
}

internal suspend inline fun <reified T> IONFLSTFUriHelper.useUriIfResolvedAsLocalFile(
    uri: IONFLSTUri,
    onResolved: (IONFLSTUri.Resolved.Local) -> Result<T>
): Result<T> = useUriIfResolvedAsLocal(uri) { resolvedUri ->
    if (resolvedUri.type != LocalUriType.DIRECTORY) {
        onResolved(resolvedUri)
    } else {
        Result.failure(IONFLSTExceptions.NotSupportedForDirectory())
    }
}

internal suspend inline fun <reified T> IONFLSTFUriHelper.useUriIfResolvedAsLocal(
    uri: IONFLSTUri,
    onResolved: (IONFLSTUri.Resolved.Local) -> Result<T>
): Result<T> = useUriIfResolved(uri) { resolvedUri ->
    if (resolvedUri is IONFLSTUri.Resolved.Local) {
        onResolved(resolvedUri)
    } else {
        Result.failure(IONFLSTExceptions.NotSupportedForContentScheme())
    }
}

internal suspend inline fun <reified T> IONFLSTFUriHelper.useUriIfResolvedAsNonDirectory(
    uri: IONFLSTUri,
    onResolved: (IONFLSTUri.Resolved) -> Result<T>
): Result<T> = useUriIfResolved(uri) { resolvedUri ->
    if (resolvedUri !is IONFLSTUri.Resolved.Local || resolvedUri.type != LocalUriType.DIRECTORY) {
        onResolved(resolvedUri)
    } else {
        Result.failure(IONFLSTExceptions.NotSupportedForDirectory())
    }
}

internal suspend inline fun <reified T> IONFLSTFUriHelper.useUriIfResolved(
    uri: IONFLSTUri,
    onResolved: (IONFLSTUri.Resolved) -> Result<T>
): Result<T> {
    val resolvedUri: IONFLSTUri.Resolved = if (uri is IONFLSTUri.Resolved) {
        uri
    } else {
        resolveUri(uri as IONFLSTUri.Unresolved).let {
            it.getOrNull()
                ?: return@useUriIfResolved Result.failure(
                    it.exceptionOrNull() ?: NullPointerException()
                )
        }
    }
    return onResolved(resolvedUri)
}