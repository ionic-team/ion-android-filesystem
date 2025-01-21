package io.ionic.libs.osfilesystemlib.model

/**
 * Parameters for saving data to a file
 *
 * @param data string contents to save in the file
 * @param encoding how the data is encoded as;
 *  for example if it is base64 then it will be decoded before writing; see [OSFLSTEncoding]
 * @param mode the mode for saving to a file; see [OSFLSTSaveMode]
 * @param createFileRecursive true to also create the file and missing directories in case they do not exist;
 *  false to create only missing file, but not create missing parent directories
 *  null if not meant to create the file, returns error if file does not exist
 */
data class OSFLSTSaveOptions(
    val data: String,
    val encoding: OSFLSTEncoding,
    val mode: OSFLSTSaveMode,
    val createFileRecursive: Boolean?
)

/**
 * Mode for saving the file
 */
enum class OSFLSTSaveMode {
    /**
     * Write mode - Will write to the beginning of the file, overriding any contents that were previously written
     */
    WRITE,

    /**
     * Append mode - Will write to the end of the file, leaving contents that were previously written intact
     */
    APPEND
}