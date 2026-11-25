package app.gamenative.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdaterTest {

    @Test
    fun isNewer_higherPatch_returnsTrue() {
        assertTrue(AppUpdater.isNewer("v2.1.3", "2.1.2"))
    }

    @Test
    fun isNewer_higherMinor_returnsTrue() {
        assertTrue(AppUpdater.isNewer("2.2.0", "2.1.2"))
    }

    @Test
    fun isNewer_higherMajor_returnsTrue() {
        assertTrue(AppUpdater.isNewer("v3.0.0", "2.1.2"))
    }

    @Test
    fun isNewer_forkBuildNumber_comparesFourthPart() {
        assertTrue(AppUpdater.isNewer("v2.4.6.2", "2.4.6.1"))
        assertTrue(AppUpdater.isNewer("v2.4.7.1", "2.4.6.9"))
        assertFalse(AppUpdater.isNewer("v2.4.6.1", "2.4.6.1"))
    }

    @Test
    fun parseSyncProblemUrl_readsFirstOpenIssue() {
        assertEquals(
            "https://github.com/o/r/issues/7",
            AppUpdater.parseSyncProblemUrl("""[{"number":7,"html_url":"https://github.com/o/r/issues/7"}]"""),
        )
        assertEquals(null, AppUpdater.parseSyncProblemUrl("[]"))
        assertEquals(null, AppUpdater.parseSyncProblemUrl("not json"))
    }

    @Test
    fun isNewer_sameVersion_returnsFalse() {
        assertFalse(AppUpdater.isNewer("v2.1.2", "2.1.2"))
        assertFalse(AppUpdater.isNewer("2.1.2", "2.1.2"))
    }

    @Test
    fun isNewer_olderVersion_returnsFalse() {
        assertFalse(AppUpdater.isNewer("v2.1.1", "2.1.2"))
        assertFalse(AppUpdater.isNewer("1.9.9", "2.1.2"))
    }

    @Test
    fun isNewer_extraSegments_comparesCorrectly() {
        assertTrue(AppUpdater.isNewer("v2.1.2.1", "2.1.2"))
        assertFalse(AppUpdater.isNewer("2.1", "2.1.2"))
    }

    @Test
    fun isNewer_handlesPrereleaseSuffix() {
        assertFalse(AppUpdater.isNewer("v2.1.2-beta1", "2.1.2"))
        assertTrue(AppUpdater.isNewer("v2.1.3-rc1", "2.1.2"))
    }

    @Test
    fun parseRelease_validNewReleaseWithApk_parsesSuccessfully() {
        val json = """
            {
                "tag_name": "v2.1.3",
                "name": "WoW Forever v2.1.3",
                "body": "Bug fixes and improvements",
                "assets": [
                    {
                        "name": "WoW-Forever.apk",
                        "browser_download_url": "https://github.com/jaredgei/wow-forever-android/releases/download/v2.1.3/WoW-Forever.apk",
                        "size": 160000000
                    }
                ]
            }
        """.trimIndent()

        val release = AppUpdater.parseRelease(json, "2.1.2")

        assertNotNull(release)
        assertEquals("2.1.3", release?.version)
        assertEquals("WoW Forever v2.1.3", release?.name)
        assertEquals("Bug fixes and improvements", release?.notes)
        assertEquals("https://github.com/jaredgei/wow-forever-android/releases/download/v2.1.3/WoW-Forever.apk", release?.downloadUrl)
        assertEquals(160000000L, release?.sizeBytes)
    }

    @Test
    fun parseRelease_sameOrOlderVersion_returnsNull() {
        val json = """
            {
                "tag_name": "v2.1.2",
                "name": "WoW Forever v2.1.2",
                "body": "Existing release",
                "assets": [
                    {
                        "name": "WoW-Forever.apk",
                        "browser_download_url": "https://github.com/jaredgei/wow-forever-android/releases/download/v2.1.2/WoW-Forever.apk",
                        "size": 160000000
                    }
                ]
            }
        """.trimIndent()

        assertNull(AppUpdater.parseRelease(json, "2.1.2"))
    }

    @Test
    fun parseRelease_noApkAsset_returnsNull() {
        val json = """
            {
                "tag_name": "v2.1.3",
                "name": "WoW Forever v2.1.3",
                "body": "Source only release",
                "assets": [
                    {
                        "name": "source.tar.gz",
                        "browser_download_url": "https://example.com/source.tar.gz",
                        "size": 5000
                    }
                ]
            }
        """.trimIndent()

        assertNull(AppUpdater.parseRelease(json, "2.1.2"))
    }

    @Test
    fun parseRelease_invalidJson_returnsNull() {
        assertNull(AppUpdater.parseRelease("not-json", "2.1.2"))
    }
}
