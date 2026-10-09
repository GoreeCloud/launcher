package com.goreecloud.launcher.core.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.core.content.ContextCompat
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

internal fun launcherLabelSortKey(label: CharSequence): String =
    Normalizer.normalize(label.toString(), Normalizer.Form.NFC).lowercase(Locale.ROOT)

/**
 * Applies a stable, profile-qualified total order to independently enumerated Android apps.
 *
 * An identical activity can appear in User and Work profiles with the same visible label,
 * package, and class name. Without the profile tie-breaker, LauncherApps enumeration order
 * affects the final drawer position on every refresh, causing needless LazyGrid reordering.
 */
internal fun <T> launcherInventoryStableOrder(
    items: List<T>,
    labelOf: (T) -> CharSequence,
    packageOf: (T) -> String,
    classOf: (T) -> String,
    profileOf: (T) -> Int,
): List<T> = items.sortedWith(
    compareBy(
        { launcherLabelSortKey(labelOf(it)) },
        packageOf,
        classOf,
        profileOf,
    ),
)

internal enum class LauncherInventoryChange {
    PACKAGE_ADDED,
    PACKAGE_REMOVED,
    PACKAGE_CHANGED,
    PACKAGE_SUSPENDED,
    PACKAGE_UNSUSPENDED,
    PACKAGES_AVAILABLE,
    PACKAGES_UNAVAILABLE,
    PROFILE_TOPOLOGY,
}

internal enum class LauncherInventoryRefreshScope {
    PACKAGE,
    FULL,
}

internal fun launcherInventoryRefreshScope(
    change: LauncherInventoryChange,
): LauncherInventoryRefreshScope = when (change) {
    LauncherInventoryChange.PACKAGE_REMOVED,
    -> LauncherInventoryRefreshScope.PACKAGE

    LauncherInventoryChange.PACKAGE_ADDED,
    LauncherInventoryChange.PACKAGE_CHANGED,
    LauncherInventoryChange.PACKAGE_SUSPENDED,
    LauncherInventoryChange.PACKAGE_UNSUSPENDED,
    LauncherInventoryChange.PACKAGES_AVAILABLE,
    LauncherInventoryChange.PACKAGES_UNAVAILABLE,
    LauncherInventoryChange.PROFILE_TOPOLOGY,
    -> LauncherInventoryRefreshScope.FULL
}

internal fun <T, U, K> launcherInventoryHasActiveProfileLoss(
    previous: List<T>,
    candidate: List<T>,
    activeProfiles: Collection<U>,
    userOf: (T) -> U,
    keyOf: (T) -> K,
): Boolean {
    if (previous.isEmpty()) return false
    val active = activeProfiles.toHashSet()
    if (active.isEmpty()) return false
    val candidateKeys = candidate.asSequence()
        .filter { item -> userOf(item) in active }
        .map(keyOf)
        .toHashSet()
    return previous.asSequence()
        .filter { item -> userOf(item) in active }
        .map(keyOf)
        .any { key -> key !in candidateKeys }
}

/**
 * Query package-scoped inventory only for activities absent from the broad result.
 * Ignore previous entries from profiles Android no longer reports as active.
 */
internal fun <T, U, K> launcherInventoryMissingPreviousActivities(
    previous: List<T>,
    candidate: List<T>,
    activeProfiles: Collection<U>,
    userOf: (T) -> U,
    keyOf: (T) -> K,
): List<T> {
    val active = activeProfiles.toHashSet()
    val observed = candidate.mapTo(hashSetOf(), keyOf)
    return previous.filter { item ->
        userOf(item) in active && keyOf(item) !in observed
    }.distinctBy(keyOf)
}

/**
 * A repeated enumeration omission does not prove app removal. Retain only entries
 * Android verifies are enabled within profiles that remain available.
 */
internal fun <T, U, K> launcherInventoryRetainVerifiedActive(
    previous: List<T>,
    candidate: List<T>,
    activeProfiles: Collection<U>,
    userOf: (T) -> U,
    keyOf: (T) -> K,
    stillEnabled: (T) -> Boolean,
): List<T> {
    val active = activeProfiles.toHashSet()
    val seen = candidate.mapTo(hashSetOf(), keyOf)
    val retained = previous.filter { item ->
        val key = keyOf(item)
        userOf(item) in active && key !in seen && stillEnabled(item) && seen.add(key)
    }
    return candidate + retained
}

/** A missing profile also merits a second scan, even if no active-profile key was lost. */
internal fun <T, U> launcherInventoryHasMissingPreviousProfile(
    previous: List<T>,
    activeProfiles: Collection<U>,
    userOf: (T) -> U,
): Boolean {
    val active = activeProfiles.toHashSet()
    return previous.any { item -> userOf(item) !in active }
}

