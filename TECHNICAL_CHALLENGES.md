# Défis Techniques et Solutions

## 🎯 Défis Majeurs

### 1. Faux Positifs vs Faux Négatifs

#### Problème
Trouver le bon équilibre entre:
- **Trop sensible** → Frustration utilisateur (déclenchements intempestifs)
- **Pas assez sensible** → Menaces manquées (défaut de protection)

#### Solutions

##### A. Scoring Pondéré Dynamique

```kotlin
class AdaptiveThreatScoring {
    
    // Pondération adaptative selon contexte
    fun computeThreatScore(
        sensorData: SensorData,
        context: ContextInfo
    ): Float {
        val weights = getAdaptiveWeights(context)
        
        val score = 
            sensorData.cameraScore * weights.camera +
            sensorData.audioScore * weights.audio +
            sensorData.motionScore * weights.motion +
            sensorData.proximityScore * weights.proximity
        
        // Multiplicateurs contextuels
        return score * getContextMultiplier(context)
    }
    
    private fun getAdaptiveWeights(context: ContextInfo): SensorWeights {
        return when {
            // Environnement bruyant → réduire poids audio
            context.ambientNoiseLevel > 0.7f -> SensorWeights(
                camera = 0.5f,
                audio = 0.2f,
                motion = 0.2f,
                proximity = 0.1f
            )
            
            // Luminosité faible → réduire poids caméra
            context.lightLevel < 0.2f -> SensorWeights(
                camera = 0.2f,
                audio = 0.4f,
                motion = 0.3f,
                proximity = 0.1f
            )
            
            // Conditions normales
            else -> SensorWeights(
                camera = 0.4f,
                audio = 0.3f,
                motion = 0.2f,
                proximity = 0.1f
            )
        }
    }
    
    private fun getContextMultiplier(context: ContextInfo): Float {
        return when (context.zone) {
            Zone.PUBLIC_TRANSPORT -> 1.3f // Plus sensible
            Zone.WORK -> 1.2f
            Zone.CAFE -> 1.1f
            Zone.HOME -> 0.7f // Moins sensible
            else -> 1.0f
        }
    }
}
```

##### B. Apprentissage des Faux Positifs

```kotlin
class FalsePositiveLearning {
    
    private val falsePositivePatterns = mutableListOf<ThreatPattern>()
    
    fun onUserDismissal(event: DetectionEvent, context: ContextInfo) {
        // Utilisateur a annulé → probablement faux positif
        val pattern = ThreatPattern(
            cameraSignature = event.cameraData.signature(),
            audioSignature = event.audioData.signature(),
            contextSignature = context.signature(),
            falsePositiveCount = 1
        )
        
        // Ajouter ou incrémenter
        val existing = falsePositivePatterns.find { it.matches(pattern) }
        if (existing != null) {
            existing.falsePositiveCount++
            
            // Si récurrent, ajuster les seuils
            if (existing.falsePositiveCount >= 3) {
                adjustThresholdsForPattern(existing)
            }
        } else {
            falsePositivePatterns.add(pattern)
        }
    }
    
    private fun adjustThresholdsForPattern(pattern: ThreatPattern) {
        // Créer exception pour ce pattern
        threatAssessmentEngine.addException(
            pattern = pattern,
            action = ThreatAction.IGNORE_OR_REDUCE_SENSITIVITY
        )
        
        Log.d("FPL", "Learned false positive pattern: $pattern")
    }
}
```

##### C. Confirmation Multi-Capteurs

```kotlin
class MultiSensorConfirmation {
    
    fun shouldTriggerProtection(sensorData: SensorData): Boolean {
        val triggeredSensors = mutableListOf<SensorType>()
        
        // Vérifier chaque capteur
        if (sensorData.cameraScore > cameraThreshold) {
            triggeredSensors.add(SensorType.CAMERA)
        }
        if (sensorData.audioScore > audioThreshold) {
            triggeredSensors.add(SensorType.AUDIO)
        }
        if (sensorData.motionScore > motionThreshold) {
            triggeredSensors.add(SensorType.MOTION)
        }
        
        // Règle: au moins 2 capteurs doivent confirmer
        // (ou 1 capteur avec score très élevé)
        return triggeredSensors.size >= 2 || 
               sensorData.hasVeryHighScore()
    }
    
    private fun SensorData.hasVeryHighScore(): Boolean {
        return cameraScore > 0.9f || 
               audioScore > 0.9f || 
               motionScore > 0.9f
    }
}
```

