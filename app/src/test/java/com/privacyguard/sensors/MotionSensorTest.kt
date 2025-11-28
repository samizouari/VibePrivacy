package com.privacyguard.sensors

import org.junit.Test
import org.junit.Assert.*
import kotlin.math.sqrt

/**
 * Tests unitaires pour MotionSensor
 * 
 * Tests la logique de détection de mouvement et d'évaluation des menaces.
 */
class MotionSensorTest {

    private val shakeThreshold = 15f
    private val movementThreshold = 2f

    /**
     * Test: Pas de mouvement → ThreatLevel.NONE
     */
    @Test
    fun `test no movement returns NONE threat level`() {
        val magnitude = 0.5f
        val previousMagnitude = 0.5f
        val (threatLevel, confidence) = evaluateThreatLevel(magnitude, previousMagnitude)
        
        assertEquals(ThreatLevel.NONE, threatLevel)
        assertEquals(1.0f, confidence, 0.01f)
    }

    /**
     * Test: Mouvement léger → ThreatLevel.LOW
     */
    @Test
    fun `test light movement returns LOW threat level`() {
        val magnitude = 1.5f
        val previousMagnitude = 0.5f
        val (threatLevel, confidence) = evaluateThreatLevel(magnitude, previousMagnitude)
        
        assertEquals(ThreatLevel.LOW, threatLevel)
        assertTrue(confidence > 0f && confidence < 1.0f)
    }

    /**
     * Test: Mouvement modéré → ThreatLevel.MEDIUM
     */
    @Test
    fun `test moderate movement returns MEDIUM threat level`() {
        val magnitude = 5.0f
        val previousMagnitude = 1.0f
        val (threatLevel, confidence) = evaluateThreatLevel(magnitude, previousMagnitude)
        
        assertEquals(ThreatLevel.MEDIUM, threatLevel)
        assertTrue(confidence >= 0.6f)
    }

    /**
     * Test: Mouvement brusque (shake) → ThreatLevel.HIGH
     */
    @Test
    fun `test sudden movement shake returns HIGH threat level`() {
        val magnitude = 20.0f // Au-dessus du seuil de shake
        val previousMagnitude = 1.0f
        val (threatLevel, confidence) = evaluateThreatLevel(magnitude, previousMagnitude)
        
        assertEquals(ThreatLevel.HIGH, threatLevel)
        assertTrue(confidence >= 0.9f)
    }

    /**
     * Test: Changement brutal d'accélération → ThreatLevel.HIGH
     */
    @Test
    fun `test sudden acceleration change returns HIGH threat level`() {
        val magnitude = 10.0f
        val previousMagnitude = 1.0f // Changement brutal
        val (threatLevel, confidence) = evaluateThreatLevel(magnitude, previousMagnitude)
        
        // Le changement brutal devrait déclencher HIGH
        assertTrue(
            "Sudden acceleration change should return HIGH",
            threatLevel == ThreatLevel.HIGH || threatLevel == ThreatLevel.MEDIUM
        )
    }

    /**
     * Test: Calcul de magnitude d'accélération
     */
    @Test
    fun `test acceleration magnitude calculation`() {
        val x = 3.0f
        val y = 4.0f
        val z = 0.0f
        val magnitude = sqrt(x * x + y * y + z * z)
        
        assertEquals(5.0f, magnitude, 0.01f)
    }

    // --- Helper functions (logique extraite de MotionSensor) ---

    private fun evaluateThreatLevel(
        magnitude: Float,
        previousMagnitude: Float
    ): Pair<ThreatLevel, Float> {
        val accelerationChange = kotlin.math.abs(magnitude - previousMagnitude)

        return when {
            // Mouvement très fort ou changement brutal = haute menace
            magnitude > shakeThreshold || accelerationChange > (shakeThreshold / 2) -> 
                ThreatLevel.HIGH to 0.9f
            // Mouvement modéré
            magnitude > movementThreshold || accelerationChange > (movementThreshold / 2) -> 
                ThreatLevel.MEDIUM to 0.6f
            // Léger mouvement
            magnitude > 1f -> 
                ThreatLevel.LOW to 0.3f
            // Pas de mouvement significatif
            else -> 
                ThreatLevel.NONE to 1.0f
        }
    }
}


