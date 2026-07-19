package com.emon.proxagallery.ai

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        AiMediaIndexEntity::class,
        AiTextAttributeEntity::class,
        AiCategoryEntity::class,
        AiObjectEntity::class,
        AiTagEntity::class,
        AiDetectedAppEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(AiConverters::class)
abstract class AiDatabase : RoomDatabase() {
    abstract fun aiIndexDao(): AiIndexDao

    companion object {
        @Volatile
        private var INSTANCE: AiDatabase? = null

        fun getInstance(context: Context): AiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AiDatabase::class.java,
                    "proxa_ai.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
