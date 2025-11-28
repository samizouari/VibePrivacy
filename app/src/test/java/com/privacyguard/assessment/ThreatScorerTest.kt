package com.privacyguard.assessment

import com.privacyguard.assessment.models.SensorWeights
import com.privacyguard.sensors.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests unitaires pour le ThreatScorer
 * 
 * Vérifie :
 * - Normalisation correcte des données de chaque capteur
 * - Calcul des scores pondérés
 * - Redistribution des poids pour capteurs manquants
 */
class ThreatScorerTest {
    
    private lateinit var scorer: ThreatScorer
    
    @Before
    fun setup() {
        scorer = ThreatScorer()
    }
    
    // ==================== Tests de normalisation Caméra ====================
    
    @Test
    fun `camera with no faces returns 0 score`() {
        val cameraData = createCameraData(facesDetected = 0)
        
        val contributions = scorer.calculateScore(
            cameraData = cameraData,
            audioData = null,
            motionData = null,
            proximityData = null
        )
        
        assertEquals(0f, contributions.cameraScore, 0.01f)
    }
    
    @Test
    fun `camera with single user returns low score`() {
        val cameraData = createCameraData(
            facesDetected = 1,
            facesLookingAtScreen = 1,
            unknownFacesCount = 0
        )
        
        val contributions = scorer.calculateScore(
            cameraData = cameraData,
            audioData = null,
            motionData = null,
            proximityData = null
        )
        
        // 1 visage = 0.1 + 1 regardant seul = 0.05 = ~0.15 max
        assertTrue(contributions.cameraScore < 0.2f)
    }
    
    @Test
    fun `camera with multiple unknown faces returns high score`() {
        val cameraData = createCameraData(
            facesDetected = 4,
            facesLookingAtScreen = 3,
            unknownFacesCount = 3,
            distanceToCamera = 0.3f // 30cm
        )
        
        val contributions = scorer.calculateScore(
            cameraData = cameraData,
            audioData = null,
            motionData = null,
            proximityData = null
        )
        
        // Score élevé attendu
        assertTrue(contributions.cameraScore > 0.6f)
    }
    
    // ==================== Tests de normalisation Audio ====================
    
    @Test
    fun `audio quiet environment returns low score`() {
        val audioData = createAudioData(averageDecibels = 35f)
        
        val contributions = scorer.calculateScore(
            cameraData = null,
            audioData = audioData,
            motionData = null,
            proximityData = null
        )
        
        assertEquals(0f, contributions.audioScore, 0.01f)
    }
    
    @Test
    fun `audio with speech detected increases score`() {
        val withoutSpeech = createAudioData(averageDecibels = 60f, isSpeechDetected = false)
        val withSpeech = createAudioData(averageDecibels = 60f, isSpeechDetected = true)
        
        val scoreWithout = scorer.calculateScore(
            cameraData = null,
            audioData = withoutSpeech,
            motionData = null,
            proximityData = null
        )
        
        val scoreWith = scorer.calculateScore(
            cameraData = null,
            audioData = withSpeech,
            motionData = null,
            proximityData = null
        )
        
        // Score avec parole doit être plus élevé
        assertTrue(scoreWith.audioScore > scoreWithout.audioScore)
    }
    
    @Test
    fun `audio with high decibels returns high score`() {
        val audioData = createAudioData(
            averageDecibels = 80f,
            peakDecibels = 95f,
            isSpeechDetected = true
        )
        
        val contributions = scorer.calculateScore(
            cameraData = null,
            audioData = audioData,
            motionData = null,
            proximityData = null
        )
        
        // Score élevé attendu
        assertTrue(contributions.audioScore > 0.7f)
    }
    
    // ==================== Tests de normalisation Mouvement ====================
    
    @Test
    fun `motion stationary returns low score`() {
        val motionData = createMotionData(
            magnitude = 9.8f, // Juste gravité
            movementIntensity = 0f,
            isMoving = false
        )
        
        val contributions = scorer.calculateScore(
            cameraData = null,
            audioData = null,
            motionData = motionData,
            proximityData = null
        )
        
        assertEquals(0f, contributions.motionScore, 0.01f)
    }
    
