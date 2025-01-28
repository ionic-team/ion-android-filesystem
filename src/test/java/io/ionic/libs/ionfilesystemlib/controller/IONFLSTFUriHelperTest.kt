package io.ionic.libs.ionfilesystemlib.controller

import android.content.Context
import android.net.Uri
import android.os.Environment
import io.ionic.libs.ionfilesystemlib.common.fileUriWithEncodings
import io.ionic.libs.ionfilesystemlib.model.LocalUriType
import io.ionic.libs.ionfilesystemlib.model.IONFLSTExceptions
import io.ionic.libs.ionfilesystemlib.model.IONFLSTFolderType
import io.ionic.libs.ionfilesystemlib.model.IONFLSTUri
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class IONFLSTFUriHelperTest {

    private lateinit var context: Context
    private lateinit var sut: IONFLSTFUriHelper

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication().applicationContext
        // To allow external storage
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED)

        sut = IONFLSTFUriHelper(context)
    }

    @Test
    fun `given there is a content scheme uri, when resolving the uri, a Resolved#Content is returned`() =
        runTest {
            val unresolvedUri = IONFLSTUri.Unresolved(
                parentFolder = null,
                uriPath = "content://media/external/audio/1"
            )

            val result = sut.resolveUri(unresolvedUri)

            assertEquals(
                IONFLSTUri.Resolved.Content(Uri.parse("content://media/external/audio/1")),
                result.getOrNull()
            )
        }

    @Test
    fun `given there is a synthetic path uri, when resolving the uri, a Resolved#Content is returned`() =
        runTest {
            val unresolvedUri = IONFLSTUri.Unresolved(
                parentFolder = null,
                uriPath = "/synthetic/photo_picker_content/12345.mp4"
            )

            val result = sut.resolveUri(unresolvedUri)

            assertEquals(
                IONFLSTUri.Resolved.Content(Uri.parse("content://media/photo_picker_content/12345")),
                result.getOrNull()
            )
        }

    @Test
    fun `given there is a synthetic path uri without extension, when attempting to resolve it, an UnresolvableUri exception it returned`() =
        runTest {
            val unresolvedUri = IONFLSTUri.Unresolved(
                parentFolder = null,
                uriPath = "/synthetic/photo_picker_content/noExtension"
            )

            val result = sut.resolveUri(unresolvedUri)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFLSTExceptions.UnresolvableUri)
        }

    @Test
    fun `given there is an internal cache file, when resolving the uri, a Resolves#Local is returned`() =
        runTest {
            val path = "existingFile.txt"
            File(context.cacheDir, path).createNewFile()
            val unresolvedUri = IONFLSTUri.Unresolved(IONFLSTFolderType.INTERNAL_CACHE, path)

            val result = sut.resolveUri(unresolvedUri)

            assertEquals(
                IONFLSTUri.Resolved.Local(
                    "${context.cacheDir}/$path",
                    Uri.parse("file://${context.cacheDir}/$path"),
                    LocalUriType.FILE
                ),
                result.getOrNull()
            )
        }

    @Test
    fun `given there is an internal files directory, when resolving the uri, a Resolves#Local is returned`() =
        runTest {
            val path = "path/to/directory"
            File(context.filesDir, path).mkdirs()
            val unresolvedUri = IONFLSTUri.Unresolved(IONFLSTFolderType.INTERNAL_FILES, path)

            val result = sut.resolveUri(unresolvedUri)

            assertEquals(
                IONFLSTUri.Resolved.Local(
                    "${context.filesDir}/$path",
                    Uri.parse("file://${context.filesDir}/$path"),
                    LocalUriType.DIRECTORY
                ),
                result.getOrNull()
            )
        }

    @Test
    fun `given external cache directory path, when resolving the uri, a Resolves#Local is returned`() =
        runTest {
            val path = "path to/the directory"
            // adding extra "/" just to make sure that the result does not go with the extra "/"
            val unresolvedUri = IONFLSTUri.Unresolved(IONFLSTFolderType.EXTERNAL_CACHE, "/$path")

            val result = sut.resolveUri(unresolvedUri)

            assertEquals(
                IONFLSTUri.Resolved.Local(
                    "${context.externalCacheDir}/$path",
                    fileUriWithEncodings("file://${context.externalCacheDir}/$path"),
                    LocalUriType.UNKNOWN
                ),
                result.getOrNull()
            )
        }

    @Test
    fun `given external files path, when resolving the uri, a Resolves#Local is returned`() =
        runTest {
            val path = "some file that does not exist"
            val unresolvedUri = IONFLSTUri.Unresolved(IONFLSTFolderType.EXTERNAL_FILES, path)

            val result = sut.resolveUri(unresolvedUri)

            assertEquals(
                IONFLSTUri.Resolved.Local(
                    "${context.getExternalFilesDir(null)}/$path",
                    fileUriWithEncodings("file://${context.getExternalFilesDir(null)}/$path"),
                    LocalUriType.UNKNOWN
                ),
                result.getOrNull()
            )
        }

    @Test
    fun `given non-existent directory path in external storage, when resolving the uri, a Resolves#Local is returned`() =
        runTest {
            val path = "this is a directory/with multiple/parent/folders"
            val unresolvedUri = IONFLSTUri.Unresolved(IONFLSTFolderType.EXTERNAL_STORAGE, path)

            val result = sut.resolveUri(unresolvedUri)

            assertEquals(
                IONFLSTUri.Resolved.Local(
                    "${Environment.getExternalStorageDirectory()}/$path",
                    fileUriWithEncodings("file://${Environment.getExternalStorageDirectory()}/$path"),
                    LocalUriType.UNKNOWN
                ),
                result.getOrNull()
            )
        }

    @Test
    fun `given non-existent file path in documents, when resolving the uri, a Resolves#Local is returned`() =
        runTest {
            val path = "doc with spaces.pdf"
            val unresolvedUri = IONFLSTUri.Unresolved(IONFLSTFolderType.DOCUMENTS, path)
            val documentsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

            val result = sut.resolveUri(unresolvedUri)

            assertEquals(
                IONFLSTUri.Resolved.Local(
                    "$documentsDir/$path",
                    fileUriWithEncodings("file://$documentsDir/$path"),
                    LocalUriType.UNKNOWN
                ),
                result.getOrNull()
            )
        }

    @Test
    fun `given there is a full file path, when resolving the uri, a Resolved#Local is returned`() =
        runTest {
            val path = "/data/files/file with spaces.txt"
            val unresolvedUri = IONFLSTUri.Unresolved(parentFolder = null, uriPath = path)

            val result = sut.resolveUri(unresolvedUri)

            assertEquals(
                IONFLSTUri.Resolved.Local(
                    path,
                    fileUriWithEncodings("file://$path"),
                    LocalUriType.UNKNOWN
                ),
                result.getOrNull()
            )
        }

    @Test
    fun `given there is a file uri, when resolving the uri, a Resolved#Local is returned`() =
        runTest {
            val path = "/data/files/test.txt"
            val unresolvedUri = IONFLSTUri.Unresolved(parentFolder = null, uriPath = "file://$path")

            val result = sut.resolveUri(unresolvedUri)

            assertEquals(
                IONFLSTUri.Resolved.Local(path, Uri.parse("file://$path"), LocalUriType.UNKNOWN),
                result.getOrNull()
            )
        }

    @Test
    fun `given a uri with unknown scheme, when attempting to resolve it, an UnresolvableUri exception it returned`() =
        runTest {
            val unresolvedUri =
                IONFLSTUri.Unresolved(parentFolder = null, uriPath = "invalidUriScheme://some/path")

            val result = sut.resolveUri(unresolvedUri)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFLSTExceptions.UnresolvableUri)
        }
}