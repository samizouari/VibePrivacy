package com.privacyguard.assessment

import com.privacyguard.assessment.models.*
import com.privacyguard.sensors.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

/**
 * Fusion des données de capteurs
 * 
 * Responsabilités :
 * - Combiner les données de tous les capteurs
 * - Calculer le niveau de menace global
 * - Identifier les raisons de déclenchement
 */
class SensorDataFusion(
    private val threatScorer: ThreatScorer = ThreatScorer()
) {
    
    /**
     * Fusionne les données des capteurs et calcule l'évaluation de menace
     * 
     * @param snapshot Données combinées des capteurs
     * @param config Configuration de l'évaluation
     * @param context Contexte actuel (mode, environnement, etc.)
     * @return Évaluation de menace complète
     */
    fun evaluate(
        snapshot: SensorDataSnapshot,
        config: ThreatAssessmentConfig = ThreatAssessmentConfig(),
        context: AssessmentContext = AssessmentContext()
    ): ThreatAssessment {
        
        // 1. Calculer les scores de chaque capteur avec pondération
        val contributions = threatScorer.calculateScore(
            cameraData = snapshot.cameraData,
            audioData = snapshot.audioData,
            motionData = snapshot.motionData,
            proximityData = snapshot.proximityData,
            weights = getWeightsForContext(context, config.sensorWeights)
        )
        
        // 2. Calculer le score final (0-100)
        val threatScore = contributions.calculateWeightedScore()
        
        // 3. Déterminer le niveau de menace
        val threatLevel = scoreToThreatLevel(threatScore)
        
        // 4. Calculer la confiance globale
        val confidence = calculateOverallConfidence(snapshot)
        
        // 5. Identifier les raisons de déclenchement
        val triggerReasons = identifyTriggerReasons(snapshot, contributions, config)
        
        // 6. Déterminer si protection doit être déclenchée
        val shouldTrigger = shouldTriggerProtection(
            threatScore = threatScore,
            confidence = confidence,
            config = config,
            context = context
        )
        
        // 7. Recommander une action
        val recommendedAction = determineAction(threatScore, context)
        
        val assessment = ThreatAssessment(
            timestamp = snapshot.timestamp,
            threatScore = threatScore,
            threatLevel = threatLevel,
            confidence = confidence,
            shouldTriggerProtection = shouldTrigger,
            recommendedAction = recommendedAction,
            triggerReasons = triggerReasons,
            sensorContributions = contributions
        )
        
        Timber.i("SensorDataFusion: Assessment complete - " +
                "Score=$threatScore, Level=$threatLevel, Trigger=$shouldTrigger, " +
                "Action=$recommendedAction")
        
        return assessment
    }
    
    /**
     * Obtient les pondérations adaptées au contexte
     */
    private fun getWeightsForContext(
        context: AssessmentContext,
        defaultWeights: SensorWeights
    ): SensorWeights {
        return when {
            // Environnement bruyant : réduire poids audio
            context.ambientNoiseLevel > 0.7f -> SensorWeights.NOISY_ENVIRONMENT
            // Faible luminosité : réduire poids caméra
            context.lightLevel < 0.3f -> SensorWeights.LOW_LIGHT
            // Mode Paranoïa : pondération spéciale
            context.currentMode == ProtectionMode.PARANOIA -> SensorWeights.PARANOIA
            // Par défaut
            else -> defaultWeights
        }
    }
    
    /**
     * Convertit un score (0-100) en niveau de menace
     */
    private fun scoreToThreatLevel(score: Int): ThreatLevel {
        return when {
            score < 20 -> ThreatLevel.NONE
            score < 40 -> ThreatLevel.LOW
            score < 60 -> ThreatLevel.MEDIUM
            score < 80 -> ThreatLevel.HIGH
            else -> ThreatLevel.CRITICAL
        }
    }
    
    /**
     * Calcule la confiance globale à partir des capteurs disponibles
     */
    private fun calculateOverallConfidence(snapshot: SensorDataSnapshot): Float {
        val confidences = listOfNotNull(
            snapshot.cameraData?.confidence,
            snapshot.audioData?.confidence,
            snapshot.motionData?.confidence,
            snapshot.proximityData?.confidence
        )
        
        return if (confidences.isNotEmpty()) {
            confidences.average().toFloat()
        } else {
            0f
        }
    }
    
    /**
     * Identifie les raisons qui ont contribué au score de menace
     */
    private fun identifyTriggerReasons(
        snapshot: SensorDataSnapshot,
        contributions: SensorContributions,
        config: ThreatAssessmentConfig
    ): List<String> {
        val reasons = mutableListOf<String>()
        
        // Raisons liées à la caméra
        snapshot.cameraData?.let { camera ->
            if (camera.facesDetected > 1) {
                reasons.add("${camera.facesDetected} visages détectés")
            }
            if (camera.facesLookingAtScreen > 0 && camera.facesDetected > 1) {
                reasons.add("${camera.facesLookingAtScreen} personne(s) regardant l'écran")
            }
            if (camera.unknownFacesCount > 0) {
                reasons.add("${camera.unknownFacesCount} visage(s) inconnu(s)")
            }
            camera.distanceToCamera?.let { dist ->
                if (dist < 0.5f) {
                    reasons.add("Visage très proche (${(dist * 100).toInt()}cm)")
                }
            }
        }
        
        // Raisons liées à l'audio
        snapshot.audioData?.let { audio ->
            if (audio.isSpeechDetected) {
                reasons.add("Parole détectée")
            }
            if (audio.averageDecibels > 70f) {
                reasons.add("Bruit élevé (${audio.averageDecibels.toInt()}dB)")
            }
        }
        
        // Raisons liées au mouvement
        snapshot.motionData?.let { motion ->
            if (motion.magnitude > 15f) {
                reasons.add("Mouvement brusque détecté")
            }
            if (motion.movementIntensity > 0.7f) {
                reasons.add("Forte intensité de mouvement")
            }
        }
        
        // Raisons liées à la proximité
        snapshot.proximityData?.let { proximity ->
            if (proximity.isNear) {
                reasons.add("Objet proche de l'écran")
            }
        }
        
        // Contribution significative d'un capteur
        if (contributions.cameraScore > 0.5f) {
            reasons.add("Caméra: score élevé")
        }
        if (contributions.audioScore > 0.6f) {
            reasons.add("Audio: score élevé")
        }
        
        return reasons
    }
    
    /**
     * Détermine si la protection doit être déclenchée
     */
    private fun shouldTriggerProtection(
        threatScore: Int,
        confidence: Float,
        config: ThreatAssessmentConfig,
        context: AssessmentContext
    ): Boolean {
        // Vérifier le seuil du mode de protection
        val threshold = context.currentMode.threshold
        
        // Vérifier la confiance minimale
        if (confidence < config.minConfidenceThreshold) {
            Timber.d("SensorDataFusion: Confidence too low ($confidence < ${config.minConfidenceThreshold})")
            return false
        }
        
        // Zone de confiance = protection réduite
        if (context.isInTrustZone) {
            // En zone de confiance, augmenter le seuil de 20%
            val adjustedThreshold = (threshold * 1.2f).toInt().coerceAtMost(95)
            return threatScore >= adjustedThreshold
        }
        
        return threatScore >= threshold
    }
    
    /**
     * Détermine l'action de protection recommandée
     */
    private fun determineAction(
        threatScore: Int,
        context: AssessmentContext
    ): ProtectionAction {
        return when {
            // Mode Paranoïa : actions plus agressives
            context.currentMode == ProtectionMode.PARANOIA -> when {
                threatScore >= 60 -> ProtectionAction.INSTANT_LOCK
                threatScore >= 40 -> ProtectionAction.DECOY_SCREEN
                threatScore >= 20 -> ProtectionAction.SOFT_BLUR
                else -> ProtectionAction.NONE
            }
            
            // Mode Équilibré
            context.currentMode == ProtectionMode.BALANCED -> when {
                threatScore >= 80 -> ProtectionAction.INSTANT_LOCK
                threatScore >= 60 -> ProtectionAction.DECOY_SCREEN
                threatScore >= 50 -> ProtectionAction.SOFT_BLUR
                else -> ProtectionAction.NONE
            }
            
            // Mode Discret (par défaut pour MVP)
            else -> when {
                threatScore >= 90 -> ProtectionAction.DECOY_SCREEN
                threatScore >= 75 -> ProtectionAction.SOFT_BLUR
                else -> ProtectionAction.NONE
            }
        }
    }
}


