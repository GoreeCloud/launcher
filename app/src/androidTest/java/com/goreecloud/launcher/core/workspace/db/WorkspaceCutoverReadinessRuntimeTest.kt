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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class WorkspaceCutoverReadinessRuntimeTest {
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
    fun readinessRequiresCurrentVerifiedEvidenceAndDoesNotPromote() = runBlocking {
        val repository = WorkspaceRepository(openWorkspaceDataStore())
        repository.ensureDefaults(
            favoriteKeys = EXPECTED_FAVORITES,
            dockKeys = EXPECTED_DOCK,
        )
        val coordinator = readinessCoordinator(repository)

        assertEquals(
            WorkspaceCutoverReadinessResult.NeedsVerification,
            coordinator.evaluate(),
        )

        assertEquals(
            WorkspaceStartupResult.RoomVerifiedMatch,
            WorkspaceStartupReconciler(
                repository = repository,
                workspaceDaoProvider = { database.workspaceDao() },
            ).reconcile(),
        )
        var verifiedState = repository.state.first()
        assertEquals(WorkspaceAuthority.ROOM_VERIFIED, verifiedState.authority)

        assertEquals(WorkspaceCutoverReadinessResult.Ready, coordinator.evaluate())
        verifiedState = repository.state.first()
        assertEquals(WorkspaceAuthority.ROOM_VERIFIED, verifiedState.authority)
        assertEquals(EXPECTED_FAVORITES, verifiedState.favoriteKeys)
        assertEquals(EXPECTED_DOCK, verifiedState.dockKeys)

        val divergent = WorkspaceLegacyImportMapper.map(
            favoriteKeys = listOf(DIVERGENT_FAVORITE),
            dockKeys = EXPECTED_DOCK,
        )
        database.workspaceDao().replaceLegacySnapshot(
            pages = divergent.pages,
            items = divergent.items,
        )

        assertEquals(WorkspaceCutoverReadinessResult.Mismatch, coordinator.evaluate())
        val stateAfterMismatch = repository.state.first()
        assertEquals(WorkspaceAuthority.ROOM_VERIFIED, stateAfterMismatch.authority)
        assertEquals(EXPECTED_FAVORITES, stateAfterMismatch.favoriteKeys)
        assertEquals(EXPECTED_DOCK, stateAfterMismatch.dockKeys)
    }

    @Test
    fun unavailableRoomCannotBeReadyAndTerminalRoomIsReportedWithoutMutation() = runBlocking {
        val repository = WorkspaceRepository(openWorkspaceDataStore())
        repository.ensureDefaults(
            favoriteKeys = EXPECTED_FAVORITES,
            dockKeys = EXPECTED_DOCK,
        )
        assertEquals(
            WorkspaceStartupResult.RoomVerifiedMatch,
            WorkspaceStartupReconciler(
                repository = repository,
                workspaceDaoProvider = { database.workspaceDao() },
            ).reconcile(),
        )

        val unavailable = WorkspaceCutoverReadinessCoordinator(
            repository = repository,
            workspaceDaoProvider = { null },
        )
        assertEquals(WorkspaceCutoverReadinessResult.Unavailable, unavailable.evaluate())
        var state = repository.state.first()
        assertEquals(WorkspaceAuthority.ROOM_VERIFIED, state.authority)

        assertTrue(repository.promoteRoomAuthority(state))
        state = repository.state.first()
        assertEquals(WorkspaceAuthority.ROOM, state.authority)
        assertEquals(
            WorkspaceCutoverReadinessResult.AlreadyRoomAuthoritative,
            readinessCoordinator(repository).evaluate(),
        )
        assertEquals(WorkspaceAuthority.ROOM, repository.state.first().authority)
    }

    private fun readinessCoordinator(
        repository: WorkspaceRepository,
    ): WorkspaceCutoverReadinessCoordinator = WorkspaceCutoverReadinessCoordinator(
        repository = repository,
        workspaceDaoProvider = { database.workspaceDao() },
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
        const val DATABASE_NAME = "launcher-workspace-cutover-readiness-test.db"
        const val WORKSPACE_DATASTORE_FILE = "launcher-workspace-cutover-readiness.preferences_pb"
        const val DIVERGENT_FAVORITE = "10:com.example.divergent/.MainActivity"
        val EXPECTED_FAVORITES = listOf(
            "10:com.example.alpha/.MainActivity",
            "10:com.example.beta/.MainActivity",
        )
        val EXPECTED_DOCK = listOf("10:com.example.dock/.MainActivity")
    }
}
