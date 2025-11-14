# Capteurs et Système de Détection

## 📹 1. Caméra Frontale

### Objectifs
- Détection de visages multiples dans l'environnement
- Reconnaissance propriétaire vs inconnu
- Estimation de distance des visages
- Eye-tracking léger pour détection de regard
- Analyse comportementale (approche lente vs rapide)

### Spécifications Techniques

```kotlin
object CameraConfig {
    const val RESOLUTION_WIDTH = 320
    const val RESOLUTION_HEIGHT = 240
    const val BASE_FPS = 10
    const val LOW_POWER_FPS = 5
    const val MINIMAL_FPS = 1 // Screen off
    const val FORMAT = ImageFormat.YUV_420_888
}
```

### Pipeline de Traitement

```
1. Capture Frame (320x240 YUV)
   ↓
2. Face Detection (ML Kit)
   ├── Nombre de visages
   ├── Bounding boxes
   └── Landmarks (yeux, nez, bouche)
   ↓
3. Face Recognition (Custom Model)
   ├── Extraction features (128-d vector)
   ├── Comparaison avec database
   └── Identification (Owner/Unknown)
   ↓
4. Distance Estimation
   ├── Taille du bounding box
   ├── Inter-ocular distance
   └── Estimation en cm
   ↓
5. Gaze Estimation
   ├── Position des pupilles
   ├── Orientation de la tête
   └── Direction du regard
   ↓
6. Behavior Analysis
   ├── Vitesse d'approche
   ├── Temps de fixation
   └── Pattern de mouvement
```

### Modèles ML Utilisés

#### Face Detection
```kotlin
val faceDetectorOptions = FaceDetectorOptions.Builder()
    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
    .setMinFaceSize(0.1f) // 10% de l'image
    .enableTracking() // Track faces across frames
    .build()
```

#### Face Recognition
- Modèle custom basé sur FaceNet
- Output: 128-dimensional embedding
- Distance metric: Cosine similarity
- Threshold: 0.85 pour match

#### Gaze Estimation
- Input: Eye landmarks + head pose
- Output: (azimuth, elevation) angles
- Précision: ±15 degrees

### Distance Estimation Algorithm

```kotlin
class DistanceEstimator {
    // Constante calibrée (distance inter-oculaire moyenne: 63mm)
    private const val AVERAGE_IPD_MM = 63f
    
    // Focal length estimée de la caméra frontale
    private const val FOCAL_LENGTH_PX = 500f
    
    fun estimateDistance(leftEye: PointF, rightEye: PointF): Float {
        // Distance inter-oculaire en pixels
        val ipdPixels = sqrt(
            (rightEye.x - leftEye.x).pow(2) + 
            (rightEye.y - leftEye.y).pow(2)
        )
        
        // Formule de triangulation
        val distanceMm = (AVERAGE_IPD_MM * FOCAL_LENGTH_PX) / ipdPixels
        
        return distanceMm / 10f // Convertir en cm
    }
}
```

### Output Data Structure

```kotlin
data class CameraDetectionResult(
    val timestamp: Long,
    val faces: List<DetectedFace>,
    val ownerPresent: Boolean,
    val unknownFaceCount: Int,
    val closestFaceDistance: Float, // cm
    val threatLevel: Float // 0-1
)

data class DetectedFace(
    val trackingId: Int,
    val boundingBox: Rect,
    val distance: Float,
    val isOwner: Boolean,
    val gazeDirection: GazeDirection,
    val approachSpeed: Float, // cm/s
    val fixationDuration: Long // ms
)

enum class GazeDirection {
    AT_SCREEN,      // Regarde l'écran
    AWAY,           // Regarde ailleurs
    GLANCING,       // Coup d'œil rapide
    STARING         // Fixation prolongée
}
```

### Optimisations

**Adaptive Frame Rate:**
```kotlin
class AdaptiveCameraManager {
    fun computeOptimalFps(context: DeviceContext): Int {
        return when {
            !context.screenOn -> 1
            context.batteryLevel < 15 -> 3
            context.powerSaveMode -> 5
            context.isCharging -> 15
            context.thermalState == CRITICAL -> 3
            else -> 10
        }
    }
}
```

**Region of Interest:**
```kotlin
// Concentrer l'analyse sur zone à risque
val roi = when (screenOrientation) {
    PORTRAIT -> Rect(0, 0, width, height / 2) // Moitié supérieure
    LANDSCAPE -> Rect(0, 0, width, height) // Tout l'écran
}
```

## 🎤 2. Microphone

