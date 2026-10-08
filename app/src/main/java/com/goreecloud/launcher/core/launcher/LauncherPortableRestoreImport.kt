package com.goreecloud.launcher.core.launcher

import com.goreecloud.launcher.core.workspace.WorkspacePortableSnapshot
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Minimal combined persistence authority for the two currently approved Launcher portability
 * subsets.
 *
 * A concrete writer receives both already-validated and pair-compatible values in one call so
 * Room/DataStore implementations can later provide a real transaction boundary without this
 * coordinator gaining package/profile discovery, widget rebinding, folder/dock reconstruction, or
 * broader HOME state authority.
 */
interface LauncherPortableRestoreWriter {
    suspend fun replacePortableState(
        workspace: WorkspacePortableSnapshot.Snapshot,
        preferences: LauncherPreferences,
    )
}

object LauncherPortableRestoreImport {
    private const val REVIEW_TOKEN_DOMAIN = "goreecloud-launcher-portable-restore-review/1"

    enum class RejectionSource {
        WORKSPACE,
        PREFERENCES,
        COMPATIBILITY,
        REVIEW_CHANGED,
    }

    sealed interface ValidationResult {
        data class Ready(
            val workspace: WorkspacePortableSnapshot.Snapshot,
            val preferences: LauncherPreferences,
            val reviewToken: String,
        ) : ValidationResult

        data class Rejected(
            val source: RejectionSource,
            val reason: String,
        ) : ValidationResult
    }

    sealed interface ApplyResult {
        data class Applied(
            val workspace: WorkspacePortableSnapshot.Snapshot,
            val preferences: LauncherPreferences,
        ) : ApplyResult

        data class Rejected(
            val source: RejectionSource,
            val reason: String,
        ) : ApplyResult
    }

    /**
     * Decode both complete snapshots and validate their shared Home-grid contract before any
     * persistence authority is granted.
     *
     * The returned review token binds the exact validated workspace/preference byte pair to a
     * privacy-minimized opaque SHA-256 value. It is not authenticity or provenance evidence; it is
     * only a local review/apply consistency guard.
     */
    fun validate(
        workspaceEncoded: String,
        preferencesEncoded: String,
    ): ValidationResult {
        val workspace = when (val decoded = WorkspacePortableSnapshot.decode(workspaceEncoded)) {
            is WorkspacePortableSnapshot.DecodeResult.Invalid -> {
                return ValidationResult.Rejected(RejectionSource.WORKSPACE, decoded.reason)
            }
            is WorkspacePortableSnapshot.DecodeResult.Success -> decoded.snapshot
        }

        val preferences = when (val decoded = LauncherPortablePreferences.decode(preferencesEncoded)) {
            is LauncherPortablePreferences.DecodeResult.Invalid -> {
                return ValidationResult.Rejected(RejectionSource.PREFERENCES, decoded.reason)
            }
            is LauncherPortablePreferences.DecodeResult.Success -> decoded.preferences
        }

        if (
            workspace.grid.columns != preferences.homeColumns ||
            workspace.grid.rows != preferences.homeRows
        ) {
            return ValidationResult.Rejected(
                RejectionSource.COMPATIBILITY,
                "workspace grid ${workspace.grid.columns}x${workspace.grid.rows} does not match " +
                    "portable Home grid ${preferences.homeColumns}x${preferences.homeRows}",
            )
        }

        return ValidationResult.Ready(
            workspace = workspace,
            preferences = preferences,
            reviewToken = reviewToken(workspaceEncoded, preferencesEncoded),
        )
    }

    /**
     * Validate both complete snapshots and their shared contract before granting any persistence
     * call.
     *
     * This method remains the non-reviewed Development apply seam. A future user-facing workflow
     * that shows [LauncherPortableRestorePreview] must use [applyReviewed] with the token returned
     * by that preview so changed inputs fail closed before the writer is invoked.
     */
    suspend fun apply(
        workspaceEncoded: String,
        preferencesEncoded: String,
        writer: LauncherPortableRestoreWriter,
    ): ApplyResult = when (val validation = validate(workspaceEncoded, preferencesEncoded)) {
        is ValidationResult.Rejected -> ApplyResult.Rejected(validation.source, validation.reason)
        is ValidationResult.Ready -> applyValidated(validation, writer)
    }

    /**
     * Apply only when the exact validated input pair is still the pair that was reviewed.
     *
     * The review token is deliberately not a signature, ownership proof, or Everkeep provenance
     * claim. It closes only the local time-of-review/time-of-apply substitution gap.
     */
    suspend fun applyReviewed(
        workspaceEncoded: String,
        preferencesEncoded: String,
        expectedReviewToken: String,
        writer: LauncherPortableRestoreWriter,
    ): ApplyResult = when (val validation = validate(workspaceEncoded, preferencesEncoded)) {
        is ValidationResult.Rejected -> ApplyResult.Rejected(validation.source, validation.reason)
        is ValidationResult.Ready -> {
            if (!constantTimeEquals(validation.reviewToken, expectedReviewToken)) {
                ApplyResult.Rejected(
                    RejectionSource.REVIEW_CHANGED,
                    "portable restore input changed after review",
                )
            } else {
                applyValidated(validation, writer)
            }
        }
    }

    private suspend fun applyValidated(
        validation: ValidationResult.Ready,
        writer: LauncherPortableRestoreWriter,
    ): ApplyResult {
        writer.replacePortableState(validation.workspace, validation.preferences)
        return ApplyResult.Applied(validation.workspace, validation.preferences)
    }

    private fun reviewToken(workspaceEncoded: String, preferencesEncoded: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        updateLengthDelimited(digest, REVIEW_TOKEN_DOMAIN)
        updateLengthDelimited(digest, workspaceEncoded)
        updateLengthDelimited(digest, preferencesEncoded)
        return digest.digest().joinToString(separator = "") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }
    }

    private fun updateLengthDelimited(digest: MessageDigest, value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        digest.update(bytes.size.toString().toByteArray(StandardCharsets.US_ASCII))
        digest.update(0.toByte())
        digest.update(bytes)
        digest.update(0.toByte())
    }

    private fun constantTimeEquals(actual: String, expected: String): Boolean =
        MessageDigest.isEqual(
            actual.toByteArray(StandardCharsets.US_ASCII),
            expected.toByteArray(StandardCharsets.US_ASCII),
        )
}
