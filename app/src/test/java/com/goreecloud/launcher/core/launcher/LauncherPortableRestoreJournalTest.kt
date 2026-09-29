package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherPortableRestoreJournalTest {
    @Test
    fun journalRoundTripsExactBeforeAndAfterState() {
        val journal = journal()
        val encoded = LauncherPortableRestoreJournalCodec.encode(journal)
        val decoded = LauncherPortableRestoreJournalCodec.decode(encoded)

        assertTrue(decoded is LauncherPortableRestoreJournalCodec.DecodeResult.Success)
        assertEquals(
            journal,
            (decoded as LauncherPortableRestoreJournalCodec.DecodeResult.Success).journal,
        )
    }

    @Test
    fun tamperedJournalFailsIntegrityBeforeRecoveryUse() {
        val encoded = LauncherPortableRestoreJournalCodec.encode(journal())
        val tampered = encoded.replace(
            "applied_workspace_sha256=${"b".repeat(64)}",
            "applied_workspace_sha256=${"c".repeat(64)}",
        )

        assertTrue(
            LauncherPortableRestoreJournalCodec.decode(tampered) is
                LauncherPortableRestoreJournalCodec.DecodeResult.Invalid,
        )
    }

    @Test
    fun expandedJournalRecordIsRejected() {
        val encoded = LauncherPortableRestoreJournalCodec.encode(journal())
        val expanded = encoded.replace(
            "target_preferences_b64=",
            "unexpected=true\ntarget_preferences_b64=",
        )

        assertTrue(
            LauncherPortableRestoreJournalCodec.decode(expanded) is
                LauncherPortableRestoreJournalCodec.DecodeResult.Invalid,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidWorkspaceFingerprintCannotBeEncoded() {
        LauncherPortableRestoreJournalCodec.encode(
            journal().copy(previousWorkspaceFingerprint = "not-a-fingerprint"),
        )
    }

    private fun journal(): LauncherPortableRestoreJournal = LauncherPortableRestoreJournal(
        transactionId = "11111111-2222-3333-4444-555555555555",
        previousWorkspaceFingerprint = "a".repeat(64),
        appliedWorkspaceFingerprint = "b".repeat(64),
        previousPreferences = LauncherPreferences(),
        targetPreferences = LauncherPreferences(
            drawerColumns = 4,
            showLabels = false,
            iconScale = 0.95f,
            layoutLocked = true,
            universalSearchHomeMode = LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY,
        ),
    )
}
