package io.ionic.libs.ionfilesystemlib

import android.content.Context
import android.net.Uri
import io.ionic.libs.ionfilesystemlib.helper.IONFILEContentHelper
import io.ionic.libs.ionfilesystemlib.helper.IONFILEDirectoriesHelper
import io.ionic.libs.ionfilesystemlib.helper.IONFILELocalFilesHelper
import io.ionic.libs.ionfilesystemlib.helper.IONFILEUriHelper
import io.ionic.libs.ionfilesystemlib.helper.common.useUriIfResolved
import io.ionic.libs.ionfilesystemlib.helper.common.useUriIfResolvedAsLocal
import io.ionic.libs.ionfilesystemlib.helper.common.useUriIfResolvedAsLocalDirectory
import io.ionic.libs.ionfilesystemlib.helper.common.useUriIfResolvedAsLocalFile
import io.ionic.libs.ionfilesystemlib.helper.common.useUriIfResolvedAsNonDirectory
import io.ionic.libs.ionfilesystemlib.model.IONFILECreateOptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEDeleteOptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEExceptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEMetadataResult
import io.ionic.libs.ionfilesystemlib.model.IONFILEReadByChunksOptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEReadOptions
import io.ionic.libs.ionfilesystemlib.model.IONFILESaveOptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEUri
import io.ionic.libs.ionfilesystemlib.model.LocalUriType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * Entry point in IONFilesystemLib-Android
 *
 * Contains all the methods for handling files.
 *
 * Usage: Just initialize the controller passing an Android [Context]
 * `val controller = IONFILEController(context)`
 */
