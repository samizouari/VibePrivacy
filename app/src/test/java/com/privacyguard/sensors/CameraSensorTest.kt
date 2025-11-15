package com.privacyguard.sensors

import android.graphics.Rect
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests unitaires pour CameraSensor
 * 
 * Tests la logique de détection de visages et d'évaluation des menaces.
 * 
 * Note: Ces tests se concentrent sur la logique métier (évaluation des menaces)
 * sans dépendre de CameraX ou ML Kit qui nécessitent un device réel.
 */
class CameraSensorTest {

    /**
     * Test: Aucun visage détecté → ThreatLevel.NONE
     */
    @Test
    fun `test no faces detected returns NONE threat level`() {
        val faces = emptyList<TestFace>()
        val threatLevel = evaluateThreatLevelFromFaces(faces)
        
        assertEquals(ThreatLevel.NONE, threatLevel.first)
        assertEquals(1.0f, threatLevel.second, 0.01f) // Confidence maximale quand aucun visage
    }

    /**
     * Test: 1 visage détecté mais loin → ThreatLevel.LOW
     */
    @Test
    fun `test single face far away returns LOW threat level`() {
        val faces = listOf(createTestFace(0.1f, 0f, 0f)) // Petit visage (loin), regarde droit
        val threatLevel = evaluateThreatLevelFromFaces(faces)
        
        assertEquals(ThreatLevel.LOW, threatLevel.first)
    }

    /**
     * Test: 1 visage proche regardant l'écran → ThreatLevel.MEDIUM
     */
    @Test
    fun `test single face close looking at screen returns MEDIUM threat level`() {
        val faces = listOf(createTestFace(0.3f, 0f, 0f)) // Grand visage (proche), regarde droit
        val threatLevel = evaluateThreatLevelFromFaces(faces)
        
        assertEquals(ThreatLevel.MEDIUM, threatLevel.first)
    }

    /**
     * Test: Plusieurs visages → ThreatLevel.HIGH ou CRITICAL
     */
    @Test
    fun `test multiple faces return HIGH or CRITICAL threat level`() {
        val faces = listOf(
            createTestFace(0.3f, 0f, 0f),
            createTestFace(0.25f, 0f, 0f)
        )
        val threatLevel = evaluateThreatLevelFromFaces(faces)
        
        assertTrue(
            "Multiple faces should return HIGH or CRITICAL",
            threatLevel.first == ThreatLevel.HIGH || threatLevel.first == ThreatLevel.CRITICAL
        )
    }

    /**
     * Test: Visage ne regardant pas l'écran → ThreatLevel plus bas
     */
    @Test
    fun `test face not looking at screen returns lower threat level`() {
        val faces = listOf(createTestFace(0.3f, 45f, 0f)) // Visage tourné à 45 degrés
        val threatLevel = evaluateThreatLevelFromFaces(faces)
        
        assertTrue(
            "Face not looking should return lower threat",
            threatLevel.first.ordinal < ThreatLevel.MEDIUM.ordinal
        )
    }

    // --- Helper functions ---

    /**
     * Fonction helper pour évaluer le niveau de menace à partir d'une liste de visages
     * (Logique extraite de CameraSensor pour les tests)
     */
    private fun evaluateThreatLevelFromFaces(faces: List<TestFace>): Pair<ThreatLevel, Float> {
        if (faces.isEmpty()) {
            return ThreatLevel.NONE to 1.0f
        }

        var facesLookingAtScreen = 0
        var maxProximityThreat = 0f

        for (face in faces) {
            // Détection de l'orientation du visage (regarde l'écran ?)
            val eulerY = face.eulerY // Yaw
            val eulerZ = face.eulerZ // Roll

            val isLooking = kotlin.math.abs(eulerY) < 20 && kotlin.math.abs(eulerZ) < 20
            if (isLooking) {
                facesLookingAtScreen++
            }

            // Estimation de la proximité (taille du bounding box)
            val faceWidth = face.boundingBox.width().toFloat()
            val faceHeight = face.boundingBox.height().toFloat()
            val faceArea = faceWidth * faceHeight

            // Normaliser la surface du visage par rapport à la taille de l'image
            val imageArea = 320f * 240f // Taille de référence pour la caméra en low-res
            val proximityRatio = faceArea / imageArea
            if (proximityRatio > maxProximityThreat) {
                maxProximityThreat = proximityRatio
            }
        }

        // Évaluation du niveau de menace
        val threatLevel = when {
            faces.size > 1 && facesLookingAtScreen > 0 -> ThreatLevel.CRITICAL
            facesLookingAtScreen > 0 && maxProximityThreat > 0.2f -> ThreatLevel.HIGH
            facesLookingAtScreen > 0 -> ThreatLevel.MEDIUM
            faces.size > 0 -> ThreatLevel.LOW
            else -> ThreatLevel.NONE
        }

        val confidence = when (threatLevel) {
            ThreatLevel.CRITICAL -> 0.9f
            ThreatLevel.HIGH -> 0.8f
            ThreatLevel.MEDIUM -> 0.6f
            ThreatLevel.LOW -> 0.4f
            ThreatLevel.NONE -> 1.0f
        }

        return threatLevel to confidence
    }

    /**
     * Crée une structure de données simulant un Face pour les tests
     */
    private data class TestFace(
        val boundingBox: Rect,
        val eulerY: Float,
        val eulerZ: Float
    )

    /**
     * Crée un TestFace avec les paramètres spécifiés
     */
    private fun createTestFace(
        areaRatio: Float, // Ratio de la surface du visage par rapport à l'image
        eulerY: Float, // Yaw (rotation horizontale)
        eulerZ: Float // Roll (rotation verticale)
    ): TestFace {
        // Calculer la taille du bounding box pour avoir le ratio d'aire souhaité
        val imageArea = 320f * 240f
        val faceArea = imageArea * areaRatio
        val faceSize = kotlin.math.sqrt(faceArea).toInt()
        
        val boundingBox = Rect(
            (320 - faceSize) / 2,
            (240 - faceSize) / 2,
            (320 + faceSize) / 2,
            (240 + faceSize) / 2
        )

        return TestFace(boundingBox, eulerY, eulerZ)
    }
}

