package com.goreecloud.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherFolderPresentationPolicyTest {
    @Test
    fun `wide phone folder uses four columns`() {
        assertEquals(
            4,
            launcherFolderGridColumns(
                availableWidthDp = 360f,
                largeText = false,
            ),
        )
    }

    @Test
    fun `narrow folder keeps three columns`() {
        assertEquals(
            3,
            launcherFolderGridColumns(
                availableWidthDp = 320f,
                largeText = false,
            ),
        )
    }

    @Test
    fun `large text favors roomier three column layout`() {
        assertEquals(
            3,
            launcherFolderGridColumns(
                availableWidthDp = 420f,
                largeText = true,
            ),
        )
    }

    @Test
    fun `invalid measured width fails closed to three columns`() {
        assertEquals(
            3,
            launcherFolderGridColumns(
                availableWidthDp = 0f,
                largeText = false,
            ),
        )
    }
    @Test
    fun `folder panel keeps wallpaper context and grows on larger canvases`() {
        assertEquals(368f, launcherFolderPanelWidthDp(400f))
        assertEquals(552f, launcherFolderPanelWidthDp(600f))
        assertEquals(600f, launcherFolderPanelWidthDp(720f))
        assertEquals(600f, launcherFolderPanelWidthDp(900f))
        assertEquals(520f, launcherFolderPanelWidthDp(0f))
    }

    @Test
    fun `folder cells yield vertical room to large text`() {
        assertEquals(
            98f,
            launcherFolderCellMinHeightDp(
                largeText = false,
                extraLargeText = false,
            ),
        )
        assertEquals(
            108f,
            launcherFolderCellMinHeightDp(
                largeText = true,
                extraLargeText = false,
            ),
        )
        assertEquals(
            118f,
            launcherFolderCellMinHeightDp(
                largeText = true,
                extraLargeText = true,
            ),
        )
    }


    @Test
    fun `folder pager reserves the final slot for Add apps`() {
        assertEquals(
            1,
            launcherFolderPageCount(
                itemCount = 11,
                columns = 4,
                includeAddTile = true,
            ),
        )
        assertEquals(
            2,
            launcherFolderPageCount(
                itemCount = 12,
                columns = 4,
                includeAddTile = true,
            ),
        )
    }

    @Test
    fun `folder pager uses three rows and remains bounded without Add tile`() {
        assertEquals(
            1,
            launcherFolderPageCount(
                itemCount = 9,
                columns = 3,
                includeAddTile = false,
            ),
        )
        assertEquals(
            2,
            launcherFolderPageCount(
                itemCount = 10,
                columns = 3,
                includeAddTile = false,
            ),
        )
        assertEquals(
            1,
            launcherFolderPageCount(
                itemCount = 0,
                columns = 0,
                includeAddTile = false,
            ),
        )
    }


    @Test
    fun `folder app picker gives large text more cell width`() {
        assertEquals(
            82f,
            launcherFolderPickerMinCellWidthDp(
                largeText = false,
                extraLargeText = false,
            ),
        )
        assertEquals(
            104f,
            launcherFolderPickerMinCellWidthDp(
                largeText = true,
                extraLargeText = false,
            ),
        )
        assertEquals(
            116f,
            launcherFolderPickerMinCellWidthDp(
                largeText = true,
                extraLargeText = true,
            ),
        )
    }

}
