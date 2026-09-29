package com.goreecloud.launcher.ui.theme

/**
 * Current-Stable Glaze UI authority and Launcher adoption boundary.
 *
 * The repository source mapping now targets the exact GLAZE UI V1.6 Stable release source.
 * Application acceptance remains separate: source mapping does not establish rendered,
 * accessibility, representative-device, performance, production, release, or Stable acceptance.
 */
object GlazeCurrentAuthority {
    const val currentRequiredVersion = "1.6.0"
    const val currentStableSourceRevision = "a7180679ea851389e0f3004515f9a25f420e716d"

    const val implementedBaselineVersion = GlazeMetrics.targetVersion
    const val implementedBaselineSourceRevision = GlazeMetrics.sourceRevision

    const val currentConsumerConformanceEstablished = false

    fun sourceMigrationRequired(): Boolean =
        implementedBaselineVersion != currentRequiredVersion ||
            implementedBaselineSourceRevision != currentStableSourceRevision

    fun consumerAcceptanceRequired(): Boolean =
        !currentConsumerConformanceEstablished

    /**
     * Compatibility helper retained for callers/tests that ask whether current-Stable Glaze work
     * remains. Source migration may be complete while application acceptance is still required.
     */
    fun migrationRequired(): Boolean =
        sourceMigrationRequired() || consumerAcceptanceRequired()
}
