package com.privacyguard.sensors

import org.junit.Test
import org.junit.Assert.*

/**
 * Tests unitaires pour ProximitySensor
 * 
 * Tests la logique de détection de proximité et d'évaluation des menaces.
 * Note: Le capteur de proximité est souvent binaire (0 ou maxRange) sur Android.
 */
class ProximitySensorTest {

    private val maxRange = 5.0f
    private val nearThreshold = 3.0f
    private val veryNearThreshold = 1.0f

    /**
     * Test: Capteur binaire - distance = 0 (très proche) → ThreatLevel.HIGH
     */
    @Test
    fun `test binary sensor distance zero returns HIGH threat level`() {
        val distance = 0f
        val (threatLevel, confidence) = evaluateThreatLevel(distance, maxRange)
        
        assertEquals(ThreatLevel.HIGH, threatLevel)
        assertEquals(0.9f, confidence, 0.01f)
    }

    /**
     * Test: Capteur binaire - distance = maxRange (loin) → ThreatLevel.NONE
     */
    @Test
    fun `test binary sensor distance maxRange returns NONE threat level`() {
        val distance = maxRange
        val (threatLevel, confidence) = evaluateThreatLevel(distance, maxRange)
        
        assertEquals(ThreatLevel.NONE, threatLevel)
        assertEquals(1.0f, confidence, 0.01f)
    }

    /**
     * Test: Capteur continu - objet très proche (< 1cm) → ThreatLevel.HIGH
     */
    @Test
    fun `test continuous sensor very close returns HIGH threat level`() {
        val distance = 0.5f // < veryNearThreshold
        val (threatLevel, confidence) = evaluateThreatLevel(distance, maxRange)
        
        assertEquals(ThreatLevel.HIGH, threatLevel)
        assertEquals(0.9f, confidence, 0.01f)
    }

    /**
     * Test: Capteur continu - objet proche (< 3cm) → ThreatLevel.MEDIUM
     */
    @Test
    fun `test continuous sensor close returns MEDIUM threat level`() {
        val distance = 2.0f // < nearThreshold mais > veryNearThreshold
        val (threatLevel, confidence) = evaluateThreatLevel(distance, maxRange)
        
        assertEquals(ThreatLevel.MEDIUM, threatLevel)
        assertEquals(0.75f, confidence, 0.01f)
    }

    /**
     * Test: Capteur continu - objet à portée mais pas proche → ThreatLevel.LOW
     */
    @Test
    fun `test continuous sensor in range but not close returns LOW threat level`() {
        val distance = 4.0f // < maxRange mais > nearThreshold
        val (threatLevel, confidence) = evaluateThreatLevel(distance, maxRange)
        
        assertEquals(ThreatLevel.LOW, threatLevel)
        assertEquals(0.5f, confidence, 0.01f)
    }

    /**
     * Test: Détection si capteur est binaire
     */
    @Test
    fun `test binary sensor detection`() {
        assertTrue("0cm should be binary", isBinarySensor(0f, maxRange))
        assertTrue("maxRange should be binary", isBinarySensor(maxRange, maxRange))
        assertFalse("Intermediate value should not be binary", isBinarySensor(2.5f, maxRange))
    }

    // --- Helper functions (logique extraite de ProximitySensor) ---

    private fun evaluateThreatLevel(distance: Float, maxRange: Float): Pair<ThreatLevel, Float> {
        // Si le capteur est binaire (distance = 0 ou = maxRange)
        val isBinarySensor = (distance == 0f || distance == maxRange) && maxRange > 0f

        return when {
            // Capteur binaire : 0 = très proche = haute menace
            isBinarySensor && distance == 0f -> ThreatLevel.HIGH to 0.9f

            // Capteur binaire : maxRange = loin = aucune menace
            isBinarySensor && distance == maxRange -> ThreatLevel.NONE to 1.0f

            // Capteur continu : Objet très proche (< 1cm) = haute menace
            !isBinarySensor && distance < veryNearThreshold -> ThreatLevel.HIGH to 0.9f

            // Capteur continu : Objet proche (< 3cm) = menace moyenne
            !isBinarySensor && distance < nearThreshold -> ThreatLevel.MEDIUM to 0.75f

            // Capteur continu : Objet à portée mais pas très proche = faible menace
            !isBinarySensor && distance < maxRange -> ThreatLevel.LOW to 0.5f

            // Rien de proche = aucune menace
            else -> ThreatLevel.NONE to 1.0f
        }
    }

    private fun isBinarySensor(distance: Float, maxRange: Float): Boolean {
        return (distance == 0f || distance == maxRange) && maxRange > 0f
    }
}


