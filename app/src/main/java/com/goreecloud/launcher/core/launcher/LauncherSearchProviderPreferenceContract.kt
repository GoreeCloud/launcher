package com.goreecloud.launcher.core.launcher

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Versioned persistence boundary for Universal Search provider user controls.
 *
 * This contract deliberately stores provider identity/control state only. It does not store queries,
 * result history, usage/frequency signals, provider credentials, authorization grants, or provider
 * payloads. Persisting the returned string is a caller responsibility; this type does not write to
 * DataStore or any other storage by itself.
 *
 * Absence of a stored value is distinct from a stored snapshot whose enabled-provider set is empty.
 * That distinction preserves [LauncherSearchProviderUserControlPolicy]'s first-run defaults versus
 * an explicit user choice to disable every optional provider.
 */
data class LauncherSearchProviderPreferenceSnapshot(
    val enabledProviderIds: Set<String>,
    val providerOrder: List<String>,
) {
    init {
        require(enabledProviderIds.none(String::isBlank)) {
            "enabled provider ids must not be blank"
        }
        require(providerOrder.none(String::isBlank)) {
            "provider order ids must not be blank"
        }
        require(providerOrder.distinct().size == providerOrder.size) {
            "provider order ids must be unique"
        }
    }

    companion object {
        fun fromControlState(
            state: LauncherSearchProviderControlState,
        ): LauncherSearchProviderPreferenceSnapshot =
            LauncherSearchProviderPreferenceSnapshot(
                enabledProviderIds = state.enabledProviderIds,
                providerOrder = state.orderedOptions.map { option -> option.providerId },
            )
    }
}

sealed class LauncherSearchProviderPreferenceDecodeResult {
    object Absent : LauncherSearchProviderPreferenceDecodeResult()

    data class Loaded(
        val snapshot: LauncherSearchProviderPreferenceSnapshot,
    ) : LauncherSearchProviderPreferenceDecodeResult()

    data class UnsupportedVersion(
        val version: Int,
    ) : LauncherSearchProviderPreferenceDecodeResult()

    data class Invalid(
        val reason: LauncherSearchProviderPreferenceInvalidReason,
    ) : LauncherSearchProviderPreferenceDecodeResult()
}

enum class LauncherSearchProviderPreferenceInvalidReason {
    EMPTY_PAYLOAD,
    INVALID_HEADER,
    INVALID_STRUCTURE,
    INVALID_PROVIDER_ID_ENCODING,
    BLANK_PROVIDER_ID,
    DUPLICATE_PROVIDER_ID,
}

/**
 * Stable text encoding for Search-provider control preferences.
 *
 * Format v1:
 *
 *     goreecloud-launcher-search-provider-preferences-v1
 *     enabled=<comma-separated base64url provider ids>
 *     order=<comma-separated base64url provider ids>
 *
 * Enabled IDs are encoded in lexical order because enablement is a set. Provider order preserves
 * the user's explicit ordering. Provider identifiers are base64url encoded so delimiter characters
 * in future provider IDs cannot make the format ambiguous.
 */
object LauncherSearchProviderPreferenceContract {
    const val CURRENT_VERSION: Int = 1

    private const val HEADER_PREFIX = "goreecloud-launcher-search-provider-preferences-v"
    private const val ENABLED_PREFIX = "enabled="
    private const val ORDER_PREFIX = "order="

    private val encoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder: Base64.Decoder = Base64.getUrlDecoder()

    fun encode(snapshot: LauncherSearchProviderPreferenceSnapshot): String {
        val enabled = snapshot.enabledProviderIds
            .sorted()
            .joinToString(separator = ",", transform = ::encodeProviderId)
        val order = snapshot.providerOrder
            .joinToString(separator = ",", transform = ::encodeProviderId)

        return buildString {
            append(HEADER_PREFIX)
            append(CURRENT_VERSION)
            append('\n')
            append(ENABLED_PREFIX)
            append(enabled)
            append('\n')
            append(ORDER_PREFIX)
            append(order)
        }
    }

