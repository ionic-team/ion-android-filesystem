package io.ionic.libs.ionfilesystemlib.model

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class IONFILEEncodingTest {

    @Test
    fun `given utf8 is passed, the correct WithCharset encoding is returned`() = runTest {
        val input = "utf8"

        val result = IONFILEEncoding.fromEncodingName(input)

        assertEquals(IONFILEEncoding.WithCharset(Charsets.UTF_8), result)
    }

    @Test
    fun `given utf16 is passed, the correct WithCharset encoding is returned`() = runTest {
        val input = "utf16"

        val result = IONFILEEncoding.fromEncodingName(input)

        assertEquals(IONFILEEncoding.WithCharset(Charsets.UTF_16), result)
    }

    @Test
    fun `given ascii is passed, the correct WithCharset encoding is returned`() = runTest {
        val input = "ascii"

        val result = IONFILEEncoding.fromEncodingName(input)

        assertEquals(IONFILEEncoding.WithCharset(Charsets.US_ASCII), result)
    }

    @Test
    fun `given null is passed, base64 encoding is returned`() = runTest {
        val input = null

        val result = IONFILEEncoding.fromEncodingName(input)

        assertEquals(IONFILEEncoding.Base64, result)
    }

    @Test
    fun `given blank string is passed, base64 encoding is returned`() = runTest {
        val input = "  "

        val result = IONFILEEncoding.fromEncodingName(input)

        assertEquals(IONFILEEncoding.Base64, result)
    }

    @Test
    fun `given unknown charset is passed, UTF-8 encoding is returned`() = runTest {
        val input = "unknown"

        val result = IONFILEEncoding.fromEncodingName(input)

        assertEquals(IONFILEEncoding.WithCharset(Charsets.UTF_8), result)
    }
}