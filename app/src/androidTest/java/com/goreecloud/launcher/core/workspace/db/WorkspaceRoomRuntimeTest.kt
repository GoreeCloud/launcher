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
import com.goreecloud.launcher.core.workspace.WorkspaceState
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class WorkspaceRoomRuntimeTest {
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
        runBlocking {
            closeWorkspaceDataStore()
        }
        database.close()
        context.deleteDatabase(DATABASE_NAME)
        workspaceDataStoreFile.delete()
    }

    @Test
    fun mirrorPersistsReopensReplacesAndClearsCompatibilitySnapshot() = runBlocking {
        val firstState = WorkspaceState(
            initialized = true,
            favoriteKeys = listOf(FAVORITE_ALPHA, FAVORITE_BETA, FAVORITE_GAMMA),
            dockKeys = listOf(DOCK_ALPHA, DOCK_BETA),
        )

        assertEquals(
            WorkspaceMirrorResult.Verified,
            WorkspaceRelationalMirror(database.workspaceDao()).sync(firstState),
        )
        assertSnapshot(
            expectedFavorites = firstState.favoriteKeys,
            expectedDock = firstState.dockKeys,
        )

        database.close()
        database = openDatabase()
        assertSnapshot(
            expectedFavorites = firstState.favoriteKeys,
            expectedDock = firstState.dockKeys,
        )

        val secondState = WorkspaceState(
            initialized = true,
            favoriteKeys = listOf(FAVORITE_GAMMA, FAVORITE_ALPHA),
            dockKeys = listOf(DOCK_BETA),
        )

        assertEquals(
            WorkspaceMirrorResult.Verified,
            WorkspaceRelationalMirror(database.workspaceDao()).sync(secondState),
        )
        assertSnapshot(
            expectedFavorites = secondState.favoriteKeys,
            expectedDock = secondState.dockKeys,
        )

        val replacedKeys = database.workspaceDao()
            .readItems(COMPATIBILITY_PAGE_IDS)
            .mapNotNull { it.appKey }
            .toSet()
        assertFalse(replacedKeys.contains(FAVORITE_BETA))
        assertFalse(replacedKeys.contains(DOCK_ALPHA))

        val emptyState = WorkspaceState(initialized = true)
        assertEquals(
            WorkspaceMirrorResult.Verified,
            WorkspaceRelationalMirror(database.workspaceDao()).sync(emptyState),
        )
        assertSnapshot(expectedFavorites = emptyList(), expectedDock = emptyList())
    }

    @Test
    fun authorityStatePersistsInvalidatesAndPromotesOneWay() = runBlocking {
        var repository = WorkspaceRepository(openWorkspaceDataStore())
        repository.ensureDefaults(
            favoriteKeys = listOf(FAVORITE_ALPHA, FAVORITE_BETA),
            dockKeys = listOf(DOCK_ALPHA),
        )
        var state = repository.state.first { it.initialized }
        assertEquals(WorkspaceAuthority.DATASTORE, state.authority)
        assertNull(state.verifiedRoomFingerprint)

        assertEquals(
            WorkspaceMirrorResult.Verified,
            WorkspaceRelationalMirror(database.workspaceDao()).sync(state),
        )
        assertTrue(repository.markRoomVerified(state))
        val verifiedBeforeRestart = repository.state.first {
            it.authority == WorkspaceAuthority.ROOM_VERIFIED
        }
        assertNotNull(verifiedBeforeRestart.verifiedRoomFingerprint)

        closeWorkspaceDataStore()
        repository = WorkspaceRepository(openWorkspaceDataStore())
        state = repository.state.first()
        assertEquals(WorkspaceAuthority.ROOM_VERIFIED, state.authority)
        assertEquals(
            verifiedBeforeRestart.verifiedRoomFingerprint,
            state.verifiedRoomFingerprint,
        )

        repository.toggleFavorite(FAVORITE_GAMMA)
        state = repository.state.first {
            it.authority == WorkspaceAuthority.DATASTORE &&
                FAVORITE_GAMMA in it.favoriteKeys
        }
        assertNull(state.verifiedRoomFingerprint)
        assertFalse(repository.promoteRoomAuthority(verifiedBeforeRestart))

        assertEquals(
            WorkspaceMirrorResult.Verified,
            WorkspaceRelationalMirror(database.workspaceDao()).sync(state),
        )
        assertTrue(repository.markRoomVerified(state))
        val verifiedCurrent = repository.state.first {
            it.authority == WorkspaceAuthority.ROOM_VERIFIED
        }
        assertTrue(repository.promoteRoomAuthority(verifiedCurrent))
        state = repository.state.first { it.authority == WorkspaceAuthority.ROOM }

        repository.markDataStoreAuthoritative()
        val afterFallbackAttempt = repository.state.first()
        assertEquals(WorkspaceAuthority.ROOM, afterFallbackAttempt.authority)
        assertEquals(state.verifiedRoomFingerprint, afterFallbackAttempt.verifiedRoomFingerprint)
    }

    @Test
    fun dualReadMatchesDetectsDivergenceAndFallsBackWhenRoomReadFails() = runBlocking {
        val repository = WorkspaceRepository(openWorkspaceDataStore())
        repository.ensureDefaults(
            favoriteKeys = listOf(FAVORITE_ALPHA, FAVORITE_BETA),
            dockKeys = listOf(DOCK_ALPHA),
        )
        var state = repository.state.first { it.initialized }
        val mirror = WorkspaceRelationalMirror(database.workspaceDao())

        assertEquals(WorkspaceMirrorResult.Verified, mirror.sync(state))
        assertTrue(repository.markRoomVerified(state))
        state = repository.state.first { it.authority == WorkspaceAuthority.ROOM_VERIFIED }

        val reader = WorkspaceRelationalReader(database.workspaceDao())
        assertEquals(WorkspaceDualReadResult.Match, reader.reconcile(state))

        val divergent = WorkspaceLegacyImportMapper.map(
            favoriteKeys = listOf(FAVORITE_BETA),
            dockKeys = listOf(DOCK_ALPHA),
        )
        database.workspaceDao().replaceLegacySnapshot(divergent.pages, divergent.items)
        assertEquals(WorkspaceDualReadResult.Mismatch, reader.reconcile(state))

        repository.markDataStoreAuthoritative()
        state = repository.state.first { it.authority == WorkspaceAuthority.DATASTORE }
        assertEquals(listOf(FAVORITE_ALPHA, FAVORITE_BETA), state.favoriteKeys)
        assertEquals(listOf(DOCK_ALPHA), state.dockKeys)

        assertEquals(WorkspaceMirrorResult.Verified, mirror.sync(state))
        assertTrue(repository.markRoomVerified(state))
        state = repository.state.first { it.authority == WorkspaceAuthority.ROOM_VERIFIED }

        val closedDatabaseReader = WorkspaceRelationalReader(database.workspaceDao())
        database.close()
        val failed = closedDatabaseReader.reconcile(state)
        assertTrue(failed is WorkspaceDualReadResult.Failed)
        failed as WorkspaceDualReadResult.Failed
        assertFalse(failed.failureType.contains(FAVORITE_ALPHA))
        assertFalse(failed.failureType.contains(DOCK_ALPHA))

        repository.markDataStoreAuthoritative()
        assertEquals(
            WorkspaceAuthority.DATASTORE,
            repository.state.first().authority,
        )

        database = openDatabase()
    }

    private suspend fun assertSnapshot(
        expectedFavorites: List<String>,
        expectedDock: List<String>,
    ) {
        val dao = database.workspaceDao()
        val pages = dao.readPages(COMPATIBILITY_PAGE_IDS)
        assertEquals(COMPATIBILITY_PAGE_IDS.toSet(), pages.map { it.pageId }.toSet())

        val items = dao.readItems(COMPATIBILITY_PAGE_IDS)
        val actualFavorites = items
            .filter { it.pageId == HOME_PAGE_ID }
            .sortedBy { it.rank }
            .mapNotNull { it.appKey }
        val actualDock = items
            .filter { it.pageId == DOCK_PAGE_ID }
            .sortedBy { it.rank }
            .mapNotNull { it.appKey }

        assertEquals(expectedFavorites, actualFavorites)
        assertEquals(expectedDock, actualDock)
        assertEquals(expectedFavorites.size + expectedDock.size, dao.itemCount())
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
        const val DATABASE_NAME = "launcher-workspace-runtime-test.db"
        const val WORKSPACE_DATASTORE_FILE = "launcher-workspace-authority-runtime.preferences_pb"
        const val HOME_PAGE_ID = "home:0"
        const val DOCK_PAGE_ID = "dock:0"
        val COMPATIBILITY_PAGE_IDS = listOf(HOME_PAGE_ID, DOCK_PAGE_ID)

        const val FAVORITE_ALPHA = "10:com.example.alpha/.MainActivity"
        const val FAVORITE_BETA = "10:com.example.beta/.MainActivity"
        const val FAVORITE_GAMMA = "10:com.example.gamma/.MainActivity"
        const val DOCK_ALPHA = "10:com.example.dockalpha/.MainActivity"
        const val DOCK_BETA = "10:com.example.dockbeta/.MainActivity"
    }
}
