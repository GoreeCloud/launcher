package com.goreecloud.launcher.core.workspace

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

/**
 * Versioned, deterministic Development portability boundary for Launcher workspace placement.
 *
 * This codec intentionally covers only the framework-independent grid/page/placement model.
 * It does not write Room/DataStore state, bind widgets, resolve Android profiles/packages, or
 * represent a complete Launcher backup/restore implementation.
 */
object WorkspacePortableSnapshot {
    const val FORMAT = "goreecloud-launcher-workspace-snapshot"
    const val VERSION = 1

    private const val MAX_SNAPSHOT_BYTES = 1 shl 20
    private const val MAX_GRID_DIMENSION = 1024

    data class Snapshot(
        val grid: WorkspaceGridPlacement.Grid,
        val pages: List<WorkspacePagedPlacement.Page>,
    )

    sealed interface DecodeResult {
        data class Success(val snapshot: Snapshot) : DecodeResult
        data class Invalid(val reason: String) : DecodeResult
    }

    fun encode(snapshot: Snapshot): String {
        validate(snapshot)?.let { reason ->
            throw IllegalArgumentException("invalid workspace snapshot: $reason")
        }

        val lines = mutableListOf(
            "format=$FORMAT",
            "version=$VERSION",
            "grid=${snapshot.grid.columns},${snapshot.grid.rows}",
        )

        WorkspacePagedPlacement.ordered(snapshot.pages).forEach { page ->
            lines += "page=${encodeToken(page.pageId)},${page.rank}"
            page.placements
                .sortedWith(
                    compareBy<WorkspaceGridPlacement.Placement>(
                        { it.cellY },
                        { it.cellX },
                        { it.spanY },
                        { it.spanX },
                        { it.itemId },
                    )
                )
                .forEach { placement ->
                    lines += buildString {
                        append("item=")
                        append(encodeToken(page.pageId))
                        append(',')
                        append(encodeToken(placement.itemId))
                        append(',')
                        append(placement.cellX)
                        append(',')
                        append(placement.cellY)
                        append(',')
                        append(placement.spanX)
                        append(',')
                        append(placement.spanY)
                    }
                }
        }

        val payload = lines.joinToString("\n")
        return "$payload\nchecksum=${sha256Hex(payload)}\n"
    }

