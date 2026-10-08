package com.goreecloud.launcher.core.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test-only native semantic evaluation of Glaze Motion Motion Core 0.4.
 *
 * This deliberately lives under src/test. It is evidence that the Experimental
 * semantics can map onto Launcher's real workspace ordering domain without making
 * Glaze Motion a production dependency.
 */
class GlazeMotionExperimentalConsumerTest {
    private enum class ReorderCommand {
        PREVIOUS,
        NEXT,
        FIRST,
        LAST,
    }

    private data class ReorderResult(
        val items: List<String>,
        val fromIndex: Int,
        val toIndex: Int,
        val position: Int,
        val total: Int,
        val changed: Boolean,
    )

    private object GlazeMotionExperimentalMapping {
        const val REFERENCE_REVISION = "e8f68770540d00499b5613a00310ac7002a674fd"

        fun reorder(
            items: List<String>,
            key: String,
            command: ReorderCommand,
        ): ReorderResult? {
            val fromIndex = items.indexOf(key)
            if (fromIndex == -1) return null

            val updated = when (command) {
                ReorderCommand.PREVIOUS ->
                    WorkspaceCodec.moved(items, key, WorkspaceMoveDirection.EARLIER)
                ReorderCommand.NEXT ->
                    WorkspaceCodec.moved(items, key, WorkspaceMoveDirection.LATER)
                ReorderCommand.FIRST ->
                    items.firstOrNull()?.let { WorkspaceCodec.movedToTarget(items, key, it) } ?: items
                ReorderCommand.LAST ->
                    items.lastOrNull()?.let { WorkspaceCodec.movedToTarget(items, key, it) } ?: items
            }
            val toIndex = updated.indexOf(key)
            return ReorderResult(
                items = updated,
                fromIndex = fromIndex,
                toIndex = toIndex,
                position = toIndex + 1,
                total = updated.size,
                changed = updated != items,
            )
        }

        fun allowsOptionalSettling(
            reducedMotion: Boolean,
            activeSettling: Int,
            maximumConcurrentSettling: Int = 2,
        ): Boolean =
            !reducedMotion && activeSettling < maximumConcurrentSettling
    }

    @Test
    fun previousAndNextCommandsUseLauncherOrderingDomain() {
        val original = listOf("one", "two", "three")

        val previous = GlazeMotionExperimentalMapping.reorder(
            original,
            "two",
            ReorderCommand.PREVIOUS,
        )
        assertEquals(listOf("two", "one", "three"), previous?.items)
        assertEquals(0, previous?.toIndex)

        val next = GlazeMotionExperimentalMapping.reorder(
            original,
            "two",
            ReorderCommand.NEXT,
        )
        assertEquals(listOf("one", "three", "two"), next?.items)
        assertEquals(2, next?.toIndex)
    }

    @Test
    fun firstAndLastCommandsRemainSemanticAndDeterministic() {
        val original = listOf("one", "two", "three", "four")

        val first = GlazeMotionExperimentalMapping.reorder(
            original,
            "three",
            ReorderCommand.FIRST,
        )
        assertEquals(listOf("three", "one", "two", "four"), first?.items)
        assertEquals(1, first?.position)
        assertEquals(4, first?.total)
        assertTrue(first?.changed == true)

        val last = GlazeMotionExperimentalMapping.reorder(
            original,
            "two",
            ReorderCommand.LAST,
        )
        assertEquals(listOf("one", "three", "four", "two"), last?.items)
        assertEquals(4, last?.position)
        assertEquals(4, last?.total)
    }

    @Test
    fun resultExposesLocalizablePositionMetadataWithoutAnnouncementCopy() {
        val result = GlazeMotionExperimentalMapping.reorder(
            listOf("one", "two", "three"),
            "two",
            ReorderCommand.NEXT,
        )

        assertEquals(1, result?.fromIndex)
        assertEquals(2, result?.toIndex)
        assertEquals(3, result?.position)
        assertEquals(3, result?.total)
        assertTrue(result?.changed == true)
    }

    @Test
    fun optionalSettlingFailsClosedForReducedMotionOrSaturation() {
        assertFalse(
            GlazeMotionExperimentalMapping.allowsOptionalSettling(
                reducedMotion = true,
                activeSettling = 0,
            )
        )
        assertFalse(
            GlazeMotionExperimentalMapping.allowsOptionalSettling(
                reducedMotion = false,
                activeSettling = 2,
            )
        )
        assertTrue(
            GlazeMotionExperimentalMapping.allowsOptionalSettling(
                reducedMotion = false,
                activeSettling = 1,
            )
        )
    }

    @Test
    fun missingStableKeyCannotInventAStateTransition() {
        val result = GlazeMotionExperimentalMapping.reorder(
            listOf("one", "two"),
            "missing",
            ReorderCommand.NEXT,
        )
        assertNull(result)
    }
}
