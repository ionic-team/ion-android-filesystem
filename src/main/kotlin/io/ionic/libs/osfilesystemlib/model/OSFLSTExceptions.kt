package io.ionic.libs.osfilesystemlib.model

sealed class OSFLSTExceptions(message: String, cause: Throwable?) : Exception(message, cause) {
    sealed class CreateFailed(
        message: String
    ) : OSFLSTExceptions(message, null) {
        class Unknown : CreateFailed("Failed to create file/directory due to unknown reason")
        class AlreadyExists : CreateFailed("The file/directory already exists")
        class NoParentDirectory :
            CreateFailed("Received recursive=false but missing parent directories")
    }


}