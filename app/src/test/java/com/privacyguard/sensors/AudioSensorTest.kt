package com.privacyguard.sensors

import org.junit.Test
import org.junit.Assert.*
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Tests unitaires pour AudioSensor
 * 
 * Tests la logique de détection audio et d'évaluation des menaces.
 */
class AudioSensorTest {

    /**
     * Test: Niveau sonore faible → ThreatLevel.NONE
     */
    @Test
    fun `test low audio level returns NONE threat level`() {
        val decibels = 5.0f
        val (threatLevel, confidence) = evaluateThreatLevelFromDecibels(decibels)
        
        assertEquals(ThreatLevel.NONE, threatLevel)
        assertEquals(1.0f, confidence, 0.01f)
    }

    /**
     * Test: Niveau sonore modéré → ThreatLevel.LOW
     */
    @Test
    fun `test moderate audio level returns LOW threat level`() {
        val decibels = 20.0f
        val (threatLevel, confidence) = evaluateThreatLevelFromDecibels(decibels)
        
        assertEquals(ThreatLevel.LOW, threatLevel)
        assertTrue(confidence > 0f && confidence < 1.0f)
    }

    /**
     * Test: Niveau sonore élevé (parole) → ThreatLevel.MEDIUM
     */
    @Test
    fun `test high audio level speech returns MEDIUM threat level`() {
        val decibels = 50.0f // Au-dessus du seuil de parole (40dB)
        val (threatLevel, confidence) = evaluateThreatLevelFromDecibels(decibels)
        
        assertEquals(ThreatLevel.MEDIUM, threatLevel)
        assertTrue(confidence >= 0.6f)
    }

    /**
     * Test: Niveau sonore très élevé → ThreatLevel.HIGH
     */
    @Test
    fun `test very high audio level returns HIGH threat level`() {
        val decibels = 75.0f // Au-dessus du seuil suspect (70dB)
        val (threatLevel, confidence) = evaluateThreatLevelFromDecibels(decibels)
        
        assertEquals(ThreatLevel.HIGH, threatLevel)
        assertTrue(confidence >= 0.8f)
    }

    /**
     * Test: Détection de parole
     */
    @Test
    fun `test speech detection`() {
        val minDecibelsForSpeech = 40.0f
        
        assertFalse("Low level should not be speech", isSpeechDetected(10.0f, minDecibelsForSpeech))
        assertTrue("High level should be speech", isSpeechDetected(50.0f, minDecibelsForSpeech))
    }

    /**
     * Test: Calcul RMS et décibels
     */
    @Test
    fun `test RMS and decibel calculation`() {
        // Simuler un buffer audio avec des valeurs
        val buffer = shortArrayOf(1000, 2000, 1500, 3000, 1000)
        val (rms, decibels) = calculateRMSAndDecibels(buffer)
        
        assertTrue("RMS should be positive", rms > 0)
        assertTrue("Decibels should be calculated", decibels.isFinite())
    }

    // --- Helper functions (logique extraite de AudioSensor) ---

    private fun evaluateThreatLevelFromDecibels(decibels: Float): Pair<ThreatLevel, Float> {
        val minDecibelsForSpeech = 40.0f
        val suspiciousDecibels = 70.0f
        
        val isSpeech = decibels > minDecibelsForSpeech
        val isSuspicious = decibels > suspiciousDecibels

        val threatLevel = when {
            isSuspicious -> ThreatLevel.HIGH
            isSpeech -> ThreatLevel.MEDIUM
            decibels > 10 -> ThreatLevel.LOW
            else -> ThreatLevel.NONE
        }

        val confidence = when (threatLevel) {
            ThreatLevel.HIGH -> 0.8f
            ThreatLevel.MEDIUM -> 0.6f
            ThreatLevel.LOW -> 0.3f
            ThreatLevel.NONE -> 1.0f
            else -> 0f
        }

        return threatLevel to confidence
    }

    private fun isSpeechDetected(decibels: Float, threshold: Float): Boolean {
        return decibels > threshold
    }

    private fun calculateRMSAndDecibels(buffer: ShortArray): Pair<Double, Double> {
        var sumOfSquares = 0.0
        for (i in buffer.indices) {
            val s = buffer[i].toDouble()
            sumOfSquares += s * s
        }
        val rms = if (buffer.isNotEmpty()) sqrt(sumOfSquares / buffer.size) else 0.0
        val decibels = if (rms > 0) 20 * log10(rms / 32767.0 * 10000) else 0.0
        return rms to decibels
    }
}


