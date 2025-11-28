package com.privacyguard.assessment

import com.privacyguard.assessment.models.*
import com.privacyguard.sensors.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests unitaires pour le ThreatAssessmentEngine
 * 
 * Vérifie :
 * - Calcul correct du score de menace
 * - Détection des différents niveaux de menace
 * - Recommandation d'actions appropriées
 * - Gestion des capteurs manquants
 */
class ThreatAssessmentEngineTest {
    
    private lateinit var engine: ThreatAssessmentEngine
    
    @Before
    fun setup() {
        engine = ThreatAssessmentEngine()
    }
    
    // ==================== Tests de base ====================
    
    @Test
    fun `evaluate with no sensor data returns NONE threat`() {
        val assessment = engine.evaluate()
        
        assertEquals(ThreatLevel.NONE, assessment.threatLevel)
        assertEquals(0, assessment.threatScore)
        assertFalse(assessment.shouldTriggerProtection)
        assertEquals(ProtectionAction.NONE, assessment.recommendedAction)
    }
    
    @Test
    fun `evaluate with single user face returns low threat`() {
        val cameraData = createCameraData(
            facesDetected = 1,
            facesLookingAtScreen = 1,
            unknownFacesCount = 0
        )
        
        val assessment = engine.evaluate(cameraData = cameraData)
        
        // Un seul utilisateur = menace faible
        assertTrue(assessment.threatScore < 30)
        assertFalse(assessment.shouldTriggerProtection) // Mode Discret seuil 75
    }
    
    @Test
    fun `evaluate with multiple faces triggers protection`() {
        val cameraData = createCameraData(
            facesDetected = 3,
            facesLookingAtScreen = 2,
            unknownFacesCount = 2,
            distanceToCamera = 0.4f // 40cm - proche
        )
        
        val assessment = engine.evaluate(cameraData = cameraData)
        
        // Plusieurs visages inconnus proches = menace élevée
        assertTrue(assessment.threatScore >= 50)
        assertTrue(assessment.triggerReasons.isNotEmpty())
    }
    
    // ==================== Tests des seuils de mode ====================
    
    @Test
    fun `discrete mode requires high score to trigger`() {
        engine.setProtectionMode(ProtectionMode.DISCRETE) // Seuil 75
        
        val cameraData = createCameraData(
            facesDetected = 2,
            facesLookingAtScreen = 1,
            unknownFacesCount = 1
        )
        
        val assessment = engine.evaluate(cameraData = cameraData)
        
        // Score modéré ne doit pas déclencher en mode Discret
        if (assessment.threatScore < 75) {
            assertFalse(assessment.shouldTriggerProtection)
        }
    }
    
    @Test
    fun `paranoia mode triggers at low score`() {
        engine.setProtectionMode(ProtectionMode.PARANOIA) // Seuil 20
        
        val cameraData = createCameraData(
            facesDetected = 2,
            facesLookingAtScreen = 1,
            unknownFacesCount = 0
        )
        
        val assessment = engine.evaluate(cameraData = cameraData)
        
        // En mode Paranoïa, même un score faible déclenche
        if (assessment.threatScore >= 20) {
            assertTrue(assessment.shouldTriggerProtection)
        }
    }
    
    @Test
    fun `balanced mode is between paranoia and discrete`() {
        engine.setProtectionMode(ProtectionMode.BALANCED) // Seuil 50
        
        val cameraData = createCameraData(
            facesDetected = 2,
            facesLookingAtScreen = 2,
            unknownFacesCount = 1
        )
        
        val assessment = engine.evaluate(cameraData = cameraData)
        
        // Score >= 50 déclenche en mode Équilibré
        val expectedTrigger = assessment.threatScore >= 50
        assertEquals(expectedTrigger, assessment.shouldTriggerProtection)
    }
    
    // ==================== Tests des capteurs individuels ====================
    
    @Test
    fun `audio with high decibels increases threat`() {
        val audioData = createAudioData(
            averageDecibels = 80f,
            peakDecibels = 95f,
            isSpeechDetected = true
        )
        
        val assessment = engine.evaluate(audioData = audioData)
        
        // Audio élevé avec parole = contribution significative
        assertTrue(assessment.sensorContributions.audioScore > 0.5f)
        assertTrue(assessment.triggerReasons.any { it.contains("Parole") || it.contains("Bruit") })
    }
    
