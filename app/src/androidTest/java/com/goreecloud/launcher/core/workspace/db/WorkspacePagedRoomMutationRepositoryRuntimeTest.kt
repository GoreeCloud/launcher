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
class WorkspacePagedRoomMutationRepositoryRuntimeTest {
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
    fun pageLifecycleAndOrderMutationRequireRoomAuthorityAndProtectPrimaryRank() = runBlocking {
        val authorityRepository = WorkspaceRepository(openDataStore())
        authorityRepository.ensureDefaults(
            favoriteKeys = listOf(APP_ONE),
            dockKeys = emptyList(),
        )
        val repository = WorkspacePagedRoomMutationRepository(
            authorityRepository = authorityRepository,
            workspaceDaoProvider = { database.workspaceDao() },
        )

        assertEquals(
            WorkspacePagedRoomMutationResult.Reserved,
            repository.createHomePage("home:user:reserved"),
        )
        assertEquals(
            WorkspacePagedRoomMutationResult.Reserved,
            repository.deleteEmptyHomePage("home:user:reserved"),
        )
        assertEquals(
            WorkspacePagedRoomMutationResult.Reserved,
            repository.moveHomePage(WorkspaceLegacyImportMapper.HOME_PAGE_ID, 0),
        )

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
                    pageId = "home:2",
                    itemType = WorkspaceItemType.APP,
                    appKey = APP_TWO,
                    rank = 0,
                    cellX = 0,
                    cellY = 0,
                )
            )
        )

        assertEquals(
            WorkspacePagedRoomMutationResult.CreatedPage("home:user:new", 3),
            repository.createHomePage("home:user:new"),
        )
        assertEquals(
            WorkspacePagedRoomMutationResult.PageAlreadyExists,
            repository.createHomePage("home:user:new"),
        )
        assertEquals(
            listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID, "home:1", "home:2", "home:user:new"),
            database.workspaceDao().readPagesByContainer(WorkspaceContainerType.HOME).map { it.pageId },
        )
        assertTrue(database.workspaceDao().readItems(listOf("home:user:new")).isEmpty())

        assertEquals(
            WorkspacePagedRoomMutationResult.PrimaryPageProtected,
            repository.deleteEmptyHomePage(WorkspaceLegacyImportMapper.HOME_PAGE_ID),
        )
        assertEquals(
            WorkspacePagedRoomMutationResult.PageNotEmpty,
            repository.deleteEmptyHomePage("home:2"),
        )
        assertEquals(
            WorkspacePagedRoomMutationResult.DeletedPage(
                pageId = "home:user:new",
                orderedPageIds = listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID, "home:1", "home:2"),
            ),
            repository.deleteEmptyHomePage("home:user:new"),
        )
        assertEquals(
            listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID, "home:1", "home:2"),
            database.workspaceDao().readPagesByContainer(WorkspaceContainerType.HOME).map { it.pageId },
        )

        assertEquals(
            WorkspacePagedRoomMutationResult.PrimaryPageProtected,
            repository.moveHomePage("home:2", 0),
        )
        assertEquals(
            WorkspacePagedRoomMutationResult.PrimaryPageProtected,
            repository.moveHomePage(WorkspaceLegacyImportMapper.HOME_PAGE_ID, 1),
        )
        assertEquals(
            WorkspacePagedRoomMutationResult.Updated(
                listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID, "home:2", "home:1")
            ),
            repository.moveHomePage("home:2", 1),
        )

        assertEquals(
            listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID, "home:2", "home:1"),
            database.workspaceDao().readPagesByContainer(WorkspaceContainerType.HOME).map { it.pageId },
        )
        val preserved = database.workspaceDao().readItems(listOf("home:2"))
        assertEquals(1, preserved.size)
        assertEquals("native:item:two", preserved.single().itemId)
        assertEquals(APP_TWO, preserved.single().appKey)
        assertEquals(
            WorkspaceRelationalSnapshot(
                favoriteKeys = listOf(APP_ONE),
                dockKeys = emptyList(),
            ),
            WorkspaceCanonicalRoomPlacementReader.read(database.workspaceDao()),
        )

        assertEquals(
            WorkspacePagedRoomMutationResult.TargetRankOutOfRange,
            repository.moveHomePage("home:2", 3),
        )
        assertEquals(
            WorkspacePagedRoomMutationResult.PageNotFound,
            repository.moveHomePage("missing", 0),
        )
    }

    private fun openDataStore(): DataStore<Preferences> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStoreScope = scope
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { dataStoreFile })
    }

    private companion object {
        const val DATABASE_NAME = "launcher-paged-room-mutation-test.db"
        const val DATASTORE_FILE = "launcher-paged-room-mutation.preferences_pb"
        const val APP_ONE = "10:com.example.one/.MainActivity"
        const val APP_TWO = "10:com.example.two/.MainActivity"
    }
}
