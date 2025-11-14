# Architecture Technique

## 🏗️ Vue d'ensemble de l'Architecture

Privacy Guard utilise une architecture modulaire en couches basée sur les principes de Clean Architecture et MVVM.

## 📊 Diagramme de l'Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ MainActivity │  │ Settings     │  │ Dashboard    │      │
│  │              │  │ Activity     │  │ Activity     │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│         │                  │                  │              │
│  ┌──────▼──────────────────▼──────────────────▼──────┐      │
│  │           ViewModels (MVVM)                        │      │
│  └────────────────────────┬───────────────────────────┘      │
└───────────────────────────┼───────────────────────────────────┘
                            │
┌───────────────────────────▼───────────────────────────────────┐
│                      Domain Layer                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │  Use Cases   │  │  Entities    │  │ Repositories │        │
│  │              │  │              │  │ (Interfaces) │        │
│  └──────────────┘  └──────────────┘  └──────────────┘        │
└───────────────────────────┬───────────────────────────────────┘
                            │
┌───────────────────────────▼───────────────────────────────────┐
│                       Data Layer                               │
│  ┌──────────────────────────────────────────────────────┐     │
│  │            Repository Implementations                 │     │
│  └────────┬─────────────────────────────────────┬───────┘     │
│           │                                      │             │
│  ┌────────▼─────────┐                  ┌────────▼─────────┐   │
│  │  Local Data      │                  │  Preferences     │   │
│  │  Source (Room)   │                  │  Data Source     │   │
│  └──────────────────┘                  └──────────────────┘   │
└───────────────────────────────────────────────────────────────┘
                            │
