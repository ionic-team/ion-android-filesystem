package io.ionic.libs.ionfilesystemlib.helper

import android.os.Build
import app.cash.turbine.test
import io.ionic.libs.ionfilesystemlib.common.IONFILEBaseJUnitTest
import io.ionic.libs.ionfilesystemlib.common.LOREM_IPSUM_2800_CHARS
import io.ionic.libs.ionfilesystemlib.helper.common.IONFILEBuildConfig
import io.ionic.libs.ionfilesystemlib.model.IONFILECreateOptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEEncoding
import io.ionic.libs.ionfilesystemlib.model.IONFILEExceptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEFileType
import io.ionic.libs.ionfilesystemlib.model.IONFILEReadInChunksOptions
import io.ionic.libs.ionfilesystemlib.model.IONFILEReadOptions
import io.ionic.libs.ionfilesystemlib.model.IONFILESaveMode
import io.ionic.libs.ionfilesystemlib.model.IONFILESaveOptions
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Base64

class IONFILELocalFilesHelperTest : IONFILEBaseJUnitTest() {

    private lateinit var sut: IONFILELocalFilesHelper

    override fun additionalSetups() {
        sut = IONFILELocalFilesHelper()
    }

    // region createFile tests
    @Test
    fun `given there is a parent directory, when we create a file in that directory, success is returned`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath

