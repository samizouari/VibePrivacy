package com.privacyguard.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de données Room pour Privacy Guard
 * Version 1: Tables sessions et threat_events
 */
@Database(
    entities = [SessionEntity::class, ThreatEventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun sessionDao(): SessionDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "privacyguard_database"
                )
                    .fallbackToDestructiveMigration() // Pour le développement
                    .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}

