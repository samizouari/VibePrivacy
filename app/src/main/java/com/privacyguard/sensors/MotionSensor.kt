package com.privacyguard.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.math.sqrt

/**
 * Capteur de mouvement (accéléromètre)
 * 
 * Détecte :
 * - Mouvements du téléphone (quelqu'un le manipule)
 * - Secousses (téléphone pris brusquement)
 * - Immobilité (téléphone posé)
 * 
 * Utile pour détecter si quelqu'un prend votre téléphone pendant
 * que vous êtes distrait.
 */
class MotionSensor(context: Context) : BaseSensor<MotionData>(context, "MotionSensor"), SensorEventListener {
    
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Seuils de mouvement (en m/s²)
    private val immobileThreshold = 0.5f // En dessous = immobile
    private val lightMovementThreshold = 2.0f // Mouvement léger
    private val moderateMovementThreshold = 5.0f // Mouvement modéré
    private val strongMovementThreshold = 10.0f // Mouvement fort
    
    // Historique pour détecter les changements
    private var lastMagnitude = 0f
    private var lastUpdateTime = 0L
    
    override suspend fun onStart() {
        if (accelerometer == null) {
            Timber.e("MotionSensor: Accelerometer not available on this device")
            throw IllegalStateException("Accelerometer not available")
        }
        
        Timber.d("MotionSensor: Starting motion detection...")
        
        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL // ~200ms entre les mises à jour
        )
        
        Timber.i("MotionSensor: Motion detection started")
    }
    
    override suspend fun onStop() {
        Timber.d("MotionSensor: Stopping motion detection...")
        
        sensorManager.unregisterListener(this)
        
        Timber.i("MotionSensor: Motion detection stopped")
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        
        val timestamp = System.currentTimeMillis()
        
        // Valeurs d'accélération (en m/s²)
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        
        // Soustraire la gravité pour obtenir l'accélération linéaire
        // Gravité terrestre ≈ 9.81 m/s²
        val gravity = 9.81f
        val xLinear = x
        val yLinear = y
        val zLinear = z - gravity
        
        // Calculer la magnitude du vecteur d'accélération
        val magnitude = sqrt(xLinear * xLinear + yLinear * yLinear + zLinear * zLinear)
        
        // Normaliser l'intensité entre 0.0 et 1.0
        val intensity = (magnitude / strongMovementThreshold).coerceIn(0f, 1f)
        
        // Déterminer si en mouvement
        val isMoving = magnitude > immobileThreshold
        
        // Évaluer le niveau de menace
        val (threatLevel, confidence) = evaluateThreatLevel(magnitude, lastMagnitude, timestamp - lastUpdateTime)
        
        // Émettre les données
        emitData(
            MotionData(
                timestamp = timestamp,
                threatLevel = threatLevel,
                confidence = confidence,
                accelerationX = xLinear,
                accelerationY = yLinear,
                accelerationZ = zLinear,
                magnitude = magnitude,
                isMoving = isMoving,
                movementIntensity = intensity
            )
        )
        
        Timber.v("MotionSensor: Magnitude=${magnitude.toInt()}, threat=$threatLevel")
        
        // Mettre à jour l'historique
        lastMagnitude = magnitude
        lastUpdateTime = timestamp
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Pas besoin de gérer les changements de précision
    }
    
    /**
     * Évalue le niveau de menace selon le mouvement
     */
    private fun evaluateThreatLevel(
        magnitude: Float,
        previousMagnitude: Float,
        deltaTime: Long
    ): Pair<ThreatLevel, Float> {
        // Calculer le changement d'accélération
        val accelerationChange = Math.abs(magnitude - previousMagnitude)
        
        return when {
            // Mouvement très fort ou changement brutal = haute menace
            magnitude > strongMovementThreshold || accelerationChange > 8.0f -> 
                ThreatLevel.HIGH to 0.85f
            
            // Mouvement modéré = menace moyenne
            magnitude > moderateMovementThreshold -> 
                ThreatLevel.MEDIUM to 0.7f
            
            // Mouvement léger = faible menace
            magnitude > lightMovementThreshold -> 
                ThreatLevel.LOW to 0.5f
            
            // Immobile = aucune menace
            else -> ThreatLevel.NONE to 1.0f
        }
    }
    
    /**
     * Nettoyage des ressources
     */
    fun cleanup() {
        scope.cancel()
    }
}

