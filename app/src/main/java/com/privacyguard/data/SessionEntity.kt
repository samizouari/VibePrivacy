package com.privacyguard.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.privacyguard.assessment.models.ProtectionMode

/**
 * Entity représentant une session de protection
 * Une session commence quand l'utilisateur active la protection
 * et se termine quand il la désactive ou que l'app se ferme
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val protectionMode: String, // "DISCRETE", "BALANCED", "PARANOID"
    
    val startTime: Long, // timestamp en millisecondes
    val endTime: Long? = null, // null si la session est en cours
    
    val totalThreatsDetected: Int = 0,
    val totalThreatScore: Float = 0f, // somme de tous les scores
    val assessmentCount: Int = 0, // nombre total d'évaluations
    
    val avgThreatScore: Float = 0f, // score moyen de la session
    val maxThreatScore: Float = 0f, // score max atteint
    val maxThreatLevel: String = "NONE" // niveau max atteint ("NONE", "LOW", "MEDIUM", "HIGH", "CRITICAL")
) {
    val duration: Long
        get() = (endTime ?: System.currentTimeMillis()) - startTime
    
    val isActive: Boolean
        get() = endTime == null
}

