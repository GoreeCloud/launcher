package com.goreecloud.launcher.core.launcher

import android.Manifest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LauncherLaunchShortcutSearchAction(
    val packageName: String,
    val shortcutId: String,
    val user: android.os.UserHandle,
) : LauncherSearchAction

data class LauncherOpenUriSearchAction(
    val intentAction: String,
    val uri: String,
) : LauncherSearchAction

object LauncherLocalSearchPermissions {
    fun permissionFor(providerId: String): String? = when (providerId) {
        LauncherContactsSearchProvider.PROVIDER_ID -> Manifest.permission.READ_CONTACTS
        LauncherCallHistorySearchProvider.PROVIDER_ID -> Manifest.permission.READ_CALL_LOG
        LauncherMessagesSearchProvider.PROVIDER_ID -> Manifest.permission.READ_SMS
        else -> null
    }

    fun isGranted(context: Context, providerId: String): Boolean {
        val permission = permissionFor(providerId) ?: return true
        return ContextCompat.checkSelfPermission(context, permission) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

/**
 * Ephemeral provider health only: no typed query, contact, call, message or file data is retained.
 * Search must distinguish Android permission denial and provider failure from an actual no-match.
 */
enum class LauncherLocalSearchIssue {
    PERMISSION_REQUIRED,
    ANDROID_RESTRICTED,
    SOURCE_UNAVAILABLE,
}

object LauncherLocalSearchDiagnostics {
    private val mutableIssues = MutableStateFlow<Map<String, LauncherLocalSearchIssue>>(emptyMap())
    val issues = mutableIssues.asStateFlow()

    fun record(providerId: String, issue: LauncherLocalSearchIssue?) {
        mutableIssues.update { current ->
            current.toMutableMap().also { next ->
                if (issue == null) next.remove(providerId) else next[providerId] = issue
            }
        }
    }
}

private suspend fun guardedLocalSearch(
    context: Context,
    providerId: String,
    search: suspend () -> List<LauncherSearchResult>,
): List<LauncherSearchResult> {
    if (!LauncherLocalSearchPermissions.isGranted(context, providerId)) {
        LauncherLocalSearchDiagnostics.record(providerId, LauncherLocalSearchIssue.PERMISSION_REQUIRED)
        return emptyList()
    }
    return try {
        search().also { LauncherLocalSearchDiagnostics.record(providerId, null) }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: SecurityException) {
        LauncherLocalSearchDiagnostics.record(providerId, LauncherLocalSearchIssue.ANDROID_RESTRICTED)
        emptyList()
    } catch (_: Exception) {
        LauncherLocalSearchDiagnostics.record(providerId, LauncherLocalSearchIssue.SOURCE_UNAVAILABLE)
        emptyList()
    }
}

/** Match formatted phone numbers even when call-log and contact providers store punctuation. */
object LauncherLocalPhoneSearchPolicy {
    fun score(name: String, number: String, rawQuery: String): Int? {
        val digits = rawQuery.filter(Char::isDigit)
        if (rawQuery.isNotBlank() && rawQuery.none(Char::isLetter) && digits.length < 2) {
            return null // Avoid broad one-digit scans of sensitive phone/call data.
        }
        LauncherSearchTextRanking.score(name, number, rawQuery)?.let { return it }
        if (digits.length < 2 || rawQuery.any(Char::isLetter)) return null
        return if (number.filter(Char::isDigit).contains(digits)) 160 else null
    }
}

object LauncherLocalSearchProviderRegistry {
    fun registrations(context: Context): List<LauncherSearchProviderRegistration> = listOf(
        registration(LauncherShortcutsSearchProvider(context), LauncherSearchAuthorizationRequirement.NONE),
        registration(LauncherContactsSearchProvider(context), LauncherSearchAuthorizationRequirement.USER_CONSENT),
        registration(LauncherCallHistorySearchProvider(context), LauncherSearchAuthorizationRequirement.USER_CONSENT),
        registration(LauncherMessagesSearchProvider(context), LauncherSearchAuthorizationRequirement.USER_CONSENT),
    )

    private fun registration(
        provider: LauncherSearchProvider,
        authorizationRequirement: LauncherSearchAuthorizationRequirement,
    ): LauncherSearchProviderRegistration = LauncherSearchProviderRegistration(
        provider = provider,
        metadata = LauncherSearchProviderMetadata(
            providerId = provider.id,
            contractVersion = LauncherSearchProviderContract.currentVersion,
            provenance = LauncherSearchProviderProvenance.LAUNCHER_BUILT_IN,
            offlineBehavior = LauncherSearchOfflineBehavior.LOCAL_ONLY,
            authorizationRequirement = authorizationRequirement,
            remoteProcessing = LauncherSearchRemoteProcessing.NONE,
            queryRetention = LauncherSearchQueryRetention.NONE,
        ),
    )
}

class LauncherShortcutsSearchProvider(context: Context) : LauncherSearchProvider, LauncherAsyncSearchProvider {
    private val launcherApps = context.applicationContext.getSystemService(LauncherApps::class.java)
    override val id: String = PROVIDER_ID
    override fun search(rawQuery: String): List<LauncherSearchResult> = emptyList()

    override suspend fun searchAsync(request: LauncherSearchRequest): List<LauncherSearchResult> =
        withContext(Dispatchers.IO) {
            val rawQuery = request.rawQuery.trim()
            if (rawQuery.isBlank() || !launcherApps.hasShortcutHostPermission()) return@withContext emptyList()
            val query = LauncherApps.ShortcutQuery().setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST,
            )
            launcherApps.profiles.flatMap { user ->
                runCatching { launcherApps.getShortcuts(query, user).orEmpty() }.getOrDefault(emptyList())
            }.mapNotNull { shortcut -> shortcut.toSearchResult(rawQuery) }
                .sortedByDescending { it.score }
                .take(MAX_RESULTS)
        }

    private fun ShortcutInfo.toSearchResult(rawQuery: String): LauncherSearchResult? {
        val packageName = getPackage()
        val title = shortLabel?.toString()?.takeIf(String::isNotBlank) ?: return null
        val subtitle = longLabel?.toString()?.takeIf(String::isNotBlank) ?: packageName
        val score = LauncherSearchTextRanking.score(title, subtitle, rawQuery) ?: return null
        return LauncherSearchResult(
            providerId = id,
            resultId = userHandle.hashCode().toString() + ":" + packageName + ":" + this.id,
            title = title,
            subtitle = subtitle,
            category = LauncherSearchCategory.SHORTCUT,
            score = score + 20,
            action = LauncherLaunchShortcutSearchAction(packageName, this.id, userHandle),
        )
    }

    companion object {
        const val PROVIDER_ID = "launcher.shortcuts"
        private const val MAX_RESULTS = 16
    }
}

class LauncherContactsSearchProvider(context: Context) : LauncherSearchProvider, LauncherAsyncSearchProvider {
    private val appContext = context.applicationContext
    override val id: String = PROVIDER_ID
    override fun search(rawQuery: String): List<LauncherSearchResult> = emptyList()

