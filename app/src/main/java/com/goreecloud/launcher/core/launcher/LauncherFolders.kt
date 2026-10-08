package com.goreecloud.launcher.core.launcher

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.launcherFolderStore by preferencesDataStore(name = "launcher_folders")

data class LauncherFolder(
    val id: String,
    val name: String,
    val appKeys: List<String>,
    /**
     * Exact Android UserHandle identity used by this folder.
     *
     * Legacy v1 records do not contain this field and decode as null. The UI treats those legacy
     * records as primary-user folders only. New profile-aware folders always persist a concrete
     * profile identifier so work/private-profile membership never crosses profile boundaries.
     */
    val profileId: Int? = null,
)

internal fun launcherProfileIdFromWorkspaceKey(appKey: String): Int? =
    appKey.substringBefore(':', missingDelimiterValue = "").toIntOrNull()

object LauncherFolderProfilePolicy {
    fun belongsToProfile(
        folder: LauncherFolder,
        profileId: Int,
        primaryProfileId: Int,
    ): Boolean =
        folder.profileId?.let { it == profileId } ?: (profileId == primaryProfileId)

    fun canContainWorkspaceKey(
        folder: LauncherFolder,
        appKey: String,
        primaryProfileId: Int,
    ): Boolean {
        val appProfileId = launcherProfileIdFromWorkspaceKey(appKey) ?: return false
        return belongsToProfile(folder, appProfileId, primaryProfileId)
    }
}

object LauncherFolderPolicy {
    const val MAX_NAME_LENGTH = 40
    const val MAX_FOLDER_COUNT = 100
    const val MAX_APPS_PER_FOLDER = 100

    fun normalizeName(raw: String?): String? =
        raw
            ?.trim()
            ?.replace(Regex("\\s+"), " ")
            ?.take(MAX_NAME_LENGTH)
            ?.takeIf { it.isNotBlank() }
}

object LauncherFolderCodec {
    fun encode(folders: List<LauncherFolder>): String =
        folders.joinToString("\n") { folder ->
            listOf(
                encodePart(folder.id),
                encodePart(folder.name),
                encodePart(folder.profileId?.toString().orEmpty()),
                folder.appKeys
                    .distinct()
                    .take(LauncherFolderPolicy.MAX_APPS_PER_FOLDER)
                    .joinToString(",") { encodePart(it) },
            ).joinToString("\t")
        }

    fun decode(raw: String?): List<LauncherFolder> {
        if (raw.isNullOrBlank()) return emptyList()
        val seenIds = mutableSetOf<String>()
        return raw.lineSequence()
            .mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size !in 3..4) return@mapNotNull null
                val id = decodePart(parts[0])?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                if (!seenIds.add(id)) return@mapNotNull null
                val name = LauncherFolderPolicy.normalizeName(decodePart(parts[1]))
                    ?: return@mapNotNull null

                val profileId: Int?
                val appsField: String
                if (parts.size == 3) {
                    profileId = null
                    appsField = parts[2]
                } else {
                    val rawProfile = decodePart(parts[2]) ?: return@mapNotNull null
                    profileId = if (rawProfile.isBlank()) {
                        null
                    } else {
                        rawProfile.toIntOrNull() ?: return@mapNotNull null
                    }
                    appsField = parts[3]
                }

                val appKeys = if (appsField.isBlank()) {
                    emptyList()
                } else {
                    appsField
                        .split(',')
                        .mapNotNull(::decodePart)
                        .filter { it.isNotBlank() }
                        .distinct()
                        .take(LauncherFolderPolicy.MAX_APPS_PER_FOLDER)
                }
                LauncherFolder(
                    id = id,
                    name = name,
                    appKeys = appKeys,
                    profileId = profileId,
                )
            }
            .take(LauncherFolderPolicy.MAX_FOLDER_COUNT)
            .toList()
    }

    private fun encodePart(value: String): String =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodePart(value: String): String? =
        runCatching {
            String(
                Base64.getUrlDecoder().decode(value),
                StandardCharsets.UTF_8,
            )
        }.getOrNull()
}

class LauncherFolderRepository(context: Context) {
    private val dataStore = context.applicationContext.launcherFolderStore
    private val foldersKey = stringPreferencesKey("folders_v1")

    val folders: Flow<List<LauncherFolder>> = dataStore.data
        .map { values -> LauncherFolderCodec.decode(values[foldersKey]) }
        .distinctUntilChanged()

    suspend fun create(
        rawName: String,
        profileId: Int? = null,
    ): LauncherFolder? {
        val name = LauncherFolderPolicy.normalizeName(rawName) ?: return null
        var created: LauncherFolder? = null
        dataStore.edit { values ->
            val current = LauncherFolderCodec.decode(values[foldersKey])
            if (current.size >= LauncherFolderPolicy.MAX_FOLDER_COUNT) return@edit
            val folder = LauncherFolder(
                id = UUID.randomUUID().toString(),
                name = name,
                appKeys = emptyList(),
                profileId = profileId,
            )
            values[foldersKey] = LauncherFolderCodec.encode(current + folder)
            created = folder
        }
        return created
    }

    suspend fun rename(folderId: String, rawName: String): Boolean {
        val name = LauncherFolderPolicy.normalizeName(rawName) ?: return false
        var changed = false
        dataStore.edit { values ->
            val current = LauncherFolderCodec.decode(values[foldersKey])
            val updated = current.map { folder ->
                if (folder.id == folderId) {
                    changed = true
                    folder.copy(name = name)
                } else {
                    folder
                }
            }
            if (changed) values[foldersKey] = LauncherFolderCodec.encode(updated)
        }
        return changed
    }

    suspend fun delete(folderId: String): Boolean {
        var changed = false
        dataStore.edit { values ->
            val current = LauncherFolderCodec.decode(values[foldersKey])
            val updated = current.filterNot { folder ->
                (folder.id == folderId).also { matches -> if (matches) changed = true }
            }
            if (changed) values[foldersKey] = LauncherFolderCodec.encode(updated)
        }
        return changed
    }

    suspend fun addApp(
        folderId: String,
        appKey: String,
        primaryProfileId: Int,
    ): Boolean {
        if (appKey.isBlank()) return false
        var changed = false
        dataStore.edit { values ->
            val current = LauncherFolderCodec.decode(values[foldersKey])
            val updated = current.map { folder ->
                if (folder.id != folderId) return@map folder
                if (
                    !LauncherFolderProfilePolicy.canContainWorkspaceKey(
                        folder = folder,
                        appKey = appKey,
                        primaryProfileId = primaryProfileId,
                    )
                ) {
                    return@map folder
                }
                if (appKey in folder.appKeys) {
                    changed = true
                    return@map folder
                }
                if (folder.appKeys.size >= LauncherFolderPolicy.MAX_APPS_PER_FOLDER) return@map folder
                changed = true
                folder.copy(appKeys = folder.appKeys + appKey)
            }
            if (changed) values[foldersKey] = LauncherFolderCodec.encode(updated)
        }
        return changed
    }

    suspend fun removeApp(folderId: String, appKey: String): Boolean {
        var changed = false
        dataStore.edit { values ->
            val current = LauncherFolderCodec.decode(values[foldersKey])
            val updated = current.map { folder ->
                if (folder.id != folderId || appKey !in folder.appKeys) return@map folder
                changed = true
                folder.copy(appKeys = folder.appKeys.filterNot { it == appKey })
            }
            if (changed) values[foldersKey] = LauncherFolderCodec.encode(updated)
        }
        return changed
    }
}
