package com.privacyguard.trust.models

/**
 * Représente un visage de confiance enregistré
 * 
 * Utilise un encodage facial (embedding) plutôt que les images brutes
 * pour des raisons de sécurité et de performance.
 */
data class TrustedFace(
    val id: String,                     // UUID unique
    val name: String?,                  // Nom optionnel (ex: "Maman", "Papa")
    
    // Encodage facial (128 dimensions normalisé)
    val encoding: FloatArray,           // Vecteur d'embedding du visage
    
    // Miniature (optionnel, chiffré)
    val thumbnailBase64: String? = null,
    
    // Métadonnées
    val createdAt: Long = System.currentTimeMillis(),
    val lastSeenAt: Long? = null,
    val recognitionCount: Int = 0,
    
    // Seuil de confiance personnalisé (0.0-1.0)
    val confidenceThreshold: Float = 0.75f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TrustedFace

        if (id != other.id) return false
        if (!encoding.contentEquals(other.encoding)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + encoding.contentHashCode()
        return result
    }
}

/**
 * Résultat de la reconnaissance faciale
 */
sealed class FaceMatchResult {
    object Unknown : FaceMatchResult()
    data class Trusted(
        val face: TrustedFace,
        val confidence: Float
    ) : FaceMatchResult()
}

/**
 * Données pour l'enregistrement d'un nouveau visage
 */
data class FaceRegistrationData(
    val name: String?,
    val photos: List<ByteArray>,        // 3-5 photos du même visage
    val thumbnailPhoto: ByteArray? = null
)