    override suspend fun searchAsync(request: LauncherSearchRequest): List<LauncherSearchResult> =
        guardedLocalSearch(appContext, id) {
            withContext(Dispatchers.IO) {
                val term = request.rawQuery.trim()
                if (term.isBlank()) return@withContext emptyList()

                val results = mutableListOf<LauncherSearchResult>()
                val seenContactIds = mutableSetOf<Long>()

                // Phone-filter URI handles matching both contact names and formatted numbers.
                val phoneUri = Uri.withAppendedPath(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
                    Uri.encode(term),
                )
                val phoneColumns = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                )
                val phoneCursor = appContext.contentResolver.query(
                    phoneUri,
                    phoneColumns,
                    null,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC",
                ) ?: throw IllegalStateException("Phone contacts provider is unavailable")
                phoneCursor.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    )
                    val nameColumn = cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                    )
                    val numberColumn = cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                    )
                    while (cursor.moveToNext() && results.size < MAX_RESULTS) {
                        val contactId = cursor.getLong(idColumn)
                        if (contactId in seenContactIds) continue
                        val name = cursor.getString(nameColumn).orEmpty()
                        val number = cursor.getString(numberColumn).orEmpty()
                        val score = LauncherLocalPhoneSearchPolicy.score(name, number, term)
                            ?: continue
                        seenContactIds += contactId
                        val contactUri = Uri.withAppendedPath(
                            ContactsContract.Contacts.CONTENT_URI,
                            contactId.toString(),
                        )
                        results += LauncherSearchResult(
                            providerId = id,
                            resultId = contactId.toString(),
                            title = name.ifBlank { number },
                            subtitle = number.takeIf(String::isNotBlank),
                            category = LauncherSearchCategory.CONTACT,
                            score = score,
                            action = LauncherOpenUriSearchAction(
                                Intent.ACTION_VIEW,
                                contactUri.toString(),
                            ),
                        )
                    }
                }

                // The phone directory excludes contacts without a number. Query the general
                // name-filtered contacts directory as well; results remain local, bounded,
                // deduplicated, and require the same explicit READ_CONTACTS opt-in.
                if (results.size < MAX_RESULTS) {
                    val contactUri = Uri.withAppendedPath(
                        ContactsContract.Contacts.CONTENT_FILTER_URI,
                        Uri.encode(term),
                    )
                    val contactColumns = arrayOf(
                        ContactsContract.Contacts._ID,
                        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                    )
                    val contactCursor = appContext.contentResolver.query(
                        contactUri,
                        contactColumns,
                        null,
                        null,
                        "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC",
                    ) ?: throw IllegalStateException("Contacts provider is unavailable")
                    contactCursor.use { cursor ->
                        val idColumn = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
                        val nameColumn = cursor.getColumnIndexOrThrow(
                            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                        )
                        while (cursor.moveToNext() && results.size < MAX_RESULTS) {
                            val contactId = cursor.getLong(idColumn)
                            if (contactId in seenContactIds) continue
                            val name = cursor.getString(nameColumn).orEmpty()
                            val score = LauncherSearchTextRanking.score(name, null, term)
                                ?: continue
                            seenContactIds += contactId
                            results += LauncherSearchResult(
                                providerId = id,
                                resultId = contactId.toString(),
                                title = name,
                                subtitle = null,
                                category = LauncherSearchCategory.CONTACT,
                                score = score,
                                action = LauncherOpenUriSearchAction(
                                    Intent.ACTION_VIEW,
                                    Uri.withAppendedPath(
                                        ContactsContract.Contacts.CONTENT_URI,
                                        contactId.toString(),
                                    ).toString(),
                                ),
                            )
                        }
                    }
                }
                results
            }
        }

    companion object {
        const val PROVIDER_ID = "launcher.contacts"
        private const val MAX_RESULTS = 20
    }
}

