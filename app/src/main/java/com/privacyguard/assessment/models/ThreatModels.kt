package com.privacyguard.assessment.models

import com.privacyguard.sensors.ThreatLevel

/**
 * Modèles de données pour l'évaluation des menaces
 * 
 * Ces classes représentent les résultats de la fusion des capteurs
 * et les décisions de protection.
 */

/**
 * Mode de protection de l'application
 * Chaque mode a un seuil de déclenchement différent
 */
enum class ProtectionMode(val threshold: Int, val description: String) {
    PARANOIA(20, "Très sensible - Moindre mouvement détecté"),
    BALANCED(50, "Équilibré - Protection raisonnable"),
    DISCRETE(75, "Discret - Uniquement menaces directes"),
    TRUST_ZONE(95, "Zone de confiance - Presque désactivé")
}

/**
 * Pondération des capteurs pour le calcul du score de menace
 */
data class SensorWeights(
    val camera: Float = 0.40f,      // 40% - Capteur principal (visages)
    val audio: Float = 0.30f,       // 30% - Sons suspects
    val motion: Float = 0.20f,      // 20% - Mouvements brusques
    val proximity: Float = 0.10f    // 10% - Objets proches
) {
    init {
        // Vérifier que les poids totalisent 100%
        val total = camera + audio + motion + proximity
        require(total in 0.99f..1.01f) { 
            "Les poids doivent totaliser 1.0, actuel: $total" 
        }
    }
    
    companion object {
        /** Pondération par défaut (Mode Discret) */
        val DEFAULT = SensorWeights()
        
        /** Pondération pour environnement bruyant (réduire audio) */
        val NOISY_ENVIRONMENT = SensorWeights(
            camera = 0.50f,
            audio = 0.15f,
            motion = 0.25f,
            proximity = 0.10f
        )
        
        /** Pondération pour faible luminosité (réduire caméra) */
        val LOW_LIGHT = SensorWeights(
            camera = 0.20f,
            audio = 0.45f,
            motion = 0.25f,
            proximity = 0.10f
        )
        
        /** Pondération Mode Paranoïa (tous les capteurs importants) */
        val PARANOIA = SensorWeights(
            camera = 0.35f,
            audio = 0.30f,
            motion = 0.25f,
            proximity = 0.10f
        )
    }
}

/**
 * Résultat de l'évaluation des menaces
 */
data class ThreatAssessment(
    val timestamp: Long,
    val threatScore: Int,           // Score de 0 à 100
    val threatLevel: ThreatLevel,
    val confidence: Float,          // Confiance globale (0.0 à 1.0)
    val shouldTriggerProtection: Boolean,
    val recommendedAction: ProtectionAction,
    val triggerReasons: List<String>,
    val sensorContributions: SensorContributions
)

/**
 * Contribution de chaque capteur au score final
 */
data class SensorContributions(
    val cameraScore: Float = 0f,     // Score normalisé caméra (0-1)
    val audioScore: Float = 0f,      // Score normalisé audio (0-1)
    val motionScore: Float = 0f,     // Score normalisé mouvement (0-1)
    val proximityScore: Float = 0f,  // Score normalisé proximité (0-1)
    val cameraWeight: Float = 0f,
    val audioWeight: Float = 0f,
    val motionWeight: Float = 0f,
    val proximityWeight: Float = 0f
) {
    /**
     * Calcule le score pondéré total (0-100)
     */
    fun calculateWeightedScore(): Int {
        val weighted = (cameraScore * cameraWeight +
                       audioScore * audioWeight +
                       motionScore * motionWeight +
                       proximityScore * proximityWeight)
        return (weighted * 100).toInt().coerceIn(0, 100)
    }
}

/**
 * Action de protection recommandée
 */
enum class ProtectionAction(val priority: Int, val description: String) {
    NONE(0, "Aucune action"),
    SOFT_BLUR(1, "Flou doux progressif"),
    DECOY_SCREEN(2, "Écran leurre"),
    INSTANT_LOCK(3, "Verrouillage instantané"),
    PANIC_MODE(4, "Mode panique")
}

/**
 * Contexte d'évaluation (pour adaptation dynamique)
 */
data class AssessmentContext(
    val currentMode: ProtectionMode = ProtectionMode.DISCRETE,
    val ambientNoiseLevel: Float = 0f,    // 0-1 (niveau de bruit ambiant)
    val lightLevel: Float = 1f,           // 0-1 (luminosité)
    val isInTrustZone: Boolean = false,
    val lastThreatTime: Long? = null,     // Timestamp dernière menace
    val consecutiveThreatCount: Int = 0   // Nombre de menaces consécutives
)

/**
 * Configuration de l'évaluateur de menaces
 */
data class ThreatAssessmentConfig(
    val protectionMode: ProtectionMode = ProtectionMode.DISCRETE,
    val sensorWeights: SensorWeights = SensorWeights.DEFAULT,
    val minConfidenceThreshold: Float = 0.3f,   // Confiance minimale pour considérer
    val debounceTimeMs: Long = 500,             // Anti-rebond entre déclenchements
    val consecutiveThreatsBeforeAction: Int = 2 // Nombre de menaces avant action
)


