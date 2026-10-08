package com.goreecloud.launcher.core.launcher

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class LauncherOpenDocumentSearchAction(
    val uri: String,
    val mimeType: String?,
) : LauncherSearchAction

/**
 * Local filename search over Storage Access Framework trees explicitly selected by the user.
 *
 * The provider keeps a bounded process-local filename/MIME index. It does not read file contents,
 * request broad storage access, persist search results, or enumerate outside selected trees.
 */
class LauncherFilesSearchProvider(
    context: Context,
    private val roots: List<Uri>,
) : LauncherSearchProvider, LauncherAsyncSearchProvider {
    private val appContext = context.applicationContext
    private val indexMutex = Mutex()

    @Volatile
    private var cachedEntries: List<FileEntry>? = null

    override val id: String = PROVIDER_ID

    override fun search(rawQuery: String): List<LauncherSearchResult> = emptyList()

    override suspend fun searchAsync(
        request: LauncherSearchRequest,
    ): List<LauncherSearchResult> = withContext(Dispatchers.IO) {
        val rawQuery = request.rawQuery.trim()
        if (rawQuery.isBlank() || roots.isEmpty()) return@withContext emptyList()

        fileIndex()
            .mapNotNull { entry ->
                val score = LauncherSearchTextRanking.score(
                    title = entry.displayName,
                    subtitle = entry.mimeType,
                    rawQuery = rawQuery,
                ) ?: return@mapNotNull null

                LauncherSearchResult(
                    providerId = id,
                    resultId = entry.uri,
                    title = entry.displayName,
                    subtitle = entry.mimeType.takeUnless { it == GENERIC_MIME_TYPE },
                    category = LauncherSearchCategory.FILE,
                    score = score,
                    action = LauncherOpenDocumentSearchAction(
                        uri = entry.uri,
                        mimeType = entry.mimeType.takeUnless { it == GENERIC_MIME_TYPE },
                    ),
                )
            }
            .sortedByDescending { result -> result.score }
            .take(MAX_RESULTS)
    }

    private suspend fun fileIndex(): List<FileEntry> =
        cachedEntries ?: indexMutex.withLock {
            cachedEntries ?: buildIndex().also { cachedEntries = it }
        }

    private fun buildIndex(): List<FileEntry> {
        val normalizedRoots = roots
            .filter(DocumentsContract::isTreeUri)
            .distinctBy(Uri::toString)

        return LauncherFileSearchRootIsolation.collect(
            roots = normalizedRoots,
            maxEntries = MAX_INDEXED_FILES,
        ) { treeUri, remaining ->
            buildRootIndex(treeUri, remaining)
        }
    }

    private fun buildRootIndex(
        treeUri: Uri,
        maxEntries: Int,
    ): List<FileEntry> {
        if (maxEntries <= 0) return emptyList()

        val entries = mutableListOf<FileEntry>()
        val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val pending = ArrayDeque<PendingDirectory>()
        pending.add(PendingDirectory(rootDocumentId, depth = 0))

        while (pending.isNotEmpty() && entries.size < maxEntries) {
            val directory = pending.removeFirst()
            val childUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                directory.documentId,
            )
            appContext.contentResolver.query(
                childUri,
                PROJECTION,
                null,
                null,
                null,
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                )
                val nameIndex = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                )
                val mimeIndex = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                )

                while (cursor.moveToNext() && entries.size < maxEntries) {
                    val documentId = cursor.getString(idIndex) ?: continue
                    val displayName = cursor.getString(nameIndex)?.trim().orEmpty()
                    val mimeType = cursor.getString(mimeIndex).orEmpty()
                        .ifBlank { GENERIC_MIME_TYPE }

                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        if (directory.depth < MAX_DEPTH) {
                            pending.add(
                                PendingDirectory(
                                    documentId = documentId,
                                    depth = directory.depth + 1,
                                ),
                            )
                        }
                        continue
                    }
                    if (displayName.isBlank()) continue

                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                        treeUri,
                        documentId,
                    )
                    entries += FileEntry(
                        uri = documentUri.toString(),
                        displayName = displayName,
                        mimeType = mimeType,
                    )
                }
            }
        }
        return entries
    }

    private data class PendingDirectory(
        val documentId: String,
        val depth: Int,
    )

    private data class FileEntry(
        val uri: String,
        val displayName: String,
        val mimeType: String,
    )

    companion object {
        const val PROVIDER_ID = "launcher.files"
        const val GENERIC_MIME_TYPE = "application/octet-stream"
        private const val MAX_RESULTS = 24
        private const val MAX_INDEXED_FILES = 1_500
        private const val MAX_DEPTH = 8

        private val PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
    }
}

/**
 * Keeps selected Storage Access Framework roots failure-isolated.
 *
 * A revoked, malformed, or broken document-provider root contributes no entries instead of
 * suppressing results from every other selected root. The aggregate remains globally bounded.
 */
internal object LauncherFileSearchRootIsolation {
    fun <Root, Entry> collect(
        roots: List<Root>,
        maxEntries: Int,
        scanRoot: (Root, Int) -> List<Entry>,
    ): List<Entry> {
        require(maxEntries >= 0) { "maxEntries must not be negative" }
        if (maxEntries == 0) return emptyList()

        val entries = mutableListOf<Entry>()
        roots.forEach { root ->
            if (entries.size >= maxEntries) return@forEach
            val remaining = maxEntries - entries.size
            val contribution = runCatching {
                scanRoot(root, remaining)
            }.getOrDefault(emptyList())
            entries += contribution.take(remaining)
        }
        return entries
    }
}

object LauncherFilesSearchProviderRegistration {
    fun registration(
        context: Context,
        roots: List<Uri>,
    ): LauncherSearchProviderRegistration = LauncherSearchProviderRegistration(
        provider = LauncherFilesSearchProvider(context, roots),
        metadata = LauncherSearchProviderMetadata(
            providerId = LauncherFilesSearchProvider.PROVIDER_ID,
            contractVersion = LauncherSearchProviderContract.currentVersion,
            provenance = LauncherSearchProviderProvenance.LAUNCHER_BUILT_IN,
            offlineBehavior = LauncherSearchOfflineBehavior.LOCAL_ONLY,
            authorizationRequirement = LauncherSearchAuthorizationRequirement.USER_CONSENT,
            remoteProcessing = LauncherSearchRemoteProcessing.NONE,
            queryRetention = LauncherSearchQueryRetention.NONE,
        ),
    )
}
