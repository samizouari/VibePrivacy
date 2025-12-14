package com.privacyguard.trust

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import kotlin.math.sqrt

/**
 * Encode un visage en vecteur d'embedding (128 dimensions)
 * 
 * Utilise ML Kit pour détecter le visage, puis génère un embedding
 * simplifié basé sur les landmarks faciaux.
 * 
 * Note: Pour une précision maximale, utiliser TensorFlow Lite avec
 * un modèle FaceNet pré-entraîné. Cette implémentation est simplifiée
 * pour le MVP.
 */
class FaceEncoder(private val context: Context) {
    
    companion object {
        const val EMBEDDING_SIZE = 128
    }
    
    private val faceDetector: FaceDetector
    
    init {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .build()
        
        faceDetector = FaceDetection.getClient(options)
    }
    
    /**
     * Encode un bitmap en vecteur d'embedding
     * 
     * @return FloatArray de 128 dimensions normalisé, ou null si pas de visage détecté
     */
    suspend fun encode(bitmap: Bitmap): FloatArray? {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val faces = faceDetector.process(image).await()
            
            if (faces.isEmpty()) {
                Timber.w("FaceEncoder: No face detected in image")
                return null
            }
            
            // Prendre le visage le plus grand
            val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                ?: return null
            
            // Générer l'embedding à partir des features du visage
            val embedding = generateEmbedding(face, bitmap)
            
            // Normaliser (L2 norm)
            val normalized = normalizeEmbedding(embedding)
            
            Timber.d("FaceEncoder: Generated embedding (${normalized.size} dims)")
            return normalized
            
        } catch (e: Exception) {
            Timber.e(e, "FaceEncoder: Error encoding face")
            return null
        }
    }
    
    /**
     * Encode plusieurs images du même visage et fait la moyenne
     * 
     * Améliore la précision en combinant plusieurs angles/éclairages
     */
    suspend fun encodeMultiple(bitmaps: List<Bitmap>): FloatArray? {
        val embeddings = mutableListOf<FloatArray>()
        
        for (bitmap in bitmaps) {
            val embedding = encode(bitmap)
            if (embedding != null) {
                embeddings.add(embedding)
            }
        }
        
        if (embeddings.isEmpty()) {
            Timber.w("FaceEncoder: No valid faces detected in any image")
            return null
        }
        
        // Moyenne des embeddings
        val avgEmbedding = FloatArray(EMBEDDING_SIZE) { 0f }
        for (embedding in embeddings) {
            for (i in embedding.indices) {
                avgEmbedding[i] += embedding[i]
            }
        }
        
        for (i in avgEmbedding.indices) {
            avgEmbedding[i] = avgEmbedding[i] / embeddings.size
        }
        
        // Normaliser le résultat
        val normalized = normalizeEmbedding(avgEmbedding)
        
        Timber.i("FaceEncoder: Generated average embedding from ${embeddings.size} images")
        return normalized
    }
    
    /**
     * Génère un embedding simplifié à partir des features ML Kit
     * 
     * Note: Ceci est une approche simplifiée pour le MVP.
     * Pour une précision optimale, utiliser un modèle FaceNet pré-entraîné.
     */
    private fun generateEmbedding(face: Face, bitmap: Bitmap): FloatArray {
        val embedding = FloatArray(EMBEDDING_SIZE)
        var idx = 0
        
        // 1. Caractéristiques géométriques du visage (20 dims)
        val box = face.boundingBox
        val width = box.width().toFloat()
        val height = box.height().toFloat()
        val aspect = width / height
        
        embedding[idx++] = width / bitmap.width
        embedding[idx++] = height / bitmap.height
        embedding[idx++] = aspect
        embedding[idx++] = box.centerX().toFloat() / bitmap.width
        embedding[idx++] = box.centerY().toFloat() / bitmap.height
        
        // Angles de rotation
        embedding[idx++] = normalizeAngle(face.headEulerAngleX)
        embedding[idx++] = normalizeAngle(face.headEulerAngleY)
        embedding[idx++] = normalizeAngle(face.headEulerAngleZ)
        
        // Probabilités de classification
        val smileProb = face.smilingProbability ?: 0.5f
        val leftEyeOpenProb = face.leftEyeOpenProbability ?: 0.5f
        val rightEyeOpenProb = face.rightEyeOpenProbability ?: 0.5f
        
        embedding[idx++] = smileProb
        embedding[idx++] = leftEyeOpenProb
        embedding[idx++] = rightEyeOpenProb
        
        // Padding pour atteindre 20
        while (idx < 20) {
            embedding[idx++] = 0f
        }
        
        // 2. Landmarks faciaux normalisés (60 dims = 30 landmarks * 2)
        val landmarks = face.allLandmarks
        for (i in 0 until 30) {
            if (i < landmarks.size) {
                val landmark = landmarks[i]
                embedding[idx++] = landmark.position.x / bitmap.width
                embedding[idx++] = landmark.position.y / bitmap.height
            } else {
                embedding[idx++] = 0f
                embedding[idx++] = 0f
            }
        }
        
        // 3. Contours faciaux (48 dims)
        val contours = face.allContours
        for (i in 0 until 24) {
            if (i < contours.size) {
                val contour = contours[i]
                if (contour.points.isNotEmpty()) {
                    val point = contour.points[0]
                    embedding[idx++] = point.x / bitmap.width
                    embedding[idx++] = point.y / bitmap.height
                } else {
                    embedding[idx++] = 0f
                    embedding[idx++] = 0f
                }
            } else {
                embedding[idx++] = 0f
                embedding[idx++] = 0f
            }
        }
        
        // Remplir le reste avec du bruit aléatoire normalisé
        while (idx < EMBEDDING_SIZE) {
            embedding[idx++] = (Math.random().toFloat() - 0.5f) * 0.1f
        }
        
        return embedding
    }
    
    /**
     * Normalise un angle en [-180, 180] vers [-1, 1]
     */
    private fun normalizeAngle(angle: Float): Float {
        return angle / 180f
    }
    
    /**
     * Normalise un embedding avec la norme L2
     */
    private fun normalizeEmbedding(embedding: FloatArray): FloatArray {
        var sumSquares = 0f
        for (value in embedding) {
            sumSquares += value * value
        }
        
        val norm = sqrt(sumSquares)
        
        return if (norm > 0f) {
            FloatArray(embedding.size) { embedding[it] / norm }
        } else {
            embedding
        }
    }
    
    /**
     * Nettoyage
     */
    fun cleanup() {
        faceDetector.close()
    }
}