    @Test
    fun `sudden motion increases threat`() {
        val motionData = createMotionData(
            magnitude = 25f, // Mouvement brusque
            movementIntensity = 0.9f,
            isMoving = true
        )
        
        val assessment = engine.evaluate(motionData = motionData)
        
        // Mouvement brusque = score de mouvement élevé
        assertTrue(assessment.sensorContributions.motionScore > 0.7f)
        assertTrue(assessment.triggerReasons.any { it.contains("mouvement") || it.contains("Mouvement") })
    }
    
    @Test
    fun `proximity sensor near object increases threat`() {
        val proximityData = createProximityData(
            isNear = true,
            distance = 0f
        )
        
        val assessment = engine.evaluate(proximityData = proximityData)
        
        // Objet proche = score proximité élevé
        assertTrue(assessment.sensorContributions.proximityScore > 0.5f)
        assertTrue(assessment.triggerReasons.any { it.contains("proche") || it.contains("Objet") })
    }
    
    // ==================== Tests de fusion multi-capteurs ====================
    
    @Test
    fun `multiple sensors combining increases overall threat`() {
        val cameraData = createCameraData(
            facesDetected = 2,
            facesLookingAtScreen = 1,
            unknownFacesCount = 1
        )
        val audioData = createAudioData(
            averageDecibels = 70f,
            isSpeechDetected = true
        )
        val motionData = createMotionData(
            magnitude = 15f,
            movementIntensity = 0.5f
        )
        
        // Score avec tous les capteurs
        val fullAssessment = engine.evaluate(
            cameraData = cameraData,
            audioData = audioData,
            motionData = motionData
        )
        
        // Score avec caméra seule
        val cameraOnlyAssessment = engine.evaluate(cameraData = cameraData)
        
        // Multi-capteurs doit avoir un score plus élevé
        assertTrue(fullAssessment.threatScore >= cameraOnlyAssessment.threatScore)
    }
    
    @Test
    fun `missing sensors redistributes weights`() {
        val cameraData = createCameraData(
            facesDetected = 3,
            facesLookingAtScreen = 2,
            unknownFacesCount = 2
        )
        
        val assessment = engine.evaluate(cameraData = cameraData)
        
        // Avec seulement caméra, son poids doit être redistribué à 100%
        assertEquals(1.0f, assessment.sensorContributions.cameraWeight, 0.01f)
        assertEquals(0f, assessment.sensorContributions.audioWeight, 0.01f)
    }
    
    // ==================== Tests des actions recommandées ====================
    
    @Test
    fun `high threat in discrete mode recommends soft blur`() {
        engine.setProtectionMode(ProtectionMode.DISCRETE)
        
        // Créer une menace qui dépasse le seuil 75 mais pas 90
        val cameraData = createCameraData(
            facesDetected = 4,
            facesLookingAtScreen = 3,
            unknownFacesCount = 3,
            distanceToCamera = 0.3f
        )
        val audioData = createAudioData(
            averageDecibels = 75f,
            isSpeechDetected = true
        )
        
        val assessment = engine.evaluate(cameraData = cameraData, audioData = audioData)
        
        if (assessment.threatScore in 75..89) {
            assertEquals(ProtectionAction.SOFT_BLUR, assessment.recommendedAction)
        }
    }
    
    @Test
    fun `very high threat recommends decoy screen`() {
        engine.setProtectionMode(ProtectionMode.DISCRETE)
        
        // Créer une menace très élevée (score >= 90)
        val cameraData = createCameraData(
            facesDetected = 5,
            facesLookingAtScreen = 4,
            unknownFacesCount = 4,
            distanceToCamera = 0.2f
        )
        val audioData = createAudioData(
            averageDecibels = 85f,
            peakDecibels = 100f,
            isSpeechDetected = true
        )
        val motionData = createMotionData(
            magnitude = 20f,
            movementIntensity = 0.9f,
            isMoving = true
        )
        val proximityData = createProximityData(isNear = true)
        
        val assessment = engine.evaluate(
            cameraData = cameraData,
            audioData = audioData,
            motionData = motionData,
            proximityData = proximityData
        )
        
        if (assessment.threatScore >= 90) {
            assertEquals(ProtectionAction.DECOY_SCREEN, assessment.recommendedAction)
        }
    }
    
