package io.ionic.libs.ionfilesystemlib.helper

import io.ionic.libs.ionfilesystemlib.common.IONFILEBaseJUnitTest
import io.ionic.libs.ionfilesystemlib.model.IONFILECreateOptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEDeleteOptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEEncoding
import io.ionic.libs.ionfilesystemlib.model.IONFILEExceptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEFileType
import io.ionic.libs.ionfilesystemlib.model.IONFILEMetadataResult
import io.ionic.libs.ionfilesystemlib.model.IONFILESaveMode
import io.ionic.libs.ionfilesystemlib.model.IONFILESaveOptions
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class IONFILEDirectoriesHelperTest : IONFILEBaseJUnitTest() {
    private lateinit var sut: IONFILEDirectoriesHelper

    override fun additionalSetups() {
        sut = IONFILEDirectoriesHelper()
    }

    // region createDirectory tests
    @Test
    fun `given there is a parent directory, when we create a directory there, success is returned`() =
        runTest {
            val dir = dirInRootDir
            val path = dir.absolutePath

            val result = sut.createDirectory(path, IONFILECreateOptions(recursive = false))

            assertTrue(result.isSuccess)
            assertTrue(dir.exists())
            assertTrue(dir.isDirectory)
        }

    @Test
    fun `given there is no parent directory, when we create a directory with recursive=true, success is returned`() =
        runTest {
            val dir = dirInSubDir
            val path = dir.absolutePath

            val result = sut.createDirectory(path, IONFILECreateOptions(recursive = true))

            assertTrue(result.isSuccess)
            assertTrue(dir.exists())
            assertTrue(dir.isDirectory)
        }

    @Test
    fun `given there is no parent directory, when we create a directory with recursive=false, NoParentDirectory error is returned`() =
        runTest {
            val dir = dirInSubDir
            val path = dir.absolutePath

            val result = sut.createDirectory(path, IONFILECreateOptions(recursive = false))

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.CreateFailed.NoParentDirectory)
            assertFalse(dir.exists())
        }

    @Test
    fun `given directory exists, when we create a directory with exclusive=true, AlreadyExists error is returned`() =
        runTest {
            val existingDir = testRootDirectory
            val path = existingDir.absolutePath

            val result =
                sut.createDirectory(path, IONFILECreateOptions(recursive = true))

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.CreateFailed.AlreadyExists)
            assertTrue(existingDir.exists())
        }
    // endregion createDirectory tests

    // region listDirectory tests
    @Test
    fun `given directory is empty, when listing directory, empty list is returned`() = runTest {
        val path = testRootDirectory.absolutePath

        val result = sut.listDirectory(path)

        assertTrue(result.isSuccess)
        assertEquals(emptyList<IONFILEMetadataResult>(), result.getOrNull())
    }

    @Test
    fun `given directory has a file, when listing directory, a one-item list with the file metadata is returned`() =
        runTest {
            val path = testRootDirectory.absolutePath
            val data = "Text"
            IONFILELocalFilesHelper().saveFile(
                fileInRootDir.absolutePath,
                IONFILESaveOptions(
                    data,
                    IONFILEEncoding.DefaultCharset,
                    IONFILESaveMode.WRITE,
                    createFileRecursive = true
                )
            )

            val result = sut.listDirectory(path)

            assertTrue(result.isSuccess)
            result.getOrNull()!!.let {
                assertEquals(1, it.size)
                assertTrue(it.first().type is IONFILEFileType.File)
                assertEquals(data.length.toLong(), it.first().size)
            }
        }

    @Test
    fun `given directory has a sub-directory, when listing directory, a one-item list with the directory metadata is returned`() =
        runTest {
            val path = testRootDirectory.absolutePath
            sut.createDirectory(dirInRootDir.absolutePath, IONFILECreateOptions(recursive = false))

            val result = sut.listDirectory(path)

            assertTrue(result.isSuccess)
            result.getOrNull()!!.let {
                assertEquals(1, it.size)
                assertEquals(DIR_NAME, it.first().name)
                assertTrue(it.first().type is IONFILEFileType.Directory)
            }
        }

    @Test
    fun `given directory several nested directories, when listing directory, a list with the directories metadata is returned`() =
        runTest {
            val path = testRootDirectory.absolutePath
            sut.createDirectory(dirInSubDir.absolutePath, IONFILECreateOptions(recursive = true))
            IONFILELocalFilesHelper().saveFile(
                fileInSubDir.absolutePath,
                IONFILESaveOptions(
                    "File",
                    IONFILEEncoding.DefaultCharset,
                    IONFILESaveMode.WRITE,
                    createFileRecursive = true
                )
            )

            val result = sut.listDirectory(path)

            assertTrue(result.isSuccess)
            result.getOrNull()!!.let {
                assertEquals(2, it.size)
                assertTrue(it.all { item -> item.type is IONFILEFileType.Directory })
            }
        }

    @Test
    fun `given directory does not exist, when listing directory, DoesNotExist error is returned`() =
        runTest {
            val path = dirInSubDir.absolutePath

            val result = sut.listDirectory(path)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.DoesNotExist)
        }
    // endregion listDirectory tests

    // region deleteDirectory tests
    @Test
    fun `given empty directory exists, when we delete it with recursive=false, success is returned`() =
        runTest {
            val dir = dirInRootDir
            val path = dir.absolutePath
            sut.createDirectory(path, IONFILECreateOptions(recursive = false))

            val result = sut.deleteDirectory(path, IONFILEDeleteOptions(recursive = false))

            assertTrue(result.isSuccess)
            assertFalse(dir.exists())
        }

    @Test
    fun `given non-empty directory exists, when we delete it with recursive=true, all children are deleted and success is returned`() =
        runTest {
            val dir = dirInSubDir
            val dirPath = dir.absolutePath
            val dirToDelete = dir.parentFile!!
            sut.createDirectory(dirPath, IONFILECreateOptions(recursive = true))

            val result =
                sut.deleteDirectory(dirToDelete.absolutePath, IONFILEDeleteOptions(true))

            assertTrue(result.isSuccess)
            assertFalse(dirToDelete.exists() && dir.exists())
        }

    @Test
    fun `given non-empty directory exists, when we delete it with recursive=false, CannotDeleteChildren error is returned`() =
        runTest {
            val dir = dirInSubDir
            val dirPath = dir.absolutePath
            val dirToDelete = dir.parentFile!!
            sut.createDirectory(dirPath, IONFILECreateOptions(recursive = true))

            val result = sut.deleteDirectory(dirToDelete.absolutePath, IONFILEDeleteOptions(false))

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.DeleteFailed.CannotDeleteChildren)
            assertTrue(dirToDelete.exists() && dir.exists())
        }

    @Test
    fun `given directory does not exist, when we delete it, DoesNotExist error is returned`() =
        runTest {
            val dir = dirInRootDir
            val path = dir.absolutePath

            val result = sut.deleteDirectory(path, IONFILEDeleteOptions(recursive = true))

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.DoesNotExist)
        }
    // endregion deleteDirectory tests

    // region copyDirectory tests
    @Test
    fun `given empty source directory, when we copy it, success is returned`() = runTest {
        val sourceDir = dirInSubDir
        val sourcePath = sourceDir.absolutePath
        val destinationDir = dirInRootDir
        val destinationPath = destinationDir.absolutePath
        sut.createDirectory(sourcePath, IONFILECreateOptions(recursive = true))

        val result = sut.copyDirectory(sourcePath, destinationPath)

        assertTrue(result.isSuccess)
        assertTrue(destinationDir.exists())
        assertTrue(sourceDir.exists())  // to confirm that source was not deleted when copying
    }

    @Test
    fun `given source directory has children, when we copy it, success is returned`() = runTest {
        val sourceDir = dirInSubDir
        val sourcePath = sourceDir.absolutePath
        val destinationDir = dirInRootDir
        val destinationPath = destinationDir.absolutePath
        IONFILELocalFilesHelper().apply {
            saveFile(
                File(sourceDir, "file1.txt").absolutePath,
                IONFILESaveOptions(
                    "data \nfile 1.",
                    IONFILEEncoding.DefaultCharset,
                    IONFILESaveMode.WRITE,
                    true
                )
            )
            saveFile(
                File(sourceDir, "file2.txt").absolutePath,
                IONFILESaveOptions(
                    "text for file #2.",
                    IONFILEEncoding.DefaultCharset,
                    IONFILESaveMode.WRITE,
                    false
                )
            )
        }
        sut.createDirectory(
            File(sourceDir, "childDirectory").absolutePath,
            IONFILECreateOptions(recursive = true)
        )

        val result = sut.copyDirectory(sourcePath, destinationPath)

        assertTrue(result.isSuccess)
        assertTrue(destinationDir.exists())
        destinationDir.listFiles()?.toList().let { children ->
            assertNotNull(children)
            assertEquals(3, children?.size)
            assertEquals(1, children?.count { it.isDirectory && it.name == "childDirectory" })
            assertEquals(
                1,
                children?.count { it.isFile && it.length() == "data \nfile 1.".length.toLong() }
            )
            assertEquals(
                1,
                children?.count { it.isFile && it.length() == "text for file #2.".length.toLong() }
            )
        }
    }

    @Test
    fun `given source directory does not exist, when we try to copy it, DoesNotExist error is returned`() =
        runTest {
            val sourcePath = dirInRootDir.absolutePath
            val destinationPath = testRootDirectory.absolutePath

            val result = sut.copyDirectory(sourcePath, destinationPath)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.DoesNotExist)
        }

    @Test
    fun `given destination is a file, when we try to copy to it, MixingFilesAndDirectories is returned`() =
        runTest {
            val sourcePath = dirInRootDir.absolutePath
            val destinationPath = fileInRootDir.let {
                it.createNewFile()
                it.absolutePath
            }
            sut.createDirectory(sourcePath, IONFILECreateOptions(recursive = true))

            val result = sut.copyDirectory(sourcePath, destinationPath)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.CopyRenameFailed.MixingFilesAndDirectories)
        }
    // endregion copyDirectory tests

    // region moveDirectory tests
    @Test
    fun `given source directory exists, when we move it, success is returned`() = runTest {
        val sourceDir = dirInSubDir
        val sourcePath = sourceDir.absolutePath
        val destinationDir = dirInRootDir
        val destinationPath = destinationDir.absolutePath
        sut.createDirectory(
            File(sourceDir, "childDirectory").absolutePath,
            IONFILECreateOptions(recursive = true)
        )
        File(sourceDir, "doc.pdf").createNewFile()
        File(sourceDir, "vid.mkv").createNewFile()
        File(sourceDir, "audio.mp3").createNewFile()

        val result = sut.moveDirectory(sourcePath, destinationPath)

        assertTrue(result.isSuccess)
        assertTrue(destinationDir.exists())
        destinationDir.listFiles()?.toList().let { children ->
            assertNotNull(children)
            assertEquals(4, children?.size)
        }
        assertFalse(sourceDir.exists())
    }

    @Test
    fun `given source directory is the same as destination, when we move it, success is returned`() =
        runTest {
            val sourcePath = dirInSubDir.absolutePath

            val result = sut.moveDirectory(sourcePath, sourcePath)

            assertTrue(result.isSuccess)
        }

    @Test
    fun `given destination has no parent directory, when we try to move to it, NoParentDirectory is returned`() =
        runTest {
            val sourcePath = dirInRootDir.absolutePath
            val destinationPath = dirInSubDir.absolutePath // sub-directories not created
            sut.createDirectory(sourcePath, IONFILECreateOptions(recursive = true))

            val result = sut.moveDirectory(sourcePath, destinationPath)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.CopyRenameFailed.NoParentDirectory)
        }

    @Test
    fun `given source is a file, when we try to move it, MixingFilesAndDirectories error is returned`() =
        runTest {
            val sourcePath = fileInRootDir.let {
                it.createNewFile()
                it.absolutePath
            }
            val destinationPath = dirInRootDir.absolutePath

            val result = sut.moveDirectory(sourcePath, destinationPath)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.CopyRenameFailed.MixingFilesAndDirectories)
        }

    @Test
    fun `given destination directory already exists, when we try to move a directory to it, DestinationDirectoryExists error is returned`() =
        runTest {
            val sourcePath = dirInRootDir.absolutePath
            val destinationPath = testRootDirectory.absolutePath
            sut.createDirectory(sourcePath, IONFILECreateOptions(recursive = true))

            val result = sut.moveDirectory(sourcePath, destinationPath)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.CopyRenameFailed.DestinationDirectoryExists)
        }
    // endregion moveDirectory tests
}