# Structure Détaillée du Projet Android

## 📁 Arborescence Complète

```
VibePrivacy/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/privacyguard/
│   │   │   │   │
│   │   │   │   ├── PrivacyGuardApplication.kt
│   │   │   │   │
│   │   │   │   ├── service/                        # Service Principal
│   │   │   │   │   ├── PrivacyGuardService.kt      # Service Accessibility + Foreground
│   │   │   │   │   ├── ServiceState.kt             # États du service
│   │   │   │   │   ├── ServiceBinder.kt            # Binder pour IPC
│   │   │   │   │   └── ServiceCoordinator.kt       # Coordination des composants
│   │   │   │   │
│   │   │   │   ├── sensors/                        # Modules de Capteurs
│   │   │   │   │   │
│   │   │   │   │   ├── base/                       # Classes de base
│   │   │   │   │   │   ├── SensorMonitor.kt        # Interface commune
│   │   │   │   │   │   ├── SensorConfig.kt         # Configuration
│   │   │   │   │   │   └── SensorResult.kt         # Résultat générique
│   │   │   │   │   │
│   │   │   │   │   ├── camera/                     # Module Caméra
│   │   │   │   │   │   ├── CameraMonitor.kt        # Monitoring principal
│   │   │   │   │   │   ├── CameraConfig.kt         # Configuration caméra
│   │   │   │   │   │   ├── FaceDetector.kt         # Détection via ML Kit
│   │   │   │   │   │   ├── FaceRecognizer.kt       # Reconnaissance custom
│   │   │   │   │   │   ├── GazeEstimator.kt        # Estimation regard
│   │   │   │   │   │   ├── DistanceEstimator.kt    # Estimation distance
│   │   │   │   │   │   └── models/
│   │   │   │   │   │       ├── DetectedFace.kt
│   │   │   │   │   │       ├── GazeDirection.kt
│   │   │   │   │   │       └── CameraDetectionResult.kt
│   │   │   │   │   │
│   │   │   │   │   ├── audio/                      # Module Audio
│   │   │   │   │   │   ├── AudioAnalyzer.kt        # Analyseur principal
│   │   │   │   │   │   ├── AudioConfig.kt
│   │   │   │   │   │   ├── AudioProcessor.kt       # Pre-processing
│   │   │   │   │   │   ├── VoiceCounter.kt         # Comptage de voix
│   │   │   │   │   │   ├── KeywordSpotter.kt       # Détection mots-clés
│   │   │   │   │   │   ├── PatternDetector.kt      # Patterns (pas, porte, etc.)
│   │   │   │   │   │   ├── ProximityEstimator.kt   # Estimation proximité audio
│   │   │   │   │   │   └── models/
│   │   │   │   │   │       ├── SoundPattern.kt
│   │   │   │   │   │       ├── DetectedKeyword.kt
│   │   │   │   │   │       ├── ProximityLevel.kt
│   │   │   │   │   │       └── AudioDetectionResult.kt
│   │   │   │   │   │
│   │   │   │   │   ├── motion/                     # Module Mouvement
│   │   │   │   │   │   ├── MotionDetector.kt       # Détecteur principal
│   │   │   │   │   │   ├── MotionConfig.kt
│   │   │   │   │   │   ├── AccelerometerProcessor.kt
│   │   │   │   │   │   ├── GyroscopeProcessor.kt
│   │   │   │   │   │   ├── SensorFusion.kt         # Fusion Kalman
│   │   │   │   │   │   ├── GestureRecognizer.kt    # Détection gestes
│   │   │   │   │   │   └── models/
│   │   │   │   │   │       ├── MotionGesture.kt
│   │   │   │   │   │       ├── Orientation.kt
│   │   │   │   │   │       ├── FusedMotionData.kt
│   │   │   │   │   │       └── MotionDetectionResult.kt
│   │   │   │   │   │
│   │   │   │   │   ├── proximity/                  # Module Proximité
│   │   │   │   │   │   ├── ProximityWatcher.kt
│   │   │   │   │   │   ├── ProximityAnalyzer.kt
│   │   │   │   │   │   └── models/
│   │   │   │   │   │       └── ProximityDetectionResult.kt
│   │   │   │   │   │
│   │   │   │   │   ├── light/                      # Module Luminosité
│   │   │   │   │   │   ├── LightSensorAnalyzer.kt
│   │   │   │   │   │   └── models/
│   │   │   │   │   │       └── LightDetectionResult.kt
│   │   │   │   │   │
│   │   │   │   │   └── location/                   # Module Localisation
│   │   │   │   │       ├── LocationMonitor.kt
│   │   │   │   │       ├── GeofencingManager.kt
│   │   │   │   │       ├── LocationLearner.kt      # Apprentissage lieux
│   │   │   │   │       └── models/
│   │   │   │   │           ├── LocationContext.kt
│   │   │   │   │           ├── Zone.kt
│   │   │   │   │           └── RiskLevel.kt
│   │   │   │   │
│   │   │   │   ├── assessment/                     # Évaluation des Menaces
│   │   │   │   │   ├── ThreatAssessmentEngine.kt   # Moteur principal
│   │   │   │   │   ├── SensorDataFusion.kt         # Fusion capteurs
│   │   │   │   │   ├── ThreatScorer.kt             # Scoring
│   │   │   │   │   ├── AdaptiveWeighting.kt        # Pondération adaptative
│   │   │   │   │   ├── FalsePositiveLearning.kt    # Apprentissage FP
│   │   │   │   │   ├── DecisionTree.kt             # Arbre de décision
│   │   │   │   │   └── models/
│   │   │   │   │       ├── ThreatAssessment.kt
│   │   │   │   │       ├── ThreatLevel.kt
│   │   │   │   │       ├── SensorData.kt
│   │   │   │   │       └── SensorWeights.kt
│   │   │   │   │
│   │   │   │   ├── protection/                     # Exécution Protection
│   │   │   │   │   ├── ProtectionExecutor.kt       # Exécuteur principal
│   │   │   │   │   ├── ProtectionAction.kt         # Interface action
│   │   │   │   │   ├── OverlayManager.kt           # Gestion overlays
│   │   │   │   │   ├── ScreenshotBlocker.kt        # Blocage screenshots
│   │   │   │   │   ├── ScreenRecordingBlocker.kt   # Blocage enregistrement
│   │   │   │   │   ├── ClipboardManager.kt         # Sécurisation presse-papier
│   │   │   │   │   │
│   │   │   │   │   ├── actions/                    # Actions spécifiques
│   │   │   │   │   │   ├── SoftMaskingProtection.kt
│   │   │   │   │   │   ├── DecoyScreenProtection.kt
│   │   │   │   │   │   ├── InstantLockProtection.kt
│   │   │   │   │   │   └── PanicModeProtection.kt
│   │   │   │   │   │
│   │   │   │   │   └── models/
│   │   │   │   │       ├── ProtectionLevel.kt
│   │   │   │   │       └── ProtectionMode.kt
│   │   │   │   │
│   │   │   │   ├── ml/                             # Machine Learning
│   │   │   │   │   ├── MLInferenceEngine.kt        # Moteur inférence
│   │   │   │   │   ├── ModelManager.kt             # Gestion modèles
│   │   │   │   │   ├── TensorFlowLiteManager.kt
│   │   │   │   │   ├── NPUAccelerator.kt           # Accélération NPU
│   │   │   │   │   ├── GPUDelegate.kt              # Délégation GPU
│   │   │   │   │   └── models/
│   │   │   │   │       └── MLModel.kt
│   │   │   │   │
│   │   │   │   ├── ui/                             # Interface Utilisateur
│   │   │   │   │   │
│   │   │   │   │   ├── MainActivity.kt             # Activité principale
│   │   │   │   │   │
│   │   │   │   │   ├── overlay/                    # Overlays
│   │   │   │   │   │   ├── PrivacyIndicator.kt     # Indicateur flottant
│   │   │   │   │   │   ├── SoftBlurOverlay.kt      # Overlay flou
│   │   │   │   │   │   ├── DecoyScreenOverlay.kt   # Overlay leurre
│   │   │   │   │   │   ├── LockScreenOverlay.kt    # Overlay verrouillage
│   │   │   │   │   │   └── decoy/                  # Contenus leurres
│   │   │   │   │   │       ├── ShoppingListDecoy.kt
│   │   │   │   │   │       ├── WikipediaDecoy.kt
│   │   │   │   │   │       ├── WeatherDecoy.kt
│   │   │   │   │   │       └── WorkNotesDecoy.kt
│   │   │   │   │   │
│   │   │   │   │   ├── dashboard/                  # Dashboard
│   │   │   │   │   │   ├── DashboardActivity.kt
│   │   │   │   │   │   ├── DashboardViewModel.kt
│   │   │   │   │   │   ├── DailyStatsCard.kt
│   │   │   │   │   │   ├── RiskZonesCard.kt
│   │   │   │   │   │   ├── EventsTimelineCard.kt
│   │   │   │   │   │   └── QuickActionsPanel.kt
│   │   │   │   │   │
│   │   │   │   │   ├── settings/                   # Paramètres
│   │   │   │   │   │   ├── SettingsActivity.kt
│   │   │   │   │   │   ├── SettingsViewModel.kt
│   │   │   │   │   │   ├── ProtectionModeScreen.kt
│   │   │   │   │   │   ├── SensorSensitivityScreen.kt
│   │   │   │   │   │   ├── ProtectedAppsScreen.kt
│   │   │   │   │   │   ├── TrustedFacesScreen.kt
│   │   │   │   │   │   ├── TrustZonesScreen.kt
│   │   │   │   │   │   └── AdvancedSettingsScreen.kt
│   │   │   │   │   │
│   │   │   │   │   ├── onboarding/                 # Onboarding
│   │   │   │   │   │   ├── OnboardingActivity.kt
│   │   │   │   │   │   ├── WelcomeScreen.kt
│   │   │   │   │   │   ├── PermissionsScreen.kt
│   │   │   │   │   │   ├── ModeSelectionScreen.kt
│   │   │   │   │   │   └── SetupCompleteScreen.kt
│   │   │   │   │   │
│   │   │   │   │   ├── components/                 # Composants réutilisables
│   │   │   │   │   │   ├── StatCard.kt
│   │   │   │   │   │   ├── ThreatIndicator.kt
│   │   │   │   │   │   ├── SensitivitySlider.kt
│   │   │   │   │   │   └── AppListItem.kt
│   │   │   │   │   │
│   │   │   │   │   └── theme/                      # Thème
│   │   │   │   │       ├── Theme.kt
│   │   │   │   │       ├── Color.kt
│   │   │   │   │       ├── Typography.kt
│   │   │   │   │       └── Shape.kt
│   │   │   │   │
│   │   │   │   ├── data/                           # Data Layer
│   │   │   │   │   │
│   │   │   │   │   ├── database/                   # Base de données
│   │   │   │   │   │   ├── PrivacyGuardDatabase.kt
│   │   │   │   │   │   │
│   │   │   │   │   │   ├── dao/                    # DAOs
│   │   │   │   │   │   │   ├── DetectionEventDao.kt
│   │   │   │   │   │   │   ├── AppConfigDao.kt
│   │   │   │   │   │   │   ├── TrustZoneDao.kt
│   │   │   │   │   │   │   ├── TrustedFaceDao.kt
│   │   │   │   │   │   │   └── AuditLogDao.kt
│   │   │   │   │   │   │
│   │   │   │   │   │   ├── entities/               # Entités Room
│   │   │   │   │   │   │   ├── DetectionEvent.kt
│   │   │   │   │   │   │   ├── AppConfig.kt
│   │   │   │   │   │   │   ├── TrustZone.kt
│   │   │   │   │   │   │   ├── TrustedFace.kt
│   │   │   │   │   │   │   └── AuditLog.kt
│   │   │   │   │   │   │
│   │   │   │   │   │   └── migrations/             # Migrations DB
│   │   │   │   │   │       └── Migration_1_2.kt
│   │   │   │   │   │
│   │   │   │   │   ├── repository/                 # Repositories
│   │   │   │   │   │   ├── DetectionEventRepository.kt
│   │   │   │   │   │   ├── AppConfigRepository.kt
│   │   │   │   │   │   ├── TrustZoneRepository.kt
│   │   │   │   │   │   ├── TrustedFaceRepository.kt
│   │   │   │   │   │   └── SettingsRepository.kt
│   │   │   │   │   │
│   │   │   │   │   └── preferences/                # Préférences
│   │   │   │   │       ├── SecurePreferences.kt    # Encrypted SharedPrefs
│   │   │   │   │       └── UserSettings.kt
│   │   │   │   │
│   │   │   │   ├── domain/                         # Domain Layer
│   │   │   │   │   │
│   │   │   │   │   ├── models/                     # Modèles de domaine
│   │   │   │   │   │   ├── ThreatReport.kt
│   │   │   │   │   │   ├── PrivacyEvent.kt
│   │   │   │   │   │   ├── SecurityProfile.kt
│   │   │   │   │   │   └── UserProfile.kt
│   │   │   │   │   │
│   │   │   │   │   ├── usecases/                   # Use Cases
│   │   │   │   │   │   ├── DetectThreatUseCase.kt
│   │   │   │   │   │   ├── ApplyProtectionUseCase.kt
│   │   │   │   │   │   ├── AddTrustedFaceUseCase.kt
│   │   │   │   │   │   ├── CreateTrustZoneUseCase.kt
│   │   │   │   │   │   └── ExportDataUseCase.kt
│   │   │   │   │   │
│   │   │   │   │   └── repository/                 # Interfaces Repository
│   │   │   │   │       └── IDetectionRepository.kt
│   │   │   │   │
│   │   │   │   ├── di/                             # Dependency Injection
│   │   │   │   │   ├── AppModule.kt
│   │   │   │   │   ├── DatabaseModule.kt
│   │   │   │   │   ├── SensorModule.kt
│   │   │   │   │   ├── MLModule.kt
│   │   │   │   │   └── NetworkModule.kt
│   │   │   │   │
│   │   │   │   ├── utils/                          # Utilitaires
│   │   │   │   │   ├── PermissionManager.kt
│   │   │   │   │   ├── NotificationManager.kt
│   │   │   │   │   ├── DeviceInfoUtil.kt
│   │   │   │   │   ├── TimeUtil.kt
│   │   │   │   │   ├── EncryptionUtil.kt
│   │   │   │   │   ├── BiometricUtil.kt
│   │   │   │   │   └── Extensions.kt
│   │   │   │   │
│   │   │   │   └── workers/                        # WorkManager Workers
│   │   │   │       ├── LogCleanupWorker.kt
│   │   │   │       ├── ModelUpdateWorker.kt
│   │   │   │       └── HealthCheckWorker.kt
│   │   │   │
│   │   │   ├── res/                                # Ressources
│   │   │   │   ├── drawable/                       # Images et icônes
│   │   │   │   ├── layout/                         # Layouts XML
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   ├── themes.xml
│   │   │   │   │   └── dimens.xml
│   │   │   │   ├── values-night/                   # Dark theme
│   │   │   │   ├── values-fr/                      # Traductions
│   │   │   │   ├── values-es/
│   │   │   │   ├── xml/
│   │   │   │   │   ├── accessibility_service_config.xml
│   │   │   │   │   └── network_security_config.xml
│   │   │   │   └── raw/                            # Fichiers bruts
│   │   │   │
│   │   │   └── assets/                             # Assets
│   │   │       └── models/                         # Modèles ML
│   │   │           ├── face_detection.tflite
│   │   │           ├── face_recognition.tflite
│   │   │           ├── keyword_spotting.tflite
│   │   │           ├── threat_assessment.tflite
│   │   │           └── labels.txt
│   │   │
│   │   ├── test/                                   # Tests Unitaires
│   │   │   └── java/com/privacyguard/
│   │   │       ├── sensors/
│   │   │       │   ├── CameraMonitorTest.kt
│   │   │       │   ├── AudioAnalyzerTest.kt
│   │   │       │   └── MotionDetectorTest.kt
│   │   │       ├── assessment/
│   │   │       │   └── ThreatAssessmentEngineTest.kt
│   │   │       └── protection/
│   │   │           └── ProtectionExecutorTest.kt
│   │   │
│   │   └── androidTest/                            # Tests d'Intégration
│   │       └── java/com/privacyguard/
│   │           ├── E2EProtectionTest.kt
│   │           ├── DatabaseTest.kt
│   │           └── UITest.kt
│   │
│   └── build.gradle.kts                            # Configuration Gradle App
│
├── buildSrc/                                       # Build Logic
│   └── src/main/kotlin/
│       └── Dependencies.kt                         # Dépendances centralisées
│
├── gradle/                                         # Gradle Wrapper
│   └── wrapper/
│
├── docs/                                           # Documentation supplémentaire
│   ├── api/                                        # Documentation API
│   ├── diagrams/                                   # Diagrammes architecture
│   └── screenshots/                                # Screenshots pour README
│
├── scripts/                                        # Scripts utilitaires
│   ├── setup.sh                                    # Setup initial
│   ├── test.sh                                     # Run tous les tests
│   └── deploy.sh                                   # Déploiement
│
├── .github/                                        # GitHub Configuration
│   ├── workflows/                                  # CI/CD
│   │   ├── ci.yml                                  # Build & Test
│   │   ├── release.yml                             # Release automation
│   │   └── security-scan.yml                       # Security checks
│   ├── ISSUE_TEMPLATE/
│   └── PULL_REQUEST_TEMPLATE.md
│
├── README.md                                       # Documentation principale
├── ARCHITECTURE.md                                 # Architecture détaillée
├── FEATURES.md                                     # Fonctionnalités
├── SENSORS.md                                      # Documentation capteurs
├── UI_UX.md                                        # Guide UI/UX
├── SECURITY_PRIVACY.md                             # Sécurité et confidentialité
├── TECHNICAL_CHALLENGES.md                         # Défis techniques
├── ROADMAP.md                                      # Feuille de route
├── CONTRIBUTING.md                                 # Guide de contribution
├── PROJECT_STRUCTURE.md                            # Ce fichier
│
├── build.gradle.kts                                # Configuration Gradle racine
├── settings.gradle.kts                             # Settings Gradle
├── gradle.properties                               # Propriétés Gradle
├── gradlew                                         # Gradle Wrapper Unix
├── gradlew.bat                                     # Gradle Wrapper Windows
│
├── .gitignore                                      # Git ignore
├── .editorconfig                                   # Configuration éditeur
├── ktlint.gradle                                   # Linter Kotlin
│
└── LICENSE                                         # Licence du projet
```