    fun decode(encoded: String): DecodeResult {
        if (encoded.toByteArray(StandardCharsets.UTF_8).size > MAX_SNAPSHOT_BYTES) {
            return DecodeResult.Invalid("snapshot exceeds the bounded size limit")
        }
        if ('\r' in encoded) {
            return DecodeResult.Invalid("snapshot must use canonical LF line endings")
        }

        val canonical = encoded.removeSuffix("\n")
        if (canonical.isBlank() || '\n' !in canonical) {
            return DecodeResult.Invalid("snapshot is incomplete")
        }
        val lines = canonical.split('\n')
        if (lines.size < 5 || lines.any { it.isBlank() }) {
            return DecodeResult.Invalid("snapshot contains missing or blank records")
        }

        val checksumRecord = lines.last()
        if (!checksumRecord.startsWith("checksum=")) {
            return DecodeResult.Invalid("snapshot checksum record is missing")
        }
        val checksum = checksumRecord.removePrefix("checksum=")
        if (!checksum.matches(Regex("[0-9a-f]{64}"))) {
            return DecodeResult.Invalid("snapshot checksum is not canonical SHA-256")
        }
        val payloadLines = lines.dropLast(1)
        val payload = payloadLines.joinToString("\n")
        if (sha256Hex(payload) != checksum) {
            return DecodeResult.Invalid("snapshot integrity check failed")
        }

        if (payloadLines.getOrNull(0) != "format=$FORMAT") {
            return DecodeResult.Invalid("unsupported snapshot format")
        }
        if (payloadLines.getOrNull(1) != "version=$VERSION") {
            return DecodeResult.Invalid("unsupported snapshot version")
        }

        val gridFields = payloadLines.getOrNull(2)
            ?.takeIf { it.startsWith("grid=") }
            ?.removePrefix("grid=")
            ?.split(',')
            ?: return DecodeResult.Invalid("grid record is missing")
        if (gridFields.size != 2) return DecodeResult.Invalid("grid record is invalid")
        val columns = parseCanonicalInt(gridFields[0])
            ?: return DecodeResult.Invalid("grid columns are invalid")
        val rows = parseCanonicalInt(gridFields[1])
            ?: return DecodeResult.Invalid("grid rows are invalid")
        if (columns !in 1..MAX_GRID_DIMENSION || rows !in 1..MAX_GRID_DIMENSION) {
            return DecodeResult.Invalid("grid dimensions are outside the bounded range")
        }

        data class PageRecord(val pageId: String, val rank: Int)
        data class ItemRecord(
            val pageId: String,
            val itemId: String,
            val cellX: Int,
            val cellY: Int,
            val spanX: Int,
            val spanY: Int,
        )

        val pages = mutableListOf<PageRecord>()
        val items = mutableListOf<ItemRecord>()

        for (line in payloadLines.drop(3)) {
            when {
                line.startsWith("page=") -> {
                    val fields = line.removePrefix("page=").split(',')
                    if (fields.size != 2) return DecodeResult.Invalid("page record is invalid")
                    val pageId = decodeToken(fields[0])
                        ?: return DecodeResult.Invalid("page identity is invalid")
                    val rank = parseCanonicalInt(fields[1])
                        ?: return DecodeResult.Invalid("page rank is invalid")
                    if (rank < 0) return DecodeResult.Invalid("page rank must be non-negative")
                    pages += PageRecord(pageId, rank)
                }

                line.startsWith("item=") -> {
                    val fields = line.removePrefix("item=").split(',')
                    if (fields.size != 6) return DecodeResult.Invalid("item record is invalid")
                    val pageId = decodeToken(fields[0])
                        ?: return DecodeResult.Invalid("item page identity is invalid")
                    val itemId = decodeToken(fields[1])
                        ?: return DecodeResult.Invalid("item identity is invalid")
                    val cellX = parseCanonicalInt(fields[2])
                        ?: return DecodeResult.Invalid("item cellX is invalid")
                    val cellY = parseCanonicalInt(fields[3])
                        ?: return DecodeResult.Invalid("item cellY is invalid")
                    val spanX = parseCanonicalInt(fields[4])
                        ?: return DecodeResult.Invalid("item spanX is invalid")
                    val spanY = parseCanonicalInt(fields[5])
                        ?: return DecodeResult.Invalid("item spanY is invalid")
                    if (cellX < 0 || cellY < 0 || spanX <= 0 || spanY <= 0) {
                        return DecodeResult.Invalid("item placement values are outside the allowed range")
                    }
                    items += ItemRecord(pageId, itemId, cellX, cellY, spanX, spanY)
                }

                else -> return DecodeResult.Invalid("snapshot contains an unknown record")
            }
        }

        if (pages.isEmpty()) return DecodeResult.Invalid("snapshot must contain at least one page")
        if (pages.map { it.pageId }.toSet().size != pages.size) {
            return DecodeResult.Invalid("snapshot contains duplicate page identities")
        }
        if (pages.map { it.rank }.toSet().size != pages.size) {
            return DecodeResult.Invalid("snapshot contains duplicate page ranks")
        }
        val orderedPageRecords = pages.sortedBy { it.rank }
        if (orderedPageRecords.anyIndexed { index, page -> page.rank != index }) {
            return DecodeResult.Invalid("page ranks must form a contiguous zero-based sequence")
        }
        val knownPageIds = pages.mapTo(mutableSetOf()) { it.pageId }
        if (items.any { it.pageId !in knownPageIds }) {
            return DecodeResult.Invalid("item record references an unknown page")
        }

        val grid = try {
            WorkspaceGridPlacement.Grid(columns, rows)
        } catch (_: IllegalArgumentException) {
            return DecodeResult.Invalid("grid record is invalid")
        }

        val decodedPages = try {
            orderedPageRecords.map { page ->
                WorkspacePagedPlacement.Page(
                    pageId = page.pageId,
                    rank = page.rank,
                    placements = items
                        .filter { it.pageId == page.pageId }
                        .map { item ->
                            WorkspaceGridPlacement.Placement(
                                itemId = item.itemId,
                                cellX = item.cellX,
                                cellY = item.cellY,
                                spanX = item.spanX,
                                spanY = item.spanY,
                            )
                        },
                )
            }
        } catch (_: IllegalArgumentException) {
            return DecodeResult.Invalid("snapshot contains an invalid workspace value")
        }

        val snapshot = Snapshot(grid, decodedPages)
        validate(snapshot)?.let { reason -> return DecodeResult.Invalid(reason) }
        return DecodeResult.Success(snapshot)
    }

    private fun validate(snapshot: Snapshot): String? {
        if (snapshot.grid.columns !in 1..MAX_GRID_DIMENSION ||
            snapshot.grid.rows !in 1..MAX_GRID_DIMENSION
        ) {
            return "grid dimensions are outside the bounded range"
        }
        if (snapshot.pages.isEmpty()) return "snapshot must contain at least one page"

        val orderedPages = WorkspacePagedPlacement.ordered(snapshot.pages)
        if (orderedPages.anyIndexed { index, page -> page.rank != index }) {
            return "page ranks must form a contiguous zero-based sequence"
        }
        for (page in snapshot.pages) {
            for (placement in page.placements) {
                val endX = placement.cellX.toLong() + placement.spanX.toLong()
                val endY = placement.cellY.toLong() + placement.spanY.toLong()
                if (endX > snapshot.grid.columns.toLong() || endY > snapshot.grid.rows.toLong()) {
                    return "workspace contains an out-of-bounds placement"
                }
            }
        }

        return when (WorkspacePagedPlacement.validate(snapshot.grid, snapshot.pages)) {
            WorkspacePagedPlacement.Validation.Valid -> null
            else -> "workspace placement validation failed"
        }
    }

    private fun encodeToken(value: String): String =
        Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeToken(value: String): String? {
        if (value.isBlank()) return null
        return try {
            val decoded = String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
            decoded.takeIf { it.isNotBlank() && encodeToken(it) == value }
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun parseCanonicalInt(value: String): Int? {
        if (!value.matches(Regex("0|[1-9][0-9]*"))) return null
        return value.toIntOrNull()
    }

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private inline fun <T> Iterable<T>.anyIndexed(predicate: (index: Int, T) -> Boolean): Boolean {
        var index = 0
        for (item in this) {
            if (predicate(index, item)) return true
            index += 1
        }
        return false
    }
}
