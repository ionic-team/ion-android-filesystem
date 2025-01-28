package io.ionic.libs.ionfilesystemlib.controller

import android.net.Uri
import io.ionic.libs.ionfilesystemlib.common.IMAGE_FILE_CONTENT
import io.ionic.libs.ionfilesystemlib.common.IMAGE_FILE_NAME
import io.ionic.libs.ionfilesystemlib.common.IONFLSTBaseTest
import io.ionic.libs.ionfilesystemlib.common.IONFLSTTestFileContentProvider
import io.ionic.libs.ionfilesystemlib.common.TEST_CONTENT_PROVIDER_NAME
import io.ionic.libs.ionfilesystemlib.common.TEST_TIMESTAMP
import io.ionic.libs.ionfilesystemlib.common.TEXT_FILE_CONTENT
import io.ionic.libs.ionfilesystemlib.common.TEXT_FILE_NAME
import io.ionic.libs.ionfilesystemlib.model.IONFLSTEncoding
import io.ionic.libs.ionfilesystemlib.model.IONFLSTExceptions
import io.ionic.libs.ionfilesystemlib.model.IONFLSTFileType
import io.ionic.libs.ionfilesystemlib.model.IONFLSTMetadataResult
import io.ionic.libs.ionfilesystemlib.model.IONFLSTReadOptions
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
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
class IONFLSTContentHelperTest {

    private lateinit var contentProvider: IONFLSTTestFileContentProvider
    private lateinit var sut: IONFLSTContentHelper

    @Before
    fun setUp() {
        val contentResolver = RuntimeEnvironment.getApplication().contentResolver
        contentProvider = Robolectric.setupContentProvider(
            IONFLSTTestFileContentProvider::class.java,
            TEST_CONTENT_PROVIDER_NAME
        )
        sut = IONFLSTContentHelper(contentResolver)
    }

    @After
    fun tearDown() {
        contentProvider.cleanup()
    }

    // region readFile tests
    @Test
    fun `given text file, when reading from its content uri, success is returned`() = runTest {
        val uri =
            IONFLSTBaseTest.fileUriWithEncodings("content://$TEST_CONTENT_PROVIDER_NAME/$TEXT_FILE_NAME")

        val result = sut.readFile(uri, IONFLSTReadOptions(IONFLSTEncoding.DefaultCharset))

        assertTrue(result.isSuccess)
        assertEquals(TEXT_FILE_CONTENT, result.getOrNull())
    }

    @Test
    fun `given image file, when reading from its content uri as Base64, success is returned`() =
        runTest {
            val uri = Uri.parse("content://$TEST_CONTENT_PROVIDER_NAME/$IMAGE_FILE_NAME")

            val result = sut.readFile(uri, IONFLSTReadOptions(IONFLSTEncoding.Base64))

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

            val result = sut.readFile(uri, IONFLSTReadOptions(IONFLSTEncoding.DefaultCharset))

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFLSTExceptions.DoesNotExist)
        }
    // endregion readFile tests

    // region getFileMetadata tests
    @Test
    fun `given text file, when getting metadata from content uri, success is returned`() = runTest {
        val uri =
            IONFLSTBaseTest.fileUriWithEncodings("content://$TEST_CONTENT_PROVIDER_NAME/$TEXT_FILE_NAME")

        val result = sut.getFileMetadata(uri)

        assertTrue(result.isSuccess)
        assertEquals(
            IONFLSTMetadataResult(
                fullPath = uri.path ?: "",
                name = "$TEXT_FILE_NAME.txt",
                size = TEXT_FILE_CONTENT.length.toLong(),
                type = IONFLSTFileType.File("application/text"),
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
                IONFLSTMetadataResult(
                    fullPath = uri.path ?: "",
                    name = "$IMAGE_FILE_NAME.jpeg",
                    size = IMAGE_FILE_CONTENT.length.toLong(),
                    type = IONFLSTFileType.File("image/jpeg"),
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
            assertTrue(result.exceptionOrNull() is IONFLSTExceptions.DoesNotExist)
        }
    // endregion getFileMetadata tests

    // region deleteFile tests
    @Test
    fun `given file exists and allows for deletion, when deleting it via content uri, success is returned`() =
        runTest {
            val uri =
                IONFLSTBaseTest.fileUriWithEncodings("content://$TEST_CONTENT_PROVIDER_NAME/$TEXT_FILE_NAME")

            val result = sut.deleteFile(uri)

            assertTrue(result.isSuccess)
        }

    @Test
    fun `given file does not exist, when deleting it via content uri, Unknown error is returned`() =
        runTest {
            val uri = Uri.parse("content://$TEST_CONTENT_PROVIDER_NAME/fileThatDoesNotExist")

            val result = sut.deleteFile(uri)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFLSTExceptions.DeleteFailed.Unknown)
        }
    // endregion deleteFile tests
}