package com.privacyguard.assessment

import com.privacyguard.assessment.models.SensorContributions
import com.privacyguard.assessment.models.SensorWeights
import com.privacyguard.sensors.*
import timber.log.Timber

/**
 * Calculateur de score de menace
 * 
 * Responsabilités :
 * - Normaliser les données de chaque capteur (0-1)
 * - Appliquer les pondérations
 * - Calculer le score final (0-100)
 */
class ThreatScorer {
    
    /**
     * Calcule le score de menace à partir des données des capteurs
     * 
     * @param cameraData Données du capteur caméra (peut être null)
     * @param audioData Données du capteur audio (peut être null)
     * @param motionData Données du capteur mouvement (peut être null)
     * @param proximityData Données du capteur proximité (peut être null)
     * @param weights Pondération des capteurs
     * @return Contributions de chaque capteur et score final
     */
    fun calculateScore(
        cameraData: CameraData?,
        audioData: AudioData?,
        motionData: MotionData?,
        proximityData: ProximityData?,
        weights: SensorWeights = SensorWeights.DEFAULT
    ): SensorContributions {
        // Debug: log des données reçues
        Timber.d("ThreatScorer INPUT: camera=${cameraData != null} (faces=${cameraData?.facesDetected}), " +
                "audio=${audioData != null} (dB=${audioData?.averageDecibels?.toInt()}), " +
                "motion=${motionData != null}, proximity=${proximityData != null}")
        
        // Normaliser chaque capteur (0-1)
        val cameraScore = normalizeCameraData(cameraData)
        val audioScore = normalizeAudioData(audioData)
        val motionScore = normalizeMotionData(motionData)
        val proximityScore = normalizeProximityData(proximityData)
        
        // Ajuster les poids si certains capteurs sont indisponibles
        val adjustedWeights = adjustWeightsForAvailability(
            weights = weights,
            cameraAvailable = cameraData != null,
            audioAvailable = audioData != null,
            motionAvailable = motionData != null,
            proximityAvailable = proximityData != null
        )
        
        val contributions = SensorContributions(
            cameraScore = cameraScore,
            audioScore = audioScore,
            motionScore = motionScore,
            proximityScore = proximityScore,
            cameraWeight = adjustedWeights.camera,
            audioWeight = adjustedWeights.audio,
            motionWeight = adjustedWeights.motion,
            proximityWeight = adjustedWeights.proximity
        )
        
        Timber.d("ThreatScorer: Score calculated - " +
                "Camera: ${(cameraScore * 100).toInt()}% (w=${(adjustedWeights.camera * 100).toInt()}%), " +
                "Audio: ${(audioScore * 100).toInt()}% (w=${(adjustedWeights.audio * 100).toInt()}%), " +
                "Motion: ${(motionScore * 100).toInt()}% (w=${(adjustedWeights.motion * 100).toInt()}%), " +
                "Proximity: ${(proximityScore * 100).toInt()}% (w=${(adjustedWeights.proximity * 100).toInt()}%), " +
                "Total: ${contributions.calculateWeightedScore()}")
        
        return contributions
    }
    
