package com.n7.vibeprivacy.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.n7.vibeprivacy.data.models.ProtectionProfile
import com.n7.vibeprivacy.data.models.ThreatEvent
import com.n7.vibeprivacy.data.models.WhitelistedFace
import com.n7.vibeprivacy.data.source.ProtectionProfileDao
import com.n7.vibeprivacy.data.source.ThreatEventDao
import com.n7.vibeprivacy.data.source.WhitelistedFaceDao

@Database(
    entities = [ThreatEvent::class, ProtectionProfile::class, WhitelistedFace::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun threatEventDao(): ThreatEventDao
    abstract fun protectionProfileDao(): ProtectionProfileDao
    abstract fun whitelistedFaceDao(): WhitelistedFaceDao
}