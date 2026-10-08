package com.goreecloud.launcher.core.workspace.db

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [WorkspacePageEntity::class, WorkspaceItemEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun workspaceDao(): WorkspaceDao
}

object LauncherDatabaseProvider {
    private const val DATABASE_NAME = "launcher-workspace.db"

    @Volatile
    private var instance: LauncherDatabase? = null

    fun get(context: Context): LauncherDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                LauncherDatabase::class.java,
                DATABASE_NAME,
            )
                .setDriver(AndroidSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
                .also { instance = it }
        }
}
