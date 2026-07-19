package com.emon.proxagallery.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TrashItem::class, AlbumCustomization::class],
    version = 3,
    exportSchema = false
)
abstract class TrashDatabase : RoomDatabase() {

    abstract fun trashDao(): TrashDao
    abstract fun albumCustomizationDao(): AlbumCustomizationDao

    companion object {
        @Volatile
        private var INSTANCE: TrashDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `album_customizations` (
                        `bucketId` INTEGER NOT NULL,
                        `customName` TEXT,
                        `customCoverUri` TEXT,
                        `isPinned` INTEGER NOT NULL DEFAULT 0,
                        `isHidden` INTEGER NOT NULL DEFAULT 0,
                        `colorTag` TEXT,
                        `sortMode` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`bucketId`)
                    )"""
                )
            }
        }

        fun getInstance(context: Context): TrashDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TrashDatabase::class.java,
                    "proxa_trash.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
