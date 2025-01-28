package io.ionic.libs.ionfilesystemlib.model

/**
 * Parameters for saving data to a file
 *
 * @param data string contents to save in the file
 * @param encoding how the data is encoded as;
 *  for example if it is base64 then it will be decoded before writing; see [IONFLSTEncoding]
 * @param mode the mode for saving to a file; see [IONFLSTSaveMode]
 * @param createFileRecursive true to also create the file and missing directories in case they do not exist;
 *  false to create only missing file, but not create missing parent directories
 *  null if not meant to create the file, returns error if file does not exist
 */
data class IONFLSTSaveOptions(
    val data: String,
    val encoding: IONFLSTEncoding,
    val mode: IONFLSTSaveMode,
    val createFileRecursive: Boolean?
)

/**
 * Mode for saving the file
 */
enum class IONFLSTSaveMode {
    /**
     * Write mode - Will write to the beginning of the file, overriding any contents that were previously written
     */
    WRITE,

    /**
     * Append mode - Will write to the end of the file, leaving contents that were previously written intact
     */
    APPEND
}