┌───────────────────────────▼───────────────────────────────────┐
│                    Service Layer (Core)                        │
│                                                                 │
│              ┌─────────────────────────┐                       │
│              │  PrivacyGuardService    │                       │
│              │  (Accessibility +       │                       │
│              │   Foreground Service)   │                       │
│              └──────────┬──────────────┘                       │
│                         │                                       │
│    ┌────────────────────┼────────────────────┐                │
│    │                    │                    │                 │
│ ┌──▼────────┐  ┌────────▼──────┐  ┌─────────▼──────┐         │
│ │ Sensor    │  │  Threat       │  │  Protection    │         │
│ │ Monitors  │  │  Assessment   │  │  Executor      │         │
│ └───────────┘  └───────────────┘  └────────────────┘         │
└─────────────────────────────────────────────────────────────┘
```

## 🔧 Composants Principaux

### 1. PrivacyGuardService (Service Central)

Service Android combinant Accessibility Service et Foreground Service.

**Responsabilités :**
- Gestion du cycle de vie de l'application
- Coordination des différents moniteurs
- Maintien du service en arrière-plan
- Communication avec l'overlay UI

**Fichiers :**
```
app/src/main/java/com/privacyguard/service/
├── PrivacyGuardService.kt          # Service principal
├── ServiceState.kt                  # États du service
└── ServiceBinder.kt                 # Binding pour communication
```

### 2. Sensor Monitors (Moniteurs de Capteurs)

Modules indépendants pour chaque capteur, s'exécutant en parallèle.

#### 2.1 CameraMonitor

**Fonction :** Analyse vidéo en temps réel pour détection de visages

**Détails techniques :**
- Résolution : 320x240 (économie batterie)
- FPS : 5-10 adaptatif selon contexte
- ML Kit pour détection faciale
- Face recognition custom pour identification propriétaire

**Données produites :**
```kotlin
data class CameraDetectionResult(
    val faceCount: Int,
    val unknownFaces: Int,
    val closestFaceDistance: Float, // en cm (estimé)
    val isOwnerPresent: Boolean,
    val gazeDirection: GazeDirection,
    val timestamp: Long
)
```

**Fichiers :**
```
app/src/main/java/com/privacyguard/sensors/camera/
├── CameraMonitor.kt
├── FaceDetector.kt
├── FaceRecognizer.kt
├── GazeEstimator.kt
└── DistanceEstimator.kt
```

#### 2.2 AudioAnalyzer

**Fonction :** Traitement du signal audio pour détection de menaces

**Détails techniques :**
- Sampling : chunks de 100ms
- Analyse FFT pour pattern recognition
- Keyword spotting avec TensorFlow Lite
- Détection de direction du son

**Données produites :**
```kotlin
data class AudioDetectionResult(
    val voiceCount: Int,
    val suspiciousKeywords: List<String>,
    val proximityLevel: ProximityLevel, // NEAR, MEDIUM, FAR
    val soundPatterns: List<SoundPattern>, // FOOTSTEPS, DOOR, WHISPER
    val ambientNoiseLevel: Float,
    val timestamp: Long
)
```

**Fichiers :**
```
app/src/main/java/com/privacyguard/sensors/audio/
├── AudioAnalyzer.kt
├── AudioProcessor.kt
├── KeywordSpotter.kt
├── PatternDetector.kt
└── VoiceCounter.kt
```

#### 2.3 MotionDetector

**Fonction :** Fusion accéléromètre + gyroscope pour détection de mouvements

**Détails techniques :**
- Sampling rate : 50Hz
- Filtrage Kalman pour réduction bruit
- Détection de gestes spécifiques
- Pattern matching pour mouvements suspects

**Données produites :**
```kotlin
data class MotionDetectionResult(
    val suddenMovement: Boolean,
    val orientationChange: Boolean,
    val phoneFlipped: Boolean,
    val peekDetected: Boolean,
    val vibrationLevel: Float,
    val movementPattern: MovementPattern,
    val timestamp: Long
)
```

**Fichiers :**
```
app/src/main/java/com/privacyguard/sensors/motion/
├── MotionDetector.kt
├── AccelerometerProcessor.kt
├── GyroscopeProcessor.kt
├── SensorFusion.kt
└── GestureRecognizer.kt
```

#### 2.4 ProximityWatcher

**Fonction :** Surveillance du capteur de proximité

**Données produites :**
```kotlin
data class ProximityDetectionResult(
    val objectNearby: Boolean,
    val distance: Float, // en cm
    val rapidOccultation: Boolean,
    val occultationCount: Int,
    val timestamp: Long
)
```

**Fichiers :**
```
app/src/main/java/com/privacyguard/sensors/proximity/
├── ProximityWatcher.kt
└── ProximityAnalyzer.kt
```

#### 2.5 LightSensor (Bonus)

**Fonction :** Détection d'ombres et changements de luminosité

**Données produites :**
```kotlin
data class LightDetectionResult(
    val shadowDetected: Boolean,
    val suddenChange: Boolean,
    val lightLevel: Float,
    val changeRate: Float,
    val timestamp: Long
)
```

#### 2.6 LocationMonitor (Bonus)

**Fonction :** Géofencing et adaptation contextuelle

**Données produites :**
```kotlin
data class LocationContext(
    val currentZone: Zone, // HOME, WORK, PUBLIC_TRANSPORT, CAFE, UNKNOWN
    val riskLevel: RiskLevel,
    val isInTrustedZone: Boolean,
    val timestamp: Long
)
```

### 3. Threat Assessment Engine

**Fonction :** Fusion des données de tous les capteurs pour évaluation de menace

**Pipeline :**
```
1. Data Collection (de tous les moniteurs)
   ↓
