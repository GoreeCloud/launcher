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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class WorkspacePromotionRehearsalRuntimeTest {
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
    fun candidateRequiresFreshReadyEvidenceAndStaleCandidateCannotPromote() = runBlocking {
        val repository = WorkspaceRepository(openWorkspaceDataStore())
        repository.ensureDefaults(
            favoriteKeys = INITIAL_FAVORITES,
            dockKeys = INITIAL_DOCK,
        )
        val rehearsal = rehearsal(repository)

        assertEquals(
            WorkspacePromotionRehearsalResult.NeedsVerification,
            rehearsal.evaluate(),
        )
        assertEquals(WorkspaceStartupResult.RoomVerifiedMatch, startup(repository).reconcile())

        val firstCandidate = requireCandidate(rehearsal.evaluate())
        assertEquals(WorkspaceAuthority.ROOM_VERIFIED, repository.state.first().authority)
        assertEquals(INITIAL_FAVORITES, firstCandidate.expectedState.favoriteKeys)
        assertEquals(INITIAL_DOCK, firstCandidate.expectedState.dockKeys)

        repository.toggleFavorite(ADDED_FAVORITE)
        assertFalse(repository.promoteRoomAuthority(firstCandidate.expectedState))
        var current = repository.state.first()
        assertEquals(WorkspaceAuthority.DATASTORE, current.authority)
        assertEquals(INITIAL_FAVORITES + ADDED_FAVORITE, current.favoriteKeys)
        assertEquals(INITIAL_DOCK, current.dockKeys)

        assertEquals(WorkspaceStartupResult.RoomVerifiedMatch, startup(repository).reconcile())
        val freshCandidate = requireCandidate(rehearsal.evaluate())
        assertTrue(repository.promoteRoomAuthority(freshCandidate.expectedState))

        current = repository.state.first()
        assertEquals(WorkspaceAuthority.ROOM, current.authority)
        assertEquals(
            WorkspacePostCutoverHealthResult.Healthy,
            health(repository) { database.workspaceDao() }.evaluate(),
        )
    }

    @Test
    fun mismatchAndUnavailableBlockCandidateAndTerminalRoomRequiresExplicitRecovery() = runBlocking {
        val repository = WorkspaceRepository(openWorkspaceDataStore())
        repository.ensureDefaults(
            favoriteKeys = INITIAL_FAVORITES,
            dockKeys = INITIAL_DOCK,
        )
        assertEquals(WorkspaceStartupResult.RoomVerifiedMatch, startup(repository).reconcile())

        val divergent = WorkspaceLegacyImportMapper.map(
            favoriteKeys = listOf(DIVERGENT_FAVORITE),
            dockKeys = INITIAL_DOCK,
        )
        database.workspaceDao().replaceLegacySnapshot(
            pages = divergent.pages,
            items = divergent.items,
        )

        assertEquals(WorkspacePromotionRehearsalResult.Mismatch, rehearsal(repository).evaluate())
        var current = repository.state.first()
        assertEquals(WorkspaceAuthority.ROOM_VERIFIED, current.authority)
        assertEquals(INITIAL_FAVORITES, current.favoriteKeys)
        assertEquals(INITIAL_DOCK, current.dockKeys)

        val unavailableRehearsal = WorkspacePromotionRehearsalCoordinator(
            repository = repository,
            workspaceDaoProvider = { null },
        )
        assertEquals(WorkspacePromotionRehearsalResult.Unavailable, unavailableRehearsal.evaluate())
        assertEquals(WorkspaceAuthority.ROOM_VERIFIED, repository.state.first().authority)

        assertEquals(WorkspaceStartupResult.FellBackToDataStore, startup(repository).reconcile())
        assertEquals(WorkspaceStartupResult.RoomVerifiedMatch, startup(repository).reconcile())
        val candidate = requireCandidate(rehearsal(repository).evaluate())
        assertTrue(repository.promoteRoomAuthority(candidate.expectedState))
        assertEquals(WorkspaceAuthority.ROOM, repository.state.first().authority)
        assertEquals(
            WorkspacePostCutoverHealthResult.Healthy,
            health(repository) { database.workspaceDao() }.evaluate(),
        )

        database.close()
        assertEquals(
            WorkspacePostCutoverHealthResult.Unavailable,
            health(repository) { null }.evaluate(),
        )
        assertEquals(WorkspaceAuthority.ROOM, repository.state.first().authority)

        database = openDatabase()
        assertEquals(
            WorkspacePostCutoverHealthResult.Healthy,
            health(repository) { database.workspaceDao() }.evaluate(),
        )
        assertEquals(WorkspaceAuthority.ROOM, repository.state.first().authority)
    }

    private fun startup(repository: WorkspaceRepository): WorkspaceStartupReconciler =
        WorkspaceStartupReconciler(
            repository = repository,
            workspaceDaoProvider = { database.workspaceDao() },
        )

    private fun rehearsal(repository: WorkspaceRepository): WorkspacePromotionRehearsalCoordinator =
        WorkspacePromotionRehearsalCoordinator(
            repository = repository,
            workspaceDaoProvider = { database.workspaceDao() },
        )

    private fun health(
        repository: WorkspaceRepository,
        daoProvider: () -> WorkspaceDao?,
    ): WorkspacePostCutoverHealthEvaluator = WorkspacePostCutoverHealthEvaluator(
        repository = repository,
        workspaceDaoProvider = daoProvider,
    )

    private fun requireCandidate(
        result: WorkspacePromotionRehearsalResult,
    ): WorkspacePromotionCandidate {
        assertTrue(result is WorkspacePromotionRehearsalResult.Candidate)
        return (result as WorkspacePromotionRehearsalResult.Candidate).candidate
    }

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
        const val DATABASE_NAME = "launcher-workspace-promotion-rehearsal-test.db"
        const val WORKSPACE_DATASTORE_FILE = "launcher-workspace-promotion-rehearsal.preferences_pb"
        const val ADDED_FAVORITE = "10:com.example.added/.MainActivity"
        const val DIVERGENT_FAVORITE = "10:com.example.divergent/.MainActivity"
        val INITIAL_FAVORITES = listOf(
            "10:com.example.alpha/.MainActivity",
            "10:com.example.beta/.MainActivity",
        )
        val INITIAL_DOCK = listOf("10:com.example.dock/.MainActivity")
    }
}
