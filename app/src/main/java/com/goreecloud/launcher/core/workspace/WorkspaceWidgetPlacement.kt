package com.goreecloud.launcher.core.workspace

sealed interface WorkspaceWidgetDescriptor {
    data class BuiltIn(val typeId: String) : WorkspaceWidgetDescriptor
    data class Android(
        val appWidgetId: Int,
        val providerComponent: String,
    ) : WorkspaceWidgetDescriptor
}

object WorkspaceWidgetKeyCodec {
    private const val PREFIX = "widget|v1|"
    private const val BUILT_IN = "builtin"
    private const val ANDROID = "android"

    fun encode(descriptor: WorkspaceWidgetDescriptor): String = when (descriptor) {
        is WorkspaceWidgetDescriptor.BuiltIn -> {
            require(descriptor.typeId.isNotBlank() && '|' !in descriptor.typeId)
            "${PREFIX}${BUILT_IN}|${descriptor.typeId}"
        }
        is WorkspaceWidgetDescriptor.Android -> {
            require(descriptor.appWidgetId > 0)
            require(descriptor.providerComponent.isNotBlank() && '|' !in descriptor.providerComponent)
            "${PREFIX}${ANDROID}|${descriptor.appWidgetId}|${descriptor.providerComponent}"
        }
    }

    fun decode(raw: String?): WorkspaceWidgetDescriptor? {
        if (raw.isNullOrBlank() || !raw.startsWith(PREFIX)) return null
        val parts = raw.split('|')
        if (parts.size < 4 || parts[0] != "widget" || parts[1] != "v1") return null
        return when (parts[2]) {
            BUILT_IN -> {
                if (parts.size != 4 || parts[3].isBlank()) null
                else WorkspaceWidgetDescriptor.BuiltIn(parts[3])
            }
            ANDROID -> {
                if (parts.size != 5) return null
                val id = parts[3].toIntOrNull()?.takeIf { it > 0 } ?: return null
                val provider = parts[4].takeIf { it.isNotBlank() } ?: return null
                WorkspaceWidgetDescriptor.Android(id, provider)
            }
            else -> null
        }
    }
}

object WorkspaceWidgetCatalog {
    const val CLOCK = "goreecloud.clock"
    const val COMPACT_CLOCK = "goreecloud.clock-compact"
    const val ANALOG_CLOCK = "goreecloud.clock-analog"
    const val DATE = "goreecloud.date"
    const val SEARCH = "goreecloud.search"
    const val QUICK_ACTIONS = "goreecloud.quick-actions"
    const val BATTERY = "goreecloud.battery"
    const val LAUNCHER_STATUS = "goreecloud.launcher-status"

    val builtInTypeIds: Set<String> = linkedSetOf(
        SEARCH,
        QUICK_ACTIONS,
        BATTERY,
        DATE,
        CLOCK,
        COMPACT_CLOCK,
        ANALOG_CLOCK,
        LAUNCHER_STATUS,
    )

    fun defaultSpan(typeId: String): Pair<Int, Int>? = when (typeId) {
        CLOCK -> 2 to 2
        COMPACT_CLOCK -> 2 to 1
        ANALOG_CLOCK -> 2 to 2
        DATE -> 2 to 1
        SEARCH -> 4 to 1
        QUICK_ACTIONS -> 4 to 2
        BATTERY -> 2 to 1
        LAUNCHER_STATUS -> 2 to 1
        else -> null
    }

    fun displayName(typeId: String): String = when (typeId) {
        CLOCK -> "Digital clock"
        COMPACT_CLOCK -> "Compact clock"
        ANALOG_CLOCK -> "Analog clock"
        DATE -> "Date"
        SEARCH -> "Universal Search"
        QUICK_ACTIONS -> "Quick actions"
        BATTERY -> "Battery"
        LAUNCHER_STATUS -> "Launcher Status"
        else -> "GoreeCloud widget"
    }

    fun description(typeId: String): String = when (typeId) {
        CLOCK -> "Time and date with a roomy glance layout."
        COMPACT_CLOCK -> "A compact time-first widget for tighter Home layouts."
        ANALOG_CLOCK -> "A quiet analog clock with Glaze styling."
        DATE -> "Day, date, and month at a glance."
        SEARCH -> "Open Launcher Universal Search from Home."
        QUICK_ACTIONS -> "Fast access to Apps, Search, Edit Home, and Settings."
        BATTERY -> "Local battery level and charging state with no extra permission."
        LAUNCHER_STATUS -> "Local Launcher readiness and operating state."
        else -> "GoreeCloud widget"
    }

    fun matchesQuery(typeId: String, query: String): Boolean {
        val needle = query.trim()
        if (needle.isEmpty()) return true
        return displayName(typeId).contains(needle, ignoreCase = true) ||
            description(typeId).contains(needle, ignoreCase = true) ||
            typeId.contains(needle, ignoreCase = true)
    }
}

object WorkspaceWidgetPlacementPolicy {
    fun firstAvailable(
        grid: WorkspaceGridPlacement.Grid,
        existing: List<WorkspaceGridPlacement.Placement>,
        itemId: String,
        spanX: Int,
        spanY: Int,
    ): WorkspaceGridPlacement.Placement? {
        if (itemId.isBlank() || spanX <= 0 || spanY <= 0) return null
        if (spanX > grid.columns || spanY > grid.rows) return null
        for (cellY in 0..(grid.rows - spanY)) {
            for (cellX in 0..(grid.columns - spanX)) {
                val candidate = WorkspaceGridPlacement.Placement(
                    itemId = itemId,
                    cellX = cellX,
                    cellY = cellY,
                    spanX = spanX,
                    spanY = spanY,
                )
                if (
                    WorkspaceGridPlacement.validate(grid, existing + candidate) ==
                    WorkspaceGridPlacement.Validation.Valid
                ) {
                    return candidate
                }
            }
        }
        return null
    }

    fun move(
        grid: WorkspaceGridPlacement.Grid,
        existing: List<WorkspaceGridPlacement.Placement>,
        itemId: String,
        cellX: Int,
        cellY: Int,
    ): WorkspaceGridPlacement.Placement? {
        val current = existing.singleOrNull { it.itemId == itemId } ?: return null
        val updated = current.copy(cellX = cellX, cellY = cellY)
        val next = existing.map { if (it.itemId == itemId) updated else it }
        return if (
            WorkspaceGridPlacement.validate(grid, next) == WorkspaceGridPlacement.Validation.Valid
        ) updated else null
    }

    fun resize(
        grid: WorkspaceGridPlacement.Grid,
        existing: List<WorkspaceGridPlacement.Placement>,
        itemId: String,
        spanX: Int,
        spanY: Int,
    ): WorkspaceGridPlacement.Placement? {
        val current = existing.singleOrNull { it.itemId == itemId } ?: return null
        if (spanX <= 0 || spanY <= 0) return null
        val updated = current.copy(spanX = spanX, spanY = spanY)
        val next = existing.map { if (it.itemId == itemId) updated else it }
        return if (
            WorkspaceGridPlacement.validate(grid, next) == WorkspaceGridPlacement.Validation.Valid
        ) updated else null
    }
}
