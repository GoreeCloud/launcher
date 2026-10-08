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
class WorkspacePostCutoverStartupRuntimeTest {
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
    fun terminalRoomSurvivesPersistenceClientReopenAndStartupReturnsReady() = runBlocking {
        var repository = WorkspaceRepository(openWorkspaceDataStore())
        promote(repository)
        assertEquals(WorkspaceAuthority.ROOM, repository.state.first().authority)

        closeWorkspaceDataStore()
        database.close()

        database = openDatabase()
        repository = WorkspaceRepository(openWorkspaceDataStore())

        assertEquals(WorkspaceAuthority.ROOM, repository.state.first().authority)
        assertEquals(INITIAL_FAVORITES, repository.state.first().favoriteKeys)
        assertEquals(INITIAL_DOCK, repository.state.first().dockKeys)
        assertEquals(
            WorkspacePostCutoverStartupResult.Ready,
            startup(repository) { database.workspaceDao() }.reconcile(),
        )
    }

    @Test
    fun unavailableRoomRequiresExplicitRecoveryAndHealthyReopenRestoresReadiness() = runBlocking {
        val repository = WorkspaceRepository(openWorkspaceDataStore())
        promote(repository)

        database.close()
        assertEquals(
            WorkspacePostCutoverStartupResult.RecoveryRequired(
                WorkspacePostCutoverHealthResult.Unavailable
            ),
            startup(repository) { null }.reconcile(),
        )
        assertEquals(WorkspaceAuthority.ROOM, repository.state.first().authority)
        assertEquals(INITIAL_FAVORITES, repository.state.first().favoriteKeys)
        assertEquals(INITIAL_DOCK, repository.state.first().dockKeys)

        database = openDatabase()
        assertEquals(
            WorkspacePostCutoverStartupResult.Ready,
            startup(repository) { database.workspaceDao() }.reconcile(),
        )
        assertEquals(WorkspaceAuthority.ROOM, repository.state.first().authority)
    }

    private suspend fun promote(repository: WorkspaceRepository) {
        repository.ensureDefaults(
            favoriteKeys = INITIAL_FAVORITES,
            dockKeys = INITIAL_DOCK,
        )
        assertEquals(
            WorkspaceStartupResult.RoomVerifiedMatch,
            WorkspaceStartupReconciler(
                repository = repository,
                workspaceDaoProvider = { database.workspaceDao() },
            ).reconcile(),
        )
        assertEquals(
            WorkspaceProductionPromotionResult.PromotedHealthy,
            WorkspaceProductionPromotionCoordinator(
                repository = repository,
                workspaceDaoProvider = { database.workspaceDao() },
            ).promote(),
        )
    }

    private fun startup(
        repository: WorkspaceRepository,
        daoProvider: () -> WorkspaceDao?,
    ): WorkspacePostCutoverStartupCoordinator = WorkspacePostCutoverStartupCoordinator(
        repository = repository,
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
        const val DATABASE_NAME = "launcher-post-cutover-startup-test.db"
        const val WORKSPACE_DATASTORE_FILE = "launcher-post-cutover-startup.preferences_pb"
        val INITIAL_FAVORITES = listOf(
            "10:com.example.alpha/.MainActivity",
            "10:com.example.beta/.MainActivity",
        )
        val INITIAL_DOCK = listOf("10:com.example.dock/.MainActivity")
    }
}
