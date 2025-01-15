package io.ionic.libs.osfilesystemlib.controller

import io.ionic.libs.osfilesystemlib.model.OSFLSTCreateOptions
import io.ionic.libs.osfilesystemlib.model.OSFLSTExceptions
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class OSFLSTFileHelperTest {

    private lateinit var sut: OSFLSTFileHelper

    private lateinit var testRootDirectory: File
    private val fileInRootDir: File get() = File(testRootDirectory, "file.txt")
    private val dirInRootDir: File get() = File(testRootDirectory, "dir")
    private val fileInSubDir: File
        get() = File(
            File(testRootDirectory, "subdir1/subdir2"),
            "doc.pdf"
        )
    private val dirInSubDir: File
        get() = File(
            File(testRootDirectory, "subdir1/subdir2"),
            "directory"
        )

    @Before
    fun setUp() {
        // Create a temporary directory for testing
        testRootDirectory = File(System.getProperty("java.io.tmpdir"), "testDir").apply {
            mkdirs()
        }
        assertTrue(testRootDirectory.exists())
        assertTrue(testRootDirectory.list().isNullOrEmpty())
        sut = OSFLSTFileHelper()
    }

    @After
    fun tearDown() {
        // Clean up temporary directory
        testRootDirectory.deleteRecursively()
    }

    // region createDirectory tests
    @Test
    fun `given there is a parent directory, when we create a directory there, success is returned`() =
        runTest {
            val dir = dirInRootDir
            val path = dir.absolutePath

            val result =
                sut.createDirectory(OSFLSTCreateOptions(path, recursive = false, exclusive = false))

            assertTrue(result.isSuccess)
            assertTrue(dir.exists())
            assertTrue(dir.isDirectory)
        }

    @Test
    fun `given there is no parent directory, when we create a directory with recursive=true, success is returned`() =
        runTest {
            val dir = dirInSubDir
            val path = dir.absolutePath

            val result =
                sut.createDirectory(OSFLSTCreateOptions(path, recursive = true, exclusive = false))

            assertTrue(result.isSuccess)
            assertTrue(dir.exists())
            assertTrue(dir.isDirectory)
        }

    @Test
    fun `given there is no parent directory, when we create a directory with recursive=false, NoParentDirectory error is returned`() =
        runTest {
            val dir = dirInSubDir
            val path = dir.absolutePath

            val result =
                sut.createDirectory(OSFLSTCreateOptions(path, recursive = false, exclusive = false))

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is OSFLSTExceptions.CreateFailed.NoParentDirectory)
            assertFalse(dir.exists())
        }

    @Test
    fun `given directory exists, when we create a directory with exclusive=true, AlreadyExists error is returned`() =
        runTest {
            val existingDir = testRootDirectory
            val path = existingDir.absolutePath

            val result =
                sut.createDirectory(OSFLSTCreateOptions(path, recursive = true, exclusive = true))

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is OSFLSTExceptions.CreateFailed.AlreadyExists)
            assertTrue(existingDir.exists())
        }

    @Test
    fun `given directory exists, when we attempt to create it with exclusive=false, success is returned`() =
        runTest {
            val existingDir = testRootDirectory
            val path = existingDir.absolutePath

            val result =
                sut.createDirectory(OSFLSTCreateOptions(path, recursive = true, exclusive = false))

            assertTrue(result.isSuccess)
            assertTrue(existingDir.exists())
        }
    // endregion createDirectory tests

    // region createFile tests
    @Test
    fun `given there is a parent directory, when we create a file in that directory, success is returned`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath

            val result =
                sut.createFile(OSFLSTCreateOptions(path, recursive = false, exclusive = false))

            assertTrue(result.isSuccess)
            assertTrue(file.exists())
            assertTrue(file.isFile)
        }

    @Test
    fun `given there is no parent directory, when we create a file with recursive=true, success is returned`() =
        runTest {
            val file = fileInSubDir
            val path = file.absolutePath

            val result =
                sut.createFile(OSFLSTCreateOptions(path, recursive = true, exclusive = false))

            assertTrue(result.isSuccess)
            assertTrue(file.exists())
            assertTrue(file.isFile)
        }

    @Test
    fun `given there is no parent directory, when we create a file with recursive=false, NoParentDirectory error is returned`() =
        runTest {
            val file = fileInSubDir
            val path = file.absolutePath

            val result =
                sut.createFile(OSFLSTCreateOptions(path, recursive = false, exclusive = false))

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is OSFLSTExceptions.CreateFailed.NoParentDirectory)
            assertFalse(file.exists())
        }
    // endregion createFile tests
}