package io.ionic.libs.ionfilesystemlib.model

import java.nio.charset.Charset

sealed class IONFLSTEncoding {
    data object Base64 : IONFLSTEncoding()

    data class WithCharset(val charset: Charset) : IONFLSTEncoding()

    companion object {
        internal val Default = Base64
        internal val DefaultCharset = WithCharset(Charsets.UTF_8)

        fun fromEncodingName(encodingName: String?): IONFLSTEncoding =
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