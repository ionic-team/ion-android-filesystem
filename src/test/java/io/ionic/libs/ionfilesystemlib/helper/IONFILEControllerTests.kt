package io.ionic.libs.ionfilesystemlib.helper

import android.net.Uri
import android.os.Environment
import app.cash.turbine.test
import io.ionic.libs.ionfilesystemlib.IONFILEController
import io.ionic.libs.ionfilesystemlib.common.IMAGE_FILE_CONTENT
import io.ionic.libs.ionfilesystemlib.common.IMAGE_FILE_NAME
import io.ionic.libs.ionfilesystemlib.common.IONFILETestFileContentProvider
import io.ionic.libs.ionfilesystemlib.common.LOREM_IPSUM_2800_CHARS
import io.ionic.libs.ionfilesystemlib.common.TEST_CONTENT_PROVIDER_NAME
import io.ionic.libs.ionfilesystemlib.common.TEXT_FILE_CONTENT
import io.ionic.libs.ionfilesystemlib.common.TEXT_FILE_NAME
import io.ionic.libs.ionfilesystemlib.model.IONFILECreateOptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEDeleteOptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEEncoding
import io.ionic.libs.ionfilesystemlib.model.IONFILEExceptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEFileType
import io.ionic.libs.ionfilesystemlib.model.IONFILEFolderType
import io.ionic.libs.ionfilesystemlib.model.IONFILEReadByChunksOptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEReadOptions
import io.ionic.libs.ionfilesystemlib.model.IONFILESaveMode
import io.ionic.libs.ionfilesystemlib.model.IONFILESaveOptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEUri
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
import java.io.File
import java.util.Base64
import kotlin.math.ceil

