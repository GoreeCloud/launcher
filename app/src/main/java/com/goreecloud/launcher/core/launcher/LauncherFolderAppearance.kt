package com.goreecloud.launcher.core.launcher

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Glaze-owned preferences: shape and layout affect previews, never stored folder membership. */
enum class LauncherFolderPreviewLayout { GRID, RADIAL, STACK, FAN, LINE }
enum class LauncherFolderPreviewShape { FOLLOW_ICONS, ROUND, SQUIRCLE, ROUNDED_SQUARE }
enum class LauncherFolderPreviewSize { SMALL, MEDIUM, LARGE }
enum class LauncherFolderPreviewSurface { GLASS, SOLID, ACCENT }

object LauncherFolderAppearance {
    private const val STORE = "goreecloud_launcher_folder_appearance"
    private const val LAYOUT = "preview_layout"
    private const val SHAPE = "preview_shape"
    private const val SIZE = "preview_size"
    private const val SURFACE = "preview_surface"
    private const val OUTLINE = "preview_outline"

    private val layoutState = MutableStateFlow(LauncherFolderPreviewLayout.GRID)
    val layout = layoutState.asStateFlow()
    private val shapeState = MutableStateFlow(LauncherFolderPreviewShape.FOLLOW_ICONS)
    val shape = shapeState.asStateFlow()
    private val sizeState = MutableStateFlow(LauncherFolderPreviewSize.MEDIUM)
    val size = sizeState.asStateFlow()
    private val surfaceState = MutableStateFlow(LauncherFolderPreviewSurface.GLASS)
    val surface = surfaceState.asStateFlow()
    private val outlineState = MutableStateFlow(true)
    val outline = outlineState.asStateFlow()

    fun initialize(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        layoutState.value = LauncherFolderPreviewLayout.entries.firstOrNull {
            it.name == prefs.getString(LAYOUT, LauncherFolderPreviewLayout.GRID.name)
        } ?: LauncherFolderPreviewLayout.GRID
        shapeState.value = LauncherFolderPreviewShape.entries.firstOrNull {
            it.name == prefs.getString(SHAPE, LauncherFolderPreviewShape.FOLLOW_ICONS.name)
        } ?: LauncherFolderPreviewShape.FOLLOW_ICONS
        sizeState.value = LauncherFolderPreviewSize.entries.firstOrNull {
            it.name == prefs.getString(SIZE, LauncherFolderPreviewSize.MEDIUM.name)
        } ?: LauncherFolderPreviewSize.MEDIUM
        surfaceState.value = LauncherFolderPreviewSurface.entries.firstOrNull {
            it.name == prefs.getString(SURFACE, LauncherFolderPreviewSurface.GLASS.name)
        } ?: LauncherFolderPreviewSurface.GLASS
        outlineState.value = prefs.getBoolean(OUTLINE, true)
    }

    fun setLayout(context: Context, next: LauncherFolderPreviewLayout) {
        context.applicationContext.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .edit().putString(LAYOUT, next.name).apply()
        layoutState.value = next
    }

    fun setShape(context: Context, next: LauncherFolderPreviewShape) {
        context.applicationContext.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .edit().putString(SHAPE, next.name).apply()
        shapeState.value = next
    }

    fun setSize(context: Context, next: LauncherFolderPreviewSize) {
        context.applicationContext.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .edit().putString(SIZE, next.name).apply()
        sizeState.value = next
    }

    fun setSurface(context: Context, next: LauncherFolderPreviewSurface) {
        context.applicationContext.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .edit().putString(SURFACE, next.name).apply()
        surfaceState.value = next
    }

    fun setOutline(context: Context, enabled: Boolean) {
        context.applicationContext.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .edit().putBoolean(OUTLINE, enabled).apply()
        outlineState.value = enabled
    }
}
