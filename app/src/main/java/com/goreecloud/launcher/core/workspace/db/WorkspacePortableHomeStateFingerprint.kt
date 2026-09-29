package com.goreecloud.launcher.core.workspace.db

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Deterministic fingerprint of the complete bounded HOME relational state used by portable restore.
 *
 * This is device-local recovery evidence, not an artifact signature or cross-device identity. It
 * deliberately includes every persisted page/item field, including appKey, so a placement-only
 * match cannot hide an application/profile identity change.
 */
internal object WorkspacePortableHomeStateFingerprint {
    private const val DOMAIN = "goreecloud-launcher-portable-home-state/1"

    fun of(
        pages: List<WorkspacePageEntity>,
        items: List<WorkspaceItemEntity>,
    ): String {
        val canonicalPages = pages.sortedWith(compareBy({ it.containerType }, { it.rank }, { it.pageId }))
        val canonicalItems = items.sortedWith(compareBy({ it.pageId }, { it.rank }, { it.itemId }))

        check(canonicalPages.map { it.pageId }.distinct().size == canonicalPages.size) {
            "portable HOME fingerprint requires unique page identities"
        }
        check(canonicalItems.map { it.itemId }.distinct().size == canonicalItems.size) {
            "portable HOME fingerprint requires unique item identities"
        }

        val digest = MessageDigest.getInstance("SHA-256")
        update(digest, DOMAIN)
        update(digest, canonicalPages.size.toString())
        canonicalPages.forEach { page ->
            update(digest, "page")
            update(digest, page.pageId)
            update(digest, page.containerType)
            update(digest, page.rank.toString())
        }

        update(digest, canonicalItems.size.toString())
        canonicalItems.forEach { item ->
            update(digest, "item")
            update(digest, item.itemId)
            update(digest, item.pageId)
            update(digest, item.itemType)
            updateNullable(digest, item.appKey)
            update(digest, item.rank.toString())
            updateNullable(digest, item.cellX?.toString())
            updateNullable(digest, item.cellY?.toString())
            update(digest, item.spanX.toString())
            update(digest, item.spanY.toString())
        }

        return digest.digest().joinToString(separator = "") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }
    }

    private fun updateNullable(digest: MessageDigest, value: String?) {
        if (value == null) {
            update(digest, "null")
        } else {
            update(digest, "value")
            update(digest, value)
        }
    }

    private fun update(digest: MessageDigest, value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        digest.update(bytes.size.toString().toByteArray(StandardCharsets.US_ASCII))
        digest.update(0.toByte())
        digest.update(bytes)
        digest.update(0.toByte())
    }
}
