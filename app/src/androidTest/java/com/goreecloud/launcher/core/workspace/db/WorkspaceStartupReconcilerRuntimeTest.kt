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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class WorkspaceStartupReconcilerRuntimeTest {
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
    fun repeatedReopenKeepsVerifiedStateAndRoomFailureRecoversWithoutWorkspaceLoss() = runBlocking {
        var repository = WorkspaceRepository(openWorkspaceDataStore())
        repository.ensureDefaults(
            favoriteKeys = EXPECTED_FAVORITES,
            dockKeys = EXPECTED_DOCK,
        )

        var reconciler = reconciler(repository)
        assertEquals(WorkspaceStartupResult.RoomVerifiedMatch, reconciler.reconcile())
        assertVerifiedState(repository)

        repeat(2) {
            closeWorkspaceDataStore()
            database.close()
            database = openDatabase()
            repository = WorkspaceRepository(openWorkspaceDataStore())
            reconciler = reconciler(repository)

            assertEquals(WorkspaceStartupResult.RoomVerifiedMatch, reconciler.reconcile())
            assertVerifiedState(repository)
        }

        database.close()
        assertEquals(WorkspaceStartupResult.FellBackToDataStore, reconciler.reconcile())
        var state = repository.state.first()
        assertEquals(WorkspaceAuthority.DATASTORE, state.authority)
        assertEquals(EXPECTED_FAVORITES, state.favoriteKeys)
        assertEquals(EXPECTED_DOCK, state.dockKeys)

        database = openDatabase()
        reconciler = reconciler(repository)
        assertEquals(WorkspaceStartupResult.RoomVerifiedMatch, reconciler.reconcile())
        assertVerifiedState(repository)

        closeWorkspaceDataStore()
        repository = WorkspaceRepository(openWorkspaceDataStore())
        assertVerifiedState(repository)
    }

    @Test
    fun unavailableRoomFallsBackOnlyFromVerifiedCompatibilityState() = runBlocking {
        val repository = WorkspaceRepository(openWorkspaceDataStore())
        repository.ensureDefaults(
            favoriteKeys = EXPECTED_FAVORITES,
            dockKeys = EXPECTED_DOCK,
        )

        val unavailableWhileDataStore = WorkspaceStartupReconciler(
            repository = repository,
            workspaceDaoProvider = { null },
        )
        assertEquals(WorkspaceStartupResult.DataStoreOnly, unavailableWhileDataStore.reconcile())
        assertEquals(WorkspaceAuthority.DATASTORE, repository.state.first().authority)

        assertEquals(WorkspaceStartupResult.RoomVerifiedMatch, reconciler(repository).reconcile())
        assertVerifiedState(repository)

        val unavailableWhileVerified = WorkspaceStartupReconciler(
            repository = repository,
            workspaceDaoProvider = { null },
        )
        assertEquals(
            WorkspaceStartupResult.FellBackToDataStore,
            unavailableWhileVerified.reconcile(),
        )
        val state = repository.state.first()
        assertEquals(WorkspaceAuthority.DATASTORE, state.authority)
        assertEquals(EXPECTED_FAVORITES, state.favoriteKeys)
        assertEquals(EXPECTED_DOCK, state.dockKeys)
    }

    private fun reconciler(repository: WorkspaceRepository): WorkspaceStartupReconciler =
        WorkspaceStartupReconciler(
            repository = repository,
            workspaceDaoProvider = { database.workspaceDao() },
        )

    private suspend fun assertVerifiedState(repository: WorkspaceRepository) {
        val state = repository.state.first()
        assertEquals(WorkspaceAuthority.ROOM_VERIFIED, state.authority)
        assertEquals(EXPECTED_FAVORITES, state.favoriteKeys)
        assertEquals(EXPECTED_DOCK, state.dockKeys)
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
        const val DATABASE_NAME = "launcher-workspace-startup-recovery-test.db"
        const val WORKSPACE_DATASTORE_FILE = "launcher-workspace-startup-recovery.preferences_pb"
        val EXPECTED_FAVORITES = listOf(
            "10:com.example.alpha/.MainActivity",
            "10:com.example.beta/.MainActivity",
        )
        val EXPECTED_DOCK = listOf("10:com.example.dock/.MainActivity")
    }
}
