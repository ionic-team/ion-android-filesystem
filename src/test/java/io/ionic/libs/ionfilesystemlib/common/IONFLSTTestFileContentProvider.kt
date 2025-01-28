package io.ionic.libs.ionfilesystemlib.common

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

internal const val TEST_CONTENT_PROVIDER_NAME = "test.file.provider"
internal val TEST_TIMESTAMP: Long = System.currentTimeMillis()
internal const val TEXT_FILE_NAME = "text file"
internal const val IMAGE_FILE_NAME = "image"
internal const val TEXT_FILE_CONTENT = "This is a\nmultiline\ntext"
internal const val IMAGE_FILE_CONTENT =
    "\u0005\u0004Hljı{#«ë5{»Ü\u000FY\u000B:‹‡'\u000EBÿN\u0005Ãz…Ÿ÷£WøΩıòljı{>ﬁ'–ı{>‡<V+\u0015ÏÒ\uF8FFÒ‰IS‰‡ú\u001A\u0007BÅí%?˜ΩÔxjÙ1"

internal class IONFLSTTestFileContentProvider : ContentProvider() {

    private val rootDir
        get() = File(System.getProperty("java.io.tmpdir"), IONFLSTBaseJUnitTest.ROOT_DIR_NAME)

    private val fileList: List<TestFileContent> = listOf(
        TestFileContent(
            name = "$TEXT_FILE_NAME.txt",
            data = TEXT_FILE_CONTENT,
            mimeType = "application/text"
        ),
        TestFileContent(
            name = "$IMAGE_FILE_NAME.jpeg",
            data = IMAGE_FILE_CONTENT,
            mimeType = "image/jpeg"
        )
    )

    override fun onCreate(): Boolean {
        rootDir.mkdirs()
        fileList.forEach { file ->
            File(rootDir, file.name).apply {
                createNewFile()
                writeBytes(file.data.toByteArray())
            }
        }
        return true
    }

    fun cleanup() {
        rootDir.deleteRecursively()
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        if (mode != "r") {
            throw NotImplementedError("This test class only supports reading of files")
        }
        val testFile = uri.getTestFile()
        if (testFile != null) {
            return ParcelFileDescriptor.open(
                File(rootDir, testFile.name),
                ParcelFileDescriptor.MODE_READ_ONLY
            )
        } else {
            throw FileNotFoundException("No file found for provided uri $uri")
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor {
        val cursor = MatrixCursor(
            arrayOf(
                BaseColumns._ID,
                OpenableColumns.DISPLAY_NAME,
                OpenableColumns.SIZE,
                MediaStore.MediaColumns.DATE_ADDED
            )
        )

        val testFile = uri.getTestFile()

        if (testFile != null) {
            val id = fileList.indexOf(testFile)
            cursor.addRow(
                arrayOf(
                    id,
                    testFile.name,
                    testFile.data.length,
                    TEST_TIMESTAMP
                )
            )
        }

        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw NotImplementedError("This test class does not implement insert")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int =
        if (uri.getTestFile() != null) {
            1  // do not actually remove file to not break tests
        } else {
            0  // file not found
        }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        throw NotImplementedError("This test class does not implement update")
    }

    override fun getType(uri: Uri): String = uri.getTestFile()?.mimeType ?: ""

    private fun Uri.getTestFile(): TestFileContent? {
        val fileName = lastPathSegment ?: return null
        return fileList.firstOrNull { it.name == fileName || it.nameWithoutExtension == fileName }
    }

    private data class TestFileContent(
        val name: String,
        val data: String,
        val mimeType: String?
    ) {
        val nameWithoutExtension = name.substringBefore('.')
    }
}
