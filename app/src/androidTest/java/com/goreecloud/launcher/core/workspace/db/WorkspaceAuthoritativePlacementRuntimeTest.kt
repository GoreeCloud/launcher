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
import com.goreecloud.launcher.core.workspace.WorkspaceMoveDirection
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class WorkspaceAuthoritativePlacementRuntimeTest {
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
    fun preCutoverMutationsRouteToDataStoreAndVerifiedMutationInvalidatesEvidence() = runBlocking {
        val repository = WorkspaceRepository(openWorkspaceDataStore())
        repository.ensureDefaults(INITIAL_FAVORITES, INITIAL_DOCK)
        val router = router(repository) { database.workspaceDao() }

        assertEquals(
            WorkspaceAuthoritativeWriteResult.Written(
                WorkspaceAuthoritativePlacementSnapshot(
                    favoriteKeys = INITIAL_FAVORITES + ADDED_FAVORITE,
                    dockKeys = INITIAL_DOCK,
                    source = WorkspacePlacementSource.DATASTORE,
                )
            ),
            router.toggleFavorite(ADDED_FAVORITE),
        )
        assertEquals(WorkspaceAuthority.DATASTORE, repository.state.first().authority)

        assertEquals(WorkspaceStartupResult.RoomVerifiedMatch, startup(repository).reconcile())
        assertEquals(WorkspaceAuthority.ROOM_VERIFIED, repository.state.first().authority)

        assertEquals(
            WorkspacePlacementSource.DATASTORE,
            (router.moveFavorite(
                ADDED_FAVORITE,
                WorkspaceMoveDirection.EARLIER,
            ) as WorkspaceAuthoritativeWriteResult.Written).snapshot.source,
        )
        assertEquals(WorkspaceAuthority.DATASTORE, repository.state.first().authority)
    }

    @Test
    fun terminalRoomMutationsRouteToRoomAndLegacyDataStoreWritesStayFrozen() = runBlocking {
        val repository = WorkspaceRepository(openWorkspaceDataStore())
        repository.ensureDefaults(INITIAL_FAVORITES, INITIAL_DOCK)
        assertEquals(WorkspaceStartupResult.RoomVerifiedMatch, startup(repository).reconcile())
        assertEquals(
            WorkspaceProductionPromotionResult.PromotedHealthy,
            WorkspaceProductionPromotionCoordinator(
                repository = repository,
                workspaceDaoProvider = { database.workspaceDao() },
            ).promote(),
        )

        val legacyAtCutover = repository.state.first()
        val router = router(repository) { database.workspaceDao() }

        assertEquals(
            WorkspaceAuthoritativeWriteResult.Written(
                WorkspaceAuthoritativePlacementSnapshot(
                    favoriteKeys = INITIAL_FAVORITES + ROOM_ADDED_FAVORITE,
                    dockKeys = INITIAL_DOCK,
                    source = WorkspacePlacementSource.ROOM,
                )
            ),
            router.toggleFavorite(ROOM_ADDED_FAVORITE),
        )
        assertEquals(
            WorkspaceAuthoritativeReadResult.Loaded(
                WorkspaceAuthoritativePlacementSnapshot(
                    favoriteKeys = INITIAL_FAVORITES + ROOM_ADDED_FAVORITE,
                    dockKeys = INITIAL_DOCK,
                    source = WorkspacePlacementSource.ROOM,
                )
            ),
            router.read(),
        )

        repository.toggleFavorite(DIRECT_LEGACY_ATTEMPT)
        repository.toggleDock(DIRECT_LEGACY_ATTEMPT)
        val afterLegacyAttempt = repository.state.first()
        assertEquals(WorkspaceAuthority.ROOM, afterLegacyAttempt.authority)
        assertEquals(legacyAtCutover.favoriteKeys, afterLegacyAttempt.favoriteKeys)
        assertEquals(legacyAtCutover.dockKeys, afterLegacyAttempt.dockKeys)

        assertEquals(
            WorkspaceAuthoritativeWriteResult.Unavailable,
            router(repository) { null }.toggleFavorite(UNAVAILABLE_ATTEMPT),
        )
        assertEquals(WorkspaceAuthority.ROOM, repository.state.first().authority)
        assertEquals(
            WorkspaceAuthoritativeReadResult.Loaded(
                WorkspaceAuthoritativePlacementSnapshot(
                    favoriteKeys = INITIAL_FAVORITES + ROOM_ADDED_FAVORITE,
                    dockKeys = INITIAL_DOCK,
                    source = WorkspacePlacementSource.ROOM,
                )
            ),
            router.read(),
        )
    }

    private fun startup(repository: WorkspaceRepository): WorkspaceStartupReconciler =
        WorkspaceStartupReconciler(
            repository = repository,
            workspaceDaoProvider = { database.workspaceDao() },
        )

    private fun router(
        repository: WorkspaceRepository,
        daoProvider: () -> WorkspaceDao?,
    ): WorkspaceAuthoritativePlacementRepository = WorkspaceAuthoritativePlacementRepository(
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
        const val DATABASE_NAME = "launcher-authoritative-placement-test.db"
        const val WORKSPACE_DATASTORE_FILE = "launcher-authoritative-placement.preferences_pb"
        const val ADDED_FAVORITE = "10:com.example.added/.MainActivity"
        const val ROOM_ADDED_FAVORITE = "10:com.example.room-added/.MainActivity"
        const val DIRECT_LEGACY_ATTEMPT = "10:com.example.legacy-attempt/.MainActivity"
        const val UNAVAILABLE_ATTEMPT = "10:com.example.unavailable/.MainActivity"
        val INITIAL_FAVORITES = listOf(
            "10:com.example.alpha/.MainActivity",
            "10:com.example.beta/.MainActivity",
        )
        val INITIAL_DOCK = listOf("10:com.example.dock/.MainActivity")
    }
}