### Objectifs
- Détection de voix multiples
- Patterns audio spécifiques (pas, porte, chuchotements)
- Mots-clés suspects
- Analyse de proximité sonore
- Détection de changements d'ambiance

### Spécifications Techniques

```kotlin
object AudioConfig {
    const val SAMPLE_RATE = 16000 // Hz (suffisant pour voix)
    const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
    const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    const val CHUNK_SIZE_MS = 100 // Analyse par chunks de 100ms
    const val BUFFER_SIZE_MS = 3000 // Buffer circulaire 3s
}
```

### Pipeline de Traitement

```
1. Audio Capture (16kHz, Mono)
   ↓
2. Pre-processing
   ├── Noise Reduction
   ├── Normalization
   └── VAD (Voice Activity Detection)
   ↓
3. Feature Extraction
   ├── MFCC (Mel-Frequency Cepstral Coefficients)
   ├── Spectral features
   └── Temporal features
   ↓
4. Analysis
   ├── Voice Counting (Speaker Diarization)
   ├── Keyword Spotting (TFLite model)
   ├── Pattern Detection (FFT analysis)
   └── Proximity Estimation (Volume + freq)
   ↓
5. Threat Assessment
```

### Voice Counting

```kotlin
class VoiceCounter {
    private val speakerEmbedding = SpeakerEmbeddingModel()
    
    fun countVoices(audioChunks: List<FloatArray>): Int {
        val embeddings = audioChunks
            .filter { hasVoiceActivity(it) }
            .map { chunk -> speakerEmbedding.extract(chunk) }
        
        // Clustering des embeddings
        val clusters = dbscan(embeddings, eps = 0.3, minSamples = 2)
        
        return clusters.size
    }
}
```

### Keyword Spotting

**Mots-clés suspects configurables:**
```kotlin
val suspiciousKeywords = listOf(
    // Français
    "regarde", "qu'est-ce que tu fais", "montre-moi", 
    "c'est quoi", "tu fais quoi", "laisse-moi voir",
    
    // Anglais
    "look", "what are you doing", "show me",
    "let me see", "what's that",
    
    // Contextuels
    "police", "contrôle", "vérification"
)
```

**Implémentation:**
```kotlin
class KeywordSpotter {
    private val model = loadTFLiteModel("keyword_spotting.tflite")
    
    fun detect(audioFeatures: FloatArray): List<DetectedKeyword> {
        val scores = model.run(audioFeatures)
        
        return keywords.indices
            .filter { scores[it] > CONFIDENCE_THRESHOLD }
            .map { DetectedKeyword(keywords[it], scores[it]) }
    }
}
```

### Pattern Detection

```kotlin
enum class SoundPattern(val description: String) {
    FOOTSTEPS("Pas qui s'approchent"),
    DOOR_OPENING("Porte qui s'ouvre"),
    DOOR_CLOSING("Porte qui se ferme"),
    CHAIR_MOVING("Chaise qui bouge"),
    WHISPER("Chuchotement"),
    CROWD("Foule/groupe de personnes"),
    SILENCE("Silence soudain suspect"),
    KEYBOARD("Frappe clavier (quelqu'un travaille à côté)")
}

class PatternDetector {
    fun detect(audioBuffer: FloatArray): List<SoundPattern> {
        val fft = computeFFT(audioBuffer)
        val spectralCentroid = computeSpectralCentroid(fft)
        val zeroCrossingRate = computeZCR(audioBuffer)
        val rmsEnergy = computeRMS(audioBuffer)
        
        return buildList {
            // Footsteps: low freq, periodic
            if (detectPeriodicity(fft, 1.5f..2.5f)) {
                add(SoundPattern.FOOTSTEPS)
            }
            
            // Door: sudden broadband noise + decay
            if (detectImpulse(audioBuffer) && hasDecay(rmsEnergy)) {
                add(SoundPattern.DOOR_OPENING)
            }
            
            // Whisper: high ZCR, low energy, mid-high freq
            if (zeroCrossingRate > 0.3f && rmsEnergy < 0.1f && spectralCentroid > 2000) {
                add(SoundPattern.WHISPER)
            }
            
            // Silence soudain (suspect)
            if (rmsEnergy < 0.05f && previousRmsEnergy > 0.2f) {
                add(SoundPattern.SILENCE)
            }
        }
    }
}
```

### Proximity Estimation

```kotlin
class AudioProximityEstimator {
    fun estimate(audioLevel: Float, frequency: Float): ProximityLevel {
        // Sons proches: volume élevé + fréquences aiguës préservées
        return when {
            audioLevel > 0.7f && frequency > 4000 -> ProximityLevel.VERY_CLOSE
            audioLevel > 0.5f && frequency > 2000 -> ProximityLevel.CLOSE
            audioLevel > 0.3f -> ProximityLevel.MEDIUM
            else -> ProximityLevel.FAR
        }
    }
}
```

