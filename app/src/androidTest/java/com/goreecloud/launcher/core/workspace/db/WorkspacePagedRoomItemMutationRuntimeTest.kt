package com.goreecloud.launcher.core.workspace.db

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.goreecloud.launcher.core.workspace.WorkspaceAuthority
import com.goreecloud.launcher.core.workspace.WorkspaceGridPlacement
import com.goreecloud.launcher.core.workspace.WorkspaceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class WorkspacePagedRoomItemMutationRuntimeTest {
    private lateinit var context: Context
    private lateinit var database: LauncherDatabase
    private lateinit var dataStoreFile: File
    private var dataStoreScope: CoroutineScope? = null

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(DATABASE_NAME)
        database = Room.databaseBuilder(context, LauncherDatabase::class.java, DATABASE_NAME)
            .setDriver(AndroidSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        dataStoreFile = File(context.cacheDir, DATASTORE_FILE)
        dataStoreFile.delete()
    }

    @After
    fun tearDown() {
        runBlocking {
            dataStoreScope?.coroutineContext?.get(Job)?.cancelAndJoin()
            dataStoreScope = null
        }
        database.close()
        context.deleteDatabase(DATABASE_NAME)
        dataStoreFile.delete()
    }

    @Test
    fun crossPageItemMoveUsesValidatedPlacementAndPreservesTargetRankUniqueness() = runBlocking {
        val authorityRepository = WorkspaceRepository(openDataStore())
        authorityRepository.ensureDefaults(favoriteKeys = emptyList(), dockKeys = emptyList())
        promoteRoom(authorityRepository)

        val dao = database.workspaceDao()
        dao.upsertPages(
            listOf(
                WorkspacePageEntity(SOURCE_PAGE, WorkspaceContainerType.HOME, 1),
                WorkspacePageEntity(TARGET_PAGE, WorkspaceContainerType.HOME, 2),
            )
        )
        dao.upsertItems(
            listOf(
                WorkspaceItemEntity(
                    itemId = MOVED_ITEM,
                    pageId = SOURCE_PAGE,
                    itemType = WorkspaceItemType.APP,
                    appKey = APP_ONE,
                    rank = 0,
                    cellX = 0,
                    cellY = 0,
                ),
                WorkspaceItemEntity(
                    itemId = TARGET_ITEM,
                    pageId = TARGET_PAGE,
                    itemType = WorkspaceItemType.APP,
                    appKey = APP_TWO,
                    rank = 0,
                    cellX = 1,
                    cellY = 0,
                ),
            )
        )

        val repository = WorkspacePagedRoomMutationRepository(
            authorityRepository = authorityRepository,
            workspaceDaoProvider = { dao },
        )
        val grid = WorkspaceGridPlacement.Grid(columns = 4, rows = 4)

        assertEquals(
            WorkspacePagedRoomMutationResult.UpdatedItem(
                itemId = MOVED_ITEM,
                pageId = TARGET_PAGE,
                cellX = 0,
                cellY = 0,
                spanX = 1,
                spanY = 1,
            ),
            repository.moveHomeItem(
                grid = grid,
                itemId = MOVED_ITEM,
                targetPageId = TARGET_PAGE,
                targetPlacement = WorkspaceGridPlacement.Placement(MOVED_ITEM, 0, 0),
            ),
        )

        assertTrue(dao.readItems(listOf(SOURCE_PAGE)).none { it.itemId == MOVED_ITEM })
        val targetItems = dao.readItems(listOf(TARGET_PAGE)).associateBy { it.itemId }
        assertEquals(2, targetItems.size)
        assertEquals(0, targetItems.getValue(TARGET_ITEM).rank)
        assertEquals(1, targetItems.getValue(MOVED_ITEM).rank)
        assertEquals(0, targetItems.getValue(MOVED_ITEM).cellX)
        assertEquals(0, targetItems.getValue(MOVED_ITEM).cellY)

        val beforeRejectedMove = dao.readItems(listOf(TARGET_PAGE)).associateBy { it.itemId }
        assertEquals(
            WorkspacePagedRoomMutationResult.InvalidWorkspace,
            repository.moveHomeItem(
                grid = grid,
                itemId = MOVED_ITEM,
                targetPageId = TARGET_PAGE,
                targetPlacement = WorkspaceGridPlacement.Placement(MOVED_ITEM, 1, 0),
            ),
        )
        assertEquals(beforeRejectedMove, dao.readItems(listOf(TARGET_PAGE)).associateBy { it.itemId })
    }

    private suspend fun promoteRoom(authorityRepository: WorkspaceRepository) {
        var state = authorityRepository.state.first { it.initialized }
        assertEquals(
            WorkspaceMirrorResult.Verified,
            WorkspaceRelationalMirror(database.workspaceDao()).sync(state),
        )
        assertTrue(authorityRepository.markRoomVerified(state))
        state = authorityRepository.state.first { it.authority == WorkspaceAuthority.ROOM_VERIFIED }
        assertEquals(
            WorkspaceDualReadResult.Match,
            WorkspaceRelationalReader(database.workspaceDao()).reconcile(state),
        )
        assertTrue(authorityRepository.promoteRoomAuthority(state))
        authorityRepository.state.first { it.authority == WorkspaceAuthority.ROOM }
    }

    private fun openDataStore(): DataStore<Preferences> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStoreScope = scope
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { dataStoreFile })
    }

    private companion object {
        const val DATABASE_NAME = "launcher-paged-room-item-mutation-test.db"
        const val DATASTORE_FILE = "launcher-paged-room-item-mutation.preferences_pb"
        const val SOURCE_PAGE = "home:source"
        const val TARGET_PAGE = "home:target"
        const val MOVED_ITEM = "native:item:one"
        const val TARGET_ITEM = "native:item:two"
        const val APP_ONE = "10:com.example.one/.MainActivity"
        const val APP_TWO = "10:com.example.two/.MainActivity"
    }
}
