package com.goreecloud.launcher.core.launcher

/**
 * Read-only preflight for the currently approved combined Launcher portability subsets.
 *
 * This object has no Room, DataStore, package/profile, widget, HOME, or writer authority. It uses
 * the same complete decode and pair-compatibility validation as [LauncherPortableRestoreImport]
 * and exposes aggregate workspace geometry/counts, the seven reviewed preference values, and one
 * opaque review token that can bind a later apply to the exact reviewed input pair.
 */
object LauncherPortableRestorePreview {
    data class Summary(
        val gridColumns: Int,
        val gridRows: Int,
        val pageCount: Int,
        val itemCount: Int,
        val homeColumns: Int,
        val homeRows: Int,
        val drawerColumns: Int,
        val showLabels: Boolean,
        val iconScale: Float,
        val layoutLocked: Boolean,
        val universalSearchHomeMode: LauncherUniversalSearchHomeMode,
    )

    sealed interface Result {
        data class Ready(
            val summary: Summary,
            val reviewToken: String,
        ) : Result

        data class Rejected(
            val source: LauncherPortableRestoreImport.RejectionSource,
            val reason: String,
        ) : Result
    }

    fun inspect(
        workspaceEncoded: String,
        preferencesEncoded: String,
    ): Result = when (
        val validation = LauncherPortableRestoreImport.validate(
            workspaceEncoded,
            preferencesEncoded,
        )
    ) {
        is LauncherPortableRestoreImport.ValidationResult.Rejected -> Result.Rejected(
            validation.source,
            validation.reason,
        )

        is LauncherPortableRestoreImport.ValidationResult.Ready -> {
            val workspace = validation.workspace
            val preferences = validation.preferences
            Result.Ready(
                summary = Summary(
                    gridColumns = workspace.grid.columns,
                    gridRows = workspace.grid.rows,
                    pageCount = workspace.pages.size,
                    itemCount = workspace.pages.sumOf { it.placements.size },
                    homeColumns = preferences.homeColumns,
                    homeRows = preferences.homeRows,
                    drawerColumns = preferences.drawerColumns,
                    showLabels = preferences.showLabels,
                    iconScale = preferences.iconScale,
                    layoutLocked = preferences.layoutLocked,
                    universalSearchHomeMode = preferences.universalSearchHomeMode,
                ),
                reviewToken = validation.reviewToken,
            )
        }
    }
}
