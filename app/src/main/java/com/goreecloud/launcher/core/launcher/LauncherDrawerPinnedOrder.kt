package com.goreecloud.launcher.core.launcher

/**
 * Device-local App Drawer presentation metadata.
 *
 * Pin membership/order, hidden discovery identities, and App Lock membership use exact
 * profile-qualified workspace keys. This sidecar state remains intentionally outside portable
 * preference v1 so security-sensitive local state is not silently exported.
 */
data class LauncherDrawerPinnedState(
    val keys: Set<String>,
    val order: List<String>,
    val hiddenKeys: Set<String> = emptySet(),
    val lockedKeys: Set<String> = emptySet(),
)

internal object LauncherDrawerPinnedOrder {
    fun encode(keys: List<String>): String =
        keys.asSequence()
            .filter(String::isNotBlank)
            .distinct()
            .joinToString(separator = "") { key -> "${key.length}:$key" }

    fun decode(raw: String?): List<String> {
        if (raw.isNullOrEmpty()) return emptyList()
        val result = mutableListOf<String>()
        var cursor = 0
        while (cursor < raw.length) {
            val colon = raw.indexOf(':', cursor)
            if (colon <= cursor) return emptyList()
            val length = raw.substring(cursor, colon).toIntOrNull() ?: return emptyList()
            if (length <= 0 || length > 16_384) return emptyList()
            val start = colon + 1
            val end = start + length
            if (end > raw.length) return emptyList()
            val key = raw.substring(start, end)
            if (key.isBlank()) return emptyList()
            if (key !in result) result += key
            cursor = end
        }
        return result
    }

    fun reconcile(
        order: List<String>,
        pinnedKeys: Set<String>,
    ): List<String> {
        if (pinnedKeys.isEmpty()) return emptyList()
        val result = order.filter { it in pinnedKeys }.distinct().toMutableList()
        val seen = result.toHashSet()
        result += pinnedKeys.asSequence().filterNot(seen::contains).sorted()
        return result
    }

    fun move(
        order: List<String>,
        pinnedKeys: Set<String>,
        appKey: String,
        delta: Int,
    ): List<String> {
        val reconciled = reconcile(order, pinnedKeys).toMutableList()
        val from = reconciled.indexOf(appKey)
        if (from < 0) return reconciled
        val to = (from + delta).coerceIn(0, reconciled.lastIndex)
        if (from == to) return reconciled
        val value = reconciled.removeAt(from)
        reconciled.add(to, value)
        return reconciled
    }
}