2. Normalization (mise à l'échelle 0-1)
   ↓
3. Feature Extraction
   ↓
4. ML Inference (TensorFlow Lite model)
   ↓
5. Context Fusion (pondération adaptative)
   ↓
6. Threat Score Calculation (0-100)
   ↓
7. Decision (seuils par mode)
```

**Pondération par défaut :**
- Caméra : 40%
- Audio : 30%
- Mouvement : 20%
- Proximité : 10%
- Luminosité : bonus multiplicateur
- Localisation : modificateur contextuel

**Seuils de déclenchement :**
```kotlin
enum class ProtectionMode(val threshold: Int) {
    PARANOIA(20),      // Très sensible
    BALANCED(50),      // Équilibré
    DISCRETE(75),      // Peu sensible
    TRUST_ZONE(95)     // Presque désactivé
}
```

**Fichiers :**
```
app/src/main/java/com/privacyguard/assessment/
├── ThreatAssessmentEngine.kt
├── SensorDataFusion.kt
├── ThreatScorer.kt
├── MLInferenceEngine.kt
└── DecisionTree.kt
```

### 4. Protection Executor

**Fonction :** Exécution des actions de protection

**Niveaux d'action :**

#### Niveau 1 : Masquage Doux
```kotlin
class SoftMaskingProtection : ProtectionAction {
    override suspend fun execute(context: Context) {
        // Flou gaussien progressif (0.3s)
        overlayManager.applyBlur(
            intensity = 0f..25f,
            duration = 300.milliseconds
        )
    }
}
```

#### Niveau 2 : Écran Leurre
```kotlin
class DecoyScreenProtection : ProtectionAction {
    override suspend fun execute(context: Context) {
        // Bascule vers contenu leurre
        overlayManager.showDecoyContent(
            contentType = userPreferences.decoyType
        )
    }
}
```

#### Niveau 3 : Verrouillage Instantané
```kotlin
class InstantLockProtection : ProtectionAction {
    override suspend fun execute(context: Context) {
        // Verrouillage complet
        screenManager.lock()
        notificationManager.showDiscreetNotification()
        screenshotBlocker.enable()
    }
}
```

#### Niveau 4 : Mode Panique
```kotlin
class PanicModeProtection : ProtectionAction {
    override suspend fun execute(context: Context) {
        // Fermeture app + nettoyage
        appManager.closeCurrentApp()
        memoryManager.clearSensitiveData()
        navigationManager.goToHomeScreen()
        historyManager.clearRecentHistory()
    }
}
```

**Fichiers :**
```
app/src/main/java/com/privacyguard/protection/
├── ProtectionExecutor.kt
├── ProtectionAction.kt
├── actions/
│   ├── SoftMaskingProtection.kt
│   ├── DecoyScreenProtection.kt
│   ├── InstantLockProtection.kt
│   └── PanicModeProtection.kt
├── OverlayManager.kt
└── ScreenshotBlocker.kt
```

## 🗄️ Modèle de Données

### Entities

```kotlin
// Événement de détection
@Entity(tableName = "detection_events")
data class DetectionEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val threatScore: Int,
    val cameraData: String?, // JSON serialized
    val audioData: String?,
    val motionData: String?,
    val protectionLevel: ProtectionLevel,
    val wasBlocked: Boolean,
    val location: String?
)

// Configuration d'application
@Entity(tableName = "app_configs")
data class AppConfig(
    @PrimaryKey
    val packageName: String,
    val sensitivityLevel: SensitivityLevel,
    val protectionLevel: ProtectionLevel,
    val isProtected: Boolean
)

// Zone de confiance
@Entity(tableName = "trust_zones")
data class TrustZone(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float, // en mètres
    val autoDisable: Boolean
)

// Visage de confiance
@Entity(tableName = "trusted_faces")
data class TrustedFace(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val faceEncoding: ByteArray, // ML Kit face encoding
    val addedTimestamp: Long,
    val lastSeenTimestamp: Long,
    val verificationCount: Int
)
```

### Database

```kotlin
@Database(
    entities = [
        DetectionEvent::class,
        AppConfig::class,
        TrustZone::class,
        TrustedFace::class
    ],
    version = 1,
    exportSchema = true
)
abstract class PrivacyGuardDatabase : RoomDatabase() {
    abstract fun detectionEventDao(): DetectionEventDao
    abstract fun appConfigDao(): AppConfigDao
    abstract fun trustZoneDao(): TrustZoneDao
    abstract fun trustedFaceDao(): TrustedFaceDao
}
```

## 🔄 Flux de Données

### Pipeline de Détection (Temps Réel)

```kotlin
// Pseudo-code du flux principal
class PrivacyGuardService : AccessibilityService() {
    
    private val detectionFlow = combine(
        cameraMonitor.detectionFlow,
        audioAnalyzer.detectionFlow,
        motionDetector.detectionFlow,
        proximityWatcher.detectionFlow,
        lightSensor.detectionFlow,
        locationMonitor.contextFlow
    ) { camera, audio, motion, proximity, light, location ->
        SensorData(camera, audio, motion, proximity, light, location)
    }
    
