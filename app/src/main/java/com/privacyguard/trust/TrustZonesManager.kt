package com.privacyguard.trust

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.privacyguard.trust.models.TrustZone
import com.privacyguard.trust.models.TrustZoneCheckResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.UUID

/**
 * Gestionnaire des zones de confiance
 * 
 * Permet de définir des zones (WiFi ou GPS) où la protection est réduite.
 * Utilise EncryptedSharedPreferences pour stocker les zones de manière sécurisée.
 */
class TrustZonesManager(
    private val context: Context,
    private val wifiDetector: WiFiZoneDetector
) {
    
    companion object {
        private const val PREFS_NAME = "trust_zones_prefs"
        private const val KEY_ZONES = "zones"
        private const val MAX_ZONES = 10
    }
    
    private val gson = Gson()
    
    // EncryptedSharedPreferences pour stockage sécurisé
    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    // État actuel
    private val _zones = MutableStateFlow<List<TrustZone>>(emptyList())
    val zones: StateFlow<List<TrustZone>> = _zones.asStateFlow()
    
    private val _currentZone = MutableStateFlow<TrustZone?>(null)
    val currentZone: StateFlow<TrustZone?> = _currentZone.asStateFlow()
    
    private val _isInTrustZone = MutableStateFlow(false)
    val isInTrustZone: StateFlow<Boolean> = _isInTrustZone.asStateFlow()
    
    init {
        loadZones()
    }
    
    /**
     * Démarre le monitoring des zones
     */
    fun startMonitoring() {
        // Démarre la détection WiFi
        wifiDetector.start()
        
        // Surveille les changements de WiFi
        // Note: Dans une vraie app, utiliser coroutineScope.launch
        // Pour simplifier ici, on fait un check périodique
        
        Timber.i("TrustZonesManager: Started monitoring")
    }
    
    /**
     * Arrête le monitoring
     */
    fun stopMonitoring() {
        wifiDetector.stop()
        Timber.i("TrustZonesManager: Stopped monitoring")
    }
    
    /**
     * Vérifie si actuellement dans une zone de confiance
     */
    fun checkCurrentLocation(): TrustZoneCheckResult {
        // 1. Vérifier par WiFi (plus rapide, moins de batterie)
        val currentWifi = wifiDetector.getCurrentSsid()
        if (currentWifi != null) {
            val wifiZone = findZoneByWifi(currentWifi)
            if (wifiZone != null && wifiZone.isEnabled) {
                updateCurrentZone(wifiZone)
                return TrustZoneCheckResult.InTrustZone(wifiZone)
            }
        }
        
        // 2. TODO: Vérifier par GPS (Phase 2)
        
        // Pas dans une zone de confiance
        updateCurrentZone(null)
        return TrustZoneCheckResult.NotInTrustZone
    }
    
    /**
     * Ajoute une nouvelle zone de confiance
     */
    fun addZone(zone: TrustZone): Result<TrustZone> {
        if (_zones.value.size >= MAX_ZONES) {
            return Result.failure(Exception("Maximum $MAX_ZONES zones atteint"))
        }
        
        val newZone = zone.copy(
            id = if (zone.id.isEmpty()) UUID.randomUUID().toString() else zone.id
        )
        
        val updatedZones = _zones.value + newZone
        saveZones(updatedZones)
        _zones.value = updatedZones
        
        Timber.i("TrustZonesManager: Added zone '${newZone.name}'")
        return Result.success(newZone)
    }
    
    /**
     * Supprime une zone
     */
    fun removeZone(zoneId: String) {
        val updatedZones = _zones.value.filter { it.id != zoneId }
        saveZones(updatedZones)
        _zones.value = updatedZones
        
        // Si la zone supprimée était la zone courante
        if (_currentZone.value?.id == zoneId) {
            updateCurrentZone(null)
        }
        
        Timber.i("TrustZonesManager: Removed zone $zoneId")
    }
    
    /**
     * Met à jour une zone existante
     */
    fun updateZone(zone: TrustZone) {
        val updatedZones = _zones.value.map {
            if (it.id == zone.id) zone else it
        }
        saveZones(updatedZones)
        _zones.value = updatedZones
        
        Timber.i("TrustZonesManager: Updated zone '${zone.name}'")
    }
    
    /**
     * Active/désactive une zone
     */
    fun toggleZone(zoneId: String) {
        val updatedZones = _zones.value.map {
            if (it.id == zoneId) it.copy(isEnabled = !it.isEnabled) else it
        }
        saveZones(updatedZones)
        _zones.value = updatedZones
    }
    
    /**
     * Trouve une zone par SSID WiFi
     */
    private fun findZoneByWifi(ssid: String): TrustZone? {
        return _zones.value.firstOrNull { zone ->
            zone.wifiSsids.any { it.equals(ssid, ignoreCase = true) }
        }
    }
    
    /**
     * Met à jour la zone courante
     */
    private fun updateCurrentZone(zone: TrustZone?) {
        val previousZone = _currentZone.value
        _currentZone.value = zone
        _isInTrustZone.value = zone != null
        
        // Logger les entrées/sorties
        when {
            zone != null && previousZone == null -> {
                Timber.i("TrustZonesManager: Entered zone '${zone.name}'")
            }
            zone == null && previousZone != null -> {
                Timber.i("TrustZonesManager: Exited zone '${previousZone.name}'")
            }
            zone != null && previousZone != null && zone.id != previousZone.id -> {
                Timber.i("TrustZonesManager: Switched from '${previousZone.name}' to '${zone.name}'")
            }
        }
    }
    
    /**
     * Charge les zones depuis le stockage
     */
    private fun loadZones() {
        try {
            val json = encryptedPrefs.getString(KEY_ZONES, null)
            if (json != null) {
                val type = object : TypeToken<List<TrustZone>>() {}.type
                val loadedZones = gson.fromJson<List<TrustZone>>(json, type)
                _zones.value = loadedZones
                Timber.d("TrustZonesManager: Loaded ${loadedZones.size} zone(s)")
            }
        } catch (e: Exception) {
            Timber.e(e, "TrustZonesManager: Error loading zones")
        }
    }
    
    /**
     * Sauvegarde les zones dans le stockage
     */
    private fun saveZones(zones: List<TrustZone>) {
        try {
            val json = gson.toJson(zones)
            encryptedPrefs.edit().putString(KEY_ZONES, json).apply()
            Timber.d("TrustZonesManager: Saved ${zones.size} zone(s)")
        } catch (e: Exception) {
            Timber.e(e, "TrustZonesManager: Error saving zones")
        }
    }
}