    /**
     * Normalise les données caméra en score 0-1
     * 
     * Facteurs considérés :
     * - Nombre de visages détectés
     * - Visages regardant l'écran
     * - Visages inconnus
     * - Distance à la caméra
     * 
     * LOGIQUE CORRIGÉE POUR STABILITÉ :
     * - 1 visage seul = LOW (probablement utilisateur) SAUF si pas trusté
     * - 2+ visages = MEDIUM à HIGH (menace réelle)
     * - Quelqu'un regarde l'écran (autre que l'utilisateur) = HIGH
     */
    private fun normalizeCameraData(data: CameraData?): Float {
        if (data == null) return 0f
        
        var score = 0f
        
        // Facteur 1 : Nombre de visages (0-0.5)
        // Logique plus claire : 1 visage = faible, 2+ = menace réelle
        val faceCountScore = when {
            data.facesDetected == 0 -> 0f
            data.facesDetected == 1 -> 0.05f   // 5% - probablement l'utilisateur seul
            data.facesDetected == 2 -> 0.30f   // 30% - 2 personnes = menace réelle
            data.facesDetected == 3 -> 0.45f   // 45% - 3 personnes = menace élevée
            else -> 0.50f                       // 50% - 4+ personnes = menace max
        }
        score += faceCountScore
        
        // Facteur 2 : Visages regardant l'écran (0-0.40)
        // CRITIQUE : Si quelqu'un d'autre regarde, c'est une vraie menace
        val lookingScore = when {
            data.facesLookingAtScreen == 0 -> 0f
            // 1 seul visage qui regarde = probablement l'utilisateur (sauf si trusté ailleurs)
            data.facesLookingAtScreen == 1 && data.facesDetected == 1 -> 0.02f
            // 1 personne regarde parmi plusieurs = MENACE !
            data.facesLookingAtScreen >= 1 && data.facesDetected >= 2 -> 0.35f
            // Plusieurs regardent = MENACE CRITIQUE
            else -> 0.40f
        }
        score += lookingScore
        
        // Facteur 3 : Visages inconnus (0-0.10)
        // Bonus pour visages non reconnus
        val unknownScore = (data.unknownFacesCount.coerceAtMost(3) / 3f) * 0.10f
        score += unknownScore
        
        // Appliquer la confiance du capteur
        val finalScore = (score * data.confidence).coerceIn(0f, 1f)
        
        Timber.d("CameraScore DETAIL: faces=${data.facesDetected}, looking=${data.facesLookingAtScreen}, " +
                "unknown=${data.unknownFacesCount} → faceScore=${"%.2f".format(faceCountScore)}, " +
                "lookingScore=${"%.2f".format(lookingScore)}, unknownScore=${"%.2f".format(unknownScore)} " +
                "→ FINAL=${"%.2f".format(finalScore)} (${(finalScore * 100).toInt()}%)")
        
        return finalScore
    }
    
    /**
     * Normalise les données audio en score 0-1
     * 
     * Facteurs considérés :
     * - Niveau de décibels
     * - Détection de parole
     * 
     * LOGIQUE CORRIGÉE :
     * - Bruit ambiant normal (< 60dB) = SAFE
     * - Conversation/musique (60-80dB) = MONITORING
     * - Bruit très fort (> 80dB) = THREAT
     * - Parole détectée = BONUS menace
     */
    private fun normalizeAudioData(data: AudioData?): Float {
        if (data == null) return 0f
        
        var score = 0f
        
        // Facteur 1 : Niveau sonore (0-0.50)
        // Échelle révisée pour être plus stable
        val decibelScore = when {
            data.averageDecibels < 40f -> 0f        // Très calme - SAFE
            data.averageDecibels < 55f -> 0.05f     // Calme - SAFE
            data.averageDecibels < 65f -> 0.15f     // Normal/léger - MONITORING
            data.averageDecibels < 75f -> 0.28f     // Conversation/musique modérée
            data.averageDecibels < 85f -> 0.40f     // Musique forte/conversation animée
            else -> 0.50f                            // Très bruyant - THREAT
        }
        score += decibelScore
        
        // Facteur 2 : Parole détectée (0-0.30)
        // Parole = présence humaine = menace potentielle
        if (data.isSpeechDetected) {
            score += 0.30f
        }
        
        // Appliquer la confiance du capteur
        val finalScore = (score * data.confidence).coerceIn(0f, 1f)
        
        Timber.d("AudioScore DETAIL: dB=${data.averageDecibels.toInt()}, speech=${data.isSpeechDetected} " +
                "→ dbScore=${"%.2f".format(decibelScore)}, speechBonus=${if (data.isSpeechDetected) "0.30" else "0.00"} " +
                "→ FINAL=${"%.2f".format(finalScore)} (${(finalScore * 100).toInt()}%)")
        
        return finalScore
    }
    