class LauncherCallHistorySearchProvider(context: Context) : LauncherSearchProvider, LauncherAsyncSearchProvider {
    private val appContext = context.applicationContext
    override val id: String = PROVIDER_ID
    override fun search(rawQuery: String): List<LauncherSearchResult> = emptyList()

    override suspend fun searchAsync(request: LauncherSearchRequest): List<LauncherSearchResult> =
        guardedLocalSearch(appContext, id) {
            withContext(Dispatchers.IO) {
            val rawQuery = request.rawQuery.trim()
            if (rawQuery.isBlank() || !LauncherLocalSearchPermissions.isGranted(appContext, id)) return@withContext emptyList()
            val projection = arrayOf(CallLog.Calls._ID, CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.DATE)
            val results = mutableListOf<LauncherSearchResult>()
            val cursor = appContext.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC",
            ) ?: throw IllegalStateException("Call history provider is unavailable")
            cursor.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
                val nameIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                val numberIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                var scanned = 0
                while (cursor.moveToNext() && results.size < MAX_RESULTS && scanned++ < MAX_SCANNED) {
                    val rowId = cursor.getLong(idIndex)
                    val name = cursor.getString(nameIndex).orEmpty()
                    val number = cursor.getString(numberIndex).orEmpty()
                    val title = name.ifBlank { number }
                    val score = LauncherLocalPhoneSearchPolicy.score(title, number, rawQuery) ?: continue
                    results += LauncherSearchResult(
                        providerId = id,
                        resultId = rowId.toString(),
                        title = title,
                        subtitle = number.takeIf { it.isNotBlank() && it != title },
                        category = LauncherSearchCategory.CALL_HISTORY,
                        score = score,
                        action = LauncherOpenUriSearchAction(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null).toString()),
                    )
                }
            }
            results
        }

        }

    companion object {
        const val PROVIDER_ID = "launcher.call-history"
        private const val MAX_RESULTS = 16
        private const val MAX_SCANNED = 250
    }
}

