package io.ionic.libs.osfilesystemlib.model

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OSFLSTFolderTypeTest {

    @Test
    fun `check that there are no duplicate aliases for OSFLSTFolderType`() = runTest {
        // This check is to provide some protection against
        //  providing an alias that can match for more than one folder type
        val entries = OSFLSTFolderType.entries
        val allAliases = entries.map { listOf(it.name) + it.alternateNames }.flatten()
        val coercedAliases = allAliases.map { OSFLSTFolderType.coerceFolderAlias(it) }

        val distinctCoercedAliases = coercedAliases.distinct()

        assertEquals(coercedAliases.size, distinctCoercedAliases.size)
    }

    @Test
    fun `test mapping cache`() = runTest {
        val input = "cache"

        val result = OSFLSTFolderType.fromStringAlias(input)

        assertEquals(OSFLSTFolderType.INTERNAL_CACHE, result)
        assertFalse(result!!.requiresPermission)
    }

    @Test
    fun `test mapping data`() = runTest {
        val input = "data"

        val result = OSFLSTFolderType.fromStringAlias(input)

        assertEquals(OSFLSTFolderType.INTERNAL_FILES, result)
    }

    @Test
    fun `test mapping external`() = runTest {
        val input = "EXTERNAL"

        val result = OSFLSTFolderType.fromStringAlias(input)

        assertEquals(OSFLSTFolderType.EXTERNAL_FILES, result)
        assertFalse(result!!.requiresPermission)
    }

    @Test
    fun `test mapping cache-external`() = runTest {
        val input = "cache-external"

        val result = OSFLSTFolderType.fromStringAlias(input)

        assertEquals(OSFLSTFolderType.EXTERNAL_CACHE, result)
        assertFalse(result!!.requiresPermission)
    }

    @Test
    fun `test mapping ExternalStorage`() = runTest {
        val input = "ExternalStorage"

        val result = OSFLSTFolderType.fromStringAlias(input)

        assertEquals(OSFLSTFolderType.EXTERNAL_STORAGE, result)
        assertTrue(result!!.requiresPermission)
    }
}