    init {
        lifecycleScope.launch {
            detectionFlow
                .debounce(50.milliseconds) // Anti-rebond
                .map { sensorData ->
                    threatAssessmentEngine.evaluate(sensorData)
                }
                .filter { threatScore ->
                    threatScore > currentMode.threshold
                }
                .distinctUntilChanged() // Éviter déclenchements répétés
                .collect { threatScore ->
                    protectionExecutor.execute(
                        level = determineProtectionLevel(threatScore)
                    )
                }
        }
    }
}
```

## ⚡ Optimisations Performance

### 1. Gestion Batterie

```kotlin
class AdaptiveSamplingStrategy {
    fun getOptimalFrameRate(context: BatteryContext): Int {
        return when {
            context.screenOff -> 1 // 1 fps
            context.batteryLow -> 3 // 3 fps
            context.powerSaveMode -> 5 // 5 fps
            else -> 10 // 10 fps
        }
    }
}
```

### 2. Gestion Mémoire

- Modèles ML quantifiés int8 (< 5MB chacun)
- Buffer circulaire pour audio (3 secondes max)
- Pas de stockage d'images (analyse streaming uniquement)
- Cache avec LRU pour résultats ML

### 3. Threading Strategy

```kotlin
// Dispatcher personnalisé pour ML inference
val mlDispatcher = Executors.newSingleThreadExecutor {
    Thread(it, "ML-Inference").apply {
        priority = Thread.MAX_PRIORITY
    }
}.asCoroutineDispatcher()

// Dispatchers par type de tâche
val cameraDispatcher = Dispatchers.Default
val audioDispatcher = Dispatchers.IO
val motionDispatcher = Dispatchers.Default
val uiDispatcher = Dispatchers.Main
```

### 4. NPU Acceleration (si disponible)

```kotlin
class MLInferenceEngine {
    private val interpreter = try {
        // Tenter d'utiliser le NPU
        Interpreter(
            modelFile,
            Interpreter.Options().apply {
                setUseNNAPI(true) // Neural Networks API
                setNumThreads(2)
            }
        )
    } catch (e: Exception) {
        // Fallback CPU
        Interpreter(modelFile)
    }
}
```

## 📦 Structure des Modules

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/privacyguard/
│   │   │   ├── service/          # Service principal
│   │   │   ├── sensors/          # Moniteurs de capteurs
│   │   │   │   ├── camera/
│   │   │   │   ├── audio/
│   │   │   │   ├── motion/
│   │   │   │   ├── proximity/
│   │   │   │   ├── light/
│   │   │   │   └── location/
│   │   │   ├── assessment/       # Évaluation des menaces
│   │   │   ├── protection/       # Exécution protection
│   │   │   ├── ml/               # Modèles ML
│   │   │   ├── ui/               # Interface utilisateur
│   │   │   │   ├── overlay/
│   │   │   │   ├── dashboard/
│   │   │   │   └── settings/
│   │   │   ├── data/             # Data layer
│   │   │   │   ├── database/
│   │   │   │   ├── repository/
│   │   │   │   └── preferences/
│   │   │   ├── domain/           # Domain layer
│   │   │   │   ├── entities/
│   │   │   │   ├── usecases/
│   │   │   │   └── repository/
│   │   │   ├── di/               # Dependency Injection
│   │   │   └── utils/            # Utilitaires
│   │   ├── res/                  # Ressources
│   │   └── assets/               # ML models
│   │       └── models/
│   │           ├── face_detection.tflite
│   │           ├── face_recognition.tflite
│   │           ├── keyword_spotting.tflite
│   │           └── threat_assessment.tflite
│   └── test/                     # Tests unitaires
└── build.gradle.kts
```

## 🔐 Sécurité Architecture

### Isolation des Composants

Chaque module de capteur est isolé et ne peut accéder qu'à ses propres données. La communication se fait uniquement via le ThreatAssessmentEngine.

### Chiffrement des Données

```kotlin
class SecureStorage {
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        "secure_prefs",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}
```

### Zero Trust Architecture

Aucune donnée sensible n'est conservée plus longtemps que nécessaire. Les images/audio sont analysés en streaming et immédiatement détruits.

## 📊 Latence Cible

| Étape | Latence Max | Stratégie |
|-------|-------------|-----------|
| Acquisition capteur | < 50ms | Sampling parallèle |
| Pre-processing | < 30ms | Optimisation native |
| ML Inference | < 80ms | NPU/GPU acceleration |
| Decision | < 10ms | Lookup tables |
| Action | < 30ms | UI thread prioritaire |
| **TOTAL** | **< 200ms** | Pipeline asynchrone |

---

Cette architecture garantit une détection rapide, une faible consommation de ressources, et une protection efficace de la vie privée.