/**
 * Tests for the [IONFILEController]
 *
 * These tests are not 100% exhaustive of the entire library.
 * That is because most of the logic is covered in IONFILE(...)HelperTest classes
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class IONFILEControllerTests {

    private val context get() = RuntimeEnvironment.getApplication()
    private lateinit var contentProvider: IONFILETestFileContentProvider
    private lateinit var sut: IONFILEController

    @Before
    fun setUp() {
        contentProvider = Robolectric.setupContentProvider(
            IONFILETestFileContentProvider::class.java,
            TEST_CONTENT_PROVIDER_NAME
        )
        sut = IONFILEController(context)
    }

    @After
    fun tearDown() {
        contentProvider.cleanup()
    }

    // region happy path tests
    @Test
    fun `given local file path, when creating it, success with file uri is returned`() = runTest {
        val uriLocalFile = IONFILEUri.Unresolved(
            parentFolder = IONFILEFolderType.INTERNAL_FILES,
            "fileToCreate.txt"
        )

        val result = sut.createFile(uriLocalFile, IONFILECreateOptions(recursive = false))

        assertTrue(result.isSuccess)
        assertEquals(
            Uri.parse("file://${context.filesDir.absolutePath}/fileToCreate.txt"),
            result.getOrNull()
        )
    }

    @Test
    fun `given local directory path, when creating it, success with directory uri is returned`() =
        runTest {
            val uriLocalFile = IONFILEUri.Unresolved(
                parentFolder = IONFILEFolderType.EXTERNAL_FILES,
                "subDir1/subDir2/directory"
            )

            val result = sut.createDirectory(uriLocalFile, IONFILECreateOptions(recursive = true))

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
            val uriLocalFile = IONFILEUri.Unresolved(
                parentFolder = IONFILEFolderType.INTERNAL_FILES,
                "fileToCreate.txt"
            )
            sut.saveFile(
                uriLocalFile,
                IONFILESaveOptions(
                    data = "Some\ntext\n$#%#$&>xj93w5ts\n\uD83D\uDC35 \uD83D\uDE1F ❎ \uD83D\uDCC1",
                    encoding = IONFILEEncoding.DefaultCharset,
                    mode = IONFILESaveMode.WRITE,
                    createFileRecursive = true
                )
            ).let { assertTrue(it.isSuccess) }

            val result =
                sut.readFile(uriLocalFile, IONFILEReadOptions(IONFILEEncoding.DefaultCharset))

            assertTrue(result.isSuccess)
            assertEquals(
                "Some\ntext\n$#%#$&>xj93w5ts\n\uD83D\uDC35 \uD83D\uDE1F ❎ \uD83D\uDCC1",
                result.getOrNull()
            )
        }

    @Test
    fun `given file in content provider, when reading it, success with file contents is returned`() =
        runTest {
            val uriContentScheme = IONFILEUri.Unresolved(
                null,
                "content://$TEST_CONTENT_PROVIDER_NAME/$IMAGE_FILE_NAME"
            )

            val result = sut.readFile(uriContentScheme, IONFILEReadOptions(IONFILEEncoding.Base64))

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
            val uriLocalFile = IONFILEUri.Unresolved(
                parentFolder = IONFILEFolderType.INTERNAL_FILES,
                "largeFile.txt"
            )
            val data = LOREM_IPSUM_2800_CHARS.repeat(4000) // over 10 MB of text
            sut.saveFile(
                uriLocalFile,
                IONFILESaveOptions(
                    data = data,
                    encoding = IONFILEEncoding.DefaultCharset,
                    mode = IONFILESaveMode.WRITE,
                    createFileRecursive = true
                )
            ).let { assertTrue(it.isSuccess) }
            val chunkSize = 256 * 1024
            val numberOfChunks = ceil(data.length.toFloat() / chunkSize).toInt()
            var result = ""

            sut.readFileByChunks(
                uriLocalFile,
                IONFILEReadByChunksOptions(IONFILEEncoding.DefaultCharset, chunkSize)
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
            val uriContentScheme = IONFILEUri.Unresolved(
                null,
                "content://$TEST_CONTENT_PROVIDER_NAME/$IMAGE_FILE_NAME"
            )

            sut.readFileByChunks(
                uriContentScheme,
                IONFILEReadByChunksOptions(IONFILEEncoding.Base64, Int.MAX_VALUE)
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
        val uriCacheDir = IONFILEUri.Unresolved(IONFILEFolderType.INTERNAL_CACHE, "")

        val result = sut.getMetadata(uriCacheDir)

        assertTrue(result.isSuccess)
        result.getOrNull().let {
            assertEquals(context.cacheDir.absolutePath, it?.fullPath)
            assertEquals(context.cacheDir.name, it?.name)
            assertEquals(IONFILEFileType.Directory, it?.type)
        }
    }

    @Test
    fun `given file in content provider, when getting metadata, success is returned`() = runTest {
        val uriContentScheme =
            IONFILEUri.Unresolved(null, "content://$TEST_CONTENT_PROVIDER_NAME/$TEXT_FILE_NAME")

        val result = sut.getMetadata(uriContentScheme)

        assertTrue(result.isSuccess)
        result.getOrNull().let {
            assertEquals("$TEXT_FILE_NAME.txt", it?.name)
            assertEquals(TEXT_FILE_CONTENT.length.toLong(), it?.size)
            assertEquals(IONFILEFileType.File("application/text"), it?.type)
        }
    }

    @Test
    fun `given directory has children, when listing the directory, success is returned with correct number of children`() =
        runTest {
            sut.createDirectory(
                IONFILEUri.Unresolved(IONFILEFolderType.INTERNAL_FILES, "dir/child1"),
                IONFILECreateOptions(recursive = true)
            ).let { assertTrue(it.isSuccess) }
            sut.createDirectory(
                IONFILEUri.Unresolved(IONFILEFolderType.INTERNAL_FILES, "dir/child2"),
                IONFILECreateOptions(recursive = true)
            ).let { assertTrue(it.isSuccess) }
            sut.createDirectory(
                IONFILEUri.Unresolved(IONFILEFolderType.INTERNAL_FILES, "dir/child3"),
                IONFILECreateOptions(recursive = true)
            ).let { assertTrue(it.isSuccess) }

            val result =
                sut.listDirectory(IONFILEUri.Unresolved(IONFILEFolderType.INTERNAL_FILES, "dir"))

            assertTrue(result.isSuccess)
            assertEquals(3, result.getOrNull()?.size)
        }

    @Test
    fun `given local file exists, when deleting it, success is returned`() = runTest {
        val localFileUri = IONFILEUri.Resolved.Local(
            fullPath = File(Environment.getExternalStorageDirectory(), "text.txt").absolutePath
        )
        sut.createFile(localFileUri, IONFILECreateOptions(recursive = true))
            .let { assertTrue(it.isSuccess) }

        val result = sut.delete(localFileUri, options = IONFILEDeleteOptions(recursive = false))

        assertTrue(result.isSuccess)
    }

    @Test
    fun `given local directory exists, when deleting it, success is returned`() = runTest {
        val localDirUri = IONFILEUri.Unresolved(IONFILEFolderType.INTERNAL_FILES, "dir")
        sut.createDirectory(localDirUri, IONFILECreateOptions(recursive = true))
            .let { assertTrue(it.isSuccess) }

        val result = sut.delete(localDirUri, options = IONFILEDeleteOptions(recursive = false))

        assertTrue(result.isSuccess)
    }

    @Test
    fun `given content file exists, when deleting it, success is returned`() = runTest {
        val uriContentScheme = IONFILEUri.Unresolved(
            null,
            "content://$TEST_CONTENT_PROVIDER_NAME/$IMAGE_FILE_NAME"
        )

        val result = sut.delete(uriContentScheme, options = IONFILEDeleteOptions(recursive = false))

        assertTrue(result.isSuccess)
    }

    @Test
    fun `given source local file exists, when copying it, success is returned`() = runTest {
        val sourceUri = IONFILEUri.Unresolved(IONFILEFolderType.EXTERNAL_FILES, "oldFile.txt")
        val destinationUri = IONFILEUri.Unresolved(IONFILEFolderType.EXTERNAL_CACHE, "newFile.txt")
        sut.saveFile(
            sourceUri,
            options = IONFILESaveOptions(
                "lorem ipsum",
                IONFILEEncoding.DefaultCharset,
                IONFILESaveMode.WRITE,
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
        val sourceDirUri = IONFILEUri.Unresolved(IONFILEFolderType.INTERNAL_CACHE, "c")
        val destinationDirUri = IONFILEUri.Unresolved(IONFILEFolderType.INTERNAL_FILES, "f")
        // to copy a non-empty source directory
        sut.createFile(
            IONFILEUri.Unresolved(IONFILEFolderType.INTERNAL_CACHE, "c/file.txt"),
            IONFILECreateOptions(recursive = true)
        ).let { assertTrue(it.isSuccess) }
        sut.createDirectory(
            IONFILEUri.Unresolved(IONFILEFolderType.INTERNAL_CACHE, "c/subdir"),
            IONFILECreateOptions(recursive = true)
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
        val sourceUriContentScheme = IONFILEUri.Unresolved(
            null,
            "content://$TEST_CONTENT_PROVIDER_NAME/$IMAGE_FILE_NAME"
        )
        val destinationUriLocalFile = IONFILEUri.Unresolved(
            IONFILEFolderType.EXTERNAL_CACHE,
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
        val sourceUri = IONFILEUri.Unresolved(IONFILEFolderType.EXTERNAL_FILES, "oldFile.txt")
        val destinationUri = IONFILEUri.Unresolved(IONFILEFolderType.DOCUMENTS, "newFile.txt")
        val documentsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        sut.saveFile(
            sourceUri,
            options = IONFILESaveOptions(
                "lorem ipsum",
                IONFILEEncoding.DefaultCharset,
                IONFILESaveMode.WRITE,
                true
            )
        ).let { assertTrue(it.isSuccess) }

        val result = sut.move(sourceUri, destinationUri)

        assertTrue(result.isSuccess)
        assertEquals(
            Uri.parse("file://${documentsDir.absolutePath}/newFile.txt"),
            result.getOrNull()
        )
    }

    @Test
    fun `given source directory exists, when moving it, success is returned`() = runTest {
        val sourceDirUri = IONFILEUri.Unresolved(IONFILEFolderType.INTERNAL_CACHE, "c")
        val destinationDirUri = IONFILEUri.Unresolved(IONFILEFolderType.INTERNAL_FILES, "f")
        // to copy a non-empty source directory
        sut.createFile(
            IONFILEUri.Unresolved(IONFILEFolderType.INTERNAL_CACHE, "c/file.txt"),
            IONFILECreateOptions(recursive = true)
        ).let { assertTrue(it.isSuccess) }
        sut.createDirectory(
            IONFILEUri.Unresolved(IONFILEFolderType.INTERNAL_CACHE, "c/subdir"),
            IONFILECreateOptions(recursive = true)
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
            val uriUnknown = IONFILEUri.Unresolved(null, "unknown://file")

            val result = sut.getFileUri(uriUnknown)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.UnresolvableUri)
        }

    @Test
    fun `given a content file, when trying to create it, NotSupportedForContentScheme error is returned`() =
        runTest {
            val uriContentScheme = IONFILEUri.Unresolved(
                null,
                "/data/synthetic/photo_picker/image_file_to_create.jpeg"
            )

            val result = sut.createFile(uriContentScheme, IONFILECreateOptions(recursive = false))

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.NotSupportedForContentScheme)
        }

    @Test
    fun `given a local file exists, when trying to create a directory in it, NotSupportedForFiles error is returned`() =
        runTest {
            val uriLocalFile = IONFILEUri.Unresolved(
                parentFolder = IONFILEFolderType.INTERNAL_FILES,
                "fileToCreate.txt"
            )
            sut.createFile(uriLocalFile, IONFILECreateOptions(recursive = false))
                .let { assertTrue(it.isSuccess) }

            val result = sut.createDirectory(uriLocalFile, IONFILECreateOptions(recursive = false))

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.NotSupportedForFiles)
        }

    @Test
    fun `given directory exists, when trying to read a file in it, NotSupportedForDirectory error is returned`() =
        runTest {
            val uriLocalDirectory = IONFILEUri.Unresolved(IONFILEFolderType.EXTERNAL_CACHE, "dir")
            sut.createDirectory(uriLocalDirectory, IONFILECreateOptions(recursive = true))
                .let { assertTrue(it.isSuccess) }

            val result = sut.readFile(uriLocalDirectory, IONFILEReadOptions(IONFILEEncoding.Base64))

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.NotSupportedForDirectory)
        }

    @Test
    fun `given directory exists, when trying to read a file in chunks, NotSupportedForDirectory error is returned`() =
        runTest {
            val uriLocalDirectory = IONFILEUri.Unresolved(IONFILEFolderType.EXTERNAL_CACHE, "dir")
            sut.createDirectory(uriLocalDirectory, IONFILECreateOptions(recursive = true))
                .let { assertTrue(it.isSuccess) }

            sut.readFileByChunks(
                uriLocalDirectory,
                IONFILEReadByChunksOptions(IONFILEEncoding.Base64, 1)
            ).test {
                val error = awaitError()

                assertTrue(error is IONFILEExceptions.NotSupportedForDirectory)
            }
        }

    @Test
    fun `given file does not exist, when we try to read from it in chunks, DoesNotExist error is returned`() =
        runTest {
            val uriLocalFile = IONFILEUri.Unresolved(
                parentFolder = IONFILEFolderType.INTERNAL_FILES,
                "nonExistent"
            )

            sut.readFileByChunks(
                uriLocalFile,
                IONFILEReadByChunksOptions(IONFILEEncoding.DefaultCharset, 8192)
            ).test {
                val error = awaitError()

                assertTrue(error is IONFILEExceptions.DoesNotExist)
            }
        }

    @Test
    fun `given source local and destination content, when trying to copy, MixingLocalAndContent error is returned`() =
        runTest {
            val sourceUriLocalFile =
                IONFILEUri.Unresolved(IONFILEFolderType.EXTERNAL_FILES, "oldFile.txt")
            val destinationUriContentScheme = IONFILEUri.Unresolved(
                null,
                "content://$TEST_CONTENT_PROVIDER_NAME/$TEXT_FILE_NAME"
            )
            sut.createFile(sourceUriLocalFile, IONFILECreateOptions(recursive = true))
                .let { assertTrue(it.isSuccess) }

            val result = sut.copy(sourceUriLocalFile, destinationUriContentScheme)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.CopyRenameFailed.LocalToContent)
        }

    @Test
    fun `given source content and destination content, when trying to copy, SourceAndDestinationContent error is returned`() =
        runTest {
            val sourceUriContentScheme = IONFILEUri.Unresolved(
                null,
                "content://$TEST_CONTENT_PROVIDER_NAME/$IMAGE_FILE_NAME"
            )
            val destinationUriContentScheme = IONFILEUri.Unresolved(
                null,
                "content://$TEST_CONTENT_PROVIDER_NAME/$TEXT_FILE_NAME"
            )

            val result = sut.copy(sourceUriContentScheme, destinationUriContentScheme)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.CopyRenameFailed.SourceAndDestinationContent)
        }

    @Test
    fun `given source content and destination local, when trying to move, NotSupportedForContentScheme error is returned`() =
        runTest {
            val sourceUriContentScheme = IONFILEUri.Unresolved(
                null,
                "content://$TEST_CONTENT_PROVIDER_NAME/$IMAGE_FILE_NAME"
            )
            val destinationUriLocalFile = IONFILEUri.Unresolved(
                IONFILEFolderType.EXTERNAL_CACHE,
                "newImage.jpeg"
            )

            val result = sut.move(sourceUriContentScheme, destinationUriLocalFile)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.NotSupportedForContentScheme)
        }
    // endregion uri resolve errors
}