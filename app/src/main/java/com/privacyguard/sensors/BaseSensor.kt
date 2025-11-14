package com.privacyguard.sensors

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapNotNull
import timber.log.Timber

/**
 * Interface de base pour tous les capteurs
 * 
 * Tous les capteurs (caméra, audio, mouvement, proximité) implémentent cette interface
 * pour une gestion unifiée dans le service principal.
 */
interface Sensor<T : SensorData> {
    /**
     * Nom du capteur (pour logging et debugging)
     */
    val sensorName: String
    
    /**
     * Indique si le capteur est actuellement actif
     */
    val isActive: Boolean
    
    /**
     * Flow des données émises par le capteur
     */
    val dataFlow: Flow<T>
    
    /**
     * Démarre le capteur
     */
    suspend fun start()
    
    /**
     * Arrête le capteur
     */
    suspend fun stop()
    
    /**
     * Met en pause le capteur (garde les ressources mais arrête la collecte)
     */
    suspend fun pause()
    
    /**
     * Reprend le capteur après une pause
     */
    suspend fun resume()
}

/**
 * Classe abstraite de base pour implémenter un capteur
 * 
 * Fournit les fonctionnalités communes à tous les capteurs :
 * - Gestion de l'état (actif/inactif)
 * - Flow de données
 * - Logging
 */
abstract class BaseSensor<T : SensorData>(
    protected val context: Context,
    override val sensorName: String
) : Sensor<T> {
    
    protected val _dataFlow = MutableStateFlow<T?>(null)
    private val _isActive = MutableStateFlow(false)
    
    override val isActive: Boolean
        get() = _isActive.value
    
    override val dataFlow: Flow<T>
        get() = _dataFlow.asStateFlow().mapNotNull { it }
    
    override suspend fun start() {
        if (_isActive.value) {
            Timber.w("$sensorName: Already started, ignoring start request")
            return
        }
        
        Timber.i("$sensorName: Starting...")
        try {
            onStart()
            _isActive.value = true
            Timber.i("$sensorName: Started successfully")
        } catch (e: Exception) {
            Timber.e(e, "$sensorName: Failed to start")
            throw e
        }
    }
    
    override suspend fun stop() {
        if (!_isActive.value) {
            Timber.w("$sensorName: Already stopped, ignoring stop request")
            return
        }
        
        Timber.i("$sensorName: Stopping...")
        try {
            onStop()
            _isActive.value = false
            Timber.i("$sensorName: Stopped successfully")
        } catch (e: Exception) {
            Timber.e(e, "$sensorName: Failed to stop cleanly")
            _isActive.value = false
        }
    }
    
    override suspend fun pause() {
        if (!_isActive.value) {
            Timber.w("$sensorName: Not active, ignoring pause request")
            return
        }
        
        Timber.i("$sensorName: Pausing...")
        onPause()
    }
    
    override suspend fun resume() {
        if (!_isActive.value) {
            Timber.w("$sensorName: Not active, ignoring resume request")
            return
        }
        
        Timber.i("$sensorName: Resuming...")
        onResume()
    }
    
    /**
     * Émet une nouvelle donnée dans le flow
     */
    protected fun emitData(data: T) {
        _dataFlow.value = data
    }
    
    /**
     * Méthode abstraite appelée lors du démarrage du capteur
     * À implémenter par les classes enfants
     */
    protected abstract suspend fun onStart()
    
    /**
     * Méthode abstraite appelée lors de l'arrêt du capteur
     * À implémenter par les classes enfants
     */
    protected abstract suspend fun onStop()
    
    /**
     * Méthode appelée lors de la mise en pause (par défaut = stop)
     */
    protected open suspend fun onPause() {
        onStop()
    }
    
    /**
     * Méthode appelée lors de la reprise (par défaut = start)
     */
    protected open suspend fun onResume() {
        onStart()
    }
}

