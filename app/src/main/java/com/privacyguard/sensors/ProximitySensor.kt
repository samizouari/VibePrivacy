package com.privacyguard.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import timber.log.Timber

/**
 * Capteur de proximité
 * 
 * Détecte la présence d'objets proches du téléphone.
 * Utile pour :
 * - Détecter quelqu'un qui s'approche très près
 * - Détecter la main devant l'écran
 * - Compléter la détection caméra
 * 
 * Note: Le capteur de proximité a généralement une portée limitée (5-10cm).
 */
class ProximitySensor(context: Context) : BaseSensor<ProximityData>(context, "ProximitySensor"), SensorEventListener {
    
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private var maxRange = 5.0f // Par défaut 5cm, sera mis à jour avec la vraie valeur
    
    // Seuils de proximité (en cm)
    private val veryNearThreshold = 1.0f // < 1cm = très proche
    private val nearThreshold = 3.0f // < 3cm = proche
    
    override suspend fun onStart() {
        if (proximitySensor == null) {
            Timber.w("ProximitySensor: Proximity sensor not available on this device")
            // Émettre des données par défaut (pas de capteur)
            emitData(
                ProximityData(
                    timestamp = System.currentTimeMillis(),
                    threatLevel = ThreatLevel.NONE,
                    confidence = 0f,
                    distance = Float.MAX_VALUE,
                    isNear = false,
                    maxRange = 0f
                )
            )
            return
        }
        
        Timber.d("ProximitySensor: Starting proximity detection...")
        
        // Récupérer la portée maximale du capteur
        maxRange = proximitySensor.maximumRange
        Timber.d("ProximitySensor: Max range = ${maxRange}cm")
        
        sensorManager.registerListener(
            this,
            proximitySensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        
        Timber.i("ProximitySensor: Proximity detection started")
    }
    
    override suspend fun onStop() {
        if (proximitySensor == null) return
        
        Timber.d("ProximitySensor: Stopping proximity detection...")
        
        sensorManager.unregisterListener(this)
        
        Timber.i("ProximitySensor: Proximity detection stopped")
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_PROXIMITY) return
        
        val timestamp = System.currentTimeMillis()
        
        // Distance en cm (ou binaire 0/maxRange sur certains devices)
        val distance = event.values[0]
        
        // Déterminer si un objet est proche
        val isNear = distance < nearThreshold
        
        // Évaluer le niveau de menace
        val (threatLevel, confidence) = evaluateThreatLevel(distance)
        
        // Émettre les données
        emitData(
            ProximityData(
                timestamp = timestamp,
                threatLevel = threatLevel,
                confidence = confidence,
                distance = distance,
                isNear = isNear,
                maxRange = maxRange
            )
        )
        
        Timber.v("ProximitySensor: Distance=${distance}cm, isNear=$isNear, threat=$threatLevel")
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Pas besoin de gérer les changements de précision
    }
    
    /**
     * Évalue le niveau de menace selon la distance de proximité
     */
    private fun evaluateThreatLevel(distance: Float): Pair<ThreatLevel, Float> {
        return when {
            // Objet très proche (< 1cm) = haute menace
            distance < veryNearThreshold -> ThreatLevel.HIGH to 0.9f
            
            // Objet proche (< 3cm) = menace moyenne
            distance < nearThreshold -> ThreatLevel.MEDIUM to 0.75f
            
            // Objet à portée du capteur mais pas très proche = faible menace
            distance < maxRange -> ThreatLevel.LOW to 0.5f
            
            // Rien de proche = aucune menace
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

