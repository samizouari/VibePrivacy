package com.n7.vibeprivacy.data.source

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.n7.vibeprivacy.data.models.ThreatEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreatEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(threatEvent: ThreatEvent)

    @Query("SELECT * FROM threat_events ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ThreatEvent>>

    @Query("DELETE FROM threat_events")
    suspend fun deleteAll()
}