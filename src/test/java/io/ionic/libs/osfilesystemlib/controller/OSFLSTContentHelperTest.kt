package io.ionic.libs.osfilesystemlib.controller

import android.net.Uri
import io.ionic.libs.osfilesystemlib.common.IMAGE_FILE_CONTENT
import io.ionic.libs.osfilesystemlib.common.IMAGE_FILE_NAME
import io.ionic.libs.osfilesystemlib.common.OSFLSTBaseTest
import io.ionic.libs.osfilesystemlib.common.OSFLSTTestFileContentProvider
import io.ionic.libs.osfilesystemlib.common.TEST_CONTENT_PROVIDER_NAME
import io.ionic.libs.osfilesystemlib.common.TEST_TIMESTAMP
import io.ionic.libs.osfilesystemlib.common.TEXT_FILE_CONTENT
import io.ionic.libs.osfilesystemlib.common.TEXT_FILE_NAME
import io.ionic.libs.osfilesystemlib.model.OSFLSTEncoding
import io.ionic.libs.osfilesystemlib.model.OSFLSTExceptions
import io.ionic.libs.osfilesystemlib.model.OSFLSTFileType
import io.ionic.libs.osfilesystemlib.model.OSFLSTMetadataResult
import io.ionic.libs.osfilesystemlib.model.OSFLSTReadOptions
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
class OSFLSTContentHelperTest {

    private lateinit var contentProvider: OSFLSTTestFileContentProvider
    private lateinit var sut: OSFLSTContentHelper

    @Before
    fun setUp() {
        val contentResolver = RuntimeEnvironment.getApplication().contentResolver
        contentProvider = Robolectric.setupContentProvider(
            OSFLSTTestFileContentProvider::class.java,
            TEST_CONTENT_PROVIDER_NAME
        )
        sut = OSFLSTContentHelper(contentResolver)
    }

    @After
    fun tearDown() {
        contentProvider.cleanup()
    }

    // region readFile tests
    @Test
    fun `given text file, when reading from its content uri, success is returned`() = runTest {
        val uri =
            OSFLSTBaseTest.fileUriWithEncodings("content://$TEST_CONTENT_PROVIDER_NAME/$TEXT_FILE_NAME")

        val result = sut.readFile(uri, OSFLSTReadOptions(OSFLSTEncoding.DefaultCharset))

        assertTrue(result.isSuccess)
        assertEquals(TEXT_FILE_CONTENT, result.getOrNull())
    }

    @Test
    fun `given image file, when reading from its content uri as Base64, success is returned`() =
        runTest {
            val uri = Uri.parse("content://$TEST_CONTENT_PROVIDER_NAME/$IMAGE_FILE_NAME")

            val result = sut.readFile(uri, OSFLSTReadOptions(OSFLSTEncoding.Base64))

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

            val result = sut.readFile(uri, OSFLSTReadOptions(OSFLSTEncoding.DefaultCharset))

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is OSFLSTExceptions.DoesNotExist)
        }
    // endregion readFile tests

    // region getFileMetadata tests
    @Test
    fun `given text file, when getting metadata from content uri, success is returned`() = runTest {
        val uri =
            OSFLSTBaseTest.fileUriWithEncodings("content://$TEST_CONTENT_PROVIDER_NAME/$TEXT_FILE_NAME")

        val result = sut.getFileMetadata(uri)

        assertTrue(result.isSuccess)
        assertEquals(
            OSFLSTMetadataResult(
                fullPath = uri.path ?: "",
                name = "$TEXT_FILE_NAME.txt",
                size = TEXT_FILE_CONTENT.length.toLong(),
                type = OSFLSTFileType.File("application/text"),
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
                OSFLSTMetadataResult(
                    fullPath = uri.path ?: "",
                    name = "$IMAGE_FILE_NAME.jpeg",
                    size = IMAGE_FILE_CONTENT.length.toLong(),
                    type = OSFLSTFileType.File("image/jpeg"),
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
            assertTrue(result.exceptionOrNull() is OSFLSTExceptions.DoesNotExist)
        }
    // endregion getFileMetadata tests

    // region deleteFile tests
    @Test
    fun `given file exists and allows for deletion, when deleting it via content uri, success is returned`() =
        runTest {
            val uri =
                OSFLSTBaseTest.fileUriWithEncodings("content://$TEST_CONTENT_PROVIDER_NAME/$TEXT_FILE_NAME")

            val result = sut.deleteFile(uri)

            assertTrue(result.isSuccess)
        }

    @Test
    fun `given file does not exist, when deleting it via content uri, Unknown error is returned`() =
        runTest {
            val uri = Uri.parse("content://$TEST_CONTENT_PROVIDER_NAME/fileThatDoesNotExist")

            val result = sut.deleteFile(uri)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is OSFLSTExceptions.DeleteFailed.Unknown)
        }
    // endregion deleteFile tests
}