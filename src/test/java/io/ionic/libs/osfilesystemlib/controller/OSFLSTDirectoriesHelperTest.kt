package io.ionic.libs.osfilesystemlib.controller

import io.ionic.libs.osfilesystemlib.common.OSFLSTBaseTest
import io.ionic.libs.osfilesystemlib.model.OSFLSTCreateOptions
import io.ionic.libs.osfilesystemlib.model.OSFLSTDeleteOptions
import io.ionic.libs.osfilesystemlib.model.OSFLSTEncoding
import io.ionic.libs.osfilesystemlib.model.OSFLSTExceptions
import io.ionic.libs.osfilesystemlib.model.OSFLSTFileType
import io.ionic.libs.osfilesystemlib.model.OSFLSTMetadataResult
import io.ionic.libs.osfilesystemlib.model.OSFLSTSaveMode
import io.ionic.libs.osfilesystemlib.model.OSFLSTSaveOptions
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OSFLSTDirectoriesHelperTest : OSFLSTBaseTest() {
    private lateinit var sut: OSFLSTDirectoriesHelper

    override fun additionalSetups() {
        sut = OSFLSTDirectoriesHelper()
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

    // region deleteDirectory tests
    @Test
    fun `given empty directory exists, when we delete it with recursive=false, success is returned`() =
        runTest {
            val dir = dirInRootDir
            val path = dir.absolutePath
            sut.createDirectory(OSFLSTCreateOptions(path, recursive = false, exclusive = false))

            val result = sut.deleteDirectory(OSFLSTDeleteOptions(path, recursive = false))

            assertTrue(result.isSuccess)
            assertFalse(dir.exists())
        }

    @Test
    fun `given non-empty directory exists, when we delete it with recursive=true, all children are deleted and success is returned`() =
        runTest {
            val dir = dirInSubDir
            val dirPath = dir.absolutePath
            val dirToDelete = dir.parentFile!!
            sut.createDirectory(OSFLSTCreateOptions(dirPath, recursive = true, exclusive = false))

            val result =
                sut.deleteDirectory(OSFLSTDeleteOptions(dirToDelete.absolutePath, recursive = true))

            assertTrue(result.isSuccess)
            assertFalse(dirToDelete.exists() && dir.exists())
        }

    @Test
    fun `given non-empty directory exists, when we delete it with recursive=false, CannotDeleteChildren error is returned`() =
        runTest {
            val dir = dirInSubDir
            val dirPath = dir.absolutePath
            val dirToDelete = dir.parentFile!!
            sut.createDirectory(OSFLSTCreateOptions(dirPath, recursive = true, exclusive = false))

            val result = sut.deleteDirectory(
                OSFLSTDeleteOptions(
                    dirToDelete.absolutePath,
                    recursive = false
                )
            )

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is OSFLSTExceptions.DeleteFailed.CannotDeleteChildren)
            assertTrue(dirToDelete.exists() && dir.exists())
        }

    @Test
    fun `given directory does not exist, when we delete it, DoesNotExist error is returned`() =
        runTest {
            val dir = dirInRootDir
            val path = dir.absolutePath

            val result = sut.deleteDirectory(OSFLSTDeleteOptions(path, recursive = true))

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is OSFLSTExceptions.DoesNotExist)
        }
    // endregion deleteDirectory tests

    // region listDirectory tests
    @Test
    fun `given directory is empty, when listing directory, empty list is returned`() = runTest {
        val path = testRootDirectory.absolutePath

        val result = sut.listDirectory(path)

        assertTrue(result.isSuccess)
        assertEquals(emptyList<OSFLSTMetadataResult>(), result.getOrNull())
    }

    @Test
    fun `given directory has a file, when listing directory, a one-item list with the file metadata is returned`() =
        runTest {
            val path = testRootDirectory.absolutePath
            val data = "Text"
            OSFLSTLocalFilesHelper().saveFile(
                OSFLSTSaveOptions(
                    fileInRootDir.absolutePath,
                    data,
                    OSFLSTEncoding.DefaultCharset,
                    OSFLSTSaveMode.WRITE,
                    createFileRecursive = true
                )
            )

            val result = sut.listDirectory(path)

            assertTrue(result.isSuccess)
            result.getOrNull()!!.let {
                assertEquals(1, it.size)
                assertTrue(it.first().type is OSFLSTFileType.File)
                assertEquals(data.length.toLong(), it.first().size)
            }
        }

    @Test
    fun `given directory has a sub-directory, when listing directory, a one-item list with the directory metadata is returned`() =
        runTest {
            val path = testRootDirectory.absolutePath
            sut.createDirectory(
                OSFLSTCreateOptions(dirInRootDir.absolutePath, recursive = false, exclusive = false)
            )

            val result = sut.listDirectory(path)

            assertTrue(result.isSuccess)
            result.getOrNull()!!.let {
                assertEquals(1, it.size)
                assertEquals(DIR_NAME, it.first().name)
                assertTrue(it.first().type is OSFLSTFileType.Directory)
            }
        }

    @Test
    fun `given directory several nested directories, when listing directory, a list with the directories metadata is returned`() =
        runTest {
            val path = testRootDirectory.absolutePath
            sut.createDirectory(
                OSFLSTCreateOptions(dirInSubDir.absolutePath, recursive = true, exclusive = false)
            )
            OSFLSTLocalFilesHelper().saveFile(
                OSFLSTSaveOptions(
                    fileInSubDir.absolutePath,
                    "File",
                    OSFLSTEncoding.DefaultCharset,
                    OSFLSTSaveMode.WRITE,
                    createFileRecursive = true
                )
            )

            val result = sut.listDirectory(path)

            assertTrue(result.isSuccess)
            result.getOrNull()!!.let {
                assertEquals(2, it.size)
                assertTrue(it.all { item -> item.type is OSFLSTFileType.Directory })
            }
        }

    @Test
    fun `given directory does not exist, when listing directory, DoesNotExist error is returned`() =
        runTest {
            val path = dirInSubDir.absolutePath

            val result = sut.listDirectory(path)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is OSFLSTExceptions.DoesNotExist)
        }
    // endregion listDirectory tests
}