package com.privacyguard.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Gestionnaire centralisé des permissions pour Privacy Guard
 * 
 * Gère toutes les permissions nécessaires au fonctionnement de l'application :
 * - Caméra (détection de visages)
 * - Microphone (détection audio)
 * - Localisation (zones de confiance)
 * - Overlay système (affichage protection)
 */
object PermissionManager {
    
    /**
     * Permissions critiques pour le MVP (Mode Discret)
     */
    val CRITICAL_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )
    
    /**
     * Permissions optionnelles (fonctionnalités avancées)
     */
    val OPTIONAL_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    
    /**
     * Toutes les permissions nécessaires
     */
    val ALL_PERMISSIONS = CRITICAL_PERMISSIONS + OPTIONAL_PERMISSIONS
    
    /**
     * Vérifie si une permission spécifique est accordée
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Vérifie si toutes les permissions critiques sont accordées
     */
    fun areCriticalPermissionsGranted(context: Context): Boolean {
        return CRITICAL_PERMISSIONS.all { permission ->
            isPermissionGranted(context, permission)
        }
    }
    
    /**
     * Vérifie si toutes les permissions (critiques + optionnelles) sont accordées
     */
    fun areAllPermissionsGranted(context: Context): Boolean {
        return ALL_PERMISSIONS.all { permission ->
            isPermissionGranted(context, permission)
        }
    }
    
    /**
     * Retourne la liste des permissions manquantes parmi les critiques
     */
    fun getMissingCriticalPermissions(context: Context): List<String> {
        return CRITICAL_PERMISSIONS.filter { permission ->
            !isPermissionGranted(context, permission)
        }
    }
    
    /**
     * Retourne la liste de toutes les permissions manquantes
     */
    fun getMissingPermissions(context: Context): List<String> {
        return ALL_PERMISSIONS.filter { permission ->
            !isPermissionGranted(context, permission)
        }
    }
    
    /**
     * Vérifie si la permission SYSTEM_ALERT_WINDOW est accordée
     * (nécessaire pour afficher l'overlay au-dessus des autres apps)
     */
    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(context)
        } else {
            true // Pas besoin de permission avant Android M
        }
    }
    
    /**
     * Obtient une description lisible d'une permission pour l'utilisateur
     */
    fun getPermissionDescription(permission: String): String {
        return when (permission) {
            Manifest.permission.CAMERA -> 
                "📷 Caméra : Détecte les personnes qui regardent votre écran"
            
            Manifest.permission.RECORD_AUDIO -> 
                "🎤 Microphone : Détecte les bruits suspects autour de vous"
            
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION -> 
                "📍 Localisation : Active la protection dans les lieux publics"
            
            else -> permission
        }
    }
    
    /**
     * Obtient le nom court d'une permission
     */
    fun getPermissionName(permission: String): String {
        return when (permission) {
            Manifest.permission.CAMERA -> "Caméra"
            Manifest.permission.RECORD_AUDIO -> "Microphone"
            Manifest.permission.ACCESS_FINE_LOCATION -> "Localisation précise"
            Manifest.permission.ACCESS_COARSE_LOCATION -> "Localisation approximative"
            else -> permission.substringAfterLast('.')
        }
    }
}

