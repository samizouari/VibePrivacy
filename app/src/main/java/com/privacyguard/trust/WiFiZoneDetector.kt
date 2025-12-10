package com.privacyguard.trust

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Détecte le WiFi actuellement connecté
 * 
 * Utilise le SSID du WiFi comme identifiant de zone de confiance.
 * Plus économe en batterie que le GPS.
 */
class WiFiZoneDetector(private val context: Context) {
    
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    // État actuel du WiFi
    private val _currentSsid = MutableStateFlow<String?>(null)
    val currentSsid: StateFlow<String?> = _currentSsid.asStateFlow()
    
    private val _isWifiConnected = MutableStateFlow(false)
    val isWifiConnected: StateFlow<Boolean> = _isWifiConnected.asStateFlow()
    
    // Callback pour changements de réseau
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateCurrentSsid()
        }
        
        override fun onLost(network: Network) {
            _currentSsid.value = null
            _isWifiConnected.value = false
            Timber.d("WiFiZoneDetector: WiFi disconnected")
        }
        
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                updateCurrentSsid()
            }
        }
    }
    
    /**
     * Démarre la surveillance du WiFi
     */
    fun start() {
        // S'enregistrer pour les changements de réseau
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        
        connectivityManager.registerNetworkCallback(request, networkCallback)
        
        // Vérifier l'état initial
        updateCurrentSsid()
        
        Timber.i("WiFiZoneDetector: Started monitoring WiFi")
    }
    
    /**
     * Arrête la surveillance
     */
    fun stop() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            Timber.i("WiFiZoneDetector: Stopped monitoring WiFi")
        } catch (e: Exception) {
            Timber.e(e, "WiFiZoneDetector: Error stopping")
        }
    }
    
    /**
     * Met à jour le SSID actuel
     */
    private fun updateCurrentSsid() {
        val ssid = getCurrentSsidInternal()
        _currentSsid.value = ssid
        _isWifiConnected.value = ssid != null
        
        if (ssid != null) {
            Timber.d("WiFiZoneDetector: Connected to '$ssid'")
        }
    }
    
    /**
     * Récupère le SSID du WiFi actuel
     * 
     * @return SSID sans guillemets, ou null si pas connecté
     */
    fun getCurrentSsid(): String? {
        return _currentSsid.value
    }
    
    /**
     * Récupère le SSID du WiFi en fonction de la version Android
     */
    private fun getCurrentSsidInternal(): String? {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ : Utiliser ConnectivityManager
                val network = connectivityManager.activeNetwork ?: return null
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return null
                
                if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return null
                }
                
                // Pour Android 12+, on ne peut obtenir le SSID que si l'app a la permission NEARBY_WIFI_DEVICES
                // Pour l'instant, on retourne un placeholder
                return extractSsidFromWifiInfo()
            } else {
                // Android < 12 : Utiliser WifiManager (déprécié mais fonctionnel)
                return extractSsidFromWifiInfo()
            }
        } catch (e: Exception) {
            Timber.e(e, "WiFiZoneDetector: Error getting SSID")
            return null
        }
    }
    
    /**
     * Extrait le SSID du WifiInfo
     */
    @Suppress("DEPRECATION")
    private fun extractSsidFromWifiInfo(): String? {
        val wifiInfo = wifiManager.connectionInfo ?: return null
        val ssid = wifiInfo.ssid ?: return null
        
        // Enlever les guillemets
        return ssid.removeSurrounding("\"").takeIf { it != "<unknown ssid>" }
    }
}

