package com.privacyguard.trust

import com.privacyguard.trust.models.FaceMatchResult
import com.privacyguard.trust.models.TrustedFace
import timber.log.Timber
import kotlin.math.sqrt

/**
 * Compare un visage détecté avec les visages de confiance
 * 
 * Utilise la similarité cosinus pour comparer les embeddings faciaux.
 */
class FaceMatcher {
    
    companion object {
        // Seuil par défaut pour considérer une correspondance
        // Note: Seuil abaissé à 0.60 car l'embedding basé sur landmarks ML Kit
        // est moins précis qu'un modèle FaceNet. Ajuster selon les tests.
        const val DEFAULT_THRESHOLD = 0.60f
    }
    
    /**
     * Vérifie si un visage correspond à un visage de confiance
     * 
     * @param encoding Embedding du visage à vérifier (128 dims)
     * @param trustedFaces Liste des visages de confiance
     * @return FaceMatchResult.Trusted si match, Unknown sinon
     */
    fun match(
        encoding: FloatArray,
        trustedFaces: List<TrustedFace>
    ): FaceMatchResult {
        if (trustedFaces.isEmpty()) {
            return FaceMatchResult.Unknown
        }
        
        var bestMatch: TrustedFace? = null
        var bestSimilarity = 0f
        
        for (face in trustedFaces) {
            val similarity = cosineSimilarity(encoding, face.encoding)
            
            Timber.v("FaceMatcher: Comparing with '${face.name ?: "Unnamed"}' - similarity=$similarity")
            
            if (similarity >= face.confidenceThreshold && similarity > bestSimilarity) {
                bestMatch = face
                bestSimilarity = similarity
            }
        }
        
        return if (bestMatch != null) {
            Timber.i("FaceMatcher: ✓ MATCH '${bestMatch.name ?: "Unnamed"}' (confidence=${(bestSimilarity * 100).toInt()}%)")
            FaceMatchResult.Trusted(bestMatch, bestSimilarity)
        } else {
            Timber.d("FaceMatcher: ✗ No match found (best similarity=$bestSimilarity)")
            FaceMatchResult.Unknown
        }
    }
    
    /**
     * Calcule la similarité cosinus entre deux embeddings
     * 
     * Similarité cosinus = dot(a, b) / (||a|| * ||b||)
     * Retourne une valeur entre 0 (différent) et 1 (identique)
     * 
     * @return Similarité entre 0 et 1
     */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) {
            Timber.e("FaceMatcher: Embedding size mismatch (${a.size} vs ${b.size})")
            return 0f
        }
        
        // Produit scalaire
        var dotProduct = 0f
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
        }
        
        // Normes
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        
        normA = sqrt(normA)
        normB = sqrt(normB)
        
        // Éviter division par zéro
        if (normA == 0f || normB == 0f) {
            return 0f
        }
        
        // Cosine similarity
        val similarity = dotProduct / (normA * normB)
        
        // S'assurer que le résultat est entre 0 et 1
        return similarity.coerceIn(0f, 1f)
    }
    
    /**
     * Calcule la distance euclidienne entre deux embeddings
     * 
     * Distance plus petite = plus similaire
     * Utilisé comme métrique alternative à la similarité cosinus
     */
    fun euclideanDistance(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) {
            return Float.MAX_VALUE
        }
        
        var sumSquares = 0f
        for (i in a.indices) {
            val diff = a[i] - b[i]
            sumSquares += diff * diff
        }
        
        return sqrt(sumSquares)
    }
}




