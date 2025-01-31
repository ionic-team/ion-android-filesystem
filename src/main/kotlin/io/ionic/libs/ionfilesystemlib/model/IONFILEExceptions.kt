package io.ionic.libs.ionfilesystemlib.model

sealed class IONFILEExceptions(message: String) : Exception(message) {
    class UnresolvableUri(uri: String) :
        IONFILEExceptions("Unable to resolve the provided uri=$uri")

    sealed class CreateFailed(message: String) : IONFILEExceptions(message) {
        class Unknown : CreateFailed("Failed to create file/directory due to unknown reason")
        class AlreadyExists : CreateFailed("The file/directory already exists")
        class NoParentDirectory :
            CreateFailed("Received recursive=false but missing parent directories")
    }

    sealed class DeleteFailed(message: String) : IONFILEExceptions(message) {
        class Unknown : DeleteFailed("Failed to delete file/directory due to unknown reason")
        class CannotDeleteChildren :
            DeleteFailed("Received recursive=false but directory is not-empty")
    }

    class DoesNotExist : IONFILEExceptions("The file/directory does not exist")

    class UnknownError : IONFILEExceptions("An unknown error occurred.")

    class NotSupportedForContentScheme :
        IONFILEExceptions("The requested operation is not supported on a content:// uri")

    class NotSupportedForDirectory :
        IONFILEExceptions("The request operation is not supported on a directory")

    class NotSupportedForFiles :
        IONFILEExceptions("The request operation is not supported on files, only directories")

    sealed class CopyRenameFailed(message: String) : IONFILEExceptions(message) {
        class Unknown :
            CopyRenameFailed("Failed to copy/rename the file/directory due to unknown reason")

        class MixingFilesAndDirectories :
            CopyRenameFailed("Copy and rename is only allowed either between files or between directories")

        class LocalToContent :
            CopyRenameFailed("Copy is not allowed from local file to content:// file")

        class SourceAndDestinationContent :
            CopyRenameFailed("Copy is not allowed from content:// to content://")

        class DestinationDirectoryExists :
            CopyRenameFailed("Cannot copy/rename to an existing directory")

        class NoParentDirectory :
            CopyRenameFailed("Unable to copy/rename because the destination's parent directory does not exist")
    }
}