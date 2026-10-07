package com.goreecloud.launcher.core.launcher

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.coroutineContext

/**
 * Explicit user-initiated, user-funded Search answers. These sources are deliberately not
 * LauncherSearchProvider registrations: none may receive query-as-you-type traffic.
 */
internal enum class LauncherDirectApiKind {
    OPENAI, CLAUDE, GEMINI, PERPLEXITY, CUSTOM_CHAT, CUSTOM_SEARCH
}

internal data class LauncherDirectApiSource(
    val id: String,
    val kind: LauncherDirectApiKind,
    val title: String,
    val endpoint: String,
    val model: String,
    val secret: String,
    val enabled: Boolean = true,
    val authHeader: String = "Authorization",
    val searchParameter: String = "q",
    val responsePath: String = "answer",
) {
    override fun toString(): String =
        "LauncherDirectApiSource(id=$id, kind=$kind, title=$title, " +
            "endpoint=[redacted], model=$model, secret=[redacted], enabled=$enabled)"
}

/** No values in this catalog are credentials. Official service endpoints are not user-editable. */
internal object LauncherDirectApiCatalog {
    fun templates(): List<LauncherDirectApiSource> = listOf(
        LauncherDirectApiSource(
            LauncherConnectedSearchProviderRegistry.CHATGPT_PROVIDER_ID,
            LauncherDirectApiKind.OPENAI,
            "ChatGPT (OpenAI API)",
            "https://api.openai.com/v1/chat/completions",
            "gpt-4.1-mini",
            "",
            false,
        ),
        LauncherDirectApiSource(
            LauncherConnectedSearchProviderRegistry.CLAUDE_PROVIDER_ID,
            LauncherDirectApiKind.CLAUDE,
            "Claude API",
            "https://api.anthropic.com/v1/messages",
            "claude-sonnet-4-5",
            "",
            false,
        ),
        LauncherDirectApiSource(
            LauncherConnectedSearchProviderRegistry.GEMINI_PROVIDER_ID,
            LauncherDirectApiKind.GEMINI,
            "Gemini API",
            "https://generativelanguage.googleapis.com",
            "gemini-2.5-flash",
            "",
            false,
        ),
        LauncherDirectApiSource(
            LauncherConnectedSearchProviderRegistry.PERPLEXITY_PROVIDER_ID,
            LauncherDirectApiKind.PERPLEXITY,
            "Perplexity API",
            "https://api.perplexity.ai/chat/completions",
            "sonar",
            "",
            false,
        ),
    )

    fun resolveSaved(source: LauncherDirectApiSource): LauncherDirectApiSource {
        val template = templates().firstOrNull { it.id == source.id }
        return if (template == null) source else source.copy(
            kind = template.kind,
            title = template.title,
            endpoint = template.endpoint,
            authHeader = template.authHeader,
        )
    }

    fun nextCustomId(): String = "custom." + UUID.randomUUID().toString()
}

/**
 * A custom API cannot be a loopback/LAN endpoint, an IP literal, an arbitrary port, or an
 * endpoint that embeds a secret in the URL. Caller-selected public HTTPS is required.
 * Official service destinations remain pinned by LauncherDirectApiCatalog.resolveSaved.
 */
internal fun launcherDirectApiValidHttpsEndpoint(raw: String): Boolean = runCatching {
    val uri = URI(raw)
    val host = uri.host?.lowercase() ?: return@runCatching false
    val labels = host.split('.')
    uri.scheme.equals("https", ignoreCase = true) &&
        uri.rawUserInfo == null &&
        uri.rawQuery == null &&
        uri.rawFragment == null &&
        (uri.port == -1 || uri.port == 443) &&
        raw.length in 12..1000 &&
        !raw.any { it.isWhitespace() } &&
        host.length <= 253 &&
        !host.contains(':') &&
        !host.matches(Regex("[0-9]+(\\.[0-9]+){3}")) &&
        host != "localhost" &&
        listOf(".localhost", ".local", ".internal", ".test", ".invalid", ".example", ".onion")
            .none(host::endsWith) &&
        labels.size >= 2 &&
        labels.all { it.matches(Regex("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?")) } &&
        !uri.rawPath.orEmpty().contains('\\')
}.getOrDefault(false)

