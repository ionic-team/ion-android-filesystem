package io.ionic.libs.ionfilesystemlib.controller

import android.os.Build
import io.ionic.libs.ionfilesystemlib.common.IONFLSTBaseJUnitTest
import io.ionic.libs.ionfilesystemlib.controller.internal.IONFLSTBuildConfig
import io.ionic.libs.ionfilesystemlib.model.IONFLSTCreateOptions
import io.ionic.libs.ionfilesystemlib.model.IONFLSTEncoding
import io.ionic.libs.ionfilesystemlib.model.IONFLSTExceptions
import io.ionic.libs.ionfilesystemlib.model.IONFLSTFileType
import io.ionic.libs.ionfilesystemlib.model.IONFLSTReadOptions
import io.ionic.libs.ionfilesystemlib.model.IONFLSTSaveMode
import io.ionic.libs.ionfilesystemlib.model.IONFLSTSaveOptions
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Base64

@OptIn(ExperimentalCoroutinesApi::class)
class IONFLSTLocalFilesHelperTest : IONFLSTBaseJUnitTest() {

    private lateinit var sut: IONFLSTLocalFilesHelper

    override fun additionalSetups() {
        sut = IONFLSTLocalFilesHelper()
    }

    // region createFile tests
    @Test
    fun `given there is a parent directory, when we create a file in that directory, success is returned`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath

