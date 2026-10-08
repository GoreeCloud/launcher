package com.goreecloud.launcher.core.workspace

import android.content.Context
import android.content.pm.LauncherActivityInfo
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.workspaceDataStore by preferencesDataStore(name = "launcher_workspace")
// Compatibility ceiling only: GoreeCloud Launcher has no product-level Dock item cap.
const val MAX_DOCK_ITEMS = Int.MAX_VALUE

data class WorkspaceState(
    val initialized: Boolean = false,
    val favoriteKeys: List<String> = emptyList(),
    val dockKeys: List<String> = emptyList(),
    val authority: WorkspaceAuthority = WorkspaceAuthority.DATASTORE,
    val verifiedRoomFingerprint: String? = null,
)

enum class WorkspaceMoveDirection(val offset: Int) {
    EARLIER(-1),
    LATER(1),
}

fun LauncherActivityInfo.workspaceKey(): String =
    "${user.hashCode()}:${componentName.flattenToString()}"

internal object WorkspaceCodec {
    private const val ENTRY_SEPARATOR = "\u001F"

    fun encode(values: List<String>): String = values.joinToString(ENTRY_SEPARATOR)

    fun decode(value: String?): List<String> =
        value.orEmpty().split(ENTRY_SEPARATOR).filter { it.isNotBlank() }

    fun toggled(values: List<String>, key: String, limit: Int? = null): List<String> {
        val current = values.toMutableList()
        if (current.remove(key)) return current
        if (limit == null || current.size < limit) current.add(key)
        return current
    }

    fun moved(values: List<String>, key: String, direction: WorkspaceMoveDirection): List<String> {
        val fromIndex = values.indexOf(key)
        if (fromIndex == -1 || values.size < 2) return values

        val toIndex = (fromIndex + direction.offset).coerceIn(0, values.lastIndex)
        if (fromIndex == toIndex) return values

        return values.toMutableList().apply {
            removeAt(fromIndex)
            add(toIndex, key)
        }
    }

    fun movedToTarget(values: List<String>, key: String, targetKey: String): List<String> {
        if (key == targetKey || values.size < 2) return values

        val fromIndex = values.indexOf(key)
        val targetIndex = values.indexOf(targetKey)
        if (fromIndex == -1 || targetIndex == -1) return values

        return values.toMutableList().apply {
            val entry = removeAt(fromIndex)
            add(targetIndex.coerceIn(0, size), entry)
        }
    }
}

