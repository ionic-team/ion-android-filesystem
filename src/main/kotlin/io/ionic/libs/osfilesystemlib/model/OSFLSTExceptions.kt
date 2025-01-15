package io.ionic.libs.osfilesystemlib.model

sealed class OSFLSTExceptions(message: String, cause: Throwable?) : Exception(message, cause) {
    sealed class CreateFailed(message: String) : OSFLSTExceptions(message, null) {
        class Unknown : CreateFailed("Failed to create file/directory due to unknown reason")
        class AlreadyExists : CreateFailed("The file/directory already exists")
        class NoParentDirectory :
            CreateFailed("Received recursive=false but missing parent directories")
    }

    sealed class DeleteFailed(message: String) : OSFLSTExceptions(message, null) {
        class Unknown : DeleteFailed("Failed to delete file/directory due to unknown reason")
        class DoesNotExist : DeleteFailed("The file/directory does not exist")
        class CannotDeleteChildren :
            DeleteFailed("Received recursive=false but directory is not-empty")
    }
}