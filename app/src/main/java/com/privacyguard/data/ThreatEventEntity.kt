package com.privacyguard.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity représentant un événement de menace détecté
 * Chaque ThreatAssessment qui déclenche une action est sauvegardé
 */
@Entity(
    tableName = "threat_events",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE // Supprimer les events si la session est supprimée
        )
    ],
    indices = [Index("sessionId")] // Index pour accélérer les requêtes par session
)
data class ThreatEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val sessionId: Long, // ID de la session parente
    
    val timestamp: Long, // timestamp en millisecondes
    
    val threatScore: Float, // score de menace [0.0 - 1.0]
    val threatLevel: String, // "NONE", "LOW", "MEDIUM", "HIGH", "CRITICAL"
    
    val actionTriggered: String?, // "SOFT_BLUR", "DECOY_SCREEN", "INSTANT_LOCK", "PANIC_MODE", null si aucune action
    
    // Contributions des capteurs (pour afficher les raisons)
    val cameraContribution: Float = 0f,
    val audioContribution: Float = 0f,
    val motionContribution: Float = 0f,
    val proximityContribution: Float = 0f,
    
    // Détails des raisons (JSON string ou texte descriptif)
    val reasons: String? = null // Ex: "Face détectée proche • Audio élevé • Mouvement brusque"
)

