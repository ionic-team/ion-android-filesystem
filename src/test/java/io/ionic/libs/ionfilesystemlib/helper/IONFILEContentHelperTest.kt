package io.ionic.libs.ionfilesystemlib.helper

import android.net.Uri
import app.cash.turbine.test
import io.ionic.libs.ionfilesystemlib.common.IMAGE_FILE_CONTENT
import io.ionic.libs.ionfilesystemlib.common.IMAGE_FILE_NAME
import io.ionic.libs.ionfilesystemlib.common.IONFILETestFileContentProvider
import io.ionic.libs.ionfilesystemlib.common.LOREM_IPSUM_2800_CHARS
import io.ionic.libs.ionfilesystemlib.common.TEST_CONTENT_PROVIDER_NAME
import io.ionic.libs.ionfilesystemlib.common.TEST_TIMESTAMP
import io.ionic.libs.ionfilesystemlib.common.TEXT_FILE_CONTENT
import io.ionic.libs.ionfilesystemlib.common.TEXT_FILE_NAME
import io.ionic.libs.ionfilesystemlib.common.fileUriWithEncodings
import io.ionic.libs.ionfilesystemlib.model.IONFILEEncoding
import io.ionic.libs.ionfilesystemlib.model.IONFILEExceptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEFileType
import io.ionic.libs.ionfilesystemlib.model.IONFILEMetadataResult
import io.ionic.libs.ionfilesystemlib.model.IONFILEReadByChunksOptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEReadOptions
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.util.Base64
import kotlin.math.ceil

@RunWith(RobolectricTestRunner::class)
class IONFILEContentHelperTest {

    private val context get() = RuntimeEnvironment.getApplication().applicationContext
    private lateinit var contentProvider: IONFILETestFileContentProvider
    private lateinit var sut: IONFILEContentHelper

    @Before
    fun setUp() {
        val contentResolver = context.contentResolver
        contentProvider = Robolectric.setupContentProvider(
            IONFILETestFileContentProvider::class.java,
            TEST_CONTENT_PROVIDER_NAME
        )
        sut = IONFILEContentHelper(contentResolver)
    }

    @After
    fun tearDown() {
        contentProvider.cleanup()
    }

    // region readFile tests
    @Test
    fun `given text file, when reading from its content uri, success is returned`() = runTest {
        val uri = fileUriWithEncodings("content://$TEST_CONTENT_PROVIDER_NAME/$TEXT_FILE_NAME")

        val result = sut.readFile(uri, IONFILEReadOptions(IONFILEEncoding.DefaultCharset))

        assertTrue(result.isSuccess)
        assertEquals(TEXT_FILE_CONTENT, result.getOrNull())
    }

    @Test
    fun `given image file, when reading from its content uri as Base64, success is returned`() =
        runTest {
            val uri = Uri.parse("content://$TEST_CONTENT_PROVIDER_NAME/$IMAGE_FILE_NAME")

            val result = sut.readFile(uri, IONFILEReadOptions(IONFILEEncoding.Base64))

            assertTrue(result.isSuccess)
            assertEquals(
                Base64.getEncoder().encodeToString(IMAGE_FILE_CONTENT.toByteArray()),
                result.getOrNull()
            )
        }

