package app.lawnchair.gestures.handlers

import android.content.Context
import android.content.Intent
import app.lawnchair.GoreeCloudUniversalSearchActivity
import app.lawnchair.LawnchairLauncher
import app.lawnchair.preferences2.PreferenceManager2
import app.lawnchair.preferences2.firstCached

/** Default Home swipe-down opens a Glaze pull-down surface, never All Apps. */
class OpenAppSearchGestureHandler(context: Context) : GestureHandler(context) {
    override suspend fun onTrigger(launcher: LawnchairLauncher) {
        val prefs = PreferenceManager2.getInstance(launcher)
        if (!prefs.universalSearchEnabled.firstCached()) return
        launcher.startActivity(Intent(launcher, GoreeCloudUniversalSearchActivity::class.java))
    }
}