/** Reject private, loopback, benchmark and shared address space before any API secret is sent. */
internal fun launcherDirectApiIsPublicAddress(address: InetAddress): Boolean {
    if (address.isAnyLocalAddress || address.isLoopbackAddress ||
        address.isLinkLocalAddress || address.isSiteLocalAddress ||
        address.isMulticastAddress
    ) return false
    val octets = address.address
    if (address is Inet6Address) {
        if (octets.size != 16 || (octets[0].toInt() and 0xfe) == 0xfc) return false
        val ipv4Mapped = (0..9).all { octets[it] == 0.toByte() } &&
            (octets[10].toInt() and 0xff) == 0xff &&
            (octets[11].toInt() and 0xff) == 0xff
        return if (ipv4Mapped) {
            launcherDirectApiIsPublicAddress(
                InetAddress.getByAddress(octets.copyOfRange(12, 16)),
            )
        } else {
            true
        }
    }
    if (octets.size != 4) return false
    val a = octets[0].toInt() and 0xff
    val b = octets[1].toInt() and 0xff
    if (a == 0 || a >= 224) return false
    if (a == 100 && b in 64..127) return false
    if (a == 192 && b == 0) return false
    if (a == 198 && b in 18..19) return false
    return true
}

internal fun launcherDirectApiValidFieldName(raw: String): Boolean =
    raw.length in 1..64 && raw.all { it.isLetterOrDigit() || it == '-' || it == '_' }

internal fun launcherDirectApiValidResponsePath(raw: String): Boolean =
    raw.length in 1..160 &&
        raw.split('.').all { part ->
            part.isNotEmpty() && part.all {
                it.isLetterOrDigit() || it == '_' || it == '-'
            }
        }

/**
 * All configuration, including API credentials, is encrypted using an app-private Android
 * Keystore key and written atomically under noBackupFilesDir. No plaintext preference, export,
 * Room entry, diagnostic field, backup, or log is created. Keystore loss fails closed.
 */
internal class LauncherDirectApiSourceStore(private val context: Context) {
    private val lock = Any()
    private val file = AtomicFile(File(context.noBackupFilesDir, "direct_api_sources_v1.enc"))

    fun list(): List<LauncherDirectApiSource> = synchronized(lock) {
        val raw = readEncrypted() ?: return@synchronized emptyList()
        val array = JSONObject(String(raw, StandardCharsets.UTF_8)).getJSONArray("sources")
        buildList {
            for (i in 0 until array.length().coerceAtMost(16)) {
                val item = array.optJSONObject(i) ?: continue
                val kind = runCatching {
                    LauncherDirectApiKind.valueOf(item.getString("kind"))
                }.getOrNull() ?: continue
                val source = LauncherDirectApiSource(
                    id = item.optString("id"),
                    kind = kind,
                    title = item.optString("title"),
                    endpoint = item.optString("endpoint"),
                    model = item.optString("model"),
                    secret = item.optString("secret"),
                    enabled = item.optBoolean("enabled", false),
                    authHeader = item.optString("authHeader", "Authorization"),
                    searchParameter = item.optString("searchParameter", "q"),
                    responsePath = item.optString("responsePath", "answer"),
                )
                if (source.id.isNotBlank()) add(LauncherDirectApiCatalog.resolveSaved(source))
            }
        }
    }

    fun upsert(incoming: LauncherDirectApiSource) = synchronized(lock) {
        val source = LauncherDirectApiCatalog.resolveSaved(incoming)
        require(source.id.length in 1..120)
        require(source.title.isNotBlank() && source.title.length <= 60)
        require(source.model.length <= 120 && !source.model.contains('\n'))
        require(source.secret.length <= 8192 && !source.secret.contains('\n'))
        require(launcherDirectApiValidHttpsEndpoint(source.endpoint))
        require(launcherDirectApiValidFieldName(source.authHeader))
        require(launcherDirectApiValidFieldName(source.searchParameter))
        require(launcherDirectApiValidResponsePath(source.responsePath))
        require(source.kind == LauncherDirectApiKind.CUSTOM_SEARCH ||
            source.model.matches(Regex("[A-Za-z0-9_.:/-]{1,120}")))
        val sources = list().toMutableList()
        val index = sources.indexOfFirst { it.id == source.id }
        if (index < 0) {
            require(sources.size < 16) { "The maximum of 16 direct API sources is reached" }
            sources.add(source)
        } else {
            sources[index] = source
        }
        writeEncrypted(serialize(sources))
    }

    fun remove(id: String) = synchronized(lock) {
        val sources = list().filterNot { it.id == id }
        writeEncrypted(serialize(sources))
    }