internal enum class LauncherDrawerProfileKind(
    val displayName: String,
) {
    USER("User Apps"),
    WORK("Work Apps"),
}

internal data class LauncherDrawerProfilePage<T>(
    val kind: LauncherDrawerProfileKind,
    val items: List<T>,
)

/**
 * Partitions authoritative LauncherApps inventory into the initial two drawer profile pages.
 *
 * The primary Android user always owns the first User Apps page. Any non-primary profile inventory
 * is grouped into Work Apps for this bounded Shelter/work-profile tranche. Android remains profile
 * and inventory authority; the drawer only projects that inventory and preserves its established
 * ordering within each page.
 */
internal fun <T, U> launcherDrawerProfilePages(
    items: List<T>,
    primaryUser: U,
    userOf: (T) -> U,
): List<LauncherDrawerProfilePage<T>> {
    val userItems = items.filter { item -> userOf(item) == primaryUser }
    val workItems = items.filterNot { item -> userOf(item) == primaryUser }

    return buildList {
        add(
            LauncherDrawerProfilePage(
                kind = LauncherDrawerProfileKind.USER,
                items = userItems,
            ),
        )
        if (workItems.isNotEmpty()) {
            add(
                LauncherDrawerProfilePage(
                    kind = LauncherDrawerProfileKind.WORK,
                    items = workItems,
                ),
            )
        }
    }
}

private data class LauncherInventoryScan(
    val apps: List<LauncherActivityInfo>,
    val activeProfiles: Set<UserHandle>,
)

private data class LauncherPackageScope(
    val packageName: String,
    val user: UserHandle,
)

private sealed interface LauncherInventoryRefreshRequest {
    data object Full : LauncherInventoryRefreshRequest

    data class PackageScope(
        val scope: LauncherPackageScope,
    ) : LauncherInventoryRefreshRequest
}

class LauncherAppsRepository(context: Context) {
    private companion object {
        const val FULL_REFRESH_CONFIRMATION_DELAY_MS = 250L
    }

    private val appContext = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val userManager = appContext.getSystemService(UserManager::class.java)
    private val callbackHandler = Handler(Looper.getMainLooper())
    private val explicitRefreshRequests = Channel<Unit>(Channel.CONFLATED)

    val apps: Flow<List<LauncherActivityInfo>> = callbackFlow {
        val refreshRequests = Channel<LauncherInventoryRefreshRequest>(Channel.UNLIMITED)
        val explicitRefreshWorker = launch {
            for (ignored in explicitRefreshRequests) {
                refreshRequests.send(LauncherInventoryRefreshRequest.Full)
            }
        }
        val refreshWorker = launch(Dispatchers.IO) {
            var currentSnapshot = emptyList<LauncherActivityInfo>()
            var initialized = false

            for (firstRequest in refreshRequests) {
                var requiresFullRefresh = firstRequest is LauncherInventoryRefreshRequest.Full
                val packageScopes = linkedSetOf<LauncherPackageScope>()
                if (firstRequest is LauncherInventoryRefreshRequest.PackageScope) {
                    packageScopes += firstRequest.scope
                }

                while (true) {
                    val queued = refreshRequests.tryReceive().getOrNull() ?: break
                    when (queued) {
                        LauncherInventoryRefreshRequest.Full -> requiresFullRefresh = true
                        is LauncherInventoryRefreshRequest.PackageScope -> packageScopes += queued.scope
                    }
                }

                if (requiresFullRefresh || !initialized) {
                    val firstScan =
                        runCatching { loadApps(previous = currentSnapshot) }.getOrNull() ?: continue
                    var snapshot = firstScan.apps
                    if (
                        initialized &&
                        (
                            launcherInventoryHasActiveProfileLoss(
                                previous = currentSnapshot,
                                candidate = snapshot,
                                activeProfiles = firstScan.activeProfiles,
                                userOf = { app -> app.user },
                                keyOf = { app ->
                                    "${app.user.hashCode()}:${app.componentName.flattenToString()}"
                                },
                            ) ||
                                launcherInventoryHasMissingPreviousProfile(
                                    previous = currentSnapshot,
                                    activeProfiles = firstScan.activeProfiles,
                                    userOf = { app -> app.user },
                                )
                            )
                    ) {
                        // A single broad + package-scoped scan can still transiently omit apps on
                        // some OEM/profile transitions. Confirm an active-profile loss before
                        // publishing it so the drawer does not visibly blank and then repopulate.
                        delay(FULL_REFRESH_CONFIRMATION_DELAY_MS)
                        val confirmedScan =
                            runCatching { loadApps(previous = currentSnapshot) }.getOrNull()
                                ?: continue
                        snapshot = normalizeSnapshot(
                            launcherInventoryRetainVerifiedActive(
                                previous = currentSnapshot,
                                candidate = confirmedScan.apps,
                                activeProfiles = confirmedScan.activeProfiles,
                                userOf = { it.user },
                                keyOf = { it.user to it.componentName },
                                stillEnabled = { app ->
                                    runCatching {
                                        !userManager.isQuietModeEnabled(app.user) &&
                                            launcherApps.isPackageEnabled(
                                                app.componentName.packageName, app.user,
                                            ) &&
                                            launcherApps.isActivityEnabled(
                                                app.componentName, app.user,
                                            )
                                    }.getOrDefault(false)
                                },
                            ),
                        )
                    }
                    currentSnapshot = snapshot
                    initialized = true
                    LauncherAppIconCache.preload(snapshot, appContext.packageManager)
                    trySend(snapshot)
                    continue
                }

                var nextSnapshot = currentSnapshot
                var refreshedAnyScope = false
                val refreshedActivities = mutableListOf<LauncherActivityInfo>()
                for (scope in packageScopes) {
                    val replacement = runCatching { loadPackageApps(scope) }.getOrNull() ?: continue
                    nextSnapshot = replacePackageScope(
                        current = nextSnapshot,
                        scope = scope,
                        replacement = replacement,
                    )
                    refreshedActivities += replacement
                    refreshedAnyScope = true
                }

                if (refreshedAnyScope) {
                    currentSnapshot = normalizeSnapshot(nextSnapshot)
                    LauncherAppIconCache.preload(
                        refreshedActivities + currentSnapshot,
                        appContext.packageManager,
                    )
                    trySend(currentSnapshot)
                }
            }
        }

        fun requestPackageRefresh(
            packageName: String,
            user: UserHandle,
            change: LauncherInventoryChange,
        ) {
            LauncherAppIconCache.invalidatePackage(packageName, user)
            when (launcherInventoryRefreshScope(change)) {
                LauncherInventoryRefreshScope.FULL ->
                    refreshRequests.trySend(LauncherInventoryRefreshRequest.Full)

                LauncherInventoryRefreshScope.PACKAGE ->
                    refreshRequests.trySend(
                        LauncherInventoryRefreshRequest.PackageScope(
                            LauncherPackageScope(packageName = packageName, user = user),
                        ),
                    )
            }
        }

        fun requestFullRefresh() {
            refreshRequests.trySend(LauncherInventoryRefreshRequest.Full)
        }

        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) =
                requestPackageRefresh(packageName, user, LauncherInventoryChange.PACKAGE_REMOVED)