### 2. Latence (< 200ms Requis)

#### Problème
Le pipeline complet doit s'exécuter en moins de 200ms pour être imperceptible.

#### Solutions

##### A. Pipeline Parallèle Asynchrone

```kotlin
class OptimizedDetectionPipeline {
    
    suspend fun processFrame(): ThreatAssessment = coroutineScope {
        // Acquisition parallèle de tous les capteurs
        val cameraDeferred = async(cameraDispatcher) {
            cameraMonitor.getCurrentResult()
        }
        val audioDeferred = async(audioDispatcher) {
            audioAnalyzer.getCurrentResult()
        }
        val motionDeferred = async(motionDispatcher) {
            motionDetector.getCurrentResult()
        }
        val proximityDeferred = async(Dispatchers.Default) {
            proximityWatcher.getCurrentResult()
        }
        
        // Attendre tous les résultats en parallèle (pas séquentiel!)
        val cameraResult = cameraDeferred.await()
        val audioResult = audioDeferred.await()
        val motionResult = motionDeferred.await()
        val proximityResult = proximityDeferred.await()
        
        // Fusion et décision (rapide, pas de ML ici)
        threatAssessmentEngine.evaluate(
            camera = cameraResult,
            audio = audioResult,
            motion = motionResult,
            proximity = proximityResult
        )
    }
}
```

##### B. ML Inference Optimisée

```kotlin
class OptimizedMLInference {
    
    private val interpreter: Interpreter
    
    init {
        val options = Interpreter.Options().apply {
            // 1. Utiliser NPU si disponible
            if (isNNAPIAvailable()) {
                setUseNNAPI(true)
            }
            
            // 2. GPU delegation
            val gpuDelegate = GpuDelegate()
            addDelegate(gpuDelegate)
            
            // 3. Threads multiples
            setNumThreads(2)
        }
        
        interpreter = Interpreter(modelFile, options)
    }
    
    fun runInference(input: FloatArray): FloatArray {
        val output = FloatArray(OUTPUT_SIZE)
        
        // Mesurer performance
        val startTime = SystemClock.elapsedRealtimeNanos()
        interpreter.run(input, output)
        val inferenceTime = (SystemClock.elapsedRealtimeNanos() - startTime) / 1_000_000 // ms
        
        if (inferenceTime > 80) {
            Log.w("ML", "Slow inference: ${inferenceTime}ms")
        }
        
        return output
    }
}
```

##### C. Cache Intelligent

```kotlin
class ResultCache {
    
    private val cache = LruCache<String, CachedResult>(maxSize = 50)
    
    fun getCachedOrCompute(
        key: CacheKey,
        compute: suspend () -> DetectionResult
    ): DetectionResult {
        // Vérifier si résultat récent existe
        val cached = cache.get(key.toString())
        if (cached != null && !cached.isExpired()) {
            return cached.result
        }
        
        // Sinon calculer
        val result = runBlocking { compute() }
        cache.put(key.toString(), CachedResult(result, timestamp = now()))
        
        return result
    }
    
    data class CachedResult(
        val result: DetectionResult,
        val timestamp: Long,
        val ttl: Long = 100 // ms
    ) {
        fun isExpired() = System.currentTimeMillis() - timestamp > ttl
    }
}
```

##### D. Early Exit Strategy

```kotlin
class EarlyExitDetection {
    
    fun quickCheck(sensorData: SensorData): QuickCheckResult {
        // Vérifications ultra-rapides sans ML
        
        // 1. Aucun visage détecté + pas de son + immobile
        if (sensorData.faceCount == 0 &&
            sensorData.audioLevel < 0.1f &&
            sensorData.isMotionStable) {
            return QuickCheckResult.SAFE // Exit early!
        }
        
        // 2. Propriétaire seul présent + environnement calme
        if (sensorData.faceCount == 1 &&
            sensorData.isOwnerPresent &&
            sensorData.audioLevel < 0.3f) {
            return QuickCheckResult.SAFE // Exit early!
        }
        
        // 3. Menace évidente (pas besoin de ML)
        if (sensorData.unknownFaceCount >= 3 &&
            sensorData.closestDistance < 30) {
            return QuickCheckResult.THREAT // Exit early!
        }
        
        // Cas ambigus → full pipeline avec ML
        return QuickCheckResult.NEEDS_FULL_ANALYSIS
    }
}
```

