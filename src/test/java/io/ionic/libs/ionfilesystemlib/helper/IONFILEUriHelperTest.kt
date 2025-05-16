package io.ionic.libs.ionfilesystemlib.helper

import android.content.Context
import android.net.Uri
import android.os.Environment
import io.ionic.libs.ionfilesystemlib.common.fileUriWithEncodings
import io.ionic.libs.ionfilesystemlib.model.LocalUriType
import io.ionic.libs.ionfilesystemlib.model.IONFILEExceptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEFolderType
import io.ionic.libs.ionfilesystemlib.model.IONFILEUri
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
class IONFILEUriHelperTest {

    private lateinit var context: Context
    private lateinit var sut: IONFILEUriHelper

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication().applicationContext
        // To allow external storage
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED)

        sut = IONFILEUriHelper(context)
    }

    @Test
    fun `given there is a content scheme uri, when resolving the uri, a Resolved#Content is returned`() =
        runTest {
            val unresolvedUri = IONFILEUri.Unresolved(
                parentFolder = null,
                uriPath = "content://media/external/audio/1"
            )

            val result = sut.resolveUri(unresolvedUri)

            assertEquals(
                IONFILEUri.Resolved.Content(Uri.parse("content://media/external/audio/1")),
                result.getOrNull()
            )
        }

    @Test
    fun `given there is a synthetic path uri, when resolving the uri, a Resolved#Content is returned`() =
        runTest {
            val unresolvedUri = IONFILEUri.Unresolved(
                parentFolder = null,
                uriPath = "/synthetic/photo_picker_content/12345.mp4"
            )

            val result = sut.resolveUri(unresolvedUri)

            assertEquals(
                IONFILEUri.Resolved.Content(Uri.parse("content://media/photo_picker_content/12345")),
                result.getOrNull()
            )
        }

    @Test
    fun `given there is a synthetic path uri without extension, when attempting to resolve it, an UnresolvableUri exception it returned`() =
        runTest {
            val unresolvedUri = IONFILEUri.Unresolved(
                parentFolder = null,
                uriPath = "/synthetic/photo_picker_content/noExtension"
            )

            val result = sut.resolveUri(unresolvedUri)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.UnresolvableUri)
        }

    @Test
    fun `given there is an internal cache file, when resolving the uri, a Resolves#Local is returned`() =
        runTest {
            val path = "existingFile.txt"
            File(context.cacheDir, path).createNewFile()
            val unresolvedUri = IONFILEUri.Unresolved(IONFILEFolderType.INTERNAL_CACHE, path)

            val result = sut.resolveUri(unresolvedUri)

            assertEquals(
                IONFILEUri.Resolved.Local(
                    "${context.cacheDir}/$path",
                    Uri.parse("file://${context.cacheDir}/$path"),
                    LocalUriType.FILE,
                    inExternalStorage = false
                ),
                result.getOrNull()
            )
        }

    @Test
    fun `given there is an internal files directory, when resolving the uri, a Resolves#Local is returned`() =
        runTest {
            val path = "path/to/directory"
            File(context.filesDir, path).mkdirs()
            val unresolvedUri = IONFILEUri.Unresolved(IONFILEFolderType.INTERNAL_FILES, path)

            val result = sut.resolveUri(unresolvedUri)

            assertEquals(
                IONFILEUri.Resolved.Local(
                    "${context.filesDir}/$path/",
                    Uri.parse("file://${context.filesDir}/$path/"),
                    LocalUriType.DIRECTORY,
                    inExternalStorage = false
                ),
                result.getOrNull()
            )
        }

    @Test
    fun `given external cache directory path, when resolving the uri, a Resolves#Local is returned`() =
        runTest {
            val path = "path to/the directory"
            // adding extra "/" just to make sure that the result does not go with the extra "/"
            val unresolvedUri = IONFILEUri.Unresolved(IONFILEFolderType.EXTERNAL_CACHE, "/$path")

            val result = sut.resolveUri(unresolvedUri)

            assertEquals(
                IONFILEUri.Resolved.Local(
                    "${context.externalCacheDir}/$path/",
                    fileUriWithEncodings("file://${context.externalCacheDir}/$path/"),
                    LocalUriType.UNKNOWN,
                    inExternalStorage = false
                ),
                result.getOrNull()
            )
        }

    @Test
    fun `given external files path, when resolving the uri, a Resolves#Local is returned`() =
        runTest {
            val path = "some file that does not exist"
            val unresolvedUri = IONFILEUri.Unresolved(IONFILEFolderType.EXTERNAL_FILES, path)

            val result = sut.resolveUri(unresolvedUri)

            assertEquals(
                IONFILEUri.Resolved.Local(
                    "${context.getExternalFilesDir(null)}/$path/",
                    fileUriWithEncodings("file://${context.getExternalFilesDir(null)}/$path/"),
                    LocalUriType.UNKNOWN,
                    inExternalStorage = false
                ),
                result.getOrNull()
            )
        }

    @Test
    fun `given non-existent directory path in external storage, when resolving the uri, a Resolves#Local is returned`() =
        runTest {
            // trailing slash present, to make sure it does not get added again
            val path = "this is a directory/with multiple/parent/folders/"
            val unresolvedUri = IONFILEUri.Unresolved(IONFILEFolderType.EXTERNAL_STORAGE, path)

            val result = sut.resolveUri(unresolvedUri)

            assertEquals(
                IONFILEUri.Resolved.Local(
                    "${Environment.getExternalStorageDirectory()}/$path",
                    fileUriWithEncodings("file://${Environment.getExternalStorageDirectory()}/$path"),
                    LocalUriType.UNKNOWN,
                    inExternalStorage = true
                ),
                result.getOrNull()
            )
        }

    @Test
    fun `given non-existent file path in documents, when resolving the uri, a Resolves#Local is returned`() =
        runTest {
            val path = "doc with spaces.pdf"
            val unresolvedUri = IONFILEUri.Unresolved(IONFILEFolderType.DOCUMENTS, path)
            val documentsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

            val result = sut.resolveUri(unresolvedUri)

            assertEquals(
                IONFILEUri.Resolved.Local(
                    "$documentsDir/$path",
                    fileUriWithEncodings("file://$documentsDir/$path"),
                    LocalUriType.UNKNOWN,
                    inExternalStorage = true
                ),
                result.getOrNull()
            )
        }

    @Test
    fun `given there is a full file path, when resolving the uri, a Resolved#Local is returned`() =
        runTest {
            val path = "/data/files/file with spaces.txt"
            val unresolvedUri = IONFILEUri.Unresolved(parentFolder = null, uriPath = path)

            val result = sut.resolveUri(unresolvedUri)

            assertEquals(
                IONFILEUri.Resolved.Local(
                    path,
                    fileUriWithEncodings("file://$path"),
                    LocalUriType.UNKNOWN,
                    inExternalStorage = true
                ),
                result.getOrNull()
            )
        }

    @Test
    fun `given there is a file uri, when resolving the uri, a Resolved#Local is returned`() =
        runTest {
            val path = "/data/files/test.txt"
            val unresolvedUri = IONFILEUri.Unresolved(parentFolder = null, uriPath = "file://$path")

            val result = sut.resolveUri(unresolvedUri)

            assertEquals(
                IONFILEUri.Resolved.Local(
                    path,
                    Uri.parse("file://$path"),
                    LocalUriType.UNKNOWN,
                    inExternalStorage = true
                ),
                result.getOrNull()
            )
        }

    @Test
    fun `given a uri with unknown scheme, when attempting to resolve it, an UnresolvableUri exception it returned`() =
        runTest {
            val unresolvedUri =
                IONFILEUri.Unresolved(parentFolder = null, uriPath = "invalidUriScheme://some/path")

            val result = sut.resolveUri(unresolvedUri)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.UnresolvableUri)
        }
}