    /**
     * Normalise les données mouvement en score 0-1
     * 
     * Facteurs considérés :
     * - Intensité du mouvement
     * - Mouvement détecté
     */
    private fun normalizeMotionData(data: MotionData?): Float {
        if (data == null) return 0f
        
        var score = 0f
        
        // Facteur 1 : Mouvement en cours (0-0.3)
        if (data.isMoving) {
            score += 0.3f
        }
        
        // Facteur 2 : Intensité du mouvement (0-0.5)
        // movementIntensity est déjà normalisé (0-1)
        score += data.movementIntensity * 0.5f
        
        // Facteur 3 : Magnitude de l'accélération (0-0.2)
        // Accélération > 15 m/s² = mouvement brusque (quelqu'un attrape le téléphone)
        val magnitudeScore = when {
            data.magnitude < 10f -> 0f       // Normal (gravité ~9.8)
            data.magnitude < 15f -> 0.1f     // Léger mouvement
            data.magnitude < 20f -> 0.15f    // Mouvement notable
            else -> 0.2f                     // Mouvement brusque
        }
        score += magnitudeScore
        
        // Appliquer la confiance du capteur
        return (score * data.confidence).coerceIn(0f, 1f)
    }
    
    /**
     * Normalise les données proximité en score 0-1
     * 
     * Facteurs considérés :
     * - Objet proche détecté
     * - Distance (si capteur non-binaire)
     */
    private fun normalizeProximityData(data: ProximityData?): Float {
        if (data == null) return 0f
        
        var score = 0f
        
        // Facteur principal : objet proche
        if (data.isNear) {
            // Beaucoup de capteurs de proximité sont binaires (proche/loin)
            // Si proche, c'est potentiellement une main devant l'écran
            score = 0.7f
            
            // Bonus si distance disponible et très proche
            if (data.distance < 1f && data.maxRange > 5f) {
                score = 0.9f  // Très proche
            }
        } else {
            // Pas d'objet proche = faible score
            score = 0f
        }
        
        // Appliquer la confiance du capteur
        return (score * data.confidence).coerceIn(0f, 1f)
    }
    
    /**
     * Ajuste les pondérations si certains capteurs sont indisponibles
     * Redistribue les poids des capteurs manquants aux autres
     */
    private fun adjustWeightsForAvailability(
        weights: SensorWeights,
        cameraAvailable: Boolean,
        audioAvailable: Boolean,
        motionAvailable: Boolean,
        proximityAvailable: Boolean
    ): SensorWeights {
        val availableCount = listOf(
            cameraAvailable,
            audioAvailable,
            motionAvailable,
            proximityAvailable
        ).count { it }
        
        // Si tous disponibles, utiliser les poids d'origine
        if (availableCount == 4) return weights
        
        // Si aucun disponible, retourner zéros
        if (availableCount == 0) return SensorWeights(0f, 0f, 0f, 0f)
        
        // Calculer le poids total disponible
        var totalAvailableWeight = 0f
        if (cameraAvailable) totalAvailableWeight += weights.camera
        if (audioAvailable) totalAvailableWeight += weights.audio
        if (motionAvailable) totalAvailableWeight += weights.motion
        if (proximityAvailable) totalAvailableWeight += weights.proximity
        
        // Redistribuer proportionnellement
        val scaleFactor = 1f / totalAvailableWeight
        
        return SensorWeights(
            camera = if (cameraAvailable) weights.camera * scaleFactor else 0f,
            audio = if (audioAvailable) weights.audio * scaleFactor else 0f,
            motion = if (motionAvailable) weights.motion * scaleFactor else 0f,
            proximity = if (proximityAvailable) weights.proximity * scaleFactor else 0f
        )
    }
}