            val result =
                sut.createFile(path, IONFLSTCreateOptions(recursive = false, exclusive = false))

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
                sut.createFile(path, IONFLSTCreateOptions(recursive = true, exclusive = false))

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
                sut.createFile(path, IONFLSTCreateOptions(recursive = false, exclusive = false))

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFLSTExceptions.CreateFailed.NoParentDirectory)
            assertFalse(file.exists())
        }
    // endregion createFile tests

    // region save + read file tests
    @Test
    fun `given empty file exists, when saving contents to file as write, success is returned`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath
            sut.createFile(path, IONFLSTCreateOptions(recursive = false, exclusive = false))
            val data = "Write"

            val result = sut.saveFile(
                path,
                IONFLSTSaveOptions(
                    data,
                    encoding = IONFLSTEncoding.DefaultCharset,
                    mode = IONFLSTSaveMode.WRITE,
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
            sut.createFile(path, IONFLSTCreateOptions(recursive = false, exclusive = false))
            val data = "Append"

            val result = sut.saveFile(
                path,
                IONFLSTSaveOptions(
                    data,
                    encoding = IONFLSTEncoding.DefaultCharset,
                    mode = IONFLSTSaveMode.APPEND,
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
            sut.createFile(path, IONFLSTCreateOptions(recursive = false, exclusive = false))
            val dataBase64 =
                Base64.getEncoder().encodeToString("Base 64 w/ special ch4rsª~´".toByteArray())

            val result = sut.saveFile(
                path,
                IONFLSTSaveOptions(
                    dataBase64,
                    encoding = IONFLSTEncoding.Base64,
                    mode = IONFLSTSaveMode.WRITE,
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
            sut.createFile(path, IONFLSTCreateOptions(recursive = false, exclusive = false))

            val result = sut.readFile(path, IONFLSTReadOptions(encoding = IONFLSTEncoding.Default))

            assertTrue(result.isSuccess)
            assertEquals("", result.getOrNull())
        }

    @Test
    fun `given non-empty file exists, when reading file, success is returned with contents`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath
            sut.createFile(path, IONFLSTCreateOptions(recursive = false, exclusive = false))
            val data = "Text"
            sut.saveFile(
                path,
                IONFLSTSaveOptions(data, IONFLSTEncoding.DefaultCharset, IONFLSTSaveMode.WRITE, false)
            )

            val result = sut.readFile(
                path, IONFLSTReadOptions(encoding = IONFLSTEncoding.DefaultCharset)
            )

            assertTrue(result.isSuccess)
            assertEquals("Text", result.getOrNull())
        }

    @Test
    fun `given non-empty file saved as base64, when reading file as utf-8, success is returned with utf-8 string`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath
            sut.createFile(path, IONFLSTCreateOptions(recursive = false, exclusive = false))
            val data = Base64.getEncoder().encodeToString("Base 64 +/ =!\uD83D\uDE00".toByteArray())
            sut.saveFile(
                path,
                IONFLSTSaveOptions(data, IONFLSTEncoding.Base64, IONFLSTSaveMode.WRITE, false)
            )

            val result = sut.readFile(
                path, IONFLSTReadOptions(encoding = IONFLSTEncoding.WithCharset(Charsets.UTF_8))
            )

            assertTrue(result.isSuccess)
            assertEquals("Base 64 +/ =!\uD83D\uDE00", result.getOrNull())
        }

    @Test
    fun `given non-empty file saved as data url with base64, when reading file as utf-8, success is returned with utf-8 string without the prefix`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath
            sut.createFile(path, IONFLSTCreateOptions(recursive = false, exclusive = false))
            val base64 = Base64.getEncoder().encodeToString("Base 64 +/ =!  ".toByteArray())
            val data = "data:textPlain;base64, $base64"
            sut.saveFile(
                path,
                IONFLSTSaveOptions(data, IONFLSTEncoding.Base64, IONFLSTSaveMode.WRITE, false)
            )

            val result = sut.readFile(
                path, IONFLSTReadOptions(encoding = IONFLSTEncoding.WithCharset(Charsets.UTF_8))
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
            sut.createFile(path, IONFLSTCreateOptions(recursive = false, exclusive = false))
            val data = "Lorem ipsum"
            sut.saveFile(
                path,
                IONFLSTSaveOptions(
                    data, IONFLSTEncoding.WithCharset(Charsets.UTF_8), IONFLSTSaveMode.WRITE, false
                )
            )

            val result = sut.readFile(path, IONFLSTReadOptions(encoding = IONFLSTEncoding.Base64))

            assertTrue(result.isSuccess)
            assertEquals("TG9yZW0gaXBzdW0=", result.getOrNull())
        }

    @Test
    fun `given non-empty file exists, when saving contents to file as write, success is returned and original content is gone`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath
            sut.createFile(path, IONFLSTCreateOptions(recursive = false, exclusive = false))
            val originalData = "Original"
            sut.saveFile(
                path,
                IONFLSTSaveOptions(
                    originalData, IONFLSTEncoding.DefaultCharset, IONFLSTSaveMode.WRITE, false
                )
            )
            val newData = "New content"

            val saveResult = sut.saveFile(
                path,
                IONFLSTSaveOptions(
                    newData,
                    encoding = IONFLSTEncoding.DefaultCharset,
                    mode = IONFLSTSaveMode.WRITE,
                    createFileRecursive = false
                )
            )
            val readResult = sut.readFile(path, IONFLSTReadOptions(IONFLSTEncoding.DefaultCharset))

            assertTrue(saveResult.isSuccess)
            assertEquals("New content", readResult.getOrNull())
        }

    @Test
    fun `given non-empty file exists, when saving contents to file as append, success is returned and original content remains`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath
            sut.createFile(path, IONFLSTCreateOptions(recursive = false, exclusive = false))
            val originalData = "Original"
            sut.saveFile(
                path,
                IONFLSTSaveOptions(
                    originalData, IONFLSTEncoding.DefaultCharset, IONFLSTSaveMode.WRITE, false
                )
            )
            val newData = "\n\n\t-> New content"

            val saveResult = sut.saveFile(
                path,
                IONFLSTSaveOptions(
                    newData,
                    encoding = IONFLSTEncoding.DefaultCharset,
                    mode = IONFLSTSaveMode.APPEND,
                    createFileRecursive = false
                )
            )
            val readResult = sut.readFile(path, IONFLSTReadOptions(IONFLSTEncoding.DefaultCharset))

            assertTrue(saveResult.isSuccess)
            assertEquals(originalData + newData, readResult.getOrNull())
        }

    @Test
    fun `given file does not exist, when saving contents to file with createFileRecursive=false, success is returned`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath

            val result = sut.saveFile(
                path,
                IONFLSTSaveOptions(
                    "any data...",
                    encoding = IONFLSTEncoding.DefaultCharset,
                    mode = IONFLSTSaveMode.WRITE,
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
                path,
                IONFLSTSaveOptions(
                    "any data...",
                    encoding = IONFLSTEncoding.DefaultCharset,
                    mode = IONFLSTSaveMode.WRITE,
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
                path,
                IONFLSTSaveOptions(
                    "any data...",
                    encoding = IONFLSTEncoding.DefaultCharset,
                    mode = IONFLSTSaveMode.WRITE,
                    createFileRecursive = null
                )
            )

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFLSTExceptions.DoesNotExist)
        }

    @Test
    fun `given nor file nor parent directories exist, when saving contents to file with createFileRecursive=false, error is returned`() =
        runTest {
            val file = fileInSubDir
            val path = file.absolutePath

            val result = sut.saveFile(
                path,
                IONFLSTSaveOptions(
                    "any data...",
                    encoding = IONFLSTEncoding.DefaultCharset,
                    mode = IONFLSTSaveMode.WRITE,
                    createFileRecursive = false
                )
            )

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFLSTExceptions.CreateFailed.NoParentDirectory)
        }

    @Test
    fun `given file does not exist, when reading file, DoesNotExist error is returned`() = runTest {
        val file = fileInRootDir
        val path = file.absolutePath

        val result = sut.readFile(path, IONFLSTReadOptions(encoding = IONFLSTEncoding.Default))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IONFLSTExceptions.DoesNotExist)
    }

    @Test
    fun `given there is a directory, when trying to save it as a file, error is returned`() =
        runTest {
            val dir = testRootDirectory
            val path = dir.absolutePath


            val result = sut.saveFile(
                path,
                IONFLSTSaveOptions(
                    "any data...",
                    // this would result in an error because the provided data is not base64
                    //  however because we are providing a directory, the method should return before base64 conversion
                    encoding = IONFLSTEncoding.Base64,
                    mode = IONFLSTSaveMode.WRITE,
                    createFileRecursive = true
                )
            )

            // error is thrown on java.io side, exception is returned as-is
            assertTrue(result.isFailure)
        }

    @Test
    fun `given there is a directory, when trying to read it as a file, error is returned`() =
        runTest {
            val dir = testRootDirectory
            val path = dir.absolutePath

            val result = sut.readFile(path, IONFLSTReadOptions(IONFLSTEncoding.Default))

            // error is thrown on java.io side, exception is returned as-is
            assertTrue(result.isFailure)
        }
    // endregion save + read file tests

    // region fileMetadata tests
    @Test
    fun `given empty file exists, when getting file metadata, the correct information is returned`() =
        runTest {
            mockkMimeTypeMap(TEXT_MIME_TYPE)
            val path = fileInRootDir.absolutePath
            sut.createFile(path, IONFLSTCreateOptions(recursive = false, exclusive = false))

            val result = sut.getFileMetadata(path)

            assertTrue(result.isSuccess)
            result.getOrNull()!!.let {
                assertEquals(path, it.fullPath)
                assertEquals(FILE_NAME_TXT, it.name)
                assertEquals(0, it.size)
                assertEquals(IONFLSTFileType.File(TEXT_MIME_TYPE), it.type)
            }
        }

    @Test
    fun `given empty directory exists, when getting file metadata, the correct information is returned`() =
        runTest {
            val path = testRootDirectory.absolutePath

            val result = sut.getFileMetadata(path)

            assertTrue(result.isSuccess)
            result.getOrNull()!!.let {
                assertEquals(path, it.fullPath)
                assertEquals(ROOT_DIR_NAME, it.name)
                assertEquals(IONFLSTFileType.Directory, it.type)
            }
        }

    @Test
    fun `given non-empty file exists, when getting file metadata, the correct size is returned`() =
        runTest {
            val path = fileInRootDir.absolutePath
            val plainTextData = "Text"
            sut.saveFile(
                path,
                IONFLSTSaveOptions(
                    data = plainTextData,
                    encoding = IONFLSTEncoding.WithCharset(Charsets.UTF_8),
                    mode = IONFLSTSaveMode.WRITE,
                    createFileRecursive = true
                )
            )

            val result = sut.getFileMetadata(path)

            assertTrue(result.isSuccess)
            assertEquals(plainTextData.length.toLong(), result.getOrNull()?.size)
        }

    @Test
    fun `given file is updated after creation, when getting file metadata, the lastModifiedTimestamp is more recent than the created`() =
        runTest {
            val path = fileInRootDir.absolutePath
            sut.createFile(path, IONFLSTCreateOptions(recursive = false, exclusive = false))
            testScheduler.advanceTimeBy(60_000L)
            sut.saveFile(
                path,
                IONFLSTSaveOptions(
                    data = "1",
                    encoding = IONFLSTEncoding.DefaultCharset,
                    mode = IONFLSTSaveMode.WRITE,
                    createFileRecursive = false
                )
            )

            val result = sut.getFileMetadata(path)

            assertTrue(result.isSuccess)
            result.getOrNull()!!.let {
                assertTrue(it.lastModifiedTimestamp > it.createdTimestamp)
            }
        }

    @Test
    fun `given Android version below 26, when getting file metadata, createdTimestamp is zero`() =
        runTest {
            val path = fileInRootDir.absolutePath
            every { IONFLSTBuildConfig.getAndroidSdkVersionCode() } returns Build.VERSION_CODES.N
            sut.createFile(path, IONFLSTCreateOptions(recursive = false, exclusive = false))

            val result = sut.getFileMetadata(path)

            assertTrue(result.isSuccess)
            assertEquals(0L, result.getOrNull()?.createdTimestamp)
        }

    @Test
    fun `given 3ga file, when getting file metadata, type is File with audio 3gpp mimeType`() =
        runTest {
            val path = File(testRootDirectory, "audio_file.3ga").absolutePath
            mockkMimeTypeMap(null)
            sut.createFile(path, IONFLSTCreateOptions(recursive = false, exclusive = false))

            val result = sut.getFileMetadata(path)

            assertTrue(result.isSuccess)
            assertEquals(IONFLSTFileType.File(mimeType = "audio/3gpp"), result.getOrNull()?.type)
        }

    @Test
    fun `given js file, when getting file metadata, type is File with text javascript mimeType`() =
        runTest {
            val path = File(testRootDirectory, "code.js").absolutePath
            mockkMimeTypeMap(null)
            sut.createFile(path, IONFLSTCreateOptions(recursive = false, exclusive = false))

            val result = sut.getFileMetadata(path)

            assertTrue(result.isSuccess)
            assertEquals(
                IONFLSTFileType.File(mimeType = "text/javascript"),
                result.getOrNull()?.type
            )
        }

    @Test
    fun `given unknown mimetype, when getting file metadata, type is File with fallback mimeType`() =
        runTest {
            val path = File(testRootDirectory, "fileWithoutExtension").absolutePath
            mockkMimeTypeMap(null)
            sut.createFile(path, IONFLSTCreateOptions(recursive = false, exclusive = false))

            val result = sut.getFileMetadata(path)

            assertTrue(result.isSuccess)
            assertEquals(
                IONFLSTFileType.File(mimeType = "application/octet-binary"),
                result.getOrNull()?.type
            )
        }

    @Test
    fun `given file does not exist, when getting file metadata, DoesNotExist error is returned`() =
        runTest {
            val path = fileInRootDir.absolutePath

            val result = sut.getFileMetadata(path)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFLSTExceptions.DoesNotExist)
        }
    // endregion fileMetadata sets

    // region deleteFile tests
    @Test
    fun `given file exists, when we delete it, success is returned`() = runTest {
        val file = fileInRootDir
        val path = file.absolutePath
        sut.createFile(path, IONFLSTCreateOptions(recursive = false, exclusive = false))

        val result = sut.deleteFile(path)

        assertTrue(result.isSuccess)
        assertFalse(file.exists())
    }

    @Test
    fun `given file does not exist, when we delete it, DoesNotExist error is returned`() = runTest {
        val file = fileInRootDir
        val path = file.absolutePath

        val result = sut.deleteFile(path)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IONFLSTExceptions.DoesNotExist)
    }
    // endregion deleteFile tests
}