    /** Explicit destructive recovery when a device reset invalidates the Keystore key. */
    fun resetAll() = synchronized(lock) {
        file.delete()
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (store.containsAlias(KEY_ALIAS)) store.deleteEntry(KEY_ALIAS)
    }

    private fun serialize(sources: List<LauncherDirectApiSource>): ByteArray {
        val arr = JSONArray()
        sources.forEach { source ->
            arr.put(JSONObject().apply {
                put("id", source.id)
                put("kind", source.kind.name)
                put("title", source.title)
                put("endpoint", source.endpoint)
                put("model", source.model)
                put("secret", source.secret)
                put("enabled", source.enabled)
                put("authHeader", source.authHeader)
                put("searchParameter", source.searchParameter)
                put("responsePath", source.responsePath)
            })
        }
        return JSONObject().put("sources", arr).toString().toByteArray(StandardCharsets.UTF_8)
    }

    private fun keystoreKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun readEncrypted(): ByteArray? {
        if (!file.baseFile.exists()) return null
        val bytes = file.openRead().use { it.readBytes() }
        require(bytes.size > 29 && bytes[0] == 1.toByte()) {
            "Stored API configuration is unreadable"
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
        return cipher.doFinal(bytes.copyOfRange(13, bytes.size))
    }

    private fun writeEncrypted(plain: ByteArray) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        val iv = cipher.iv
        require(iv.size == 12) { "Unexpected Android Keystore GCM nonce length" }
        val result = byteArrayOf(1) + iv + cipher.doFinal(plain)
        val output = file.startWrite()
        try {
            output.write(result)
            file.finishWrite(output)
        } catch (e: Exception) {
            file.failWrite(output)
            throw e
        }
    }

    private companion object {
        const val KEY_ALIAS = "goreecloud_launcher_search_direct_api_v1"
    }
}

/**
 * One HTTPS request per explicit Ask tap, no redirect (protects the secret header), no raw HTTP
 * error-body disclosure, no prompt/response logging, bounded timeouts and response size.
 */
