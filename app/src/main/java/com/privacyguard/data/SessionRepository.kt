package com.privacyguard.data

import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * Repository pour gérer l'accès aux sessions et aux événements de menace
 * Implémente la logique de limitation à 100 sessions max
 */
class SessionRepository(private val sessionDao: SessionDao) {
    
    companion object {
        private const val MAX_SESSIONS = 100
    }
    
    // ===== Sessions =====
    
    suspend fun createSession(protectionMode: String): Long {
        Timber.d("Creating new session with mode: $protectionMode")
        
        // Vérifier si on dépasse la limite
        val sessionCount = sessionDao.getSessionCount()
        if (sessionCount >= MAX_SESSIONS) {
            val toDelete = sessionCount - MAX_SESSIONS + 1
            Timber.i("Max sessions reached ($sessionCount/$MAX_SESSIONS), deleting $toDelete oldest sessions")
            sessionDao.deleteOldestSessions(toDelete)
        }
        
        val session = SessionEntity(
            protectionMode = protectionMode,
            startTime = System.currentTimeMillis()
        )
        
        val sessionId = sessionDao.insertSession(session)
        Timber.i("Session created with ID: $sessionId")
        return sessionId
    }
    
    suspend fun updateSession(session: SessionEntity) {
        sessionDao.updateSession(session)
        Timber.d("Session ${session.id} updated")
    }
    
    suspend fun endSession(
        sessionId: Long,
        totalThreatsDetected: Int,
        totalThreatScore: Float,
        assessmentCount: Int,
        avgThreatScore: Float,
        maxThreatScore: Float,
        maxThreatLevel: String
    ) {
        val session = sessionDao.getSessionById(sessionId)
        if (session != null) {
            val updatedSession = session.copy(
                endTime = System.currentTimeMillis(),
                totalThreatsDetected = totalThreatsDetected,
                totalThreatScore = totalThreatScore,
                assessmentCount = assessmentCount,
                avgThreatScore = avgThreatScore,
                maxThreatScore = maxThreatScore,
                maxThreatLevel = maxThreatLevel
            )
            sessionDao.updateSession(updatedSession)
            Timber.i("Session $sessionId ended. Duration: ${updatedSession.duration}ms, Threats: $totalThreatsDetected")
        } else {
            Timber.w("Cannot end session $sessionId: not found")
        }
    }
    
    suspend fun getActiveSession(): SessionEntity? {
        return sessionDao.getActiveSession()
    }
    
    fun getSessionsByMode(mode: String): Flow<List<SessionEntity>> {
        return sessionDao.getSessionsByMode(mode)
    }
    
    fun getAllSessions(): Flow<List<SessionEntity>> {
        return sessionDao.getAllSessions()
    }
    
    suspend fun deleteSession(session: SessionEntity) {
        sessionDao.deleteSession(session)
        Timber.i("Session ${session.id} deleted")
    }
    
    // ===== Threat Events =====
    
    suspend fun addThreatEvent(
        sessionId: Long,
        threatScore: Float,
        threatLevel: String,
        actionTriggered: String?,
        cameraContribution: Float,
        audioContribution: Float,
        motionContribution: Float,
        proximityContribution: Float,
        reasons: String?
    ): Long {
        val event = ThreatEventEntity(
            sessionId = sessionId,
            timestamp = System.currentTimeMillis(),
            threatScore = threatScore,
            threatLevel = threatLevel,
            actionTriggered = actionTriggered,
            cameraContribution = cameraContribution,
            audioContribution = audioContribution,
            motionContribution = motionContribution,
            proximityContribution = proximityContribution,
            reasons = reasons
        )
        
        val eventId = sessionDao.insertThreatEvent(event)
        Timber.d("Threat event added to session $sessionId: level=$threatLevel, score=$threatScore, action=$actionTriggered")
        return eventId
    }
    
    fun getThreatEventsBySession(sessionId: Long): Flow<List<ThreatEventEntity>> {
        return sessionDao.getThreatEventsBySession(sessionId)
    }
    
    suspend fun getThreatEventsBySessionSync(sessionId: Long): List<ThreatEventEntity> {
        return sessionDao.getThreatEventsBySessionSync(sessionId)
    }
    
    suspend fun getSessionWithEvents(sessionId: Long): SessionWithEvents? {
        return sessionDao.getSessionWithEvents(sessionId)
    }
}

