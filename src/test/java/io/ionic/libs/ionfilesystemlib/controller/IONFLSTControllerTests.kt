package io.ionic.libs.ionfilesystemlib.controller

import android.net.Uri
import app.cash.turbine.test
import io.ionic.libs.ionfilesystemlib.IONFLSTController
import io.ionic.libs.ionfilesystemlib.common.IMAGE_FILE_CONTENT
import io.ionic.libs.ionfilesystemlib.common.IMAGE_FILE_NAME
import io.ionic.libs.ionfilesystemlib.common.IONFLSTTestFileContentProvider
import io.ionic.libs.ionfilesystemlib.common.LOREM_IPSUM_2800_CHARS
import io.ionic.libs.ionfilesystemlib.common.TEST_CONTENT_PROVIDER_NAME
import io.ionic.libs.ionfilesystemlib.common.TEXT_FILE_CONTENT
import io.ionic.libs.ionfilesystemlib.common.TEXT_FILE_NAME
import io.ionic.libs.ionfilesystemlib.model.IONFLSTCreateOptions
import io.ionic.libs.ionfilesystemlib.model.IONFLSTDeleteOptions
import io.ionic.libs.ionfilesystemlib.model.IONFLSTEncoding
import io.ionic.libs.ionfilesystemlib.model.IONFLSTExceptions
import io.ionic.libs.ionfilesystemlib.model.IONFLSTFileType
import io.ionic.libs.ionfilesystemlib.model.IONFLSTFolderType
import io.ionic.libs.ionfilesystemlib.model.IONFLSTReadByChunksOptions
import io.ionic.libs.ionfilesystemlib.model.IONFLSTReadOptions
import io.ionic.libs.ionfilesystemlib.model.IONFLSTSaveMode
import io.ionic.libs.ionfilesystemlib.model.IONFLSTSaveOptions
import io.ionic.libs.ionfilesystemlib.model.IONFLSTUri
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.math.ceil

