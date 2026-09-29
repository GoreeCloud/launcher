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
class WorkspacePagedHomeObserverRuntimeTest {
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
    fun observerWaitsForRoomThenReactivelyProjectsHomePages() = runBlocking {
        val authorityRepository = WorkspaceRepository(openDataStore())
        authorityRepository.ensureDefaults(
            favoriteKeys = listOf(APP_ONE),
            dockKeys = emptyList(),
        )
        val observer = WorkspacePagedHomeObserver(
            authorityRepository = authorityRepository,
            workspaceDaoProvider = { database.workspaceDao() },
        )

        assertEquals(WorkspacePagedHomeState.WaitingForRoom, observer.observe().first())

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

        val initial = observer.observe().first { it is WorkspacePagedHomeState.Ready } as WorkspacePagedHomeState.Ready
        assertEquals(listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID), initial.pages.map { it.pageId })
        assertEquals(listOf(APP_ONE), initial.pages.single().appKeys)

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
                )
            )
        )

        val expanded = observer.observe().first {
            it is WorkspacePagedHomeState.Ready && it.pages.size == 2
        } as WorkspacePagedHomeState.Ready
        assertEquals(
            listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID, "home:1"),
            expanded.pages.map { it.pageId },
        )
        assertEquals(listOf(APP_TWO), expanded.pages[1].appKeys)
    }

    private fun openDataStore(): DataStore<Preferences> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStoreScope = scope
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { dataStoreFile })
    }

    private companion object {
        const val DATABASE_NAME = "launcher-paged-home-observer-test.db"
        const val DATASTORE_FILE = "launcher-paged-home-observer.preferences_pb"
        const val APP_ONE = "10:com.example.one/.MainActivity"
        const val APP_TWO = "10:com.example.two/.MainActivity"
    }
}
