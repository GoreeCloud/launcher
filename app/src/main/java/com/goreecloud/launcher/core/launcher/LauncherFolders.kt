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
)

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
                if (parts.size != 3) return@mapNotNull null
                val id = decodePart(parts[0])?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                if (!seenIds.add(id)) return@mapNotNull null
                val name = LauncherFolderPolicy.normalizeName(decodePart(parts[1]))
                    ?: return@mapNotNull null
                val appKeys = if (parts[2].isBlank()) {
                    emptyList()
                } else {
                    parts[2]
                        .split(',')
                        .mapNotNull(::decodePart)
                        .filter { it.isNotBlank() }
                        .distinct()
                        .take(LauncherFolderPolicy.MAX_APPS_PER_FOLDER)
                }
                LauncherFolder(id = id, name = name, appKeys = appKeys)
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

    suspend fun create(rawName: String): LauncherFolder? {
        val name = LauncherFolderPolicy.normalizeName(rawName) ?: return null
        var created: LauncherFolder? = null
        dataStore.edit { values ->
            val current = LauncherFolderCodec.decode(values[foldersKey])
            if (current.size >= LauncherFolderPolicy.MAX_FOLDER_COUNT) return@edit
            val folder = LauncherFolder(
                id = UUID.randomUUID().toString(),
                name = name,
                appKeys = emptyList(),
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

    suspend fun addApp(folderId: String, appKey: String): Boolean {
        if (appKey.isBlank()) return false
        var changed = false
        dataStore.edit { values ->
            val current = LauncherFolderCodec.decode(values[foldersKey])
            val updated = current.map { folder ->
                if (folder.id != folderId) return@map folder
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
