package com.privacyguard.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.privacyguard.data.database.dao.DetectionEventDao
import com.privacyguard.data.database.entities.DetectionEvent

@Database(
    entities = [
        DetectionEvent::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PrivacyGuardDatabase : RoomDatabase() {
    abstract fun detectionEventDao(): DetectionEventDao
}