            override fun onPackageAdded(packageName: String, user: UserHandle) =
                requestPackageRefresh(packageName, user, LauncherInventoryChange.PACKAGE_ADDED)

            override fun onPackageChanged(packageName: String, user: UserHandle) =
                requestPackageRefresh(packageName, user, LauncherInventoryChange.PACKAGE_CHANGED)

            override fun onPackagesAvailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean,
            ) {
                packageNames.forEach { packageName ->
                    LauncherAppIconCache.invalidatePackage(packageName, user)
                }
                requestFullRefresh()
            }

            override fun onPackagesUnavailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean,
            ) {
                packageNames.forEach { packageName ->
                    LauncherAppIconCache.invalidatePackage(packageName, user)
                }
                requestFullRefresh()
            }

            override fun onPackagesSuspended(packageNames: Array<out String>, user: UserHandle) {
                packageNames.forEach { packageName ->
                    requestPackageRefresh(
                        packageName,
                        user,
                        LauncherInventoryChange.PACKAGE_SUSPENDED,
                    )
                }
            }

            override fun onPackagesUnsuspended(packageNames: Array<out String>, user: UserHandle) {
                packageNames.forEach { packageName ->
                    requestPackageRefresh(
                        packageName,
                        user,
                        LauncherInventoryChange.PACKAGE_UNSUSPENDED,
                    )
                }
            }
        }

        val profileReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_MANAGED_PROFILE_ADDED,
                    Intent.ACTION_MANAGED_PROFILE_REMOVED,
                    Intent.ACTION_MANAGED_PROFILE_AVAILABLE,
                    Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE,
                    Intent.ACTION_PROFILE_ADDED,
                    Intent.ACTION_PROFILE_REMOVED,
                    Intent.ACTION_PROFILE_AVAILABLE,
                    Intent.ACTION_PROFILE_UNAVAILABLE,
                    -> {
                        LauncherAppIconCache.clear()
                        requestFullRefresh()
                    }
                }
            }
        }
        val profileFilter = IntentFilter().apply {
            addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
            addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
            addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
            if (Build.VERSION.SDK_INT >= 34) {
                addAction(Intent.ACTION_PROFILE_ADDED)
                addAction(Intent.ACTION_PROFILE_REMOVED)
            }
            if (Build.VERSION.SDK_INT >= 35) {
                addAction(Intent.ACTION_PROFILE_AVAILABLE)
                addAction(Intent.ACTION_PROFILE_UNAVAILABLE)
            }
        }

        launcherApps.registerCallback(callback, callbackHandler)
        val profileReceiverRegistered = runCatching {
            ContextCompat.registerReceiver(
                appContext,
                profileReceiver,
                profileFilter,
                ContextCompat.RECEIVER_EXPORTED,
            )
        }.isSuccess
        requestFullRefresh()

        awaitClose {
            launcherApps.unregisterCallback(callback)
            if (profileReceiverRegistered) {
                runCatching { appContext.unregisterReceiver(profileReceiver) }
            }
            explicitRefreshWorker.cancel()
            refreshRequests.close()
            refreshWorker.cancel()
        }
    }.conflate()

    /**
     * Reconciles the complete Android-visible launcher inventory across User and Work profiles.
     * LauncherApps callbacks remain the live path; this is a foreground safety refresh.
     */
    fun refreshInventory() {
        explicitRefreshRequests.trySend(Unit)
    }

    fun launch(app: LauncherActivityInfo) {
        launcherApps.startMainActivity(app.componentName, app.user, Rect(), Bundle.EMPTY)
    }

    fun launchShortcut(
        packageName: String,
        shortcutId: String,
        user: UserHandle,
    ) {
        launcherApps.startShortcut(packageName, shortcutId, Rect(), Bundle.EMPTY, user)
    }

    fun openDetails(app: LauncherActivityInfo) {
        launcherApps.startAppDetailsActivity(app.componentName, app.user, Rect(), Bundle.EMPTY)
    }

    /**
     * Reconciles broad LauncherApps enumeration with package-scoped reads so transient OEM broad-scan
     * omissions do not make still-launchable apps disappear from the drawer.
     */
    private fun loadApps(
        previous: List<LauncherActivityInfo> = emptyList(),
    ): LauncherInventoryScan {
        val activeProfiles = launcherApps.profiles.toSet()
        val broadSnapshot = activeProfiles.flatMap { profile ->
            launcherApps.getActivityList(null, profile)
        }
        val recoveryScopes = linkedSetOf<LauncherPackageScope>()
        // Recover only previously observed activities that the broad scan missed.
        launcherInventoryMissingPreviousActivities(
            previous = previous,
            candidate = broadSnapshot,
            activeProfiles = activeProfiles,
            userOf = { it.user },
            keyOf = { it.user to it.componentName },
        ).forEach { app ->
            recoveryScopes += LauncherPackageScope(
                packageName = app.componentName.packageName,
                user = app.user,
            )
        }
        val primaryUser = Process.myUserHandle()
        val observedPrimaryPackages = broadSnapshot.asSequence()
            .filter { it.user == primaryUser }
            .map { it.componentName.packageName }
            .toSet()
        visiblePrimaryLauncherPackages()
            .filterNot { it in observedPrimaryPackages }
            .forEach { packageName ->
                recoveryScopes += LauncherPackageScope(packageName, primaryUser)
            }
        val recovered = recoveryScopes.flatMap { scope ->
            runCatching { launcherApps.getActivityList(scope.packageName, scope.user) }
                .getOrDefault(emptyList())
        }
        return LauncherInventoryScan(
            apps = normalizeSnapshot(broadSnapshot + recovered),
            activeProfiles = activeProfiles,
        )
    }

    @Suppress("DEPRECATION")
    private fun visiblePrimaryLauncherPackages(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return appContext.packageManager
            .queryIntentActivities(intent, 0)
            .mapNotNull { resolveInfo -> resolveInfo.activityInfo?.packageName }
            .filter { it.isNotBlank() }
            .toCollection(linkedSetOf())
    }

    private fun loadPackageApps(scope: LauncherPackageScope): List<LauncherActivityInfo> =
        launcherApps.getActivityList(scope.packageName, scope.user)

    private fun replacePackageScope(
        current: List<LauncherActivityInfo>,
        scope: LauncherPackageScope,
        replacement: List<LauncherActivityInfo>,
    ): List<LauncherActivityInfo> =
        current.filterNot { app ->
            app.user == scope.user && app.componentName.packageName == scope.packageName
        } + replacement

    private fun normalizeSnapshot(
        apps: List<LauncherActivityInfo>,
    ): List<LauncherActivityInfo> =
        apps.distinctBy { app ->
            "${app.user.hashCode()}:${app.componentName.flattenToString()}"
        }.let { deduplicated ->
            launcherInventoryStableOrder(
                items = deduplicated,
                labelOf = { it.label },
                packageOf = { it.componentName.packageName },
                classOf = { it.componentName.className },
                profileOf = { it.user.hashCode() },
            )
        }
}