    @Test
    fun `given non-existent file, when reading from its content uri, DoesNotExist error is returned`() =
        runTest {
            val uri = Uri.parse("content://$TEST_CONTENT_PROVIDER_NAME/fileThatDoesNotExist")

            val result = sut.readFile(uri, IONFILEReadOptions(IONFILEEncoding.DefaultCharset))

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.DoesNotExist)
        }
    // endregion readFile tests

    // region readFileByChunks tests
    @Test
    fun `given file has content, when reading with a very large chunk size, content is emitted once`() =
        runTest {
            val uri = fileUriWithEncodings("content://$TEST_CONTENT_PROVIDER_NAME/$IMAGE_FILE_NAME")

            sut.readFileByChunks(
                uri, IONFILEReadByChunksOptions(IONFILEEncoding.DefaultCharset, Int.MAX_VALUE)
            ).test {

                assertEquals(IMAGE_FILE_CONTENT, awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `given large file, when reading in chunks in base64, multiple items are returned and concatenated result is correct`() =
        runTest {
            val fileName = "newFile"
            val data = LOREM_IPSUM_2800_CHARS.repeat(2000)  // > 5 MB of text
            val chunkSize = 50000
            contentProvider.addToProvider(
                IONFILETestFileContentProvider.TestFileContent(fileName, data, mimeType = null)
            )
            val uri = fileUriWithEncodings("content://$TEST_CONTENT_PROVIDER_NAME/$fileName")
            val chunkCount: Int = ceil(data.length.toFloat() / chunkSize).toInt()
            var result = ""

            sut.readFileByChunks(
                uri, IONFILEReadByChunksOptions(IONFILEEncoding.Base64, chunkSize)
            ).test {
                for (index in 1..chunkCount) {
                    val chunk = awaitItem()
                    result += chunk
                }
                awaitComplete()
            }

            assertEquals(
                data,
                String(Base64.getDecoder().decode(result))
            )
        }

    @Test
    fun `given non-existent file, when reading from its content uri in chunks, DoesNotExist error is returned`() =
        runTest {
            val uri = Uri.parse("content://$TEST_CONTENT_PROVIDER_NAME/fileThatDoesNotExist")

            sut.readFileByChunks(uri, IONFILEReadByChunksOptions(IONFILEEncoding.Base64, 1))
                .test {
                    val error = awaitError()

                    assertTrue(error is IONFILEExceptions.DoesNotExist)
                }
        }
    // endregion readFileByChunks tests

    // region getFileMetadata tests
    @Test
    fun `given text file, when getting metadata from content uri, success is returned`() = runTest {
        val uri = fileUriWithEncodings("content://$TEST_CONTENT_PROVIDER_NAME/$TEXT_FILE_NAME")

        val result = sut.getFileMetadata(uri)

        assertTrue(result.isSuccess)
        assertEquals(
            IONFILEMetadataResult(
                fullPath = uri.path ?: "",
                name = "$TEXT_FILE_NAME.txt",
                uri = uri,
                size = TEXT_FILE_CONTENT.length.toLong(),
                type = IONFILEFileType.File("application/text"),
                createdTimestamp = TEST_TIMESTAMP,
                lastModifiedTimestamp = TEST_TIMESTAMP
            ),
            result.getOrNull()
        )
    }

    @Test
    fun `given image file, when getting metadata from content uri, success is returned`() =
        runTest {
            val uri = Uri.parse("content://$TEST_CONTENT_PROVIDER_NAME/$IMAGE_FILE_NAME")

            val result = sut.getFileMetadata(uri)

            assertTrue(result.isSuccess)
            assertEquals(
                IONFILEMetadataResult(
                    fullPath = uri.path ?: "",
                    name = "$IMAGE_FILE_NAME.jpeg",
                    uri = uri,
                    size = IMAGE_FILE_CONTENT.length.toLong(),
                    type = IONFILEFileType.File("image/jpeg"),
                    createdTimestamp = TEST_TIMESTAMP,
                    lastModifiedTimestamp = TEST_TIMESTAMP
                ),
                result.getOrNull()
            )
        }

    @Test
    fun `given non-existent file, when getting metadata from content uri, DoesNotExist error is returned`() =
        runTest {
            val uri = Uri.parse("content://$TEST_CONTENT_PROVIDER_NAME/fileThatDoesNotExist")

            val result = sut.getFileMetadata(uri)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.DoesNotExist)
        }
    // endregion getFileMetadata tests

    // region deleteFile tests
    @Test
    fun `given file exists and allows for deletion, when deleting it via content uri, success is returned`() =
        runTest {
            val uri = fileUriWithEncodings("content://$TEST_CONTENT_PROVIDER_NAME/$TEXT_FILE_NAME")

            val result = sut.deleteFile(uri)

            assertTrue(result.isSuccess)
        }

    @Test
    fun `given file does not exist, when deleting it via content uri, UnknownError is returned`() =
        runTest {
            val uri = Uri.parse("content://$TEST_CONTENT_PROVIDER_NAME/fileThatDoesNotExist")

            val result = sut.deleteFile(uri)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.UnknownError)
        }
    // endregion deleteFile tests

    // region copyFile tests
    @Test
    fun `given file exists, when copying to local file, success is returned`() = runTest {
        val sourceUri = Uri.parse("content://$TEST_CONTENT_PROVIDER_NAME/$IMAGE_FILE_NAME")
        val destinationFile = File(context.filesDir, "newFile.jpeg")

        val result = sut.copyFile(sourceUri, destinationFile.absolutePath)

        assertTrue(result.isSuccess)
        assertEquals(IMAGE_FILE_CONTENT.toByteArray().size.toLong(), destinationFile.length())
    }

    @Test
    fun `given source file does not exist, when trying to copy it, DoesNotExist error is returned`() =
        runTest {
            val sourceUri = Uri.parse("content://$TEST_CONTENT_PROVIDER_NAME/doesNotExist")
            val destinationFile = File(context.cacheDir, "newFile")

            val result = sut.copyFile(sourceUri, destinationFile.absolutePath)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.DoesNotExist)
        }

    @Test
    fun `given destination is a directory, when trying to copy it, MixingFilesAndDirectories error is returned`() =
        runTest {
            val sourceUri = Uri.parse("content://$TEST_CONTENT_PROVIDER_NAME/doesNotExist")
            val destinationPath = context.cacheDir.absolutePath

            val result = sut.copyFile(sourceUri, destinationPath)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.CopyRenameFailed.MixingFilesAndDirectories)
        }

    @Test
    fun `given destination has no parent directory, when trying to copy it, NoParentDirectory is returned`() =
        runTest {
            val sourceUri =
                fileUriWithEncodings("content://$TEST_CONTENT_PROVIDER_NAME/$TEXT_FILE_NAME")
            val destinationPath = File(context.externalCacheDir, "nonExistingDir/file").absolutePath

            val result = sut.copyFile(sourceUri, destinationPath)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.CopyRenameFailed.NoParentDirectory)
        }
    // endregion copyFile tests
}