    @Test
    fun `motion sudden movement returns high score`() {
        val motionData = createMotionData(
            magnitude = 25f, // Mouvement brusque
            movementIntensity = 0.9f,
            isMoving = true
        )
        
        val contributions = scorer.calculateScore(
            cameraData = null,
            audioData = null,
            motionData = motionData,
            proximityData = null
        )
        
        // Score très élevé attendu
        assertTrue(contributions.motionScore > 0.8f)
    }
    
    // ==================== Tests de normalisation Proximité ====================
    
    @Test
    fun `proximity far returns 0 score`() {
        val proximityData = createProximityData(isNear = false, distance = 5f)
        
        val contributions = scorer.calculateScore(
            cameraData = null,
            audioData = null,
            motionData = null,
            proximityData = proximityData
        )
        
        assertEquals(0f, contributions.proximityScore, 0.01f)
    }
    
    @Test
    fun `proximity near returns high score`() {
        val proximityData = createProximityData(isNear = true, distance = 0f)
        
        val contributions = scorer.calculateScore(
            cameraData = null,
            audioData = null,
            motionData = null,
            proximityData = proximityData
        )
        
        assertTrue(contributions.proximityScore >= 0.7f)
    }
    
    // ==================== Tests de pondération ====================
    
    @Test
    fun `default weights total 100 percent`() {
        val weights = SensorWeights.DEFAULT
        val total = weights.camera + weights.audio + weights.motion + weights.proximity
        
        assertEquals(1.0f, total, 0.01f)
    }
    
    @Test
    fun `weights redistributed when sensors missing`() {
        val cameraData = createCameraData(facesDetected = 2)
        
        val contributions = scorer.calculateScore(
            cameraData = cameraData,
            audioData = null, // Missing
            motionData = null, // Missing
            proximityData = null // Missing
        )
        
        // Seule la caméra est disponible, donc son poids = 100%
        assertEquals(1.0f, contributions.cameraWeight, 0.01f)
        assertEquals(0f, contributions.audioWeight, 0.01f)
        assertEquals(0f, contributions.motionWeight, 0.01f)
        assertEquals(0f, contributions.proximityWeight, 0.01f)
    }
    
    @Test
    fun `weighted score calculation is correct`() {
        val cameraData = createCameraData(facesDetected = 3, facesLookingAtScreen = 2)
        val audioData = createAudioData(averageDecibels = 70f, isSpeechDetected = true)
        
        val contributions = scorer.calculateScore(
            cameraData = cameraData,
            audioData = audioData,
            motionData = null,
            proximityData = null
        )
        
        // Vérifier que le score final est dans les limites
        val finalScore = contributions.calculateWeightedScore()
        assertTrue(finalScore in 0..100)
        
        // Le score pondéré devrait correspondre au calcul manuel
        val expectedScore = (
            contributions.cameraScore * contributions.cameraWeight +
            contributions.audioScore * contributions.audioWeight
        ) * 100
        assertEquals(expectedScore.toInt(), finalScore, 5) // Tolérance de 5
    }
    
    // ==================== Tests de confiance ====================
    
    @Test
    fun `low confidence reduces score`() {
        val highConfidence = createCameraData(
            facesDetected = 2,
            facesLookingAtScreen = 1,
            confidence = 1.0f
        )
        val lowConfidence = createCameraData(
            facesDetected = 2,
            facesLookingAtScreen = 1,
            confidence = 0.3f
        )
        
        val highScore = scorer.calculateScore(
            cameraData = highConfidence,
            audioData = null,
            motionData = null,
            proximityData = null
        )
        
        val lowScore = scorer.calculateScore(
            cameraData = lowConfidence,
            audioData = null,
            motionData = null,
            proximityData = null
        )
        
        // Score avec faible confiance doit être plus bas
        assertTrue(lowScore.cameraScore < highScore.cameraScore)
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
}


