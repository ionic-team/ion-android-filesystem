package io.ionic.libs.osfilesystemlib.controller

import io.ionic.libs.osfilesystemlib.model.OSFLSTCreateOptions
import io.ionic.libs.osfilesystemlib.model.OSFLSTDeleteOptions
import io.ionic.libs.osfilesystemlib.model.OSFLSTEncoding
import io.ionic.libs.osfilesystemlib.model.OSFLSTExceptions
import io.ionic.libs.osfilesystemlib.model.OSFLSTReadOptions
import io.ionic.libs.osfilesystemlib.model.OSFLSTSaveMode
import io.ionic.libs.osfilesystemlib.model.OSFLSTSaveOptions
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.Base64

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

        sut = OSFLSTFileHelper()
    }

    @After
    fun tearDown() {
        unmockkStatic(android.util.Base64::class)
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

    // region delete tests
    @Test
    fun `given file exists, when we delete it, success is returned`() = runTest {
        val file = fileInRootDir
        val path = file.absolutePath
        sut.createFile(OSFLSTCreateOptions(path, recursive = false, exclusive = false))

        val result = sut.delete(OSFLSTDeleteOptions(path, recursive = false))

        assertTrue(result.isSuccess)
        assertFalse(file.exists())
    }

    @Test
    fun `given empty directory exists, when we delete it with recursive=false, success is returned`() =
        runTest {
            val dir = dirInRootDir
            val path = dir.absolutePath
            sut.createDirectory(OSFLSTCreateOptions(path, recursive = false, exclusive = false))

            val result = sut.delete(OSFLSTDeleteOptions(path, recursive = false))

            assertTrue(result.isSuccess)
            assertFalse(dir.exists())
        }

    @Test
    fun `given non-empty directory exists, when we delete it with recursive=true, success is returned`() =
        runTest {
            val file = fileInSubDir
            val filePath = file.absolutePath
            val parentDir = file.parentFile!!
            sut.createFile(OSFLSTCreateOptions(filePath, recursive = true, exclusive = false))

            val result = sut.delete(OSFLSTDeleteOptions(parentDir.absolutePath, recursive = true))

            assertTrue(result.isSuccess)
            assertFalse(parentDir.exists() && file.exists())
        }

    @Test
    fun `given non-empty directory exists, when we delete it with recursive=false, CannotDeleteChildren error is returned`() =
        runTest {
            val file = fileInSubDir
            val filePath = file.absolutePath
            val parentDir = file.parentFile!!
            sut.createFile(OSFLSTCreateOptions(filePath, recursive = true, exclusive = false))

            val result = sut.delete(OSFLSTDeleteOptions(parentDir.absolutePath, recursive = false))

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is OSFLSTExceptions.DeleteFailed.CannotDeleteChildren)
            assertTrue(parentDir.exists() && file.exists())
        }

    @Test
    fun `given file does not exist, when we delete it, DoesNotExist error is returned`() = runTest {
        val file = fileInRootDir
        val path = file.absolutePath

        val result = sut.delete(OSFLSTDeleteOptions(path, recursive = false))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is OSFLSTExceptions.DeleteFailed.DoesNotExist)
    }

    @Test
    fun `given directory does not exist, when we delete it, DoesNotExist error is returned`() =
        runTest {
            val dir = dirInRootDir
            val path = dir.absolutePath

            val result = sut.delete(OSFLSTDeleteOptions(path, recursive = true))

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is OSFLSTExceptions.DeleteFailed.DoesNotExist)
        }
    // endregion delete tests

    // region save + read file tests
    @Test
    fun `given empty file exists, when saving contents to file as write, success is returned`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath
            sut.createFile(OSFLSTCreateOptions(path, recursive = false, exclusive = false))
            val data = "Write"

            val result = sut.saveFile(
                OSFLSTSaveOptions(
                    path,
                    data,
                    encoding = OSFLSTEncoding.DefaultCharset,
                    mode = OSFLSTSaveMode.WRITE,
                    createFileRecursive = false
                )
            )

            assertTrue(result.isSuccess)
            assertTrue(file.length() > 0)
        }

    @Test
    fun `given empty file exists, when saving contents to file as append, success is returned`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath
            sut.createFile(OSFLSTCreateOptions(path, recursive = false, exclusive = false))
            val data = "Append"

            val result = sut.saveFile(
                OSFLSTSaveOptions(
                    path,
                    data,
                    encoding = OSFLSTEncoding.DefaultCharset,
                    mode = OSFLSTSaveMode.APPEND,
                    createFileRecursive = false
                )
            )

            assertTrue(result.isSuccess)
            assertTrue(file.length() > 0)
        }

    @Test
    fun `given empty file exists, when saving contents as base64, success is returned`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath
            sut.createFile(OSFLSTCreateOptions(path, recursive = false, exclusive = false))
            val dataBase64 =
                Base64.getEncoder().encodeToString("Base 64 w/ special ch4rsª~´".toByteArray())

            val result = sut.saveFile(
                OSFLSTSaveOptions(
                    path,
                    dataBase64,
                    encoding = OSFLSTEncoding.Base64,
                    mode = OSFLSTSaveMode.WRITE,
                    createFileRecursive = false
                )
            )

            assertTrue(result.isSuccess)
            assertTrue(file.length() > 0)
        }

    @Test
    fun `given empty file exists, when reading file, success is returned with empty string`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath
            sut.createFile(OSFLSTCreateOptions(path, recursive = false, exclusive = false))

            val result = sut.readFile(
                OSFLSTReadOptions(path, encoding = OSFLSTEncoding.Default)
            )

            assertTrue(result.isSuccess)
            assertEquals("", result.getOrNull())
        }

    @Test
    fun `given non-empty file exists, when reading file, success is returned with contents`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath
            sut.createFile(OSFLSTCreateOptions(path, recursive = false, exclusive = false))
            val data = "Text"
            sut.saveFile(
                OSFLSTSaveOptions(
                    path, data, OSFLSTEncoding.DefaultCharset, OSFLSTSaveMode.WRITE, false
                )
            )

            val result = sut.readFile(
                OSFLSTReadOptions(path, encoding = OSFLSTEncoding.DefaultCharset)
            )

            assertTrue(result.isSuccess)
            assertEquals("Text", result.getOrNull())
        }

    @Test
    fun `given non-empty file saved as base64, when reading file as utf-8, success is returned with utf-8 string`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath
            sut.createFile(OSFLSTCreateOptions(path, recursive = false, exclusive = false))
            val data = Base64.getEncoder().encodeToString("Base 64 +/ =!\uD83D\uDE00".toByteArray())
            sut.saveFile(
                OSFLSTSaveOptions(
                    path, data, OSFLSTEncoding.Base64, OSFLSTSaveMode.WRITE, false
                )
            )

            val result = sut.readFile(
                OSFLSTReadOptions(path, encoding = OSFLSTEncoding.WithCharset(Charsets.UTF_8))
            )

            assertTrue(result.isSuccess)
            assertEquals("Base 64 +/ =!\uD83D\uDE00", result.getOrNull())
        }

    @Test
    fun `given non-empty file saved as data url with base64, when reading file as utf-8, success is returned with utf-8 string without the prefix`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath
            sut.createFile(OSFLSTCreateOptions(path, recursive = false, exclusive = false))
            val base64 = Base64.getEncoder().encodeToString("Base 64 +/ =!  ".toByteArray())
            val data = "data:textPlain;base64, $base64"
            sut.saveFile(
                OSFLSTSaveOptions(
                    path, data, OSFLSTEncoding.Base64, OSFLSTSaveMode.WRITE, false
                )
            )

            val result = sut.readFile(
                OSFLSTReadOptions(path, encoding = OSFLSTEncoding.WithCharset(Charsets.UTF_8))
            )

            // This test is to make sure we are not assuming that the contents com as a url
            //  This stripping needs to be done by the caller
            assertTrue(result.isSuccess)
            assertEquals("Base 64 +/ =!  ", result.getOrNull())
        }

    @Test
    fun `given non-empty file saved as utf-8, when reading file as base64, success is returned with base64 encoded string`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath
            sut.createFile(OSFLSTCreateOptions(path, recursive = false, exclusive = false))
            val data = "Lorem ipsum"
            sut.saveFile(
                OSFLSTSaveOptions(
                    path,
                    data,
                    OSFLSTEncoding.WithCharset(Charsets.UTF_8),
                    OSFLSTSaveMode.WRITE,
                    false
                )
            )

            val result = sut.readFile(OSFLSTReadOptions(path, encoding = OSFLSTEncoding.Base64))

            assertTrue(result.isSuccess)
            assertEquals("TG9yZW0gaXBzdW0=", result.getOrNull())
        }

    @Test
    fun `given non-empty file exists, when saving contents to file as write, success is returned and original content is gone`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath
            sut.createFile(OSFLSTCreateOptions(path, recursive = false, exclusive = false))
            val originalData = "Original"
            sut.saveFile(
                OSFLSTSaveOptions(
                    path, originalData, OSFLSTEncoding.DefaultCharset, OSFLSTSaveMode.WRITE, false
                )
            )
            val newData = "New content"

            val saveResult = sut.saveFile(
                OSFLSTSaveOptions(
                    path,
                    newData,
                    encoding = OSFLSTEncoding.DefaultCharset,
                    mode = OSFLSTSaveMode.WRITE,
                    createFileRecursive = false
                )
            )
            val readResult = sut.readFile(OSFLSTReadOptions(path, OSFLSTEncoding.DefaultCharset))

            assertTrue(saveResult.isSuccess)
            assertEquals("New content", readResult.getOrNull())
        }

    @Test
    fun `given non-empty file exists, when saving contents to file as append, success is returned and original content remains`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath
            sut.createFile(OSFLSTCreateOptions(path, recursive = false, exclusive = false))
            val originalData = "Original"
            sut.saveFile(
                OSFLSTSaveOptions(
                    path, originalData, OSFLSTEncoding.DefaultCharset, OSFLSTSaveMode.WRITE, false
                )
            )
            val newData = "\n\n\t-> New content"

            val saveResult = sut.saveFile(
                OSFLSTSaveOptions(
                    path,
                    newData,
                    encoding = OSFLSTEncoding.DefaultCharset,
                    mode = OSFLSTSaveMode.APPEND,
                    createFileRecursive = false
                )
            )
            val readResult = sut.readFile(OSFLSTReadOptions(path, OSFLSTEncoding.DefaultCharset))

            assertTrue(saveResult.isSuccess)
            assertEquals(originalData + newData, readResult.getOrNull())
        }

    @Test
    fun `given file does not exist, when saving contents to file with createFileRecursive=false, success is returned`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath

            val result = sut.saveFile(
                OSFLSTSaveOptions(
                    path,
                    "any data...",
                    encoding = OSFLSTEncoding.DefaultCharset,
                    mode = OSFLSTSaveMode.WRITE,
                    createFileRecursive = false
                )
            )

            assertTrue(result.isSuccess)
            assertTrue(file.length() > 0)
        }

    @Test
    fun `given nor file nor parent directories exist, when saving contents to file with createFileRecursive=true, success is returned`() =
        runTest {
            val file = fileInSubDir
            val path = file.absolutePath

            val result = sut.saveFile(
                OSFLSTSaveOptions(
                    path,
                    "any data...",
                    encoding = OSFLSTEncoding.DefaultCharset,
                    mode = OSFLSTSaveMode.WRITE,
                    createFileRecursive = true
                )
            )

            assertTrue(result.isSuccess)
            assertTrue(file.length() > 0)
        }

    @Test
    fun `given file does not exist, when saving contents to file with create=null, DoesNotExist error is returned`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath

            val result = sut.saveFile(
                OSFLSTSaveOptions(
                    path,
                    "any data...",
                    encoding = OSFLSTEncoding.DefaultCharset,
                    mode = OSFLSTSaveMode.WRITE,
                    createFileRecursive = null
                )
            )

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is OSFLSTExceptions.SaveFailed.DoesNotExist)
        }

    @Test
    fun `given nor file nor parent directories exist, when saving contents to file with createFileRecursive=false, error is returned`() =
        runTest {
            val file = fileInSubDir
            val path = file.absolutePath

            val result = sut.saveFile(
                OSFLSTSaveOptions(
                    path,
                    "any data...",
                    encoding = OSFLSTEncoding.DefaultCharset,
                    mode = OSFLSTSaveMode.WRITE,
                    createFileRecursive = false
                )
            )

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is OSFLSTExceptions.CreateFailed.NoParentDirectory)
        }

    @Test
    fun `given file does not exist, when reading file, DoesNotExist error is returned`() = runTest {
        val file = fileInRootDir
        val path = file.absolutePath

        val result = sut.readFile(
            OSFLSTReadOptions(
                path,
                encoding = OSFLSTEncoding.Default
            )
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is OSFLSTExceptions.ReadFailed.DoesNotExist)
    }

    @Test
    fun `given there is a directory, when trying to save it as a file, IsDirectory error is returned`() =
        runTest {
            val dir = dirInSubDir
            val path = dir.absolutePath
            sut.createDirectory(OSFLSTCreateOptions(path, recursive = true, exclusive = false))


            val result = sut.saveFile(
                OSFLSTSaveOptions(
                    path,
                    "any data...",
                    // this would result in an error because the provided data is not base64
                    //  however because we are providing a directory, the method should return before base64 conversion
                    encoding = OSFLSTEncoding.Base64,
                    mode = OSFLSTSaveMode.WRITE,
                    createFileRecursive = true
                )
            )

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is OSFLSTExceptions.SaveFailed.IsDirectory)
        }

    @Test
    fun `given there is a directory, when trying to read it as a file, IsDirectory error is returned`() =
        runTest {
            val dir = dirInRootDir
            val path = dir.absolutePath
            sut.createDirectory(OSFLSTCreateOptions(path, recursive = false, exclusive = false))

            val result = sut.readFile(OSFLSTReadOptions(path, OSFLSTEncoding.Default))

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is OSFLSTExceptions.ReadFailed.IsDirectory)
        }
    // endregion save + read file tests
}