class LauncherMessagesSearchProvider(context: Context) : LauncherSearchProvider, LauncherAsyncSearchProvider {
    private val appContext = context.applicationContext
    override val id: String = PROVIDER_ID
    override fun search(rawQuery: String): List<LauncherSearchResult> = emptyList()

    override suspend fun searchAsync(request: LauncherSearchRequest): List<LauncherSearchResult> =
        guardedLocalSearch(appContext, id) {
            withContext(Dispatchers.IO) {
            val rawQuery = request.rawQuery.trim()
            if (rawQuery.isBlank() || !LauncherLocalSearchPermissions.isGranted(appContext, id)) return@withContext emptyList()
            val projection = arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
            val wildcard = "%${escapeLike(rawQuery)}%"
            val selection =
                "${Telephony.Sms.ADDRESS} LIKE ? ESCAPE '\\' OR " +
                    "${Telephony.Sms.BODY} LIKE ? ESCAPE '\\'"
            val results = mutableListOf<LauncherSearchResult>()
            val cursor = appContext.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                selection,
                arrayOf(wildcard, wildcard),
                "${Telephony.Sms.DATE} DESC",
            ) ?: throw IllegalStateException("Messages provider is unavailable")
            cursor.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                while (cursor.moveToNext() && results.size < MAX_RESULTS) {
                    val rowId = cursor.getLong(idIndex)
                    val address = cursor.getString(addressIndex).orEmpty()
                    val body = cursor.getString(bodyIndex).orEmpty()
                    val title = address.ifBlank { "Message" }
                    val score = LauncherSearchTextRanking.score(title, body, rawQuery) ?: continue
                    results += LauncherSearchResult(
                        providerId = id,
                        resultId = rowId.toString(),
                        title = title,
                        subtitle = body.replace('\n', ' ').take(100).takeIf(String::isNotBlank),
                        category = LauncherSearchCategory.MESSAGE,
                        score = score,
                        action = LauncherOpenUriSearchAction(Intent.ACTION_VIEW, Uri.fromParts("sms", address, null).toString()),
                    )
                }
            }
            results
        }

        }

    companion object {
        const val PROVIDER_ID = "launcher.messages"
        private const val MAX_RESULTS = 16
    }
}

internal fun escapeLike(value: String): String =
    value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")


object LauncherRuntimeSearchProviderRegistry {
    fun catalog(
        context: Context,
        apps: List<android.content.pm.LauncherActivityInfo>,
        fileRoots: List<Uri> = emptyList(),
    ): LauncherSearchProviderCatalog = LauncherSearchProviderContract.evaluate(
        LauncherBuiltInSearchProviderRegistry.registrations(apps) +
            LauncherLocalSearchProviderRegistry.registrations(context) +
            listOf(LauncherFilesSearchProviderRegistration.registration(context, fileRoots)) +
            LauncherConnectedSearchProviderRegistry.registrations(context),
    )
}