internal class LauncherDirectApiAnswerClient {
    suspend fun answer(source: LauncherDirectApiSource, rawQuery: String): String =
        withContext(Dispatchers.IO) {
            require(source.enabled) { "Enable this source in Search Sources first" }
            val query = rawQuery.trim().take(2000)
            require(query.isNotEmpty()) { "Enter a question first" }
            val trusted = LauncherDirectApiCatalog.resolveSaved(source)
            require(launcherDirectApiValidHttpsEndpoint(trusted.endpoint)) {
                "Configure a valid HTTPS endpoint"
            }

            val endpoint = when (trusted.kind) {
                LauncherDirectApiKind.GEMINI -> {
                    require(trusted.model.matches(Regex("[A-Za-z0-9_.-]{1,120}")))
                    trusted.endpoint.trimEnd('/') + "/v1beta/models/" +
                        trusted.model + ":generateContent"
                }
                LauncherDirectApiKind.CUSTOM_SEARCH -> trusted.endpoint + "?" +
                    URLEncoder.encode(trusted.searchParameter, "UTF-8") + "=" +
                    URLEncoder.encode(query, "UTF-8")
                else -> trusted.endpoint
            }
            // Reject DNS resolving to private/device addresses before attaching any API key.
            // Redirects remain disabled; a hardened DNS-pinned broker is still a later gate.
            val addresses = InetAddress.getAllByName(checkNotNull(URI(endpoint).host))
            require(addresses.isNotEmpty() && addresses.all(::launcherDirectApiIsPublicAddress)) {
                "The API endpoint must resolve to a public address"
            }
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod =
                    if (trusted.kind == LauncherDirectApiKind.CUSTOM_SEARCH) "GET" else "POST"
                connectTimeout = 10_000
                readTimeout = 20_000
                instanceFollowRedirects = false
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Cache-Control", "no-store")
                if (trusted.kind != LauncherDirectApiKind.CUSTOM_SEARCH) {
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                }
                if (trusted.secret.isNotBlank()) {
                    when (trusted.kind) {
                        LauncherDirectApiKind.CLAUDE -> {
                            setRequestProperty("x-api-key", trusted.secret)
                            setRequestProperty("anthropic-version", "2023-06-01")
                        }
                        LauncherDirectApiKind.GEMINI ->
                            setRequestProperty("x-goog-api-key", trusted.secret)
                        LauncherDirectApiKind.CUSTOM_CHAT,
                        LauncherDirectApiKind.CUSTOM_SEARCH -> {
                            setRequestProperty(
                                trusted.authHeader,
                                if (trusted.authHeader.equals("Authorization", true)) {
                                    "Bearer " + trusted.secret
                                } else {
                                    trusted.secret
                                },
                            )
                        }
                        else -> setRequestProperty("Authorization", "Bearer " + trusted.secret)
                    }
                }
            }
            val cancellation = coroutineContext.job.invokeOnCompletion { connection.disconnect() }
            try {
                coroutineContext.ensureActive()
                if (trusted.kind != LauncherDirectApiKind.CUSTOM_SEARCH) {
                    val request = when (trusted.kind) {
                        LauncherDirectApiKind.CLAUDE -> JSONObject().apply {
                            put("model", trusted.model)
                            put("max_tokens", 512)
                            put("messages", JSONArray().put(
                                JSONObject().put("role", "user").put("content", query),
                            ))
                        }
                        LauncherDirectApiKind.GEMINI -> JSONObject().apply {
                            put("contents", JSONArray().put(
                                JSONObject().put("parts", JSONArray().put(
                                    JSONObject().put("text", query),
                                )),
                            ))
                            put("generationConfig", JSONObject().put("maxOutputTokens", 512))
                        }
                        else -> JSONObject().apply {
                            put("model", trusted.model)
                            put("max_tokens", 512)
                            put("messages", JSONArray().put(
                                JSONObject().put("role", "user").put("content", query),
                            ))
                        }
                    }
                    connection.outputStream.use { stream ->
                        stream.write(request.toString().toByteArray(StandardCharsets.UTF_8))
                    }
                }
                val status = connection.responseCode
                if (status !in 200..299) {
                    throw IllegalStateException("Provider returned HTTP " + status)
                }
                val output = ByteArrayOutputStream()
                connection.inputStream.use { stream ->
                    val block = ByteArray(4096)
                    while (true) {
                        coroutineContext.ensureActive()
                        val count = stream.read(block)
                        if (count < 0) break
                        if (output.size() + count > 96_000) {
                            throw IllegalStateException("Provider response exceeds the size limit")
                        }
                        output.write(block, 0, count)
                    }
                }
                coroutineContext.ensureActive()
                val json = JSONObject(output.toString(StandardCharsets.UTF_8.name()))
                extractAnswer(trusted, json)
                    .trim().take(12_000)
                    .ifBlank { throw IllegalStateException("Provider returned no readable answer") }
            } finally {
                cancellation.dispose()
                connection.disconnect()
            }
        }

    private fun extractAnswer(source: LauncherDirectApiSource, root: JSONObject): String =
        when (source.kind) {
            LauncherDirectApiKind.CLAUDE -> {
                val blocks = root.optJSONArray("content") ?: JSONArray()
                (0 until blocks.length()).mapNotNull { index ->
                    blocks.optJSONObject(index)?.optString("text")?.takeIf { it.isNotBlank() }
                }.joinToString("\n\n")
            }
            LauncherDirectApiKind.GEMINI -> {
                val parts = root.optJSONArray("candidates")?.optJSONObject(0)
                    ?.optJSONObject("content")?.optJSONArray("parts") ?: JSONArray()
                (0 until parts.length()).mapNotNull { index ->
                    parts.optJSONObject(index)?.optString("text")?.takeIf { it.isNotBlank() }
                }.joinToString("\n\n")
            }
            LauncherDirectApiKind.CUSTOM_SEARCH ->
                extractJsonPath(root, source.responsePath)
            else -> root.optJSONArray("choices")?.optJSONObject(0)
                ?.optJSONObject("message")?.optString("content").orEmpty()
        }

    private fun extractJsonPath(root: JSONObject, path: String): String {
        var current: Any = root
        for (part in path.split('.')) {
            current = when (current) {
                is JSONObject -> current.opt(part)
                is JSONArray -> current.opt(part.toIntOrNull() ?: return "")
                else -> return ""
            } ?: return ""
        }
        return when (current) {
            is String -> current
            is Number -> current.toString()
            is JSONArray -> (0 until current.length().coerceAtMost(5))
                .mapNotNull { index ->
                    when (val item = current.opt(index)) {
                        is String -> item
                        is JSONObject -> listOf(
                            item.optString("title"),
                            item.optString("snippet"),
                            item.optString("url"),
                        ).filter(String::isNotBlank).joinToString(" — ").takeIf(String::isNotBlank)
                        else -> null
                    }
                }.joinToString("\n\n")
            else -> ""
        }
    }
}