### 3. Consommation Batterie

#### Problème
Surveillance continue = drain batterie important

#### Solutions

##### A. Sampling Adaptatif

```kotlin
class AdaptiveSampling {
    
    fun getOptimalSamplingRates(context: DeviceContext): SamplingRates {
        return when {
            // Écran éteint → minimal
            !context.screenOn -> SamplingRates(
                camera = 1, // fps
                audio = 0, // désactivé
                motion = 10, // Hz
                proximity = 5 // Hz
            )
            
            // Batterie critique
            context.batteryLevel < 10 -> SamplingRates(
                camera = 2,
                audio = 5, // samples/sec
                motion = 20,
                proximity = 10
            )
            
            // Mode économie d'énergie
            context.powerSaveMode -> SamplingRates(
                camera = 5,
                audio = 10,
                motion = 30,
                proximity = 20
            )
            
            // En charge
            context.isCharging -> SamplingRates(
                camera = 15, // Maximum
                audio = 20,
                motion = 50,
                proximity = 50
            )
            
            // Normal
            else -> SamplingRates(
                camera = 10,
                audio = 16,
                motion = 50,
                proximity = 30
            )
        }
    }
    
    // Ajustement dynamique selon température
    fun adjustForThermal(rates: SamplingRates, thermalState: ThermalState): SamplingRates {
        val factor = when (thermalState) {
            ThermalState.CRITICAL -> 0.3f
            ThermalState.SEVERE -> 0.5f
            ThermalState.MODERATE -> 0.7f
            else -> 1.0f
        }
        
        return rates * factor
    }
}
```

##### B. Résolution Adaptative

```kotlin
class AdaptiveResolution {
    
    fun getOptimalCameraResolution(context: DeviceContext): Size {
        return when {
            context.batteryLevel < 15 -> Size(160, 120) // Très bas
            context.batteryLevel < 30 -> Size(240, 180) // Bas
            context.powerSaveMode -> Size(320, 240) // Normal
            else -> Size(480, 360) // Haute qualité
        }
    }
    
    fun getOptimalAudioQuality(context: DeviceContext): AudioQuality {
        return when {
            context.batteryLevel < 15 -> AudioQuality.LOW // 8kHz
            context.powerSaveMode -> AudioQuality.MEDIUM // 12kHz
            else -> AudioQuality.HIGH // 16kHz
        }
    }
}
```

##### C. Wake Locks Intelligents

```kotlin
class SmartWakeLockManager {
    
    private val wakeLock = powerManager.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK,
        "PrivacyGuard::DetectionWakeLock"
    )
    
    fun manageWakeLock(screenOn: Boolean, threatLevel: ThreatLevel) {
        when {
            // Écran allumé + menace → Full wake lock
            screenOn && threatLevel > ThreatLevel.LOW -> {
                acquireFullWakeLock()
            }
            
            // Écran allumé + pas de menace → Partial
            screenOn -> {
                acquirePartialWakeLock()
            }
            
            // Écran éteint → Release (sauf si menace active)
            else -> {
                if (threatLevel == ThreatLevel.NONE) {
                    releaseWakeLock()
                }
            }
        }
    }
}
```

##### D. Doze Mode Compliance

```kotlin
class DozeCompatibility {
    
    fun requestBatteryOptimizationExemption() {
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            // Demander exemption (avec justification claire à l'utilisateur)
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }
    
    fun schedulePeriodicWork() {
        // Utiliser WorkManager pour travail périodique
        val workRequest = PeriodicWorkRequestBuilder<DetectionWorker>(
            repeatInterval = 15, // minutes (minimum Android)
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
        .setConstraints(
            Constraints.Builder()
                .setRequiresBatteryNotLow(true) // Respecter batterie faible
                .build()
        )
        .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "privacy_guard_detection",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
```

### 4. Compatibilité Multi-Devices

#### Problème
Variété de hardware Android (capteurs, puissance, versions OS)

#### Solutions

##### A. Feature Detection

