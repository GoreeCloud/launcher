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
import com.goreecloud.launcher.core.workspace.WorkspaceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class WorkspaceProductionRuntimeCoordinatorRuntimeTest {
    private lateinit var context: Context
    private lateinit var database: LauncherDatabase
    private lateinit var workspaceDataStoreFile: File
    private var workspaceDataStoreScope: CoroutineScope? = null

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(DATABASE_NAME)
        database = openDatabase()
        workspaceDataStoreFile = File(context.cacheDir, WORKSPACE_DATASTORE_FILE)
        workspaceDataStoreFile.delete()
    }

    @After
    fun tearDown() {
        runBlocking { closeWorkspaceDataStore() }
        database.close()
        context.deleteDatabase(DATABASE_NAME)
        workspaceDataStoreFile.delete()
    }

    @Test
    fun productionActivationRoutesRoomAndSurvivesPersistenceClientRecreation() = runBlocking {
        var repository = WorkspaceRepository(openWorkspaceDataStore())
        repository.ensureDefaults(INITIAL_FAVORITES, INITIAL_DOCK)
        var runtime = runtime(repository) { database.workspaceDao() }

        assertEquals(
            WorkspaceProductionRuntimeResult.RoomReady,
            runtime.reconcileAndActivate(),
        )
        assertEquals(WorkspaceAuthority.ROOM, repository.state.first().authority)
        assertReady(runtime, INITIAL_FAVORITES, INITIAL_DOCK)

        assertEquals(
            WorkspaceAuthoritativeWriteResult.Written(
                WorkspaceAuthoritativePlacementSnapshot(
                    favoriteKeys = INITIAL_FAVORITES + ROOM_ADDED,
                    dockKeys = INITIAL_DOCK,
                    source = WorkspacePlacementSource.ROOM,
                )
            ),
            runtime.toggleFavorite(ROOM_ADDED),
        )
        assertReady(runtime, INITIAL_FAVORITES + ROOM_ADDED, INITIAL_DOCK)

        closeWorkspaceDataStore()
        database.close()
        database = openDatabase()
        repository = WorkspaceRepository(openWorkspaceDataStore())
        runtime = runtime(repository) { database.workspaceDao() }

        assertEquals(WorkspaceAuthority.ROOM, repository.state.first().authority)
        assertEquals(
            WorkspaceProductionRuntimeResult.RoomReady,
            runtime.reconcileAndActivate(),
        )
        assertReady(runtime, INITIAL_FAVORITES + ROOM_ADDED, INITIAL_DOCK)

        assertEquals(
            WorkspaceAuthoritativeWriteResult.Written(
                WorkspaceAuthoritativePlacementSnapshot(
                    favoriteKeys = INITIAL_FAVORITES + ROOM_ADDED,
                    dockKeys = INITIAL_DOCK + DOCK_ADDED,
                    source = WorkspacePlacementSource.ROOM,
                )
            ),
            runtime.toggleDock(DOCK_ADDED),
        )
        assertReady(runtime, INITIAL_FAVORITES + ROOM_ADDED, INITIAL_DOCK + DOCK_ADDED)
    }

    @Test
    fun terminalRoomRuntimeProtectsPrimaryPageAndRoutesSecondaryPageOrderMutation() = runBlocking {
        val repository = WorkspaceRepository(openWorkspaceDataStore())
        repository.ensureDefaults(INITIAL_FAVORITES, INITIAL_DOCK)
        val runtime = runtime(repository) { database.workspaceDao() }

        assertEquals(WorkspaceProductionRuntimeResult.RoomReady, runtime.reconcileAndActivate())
        database.workspaceDao().upsertPages(
            listOf(
                WorkspacePageEntity("home:1", WorkspaceContainerType.HOME, 1),
                WorkspacePageEntity("home:2", WorkspaceContainerType.HOME, 2),
            )
        )

        assertEquals(
            WorkspacePagedRoomMutationResult.PrimaryPageProtected,
            runtime.moveHomePage("home:2", 0),
        )
        assertEquals(
            listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID, "home:1", "home:2"),
            database.workspaceDao().readPagesByContainer(WorkspaceContainerType.HOME).map { it.pageId },
        )

        assertEquals(
            WorkspacePagedRoomMutationResult.Updated(
                listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID, "home:2", "home:1")
            ),
            runtime.moveHomePage("home:2", 1),
        )
        assertEquals(
            listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID, "home:2", "home:1"),
            database.workspaceDao().readPagesByContainer(WorkspaceContainerType.HOME).map { it.pageId },
        )
    }

    @Test
    fun terminalRoomRuntimeRoutesGuardedPrimaryBoundaryTransfer() = runBlocking {
        val repository = WorkspaceRepository(openWorkspaceDataStore())
        repository.ensureDefaults(INITIAL_FAVORITES, INITIAL_DOCK)
        val runtime = runtime(repository) { database.workspaceDao() }

        assertEquals(WorkspaceProductionRuntimeResult.RoomReady, runtime.reconcileAndActivate())
        database.workspaceDao().upsertPages(
            listOf(WorkspacePageEntity("home:1", WorkspaceContainerType.HOME, 1))
        )

        assertEquals(
            WorkspacePagedRoomMutationResult.PrimaryPageProtected,
            runtime.moveHomeAppToPage(
                sourcePageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                appKey = INITIAL_FAVORITES.first(),
                targetPageId = "home:1",
            ),
        )
        assertEquals(
            WorkspacePagedRoomMutationResult.UpdatedItem(
                itemId = "legacy:home:${INITIAL_FAVORITES.first()}",
                pageId = "home:1",
                cellX = 0,
                cellY = 0,
                spanX = 1,
                spanY = 1,
            ),
            runtime.moveHomeAppToPage(
                sourcePageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                appKey = INITIAL_FAVORITES.first(),
                targetPageId = "home:1",
                primaryColumns = 4,
                primaryRows = 5,
            ),
        )
        assertReady(runtime, listOf(INITIAL_FAVORITES[1]), INITIAL_DOCK)

        assertEquals(
            WorkspacePagedRoomMutationResult.UpdatedItem(
                itemId = "legacy:home:${INITIAL_FAVORITES.first()}",
                pageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                cellX = 0,
                cellY = 0,
                spanX = 1,
                spanY = 1,
            ),
            runtime.moveHomeAppToPage(
                sourcePageId = "home:1",
                appKey = INITIAL_FAVORITES.first(),
                targetPageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                primaryColumns = 4,
                primaryRows = 5,
            ),
        )
        assertReady(
            runtime,
            listOf(INITIAL_FAVORITES[1], INITIAL_FAVORITES[0]),
            INITIAL_DOCK,
        )
    }

    @Test
    fun terminalRoomUnavailabilityRequiresRecoveryAndNeverFallsBackToLegacyWrites() = runBlocking {
        val repository = WorkspaceRepository(openWorkspaceDataStore())
        repository.ensureDefaults(INITIAL_FAVORITES, INITIAL_DOCK)
        val healthyRuntime = runtime(repository) { database.workspaceDao() }
        assertEquals(
            WorkspaceProductionRuntimeResult.RoomReady,
            healthyRuntime.reconcileAndActivate(),
        )
        assertEquals(WorkspaceAuthority.ROOM, repository.state.first().authority)

        val legacyAtCutover = repository.state.first()
        val unavailableRuntime = runtime(repository) { null }
        assertEquals(
            WorkspaceProductionRuntimeResult.RecoveryRequired(
                WorkspacePostCutoverHealthResult.Unavailable
            ),
            unavailableRuntime.reconcileAndActivate(),
        )
        assertEquals(
            WorkspaceAuthoritativePlacementState.RecoveryRequired(
                WorkspaceAuthoritativePlacementRecoveryReason.Unavailable
            ),
            unavailableRuntime.observePlacement().first(),
        )
        assertEquals(
            WorkspaceAuthoritativeWriteResult.Unavailable,
            unavailableRuntime.toggleFavorite(UNAVAILABLE_ATTEMPT),
        )

        repository.toggleFavorite(DIRECT_LEGACY_ATTEMPT)
        val afterLegacyAttempt = repository.state.first()
        assertEquals(WorkspaceAuthority.ROOM, afterLegacyAttempt.authority)
        assertEquals(legacyAtCutover.favoriteKeys, afterLegacyAttempt.favoriteKeys)
        assertEquals(legacyAtCutover.dockKeys, afterLegacyAttempt.dockKeys)

        assertEquals(
            WorkspaceProductionRuntimeResult.RoomReady,
            healthyRuntime.reconcileAndActivate(),
        )
        assertReady(healthyRuntime, INITIAL_FAVORITES, INITIAL_DOCK)
    }

    private suspend fun assertReady(
        runtime: WorkspaceProductionRuntimeCoordinator,
        favorites: List<String>,
        dock: List<String>,
    ) {
        assertEquals(
            WorkspaceAuthoritativePlacementState.Ready(
                WorkspaceAuthoritativePlacementSnapshot(
                    favoriteKeys = favorites,
                    dockKeys = dock,
                    source = WorkspacePlacementSource.ROOM,
                )
            ),
            withTimeout(5_000) {
                runtime.observePlacement().first { state ->
                    state is WorkspaceAuthoritativePlacementState.Ready &&
                        state.snapshot.source == WorkspacePlacementSource.ROOM &&
                        state.snapshot.favoriteKeys == favorites &&
                        state.snapshot.dockKeys == dock
                }
            },
        )
    }

    private fun runtime(
        repository: WorkspaceRepository,
        daoProvider: () -> WorkspaceDao?,
    ): WorkspaceProductionRuntimeCoordinator = WorkspaceProductionRuntimeCoordinator(
        authorityRepository = repository,
        workspaceDaoProvider = daoProvider,
    )

    private fun openDatabase(): LauncherDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            LauncherDatabase::class.java,
            DATABASE_NAME,
        )
            .setDriver(AndroidSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

    private fun openWorkspaceDataStore(): DataStore<Preferences> {
        check(workspaceDataStoreScope == null)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        workspaceDataStoreScope = scope
        return PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { workspaceDataStoreFile },
        )
    }

    private suspend fun closeWorkspaceDataStore() {
        val scope = workspaceDataStoreScope ?: return
        workspaceDataStoreScope = null
        scope.coroutineContext[Job]?.cancelAndJoin()
    }

    private companion object {
        const val DATABASE_NAME = "launcher-production-runtime-test.db"
        const val WORKSPACE_DATASTORE_FILE = "launcher-production-runtime.preferences_pb"
        const val ROOM_ADDED = "10:com.example.room-added/.MainActivity"
        const val DOCK_ADDED = "10:com.example.dock-added/.MainActivity"
        const val UNAVAILABLE_ATTEMPT = "10:com.example.unavailable/.MainActivity"
        const val DIRECT_LEGACY_ATTEMPT = "10:com.example.legacy-attempt/.MainActivity"
        val INITIAL_FAVORITES = listOf(
            "10:com.example.alpha/.MainActivity",
            "10:com.example.beta/.MainActivity",
        )
        val INITIAL_DOCK = listOf("10:com.example.dock/.MainActivity")
    }
}
