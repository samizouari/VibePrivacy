package com.privacyguard.sensors

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

/**
 * Gestionnaire centralisé de tous les capteurs
 * 
 * Coordonne les 4 capteurs :
 * - Camera (ML Kit face detection)
 * - Audio (niveau sonore)
 * - Motion (accéléromètre)
 * - Proximity (capteur de proximité)
 * 
 * Expose un Flow combiné pour la fusion de capteurs.
 */
class SensorManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Capteurs
    private lateinit var cameraSensor: CameraSensor
    private lateinit var audioSensor: AudioSensor
    private lateinit var motionSensor: MotionSensor
    private lateinit var proximitySensor: ProximitySensor
    
    // Dernières données de chaque capteur
    private val _cameraData = MutableStateFlow<CameraData?>(null)
    private val _audioData = MutableStateFlow<AudioData?>(null)
    private val _motionData = MutableStateFlow<MotionData?>(null)
    private val _proximityData = MutableStateFlow<ProximityData?>(null)
    
    val cameraData: StateFlow<CameraData?> = _cameraData.asStateFlow()
    val audioData: StateFlow<AudioData?> = _audioData.asStateFlow()
    val motionData: StateFlow<MotionData?> = _motionData.asStateFlow()
    val proximityData: StateFlow<ProximityData?> = _proximityData.asStateFlow()
    
    /**
     * Flow combiné de toutes les données de capteurs
     * Émis à chaque fois qu'un capteur émet de nouvelles données
     */
    val combinedSensorData: Flow<SensorDataSnapshot> = combine(
        _cameraData,
        _audioData,
        _motionData,
        _proximityData
    ) { camera, audio, motion, proximity ->
        // Log pour debug
        Timber.v("SensorManager combine: camera=${camera != null}, audio=${audio != null}, motion=${motion != null}, proximity=${proximity != null}")
        
        SensorDataSnapshot(
            timestamp = System.currentTimeMillis(),
            cameraData = camera,
            audioData = audio,
            motionData = motion,
            proximityData = proximity
        )
    }
    
    private var sensorsJob: Job? = null
    
    /**
     * Initialise tous les capteurs
     */
    fun initialize() {
        Timber.i("SensorManager: Initializing sensors...")
        
        try {
            cameraSensor = CameraSensor(context, lifecycleOwner)
            audioSensor = AudioSensor(context)
            motionSensor = MotionSensor(context)
            proximitySensor = ProximitySensor(context)
            
            Timber.i("SensorManager: All sensors initialized")
        } catch (e: Exception) {
            Timber.e(e, "SensorManager: Failed to initialize sensors")
            throw e
        }
    }
    
    /**
     * Démarre tous les capteurs
     */
    suspend fun startAll() {
        Timber.i("SensorManager: Starting all sensors...")
        
        sensorsJob = scope.launch {
            // Démarrer les capteurs en parallèle
            val jobs = listOf(
                async { startCameraSensor() },
                async { startAudioSensor() },
                async { startMotionSensor() },
                async { startProximitySensor() }
            )
            
            jobs.awaitAll()
        }
        
        Timber.i("SensorManager: All sensors started")
    }
    
    /**
     * Arrête tous les capteurs
     */
    suspend fun stopAll() {
        Timber.i("SensorManager: Stopping all sensors...")
        
        sensorsJob?.cancelAndJoin()
        sensorsJob = null
        
        // Arrêter les capteurs en parallèle
        coroutineScope {
            listOf(
                async { stopCameraSensor() },
                async { stopAudioSensor() },
                async { stopMotionSensor() },
                async { stopProximitySensor() }
            ).awaitAll()
        }
        
        Timber.i("SensorManager: All sensors stopped")
    }
    
    /**
     * Met en pause tous les capteurs
     */
    suspend fun pauseAll() {
        Timber.i("SensorManager: Pausing all sensors...")
        
        coroutineScope {
            listOf(
                async { if (::cameraSensor.isInitialized) cameraSensor.pause() },
                async { if (::audioSensor.isInitialized) audioSensor.pause() },
                async { if (::motionSensor.isInitialized) motionSensor.pause() },
                async { if (::proximitySensor.isInitialized) proximitySensor.pause() }
            ).awaitAll()
        }
        
        Timber.i("SensorManager: All sensors paused")
    }
    
    /**
     * Reprend tous les capteurs
     */
    suspend fun resumeAll() {
        Timber.i("SensorManager: Resuming all sensors...")
        
        coroutineScope {
            listOf(
                async { if (::cameraSensor.isInitialized) cameraSensor.resume() },
                async { if (::audioSensor.isInitialized) audioSensor.resume() },
                async { if (::motionSensor.isInitialized) motionSensor.resume() },
                async { if (::proximitySensor.isInitialized) proximitySensor.resume() }
            ).awaitAll()
        }
        
        Timber.i("SensorManager: All sensors resumed")
    }
    
    // --- Méthodes privées pour chaque capteur ---
    
    private suspend fun startCameraSensor() {
        try {
            Timber.d("SensorManager: Starting CameraSensor...")
            cameraSensor.start()
            Timber.i("SensorManager: CameraSensor started, starting data collection...")
            // Collecter les données
            scope.launch {
                try {
                    cameraSensor.dataFlow.collect { data ->
                        _cameraData.value = data
                        Timber.d("SensorManager: Camera data received - faces=${data.facesDetected}, looking=${data.facesLookingAtScreen}, threat=${data.threatLevel}")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "SensorManager: Camera data collection failed")
                }
            }
            Timber.i("SensorManager: CameraSensor data collection started")
        } catch (e: Exception) {
            Timber.e(e, "SensorManager: Failed to start camera sensor: ${e.message}")
        }
    }
    
    private suspend fun startAudioSensor() {
        try {
            Timber.d("SensorManager: Starting AudioSensor...")
            audioSensor.start()
            Timber.i("SensorManager: AudioSensor started, starting data collection...")
            scope.launch {
                try {
                    audioSensor.dataFlow.collect { data ->
                        _audioData.value = data
                        Timber.d("SensorManager: Audio data received - dB=${data.averageDecibels.toInt()}, speech=${data.isSpeechDetected}, threat=${data.threatLevel}")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "SensorManager: Audio data collection failed")
                }
            }
            Timber.i("SensorManager: AudioSensor data collection started")
        } catch (e: Exception) {
            Timber.e(e, "SensorManager: Failed to start audio sensor: ${e.message}")
        }
    }
    
    private suspend fun startMotionSensor() {
        try {
            motionSensor.start()
            scope.launch {
                motionSensor.dataFlow.collect { data ->
                    _motionData.value = data
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SensorManager: Failed to start motion sensor")
        }
    }
    
    private suspend fun startProximitySensor() {
        try {
            Timber.d("SensorManager: Starting ProximitySensor...")
            proximitySensor.start()
            Timber.i("SensorManager: ProximitySensor started, starting data collection...")
            scope.launch {
                proximitySensor.dataFlow.collect { data ->
                    _proximityData.value = data
                    Timber.v("SensorManager: ProximitySensor data received: distance=${data.distance}cm, threat=${data.threatLevel}")
                }
            }
            Timber.i("SensorManager: ProximitySensor data collection started")
        } catch (e: Exception) {
            Timber.e(e, "SensorManager: Failed to start proximity sensor: ${e.message}")
        }
    }
    
    private suspend fun stopCameraSensor() {
        if (::cameraSensor.isInitialized) {
            cameraSensor.stop()
            cameraSensor.cleanup()
        }
    }
    
    private suspend fun stopAudioSensor() {
        if (::audioSensor.isInitialized) {
            audioSensor.stop()
            audioSensor.cleanup()
        }
    }
    
    private suspend fun stopMotionSensor() {
        if (::motionSensor.isInitialized) {
            motionSensor.stop()
            motionSensor.cleanup()
        }
    }
    
    private suspend fun stopProximitySensor() {
        if (::proximitySensor.isInitialized) {
            proximitySensor.stop()
            proximitySensor.cleanup()
        }
    }
    
    /**
     * Nettoyage complet
     */
    fun cleanup() {
        scope.cancel()
        
        if (::cameraSensor.isInitialized) cameraSensor.cleanup()
        if (::audioSensor.isInitialized) audioSensor.cleanup()
        if (::motionSensor.isInitialized) motionSensor.cleanup()
        if (::proximitySensor.isInitialized) proximitySensor.cleanup()
    }
}

/**
 * Snapshot des données de tous les capteurs à un instant T
 */
data class SensorDataSnapshot(
    val timestamp: Long,
    val cameraData: CameraData?,
    val audioData: AudioData?,
    val motionData: MotionData?,
    val proximityData: ProximityData?
) {
    /**
     * Calcule le niveau de menace global
     * (Simplifié pour MVP, sera amélioré au Jour 3 avec fusion avancée)
     */
    fun calculateOverallThreat(): Pair<ThreatLevel, Float> {
        val threats = listOfNotNull(
            cameraData?.threatLevel,
            audioData?.threatLevel,
            motionData?.threatLevel,
            proximityData?.threatLevel
        )
        
        if (threats.isEmpty()) return ThreatLevel.NONE to 0f
        
        // Prendre le niveau de menace le plus élevé
        val maxThreat = threats.maxOrNull() ?: ThreatLevel.NONE
        
        // Calculer la confiance moyenne
        val confidences = listOfNotNull(
            cameraData?.confidence,
            audioData?.confidence,
            motionData?.confidence,
            proximityData?.confidence
        )
        val avgConfidence = if (confidences.isNotEmpty()) {
            confidences.average().toFloat()
        } else 0f
        
        return maxThreat to avgConfidence
    }
}

