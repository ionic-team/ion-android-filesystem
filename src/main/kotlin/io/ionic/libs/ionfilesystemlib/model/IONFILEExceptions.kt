package io.ionic.libs.ionfilesystemlib.model

sealed class IONFILEExceptions(message: String, cause: Throwable? = null) :
    Exception(message, cause) {

    class UnresolvableUri(val uri: String) :
        IONFILEExceptions("Unable to resolve the provided uri=$uri")

    class DoesNotExist(val path: String, override val cause: Throwable? = null) :
        IONFILEExceptions("The file/directory at $path does not exist")

    class UnknownError(override val cause: Throwable? = null) :
        IONFILEExceptions("An unknown error occurred.")

    class NotSupportedForContentScheme :
        IONFILEExceptions("The requested operation is not supported on a content:// uri")

    class NotSupportedForDirectory :
        IONFILEExceptions("The request operation is not supported on a directory")

    class NotSupportedForFiles :
        IONFILEExceptions("The request operation is not supported on files, only directories")

    sealed class CreateFailed(message: String) : IONFILEExceptions(message) {
        class AlreadyExists(val path: String) : CreateFailed("The file/directory at $path already exists")
        class NoParentDirectory :
            CreateFailed("Missing parent directories - either recursive=false was received or parent directory creation failed")
    }

    sealed class DeleteFailed(message: String) : IONFILEExceptions(message) {
        class CannotDeleteChildren :
            DeleteFailed("Received recursive=false but directory is not-empty")
    }

    sealed class CopyRenameFailed(message: String) : IONFILEExceptions(message) {
        class MixingFilesAndDirectories :
            CopyRenameFailed("Copy and rename is only allowed either between files or between directories")

        class LocalToContent :
            CopyRenameFailed("Copy is not allowed from local file to content:// file")

        class SourceAndDestinationContent :
            CopyRenameFailed("Copy is not allowed from content:// to content://")

        class DestinationDirectoryExists(val path: String) :
            CopyRenameFailed("Cannot copy/rename to an existing directory ($path)")

        class NoParentDirectory() :
            CopyRenameFailed("Unable to copy/rename because the destination's parent directory does not exist")
    }
}