/**
 * Tests for the [IONFLSTController]
 *
 * These tests are not 100% exhaustive of the entire library.
 * That is because most of the logic is covered in IONFLST(...)HelperTest classes
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class IONFLSTControllerTests {

    private val context get() = RuntimeEnvironment.getApplication()
    private lateinit var contentProvider: IONFLSTTestFileContentProvider
    private lateinit var sut: IONFLSTController

    @Before
    fun setUp() {
        contentProvider = Robolectric.setupContentProvider(
            IONFLSTTestFileContentProvider::class.java,
            TEST_CONTENT_PROVIDER_NAME
        )
        sut = IONFLSTController(context)
    }

    @After
    fun tearDown() {
        contentProvider.cleanup()
    }

    // region happy path tests
    @Test
    fun `given local file path, when creating it, success with file uri is returned`() = runTest {
        val uriLocalFile = IONFLSTUri.Unresolved(
            parentFolder = IONFLSTFolderType.INTERNAL_FILES,
            "fileToCreate.txt"
        )

        val result = sut.createFile(uriLocalFile, IONFLSTCreateOptions(recursive = false))

        assertTrue(result.isSuccess)
        assertEquals(
            Uri.parse("file://${context.filesDir.absolutePath}/fileToCreate.txt"),
            result.getOrNull()
        )
    }

    @Test
    fun `given local directory path, when creating it, success with directory uri is returned`() =
        runTest {
            val uriLocalFile = IONFLSTUri.Unresolved(
                parentFolder = IONFLSTFolderType.EXTERNAL_FILES,
                "subDir1/subDir2/directory"
            )

            val result = sut.createDirectory(uriLocalFile, IONFLSTCreateOptions(recursive = true))

            assertTrue(result.isSuccess)
            assertEquals(
                Uri.parse("file://${context.getExternalFilesDir(null)!!.absolutePath}/subDir1/subDir2/directory"),
                result.getOrNull()
            )
        }

    @Test
    fun `given local file path saved, when reading it, success with file contents is returned`() =
        runTest {
            // save file for it to have contents to be read
            val uriLocalFile = IONFLSTUri.Unresolved(
                parentFolder = IONFLSTFolderType.INTERNAL_FILES,
                "fileToCreate.txt"
            )
            sut.saveFile(
                uriLocalFile,
                IONFLSTSaveOptions(
                    data = "Some\ntext\n$#%#$&>xj93w5ts\n\uD83D\uDC35 \uD83D\uDE1F ❎ \uD83D\uDCC1",
                    encoding = IONFLSTEncoding.DefaultCharset,
                    mode = IONFLSTSaveMode.WRITE,
                    createFileRecursive = true
                )
            ).let { assertTrue(it.isSuccess) }

            val result =
                sut.readFile(uriLocalFile, IONFLSTReadOptions(IONFLSTEncoding.DefaultCharset))

            assertTrue(result.isSuccess)
            assertEquals(
                "Some\ntext\n$#%#$&>xj93w5ts\n\uD83D\uDC35 \uD83D\uDE1F ❎ \uD83D\uDCC1",
                result.getOrNull()
            )
        }

    @Test
    fun `given file in content provider, when reading it, success with file contents is returned`() =
        runTest {
            val uriContentScheme = IONFLSTUri.Unresolved(
                null,
                "content://$TEST_CONTENT_PROVIDER_NAME/$IMAGE_FILE_NAME"
            )

            val result = sut.readFile(uriContentScheme, IONFLSTReadOptions(IONFLSTEncoding.Base64))

            assertTrue(result.isSuccess)
            assertEquals(
                Base64.getEncoder().encodeToString(IMAGE_FILE_CONTENT.toByteArray()),
                result.getOrNull()
            )
        }

    @Test
    fun `given large local file exists, when reading it by chunks, chunks are emitted and concatenated result is  correct`() =
        runTest {
            // save file for it to have contents to be read
            val uriLocalFile = IONFLSTUri.Unresolved(
                parentFolder = IONFLSTFolderType.INTERNAL_FILES,
                "largeFile.txt"
            )
            val data = LOREM_IPSUM_2800_CHARS.repeat(4000) // over 10 MB of text
            sut.saveFile(
                uriLocalFile,
                IONFLSTSaveOptions(
                    data = data,
                    encoding = IONFLSTEncoding.DefaultCharset,
                    mode = IONFLSTSaveMode.WRITE,
                    createFileRecursive = true
                )
            ).let { assertTrue(it.isSuccess) }
            val chunkSize = 256 * 1024
            val numberOfChunks = ceil(data.length.toFloat() / chunkSize).toInt()
            var result = ""

            sut.readFileByChunks(
                uriLocalFile,
                IONFLSTReadByChunksOptions(IONFLSTEncoding.DefaultCharset, chunkSize)
            ).test {
                (1..numberOfChunks).forEach { index ->
                    val chunk = awaitItem()
                    if (index < numberOfChunks) {
                        assertEquals(chunkSize, chunk.length)
                    }
                    result += chunk
                }
                awaitComplete()
            }

            assertEquals(data, result)
        }

    @Test
    fun `given file in content provider, when reading it with large chunk, file contents emitted in a single item`() =
        runTest {
            val uriContentScheme = IONFLSTUri.Unresolved(
                null,
                "content://$TEST_CONTENT_PROVIDER_NAME/$IMAGE_FILE_NAME"
            )

            sut.readFileByChunks(
                uriContentScheme,
                IONFLSTReadByChunksOptions(IONFLSTEncoding.Base64, Int.MAX_VALUE)
            ).test {
                val result = awaitItem()

                assertEquals(
                    Base64.getEncoder().encodeToString(IMAGE_FILE_CONTENT.toByteArray()),
                    result
                )
                awaitComplete()
            }
        }

    @Test
    fun `given directory file path, when getting metadata, success is returned`() = runTest {
        val uriCacheDir = IONFLSTUri.Unresolved(IONFLSTFolderType.INTERNAL_CACHE, "")

        val result = sut.getMetadata(uriCacheDir)

        assertTrue(result.isSuccess)
        result.getOrNull().let {
            assertEquals(context.cacheDir.absolutePath, it?.fullPath)
            assertEquals(context.cacheDir.name, it?.name)
            assertEquals(IONFLSTFileType.Directory, it?.type)
        }
    }

    @Test
    fun `given file in content provider, when getting metadata, success is returned`() = runTest {
        val uriContentScheme =
            IONFLSTUri.Unresolved(null, "content://$TEST_CONTENT_PROVIDER_NAME/$TEXT_FILE_NAME")

        val result = sut.getMetadata(uriContentScheme)

        assertTrue(result.isSuccess)
        result.getOrNull().let {
            assertEquals("$TEXT_FILE_NAME.txt", it?.name)
            assertEquals(TEXT_FILE_CONTENT.length.toLong(), it?.size)
            assertEquals(IONFLSTFileType.File("application/text"), it?.type)
        }
    }

    @Test
    fun `given directory has children, when listing the directory, success is returned with correct number of children`() =
        runTest {
            sut.createDirectory(
                IONFLSTUri.Unresolved(IONFLSTFolderType.INTERNAL_FILES, "dir/child1"),
                IONFLSTCreateOptions(recursive = true)
            ).let { assertTrue(it.isSuccess) }
            sut.createDirectory(
                IONFLSTUri.Unresolved(IONFLSTFolderType.INTERNAL_FILES, "dir/child2"),
                IONFLSTCreateOptions(recursive = true)
            ).let { assertTrue(it.isSuccess) }
            sut.createDirectory(
                IONFLSTUri.Unresolved(IONFLSTFolderType.INTERNAL_FILES, "dir/child3"),
                IONFLSTCreateOptions(recursive = true)
            ).let { assertTrue(it.isSuccess) }

            val result =
                sut.listDirectory(IONFLSTUri.Unresolved(IONFLSTFolderType.INTERNAL_FILES, "dir"))

            assertTrue(result.isSuccess)
            assertEquals(3, result.getOrNull()?.size)
        }

    @Test
    fun `given local file exists, when deleting it, success is returned`() = runTest {
        val localFileUri = IONFLSTUri.Unresolved(IONFLSTFolderType.INTERNAL_FILES, "file.txt")
        sut.createFile(localFileUri, IONFLSTCreateOptions(recursive = true))
            .let { assertTrue(it.isSuccess) }

        val result = sut.delete(localFileUri, options = IONFLSTDeleteOptions(recursive = false))

        assertTrue(result.isSuccess)
    }

    @Test
    fun `given local directory exists, when deleting it, success is returned`() = runTest {
        val localDirUri = IONFLSTUri.Unresolved(IONFLSTFolderType.INTERNAL_FILES, "dir")
        sut.createDirectory(localDirUri, IONFLSTCreateOptions(recursive = true))
            .let { assertTrue(it.isSuccess) }

        val result = sut.delete(localDirUri, options = IONFLSTDeleteOptions(recursive = false))

        assertTrue(result.isSuccess)
    }

    @Test
    fun `given content file exists, when deleting it, success is returned`() = runTest {
        val uriContentScheme = IONFLSTUri.Unresolved(
            null,
            "content://$TEST_CONTENT_PROVIDER_NAME/$IMAGE_FILE_NAME"
        )

        val result = sut.delete(uriContentScheme, options = IONFLSTDeleteOptions(recursive = false))

        assertTrue(result.isSuccess)
    }

    @Test
    fun `given source local file exists, when copying it, success is returned`() = runTest {
        val sourceUri = IONFLSTUri.Unresolved(IONFLSTFolderType.EXTERNAL_FILES, "oldFile.txt")
        val destinationUri = IONFLSTUri.Unresolved(IONFLSTFolderType.EXTERNAL_CACHE, "newFile.txt")
        sut.saveFile(
            sourceUri,
            options = IONFLSTSaveOptions(
                "lorem ipsum",
                IONFLSTEncoding.DefaultCharset,
                IONFLSTSaveMode.WRITE,
                true
            )
        ).let { assertTrue(it.isSuccess) }

        val result = sut.copy(sourceUri, destinationUri)

        assertTrue(result.isSuccess)
        assertEquals(
            Uri.parse("file://${context.externalCacheDir?.absolutePath}/newFile.txt"),
            result.getOrNull()
        )
    }

    @Test
    fun `given source directory exists, when copying it, success is returned`() = runTest {
        val sourceDirUri = IONFLSTUri.Unresolved(IONFLSTFolderType.INTERNAL_CACHE, "c")
        val destinationDirUri = IONFLSTUri.Unresolved(IONFLSTFolderType.INTERNAL_FILES, "f")
        // to copy a non-empty source directory
        sut.createFile(
            IONFLSTUri.Unresolved(IONFLSTFolderType.INTERNAL_CACHE, "c/file.txt"),
            IONFLSTCreateOptions(recursive = true)
        ).let { assertTrue(it.isSuccess) }
        sut.createDirectory(
            IONFLSTUri.Unresolved(IONFLSTFolderType.INTERNAL_CACHE, "c/subdir"),
            IONFLSTCreateOptions(recursive = true)
        ).let { assertTrue(it.isSuccess) }

        val result = sut.copy(sourceDirUri, destinationDirUri)

        assertTrue(result.isSuccess)
        assertEquals(
            Uri.parse("file://${context.filesDir.absolutePath}/f"),
            result.getOrNull()
        )
    }

    @Test
    fun `given source content file exists, when copying it, success is returned`() = runTest {
        val sourceUriContentScheme = IONFLSTUri.Unresolved(
            null,
            "content://$TEST_CONTENT_PROVIDER_NAME/$IMAGE_FILE_NAME"
        )
        val destinationUriLocalFile = IONFLSTUri.Unresolved(
            IONFLSTFolderType.EXTERNAL_CACHE,
            "newImage.jpeg"
        )

        val result = sut.copy(sourceUriContentScheme, destinationUriLocalFile)

        assertTrue(result.isSuccess)
        assertEquals(
            Uri.parse("file://${context.externalCacheDir?.absolutePath}/newImage.jpeg"),
            result.getOrNull()
        )
    }

    @Test
    fun `given source local file exists, when moving it, success is returned`() = runTest {
        val sourceUri = IONFLSTUri.Unresolved(IONFLSTFolderType.EXTERNAL_FILES, "oldFile.txt")
        val destinationUri = IONFLSTUri.Unresolved(IONFLSTFolderType.EXTERNAL_CACHE, "newFile.txt")
        sut.saveFile(
            sourceUri,
            options = IONFLSTSaveOptions(
                "lorem ipsum",
                IONFLSTEncoding.DefaultCharset,
                IONFLSTSaveMode.WRITE,
                true
            )
        ).let { assertTrue(it.isSuccess) }

        val result = sut.move(sourceUri, destinationUri)

        assertTrue(result.isSuccess)
        assertEquals(
            Uri.parse("file://${context.externalCacheDir?.absolutePath}/newFile.txt"),
            result.getOrNull()
        )
    }

    @Test
    fun `given source directory exists, when moving it, success is returned`() = runTest {
        val sourceDirUri = IONFLSTUri.Unresolved(IONFLSTFolderType.INTERNAL_CACHE, "c")
        val destinationDirUri = IONFLSTUri.Unresolved(IONFLSTFolderType.INTERNAL_FILES, "f")
        // to copy a non-empty source directory
        sut.createFile(
            IONFLSTUri.Unresolved(IONFLSTFolderType.INTERNAL_CACHE, "c/file.txt"),
            IONFLSTCreateOptions(recursive = true)
        ).let { assertTrue(it.isSuccess) }
        sut.createDirectory(
            IONFLSTUri.Unresolved(IONFLSTFolderType.INTERNAL_CACHE, "c/subdir"),
            IONFLSTCreateOptions(recursive = true)
        ).let { assertTrue(it.isSuccess) }

        val result = sut.move(sourceDirUri, destinationDirUri)

        assertTrue(result.isSuccess)
        assertEquals(
            Uri.parse("file://${context.filesDir.absolutePath}/f"),
            result.getOrNull()
        )
    }
    // endregion happy path tests

    // region uri resolve errors
    @Test
    fun `given an unresolved uri input, when trying to get the full uri, UnresolvableUri error is returned`() =
        runTest {
            val uriUnknown = IONFLSTUri.Unresolved(null, "unknown://file")

            val result = sut.getFileUri(uriUnknown)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFLSTExceptions.UnresolvableUri)
        }

    @Test
    fun `given a content file, when trying to create it, NotSupportedForContentScheme error is returned`() =
        runTest {
            val uriContentScheme = IONFLSTUri.Unresolved(
                null,
                "/data/synthetic/photo_picker/image_file_to_create.jpeg"
            )

            val result = sut.createFile(uriContentScheme, IONFLSTCreateOptions(recursive = false))

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFLSTExceptions.NotSupportedForContentScheme)
        }

    @Test
    fun `given a local file exists, when trying to create a directory in it, NotSupportedForFiles error is returned`() =
        runTest {
            val uriLocalFile = IONFLSTUri.Unresolved(
                parentFolder = IONFLSTFolderType.INTERNAL_FILES,
                "fileToCreate.txt"
            )
            sut.createFile(uriLocalFile, IONFLSTCreateOptions(recursive = false))
                .let { assertTrue(it.isSuccess) }

            val result = sut.createDirectory(uriLocalFile, IONFLSTCreateOptions(recursive = false))

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFLSTExceptions.NotSupportedForFiles)
        }

    @Test
    fun `given directory exists, when trying to read a file in it, NotSupportedForDirectory error is returned`() =
        runTest {
            val uriLocalDirectory = IONFLSTUri.Unresolved(IONFLSTFolderType.EXTERNAL_CACHE, "dir")
            sut.createDirectory(uriLocalDirectory, IONFLSTCreateOptions(recursive = true))
                .let { assertTrue(it.isSuccess) }

            val result = sut.readFile(uriLocalDirectory, IONFLSTReadOptions(IONFLSTEncoding.Base64))

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFLSTExceptions.NotSupportedForDirectory)
        }

    @Test
    fun `given directory exists, when trying to read a file in chunks, NotSupportedForDirectory error is returned`() =
        runTest {
            val uriLocalDirectory = IONFLSTUri.Unresolved(IONFLSTFolderType.EXTERNAL_CACHE, "dir")
            sut.createDirectory(uriLocalDirectory, IONFLSTCreateOptions(recursive = true))
                .let { assertTrue(it.isSuccess) }

            sut.readFileByChunks(
                uriLocalDirectory,
                IONFLSTReadByChunksOptions(IONFLSTEncoding.Base64, 1)
            ).test {
                val error = awaitError()

                assertTrue(error is IONFLSTExceptions.NotSupportedForDirectory)
            }
        }

    @Test
    fun `given file does not exist, when we try to read from it in chunks, DoesNotExist error is returned`() =
        runTest {
            val uriLocalFile = IONFLSTUri.Unresolved(
                parentFolder = IONFLSTFolderType.INTERNAL_FILES,
                "nonExistent"
            )

            sut.readFileByChunks(
                uriLocalFile,
                IONFLSTReadByChunksOptions(IONFLSTEncoding.DefaultCharset, 8192)
            ).test {
                val error = awaitError()

                assertTrue(error is IONFLSTExceptions.DoesNotExist)
            }
        }

    @Test
    fun `given source local and destination content, when trying to copy, MixingLocalAndContent error is returned`() =
        runTest {
            val sourceUriLocalFile =
                IONFLSTUri.Unresolved(IONFLSTFolderType.EXTERNAL_FILES, "oldFile.txt")
            val destinationUriContentScheme = IONFLSTUri.Unresolved(
                null,
                "content://$TEST_CONTENT_PROVIDER_NAME/$TEXT_FILE_NAME"
            )
            sut.createFile(sourceUriLocalFile, IONFLSTCreateOptions(recursive = true))
                .let { assertTrue(it.isSuccess) }

            val result = sut.copy(sourceUriLocalFile, destinationUriContentScheme)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFLSTExceptions.CopyRenameFailed.LocalToContent)
        }

    @Test
    fun `given source content and destination content, when trying to copy, SourceAndDestinationContent error is returned`() =
        runTest {
            val sourceUriContentScheme = IONFLSTUri.Unresolved(
                null,
                "content://$TEST_CONTENT_PROVIDER_NAME/$IMAGE_FILE_NAME"
            )
            val destinationUriContentScheme = IONFLSTUri.Unresolved(
                null,
                "content://$TEST_CONTENT_PROVIDER_NAME/$TEXT_FILE_NAME"
            )

            val result = sut.copy(sourceUriContentScheme, destinationUriContentScheme)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFLSTExceptions.CopyRenameFailed.SourceAndDestinationContent)
        }

    @Test
    fun `given source content and destination local, when trying to move, NotSupportedForContentScheme error is returned`() =
        runTest {
            val sourceUriContentScheme = IONFLSTUri.Unresolved(
                null,
                "content://$TEST_CONTENT_PROVIDER_NAME/$IMAGE_FILE_NAME"
            )
            val destinationUriLocalFile = IONFLSTUri.Unresolved(
                IONFLSTFolderType.EXTERNAL_CACHE,
                "newImage.jpeg"
            )

            val result = sut.move(sourceUriContentScheme, destinationUriLocalFile)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFLSTExceptions.NotSupportedForContentScheme)
        }
    // endregion uri resolve errors
}