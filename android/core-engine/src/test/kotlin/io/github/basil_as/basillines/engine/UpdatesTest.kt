package io.github.basil_as.basillines.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatesTest {
    @Test
    fun comparesVersionsNumerically() {
        assertTrue(Updates.isNewer("v1.5.5", "1.5.4"))
        assertTrue(Updates.isNewer("1.10.0", "1.9.9"))
        assertTrue(Updates.isNewer("2.0", "1.99.99"))
        assertFalse(Updates.isNewer("1.5.4", "1.5.4"))
        assertFalse(Updates.isNewer("v1.5.3", "1.5.4"))
        assertFalse(Updates.isNewer("1.5", "1.5.0"))
        assertFalse(Updates.isNewer("latest", "1.5.4"))
        assertFalse(Updates.isNewer("1.5.5", "dev"))
        assertNull(Updates.parse("1.x.3"))
        assertNull(Updates.parse("1"))
    }

    private fun release(tag: String, url: String, name: String = "ColorLines.apk") =
        """{"tag_name":"$tag","assets":[{"name":"SHA256SUMS.txt","browser_download_url":"https://github.com/x"},{"name":"$name","browser_download_url":"$url"}]}"""

    @Test
    fun readsTheNewestReleaseButOnlyFromTheProjectRepository() {
        val good = "https://github.com/Basil-AS/color-lines-98/releases/download/v1.6.0/ColorLines.apk"
        assertEquals(UpdateInfo("1.6.0", good), Updates.fromReleaseJson(release("v1.6.0", good), "1.5.4"))
        assertNull(Updates.fromReleaseJson(release("v1.5.4", good), "1.5.4"))
        assertNull(Updates.fromReleaseJson(release("v1.6.0", "https://evil.example/ColorLines.apk"), "1.5.4"))
        assertNull(Updates.fromReleaseJson(release("v1.6.0", good, name = "Other.apk"), "1.5.4"))
        assertNull(Updates.fromReleaseJson("not json", "1.5.4"))
        assertNull(Updates.fromReleaseJson("""{"tag_name":"v9.0.0"}""", "1.5.4"))
    }

    @Test
    fun recognisesTheProjectSigningKey() {
        val key = Updates.PROJECT_KEY_SHA256.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        assertTrue(Updates.isProjectKey(key))
        assertFalse(Updates.isProjectKey(ByteArray(32)))
        assertEquals("5D:30:30:E9:36:38:04:C3…", Updates.shortFingerprint(key))
    }
}