### Output Data Structure

```kotlin
data class AudioDetectionResult(
    val timestamp: Long,
    val voiceCount: Int,
    val detectedKeywords: List<DetectedKeyword>,
    val soundPatterns: List<SoundPattern>,
    val proximityLevel: ProximityLevel,
    val ambientNoiseLevel: Float,
    val threatLevel: Float
)

data class DetectedKeyword(
    val keyword: String,
    val confidence: Float,
    val timestamp: Long
)
```

## 📱 3. Accéléromètre + Gyroscope

### Objectifs
- Détection mouvements brusques
- Changements d'orientation soudains
- Détection de "peek" (soulever/reposer rapidement)
- Téléphone retourné
- Vibrations inhabituelles

### Spécifications Techniques

```kotlin
object MotionConfig {
    const val SAMPLE_RATE = 50 // Hz
    const val BUFFER_SIZE = 100 // 2 secondes de données
    const val GRAVITY = 9.81f // m/s²
}
```

### Sensor Fusion

```kotlin
class SensorFusion {
    private val kalmanFilter = KalmanFilter()
    
    fun fuse(
        accel: FloatArray,
        gyro: FloatArray,
        timestamp: Long
    ): FusedMotionData {
        // Filtrage Kalman pour réduire bruit
        val filteredAccel = kalmanFilter.filter(accel)
        val filteredGyro = kalmanFilter.filter(gyro)
        
        // Calcul de l'orientation
        val orientation = computeOrientation(filteredAccel, filteredGyro)
        
        // Détection de mouvement
        val movement = detectMovement(filteredAccel, filteredGyro)
        
        return FusedMotionData(
            acceleration = filteredAccel,
            angularVelocity = filteredGyro,
            orientation = orientation,
            movement = movement,
            timestamp = timestamp
        )
    }
}
```

### Gesture Detection

```kotlin
class GestureDetector {
    fun detect(motionHistory: List<FusedMotionData>): MotionGesture? {
        return when {
            detectSuddenMovement(motionHistory) -> MotionGesture.SUDDEN_GRAB
            detectFlip(motionHistory) -> MotionGesture.PHONE_FLIPPED
            detectPeek(motionHistory) -> MotionGesture.PEEK
            detectDrop(motionHistory) -> MotionGesture.DROPPED
            detectShake(motionHistory) -> MotionGesture.SHAKE
            detectRotation(motionHistory) -> MotionGesture.ROTATION
            else -> null
        }
    }
    
    private fun detectSuddenMovement(history: List<FusedMotionData>): Boolean {
        // Accélération soudaine > 20 m/s²
        val recentAccel = history.takeLast(10)
        val maxAccel = recentAccel.maxOf { it.acceleration.magnitude() }
        return maxAccel > 20f
    }
    
    private fun detectPeek(history: List<FusedMotionData>): Boolean {
        // Pattern: soulevé rapidement puis reposé
        // 1. Accélération vers le haut
        // 2. Stabilisation brève (< 500ms)
        // 3. Décélération vers le bas
        
        if (history.size < 50) return false
        
        val last2Seconds = history.takeLast(100)
        val hasUpwardAccel = last2Seconds.take(30).any { 
            it.acceleration[2] > 12f // Z-axis
        }
        val hasDownwardAccel = last2Seconds.takeLast(30).any {
            it.acceleration[2] < 8f
        }
        val briefStable = last2Seconds.subList(30, 70).all {
            abs(it.acceleration[2] - GRAVITY) < 2f
        }
        
        return hasUpwardAccel && briefStable && hasDownwardAccel
    }
    
    private fun detectFlip(history: List<FusedMotionData>): Boolean {
        // Changement d'orientation de 180° en Z
        val first = history.first().orientation
        val last = history.last().orientation
        val angleDiff = abs(last.z - first.z)
        return angleDiff > 160f && angleDiff < 200f
    }
}
```

### Output Data Structure

```kotlin
data class MotionDetectionResult(
    val timestamp: Long,
    val gesture: MotionGesture?,
    val orientation: Orientation,
    val isStable: Boolean,
    val vibrationLevel: Float,
    val threatLevel: Float
)

enum class MotionGesture {
    SUDDEN_GRAB,    // Quelqu'un attrape le téléphone
    PHONE_FLIPPED,  // Retourné face cachée
    PEEK,           // Soulevé puis reposé rapidement
    DROPPED,        // Tombé
    SHAKE,          // Secoué
    ROTATION        // Rotation rapide
}
```

