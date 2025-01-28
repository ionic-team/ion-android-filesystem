package io.ionic.libs.ionfilesystemlib.controller

import io.ionic.libs.ionfilesystemlib.common.IONFLSTBaseTest
import io.ionic.libs.ionfilesystemlib.model.IONFLSTCreateOptions
import io.ionic.libs.ionfilesystemlib.model.IONFLSTDeleteOptions
import io.ionic.libs.ionfilesystemlib.model.IONFLSTEncoding
import io.ionic.libs.ionfilesystemlib.model.IONFLSTExceptions
import io.ionic.libs.ionfilesystemlib.model.IONFLSTFileType
import io.ionic.libs.ionfilesystemlib.model.IONFLSTMetadataResult
import io.ionic.libs.ionfilesystemlib.model.IONFLSTSaveMode
import io.ionic.libs.ionfilesystemlib.model.IONFLSTSaveOptions
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IONFLSTDirectoriesHelperTest : IONFLSTBaseTest() {
    private lateinit var sut: IONFLSTDirectoriesHelper

    override fun additionalSetups() {
        sut = IONFLSTDirectoriesHelper()
    }

    // region createDirectory tests
    @Test
    fun `given there is a parent directory, when we create a directory there, success is returned`() =
        runTest {
            val dir = dirInRootDir
            val path = dir.absolutePath

            val result =
                sut.createDirectory(path, IONFLSTCreateOptions(recursive = false, exclusive = false))

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
                sut.createDirectory(path, IONFLSTCreateOptions(recursive = true, exclusive = false))

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
                sut.createDirectory(path, IONFLSTCreateOptions(recursive = false, exclusive = false))

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFLSTExceptions.CreateFailed.NoParentDirectory)
            assertFalse(dir.exists())
        }

    @Test
    fun `given directory exists, when we create a directory with exclusive=true, AlreadyExists error is returned`() =
        runTest {
            val existingDir = testRootDirectory
            val path = existingDir.absolutePath

            val result =
                sut.createDirectory(path, IONFLSTCreateOptions(recursive = true, exclusive = true))

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFLSTExceptions.CreateFailed.AlreadyExists)
            assertTrue(existingDir.exists())
        }

    @Test
    fun `given directory exists, when we attempt to create it with exclusive=false, success is returned`() =
        runTest {
            val existingDir = testRootDirectory
            val path = existingDir.absolutePath

            val result =
                sut.createDirectory(path, IONFLSTCreateOptions(recursive = true, exclusive = false))

            assertTrue(result.isSuccess)
            assertTrue(existingDir.exists())
        }
    // endregion createDirectory tests

    // region listDirectory tests
    @Test
    fun `given directory is empty, when listing directory, empty list is returned`() = runTest {
        val path = testRootDirectory.absolutePath

        val result = sut.listDirectory(path)

        assertTrue(result.isSuccess)
        assertEquals(emptyList<IONFLSTMetadataResult>(), result.getOrNull())
    }

    @Test
    fun `given directory has a file, when listing directory, a one-item list with the file metadata is returned`() =
        runTest {
            val path = testRootDirectory.absolutePath
            val data = "Text"
            IONFLSTLocalFilesHelper().saveFile(
                fileInRootDir.absolutePath,
                IONFLSTSaveOptions(
                    data,
                    IONFLSTEncoding.DefaultCharset,
                    IONFLSTSaveMode.WRITE,
                    createFileRecursive = true
                )
            )

            val result = sut.listDirectory(path)

            assertTrue(result.isSuccess)
            result.getOrNull()!!.let {
                assertEquals(1, it.size)
                assertTrue(it.first().type is IONFLSTFileType.File)
                assertEquals(data.length.toLong(), it.first().size)
            }
        }

    @Test
    fun `given directory has a sub-directory, when listing directory, a one-item list with the directory metadata is returned`() =
        runTest {
            val path = testRootDirectory.absolutePath
            sut.createDirectory(
                dirInRootDir.absolutePath, IONFLSTCreateOptions(recursive = false, exclusive = false)
            )

            val result = sut.listDirectory(path)

            assertTrue(result.isSuccess)
            result.getOrNull()!!.let {
                assertEquals(1, it.size)
                assertEquals(DIR_NAME, it.first().name)
                assertTrue(it.first().type is IONFLSTFileType.Directory)
            }
        }

    @Test
    fun `given directory several nested directories, when listing directory, a list with the directories metadata is returned`() =
        runTest {
            val path = testRootDirectory.absolutePath
            sut.createDirectory(
                dirInSubDir.absolutePath, IONFLSTCreateOptions(recursive = true, exclusive = false)
            )
            IONFLSTLocalFilesHelper().saveFile(
                fileInSubDir.absolutePath,
                IONFLSTSaveOptions(
                    "File",
                    IONFLSTEncoding.DefaultCharset,
                    IONFLSTSaveMode.WRITE,
                    createFileRecursive = true
                )
            )

            val result = sut.listDirectory(path)

            assertTrue(result.isSuccess)
            result.getOrNull()!!.let {
                assertEquals(2, it.size)
                assertTrue(it.all { item -> item.type is IONFLSTFileType.Directory })
            }
        }

    @Test
    fun `given directory does not exist, when listing directory, DoesNotExist error is returned`() =
        runTest {
            val path = dirInSubDir.absolutePath

            val result = sut.listDirectory(path)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFLSTExceptions.DoesNotExist)
        }
    // endregion listDirectory tests

    // region deleteDirectory tests
    @Test
    fun `given empty directory exists, when we delete it with recursive=false, success is returned`() =
        runTest {
            val dir = dirInRootDir
            val path = dir.absolutePath
            sut.createDirectory(path, IONFLSTCreateOptions(recursive = false, exclusive = false))

            val result = sut.deleteDirectory(path, IONFLSTDeleteOptions(recursive = false))

            assertTrue(result.isSuccess)
            assertFalse(dir.exists())
        }

    @Test
    fun `given non-empty directory exists, when we delete it with recursive=true, all children are deleted and success is returned`() =
        runTest {
            val dir = dirInSubDir
            val dirPath = dir.absolutePath
            val dirToDelete = dir.parentFile!!
            sut.createDirectory(dirPath, IONFLSTCreateOptions(recursive = true, exclusive = false))

            val result =
                sut.deleteDirectory(dirToDelete.absolutePath, IONFLSTDeleteOptions(true))

            assertTrue(result.isSuccess)
            assertFalse(dirToDelete.exists() && dir.exists())
        }

    @Test
    fun `given non-empty directory exists, when we delete it with recursive=false, CannotDeleteChildren error is returned`() =
        runTest {
            val dir = dirInSubDir
            val dirPath = dir.absolutePath
            val dirToDelete = dir.parentFile!!
            sut.createDirectory(dirPath, IONFLSTCreateOptions(recursive = true, exclusive = false))

            val result = sut.deleteDirectory(dirToDelete.absolutePath, IONFLSTDeleteOptions(false))

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFLSTExceptions.DeleteFailed.CannotDeleteChildren)
            assertTrue(dirToDelete.exists() && dir.exists())
        }

    @Test
    fun `given directory does not exist, when we delete it, DoesNotExist error is returned`() =
        runTest {
            val dir = dirInRootDir
            val path = dir.absolutePath

            val result = sut.deleteDirectory(path, IONFLSTDeleteOptions(recursive = true))

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFLSTExceptions.DoesNotExist)
        }
    // endregion deleteDirectory tests
}