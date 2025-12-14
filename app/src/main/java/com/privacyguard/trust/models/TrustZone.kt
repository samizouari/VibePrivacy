package com.privacyguard.trust.models

import com.privacyguard.assessment.models.ProtectionMode

/**
 * Représente une zone de confiance
 * 
 * Une zone de confiance est un endroit (maison, bureau) où l'utilisateur
 * souhaite avoir une protection réduite ou désactivée.
 */
data class TrustZone(
    val id: String,                     // UUID unique
    val name: String,                   // "Maison", "Bureau"
    
    // Localisation GPS (optionnel)
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Int = 100,        // Rayon en mètres
    
    // WiFi associés (optionnel)
    val wifiSsids: List<String> = emptyList(),
    
    // Comportement dans cette zone
    val protectionBehavior: TrustZoneProtection = TrustZoneProtection.DISABLED,
    
    // État
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastEnteredAt: Long? = null
)

/**
 * Définit le comportement de protection dans une zone de confiance
 */
enum class TrustZoneProtection(
    val displayName: String,
    val description: String,
    val protectionMode: ProtectionMode?
) {
    DISABLED(
        "Désactivée",
        "Aucune protection active",
        null
    ),
    MINIMAL(
        "Minimale",
        "Mode TRUST_ZONE (seuil 95%)",
        ProtectionMode.TRUST_ZONE
    ),
    NO_OVERLAY(
        "Sans overlay",
        "Protection active mais pas d'overlay visible",
        ProtectionMode.DISCRETE
    ),
    CAMERA_ONLY_OFF(
        "Caméra désactivée",
        "Tous les capteurs sauf la caméra",
        ProtectionMode.BALANCED
    )
}

/**
 * Résultat de la vérification de zone de confiance
 */
sealed class TrustZoneCheckResult {
    object NotInTrustZone : TrustZoneCheckResult()
    data class InTrustZone(val zone: TrustZone) : TrustZoneCheckResult()
}



