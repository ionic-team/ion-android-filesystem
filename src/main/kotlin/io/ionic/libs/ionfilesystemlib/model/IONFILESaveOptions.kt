package io.ionic.libs.ionfilesystemlib.model

/**
 * Parameters for saving data to a file
 *
 * @param data string contents to save in the file
 * @param encoding how the data is encoded as;
 *  for example if it is base64 then it will be decoded before writing; see [IONFILEEncoding]
 * @param mode the mode for saving to a file; see [IONFILESaveMode]
 * @param createFileRecursive true to also create the file and missing directories in case they do not exist;
 *  false to create only missing file, but not create missing parent directories
 */
data class IONFILESaveOptions(
    val data: String,
    val encoding: IONFILEEncoding,
    val mode: IONFILESaveMode,
    val createFileRecursive: Boolean
)

/**
 * Mode for saving the file
 */
enum class IONFILESaveMode {
    /**
     * Write mode - Will write to the beginning of the file, overriding any contents that were previously written
     */
    WRITE,

    /**
     * Append mode - Will write to the end of the file, leaving contents that were previously written intact
     */
    APPEND
}