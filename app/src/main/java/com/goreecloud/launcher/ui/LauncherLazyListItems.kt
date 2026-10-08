package com.goreecloud.launcher.ui

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable

/**
 * Typed List overload used by Launcher-owned lazy row surfaces.
 *
 * Compose's core LazyListScope overload is count-based; this adapter keeps stable item identity
 * explicit for row-backed Launcher surfaces without relying on an import alias at each call site.
 */
internal fun <T> LazyListScope.items(
    items: List<T>,
    key: ((T) -> Any)? = null,
    contentType: (T) -> Any? = { null },
    itemContent: @Composable LazyItemScope.(T) -> Unit,
) {
    items(
        count = items.size,
        key = if (key == null) null else { index -> key(items[index]) },
        contentType = { index -> contentType(items[index]) },
    ) { index ->
        itemContent(items[index])
    }
}
