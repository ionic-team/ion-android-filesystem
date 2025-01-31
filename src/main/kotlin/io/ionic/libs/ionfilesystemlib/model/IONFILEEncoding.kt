package io.ionic.libs.ionfilesystemlib.model

import java.nio.charset.Charset

sealed class IONFILEEncoding {
    data object Base64 : IONFILEEncoding()

    data class WithCharset(val charset: Charset) : IONFILEEncoding()

    companion object {
        internal val Default = Base64
        internal val DefaultCharset = WithCharset(Charsets.UTF_8)

        fun fromEncodingName(encodingName: String?): IONFILEEncoding =
            if (encodingName.isNullOrBlank()) {
                Default
            } else {
                try {
                    WithCharset(Charset.forName(encodingName))
                } catch (ex: Exception) {
                    DefaultCharset
                }
            }
    }
}