```kotlin
class DeviceCapabilities {
    
    fun detect(): Capabilities {
        return Capabilities(
            hasFrontCamera = hasFrontCamera(),
            hasMicrophone = hasMicrophone(),
            hasProximitySensor = hasProximitySensor(),
            hasLightSensor = hasLightSensor(),
            hasNPU = hasNPU(),
            hasGPU = hasGPU(),
            supportsAccessibilityService = sdkVersion >= Build.VERSION_CODES.O,
            supportsMLKit = checkMLKitAvailability(),
            ram = getTotalRam(),
            cpuCores = Runtime.getRuntime().availableProcessors()
        )
    }
    
    private fun hasFrontCamera(): Boolean {
        val cameraIds = cameraManager.cameraIdList
        return cameraIds.any { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            characteristics.get(CameraCharacteristics.LENS_FACING) == 
                CameraCharacteristics.LENS_FACING_FRONT
        }
    }
    
    private fun hasNPU(): Boolean {
        return try {
            // Vérifier si NNAPI disponible
            val options = Interpreter.Options().apply {
                setUseNNAPI(true)
            }
            // Tester avec un modèle dummy
            testNNAPIAvailability(options)
            true
        } catch (e: Exception) {
            false
        }
    }
}
```

##### B. Fallback Gracieux

```kotlin
class FeatureFallback {
    
    fun getFaceDetector(capabilities: Capabilities): FaceDetector {
        return when {
            // ML Kit disponible (optimal)
            capabilities.supportsMLKit -> MLKitFaceDetector()
            
            // Fallback: OpenCV
            capabilities.hasOpenCV -> OpenCVFaceDetector()
            
            // Fallback: détection simple basée sur Haar Cascades
            else -> SimpleFaceDetector()
        }
    }
    
    fun getMLInferenceEngine(capabilities: Capabilities): InferenceEngine {
        return when {
            // NPU disponible
            capabilities.hasNPU -> NNAPIInferenceEngine()
            
            // GPU disponible
            capabilities.hasGPU -> GPUInferenceEngine()
            
            // Fallback CPU (avec modèle quantifié)
            else -> CPUInferenceEngine(quantized = true)
        }
    }
}
```

##### C. Performance Profiles

```kotlin
enum class DeviceProfile(
    val cameraFps: Int,
    val audioSampleRate: Int,
    val mlModelComplexity: ModelComplexity
) {
    HIGH_END(
        cameraFps = 15,
        audioSampleRate = 16000,
        mlModelComplexity = ModelComplexity.FULL
    ),
    
    MID_RANGE(
        cameraFps = 10,
        audioSampleRate = 12000,
        mlModelComplexity = ModelComplexity.OPTIMIZED
    ),
    
    LOW_END(
        cameraFps = 5,
        audioSampleRate = 8000,
        mlModelComplexity = ModelComplexity.LITE
    ),
    
    VERY_LOW_END(
        cameraFps = 3,
        audioSampleRate = 8000,
        mlModelComplexity = ModelComplexity.MINIMAL
    );
    
    companion object {
        fun detect(capabilities: Capabilities): DeviceProfile {
            val score = computePerformanceScore(capabilities)
            return when {
                score >= 80 -> HIGH_END
                score >= 60 -> MID_RANGE
                score >= 40 -> LOW_END
                else -> VERY_LOW_END
            }
        }
        
        private fun computePerformanceScore(cap: Capabilities): Int {
            var score = 0
            
            // RAM
            score += when {
                cap.ram >= 8_000 -> 30
                cap.ram >= 6_000 -> 25
                cap.ram >= 4_000 -> 20
                cap.ram >= 2_000 -> 10
                else -> 5
            }
            
            // CPU cores
            score += when {
                cap.cpuCores >= 8 -> 20
                cap.cpuCores >= 6 -> 15
                cap.cpuCores >= 4 -> 10
                else -> 5
            }
            
            // NPU/GPU
            if (cap.hasNPU) score += 25
            else if (cap.hasGPU) score += 15
            
            // ML Kit support
            if (cap.supportsMLKit) score += 15
            
            // Android version
            score += min((Build.VERSION.SDK_INT - 26) * 2, 10)
            
            return score
        }
    }
}
```

### 5. Accessibility Service (Permission Sensible)

