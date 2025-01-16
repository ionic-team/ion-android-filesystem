package io.ionic.libs.osfilesystemlib.common

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import java.io.File
import java.util.Base64

abstract class OSFLSTBaseTest {
    protected lateinit var testRootDirectory: File
    protected val fileInRootDir: File get() = File(testRootDirectory, "file.txt")
    protected val dirInRootDir: File get() = File(testRootDirectory, "dir")
    protected val fileInSubDir: File
        get() = File(
            File(testRootDirectory, "subdir1/subdir2"),
            "doc.pdf"
        )
    protected val dirInSubDir: File
        get() = File(
            File(testRootDirectory, "subdir1/subdir2"),
            "directory"
        )

    protected abstract fun additionalSetups()
    protected open fun additionalTearDowns() {}

    @Before
    fun setUp() {
        testRootDirectory = File(System.getProperty("java.io.tmpdir"), "testDir").apply {
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

        additionalSetups()
    }

    @After
    fun tearDown() {
        unmockkStatic(android.util.Base64::class)
        testRootDirectory.deleteRecursively()
        additionalTearDowns()
    }
}