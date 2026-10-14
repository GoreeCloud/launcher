package app.lawnchair.gestures.handlers

import android.content.Context
import app.lawnchair.LawnchairLauncher
import app.lawnchair.animateToAllApps
import app.lawnchair.preferences2.PreferenceManager2
import app.lawnchair.preferences2.firstCached

class OpenAppSearchGestureHandler(context: Context) : GestureHandler(context) {

    override suspend fun onTrigger(launcher: LawnchairLauncher) {
        val prefs = PreferenceManager2.getInstance(launcher)
        if (!prefs.universalSearchEnabled.firstCached()) return
        val searchUiManager = launcher.appsView.searchUiManager
        val openKeyboard = prefs.universalSearchKeyboardOnOpen.firstCached()
        searchUiManager.setDirectFocus(openKeyboard)
        launcher.animateToAllApps()
        if (openKeyboard) {
            searchUiManager.editText?.post {
                searchUiManager.editText?.requestFocus()
                searchUiManager.editText?.showKeyboard()
            }
        }
    }
}
