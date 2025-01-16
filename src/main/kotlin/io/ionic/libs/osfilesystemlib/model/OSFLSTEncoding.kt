package io.ionic.libs.osfilesystemlib.model

import java.nio.charset.Charset

sealed class OSFLSTEncoding {
    data object Base64 : OSFLSTEncoding()

    data class WithCharset(val charset: Charset) : OSFLSTEncoding()

    companion object {
        val Default = Base64
        val DefaultCharset = WithCharset(Charsets.UTF_8)
    }
}