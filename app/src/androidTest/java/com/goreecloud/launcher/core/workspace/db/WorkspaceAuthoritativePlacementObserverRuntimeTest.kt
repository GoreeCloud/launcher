package com.goreecloud.launcher.core.workspace.db

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
class WorkspaceAuthoritativePlacementObserverRuntimeTest {
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
    fun observationTracksDataStoreBeforeCutoverAndRoomAfterPromotion() = runBlocking {
        val repository = WorkspaceRepository(openWorkspaceDataStore())
        val observer = observer(repository) { database.workspaceDao() }
        val router = router(repository) { database.workspaceDao() }

        assertEquals(
            WorkspaceAuthoritativePlacementState.WaitingForInitialization,
            observer.observe().first(),
        )

        repository.ensureDefaults(INITIAL_FAVORITES, INITIAL_DOCK)
        assertReady(
            observer = observer,
            source = WorkspacePlacementSource.DATASTORE,
            favorites = INITIAL_FAVORITES,
            dock = INITIAL_DOCK,
        )

        router.toggleFavorite(DATASTORE_ADDED)
        assertReady(
            observer = observer,
            source = WorkspacePlacementSource.DATASTORE,
            favorites = INITIAL_FAVORITES + DATASTORE_ADDED,
            dock = INITIAL_DOCK,
        )

        assertEquals(WorkspaceStartupResult.RoomVerifiedMatch, startup(repository).reconcile())
        assertEquals(
            WorkspaceProductionPromotionResult.PromotedHealthy,
            WorkspaceProductionPromotionCoordinator(
                repository = repository,
                workspaceDaoProvider = { database.workspaceDao() },
            ).promote(),
        )

        assertReady(
            observer = observer,
            source = WorkspacePlacementSource.ROOM,
            favorites = INITIAL_FAVORITES + DATASTORE_ADDED,
            dock = INITIAL_DOCK,
        )

        router.toggleFavorite(ROOM_ADDED)
        assertReady(
            observer = observer,
            source = WorkspacePlacementSource.ROOM,
            favorites = INITIAL_FAVORITES + DATASTORE_ADDED + ROOM_ADDED,
            dock = INITIAL_DOCK,
        )
    }

    @Test
    fun terminalRoomObservationFailsClosedWhenUnavailableOrMalformed() = runBlocking {
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

        assertEquals(
            WorkspaceAuthoritativePlacementState.RecoveryRequired(
                WorkspaceAuthoritativePlacementRecoveryReason.Unavailable
            ),
            observer(repository) { null }.observe().first(),
        )

        val divergent = WorkspaceLegacyImportMapper.map(
            favoriteKeys = listOf(DIVERGENT_FAVORITE),
            dockKeys = INITIAL_DOCK,
        )
        database.workspaceDao().replaceLegacySnapshot(
            pages = divergent.pages,
            items = divergent.items,
        )
        database.workspaceDao().upsertItems(
            divergent.items.mapIndexed { index, item ->
                if (index == 0) item.copy(rank = item.rank + 10) else item
            }
        )

        assertEquals(
            WorkspaceAuthoritativePlacementState.RecoveryRequired(
                WorkspaceAuthoritativePlacementRecoveryReason.Mismatch
            ),
            withTimeout(5_000) {
                observer(repository) { database.workspaceDao() }
                    .observe()
                    .first { it is WorkspaceAuthoritativePlacementState.RecoveryRequired }
            },
        )
    }

    private suspend fun assertReady(
        observer: WorkspaceAuthoritativePlacementObserver,
        source: WorkspacePlacementSource,
        favorites: List<String>,
        dock: List<String>,
    ) {
        assertEquals(
            WorkspaceAuthoritativePlacementState.Ready(
                WorkspaceAuthoritativePlacementSnapshot(
                    favoriteKeys = favorites,
                    dockKeys = dock,
                    source = source,
                )
            ),
            withTimeout(5_000) {
                observer.observe().first { state ->
                    state is WorkspaceAuthoritativePlacementState.Ready &&
                        state.snapshot.source == source &&
                        state.snapshot.favoriteKeys == favorites &&
                        state.snapshot.dockKeys == dock
                }
            },
        )
    }

    private fun startup(repository: WorkspaceRepository): WorkspaceStartupReconciler =
        WorkspaceStartupReconciler(
            repository = repository,
            workspaceDaoProvider = { database.workspaceDao() },
        )

    private fun observer(
        repository: WorkspaceRepository,
        daoProvider: () -> WorkspaceDao?,
    ): WorkspaceAuthoritativePlacementObserver = WorkspaceAuthoritativePlacementObserver(
        authorityRepository = repository,
        workspaceDaoProvider = daoProvider,
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
        const val DATABASE_NAME = "launcher-authoritative-observer-test.db"
        const val WORKSPACE_DATASTORE_FILE = "launcher-authoritative-observer.preferences_pb"
        const val DATASTORE_ADDED = "10:com.example.datastore-added/.MainActivity"
        const val ROOM_ADDED = "10:com.example.room-added/.MainActivity"
        const val DIVERGENT_FAVORITE = "10:com.example.divergent/.MainActivity"
        val INITIAL_FAVORITES = listOf(
            "10:com.example.alpha/.MainActivity",
            "10:com.example.beta/.MainActivity",
        )
        val INITIAL_DOCK = listOf("10:com.example.dock/.MainActivity")
    }
}
