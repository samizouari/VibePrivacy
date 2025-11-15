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
 * 
 * ⚠️ LIMITATION HARDWARE :
 * La plupart des téléphones Android ont un capteur de proximité BINAIRE :
 * - 0cm = objet très proche (< 1cm)
 * - maxRange (généralement 5cm) = rien de proche
 * 
 * UTILITÉ DANS L'APP :
 * - Indicateur complémentaire (poids 10% dans fusion)
 * - Détecte main/visage très proche quand distance = 0cm
 * - Complète la caméra si elle ne voit pas (angle, obscurité)
 * - Détecte occultations rapides (main passant devant)
 * 
 * NOTE : Ce capteur est LIMITÉ mais UTILE comme signal complémentaire.
 * Il ne remplace pas la caméra mais ajoute une couche de détection.
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
        Timber.d("ProximitySensor: onStart() called")
        
        if (proximitySensor == null) {
            Timber.w("ProximitySensor: ⚠️ Proximity sensor NOT AVAILABLE on this device")
            Timber.w("ProximitySensor: This device does not have a proximity sensor hardware")
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
        
        Timber.i("ProximitySensor: ✅ Proximity sensor hardware found, starting detection...")
        
        // Récupérer la portée maximale du capteur
        maxRange = proximitySensor.maximumRange
        Timber.i("ProximitySensor: Max range = ${maxRange}cm")
        
        sensorManager.registerListener(
            this,
            proximitySensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        
        Timber.i("ProximitySensor: ✅ Proximity detection started and listener registered")
    }
    
    override suspend fun onStop() {
        if (proximitySensor == null) return
        
        Timber.d("ProximitySensor: Stopping proximity detection...")
        
        sensorManager.unregisterListener(this)
        
        Timber.i("ProximitySensor: Proximity detection stopped")
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_PROXIMITY) {
            Timber.w("ProximitySensor: Received sensor event but type is not PROXIMITY: ${event?.sensor?.type}")
            return
        }
        
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
        
        val sensorType = if ((distance == 0f || distance == maxRange) && maxRange > 0f) "binary" else "continuous"
        Timber.d("ProximitySensor: 📊 Distance=${distance}cm, isNear=$isNear, threat=$threatLevel, type=$sensorType, confidence=$confidence")
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Pas besoin de gérer les changements de précision
    }
    
    /**
     * Évalue le niveau de menace selon la distance de proximité
     * 
     * Note: Certains capteurs Android sont binaires (0.0 = proche, maxRange = loin)
     * On adapte la logique en conséquence.
     */
    private fun evaluateThreatLevel(distance: Float): Pair<ThreatLevel, Float> {
        // Si le capteur est binaire (distance = 0 ou = maxRange)
        val isBinarySensor = (distance == 0f || distance == maxRange) && maxRange > 0f
        
        return when {
            // Capteur binaire : 0 = très proche = haute menace
            isBinarySensor && distance == 0f -> ThreatLevel.HIGH to 0.9f
            
            // Capteur binaire : maxRange = loin = aucune menace
            isBinarySensor && distance == maxRange -> ThreatLevel.NONE to 1.0f
            
            // Capteur continu : Objet très proche (< 1cm) = haute menace
            !isBinarySensor && distance < veryNearThreshold -> ThreatLevel.HIGH to 0.9f
            
            // Capteur continu : Objet proche (< 3cm) = menace moyenne
            !isBinarySensor && distance < nearThreshold -> ThreatLevel.MEDIUM to 0.75f
            
            // Capteur continu : Objet à portée mais pas très proche = faible menace
            !isBinarySensor && distance < maxRange -> ThreatLevel.LOW to 0.5f
            
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

