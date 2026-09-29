package com.goreecloud.launcher.core.workspace.db

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

object WorkspaceContainerType {
    const val HOME = "HOME"
    const val DOCK = "DOCK"
}

object WorkspaceItemType {
    const val APP = "APP"
    const val SHORTCUT = "SHORTCUT"
    const val FOLDER = "FOLDER"
    const val WIDGET = "WIDGET"
}

@Entity(
    tableName = "workspace_pages",
    indices = [
        Index(value = ["containerType", "rank"], unique = true),
    ],
)
data class WorkspacePageEntity(
    @PrimaryKey val pageId: String,
    val containerType: String,
    val rank: Int,
)

@Entity(
    tableName = "workspace_items",
    foreignKeys = [
        ForeignKey(
            entity = WorkspacePageEntity::class,
            parentColumns = ["pageId"],
            childColumns = ["pageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["pageId"]),
        Index(value = ["pageId", "rank"], unique = true),
        Index(value = ["appKey"]),
    ],
)
data class WorkspaceItemEntity(
    @PrimaryKey val itemId: String,
    val pageId: String,
    val itemType: String,
    val appKey: String?,
    val rank: Int,
    val cellX: Int?,
    val cellY: Int?,
    val spanX: Int = 1,
    val spanY: Int = 1,
)
