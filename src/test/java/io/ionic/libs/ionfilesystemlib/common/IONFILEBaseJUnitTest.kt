package io.ionic.libs.ionfilesystemlib.common

import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import io.ionic.libs.ionfilesystemlib.helper.common.IONFILEBuildConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import java.io.File
import java.util.Base64

abstract class IONFILEBaseJUnitTest {
    protected lateinit var testRootDirectory: File
    protected val fileInRootDir: File get() = File(testRootDirectory, FILE_NAME_TXT)
    protected val dirInRootDir: File get() = File(testRootDirectory, DIR_NAME)
    protected val fileInSubDir: File
        get() = File(
            File(testRootDirectory, "subdir1/subdir2"),
            FILE_NAME_PDF
        )
    protected val dirInSubDir: File
        get() = File(
            File(testRootDirectory, "subdir3/subdir4"),
            SUBDIR_NAME
        )

    protected abstract fun additionalSetups()
    protected open fun additionalTearDowns() {}

    protected fun mockkMimeTypeMap(mimeType: String?) {
        every { MimeTypeMap.getSingleton() } returns mockk {
            every { getMimeTypeFromExtension(any()) } returns mimeType
        }
    }

    @Before
    fun setUp() {
        testRootDirectory = File(System.getProperty("java.io.tmpdir"), ROOT_DIR_NAME).apply {
            mkdirs()
        }
        // these asserts are to make sure the code in `tearDown`,
        // if one of the tests crashes, it may cause the asserts to fail
        assertTrue(testRootDirectory.exists())
        assertTrue(testRootDirectory.list().isNullOrEmpty())

        // substitute android implementation for base64 so that unit tests work
        mockkStatic(android.util.Base64::class)
        every { android.util.Base64.encodeToString(any(), any()) } answers {
            Base64.getEncoder().encodeToString(args.first() as ByteArray)
        }
        every { android.util.Base64.decode(any<String>(), any()) } answers {
            Base64.getDecoder().decode(args.first() as String)
        }
        every { android.util.Base64.decode(any<ByteArray>(), any()) } answers {
            Base64.getDecoder().decode(args.first() as ByteArray)
        }

        mockkStatic(Uri::class)
        every { Uri.fromFile(any()) } returns mockk()

        mockkObject(IONFILEBuildConfig)
        every { IONFILEBuildConfig.getAndroidSdkVersionCode() } returns Build.VERSION_CODES.VANILLA_ICE_CREAM
        mockkStatic(MimeTypeMap::class)
        mockkMimeTypeMap(PDF_MIME_TYPE)

        additionalSetups()
    }

    @After
    fun tearDown() {
        unmockkStatic(MimeTypeMap::class)
        unmockkObject(IONFILEBuildConfig)
        unmockkStatic(Uri::class)
        unmockkStatic(android.util.Base64::class)
        testRootDirectory.deleteRecursively()
        additionalTearDowns()
    }

    companion object {
        const val ROOT_DIR_NAME = "testDir"
        const val FILE_NAME_TXT = "file.txt"
        const val FILE_NAME_PDF = "doc.pdf"
        const val DIR_NAME = "dir"
        const val SUBDIR_NAME = "directory"
        const val TEXT_MIME_TYPE = "text/plain"
        const val PDF_MIME_TYPE = "application/pdf"
    }
}