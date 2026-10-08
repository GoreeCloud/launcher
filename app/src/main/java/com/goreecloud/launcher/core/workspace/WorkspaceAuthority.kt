package com.goreecloud.launcher.core.workspace

import java.security.MessageDigest

enum class WorkspaceAuthority {
    DATASTORE,
    ROOM_VERIFIED,
    ROOM,
}

internal object WorkspaceAuthorityCodec {
    fun encode(authority: WorkspaceAuthority): String = authority.name

    fun decode(value: String?): WorkspaceAuthority =
        WorkspaceAuthority.entries.firstOrNull { it.name == value } ?: WorkspaceAuthority.DATASTORE
}

internal object WorkspaceSnapshotFingerprint {
    private const val ALGORITHM = "SHA-256"
    private val hex = "0123456789abcdef".toCharArray()

    fun of(state: WorkspaceState): String = of(
        initialized = state.initialized,
        favoriteKeys = state.favoriteKeys,
        dockKeys = state.dockKeys,
    )

    fun of(
        initialized: Boolean,
        favoriteKeys: List<String>,
        dockKeys: List<String>,
    ): String {
        val digest = MessageDigest.getInstance(ALGORITHM)
        digest.update(if (initialized) 1.toByte() else 0.toByte())
        updateList(digest, favoriteKeys)
        updateList(digest, dockKeys)
        return digest.digest().toHex()
    }

    private fun updateList(digest: MessageDigest, values: List<String>) {
        digest.updateInt(values.size)
        values.forEach { value ->
            val bytes = value.toByteArray(Charsets.UTF_8)
            digest.updateInt(bytes.size)
            digest.update(bytes)
        }
    }

    private fun MessageDigest.updateInt(value: Int) {
        update((value ushr 24).toByte())
        update((value ushr 16).toByte())
        update((value ushr 8).toByte())
        update(value.toByte())
    }

    private fun ByteArray.toHex(): String = buildString(size * 2) {
        this@toHex.forEach { byte ->
            val value = byte.toInt() and 0xff
            append(hex[value ushr 4])
            append(hex[value and 0x0f])
        }
    }
}
