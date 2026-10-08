package com.goreecloud.launcher.ui

internal const val LAUNCHER_DRAWER_EAGER_GRID_MAX_ITEMS = 320

/**
 * Representative phones commonly expose well under 320 launchable activities per profile.
 * Keeping those cells composed while the drawer is open avoids OEM/Compose lazy-item recycling
 * defects that can detach individual icon/label nodes during a fast scroll. Very large enterprise
 * profiles still use virtualization so memory and composition cost stay bounded.
 */
internal fun launcherDrawerUsesEagerGrid(itemCount: Int): Boolean =
    itemCount in 0..LAUNCHER_DRAWER_EAGER_GRID_MAX_ITEMS

/**
 * Chunks drawer entries into deterministic rows without filtering or dropping entries.
 */
internal fun <T> launcherDrawerStableRows(
    items: List<T>,
    columns: Int,
): List<List<T>> = items.chunked(columns.coerceAtLeast(1))