class IONFILEController internal constructor(
    private val uriHelper: IONFILEUriHelper,
    private val localFilesHelper: IONFILELocalFilesHelper,
    private val directoriesHelper: IONFILEDirectoriesHelper,
    private val contentResolverHelper: IONFILEContentHelper
) {

    constructor(context: Context) : this(
        uriHelper = IONFILEUriHelper(context),
        localFilesHelper = IONFILELocalFilesHelper(),
        directoriesHelper = IONFILEDirectoriesHelper(),
        contentResolverHelper = IONFILEContentHelper(context.contentResolver)
    )

    /**
     * Resolve a uri for a file (or directory) and return it
     *
     * @param uri the uri to resolve; see [IONFILEUri.Unresolved]
     * @return success with [IONFILEUri.Resolved] uri, or error otherwise
     */
    suspend fun getFileUri(uri: IONFILEUri.Unresolved): Result<IONFILEUri.Resolved> =
        uriHelper.resolveUri(uri)

    /**
     * Create a file
     *
     * This method will fail if a "content://" type URI is passed
     *
     * @param uri a [IONFILEUri] object; will resolve internally to determine the actual path
     * @param options options to configure file creation; see [IONFILECreateOptions]
     * @return success with the android "file://" [Uri] that was created, or error otherwise
     */
    suspend fun createFile(uri: IONFILEUri, options: IONFILECreateOptions): Result<Uri> =
        uriHelper.useUriIfResolvedAsLocalFile(uri) { resolvedUri ->
            localFilesHelper.createFile(resolvedUri.fullPath, options).map { resolvedUri.uri }
        }

    /**
     * Create a directory
     *
     * This method will fail if a "content://" type URI is passed
     *
     * @param uri a [IONFILEUri] object; will resolve internally to determine the actual path
     * @param options options to configure directory creation; see [IONFILECreateOptions]
     * @return success with the android "file://" [Uri] that was created, or error otherwise
     */
    suspend fun createDirectory(uri: IONFILEUri, options: IONFILECreateOptions): Result<Uri> =
        uriHelper.useUriIfResolvedAsLocalDirectory(uri) { resolvedUri ->
            directoriesHelper.createDirectory(resolvedUri.fullPath, options).map { resolvedUri.uri }
        }

    /**
     * Read the contents of a file.
     *
     * Not recommended for large files (higher than a few MB) - use [readFileByChunks] for that
     *
     * This method will fail if a directory path is passed.
     *
     * @param uri a [IONFILEUri] object; will resolve internally to determine the actual file
     * @param options the options for customizing file reading; see [IONFILEReadOptions]
     */
    suspend fun readFile(
        uri: IONFILEUri,
        options: IONFILEReadOptions
    ): Result<String> = uriHelper.useUriIfResolvedAsNonDirectory(uri) { resolvedUri ->
        if (resolvedUri is IONFILEUri.Resolved.Local) {
            localFilesHelper.readFile(resolvedUri.fullPath, options)
        } else {
            contentResolverHelper.readFile(resolvedUri.uri, options)
        }
    }

    /**
     * Read the contents of a file, in chunks.
     *
     * Useful when reading large files that may not fit entirely in memory
     *
     * This method will fail if a directory path is passed.
     *
     * Example usage:
     *
     * ```kotlin
     * controller.readFileByChunks(path, options)
     *     .onEach {
     *         // handle receiving chunks here
     *     }
     *     .catch {
     *         // handle errors here
     *     }
     *     .onCompletion {
     *         // handle file finished read successfully here
     *     }
     *     .launchIn(coroutineScope)
     * ```
     *
     * @param uri a [IONFILEUri] object; will resolve internally to determine the actual file
     * @param options the options for customizing file reading; see [IONFILEReadByChunksOptions]
     */
    @ExperimentalCoroutinesApi
    fun readFileByChunks(
        uri: IONFILEUri,
        options: IONFILEReadByChunksOptions
    ): Flow<String> = flow {
        val resolveResult = uriHelper.useUriIfResolvedAsNonDirectory(uri) { Result.success(it) }
        resolveResult.fold(
            onSuccess = { resolvedUri ->
                val readByChunksFlow = when (resolvedUri) {
                    is IONFILEUri.Resolved.Local ->
                        localFilesHelper.readFileByChunks(resolvedUri.fullPath, options)

                    else -> contentResolverHelper.readFileByChunks(resolvedUri.uri, options)
                }
                emitAll(readByChunksFlow)
            },
            onFailure = { throw it }
        )
    }

    /**
     * Get metadata / information on a file or directory
     *
     * @param uri a [IONFILEUri] object; will resolve internally to determine the actual file
     * @return success with a [IONFILEMetadataResult] object containing information on the file, or error otherwise
     */
    suspend fun getMetadata(uri: IONFILEUri): Result<IONFILEMetadataResult> =
        uriHelper.useUriIfResolved(uri) { resolvedUri ->
            if (resolvedUri is IONFILEUri.Resolved.Local) {
                localFilesHelper.getFileMetadata(resolvedUri.fullPath)
            } else {
                contentResolverHelper.getFileMetadata(resolvedUri.uri)
            }
        }

    /**
     * Save contents to a file
     *
     * This method will fail if a "content://" type URI is passed
     *
     * @param uri a [IONFILEUri] object; will resolve internally to determine the actual file
     * @param options data and options for configuring file saving; see [IONFILESaveOptions]
     * @return success with the android [Uri] that was saved to, or error otherwise
     */
    suspend fun saveFile(
        uri: IONFILEUri,
        options: IONFILESaveOptions
    ): Result<Uri> = uriHelper.useUriIfResolvedAsLocalFile(uri) { resolvedLocalFile ->
        localFilesHelper.saveFile(resolvedLocalFile.fullPath, options)
            .map { resolvedLocalFile.uri }
    }

    /**
     * List the contents of a directory - all children including files and directories
     *
     * This method is not recursive, meaning that if the directories has sub-directories, the children of these will not be listed
     *
     * This method will fail if a file is passed instead of a directory
     *
     * @param uri a [IONFILEUri] object; will resolve internally to determine the actual directory
     * @return success with list of [IONFILEMetadataResult] objects containing information about directory's children, or error otherwise
     */
    suspend fun listDirectory(uri: IONFILEUri): Result<List<IONFILEMetadataResult>> =
        uriHelper.useUriIfResolvedAsLocalDirectory(uri) { resolvedUri ->
            directoriesHelper.listDirectory(resolvedUri.fullPath)
        }

    /**
     * Delete a file or directory
     *
     * @param uri a [IONFILEUri] object; will resolve internally to determine the actual path
     * @param options the options to configure deletion; only applies for directories; see [IONFILEDeleteOptions]
     * @return success if file / directory was deleted, error otherwise
     */
    suspend fun delete(uri: IONFILEUri, options: IONFILEDeleteOptions): Result<Unit> =
        uriHelper.useUriIfResolved(uri) { resolvedUri ->
            when {
                resolvedUri is IONFILEUri.Resolved.Local && resolvedUri.type == LocalUriType.DIRECTORY ->
                    directoriesHelper.deleteDirectory(resolvedUri.fullPath, options)

                resolvedUri is IONFILEUri.Resolved.Local ->
                    localFilesHelper.deleteFile(resolvedUri.fullPath)

                else -> contentResolverHelper.deleteFile(resolvedUri.uri)
            }
        }

    /**
     * Copy a file or directory (the latter is copied recursively)
     *
     * @param source a [IONFILEUri] object for the source file/directory to copy from;
     *  will resolve internally to determine the actual path
     * @param destination a [IONFILEUri] object for the destination file/directory to copy to;
     *  will resolve internally to determine the actual path
     *
     * @return success with the android [Uri] that was copied to, or error otherwise
     */
    suspend fun copy(source: IONFILEUri, destination: IONFILEUri): Result<Uri> =
        uriHelper.useUriIfResolved(source) { resolvedSourceUri ->
            uriHelper.useUriIfResolved(destination) { resolvedDestinationUri ->
                when {
                    resolvedSourceUri is IONFILEUri.Resolved.Local && resolvedDestinationUri is IONFILEUri.Resolved.Content ->
                        return Result.failure(IONFILEExceptions.CopyRenameFailed.LocalToContent())

                    resolvedSourceUri is IONFILEUri.Resolved.Content && resolvedDestinationUri is IONFILEUri.Resolved.Content ->
                        return Result.failure(IONFILEExceptions.CopyRenameFailed.SourceAndDestinationContent())

                    resolvedSourceUri is IONFILEUri.Resolved.Local -> {
                        val sourcePath = resolvedSourceUri.fullPath
                        val destinationPath =
                            (resolvedDestinationUri as IONFILEUri.Resolved.Local).fullPath
                        if (resolvedSourceUri.type == LocalUriType.DIRECTORY) {
                            directoriesHelper.copyDirectory(sourcePath, destinationPath)
                        } else {
                            localFilesHelper.copyFile(sourcePath, destinationPath)
                        }
                    }

                    else -> {
                        val sourceUri = resolvedSourceUri.uri
                        val destinationPath =
                            (resolvedDestinationUri as IONFILEUri.Resolved.Local).fullPath
                        contentResolverHelper.copyFile(sourceUri, destinationPath)
                    }
                }
                Result.success(resolvedDestinationUri.uri)
            }
        }

    /**
     * Move or rename a file or directory (the latter is moved recursively)
     *
     * @param source a [IONFILEUri] object for the source file/directory to move;
     *  will resolve internally to determine the actual path
     * @param destination a [IONFILEUri] object for the destination file/directory to move to;
     *  will resolve internally to determine the actual path
     *
     * @return success with the android [Uri] that was copied to, or error otherwise
     */
    suspend fun move(source: IONFILEUri, destination: IONFILEUri): Result<Uri> =
        uriHelper.useUriIfResolvedAsLocal(source) { resolvedSource ->
            uriHelper.useUriIfResolvedAsLocal(destination) { resolvedDestination ->
                val sourcePath = resolvedSource.fullPath
                val destinationPath = resolvedDestination.fullPath
                if (resolvedSource.type == LocalUriType.DIRECTORY) {
                    directoriesHelper.moveDirectory(sourcePath, destinationPath)
                } else {
                    localFilesHelper.renameFile(sourcePath, destinationPath)
                }
                Result.success(resolvedDestination.uri)
            }
        }
}