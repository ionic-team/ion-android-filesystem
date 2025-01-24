package io.ionic.libs.osfilesystemlib.model

sealed class OSFLSTExceptions(message: String) : Exception(message) {
    class UnresolvableUri(uri: String) : OSFLSTExceptions("Unable to resolve the provided uri=$uri")

    sealed class CreateFailed(message: String) : OSFLSTExceptions(message) {
        class Unknown : CreateFailed("Failed to create file/directory due to unknown reason")
        class AlreadyExists : CreateFailed("The file/directory already exists")
        class NoParentDirectory :
            CreateFailed("Received recursive=false but missing parent directories")
    }

    sealed class DeleteFailed(message: String) : OSFLSTExceptions(message) {
        class Unknown : DeleteFailed("Failed to delete file/directory due to unknown reason")
        class CannotDeleteChildren :
            DeleteFailed("Received recursive=false but directory is not-empty")
    }

    class DoesNotExist : OSFLSTExceptions("The file/directory does not exist")

    class UnknownError : OSFLSTExceptions("An unknown error occurred.")

    class NotAllowed : OSFLSTExceptions("Unable to execute the requested operation on the file")
}