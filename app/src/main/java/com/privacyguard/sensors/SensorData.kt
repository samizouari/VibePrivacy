package com.privacyguard.sensors

/**
 * Modèles de données pour les capteurs
 * 
 * Ces classes représentent les données collectées par chaque capteur.
 * Elles sont utilisées pour la fusion de capteurs et l'évaluation des menaces.
 */

/**
 * Niveau de menace détecté par un capteur
 */
enum class ThreatLevel {
    NONE,       // Aucune menace
    LOW,        // Menace faible
    MEDIUM,     // Menace moyenne
    HIGH,       // Menace élevée
    CRITICAL    // Menace critique
}

/**
 * Classe de base pour toutes les données de capteurs
 */
sealed class SensorData {
    abstract val timestamp: Long
    abstract val threatLevel: ThreatLevel
    abstract val confidence: Float // 0.0 to 1.0
}

/**
 * Données du capteur caméra (ML Kit Face Detection)
 */
data class CameraData(
    override val timestamp: Long,
    override val threatLevel: ThreatLevel,
    override val confidence: Float,
    val facesDetected: Int,
    val facesLookingAtScreen: Int,
    val unknownFacesCount: Int,
    val distanceToCamera: Float? = null // En mètres (si disponible)
) : SensorData()

/**
 * Données du capteur audio
 */
data class AudioData(
    override val timestamp: Long,
    override val threatLevel: ThreatLevel,
    override val confidence: Float,
    val averageDecibels: Float,
    val peakDecibels: Float,
    val isSpeechDetected: Boolean = false
) : SensorData()

/**
 * Données du capteur de mouvement (accéléromètre)
 */
data class MotionData(
    override val timestamp: Long,
    override val threatLevel: ThreatLevel,
    override val confidence: Float,
    val accelerationX: Float,
    val accelerationY: Float,
    val accelerationZ: Float,
    val magnitude: Float, // Magnitude du vecteur d'accélération
    val isMoving: Boolean,
    val movementIntensity: Float // 0.0 à 1.0
) : SensorData()

/**
 * Données du capteur de proximité
 */
data class ProximityData(
    override val timestamp: Long,
    override val threatLevel: ThreatLevel,
    override val confidence: Float,
    val distance: Float, // Distance en cm
    val isNear: Boolean, // Objet proche détecté
    val maxRange: Float // Portée maximale du capteur
) : SensorData()

/**
 * Événement de détection consolidé (fusion de tous les capteurs)
 */
data class DetectionEvent(
    val id: String,
    val timestamp: Long,
    val overallThreatLevel: ThreatLevel,
    val overallConfidence: Float,
    val cameraData: CameraData?,
    val audioData: AudioData?,
    val motionData: MotionData?,
    val proximityData: ProximityData?,
    val triggerAction: Boolean = false, // Si true, déclencher l'overlay de protection
    val actionTaken: String? = null // Action prise (ex: "overlay_activated", "photo_taken")
)

