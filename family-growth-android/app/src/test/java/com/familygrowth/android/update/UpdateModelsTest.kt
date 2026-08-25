package com.familygrowth.android.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateModelsTest {
    @Test fun `semantic versions are parsed and ordered`() {
        assertTrue(SemanticVersion.parse("v1.2.4")!! > SemanticVersion.parse("1.2.3")!!)
        assertEquals("10.0.1", SemanticVersion.parse("v10.0.1").toString())
        assertNull(SemanticVersion.parse("v1.2"))
        assertNull(SemanticVersion.parse("1.02.3"))
    }

    @Test fun `only exact repository asset with digest is accepted`() {
        val version = SemanticVersion(0, 1, 1)
        val expected = ReleaseAsset(
            "family-growth-0.1.1.apk",
            "https://github.com/acme/family-growth/releases/download/v0.1.1/family-growth-0.1.1.apk",
            "sha256:${"a".repeat(64)}",
            1024,
        )
        assertEquals(expected, selectReleaseAsset(version, "acme/family-growth", listOf(expected)))
    }

    @Test fun `repository configuration must be an owner and repo pair`() {
        assertTrue(isValidGitHubRepository("acme/family-growth"))
        assertTrue(!isValidGitHubRepository(""))
        assertTrue(!isValidGitHubRepository("https://github.com/acme/family-growth"))
    }

    @Test fun `missing digest and foreign download host fail closed`() {
        val version = SemanticVersion(0, 1, 1)
        val base = ReleaseAsset(
            "family-growth-0.1.1.apk",
            "https://github.com/acme/family-growth/releases/download/v0.1.1/family-growth-0.1.1.apk",
            null,
            1024,
        )
        assertThrows(UpdateException::class.java) {
            selectReleaseAsset(version, "acme/family-growth", listOf(base))
        }
        assertThrows(UpdateException::class.java) {
            selectReleaseAsset(version, "acme/family-growth", listOf(base.copy(downloadUrl = "https://example.com/update.apk", digest = "sha256:${"b".repeat(64)}")))
        }
    }
}