## 🔍 4. Capteur de Proximité

### Objectifs
- Détection d'objets proches de l'écran
- Main qui passe devant
- Occultation répétée (quelqu'un essaie de voir)

### Implémentation

```kotlin
class ProximityWatcher(private val sensor: Sensor) {
    private val occultationHistory = CircularBuffer<Long>(capacity = 10)
    
    fun onSensorChanged(event: SensorEvent) {
        val distance = event.values[0] // cm
        val isNear = distance < sensor.maximumRange * 0.2f // < 20% de la portée max
        
        if (isNear) {
            occultationHistory.add(System.currentTimeMillis())
            
            // Détection d'occultation répétée
            if (isRapidOccultation()) {
                onThreatDetected(ThreatType.RAPID_OCCULTATION)
            }
        }
    }
    
    private fun isRapidOccultation(): Boolean {
        // 3+ occultations en moins de 5 secondes
        if (occultationHistory.size < 3) return false
        
        val timespan = occultationHistory.last() - occultationHistory.first()
        return timespan < 5000 // ms
    }
}
```

## 💡 5. Capteur de Luminosité

### Objectifs
- Détection d'ombres (personne qui se penche)
- Changements brusques d'exposition

### Implémentation

```kotlin
class LightSensorAnalyzer {
    private val lightHistory = CircularBuffer<Float>(capacity = 50)
    
    fun analyze(lightLevel: Float): LightDetectionResult {
        lightHistory.add(lightLevel)
        
        val shadowDetected = detectShadow(lightLevel)
        val suddenChange = detectSuddenChange(lightLevel)
        
        return LightDetectionResult(
            timestamp = System.currentTimeMillis(),
            shadowDetected = shadowDetected,
            suddenChange = suddenChange,
            lightLevel = lightLevel,
            changeRate = computeChangeRate(),
            threatLevel = computeThreatLevel(shadowDetected, suddenChange)
        )
    }
    
    private fun detectShadow(current: Float): Boolean {
        if (lightHistory.size < 10) return false
        
        val baseline = lightHistory.takeLast(10).average()
        val dropPercentage = (baseline - current) / baseline
        
        // Baisse de 30%+ = ombre
        return dropPercentage > 0.3f
    }
    
    private fun detectSuddenChange(current: Float): Boolean {
        if (lightHistory.size < 2) return false
        
        val previous = lightHistory[lightHistory.size - 2]
        val changeMagnitude = abs(current - previous) / previous
        
        // Changement de 50%+ en 1 sample
        return changeMagnitude > 0.5f
    }
}
```

## 📍 6. GPS + Géofencing

### Objectifs
- Définir zones à haut risque
- Adaptation automatique de la sensibilité
- Apprentissage des lieux fréquents

### Implémentation

```kotlin
class LocationMonitor {
    private val geofencingClient = LocationServices.getGeofencingClient(context)
    
    fun setupGeofences(zones: List<TrustZone>) {
        val geofences = zones.map { zone ->
            Geofence.Builder()
                .setRequestId(zone.id.toString())
                .setCircularRegion(zone.latitude, zone.longitude, zone.radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or
                    Geofence.GEOFENCE_TRANSITION_EXIT
                )
                .build()
        }
        
        geofencingClient.addGeofences(
            GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofences)
                .build(),
            geofencePendingIntent
        )
    }
    
    fun onGeofenceTransition(geofenceTransition: GeofencingEvent) {
        when (geofenceTransition.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                val zone = getZoneById(geofenceTransition.triggeringGeofences[0].requestId)
                applyZoneSettings(zone)
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                resetToDefaultSettings()
            }
        }
    }
}
```

### Auto-Learning Locations

```kotlin
class LocationLearner {
    fun learnFrequentLocations() {
        val locationHistory = locationDao.getLastMonth()
        
        // Clustering des localisations
        val clusters = dbscan(
            locationHistory.map { Point(it.latitude, it.longitude) },
            eps = 0.001, // ~100m
            minSamples = 10
        )
        
        clusters.forEach { cluster ->
            val centroid = cluster.centroid()
            val visits = cluster.size
            val avgDuration = cluster.map { it.duration }.average()
            
            // Si visité souvent et temps long = probable maison/travail
            if (visits > 20 && avgDuration > 30.minutes) {
                suggestTrustZone(
                    location = centroid,
                    type = inferZoneType(cluster),
                    confidence = computeConfidence(visits, avgDuration)
                )
            }
        }
    }
}
```

---

Cette architecture de capteurs fournit une détection multi-modale robuste et adaptative.

