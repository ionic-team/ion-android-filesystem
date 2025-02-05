package io.ionic.libs.ionfilesystemlib.model

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IONFILEFolderTypeTest {

    @Test
    fun `check that there are no duplicate aliases for IONFILEFolderType`() = runTest {
        // This check is to provide some protection against
        //  providing an alias that can match for more than one folder type
        val entries = IONFILEFolderType.entries
        val allAliases = entries.map { listOf(it.name) + it.alternateNames }.flatten()
        val coercedAliases = allAliases.map { IONFILEFolderType.coerceFolderAlias(it) }

        val distinctCoercedAliases = coercedAliases.distinct()

        assertEquals(coercedAliases.size, distinctCoercedAliases.size)
    }

    @Test
    fun `test mapping cache`() = runTest {
        val input = "cache"

        val result = IONFILEFolderType.fromStringAlias(input)

        assertEquals(IONFILEFolderType.INTERNAL_CACHE, result)
        assertFalse(result!!.inExternalStorage)
    }

    @Test
    fun `test mapping data`() = runTest {
        val input = "data"

        val result = IONFILEFolderType.fromStringAlias(input)

        assertEquals(IONFILEFolderType.INTERNAL_FILES, result)
    }

    @Test
    fun `test mapping external`() = runTest {
        val input = "EXTERNAL"

        val result = IONFILEFolderType.fromStringAlias(input)

        assertEquals(IONFILEFolderType.EXTERNAL_FILES, result)
        assertFalse(result!!.inExternalStorage)
    }

    @Test
    fun `test mapping cache-external`() = runTest {
        val input = "cache-external"

        val result = IONFILEFolderType.fromStringAlias(input)

        assertEquals(IONFILEFolderType.EXTERNAL_CACHE, result)
        assertFalse(result!!.inExternalStorage)
    }

    @Test
    fun `test mapping ExternalStorage`() = runTest {
        val input = "ExternalStorage"

        val result = IONFILEFolderType.fromStringAlias(input)

        assertEquals(IONFILEFolderType.EXTERNAL_STORAGE, result)
        assertTrue(result!!.inExternalStorage)
    }
}