## 📦 Dépendances Principales

### build.gradle.kts (App Module)

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
}

android {
    namespace = "com.privacyguard"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.privacyguard"
        minSdk = 26 // Android 8.0
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
        viewBinding = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    
    // Jetpack Compose
    val composeVersion = "1.5.4"
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.navigation:navigation-compose:2.7.5")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")
    
    // CameraX
    val cameraVersion = "1.3.0"
    implementation("androidx.camera:camera-core:$cameraVersion")
    implementation("androidx.camera:camera-camera2:$cameraVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraVersion")
    implementation("androidx.camera:camera-view:$cameraVersion")
    
    // ML Kit
    implementation("com.google.mlkit:face-detection:16.1.5")
    implementation("com.google.mlkit:face-mesh-detection:16.0.0-beta1")
    
    // TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    
    // Audio Processing
    implementation("com.github.Jonatino:TarsosDSP:2.4")
    
    // Room Database
    val roomVersion = "2.6.0"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    
    // SQLCipher (chiffrement DB)
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    
    // Security Crypto
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Biometric
    implementation("androidx.biometric:biometric:1.1.0")
    
    // Dependency Injection - Hilt
    val hiltVersion = "2.48"
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    kapt("com.google.dagger:hilt-compiler:$hiltVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Location Services
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // Charts (pour dashboard)
    implementation("com.patrykandpatrick.vico:compose:1.13.1")
    
    // Lottie (animations)
    implementation("com.airbnb.android:lottie-compose:6.2.0")
    
    // Coil (images)
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // Timber (logging)
    implementation("com.jakewharton.timber:timber:5.0.1")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("com.google.truth:truth:1.1.5")
    
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$composeVersion")
}
```

## 🔑 Fichiers de Configuration Importants

### AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Permissions -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.USE_BIOMETRIC" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    
    <uses-feature android:name="android.hardware.camera.front" android:required="false" />
    <uses-feature android:name="android.hardware.microphone" android:required="false" />
    <uses-feature android:name="android.hardware.location.gps" android:required="false" />

    <application
        android:name=".PrivacyGuardApplication"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.PrivacyGuard"
        android:networkSecurityConfig="@xml/network_security_config"
        tools:targetApi="31">
        
        <!-- Main Activity -->
        <activity
            android:name=".ui.MainActivity"
            android:exported="true"
            android:theme="@style/Theme.PrivacyGuard">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
        <!-- Accessibility Service -->
        <service
            android:name=".service.PrivacyGuardService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>
        
        <!-- Foreground Service -->
        <service
            android:name=".service.PrivacyGuardForegroundService"
            android:foregroundServiceType="camera|microphone|location" />
        
        <!-- WorkManager Workers -->
        <worker
            android:name=".workers.LogCleanupWorker"
            android:exported="false" />
            
        <!-- Receivers -->
        <receiver
            android:name=".receivers.BootCompletedReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>
        
    </application>

</manifest>
```

---

Cette structure complète et détaillée fournit une base solide pour le développement de Privacy Guard!

