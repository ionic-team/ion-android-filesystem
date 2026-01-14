package io.ionic.libs.ionfilesystemlib.model

import io.ionic.libs.ionfilesystemlib.helper.common.LENGTH_DEFAULT_VALUE

/**
 * Parameters for reading a file all at once
 *
 * @param encoding how the file data to return should be encoded; see [IONFILEEncoding]
 * @param offset An optional number of bytes to skip from the start of the file.
 * Default is 0 (read from beginning of file).
 * @param length An optional maximum number of bytes to read after the [offset].
 * If fewer bytes are available before the end of the file, only the available bytes are returned.
 * By default, will read the entirety of contents after [offset].
 */
data class IONFILEReadOptions(
    val encoding: IONFILEEncoding,
    val offset: Int = 0,
    val length: Int = LENGTH_DEFAULT_VALUE
)