    fun decode(storedValue: String?): LauncherSearchProviderPreferenceDecodeResult {
        if (storedValue == null) {
            return LauncherSearchProviderPreferenceDecodeResult.Absent
        }
        if (storedValue.isBlank()) {
            return LauncherSearchProviderPreferenceDecodeResult.Invalid(
                LauncherSearchProviderPreferenceInvalidReason.EMPTY_PAYLOAD,
            )
        }

        val lines = storedValue.split('\n')
        if (lines.size != 3) {
            return LauncherSearchProviderPreferenceDecodeResult.Invalid(
                LauncherSearchProviderPreferenceInvalidReason.INVALID_STRUCTURE,
            )
        }

        val header = lines[0]
        if (!header.startsWith(HEADER_PREFIX)) {
            return LauncherSearchProviderPreferenceDecodeResult.Invalid(
                LauncherSearchProviderPreferenceInvalidReason.INVALID_HEADER,
            )
        }
        val version = header.removePrefix(HEADER_PREFIX).toIntOrNull()
            ?: return LauncherSearchProviderPreferenceDecodeResult.Invalid(
                LauncherSearchProviderPreferenceInvalidReason.INVALID_HEADER,
            )
        if (version != CURRENT_VERSION) {
            return LauncherSearchProviderPreferenceDecodeResult.UnsupportedVersion(version)
        }

        if (!lines[1].startsWith(ENABLED_PREFIX) || !lines[2].startsWith(ORDER_PREFIX)) {
            return LauncherSearchProviderPreferenceDecodeResult.Invalid(
                LauncherSearchProviderPreferenceInvalidReason.INVALID_STRUCTURE,
            )
        }

        val enabledResult = decodeProviderIds(lines[1].removePrefix(ENABLED_PREFIX))
        if (enabledResult is DecodedProviderIds.Invalid) {
            return LauncherSearchProviderPreferenceDecodeResult.Invalid(enabledResult.reason)
        }
        val orderResult = decodeProviderIds(lines[2].removePrefix(ORDER_PREFIX))
        if (orderResult is DecodedProviderIds.Invalid) {
            return LauncherSearchProviderPreferenceDecodeResult.Invalid(orderResult.reason)
        }

        val enabledIds = (enabledResult as DecodedProviderIds.Valid).providerIds
        val orderIds = (orderResult as DecodedProviderIds.Valid).providerIds
        if (enabledIds.distinct().size != enabledIds.size || orderIds.distinct().size != orderIds.size) {
            return LauncherSearchProviderPreferenceDecodeResult.Invalid(
                LauncherSearchProviderPreferenceInvalidReason.DUPLICATE_PROVIDER_ID,
            )
        }

        return LauncherSearchProviderPreferenceDecodeResult.Loaded(
            LauncherSearchProviderPreferenceSnapshot(
                enabledProviderIds = enabledIds.toSet(),
                providerOrder = orderIds,
            ),
        )
    }

    private fun encodeProviderId(providerId: String): String =
        encoder.encodeToString(providerId.toByteArray(StandardCharsets.UTF_8))

    private fun decodeProviderIds(encodedIds: String): DecodedProviderIds {
        if (encodedIds.isEmpty()) {
            return DecodedProviderIds.Valid(emptyList())
        }

        val decoded = mutableListOf<String>()
        encodedIds.split(',').forEach { encodedId ->
            val providerId = try {
                val providerBytes = decoder.decode(encodedId)
                StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(providerBytes))
                    .toString()
            } catch (_: IllegalArgumentException) {
                return DecodedProviderIds.Invalid(
                    LauncherSearchProviderPreferenceInvalidReason.INVALID_PROVIDER_ID_ENCODING,
                )
            } catch (_: CharacterCodingException) {
                return DecodedProviderIds.Invalid(
                    LauncherSearchProviderPreferenceInvalidReason.INVALID_PROVIDER_ID_ENCODING,
                )
            }
            if (providerId.isBlank()) {
                return DecodedProviderIds.Invalid(
                    LauncherSearchProviderPreferenceInvalidReason.BLANK_PROVIDER_ID,
                )
            }
            decoded += providerId
        }
        return DecodedProviderIds.Valid(decoded)
    }

    private sealed class DecodedProviderIds {
        data class Valid(val providerIds: List<String>) : DecodedProviderIds()
        data class Invalid(
            val reason: LauncherSearchProviderPreferenceInvalidReason,
        ) : DecodedProviderIds()
    }
}