            val result =
                sut.createFile(path, IONFILECreateOptions(recursive = false))

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
                sut.createFile(path, IONFILECreateOptions(recursive = true))

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
                sut.createFile(path, IONFILECreateOptions(recursive = false))

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.CreateFailed.NoParentDirectory)
            assertFalse(file.exists())
        }
    // endregion createFile tests

    // region save + read file tests
    @Test
    fun `given empty file exists, when saving contents to file as write, success is returned`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath
            sut.createFile(path, IONFILECreateOptions(recursive = false))
            val data = "Write"

            val result = sut.saveFile(
                path,
                IONFILESaveOptions(
                    data,
                    encoding = IONFILEEncoding.DefaultCharset,
                    mode = IONFILESaveMode.WRITE,
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
            sut.createFile(path, IONFILECreateOptions(recursive = false))
            val data = "Append"

            val result = sut.saveFile(
                path,
                IONFILESaveOptions(
                    data,
                    encoding = IONFILEEncoding.DefaultCharset,
                    mode = IONFILESaveMode.APPEND,
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
            sut.createFile(path, IONFILECreateOptions(recursive = false))
            val dataBase64 =
                Base64.getEncoder().encodeToString("Base 64 w/ special ch4rsª~´".toByteArray())

            val result = sut.saveFile(
                path,
                IONFILESaveOptions(
                    dataBase64,
                    encoding = IONFILEEncoding.Base64,
                    mode = IONFILESaveMode.WRITE,
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
            sut.createFile(path, IONFILECreateOptions(recursive = false))

            val result = sut.readFile(path, IONFILEReadOptions(encoding = IONFILEEncoding.Default))

            assertTrue(result.isSuccess)
            assertEquals("", result.getOrNull())
        }

    @Test
    fun `given non-empty file exists, when reading file, success is returned with contents`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath
            sut.createFile(path, IONFILECreateOptions(recursive = false))
            sut.saveFile(
                path,
                IONFILESaveOptions(
                    LOREM_IPSUM_2800_CHARS,
                    IONFILEEncoding.DefaultCharset,
                    IONFILESaveMode.WRITE,
                    false
                )
            )

            val result = sut.readFile(
                path, IONFILEReadOptions(encoding = IONFILEEncoding.DefaultCharset)
            )

            assertTrue(result.isSuccess)
            assertEquals(LOREM_IPSUM_2800_CHARS, result.getOrNull())
        }

    @Test
    fun `given non-empty file exists, when reading file with extra file separator, success is returned with contents`() =
        runTest {
            val file = File(testRootDirectory, "subdir/extra_separator.txt")
            val path = file.absolutePath
            val pathExtraSeparator = path.replace("subdir/", "subdir//")
            sut.saveFile(
                path,
                IONFILESaveOptions(
                    LOREM_IPSUM_2800_CHARS,
                    IONFILEEncoding.DefaultCharset,
                    IONFILESaveMode.WRITE,
                    createFileRecursive = true
                )
            )

            val result = sut.readFile(
                pathExtraSeparator, IONFILEReadOptions(encoding = IONFILEEncoding.DefaultCharset)
            )

            assertTrue(result.isSuccess)
            assertEquals(LOREM_IPSUM_2800_CHARS, result.getOrNull())
        }

    @Test
    fun `given non-empty file saved as base64, when reading file as utf-8, success is returned with utf-8 string`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath
            sut.createFile(path, IONFILECreateOptions(recursive = false))
            val data = Base64.getEncoder().encodeToString("Base 64 +/ =!\uD83D\uDE00".toByteArray())
            sut.saveFile(
                path,
                IONFILESaveOptions(data, IONFILEEncoding.Base64, IONFILESaveMode.WRITE, false)
            )

            val result = sut.readFile(
                path, IONFILEReadOptions(encoding = IONFILEEncoding.WithCharset(Charsets.UTF_8))
            )

            assertTrue(result.isSuccess)
            assertEquals("Base 64 +/ =!\uD83D\uDE00", result.getOrNull())
        }

    @Test
    fun `given non-empty file saved as data url with base64, when reading file as utf-8, success is returned with utf-8 string without the prefix`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath
            sut.createFile(path, IONFILECreateOptions(recursive = false))
            val base64 = Base64.getEncoder().encodeToString("Base 64 +/ =!  ".toByteArray())
            val data = "data:textPlain;base64, $base64"
            sut.saveFile(
                path,
                IONFILESaveOptions(data, IONFILEEncoding.Base64, IONFILESaveMode.WRITE, false)
            )

            val result = sut.readFile(
                path, IONFILEReadOptions(encoding = IONFILEEncoding.WithCharset(Charsets.UTF_8))
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
            sut.createFile(path, IONFILECreateOptions(recursive = false))
            val data = "Lorem ipsum"
            sut.saveFile(
                path,
                IONFILESaveOptions(
                    data, IONFILEEncoding.WithCharset(Charsets.UTF_8), IONFILESaveMode.WRITE, false
                )
            )

            val result = sut.readFile(path, IONFILEReadOptions(encoding = IONFILEEncoding.Base64))

            assertTrue(result.isSuccess)
            assertEquals("TG9yZW0gaXBzdW0=", result.getOrNull())
        }

    @Test
    fun `given non-empty file exists, when saving contents to file as write, success is returned and original content is gone`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath
            sut.createFile(path, IONFILECreateOptions(recursive = false))
            val originalData = "Original"
            sut.saveFile(
                path,
                IONFILESaveOptions(
                    originalData, IONFILEEncoding.DefaultCharset, IONFILESaveMode.WRITE, false
                )
            )
            val newData = "New content"

            val saveResult = sut.saveFile(
                path,
                IONFILESaveOptions(
                    newData,
                    encoding = IONFILEEncoding.DefaultCharset,
                    mode = IONFILESaveMode.WRITE,
                    createFileRecursive = false
                )
            )
            val readResult = sut.readFile(path, IONFILEReadOptions(IONFILEEncoding.DefaultCharset))

            assertTrue(saveResult.isSuccess)
            assertEquals("New content", readResult.getOrNull())
        }

    @Test
    fun `given non-empty file exists, when saving contents to file as append, success is returned and original content remains`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath
            sut.createFile(path, IONFILECreateOptions(recursive = false))
            val originalData = "Original"
            sut.saveFile(
                path,
                IONFILESaveOptions(
                    originalData, IONFILEEncoding.DefaultCharset, IONFILESaveMode.WRITE, false
                )
            )
            val newData = "\n\n\t-> New content"

            val saveResult = sut.saveFile(
                path,
                IONFILESaveOptions(
                    newData,
                    encoding = IONFILEEncoding.DefaultCharset,
                    mode = IONFILESaveMode.APPEND,
                    createFileRecursive = false
                )
            )
            val readResult = sut.readFile(path, IONFILEReadOptions(IONFILEEncoding.DefaultCharset))

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
                IONFILESaveOptions(
                    "any data...",
                    encoding = IONFILEEncoding.DefaultCharset,
                    mode = IONFILESaveMode.WRITE,
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
                IONFILESaveOptions(
                    "any data...",
                    encoding = IONFILEEncoding.DefaultCharset,
                    mode = IONFILESaveMode.WRITE,
                    createFileRecursive = true
                )
            )

            assertTrue(result.isSuccess)
            assertTrue(file.length() > 0)
        }

    @Test
    fun `given nor file nor parent directories exist, when saving contents to file with createFileRecursive=false, error is returned`() =
        runTest {
            val file = fileInSubDir
            val path = file.absolutePath

            val result = sut.saveFile(
                path,
                IONFILESaveOptions(
                    "any data...",
                    encoding = IONFILEEncoding.DefaultCharset,
                    mode = IONFILESaveMode.WRITE,
                    createFileRecursive = false
                )
            )

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.CreateFailed.NoParentDirectory)
        }

    @Test
    fun `given file does not exist, when reading file, DoesNotExist error is returned`() = runTest {
        val file = fileInRootDir
        val path = file.absolutePath

        val result = sut.readFile(path, IONFILEReadOptions(encoding = IONFILEEncoding.Default))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IONFILEExceptions.DoesNotExist)
    }

    @Test
    fun `given there is a directory, when trying to save it as a file, error is returned`() =
        runTest {
            val dir = testRootDirectory
            val path = dir.absolutePath


            val result = sut.saveFile(
                path,
                IONFILESaveOptions(
                    "any data...",
                    // this would result in an error because the provided data is not base64
                    //  however because we are providing a directory, the method should return before base64 conversion
                    encoding = IONFILEEncoding.Base64,
                    mode = IONFILESaveMode.WRITE,
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

            val result = sut.readFile(path, IONFILEReadOptions(IONFILEEncoding.Default))

            // error is thrown on java.io side, exception is returned as-is
            assertTrue(result.isFailure)
        }
    // endregion save + read file tests

    // region read by chunks tests
    @Test
    fun `given empty file exists, when reading in chunks, only complete even is emitted`() =
        runTest {
            val path = fileInRootDir.absolutePath
            sut.createFile(path, IONFILECreateOptions(recursive = false))

            sut.readFileInChunks(
                path,
                IONFILEReadInChunksOptions(IONFILEEncoding.Default, Int.MAX_VALUE)
            ).test {

                awaitComplete()
            }
        }

    @Test
    fun `given non-empty file exists, when reading with large chunk, a single string with all file contents is emitted`() =
        runTest {
            val path = fileInRootDir.absolutePath
            sut.saveFile(
                path,
                IONFILESaveOptions(
                    LOREM_IPSUM_2800_CHARS,
                    IONFILEEncoding.DefaultCharset,
                    IONFILESaveMode.WRITE,
                    createFileRecursive = true
                )
            )

            sut.readFileInChunks(
                path,
                IONFILEReadInChunksOptions(IONFILEEncoding.DefaultCharset, Int.MAX_VALUE)
            ).test {

                val result = awaitItem()
                assertEquals(LOREM_IPSUM_2800_CHARS, result)
                awaitComplete()
            }
        }

    @Test
    fun `given non-empty file exists, when reading in chunks, multiple strings are emitted`() =
        runTest {
            val path = fileInRootDir.absolutePath
            val fileContents = """
                This is a small file\n
                Sm4ll chunk s1z3 shou1d b3 us3d!!\r\t
                damsodnzcxnxknl\\n
                
                $path
            """.trimIndent()
            sut.saveFile(
                path,
                IONFILESaveOptions(
                    fileContents,
                    IONFILEEncoding.DefaultCharset,
                    IONFILESaveMode.WRITE,
                    createFileRecursive = true
                )
            )
            val splits = 5
            val chunkSize = fileContents.length / splits
            val hasAdditionalChunk = (chunkSize * splits < fileContents.length)
            var result = ""

            sut.readFileInChunks(
                path,
                IONFILEReadInChunksOptions(IONFILEEncoding.DefaultCharset, chunkSize),
                bufferSize = chunkSize / 2 // to make sure all the chunks are emitted, while reading from file multiple times
            ).test {
                val loopStart = if (hasAdditionalChunk) 0 else 1
                var offset = 0
                for (splitIndex in loopStart..splits) {
                    val chunk = awaitItem()
                    assertEquals(
                        fileContents.substring(offset, offset + chunk.length),
                        chunk
                    )
                    offset += chunk.length
                    result += chunk
                }
                awaitComplete()
            }

            assertEquals(fileContents, result)
        }

    @Test
    fun `given non-empty file exists, when reading in chunks with Base64, multiple strings are emitted and the final concatenated string can be decoded correctly`() =
        runTest {
            val path = fileInRootDir.absolutePath
            sut.saveFile(
                path,
                IONFILESaveOptions(
                    LOREM_IPSUM_2800_CHARS,
                    IONFILEEncoding.DefaultCharset,
                    IONFILESaveMode.WRITE,
                    createFileRecursive = true
                )
            )
            val splits = 10
            val chunkSize = LOREM_IPSUM_2800_CHARS.length / splits
            val hasAdditionalChunk =
                (((chunkSize / 3 * 3) + 3) * splits < LOREM_IPSUM_2800_CHARS.length)
            var result = ""

            sut.readFileInChunks(
                path,
                IONFILEReadInChunksOptions(IONFILEEncoding.Base64, chunkSize),
                bufferSize = chunkSize / 3 // to make sure all the chunks are emitted, while reading from file multiple times
            ).test {
                val loopStart = if (hasAdditionalChunk) 0 else 1
                for (splitIndex in loopStart..splits) {
                    val chunk = awaitItem()
                    if (splitIndex < splits) {
                        assertEquals(0, Base64.getDecoder().decode(result).size % 3)
                    }
                    result += chunk
                }
                awaitComplete()
            }

            assertEquals(LOREM_IPSUM_2800_CHARS, String(Base64.getDecoder().decode(result)))
        }

    @Test
    fun `given file does not exist, when read in chunks, DoesNotExist error is returned`() =
        runTest {
            val file = fileInRootDir
            val path = file.absolutePath

            sut.readFileInChunks(
                path,
                IONFILEReadInChunksOptions(IONFILEEncoding.Default, 1)
            ).test {

                val result = awaitError()
                assertTrue(result is IONFILEExceptions.DoesNotExist)
            }
        }
    // endregion read by chunks tests

    // region fileMetadata tests
    @Test
    fun `given empty file exists, when getting file metadata, the correct information is returned`() =
        runTest {
            mockkMimeTypeMap(TEXT_MIME_TYPE)
            val path = fileInRootDir.absolutePath
            sut.createFile(path, IONFILECreateOptions(recursive = false))

            val result = sut.getFileMetadata(path)

            assertTrue(result.isSuccess)
            result.getOrNull()!!.let {
                assertEquals(path, it.fullPath)
                assertEquals(FILE_NAME_TXT, it.name)
                assertEquals(0, it.size)
                assertEquals(IONFILEFileType.File(TEXT_MIME_TYPE), it.type)
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
                assertEquals(IONFILEFileType.Directory, it.type)
            }
        }

    @Test
    fun `given non-empty file exists, when getting file metadata, the correct size is returned`() =
        runTest {
            val path = fileInRootDir.absolutePath
            val plainTextData = "Text"
            sut.saveFile(
                path,
                IONFILESaveOptions(
                    data = plainTextData,
                    encoding = IONFILEEncoding.WithCharset(Charsets.UTF_8),
                    mode = IONFILESaveMode.WRITE,
                    createFileRecursive = true
                )
            )

            val result = sut.getFileMetadata(path)

            assertTrue(result.isSuccess)
            assertEquals(plainTextData.length.toLong(), result.getOrNull()?.size)
        }

    @Test
    fun `given Android version below 26, when getting file metadata, createdTimestamp is zero`() =
        runTest {
            val path = fileInRootDir.absolutePath
            every { IONFILEBuildConfig.getAndroidSdkVersionCode() } returns Build.VERSION_CODES.N
            sut.createFile(path, IONFILECreateOptions(recursive = false))

            val result = sut.getFileMetadata(path)

            assertTrue(result.isSuccess)
            assertEquals(null, result.getOrNull()?.createdTimestamp)
        }

    @Test
    fun `given 3ga file, when getting file metadata, type is File with audio 3gpp mimeType`() =
        runTest {
            val path = File(testRootDirectory, "audio_file.3ga").absolutePath
            mockkMimeTypeMap(null)
            sut.createFile(path, IONFILECreateOptions(recursive = false))

            val result = sut.getFileMetadata(path)

            assertTrue(result.isSuccess)
            assertEquals(IONFILEFileType.File(mimeType = "audio/3gpp"), result.getOrNull()?.type)
        }

    @Test
    fun `given js file, when getting file metadata, type is File with text javascript mimeType`() =
        runTest {
            val path = File(testRootDirectory, "code.js").absolutePath
            mockkMimeTypeMap(null)
            sut.createFile(path, IONFILECreateOptions(recursive = false))

            val result = sut.getFileMetadata(path)

            assertTrue(result.isSuccess)
            assertEquals(
                IONFILEFileType.File(mimeType = "text/javascript"),
                result.getOrNull()?.type
            )
        }

    @Test
    fun `given unknown mimetype, when getting file metadata, type is File with fallback mimeType`() =
        runTest {
            val path = File(testRootDirectory, "fileWithoutExtension").absolutePath
            mockkMimeTypeMap(null)
            sut.createFile(path, IONFILECreateOptions(recursive = false))

            val result = sut.getFileMetadata(path)

            assertTrue(result.isSuccess)
            assertEquals(
                IONFILEFileType.File(mimeType = "application/octet-binary"),
                result.getOrNull()?.type
            )
        }

    @Test
    fun `given file does not exist, when getting file metadata, DoesNotExist error is returned`() =
        runTest {
            val path = fileInRootDir.absolutePath

            val result = sut.getFileMetadata(path)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.DoesNotExist)
        }
    // endregion fileMetadata sets

    // region deleteFile tests
    @Test
    fun `given file exists, when we delete it, success is returned`() = runTest {
        val file = fileInRootDir
        val path = file.absolutePath
        sut.createFile(path, IONFILECreateOptions(recursive = false))

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
        assertTrue(result.exceptionOrNull() is IONFILEExceptions.DoesNotExist)
    }
    // endregion deleteFile tests

    // region copyFile tests
    @Test
    fun `given source file has content, when we copy it, success is returned`() = runTest {
        val sourceFile = fileInSubDir
        val sourcePath = sourceFile.absolutePath
        val destinationFile = fileInRootDir
        val destinationPath = destinationFile.absolutePath
        sut.saveFile(
            sourcePath,
            IONFILESaveOptions(
                "data_to_copy",
                IONFILEEncoding.DefaultCharset,
                IONFILESaveMode.WRITE,
                createFileRecursive = true
            )
        )

        val result = sut.copyFile(sourcePath, destinationPath)

        assertTrue(result.isSuccess)
        assertEquals("data_to_copy".length.toLong(), destinationFile.length())
        // to confirm that original file is still there after copying
        assertEquals("data_to_copy".length.toLong(), sourceFile.length())
    }

    @Test
    fun `given source file is the same as destination, when we copy to it, success is returned`() =
        runTest {
            val sourcePath = fileInRootDir.absolutePath

            val result = sut.copyFile(sourcePath, sourcePath)

            assertTrue(result.isSuccess)
        }

    @Test
    fun `given source file does not exist, when we try to copy it, DoesNotExist error is returned`() =
        runTest {
            val sourcePath = fileInSubDir.absolutePath
            val destinationPath = fileInRootDir.absolutePath

            val result = sut.copyFile(sourcePath, destinationPath)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.DoesNotExist)
        }

    @Test
    fun `given destination is a directory, when we try to copy to it, MixingFilesAndDirectories is returned`() =
        runTest {
            val sourcePath = fileInRootDir.absolutePath
            val destinationPath = testRootDirectory.absolutePath
            sut.createFile(sourcePath, IONFILECreateOptions(recursive = true))

            val result = sut.copyFile(sourcePath, destinationPath)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.CopyRenameFailed.MixingFilesAndDirectories)
        }
    // endregion copyFile tests

    // region renameFile tests
    @Test
    fun `given source file exists, when we rename it, success is returned`() = runTest {
        val sourceFile = fileInSubDir
        val sourcePath = sourceFile.absolutePath
        val destinationFile = fileInRootDir
        val destinationPath = destinationFile.absolutePath
        sut.createFile(sourcePath, IONFILECreateOptions(recursive = true))

        val result = sut.renameFile(sourcePath, destinationPath)

        assertTrue(result.isSuccess)
        assertTrue(destinationFile.exists())
        assertFalse(sourceFile.exists())
    }

    @Test
    fun `given destination has no parent directory, when we try to rename it, NoParentDirectory is returned`() =
        runTest {
            val sourcePath = fileInRootDir.absolutePath
            val destinationPath = fileInSubDir.absolutePath // sub-directories not created
            sut.createFile(sourcePath, IONFILECreateOptions(recursive = true))

            val result = sut.renameFile(sourcePath, destinationPath)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.CopyRenameFailed.NoParentDirectory)
        }

    @Test
    fun `given source file is a directory, when we try to rename it, MixingFilesAndDirectories error is returned`() =
        runTest {
            val sourceDir = testRootDirectory
            val sourcePath = sourceDir.absolutePath
            val destinationFile = fileInRootDir
            val destinationPath = destinationFile.absolutePath

            val result = sut.renameFile(sourcePath, destinationPath)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IONFILEExceptions.CopyRenameFailed.MixingFilesAndDirectories)
        }
    // endregion renameFile tests
    // region readRange tests
    @Test
    fun `readRange should read specific bytes from file`() = runTest {
        val file = fileInRootDir
        val path = file.absolutePath
        val content = "Hello, World!"
        sut.saveFile(
            path,
            IONFILESaveOptions(
                content,
                IONFILEEncoding.WithCharset(Charsets.UTF_8),
                IONFILESaveMode.WRITE,
                createFileRecursive = false
            )
        )

        val result = sut.readRange(
            fullPath = path,
            offset = 7,
            length = 5,
            options = IONFILEReadOptions(encoding = IONFILEEncoding.WithCharset(Charsets.UTF_8))
        )

        assertEquals("World", result.getOrNull())
    }

    @Test
    fun `readRange should handle Base64 encoding`() = runTest {
        val file = fileInRootDir
        val path = file.absolutePath
        val content = "Hello, World!"
        sut.saveFile(
             path,
            IONFILESaveOptions(
                content,
                IONFILEEncoding.WithCharset(Charsets.UTF_8),
                IONFILESaveMode.WRITE,
                createFileRecursive = false
            )
        )

        val result = sut.readRange(
            fullPath = path,
            offset = 0,
            length = 5,
            options = IONFILEReadOptions(encoding = IONFILEEncoding.Base64)
        )

        // "Hello" in Base64 is "SGVsbG8="
        assertEquals("SGVsbG8=", result.getOrNull())
    }

    @Test
    fun `readRange should handle EOF gracefully`() = runTest {
         val file = fileInRootDir
         val path = file.absolutePath
         val content = "12345"
         sut.saveFile(
            path,
            IONFILESaveOptions(
                content,
                IONFILEEncoding.WithCharset(Charsets.UTF_8),
                IONFILESaveMode.WRITE,
                createFileRecursive = false
            )
        )

         val result = sut.readRange(
             fullPath = path,
             offset = 3,
             length = 10, // Try to read past EOF
             options = IONFILEReadOptions(encoding = IONFILEEncoding.WithCharset(Charsets.UTF_8))
         )

         assertEquals("45", result.getOrNull())
    }

    @Test
    fun `readRange should return empty string if offset is at EOF`() = runTest {
         val file = fileInRootDir
         val path = file.absolutePath
         val content = "12345"
         sut.saveFile(
            path,
            IONFILESaveOptions(
                content,
                IONFILEEncoding.WithCharset(Charsets.UTF_8),
                IONFILESaveMode.WRITE,
                createFileRecursive = false
            )
        )

         val result = sut.readRange(
             fullPath = path,
             offset = 5,
             length = 10,
             options = IONFILEReadOptions(encoding = IONFILEEncoding.WithCharset(Charsets.UTF_8))
         )

         assertEquals("", result.getOrNull())
    }
    // endregion readRange tests
}