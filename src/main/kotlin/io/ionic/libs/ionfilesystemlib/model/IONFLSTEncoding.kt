package io.ionic.libs.ionfilesystemlib.model

import java.nio.charset.Charset

sealed class IONFLSTEncoding {
    data object Base64 : IONFLSTEncoding()

    data class WithCharset(val charset: Charset) : IONFLSTEncoding()

    companion object {
        val Default = Base64
        val DefaultCharset = WithCharset(Charsets.UTF_8)
    }
}