#### Problème
Accessibility Service requis pour overlay universel, mais permission effrayante pour utilisateurs

#### Solutions

##### A. Explication Claire

```kotlin
class AccessibilityOnboarding {
    
    fun showExplanation() {
        AlertDialog.Builder(context)
            .setTitle("Service d'Accessibilité")
            .setMessage("""
                Privacy Guard nécessite le service d'accessibilité pour:
                
                ✓ Afficher un overlay de protection sur TOUTES les apps
                ✓ Détecter quelle app est active pour appliquer les bons réglages
                ✓ Masquer le contenu en temps réel
                
                ❌ Privacy Guard NE lit PAS le contenu de votre écran
                ❌ Privacy Guard NE capture PAS vos frappes clavier
                ❌ Privacy Guard NE transmet AUCUNE donnée en ligne
                
                Le code est open-source et auditable.
                
                Souhaitez-vous continuer ?
            """.trimIndent())
            .setPositiveButton("Activer") { _, _ ->
                openAccessibilitySettings()
            }
            .setNegativeButton("En savoir plus") { _, _ ->
                showDetailedExplanation()
            }
            .show()
    }
    
    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }
}
```

##### B. Audit Trail

```kotlin
class AccessibilityAuditLog {
    
    fun logAccessibilityEvent(event: AccessibilityEvent) {
        // Enregistrer uniquement métadonnées (pas de contenu)
        val log = AccessibilityEventLog(
            timestamp = System.currentTimeMillis(),
            eventType = event.eventType.toHumanReadable(),
            packageName = event.packageName?.toString() ?: "unknown",
            // ❌ PAS de texte, pas de contenu
            action = "Protection evaluation"
        )
        
        if (settings.auditLogEnabled) {
            auditLogDao.insert(log)
        }
    }
    
    fun getAuditLog(): List<AccessibilityEventLog> {
        // Permettre à l'utilisateur de voir ce que l'app fait
        return auditLogDao.getLast1000()
    }
}
```

## 🔬 Tests et Validation

### Tests de Performance

```kotlin
class PerformanceTest {
    
    @Test
    fun testPipelineLatency() = runTest {
        val pipeline = OptimizedDetectionPipeline()
        
        repeat(100) {
            val startTime = System.nanoTime()
            pipeline.processFrame()
            val latency = (System.nanoTime() - startTime) / 1_000_000 // ms
            
            // Assertion: < 200ms
            assert(latency < 200) {
                "Pipeline too slow: ${latency}ms"
            }
        }
    }
    
    @Test
    fun testBatteryImpact() {
        val batteryBefore = getBatteryLevel()
        
        // Run for 1 hour
        runDetectionFor(1.hours)
        
        val batteryAfter = getBatteryLevel()
        val drain = batteryBefore - batteryAfter
        
        // Assertion: < 10% drain per hour
        assert(drain < 10) {
            "Excessive battery drain: $drain%/hour"
        }
    }
}
```

### Tests de Précision

```kotlin
class AccuracyTest {
    
    @Test
    fun testFalsePositiveRate() {
        val testCases = loadTestScenarios("safe_scenarios.json")
        var falsePositives = 0
        
        testCases.forEach { scenario ->
            val result = threatAssessmentEngine.evaluate(scenario.sensorData)
            if (result.threatScore > threshold) {
                falsePositives++
            }
        }
        
        val fpr = falsePositives.toFloat() / testCases.size
        
        // Objectif: < 5% FPR
        assert(fpr < 0.05) {
            "False positive rate too high: ${fpr * 100}%"
        }
    }
    
    @Test
    fun testFalseNegativeRate() {
        val testCases = loadTestScenarios("threat_scenarios.json")
        var falseNegatives = 0
        
        testCases.forEach { scenario ->
            val result = threatAssessmentEngine.evaluate(scenario.sensorData)
            if (result.threatScore <= threshold) {
                falseNegatives++
            }
        }
        
        val fnr = falseNegatives.toFloat() / testCases.size
        
        // Objectif: < 1% FNR
        assert(fnr < 0.01) {
            "False negative rate too high: ${fnr * 100}%"
        }
    }
}
```

---

Ces solutions techniques permettent de surmonter les principaux défis tout en maintenant une expérience utilisateur fluide et performante.

