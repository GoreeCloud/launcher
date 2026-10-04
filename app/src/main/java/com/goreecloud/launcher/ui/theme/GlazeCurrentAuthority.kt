package com.goreecloud.launcher.ui.theme

/**
 * Current Glaze shared authority and Launcher adoption boundary.
 *
 * Glaze V1.7 / 1.7.0 is the current Anchor shared release. Its bounded runtime intentionally
 * inherits accepted V1.6.0 behavior, so Launcher's V1.6 presentation implementation remains
 * valid provenance but is not automatically V1.7 consumer acceptance.
 */
object GlazeCurrentAuthority {
    const val currentRequiredVersion = "1.7.0"
    const val currentReleaseIntegrationRevision = "1a5756daed2294155be2e9972b24f580f6222b7b"
    const val currentQualificationAnchor = "7c4ded83d7a8725165bb6a55dfb175667cc9589e"
    const val inheritedAcceptedRuntimeVersion = "1.6.0"
    const val inheritedAcceptedRuntimeSourceRevision = "a7180679ea851389e0f3004515f9a25f420e716d"

    const val implementedBaselineVersion = GlazeMetrics.targetVersion
    const val implementedBaselineSourceRevision = GlazeMetrics.sourceRevision

    const val currentConsumerConformanceEstablished = false

    fun sourceMigrationRequired(): Boolean =
        implementedBaselineVersion != currentRequiredVersion

    fun consumerAcceptanceRequired(): Boolean =
        !currentConsumerConformanceEstablished

    /**
     * Compatibility helper retained for callers/tests that ask whether current-authority Glaze work
     * remains. Runtime behavior may be inherited while contract migration and application acceptance remain required.
     */
    fun migrationRequired(): Boolean =
        sourceMigrationRequired() || consumerAcceptanceRequired()
}
