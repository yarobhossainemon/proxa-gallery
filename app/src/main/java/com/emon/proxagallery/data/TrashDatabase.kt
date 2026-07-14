package com.emon.proxagallery.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TrashItem::class], version = 1, exportSchema = false)
abstract class TrashDatabase : RoomDatabase() {

    abstract fun trashDao(): TrashDao

    companion object {
        @Volatile
        private var INSTANCE: TrashDatabase? = null

        fun getInstance(context: Context): TrashDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TrashDatabase::class.java,
                    "proxa_trash.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
