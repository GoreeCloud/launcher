package com.goreecloud.launcher.core.launcher

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.goreecloud.launcher.core.workspace.WorkspaceGridPlacement
import com.goreecloud.launcher.core.workspace.WorkspacePagedPlacement
import com.goreecloud.launcher.core.workspace.WorkspacePortableSnapshot
import com.goreecloud.launcher.core.workspace.db.LauncherDatabase
import com.goreecloud.launcher.core.workspace.db.WorkspaceContainerType
import com.goreecloud.launcher.core.workspace.db.WorkspaceItemEntity
import com.goreecloud.launcher.core.workspace.db.WorkspaceItemType
import com.goreecloud.launcher.core.workspace.db.WorkspacePageEntity
import com.goreecloud.launcher.core.workspace.db.WorkspacePortableHomeRestoreCommit
import com.goreecloud.launcher.core.workspace.db.WorkspacePortableHomeStateFingerprint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherPortableRestorePersistenceRuntimeTest {
    private lateinit var context: Context
    private lateinit var database: LauncherDatabase
    private lateinit var preferencesRepository: LauncherPreferencesRepository
    private lateinit var originalPreferences: LauncherPreferences

    @Before
    fun setUp() = runBlocking {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(DATABASE_NAME)
        database = Room.databaseBuilder(
            context.applicationContext,
            LauncherDatabase::class.java,
            DATABASE_NAME,
        )
            .setDriver(AndroidSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        preferencesRepository = LauncherPreferencesRepository(context.applicationContext)
        clearAnyValidJournal()
        originalPreferences = preferencesRepository.readPortablePreferences()
    }

    @After
    fun tearDown() {
        runBlocking {
            clearAnyValidJournal()
            preferencesRepository.replacePortablePreferences(originalPreferences)
            database.close()
            context.deleteDatabase(DATABASE_NAME)
        }
    }

    @Test
    fun roomPlacementRestoreIsTransactionalAndRollbackRestoresPreviousHomeState() = runBlocking {
        val dao = database.workspaceDao()
        seedTwoHomeAppsAndDock()
        val previousPages = dao.readPagesByContainer(WorkspaceContainerType.HOME)
        val previousItems = dao.readItemsByContainer(WorkspaceContainerType.HOME)

        val commit = dao.replacePortableHomePlacements(restoredWorkspace())

        assertEquals(listOf("restored:0"), dao.readPagesByContainer(WorkspaceContainerType.HOME).map { it.pageId })
        val restoredItems = dao.readItemsByContainer(WorkspaceContainerType.HOME)
        assertEquals(listOf(APP_B, APP_A), restoredItems.map { it.itemId })
        assertEquals(listOf(0, 1), restoredItems.map { it.rank })
        assertEquals(listOf(0, 1), restoredItems.map { it.cellX })
        assertEquals(listOf(APP_KEY_B, APP_KEY_A), restoredItems.map { it.appKey })
        assertEquals(listOf(DOCK_PAGE), dao.readPagesByContainer(WorkspaceContainerType.DOCK).map { it.pageId })

        dao.rollbackPortableHomePlacements(commit)

        assertEquals(previousPages, dao.readPagesByContainer(WorkspaceContainerType.HOME))
        assertEquals(previousItems, dao.readItemsByContainer(WorkspaceContainerType.HOME))
        assertEquals(listOf(DOCK_PAGE), dao.readPagesByContainer(WorkspaceContainerType.DOCK).map { it.pageId })
    }

    @Test
    fun plannedRestoreRefusesWorkspaceChangedBeforeApply() = runBlocking {
        val dao = database.workspaceDao()
        seedTwoHomeAppsAndDock()
        val plan = dao.planPortableHomePlacements(restoredWorkspace())
        val original = dao.readItemsByContainer(WorkspaceContainerType.HOME).first { it.itemId == APP_A }
        dao.upsertItems(listOf(original.copy(cellX = 3)))

        try {
            dao.applyPortableHomeRestoreCommit(plan)
            fail("a planned restore must not overwrite HOME state changed after planning")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message.orEmpty().contains("changed after restore planning"))
        }

        assertEquals(
            3,
            dao.readItemsByContainer(WorkspaceContainerType.HOME).first { it.itemId == APP_A }.cellX,
        )
    }

    @Test
    fun cleanTargetCannotInventMissingAppIdentityRebinding() = runBlocking {
        val dao = database.workspaceDao()
        dao.upsertPages(
            listOf(
                WorkspacePageEntity(
                    pageId = HOME_PAGE_A,
                    containerType = WorkspaceContainerType.HOME,
                    rank = 0,
                )
            )
        )

        try {
            dao.replacePortableHomePlacements(restoredWorkspace())
            fail("non-empty portable restore must not invent app/package/profile identities on a clean target")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message.orEmpty().contains("cannot be rebound"))
        }

        assertEquals(listOf(HOME_PAGE_A), dao.readPagesByContainer(WorkspaceContainerType.HOME).map { it.pageId })
        assertTrue(dao.readItemsByContainer(WorkspaceContainerType.HOME).isEmpty())
    }

    @Test
    fun concreteWriterAppliesCompatibleRoomAndDataStorePairAndClearsJournal() = runBlocking {
        val dao = database.workspaceDao()
        seedTwoHomeAppsAndDock()
        val before = basePreferences()
        val target = targetPreferences()
        preferencesRepository.replacePortablePreferences(before)

        LauncherTransactionalPortableRestoreWriter(dao, preferencesRepository)
            .replacePortableState(restoredWorkspace(), target)

        assertEquals(target, preferencesRepository.readPortablePreferences())
        assertEquals(
            LauncherPortableRestoreJournalReadResult.Absent,
            preferencesRepository.readPortableRestoreJournal(),
        )
        assertEquals(listOf("restored:0"), dao.readPagesByContainer(WorkspaceContainerType.HOME).map { it.pageId })
        assertEquals(listOf(APP_B, APP_A), dao.readItemsByContainer(WorkspaceContainerType.HOME).map { it.itemId })
    }

    @Test
    fun recoveryAbandonsJournalWhenRoomNeverApplied() = runBlocking {
        val dao = database.workspaceDao()
        seedTwoHomeAppsAndDock()
        val before = basePreferences()
        val target = targetPreferences()
        preferencesRepository.replacePortablePreferences(before)
        val plan = dao.planPortableHomePlacements(restoredWorkspace())
        val journal = journalFor(plan, before, target)
        assertTrue(preferencesRepository.beginPortableRestoreJournal(journal))

        val result = LauncherPortableRestoreRecoveryCoordinator(dao, preferencesRepository).reconcile()

        assertEquals(
            LauncherPortableRestoreRecoveryCoordinator.Result.AbandonedBeforeWorkspaceApply,
            result,
        )
        assertEquals(before, preferencesRepository.readPortablePreferences())
        assertEquals(
            plan.previousPages,
            dao.readPagesByContainer(WorkspaceContainerType.HOME),
        )
        assertEquals(
            LauncherPortableRestoreJournalReadResult.Absent,
            preferencesRepository.readPortableRestoreJournal(),
        )
    }

    @Test
    fun recoveryFinalizesPreferencesWhenRoomAppliedBeforeProcessLoss() = runBlocking {
        val dao = database.workspaceDao()
        seedTwoHomeAppsAndDock()
        val before = basePreferences()
        val target = targetPreferences()
        preferencesRepository.replacePortablePreferences(before)
        val plan = dao.planPortableHomePlacements(restoredWorkspace())
        val journal = journalFor(plan, before, target)
        assertTrue(preferencesRepository.beginPortableRestoreJournal(journal))
        dao.applyPortableHomeRestoreCommit(plan)

        val result = LauncherPortableRestoreRecoveryCoordinator(dao, preferencesRepository).reconcile()

        assertEquals(
            LauncherPortableRestoreRecoveryCoordinator.Result.FinalizedAfterWorkspaceApply,
            result,
        )
        assertEquals(target, preferencesRepository.readPortablePreferences())
        assertEquals(plan.appliedPages, dao.readPagesByContainer(WorkspaceContainerType.HOME))
        assertEquals(
            LauncherPortableRestoreJournalReadResult.Absent,
            preferencesRepository.readPortableRestoreJournal(),
        )
    }

    @Test
    fun recoveryConfirmsAlreadyAppliedPairAndClearsMatchingJournal() = runBlocking {
        val dao = database.workspaceDao()
        seedTwoHomeAppsAndDock()
        val before = basePreferences()
        val target = targetPreferences()
        preferencesRepository.replacePortablePreferences(before)
        val plan = dao.planPortableHomePlacements(restoredWorkspace())
        val journal = journalFor(plan, before, target)
        assertTrue(preferencesRepository.beginPortableRestoreJournal(journal))
        dao.applyPortableHomeRestoreCommit(plan)
        // Simulate target preferences becoming visible while the matching journal remains durable.
        preferencesRepository.replacePortablePreferences(target)

        val result = LauncherPortableRestoreRecoveryCoordinator(dao, preferencesRepository).reconcile()

        assertEquals(LauncherPortableRestoreRecoveryCoordinator.Result.ConfirmedCommitted, result)
        assertEquals(target, preferencesRepository.readPortablePreferences())
        assertEquals(
            LauncherPortableRestoreJournalReadResult.Absent,
            preferencesRepository.readPortableRestoreJournal(),
        )
    }

    @Test
    fun recoveryRefusesConcurrentThirdPreferenceStateAndPreservesJournal() = runBlocking {
        val dao = database.workspaceDao()
        seedTwoHomeAppsAndDock()
        val before = basePreferences()
        val target = targetPreferences()
        val concurrent = before.copy(drawerColumns = 6, iconScale = 1.1f)
        preferencesRepository.replacePortablePreferences(before)
        val plan = dao.planPortableHomePlacements(restoredWorkspace())
        val journal = journalFor(plan, before, target)
        assertTrue(preferencesRepository.beginPortableRestoreJournal(journal))
        dao.applyPortableHomeRestoreCommit(plan)
        preferencesRepository.replacePortablePreferences(concurrent)

        val result = LauncherPortableRestoreRecoveryCoordinator(dao, preferencesRepository).reconcile()

        assertEquals(
            LauncherPortableRestoreRecoveryCoordinator.Result.RecoveryRequired(
                LauncherPortableRestoreRecoveryCoordinator.RecoveryReason.STATE_MISMATCH,
            ),
            result,
        )
        assertEquals(concurrent, preferencesRepository.readPortablePreferences())
        assertTrue(
            preferencesRepository.readPortableRestoreJournal() is
                LauncherPortableRestoreJournalReadResult.Present,
        )
        // Test cleanup only; the recovery coordinator deliberately did not erase this evidence.
        assertTrue(preferencesRepository.clearPortableRestoreJournalIfMatches(journal))
    }

    @Test
    fun concreteWriterRejectsGridMismatchBeforeRoomMutation() = runBlocking {
        val dao = database.workspaceDao()
        seedTwoHomeAppsAndDock()
        val beforePages = dao.readPagesByContainer(WorkspaceContainerType.HOME)
        val beforeItems = dao.readItemsByContainer(WorkspaceContainerType.HOME)

        try {
            LauncherTransactionalPortableRestoreWriter(dao, preferencesRepository)
                .replacePortableState(
                    restoredWorkspace(),
                    LauncherPreferences(homeColumns = 5, homeRows = 5),
                )
            fail("incompatible workspace/preference pair must be rejected before persistence")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message.orEmpty().contains("grid must match"))
        }

        assertEquals(beforePages, dao.readPagesByContainer(WorkspaceContainerType.HOME))
        assertEquals(beforeItems, dao.readItemsByContainer(WorkspaceContainerType.HOME))
    }

    private suspend fun clearAnyValidJournal() {
        when (val pending = preferencesRepository.readPortableRestoreJournal()) {
            LauncherPortableRestoreJournalReadResult.Absent -> Unit
            is LauncherPortableRestoreJournalReadResult.Present -> {
                check(preferencesRepository.clearPortableRestoreJournalIfMatches(pending.journal))
            }
            is LauncherPortableRestoreJournalReadResult.Invalid -> {
                fail("runtime test encountered an invalid pre-existing restore journal")
            }
        }
    }

    private suspend fun seedTwoHomeAppsAndDock() {
        val dao = database.workspaceDao()
        dao.upsertPages(
            listOf(
                WorkspacePageEntity(HOME_PAGE_A, WorkspaceContainerType.HOME, 0),
                WorkspacePageEntity(HOME_PAGE_B, WorkspaceContainerType.HOME, 1),
                WorkspacePageEntity(DOCK_PAGE, WorkspaceContainerType.DOCK, 0),
            )
        )
        dao.upsertItems(
            listOf(
                WorkspaceItemEntity(
                    itemId = APP_A,
                    pageId = HOME_PAGE_A,
                    itemType = WorkspaceItemType.APP,
                    appKey = APP_KEY_A,
                    rank = 0,
                    cellX = 0,
                    cellY = 0,
                ),
                WorkspaceItemEntity(
                    itemId = APP_B,
                    pageId = HOME_PAGE_B,
                    itemType = WorkspaceItemType.APP,
                    appKey = APP_KEY_B,
                    rank = 0,
                    cellX = 2,
                    cellY = 1,
                ),
                WorkspaceItemEntity(
                    itemId = DOCK_APP,
                    pageId = DOCK_PAGE,
                    itemType = WorkspaceItemType.APP,
                    appKey = DOCK_APP_KEY,
                    rank = 0,
                    cellX = 0,
                    cellY = 0,
                ),
            )
        )
    }

    private fun journalFor(
        plan: WorkspacePortableHomeRestoreCommit,
        previous: LauncherPreferences,
        target: LauncherPreferences,
    ): LauncherPortableRestoreJournal = LauncherPortableRestoreJournal(
        transactionId = "runtime-test-restore",
        previousWorkspaceFingerprint = WorkspacePortableHomeStateFingerprint.of(
            plan.previousPages,
            plan.previousItems,
        ),
        appliedWorkspaceFingerprint = WorkspacePortableHomeStateFingerprint.of(
            plan.appliedPages,
            plan.appliedItems,
        ),
        previousPreferences = previous,
        targetPreferences = target,
    )

    private fun basePreferences(): LauncherPreferences = LauncherPreferences(
        homeColumns = 4,
        homeRows = 5,
        drawerColumns = 5,
        showLabels = true,
        iconScale = 1.0f,
        layoutLocked = false,
        universalSearchHomeMode = LauncherUniversalSearchHomeMode.PERMANENT,
    )

    private fun targetPreferences(): LauncherPreferences = basePreferences().copy(
        drawerColumns = 4,
        showLabels = false,
        iconScale = 0.95f,
        layoutLocked = true,
        universalSearchHomeMode = LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY,
    )

    private fun restoredWorkspace(): WorkspacePortableSnapshot.Snapshot =
        WorkspacePortableSnapshot.Snapshot(
            grid = WorkspaceGridPlacement.Grid(columns = 4, rows = 5),
            pages = listOf(
                WorkspacePagedPlacement.Page(
                    pageId = "restored:0",
                    rank = 0,
                    placements = listOf(
                        WorkspaceGridPlacement.Placement(APP_B, cellX = 0, cellY = 0),
                        WorkspaceGridPlacement.Placement(APP_A, cellX = 1, cellY = 0),
                    ),
                )
            ),
        )

    private companion object {
        const val DATABASE_NAME = "launcher-portable-restore-persistence-runtime.db"
        const val HOME_PAGE_A = "home:0"
        const val HOME_PAGE_B = "home:1"
        const val DOCK_PAGE = "dock:0"
        const val APP_A = "item:alpha"
        const val APP_B = "item:beta"
        const val DOCK_APP = "item:dock"
        const val APP_KEY_A = "10:com.example.alpha/.MainActivity"
        const val APP_KEY_B = "10:com.example.beta/.MainActivity"
        const val DOCK_APP_KEY = "10:com.example.dock/.MainActivity"
    }
}