class WorkspaceRepository(
    private val dataStore: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.workspaceDataStore)

    private object Keys {
        val initialized = booleanPreferencesKey("initialized")
        val favorites = stringPreferencesKey("favorites")
        val dock = stringPreferencesKey("dock")
        val authority = stringPreferencesKey("workspace_authority")
        val verifiedRoomFingerprint = stringPreferencesKey("verified_room_fingerprint")
    }

    val state: Flow<WorkspaceState> = dataStore.data
        .map(::decodeState)
        .distinctUntilChanged()

    suspend fun ensureDefaults(favoriteKeys: List<String>, dockKeys: List<String>) {
        dataStore.edit { preferences ->
            if (authorityOf(preferences) == WorkspaceAuthority.ROOM) return@edit
            if (preferences[Keys.initialized] == true) return@edit
            invalidateRoomVerification(preferences)
            preferences[Keys.favorites] = WorkspaceCodec.encode(favoriteKeys.distinct())
            preferences[Keys.dock] = WorkspaceCodec.encode(
                dockKeys.distinct().take(MAX_DOCK_ITEMS)
            )
            preferences[Keys.initialized] = true
        }
    }

    suspend fun toggleFavorite(key: String) {
        dataStore.edit { preferences ->
            if (authorityOf(preferences) == WorkspaceAuthority.ROOM) return@edit
            invalidateRoomVerification(preferences)
            val current = WorkspaceCodec.decode(preferences[Keys.favorites])
            preferences[Keys.favorites] = WorkspaceCodec.encode(WorkspaceCodec.toggled(current, key))
            preferences[Keys.initialized] = true
        }
    }

    suspend fun toggleDock(key: String) {
        dataStore.edit { preferences ->
            if (authorityOf(preferences) == WorkspaceAuthority.ROOM) return@edit
            invalidateRoomVerification(preferences)
            val current = WorkspaceCodec.decode(preferences[Keys.dock])
            preferences[Keys.dock] = WorkspaceCodec.encode(
                WorkspaceCodec.toggled(current, key, MAX_DOCK_ITEMS)
            )
            preferences[Keys.initialized] = true
        }
    }

    suspend fun moveFavorite(key: String, direction: WorkspaceMoveDirection) {
        dataStore.edit { preferences ->
            if (authorityOf(preferences) == WorkspaceAuthority.ROOM) return@edit
            invalidateRoomVerification(preferences)
            val current = WorkspaceCodec.decode(preferences[Keys.favorites])
            preferences[Keys.favorites] = WorkspaceCodec.encode(
                WorkspaceCodec.moved(current, key, direction)
            )
            preferences[Keys.initialized] = true
        }
    }

    suspend fun moveDock(key: String, direction: WorkspaceMoveDirection) {
        dataStore.edit { preferences ->
            if (authorityOf(preferences) == WorkspaceAuthority.ROOM) return@edit
            invalidateRoomVerification(preferences)
            val current = WorkspaceCodec.decode(preferences[Keys.dock])
            preferences[Keys.dock] = WorkspaceCodec.encode(
                WorkspaceCodec.moved(current, key, direction)
            )
            preferences[Keys.initialized] = true
        }
    }

    suspend fun moveFavoriteToTarget(key: String, targetKey: String) {
        dataStore.edit { preferences ->
            if (authorityOf(preferences) == WorkspaceAuthority.ROOM) return@edit
            invalidateRoomVerification(preferences)
            val current = WorkspaceCodec.decode(preferences[Keys.favorites])
            preferences[Keys.favorites] = WorkspaceCodec.encode(
                WorkspaceCodec.movedToTarget(current, key, targetKey)
            )
            preferences[Keys.initialized] = true
        }
    }

    suspend fun moveDockToTarget(key: String, targetKey: String) {
        dataStore.edit { preferences ->
            if (authorityOf(preferences) == WorkspaceAuthority.ROOM) return@edit
            invalidateRoomVerification(preferences)
            val current = WorkspaceCodec.decode(preferences[Keys.dock])
            preferences[Keys.dock] = WorkspaceCodec.encode(
                WorkspaceCodec.movedToTarget(current, key, targetKey)
            )
            preferences[Keys.initialized] = true
        }
    }

    suspend fun moveHomeToDock(key: String, targetDockKey: String?) {
        mutateDragPlacement { current ->
            WorkspaceDragPlacementPolicy.moveHomeToDock(
                favoriteKeys = current.favoriteKeys,
                dockKeys = current.dockKeys,
                key = key,
                targetDockKey = targetDockKey,
            )
        }
    }

    suspend fun moveDockToHome(key: String, homeLimit: Int) {
        mutateDragPlacement { current ->
            WorkspaceDragPlacementPolicy.moveDockToHome(
                favoriteKeys = current.favoriteKeys,
                dockKeys = current.dockKeys,
                key = key,
                homeLimit = homeLimit,
            )
        }
    }

    suspend fun copyDrawerToHome(key: String, homeLimit: Int) {
        mutateDragPlacement { current ->
            WorkspaceDragPlacementPolicy.copyDrawerToHome(
                favoriteKeys = current.favoriteKeys,
                dockKeys = current.dockKeys,
                key = key,
                homeLimit = homeLimit,
            )
        }
    }

    suspend fun copyDrawerToDock(key: String, targetDockKey: String?) {
        mutateDragPlacement { current ->
            WorkspaceDragPlacementPolicy.copyDrawerToDock(
                favoriteKeys = current.favoriteKeys,
                dockKeys = current.dockKeys,
                key = key,
                targetDockKey = targetDockKey,
            )
        }
    }

    suspend fun reorderDockByDrop(key: String, targetDockKey: String?) {
        mutateDragPlacement { current ->
            WorkspaceDragPlacementPolicy.reorderDock(
                favoriteKeys = current.favoriteKeys,
                dockKeys = current.dockKeys,
                key = key,
                targetDockKey = targetDockKey,
            )
        }
    }

    suspend fun markRoomVerified(expectedState: WorkspaceState): Boolean {
        if (!expectedState.initialized) return false
        val expectedFingerprint = WorkspaceSnapshotFingerprint.of(expectedState)
        var marked = false

        dataStore.edit { preferences ->
            if (authorityOf(preferences) == WorkspaceAuthority.ROOM) {
                return@edit
            }

            if (WorkspaceSnapshotFingerprint.of(decodeState(preferences)) == expectedFingerprint) {
                preferences[Keys.authority] = WorkspaceAuthorityCodec.encode(WorkspaceAuthority.ROOM_VERIFIED)
                preferences[Keys.verifiedRoomFingerprint] = expectedFingerprint
                marked = true
            } else {
                invalidateRoomVerification(preferences)
            }
        }

        return marked
    }

    suspend fun markDataStoreAuthoritative() {
        dataStore.edit { preferences ->
            invalidateRoomVerification(preferences)
        }
    }

    suspend fun promoteRoomAuthority(expectedState: WorkspaceState): Boolean {
        if (!expectedState.initialized) return false
        val expectedFingerprint = WorkspaceSnapshotFingerprint.of(expectedState)
        var promoted = false

        dataStore.edit { preferences ->
            val currentAuthority = authorityOf(preferences)
            if (currentAuthority == WorkspaceAuthority.ROOM) {
                promoted = true
                return@edit
            }

            val currentFingerprint = WorkspaceSnapshotFingerprint.of(decodeState(preferences))
            val verifiedFingerprint = preferences[Keys.verifiedRoomFingerprint]
            if (
                currentAuthority == WorkspaceAuthority.ROOM_VERIFIED &&
                currentFingerprint == expectedFingerprint &&
                verifiedFingerprint == expectedFingerprint
            ) {
                preferences[Keys.authority] = WorkspaceAuthorityCodec.encode(WorkspaceAuthority.ROOM)
                promoted = true
            } else {
                invalidateRoomVerification(preferences)
            }
        }

        return promoted
    }

    private suspend fun mutateDragPlacement(
        transform: (WorkspaceDragPlacement) -> WorkspaceDragPlacement,
    ) {
        dataStore.edit { preferences ->
            if (authorityOf(preferences) == WorkspaceAuthority.ROOM) return@edit
            invalidateRoomVerification(preferences)
            val current = WorkspaceDragPlacement(
                favoriteKeys = WorkspaceCodec.decode(preferences[Keys.favorites]),
                dockKeys = WorkspaceCodec.decode(preferences[Keys.dock]),
            )
            val updated = transform(current)
            preferences[Keys.favorites] = WorkspaceCodec.encode(updated.favoriteKeys.distinct())
            preferences[Keys.dock] = WorkspaceCodec.encode(
                updated.dockKeys.distinct().take(MAX_DOCK_ITEMS)
            )
            preferences[Keys.initialized] = true
        }
    }

    private fun decodeState(preferences: Preferences): WorkspaceState = WorkspaceState(
        initialized = preferences[Keys.initialized] ?: false,
        favoriteKeys = WorkspaceCodec.decode(preferences[Keys.favorites]),
        dockKeys = WorkspaceCodec.decode(preferences[Keys.dock]),
        authority = WorkspaceAuthorityCodec.decode(preferences[Keys.authority]),
        verifiedRoomFingerprint = preferences[Keys.verifiedRoomFingerprint],
    )

    private fun authorityOf(preferences: Preferences): WorkspaceAuthority =
        WorkspaceAuthorityCodec.decode(preferences[Keys.authority])

    private fun invalidateRoomVerification(preferences: MutablePreferences) {
        if (authorityOf(preferences) == WorkspaceAuthority.ROOM) return
        preferences[Keys.authority] = WorkspaceAuthorityCodec.encode(WorkspaceAuthority.DATASTORE)
        preferences.remove(Keys.verifiedRoomFingerprint)
    }
}