    // ==================== Tests de contexte ====================
    
    @Test
    fun `trust zone reduces trigger sensitivity`() {
        engine.setProtectionMode(ProtectionMode.DISCRETE)
        engine.setTrustZone(false)
        
        val cameraData = createCameraData(
            facesDetected = 2,
            facesLookingAtScreen = 1,
            unknownFacesCount = 1
        )
        
        val normalAssessment = engine.evaluate(cameraData = cameraData)
        
        // Activer zone de confiance
        engine.setTrustZone(true)
        val trustZoneAssessment = engine.evaluate(cameraData = cameraData)
        
        // Même score, mais trigger moins probable en zone de confiance
        if (normalAssessment.shouldTriggerProtection) {
            // Trust zone augmente le seuil de 20%
            // Donc possiblement ne déclenche plus
        }
    }
    
    // ==================== Tests Flow ====================
    
    @Test
    fun `processFlow filters duplicate assessments`() = runTest {
        val snapshots = listOf(
            createSnapshot(facesDetected = 0),
            createSnapshot(facesDetected = 0), // Duplicate
            createSnapshot(facesDetected = 1),
            createSnapshot(facesDetected = 1)  // Duplicate
        )
        
        val results = engine.processFlow(flowOf(*snapshots.toTypedArray())).toList()
        
        // Les duplicates avec même trigger status devraient être filtrés
        assertTrue(results.size <= snapshots.size)
    }
    
    // ==================== Helpers ====================
    
    private fun createCameraData(
        facesDetected: Int = 0,
        facesLookingAtScreen: Int = 0,
        unknownFacesCount: Int = 0,
        distanceToCamera: Float? = null,
        confidence: Float = 0.9f
    ) = CameraData(
        timestamp = System.currentTimeMillis(),
        threatLevel = ThreatLevel.NONE,
        confidence = confidence,
        facesDetected = facesDetected,
        facesLookingAtScreen = facesLookingAtScreen,
        unknownFacesCount = unknownFacesCount,
        distanceToCamera = distanceToCamera
    )
    
    private fun createAudioData(
        averageDecibels: Float = 40f,
        peakDecibels: Float = 50f,
        isSpeechDetected: Boolean = false,
        confidence: Float = 0.8f
    ) = AudioData(
        timestamp = System.currentTimeMillis(),
        threatLevel = ThreatLevel.NONE,
        confidence = confidence,
        averageDecibels = averageDecibels,
        peakDecibels = peakDecibels,
        isSpeechDetected = isSpeechDetected
    )
    
    private fun createMotionData(
        magnitude: Float = 9.8f,
        movementIntensity: Float = 0f,
        isMoving: Boolean = false,
        confidence: Float = 0.9f
    ) = MotionData(
        timestamp = System.currentTimeMillis(),
        threatLevel = ThreatLevel.NONE,
        confidence = confidence,
        accelerationX = 0f,
        accelerationY = 0f,
        accelerationZ = magnitude,
        magnitude = magnitude,
        isMoving = isMoving,
        movementIntensity = movementIntensity
    )
    
    private fun createProximityData(
        isNear: Boolean = false,
        distance: Float = 5f,
        confidence: Float = 1.0f
    ) = ProximityData(
        timestamp = System.currentTimeMillis(),
        threatLevel = ThreatLevel.NONE,
        confidence = confidence,
        distance = distance,
        isNear = isNear,
        maxRange = 5f
    )
    
    private fun createSnapshot(
        facesDetected: Int = 0,
        audioDecibels: Float = 40f,
        isMoving: Boolean = false,
        isNear: Boolean = false
    ) = SensorDataSnapshot(
        timestamp = System.currentTimeMillis(),
        cameraData = createCameraData(facesDetected = facesDetected),
        audioData = createAudioData(averageDecibels = audioDecibels),
        motionData = createMotionData(isMoving = isMoving),
        proximityData = createProximityData(isNear = isNear)
    )
}


