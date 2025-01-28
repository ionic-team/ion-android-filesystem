package io.ionic.libs.ionfilesystemlib.model

sealed class IONFLSTExceptions(message: String) : Exception(message) {
    class UnresolvableUri(uri: String) : IONFLSTExceptions("Unable to resolve the provided uri=$uri")

    sealed class CreateFailed(message: String) : IONFLSTExceptions(message) {
        class Unknown : CreateFailed("Failed to create file/directory due to unknown reason")
        class AlreadyExists : CreateFailed("The file/directory already exists")
        class NoParentDirectory :
            CreateFailed("Received recursive=false but missing parent directories")
    }

    sealed class DeleteFailed(message: String) : IONFLSTExceptions(message) {
        class Unknown : DeleteFailed("Failed to delete file/directory due to unknown reason")
        class CannotDeleteChildren :
            DeleteFailed("Received recursive=false but directory is not-empty")
    }

    class DoesNotExist : IONFLSTExceptions("The file/directory does not exist")

    class UnknownError : IONFLSTExceptions("An unknown error occurred.")

    class NotAllowed : IONFLSTExceptions("Unable to execute the requested operation on the file")
}