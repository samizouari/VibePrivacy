package com.privacyguard.protection

import com.privacyguard.assessment.models.ProtectionAction
import com.privacyguard.assessment.models.ProtectionMode
import com.privacyguard.assessment.models.ThreatAssessment
import timber.log.Timber

/**
 * Stratégie de protection basée sur le nombre de visages et le mode
 * 
 * Logique Jour 8 (Optimisée) :
 * - DISCRETE : Flou si 3+ visages OU score > 70
 * - BALANCED : Flou si 2+ visages OU score > 40, Décoy si 3+ visages OU score > 50, Lock si score > 55
 * - PARANOIA : Flou si 1+ visage OU score > 30, Décoy si 2+ visages OU score > 35, Lock si score > 40
 * - TRUST_ZONE : Basé sur score uniquement (seuils élevés)
 */
object ProtectionStrategy {
    
    /**
     * Détermine l'action de protection basée sur visages + score + mode
     */
    fun determineProtectionAction(assessment: ThreatAssessment): ProtectionActionPlan {
        val faces = assessment.facesDetected
        val unknownFaces = assessment.unknownFacesCount
        val score = assessment.threatScore
        val mode = assessment.protectionMode
        
        Timber.d("ProtectionStrategy: faces=$faces, unknown=$unknownFaces, score=$score, mode=${mode.name}")
        
        // Déterminer les overlays à activer
        val shouldBlur: Boolean
        val shouldDecoy: Boolean
        val shouldLock: Boolean
        val shouldCapture: Boolean
        
        when (mode) {
            ProtectionMode.DISCRETE -> {
                // DISCRETE : Flou si 3+ visages OU score élevé
                shouldBlur = faces >= 3 || score > 70
                shouldDecoy = false
                shouldLock = false
                shouldCapture = false
                
                Timber.d("ProtectionStrategy [DISCRETE]: blur=$shouldBlur (faces >= 3 OR score > 70)")
            }
            
            ProtectionMode.BALANCED -> {
                // BALANCED : Réactif au score ET aux visages
                shouldBlur = faces >= 2 || score > 40
                shouldDecoy = faces >= 3 || score > 50
                shouldLock = score > 55 || (faces >= 3 && score > 45)
                shouldCapture = unknownFaces >= 2
                
                Timber.d("ProtectionStrategy [BALANCED]: blur=$shouldBlur (faces >= 2 OR score > 40), decoy=$shouldDecoy (faces >= 3 OR score > 50), lock=$shouldLock (score > 55), capture=$shouldCapture (unknown >= 2)")
            }
            
            ProtectionMode.PARANOIA -> {
                // PARANOIA : Très strict, réagit rapidement
                shouldBlur = faces >= 1 || score > 30
                shouldDecoy = faces >= 2 || score > 35
                shouldLock = score > 40 || (faces >= 2 && score > 30)
                shouldCapture = unknownFaces >= 1
                
                Timber.d("ProtectionStrategy [PARANOIA]: blur=$shouldBlur (faces >= 1 OR score > 30), decoy=$shouldDecoy (faces >= 2 OR score > 35), lock=$shouldLock (score > 40), capture=$shouldCapture (unknown >= 1)")
            }
            
            ProtectionMode.TRUST_ZONE -> {
                // TRUST_ZONE : Basé uniquement sur le score (seuil très élevé)
                shouldBlur = score > 80
                shouldDecoy = score > 90
                shouldLock = score > 95
                shouldCapture = false
                
                Timber.d("ProtectionStrategy [TRUST_ZONE]: blur=$shouldBlur (score > 80), decoy=$shouldDecoy (score > 90), lock=$shouldLock (score > 95)")
            }
        }
        
        // Déterminer l'action principale
        val mainAction = when {
            shouldLock -> ProtectionAction.INSTANT_LOCK
            shouldDecoy -> ProtectionAction.DECOY_SCREEN
            shouldBlur -> ProtectionAction.SOFT_BLUR
            else -> ProtectionAction.NONE
        }
        
        return ProtectionActionPlan(
            mainAction = mainAction,
            shouldBlur = shouldBlur,
            shouldDecoy = shouldDecoy,
            shouldLock = shouldLock,
            shouldCapture = shouldCapture,
            blurIntensity = calculateBlurIntensity(faces, score)
        )
    }
    
    /**
     * Calcule l'intensité du flou basée sur faces + score
     */
    private fun calculateBlurIntensity(faces: Int, score: Int): Float {
        // Plus de visages ou score élevé = plus de flou
        val facesFactor = (faces.coerceAtMost(10) / 10f) * 0.5f
        val scoreFactor = (score / 100f) * 0.5f
        return (facesFactor + scoreFactor).coerceIn(0.3f, 1.0f)
    }
    
    /**
     * Détermine l'état de l'indicateur basé sur faces + score
     */
    fun determineIndicatorState(assessment: ThreatAssessment): IndicatorState {
        val faces = assessment.facesDetected
        val unknownFaces = assessment.unknownFacesCount
        val score = assessment.threatScore
        
        return when {
            // THREAT : 2+ visages inconnus OU score élevé
            unknownFaces >= 2 -> IndicatorState.THREAT
            score >= 65 -> IndicatorState.THREAT
            
            // MONITORING : 1 visage inconnu OU score moyen
            unknownFaces >= 1 -> IndicatorState.MONITORING
            faces >= 2 -> IndicatorState.MONITORING
            score >= 35 -> IndicatorState.MONITORING
            
            // SAFE : Rien de suspect
            else -> IndicatorState.SAFE
        }
    }
}

/**
 * Plan d'action de protection
 */
data class ProtectionActionPlan(
    val mainAction: ProtectionAction,
    val shouldBlur: Boolean,
    val shouldDecoy: Boolean,
    val shouldLock: Boolean,
    val shouldCapture: Boolean,
    val blurIntensity: Float = 0.5f
)

