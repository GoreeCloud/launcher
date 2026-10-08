package com.goreecloud.launcher

/**
 * Pure classification for Android intents that must return Launcher to its primary Home surface.
 *
 * Keeping this logic independent from Activity state makes the HOME contract regression-testable.
 */
object LauncherHomeIntentPolicy {
    private const val ACTION_MAIN = "android.intent.action.MAIN"
    private const val CATEGORY_HOME = "android.intent.category.HOME"

    fun shouldResetToPrimaryHome(action: String?, categories: Set<String>?): Boolean =
        action == ACTION_MAIN && categories?.contains(CATEGORY_HOME) == true
}
