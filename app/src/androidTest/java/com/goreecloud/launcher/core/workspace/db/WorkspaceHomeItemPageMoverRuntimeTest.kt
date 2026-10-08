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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class WorkspaceHomeItemPageMoverRuntimeTest {
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
    fun secondaryMovePreservesCanonicalPrimaryAndChoosesFirstFreeCell() = runBlocking {
        val authorityRepository = WorkspaceRepository(openDataStore())
        authorityRepository.ensureDefaults(
            favoriteKeys = listOf(APP_ONE),
            dockKeys = emptyList(),
        )
        val mutations = WorkspacePagedRoomMutationRepository(
            authorityRepository = authorityRepository,
            workspaceDaoProvider = { database.workspaceDao() },
        )
        val mover = WorkspaceHomeItemPageMover(
            authorityRepository = authorityRepository,
            workspaceDaoProvider = { database.workspaceDao() },
            mutationRepository = mutations,
        )

        assertEquals(
            WorkspacePagedRoomMutationResult.Reserved,
            mover.moveAppToPage("home:1", APP_TWO, "home:2"),
        )

        promoteRoomAuthority(authorityRepository)
        database.workspaceDao().upsertPages(
            listOf(
                WorkspacePageEntity("home:1", WorkspaceContainerType.HOME, 1),
                WorkspacePageEntity("home:2", WorkspaceContainerType.HOME, 2),
            )
        )
        database.workspaceDao().upsertItems(
            listOf(
                WorkspaceItemEntity(
                    itemId = "native:item:two",
                    pageId = "home:1",
                    itemType = WorkspaceItemType.APP,
                    appKey = APP_TWO,
                    rank = 0,
                    cellX = 0,
                    cellY = 0,
                ),
                WorkspaceItemEntity(
                    itemId = "native:item:three",
                    pageId = "home:2",
                    itemType = WorkspaceItemType.APP,
                    appKey = APP_THREE,
                    rank = 0,
                    cellX = 0,
                    cellY = 0,
                ),
            )
        )

        assertEquals(
            WorkspacePagedRoomMutationResult.UpdatedItem(
                itemId = "native:item:two",
                pageId = "home:2",
                cellX = 1,
                cellY = 0,
                spanX = 1,
                spanY = 1,
            ),
            mover.moveAppToPage("home:1", APP_TWO, "home:2"),
        )

        assertTrue(database.workspaceDao().readItems(listOf("home:1")).isEmpty())
        val targetItems = database.workspaceDao().readItems(listOf("home:2")).sortedBy { it.rank }
        assertEquals(listOf(APP_THREE, APP_TWO), targetItems.map { it.appKey })
        assertEquals(1, targetItems.last().cellX)
        assertEquals(0, targetItems.last().cellY)

        val primaryItems = database.workspaceDao()
            .readItems(listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID))
        assertEquals(listOf(APP_ONE), primaryItems.map { it.appKey })
        assertNull(primaryItems.single().cellX)
        assertNull(primaryItems.single().cellY)
        assertEquals(
            WorkspaceRelationalSnapshot(
                favoriteKeys = listOf(APP_ONE),
                dockKeys = emptyList(),
            ),
            WorkspaceCanonicalRoomPlacementReader.read(database.workspaceDao()),
        )
        assertEquals(
            WorkspacePostCutoverHealthResult.Healthy,
            WorkspacePostCutoverHealthEvaluator(
                repository = authorityRepository,
                workspaceDaoProvider = { database.workspaceDao() },
            ).evaluate(),
        )

        assertEquals(
            WorkspacePagedRoomMutationResult.PrimaryPageProtected,
            mover.moveAppToPage("home:2", APP_TWO, WorkspaceLegacyImportMapper.HOME_PAGE_ID),
        )
        assertEquals(
            WorkspacePagedRoomMutationResult.PrimaryPageProtected,
            mover.moveAppToPage(WorkspaceLegacyImportMapper.HOME_PAGE_ID, APP_ONE, "home:2"),
        )
        assertEquals(
            WorkspacePagedRoomMutationResult.PageNotFound,
            mover.moveAppToPage("home:2", APP_TWO, "missing"),
        )
        assertEquals(
            WorkspacePagedRoomMutationResult.InvalidWorkspace,
            mover.moveAppToPage("home:2", APP_TWO, "home:2"),
        )
    }

    @Test
    fun primaryBoundaryMoveCompactsCanonicalRanksAndSupportsRoundTrip() = runBlocking {
        val authorityRepository = WorkspaceRepository(openDataStore())
        authorityRepository.ensureDefaults(
            favoriteKeys = listOf(APP_ONE, APP_TWO),
            dockKeys = emptyList(),
        )
        promoteRoomAuthority(authorityRepository)

        val grid = WorkspaceGridPlacement.Grid(columns = 4, rows = 5)
        val primarySpatialRepository = WorkspacePrimaryHomeSpatialRepository(
            authorityRepository = authorityRepository,
            workspaceDaoProvider = { database.workspaceDao() },
        )
        assertEquals(
            WorkspacePrimaryHomeSpatialResult.Ready(
                changed = true,
                columns = 4,
                rows = 5,
            ),
            primarySpatialRepository.ensureGrid(columns = 4, rows = 5),
        )
        database.workspaceDao().upsertPages(
            listOf(WorkspacePageEntity("home:1", WorkspaceContainerType.HOME, 1))
        )

        val mover = WorkspaceHomeItemPageMover(
            authorityRepository = authorityRepository,
            workspaceDaoProvider = { database.workspaceDao() },
            mutationRepository = WorkspacePagedRoomMutationRepository(
                authorityRepository = authorityRepository,
                workspaceDaoProvider = { database.workspaceDao() },
            ),
        )

        assertEquals(
            WorkspacePagedRoomMutationResult.UpdatedItem(
                itemId = "legacy:home:$APP_ONE",
                pageId = "home:1",
                cellX = 0,
                cellY = 0,
                spanX = 1,
                spanY = 1,
            ),
            mover.moveAppToPage(
                sourcePageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                appKey = APP_ONE,
                targetPageId = "home:1",
                primaryGrid = grid,
            ),
        )

        val primaryAfterMoveOut = database.workspaceDao()
            .readItems(listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID))
            .sortedBy { it.rank }
        assertEquals(listOf(APP_TWO), primaryAfterMoveOut.map { it.appKey })
        assertEquals(listOf(0), primaryAfterMoveOut.map { it.rank })
        assertEquals(1, primaryAfterMoveOut.single().cellX)
        assertEquals(0, primaryAfterMoveOut.single().cellY)
        assertEquals(
            WorkspaceRelationalSnapshot(
                favoriteKeys = listOf(APP_TWO),
                dockKeys = emptyList(),
            ),
            WorkspaceCanonicalRoomPlacementReader.read(database.workspaceDao()),
        )

        assertEquals(
            WorkspacePagedRoomMutationResult.UpdatedItem(
                itemId = "legacy:home:$APP_ONE",
                pageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                cellX = 0,
                cellY = 0,
                spanX = 1,
                spanY = 1,
            ),
            mover.moveAppToPage(
                sourcePageId = "home:1",
                appKey = APP_ONE,
                targetPageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                primaryGrid = grid,
            ),
        )

        assertTrue(database.workspaceDao().readItems(listOf("home:1")).isEmpty())
        val primaryAfterRoundTrip = database.workspaceDao()
            .readItems(listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID))
            .sortedBy { it.rank }
        assertEquals(listOf(APP_TWO, APP_ONE), primaryAfterRoundTrip.map { it.appKey })
        assertEquals(listOf(0, 1), primaryAfterRoundTrip.map { it.rank })
        assertEquals(1, primaryAfterRoundTrip[0].cellX)
        assertEquals(0, primaryAfterRoundTrip[0].cellY)
        assertEquals(0, primaryAfterRoundTrip[1].cellX)
        assertEquals(0, primaryAfterRoundTrip[1].cellY)
        assertEquals(
            WorkspaceRelationalSnapshot(
                favoriteKeys = listOf(APP_TWO, APP_ONE),
                dockKeys = emptyList(),
            ),
            WorkspaceCanonicalRoomPlacementReader.read(database.workspaceDao()),
        )
        assertEquals(
            WorkspacePostCutoverHealthResult.Healthy,
            WorkspacePostCutoverHealthEvaluator(
                repository = authorityRepository,
                workspaceDaoProvider = { database.workspaceDao() },
            ).evaluate(),
        )
    }

    @Test
    fun exactCrossPageCellMovePersistsRequestedCellAndRejectsCollision() = runBlocking {
        val authorityRepository = WorkspaceRepository(openDataStore())
        authorityRepository.ensureDefaults(
            favoriteKeys = listOf(APP_ONE, APP_TWO),
            dockKeys = emptyList(),
        )
        promoteRoomAuthority(authorityRepository)

        val grid = WorkspaceGridPlacement.Grid(columns = 4, rows = 5)
        val primarySpatialRepository = WorkspacePrimaryHomeSpatialRepository(
            authorityRepository = authorityRepository,
            workspaceDaoProvider = { database.workspaceDao() },
        )
        assertEquals(
            WorkspacePrimaryHomeSpatialResult.Ready(
                changed = true,
                columns = 4,
                rows = 5,
            ),
            primarySpatialRepository.ensureGrid(columns = 4, rows = 5),
        )
        database.workspaceDao().upsertPages(
            listOf(WorkspacePageEntity("home:1", WorkspaceContainerType.HOME, 1))
        )

        val mover = WorkspaceHomeItemPageMover(
            authorityRepository = authorityRepository,
            workspaceDaoProvider = { database.workspaceDao() },
            mutationRepository = WorkspacePagedRoomMutationRepository(
                authorityRepository = authorityRepository,
                workspaceDaoProvider = { database.workspaceDao() },
            ),
        )

        assertEquals(
            WorkspacePagedRoomMutationResult.UpdatedItem(
                itemId = "legacy:home:$APP_ONE",
                pageId = "home:1",
                cellX = 3,
                cellY = 2,
                spanX = 1,
                spanY = 1,
            ),
            mover.moveAppToPage(
                sourcePageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                appKey = APP_ONE,
                targetPageId = "home:1",
                primaryGrid = grid,
                targetCellX = 3,
                targetCellY = 2,
            ),
        )
        val secondary = database.workspaceDao().readItems(listOf("home:1"))
        assertEquals(1, secondary.size)
        assertEquals(3, secondary.single().cellX)
        assertEquals(2, secondary.single().cellY)

        assertEquals(
            WorkspacePagedRoomMutationResult.InvalidWorkspace,
            mover.moveAppToPage(
                sourcePageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                appKey = APP_TWO,
                targetPageId = "home:1",
                primaryGrid = grid,
                targetCellX = 3,
                targetCellY = 2,
            ),
        )
        assertEquals(
            listOf(APP_TWO),
            database.workspaceDao()
                .readItems(listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID))
                .mapNotNull { it.appKey },
        )

        assertEquals(
            WorkspacePagedRoomMutationResult.UpdatedItem(
                itemId = "legacy:home:$APP_ONE",
                pageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                cellX = 2,
                cellY = 2,
                spanX = 1,
                spanY = 1,
            ),
            mover.moveAppToPage(
                sourcePageId = "home:1",
                appKey = APP_ONE,
                targetPageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                primaryGrid = grid,
                targetCellX = 2,
                targetCellY = 2,
            ),
        )
        val restored = database.workspaceDao()
            .readItems(listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID))
            .associateBy { it.appKey }
        assertEquals(2, restored.getValue(APP_ONE).cellX)
        assertEquals(2, restored.getValue(APP_ONE).cellY)
        assertEquals(1, restored.getValue(APP_TWO).cellX)
        assertEquals(0, restored.getValue(APP_TWO).cellY)
    }

    @Test
    fun oneCellMovementRejectsOccupiedAndOutOfBoundsTargetsWithoutDisplacement() = runBlocking {
        val authorityRepository = WorkspaceRepository(openDataStore())
        authorityRepository.ensureDefaults(
            favoriteKeys = listOf(APP_ONE),
            dockKeys = emptyList(),
        )
        promoteRoomAuthority(authorityRepository)
        val mover = WorkspaceHomeItemPageMover(
            authorityRepository = authorityRepository,
            workspaceDaoProvider = { database.workspaceDao() },
            mutationRepository = WorkspacePagedRoomMutationRepository(
                authorityRepository = authorityRepository,
                workspaceDaoProvider = { database.workspaceDao() },
            ),
        )

        database.workspaceDao().upsertPages(
            listOf(WorkspacePageEntity("home:1", WorkspaceContainerType.HOME, 1))
        )
        database.workspaceDao().upsertItems(
            listOf(
                WorkspaceItemEntity(
                    itemId = "native:item:two",
                    pageId = "home:1",
                    itemType = WorkspaceItemType.APP,
                    appKey = APP_TWO,
                    rank = 0,
                    cellX = 1,
                    cellY = 0,
                ),
                WorkspaceItemEntity(
                    itemId = "native:item:three",
                    pageId = "home:1",
                    itemType = WorkspaceItemType.APP,
                    appKey = APP_THREE,
                    rank = 1,
                    cellX = 2,
                    cellY = 0,
                ),
            )
        )

        assertEquals(
            WorkspacePagedRoomMutationResult.UpdatedItem(
                itemId = "native:item:two",
                pageId = "home:1",
                cellX = 0,
                cellY = 0,
                spanX = 1,
                spanY = 1,
            ),
            mover.moveAppOneCellWithinPage("home:1", APP_TWO, WorkspaceHomeSpatialDirection.LEFT),
        )
        assertEquals(
            WorkspacePagedRoomMutationResult.InvalidWorkspace,
            mover.moveAppOneCellWithinPage("home:1", APP_TWO, WorkspaceHomeSpatialDirection.LEFT),
        )
        assertEquals(
            WorkspacePagedRoomMutationResult.UpdatedItem(
                itemId = "native:item:two",
                pageId = "home:1",
                cellX = 1,
                cellY = 0,
                spanX = 1,
                spanY = 1,
            ),
            mover.moveAppOneCellWithinPage("home:1", APP_TWO, WorkspaceHomeSpatialDirection.RIGHT),
        )
        assertEquals(
            WorkspacePagedRoomMutationResult.InvalidWorkspace,
            mover.moveAppOneCellWithinPage("home:1", APP_TWO, WorkspaceHomeSpatialDirection.RIGHT),
        )
        assertEquals(
            WorkspacePagedRoomMutationResult.InvalidWorkspace,
            mover.moveAppOneCellWithinPage("home:1", APP_TWO, WorkspaceHomeSpatialDirection.UP),
        )
        assertEquals(
            WorkspacePagedRoomMutationResult.UpdatedItem(
                itemId = "native:item:two",
                pageId = "home:1",
                cellX = 1,
                cellY = 1,
                spanX = 1,
                spanY = 1,
            ),
            mover.moveAppOneCellWithinPage("home:1", APP_TWO, WorkspaceHomeSpatialDirection.DOWN),
        )

        val items = database.workspaceDao().readItems(listOf("home:1")).associateBy { it.appKey }
        assertEquals(1, items.getValue(APP_TWO).cellX)
        assertEquals(1, items.getValue(APP_TWO).cellY)
        assertEquals(2, items.getValue(APP_THREE).cellX)
        assertEquals(0, items.getValue(APP_THREE).cellY)
        assertEquals(
            WorkspacePostCutoverHealthResult.Healthy,
            WorkspacePostCutoverHealthEvaluator(
                repository = authorityRepository,
                workspaceDaoProvider = { database.workspaceDao() },
            ).evaluate(),
        )
    }

    @Test
    fun exactSamePageCellMoveUsesConfiguredGridAndRejectsInvalidTargets() = runBlocking {
        val authorityRepository = WorkspaceRepository(openDataStore())
        authorityRepository.ensureDefaults(
            favoriteKeys = listOf(APP_ONE),
            dockKeys = emptyList(),
        )
        promoteRoomAuthority(authorityRepository)
        val mover = WorkspaceHomeItemPageMover(
            authorityRepository = authorityRepository,
            workspaceDaoProvider = { database.workspaceDao() },
            mutationRepository = WorkspacePagedRoomMutationRepository(
                authorityRepository = authorityRepository,
                workspaceDaoProvider = { database.workspaceDao() },
            ),
        )
        val grid = WorkspaceGridPlacement.Grid(columns = 4, rows = 5)

        database.workspaceDao().upsertPages(
            listOf(WorkspacePageEntity("home:1", WorkspaceContainerType.HOME, 1))
        )
        database.workspaceDao().upsertItems(
            listOf(
                WorkspaceItemEntity(
                    itemId = "native:item:two",
                    pageId = "home:1",
                    itemType = WorkspaceItemType.APP,
                    appKey = APP_TWO,
                    rank = 0,
                    cellX = 0,
                    cellY = 0,
                ),
                WorkspaceItemEntity(
                    itemId = "native:item:three",
                    pageId = "home:1",
                    itemType = WorkspaceItemType.APP,
                    appKey = APP_THREE,
                    rank = 1,
                    cellX = 2,
                    cellY = 0,
                ),
            )
        )

        assertEquals(
            WorkspacePagedRoomMutationResult.UpdatedItem(
                itemId = "native:item:two",
                pageId = "home:1",
                cellX = 3,
                cellY = 3,
                spanX = 1,
                spanY = 1,
            ),
            mover.moveAppToCellWithinPage(
                pageId = "home:1",
                appKey = APP_TWO,
                grid = grid,
                targetCellX = 3,
                targetCellY = 3,
            ),
        )
        assertEquals(
            WorkspacePagedRoomMutationResult.InvalidWorkspace,
            mover.moveAppToCellWithinPage(
                pageId = "home:1",
                appKey = APP_TWO,
                grid = grid,
                targetCellX = 2,
                targetCellY = 0,
            ),
        )
        assertEquals(
            WorkspacePagedRoomMutationResult.InvalidWorkspace,
            mover.moveAppToCellWithinPage(
                pageId = "home:1",
                appKey = APP_TWO,
                grid = grid,
                targetCellX = 4,
                targetCellY = 0,
            ),
        )
        assertEquals(
            WorkspacePagedRoomMutationResult.PrimaryPageProtected,
            mover.moveAppToCellWithinPage(
                pageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                appKey = APP_ONE,
                grid = grid,
                targetCellX = 0,
                targetCellY = 0,
            ),
        )

        val items = database.workspaceDao().readItems(listOf("home:1")).associateBy { it.appKey }
        assertEquals(3, items.getValue(APP_TWO).cellX)
        assertEquals(3, items.getValue(APP_TWO).cellY)
        assertEquals(2, items.getValue(APP_THREE).cellX)
        assertEquals(0, items.getValue(APP_THREE).cellY)
        assertEquals(
            WorkspacePostCutoverHealthResult.Healthy,
            WorkspacePostCutoverHealthEvaluator(
                repository = authorityRepository,
                workspaceDaoProvider = { database.workspaceDao() },
            ).evaluate(),
        )
    }

    private suspend fun promoteRoomAuthority(authorityRepository: WorkspaceRepository) {
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
        const val DATABASE_NAME = "launcher-home-item-page-mover-test.db"
        const val DATASTORE_FILE = "launcher-home-item-page-mover.preferences_pb"
        const val APP_ONE = "10:com.example.one/.MainActivity"
        const val APP_TWO = "10:com.example.two/.MainActivity"
        const val APP_THREE = "10:com.example.three/.MainActivity"
    }
}
