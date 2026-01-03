package com.privacyguard.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO pour accéder aux sessions et aux événements de menace
 */
@Dao
interface SessionDao {
    
    // ===== Sessions =====
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long
    
    @Update
    suspend fun updateSession(session: SessionEntity)
    
    @Delete
    suspend fun deleteSession(session: SessionEntity)
    
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): SessionEntity?
    
    @Query("SELECT * FROM sessions WHERE endTime IS NULL LIMIT 1")
    suspend fun getActiveSession(): SessionEntity?
    
    @Query("SELECT * FROM sessions WHERE protectionMode = :mode ORDER BY startTime DESC")
    fun getSessionsByMode(mode: String): Flow<List<SessionEntity>>
    
    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>
    
    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun getSessionCount(): Int
    
    @Query("DELETE FROM sessions WHERE id IN (SELECT id FROM sessions ORDER BY startTime ASC LIMIT :count)")
    suspend fun deleteOldestSessions(count: Int)
    
    // ===== Threat Events =====
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreatEvent(event: ThreatEventEntity): Long
    
    @Query("SELECT * FROM threat_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getThreatEventsBySession(sessionId: Long): Flow<List<ThreatEventEntity>>
    
    @Query("SELECT * FROM threat_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getThreatEventsBySessionSync(sessionId: Long): List<ThreatEventEntity>
    
    @Query("DELETE FROM threat_events WHERE sessionId = :sessionId")
    suspend fun deleteThreatEventsBySession(sessionId: Long)
    
    @Query("SELECT COUNT(*) FROM threat_events WHERE sessionId = :sessionId")
    suspend fun getThreatEventCount(sessionId: Long): Int
    
    // ===== Combined queries =====
    
    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getSessionWithEvents(sessionId: Long): SessionWithEvents?
}

/**
 * Relation entre une session et ses événements de menace
 */
data class SessionWithEvents(
    @Embedded val session: SessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val events: List<ThreatEventEntity>
)

