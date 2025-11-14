package com.privacyguard.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.privacyguard.data.database.entities.DetectionEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface DetectionEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: DetectionEvent)

    @Query("SELECT * FROM detection_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<DetectionEvent>>

    @Query("DELETE FROM detection_events WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}
