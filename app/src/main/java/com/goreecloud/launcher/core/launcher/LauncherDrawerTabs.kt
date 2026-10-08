package com.goreecloud.launcher.core.launcher

import java.nio.charset.StandardCharsets
import java.util.Base64

data class LauncherDrawerTab(
    val id: String,
    val name: String,
    val memberKeys: Set<String>,
)

internal object LauncherDrawerTabsCodec {
    private const val VERSION = "v1"
    const val MAX_TABS = 8
    const val MAX_NAME_LENGTH = 32
    const val MAX_MEMBERS_PER_TAB = 512

    fun sanitizeName(raw: String): String =
        raw.trim()
            .replace(Regex("\\s+"), " ")
            .take(MAX_NAME_LENGTH)

    fun encode(tabs: List<LauncherDrawerTab>): String {
        val sanitized = tabs
            .asSequence()
            .mapNotNull(::sanitize)
            .distinctBy { it.id }
            .take(MAX_TABS)
            .toList()
        if (sanitized.isEmpty()) return VERSION
        return buildString {
            append(VERSION)
            sanitized.forEach { tab ->
                append('\n')
                append(encodePart(tab.id))
                append('\t')
                append(encodePart(tab.name))
                append('\t')
                append(
                    tab.memberKeys
                        .asSequence()
                        .filterNot(String::isBlank)
                        .distinct()
                        .take(MAX_MEMBERS_PER_TAB)
                        .joinToString(",") { encodePart(it) },
                )
            }
        }
    }

    fun decode(raw: String?): List<LauncherDrawerTab> {
        if (raw.isNullOrBlank()) return emptyList()
        val lines = raw.lineSequence().toList()
        if (lines.firstOrNull() != VERSION) return emptyList()

        return lines
            .drop(1)
            .asSequence()
            .mapNotNull { line ->
                val fields = line.split('\t', limit = 3)
                if (fields.size != 3) return@mapNotNull null
                val id = decodePart(fields[0])?.trim().orEmpty()
                val name = decodePart(fields[1])?.let(::sanitizeName).orEmpty()
                if (id.isBlank() || name.isBlank()) return@mapNotNull null
                val members = fields[2]
                    .split(',')
                    .asSequence()
                    .mapNotNull(::decodePart)
                    .map(String::trim)
                    .filterNot(String::isBlank)
                    .distinct()
                    .take(MAX_MEMBERS_PER_TAB)
                    .toSet()
                LauncherDrawerTab(
                    id = id,
                    name = name,
                    memberKeys = members,
                )
            }
            .distinctBy { it.id }
            .take(MAX_TABS)
            .toList()
    }

    private fun sanitize(tab: LauncherDrawerTab): LauncherDrawerTab? {
        val id = tab.id.trim()
        val name = sanitizeName(tab.name)
        if (id.isBlank() || name.isBlank()) return null
        return LauncherDrawerTab(
            id = id,
            name = name,
            memberKeys = tab.memberKeys
                .asSequence()
                .map(String::trim)
                .filterNot(String::isBlank)
                .distinct()
                .take(MAX_MEMBERS_PER_TAB)
                .toSet(),
        )
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
