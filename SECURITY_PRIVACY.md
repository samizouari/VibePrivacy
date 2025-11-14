# Sécurité et Confidentialité

## 🔐 Principes Fondamentaux

### 1. Privacy by Design
La confidentialité est intégrée dès la conception, pas ajoutée après coup.

### 2. Zero Knowledge
L'application ne connaît et ne stocke que ce qui est strictement nécessaire.

### 3. Local First
Toutes les données sont traitées localement. Aucun serveur externe.

### 4. Minimal Data Retention
Les données ne sont conservées que le temps nécessaire, puis supprimées.

### 5. User Control
L'utilisateur a un contrôle total sur ses données et leur utilisation.

## 🛡️ Architecture de Sécurité

### Traitement Local Uniquement

```kotlin
/**
 * Tous les capteurs traitent les données en streaming
 * Aucune image/audio n'est stocké sur le disque
 */
class SensorDataPolicy {
    // ❌ INTERDIT
    fun saveImageToFile(image: Bitmap) {
        // Cette fonction ne doit JAMAIS exister
    }
    
    // ✅ AUTORISÉ
    suspend fun processImageStream(imageFlow: Flow<Bitmap>) {
        imageFlow
            .map { image -> 
                // Traitement immédiat
                val faces = faceDetector.detect(image)
                // Image détruite après traitement
                image.recycle()
                faces
            }
            .collect { faces ->
                // Seules les métadonnées sont conservées
                handleFaceDetection(faces)
            }
    }
}
```

### Chiffrement des Données Sensibles

#### Données Chiffrées

```kotlin
class SecureDataStorage(context: Context) {
    
    // Encrypted SharedPreferences pour settings
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        "privacy_guard_prefs",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    // Base de données chiffrée avec SQLCipher
    private val database = Room.databaseBuilder(
        context,
        PrivacyGuardDatabase::class.java,
        "privacy_guard_db"
    )
    .openHelperFactory(SupportFactory(SQLiteDatabase.getBytes("password".toCharArray())))
    .build()
    
    // Keystore pour clés biométriques
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }
    
    fun storeTrustedFace(face: TrustedFace) {
        // Encodage facial chiffré avant stockage
        val encrypted = encrypt(face.faceEncoding)
        database.trustedFaceDao().insert(face.copy(faceEncoding = encrypted))
    }
    
    private fun encrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(
            KeyProperties.KEY_ALGORITHM_AES + "/" +
            KeyProperties.BLOCK_MODE_GCM + "/" +
            KeyProperties.ENCRYPTION_PADDING_NONE
        )
        
        val key = keyStore.getKey("privacy_guard_key", null)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        
        return cipher.doFinal(data)
    }
}
```

#### Données NON Stockées

- ❌ Images de la caméra
- ❌ Enregistrements audio
- ❌ Captures d'écran de contenu sensible
- ❌ Historique de navigation
- ❌ Contenu des applications protégées

#### Données Stockées (Chiffrées)

- ✅ Encodages faciaux des personnes de confiance (128 bytes par visage)
- ✅ Logs de détection (métadonnées uniquement)
- ✅ Configurations utilisateur
- ✅ Zones géographiques de confiance (coordonnées GPS)
- ✅ Photos des intrus (optionnel, opt-in explicite)

### Protection des Logs

```kotlin
@Entity(tableName = "detection_events")
data class DetectionEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val threatScore: Int,
    
    // ❌ PAS de données brutes
    // val cameraImage: ByteArray, // INTERDIT
    // val audioRecording: ByteArray, // INTERDIT
    
    // ✅ Uniquement métadonnées
    val faceCount: Int,
    val unknownFaceCount: Int,
    val audioPatterns: String, // JSON: ["FOOTSTEPS", "DOOR"]
    val motionGesture: String?, // "SUDDEN_GRAB"
    val protectionLevel: ProtectionLevel,
    val wasBlocked: Boolean,
    val location: String? // Ville uniquement, pas coordonnées exactes
) {
    // Auto-expiration après 30 jours
    @Ignore
    val isExpired: Boolean
        get() = System.currentTimeMillis() - timestamp > 30.days.inWholeMilliseconds
}

// Nettoyage automatique
class LogCleanupWorker : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        detectionEventDao.deleteOlderThan(30.days.ago)
        return Result.success()
    }
}
```

## 🔒 Permissions et Accès

### Permissions Requises

```xml
<!-- AndroidManifest.xml -->
<manifest>
    <!-- Capteurs -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    
    <!-- Service -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    
    <!-- Biométrie -->
    <uses-permission android:name="android.permission.USE_BIOMETRIC" />
    
    <!-- Réseau (pour ML models updates uniquement, optionnel) -->
    <uses-permission android:name="android.permission.INTERNET" />
</manifest>
```

### Gestion des Permissions

```kotlin
class PermissionManager(private val activity: Activity) {
    
    fun requestAllPermissions() {
        // Explication claire AVANT de demander
        showPermissionRationale {
            // Demande groupée
            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                REQUEST_CODE_ALL
            )
        }
    }
    
    private fun showPermissionRationale(onAccept: () -> Unit) {
        AlertDialog.Builder(activity)
            .setTitle("Permissions requises")
            .setMessage("""
                Privacy Guard a besoin des permissions suivantes :
                
                📹 Caméra : Détecter les visages autour de vous
                🎤 Microphone : Analyser les sons suspects
                📍 Localisation : Zones de confiance automatiques
                
                ⚠️ Toutes les données sont traitées localement.
                Aucune image/audio n'est envoyé en ligne.
                
                Voulez-vous continuer ?
            """.trimIndent())
            .setPositiveButton("Autoriser") { _, _ -> onAccept() }
            .setNegativeButton("Refuser") { dialog, _ -> 
                dialog.dismiss()
                showLimitedFunctionalityWarning()
            }
            .show()
    }
    
    fun checkPermissionStatus(): PermissionStatus {
        return PermissionStatus(
            camera = checkSelfPermission(Manifest.permission.CAMERA),
            audio = checkSelfPermission(Manifest.permission.RECORD_AUDIO),
            location = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION),
            overlay = Settings.canDrawOverlays(activity),
            accessibility = isAccessibilityServiceEnabled()
        )
    }
}
```

### Dégradation Gracieuse

```kotlin
class AdaptiveProtectionManager {
    fun getAvailableProtection(permissions: PermissionStatus): ProtectionCapabilities {
        return ProtectionCapabilities(
            faceDetection = permissions.camera,
            audioAnalysis = permissions.audio,
            geoFencing = permissions.location,
            motionDetection = true, // Pas de permission requise
            proximityDetection = true, // Pas de permission requise
            fullOverlay = permissions.overlay,
            
            // Mode dégradé si permissions manquantes
            degradedMode = !permissions.hasAllRequired()
        )
    }
    
    fun showDegradedModeWarning() {
        Toast.makeText(
            context,
            "⚠️ Protection limitée : certaines permissions manquent",
            Toast.LENGTH_LONG
        ).show()
    }
}
```

## 🕵️ Anti-Surveillance

### Indicateurs de Confidentialité

```kotlin
class PrivacyIndicatorManager {
    
    // Afficher indicateur quand caméra/micro actifs
    fun showCameraIndicator() {
        notificationManager.notify(
            CAMERA_INDICATOR_ID,
            Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_camera_active)
                .setContentTitle("Caméra active")
                .setContentText("Privacy Guard analyse l'environnement")
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .build()
        )
    }
    
    fun showMicIndicator() {
        // Indicateur similaire pour micro
    }
}
```

### Détection de Tentatives de Désactivation

```kotlin
class TamperDetection {
    
    fun detectForceStop() {
        // Monitorer si l'app est force-stopped
        val alarmManager = context.getSystemService<AlarmManager>()
        
        // Alarme périodique pour vérifier si le service tourne
        alarmManager?.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 60000,
            60000, // Toutes les minutes
            createCheckServiceIntent()
        )
    }
    
    fun onServiceStopped(unexpected: Boolean) {
        if (unexpected && settings.tamperAlerts) {
            // Alerte que le service a été arrêté
            notificationManager.notify(
                TAMPER_ALERT_ID,
                Notification.Builder(context, URGENT_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_warning)
                    .setContentTitle("⚠️ Privacy Guard désactivé")
                    .setContentText("Le service de protection a été arrêté")
                    .setPriority(Notification.PRIORITY_HIGH)
                    .build()
            )
            
            // Log de l'incident
            securityLogger.logTamperAttempt(
                type = TamperType.SERVICE_STOPPED,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}
```

### Mode Stealth

```kotlin
class StealthMode {
    
    fun enable() {
        // Changer l'icône de l'app
        setComponentEnabledSetting(
            "com.privacyguard.NormalIcon",
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        )
        setComponentEnabledSetting(
            "com.privacyguard.CalculatorIcon", // Icône leurre
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        )
        
        // Changer le nom de l'app
        changeAppName("Calculatrice")
        
        // Cacher des Recent Apps
        activityManager.excludeFromRecents = true
        
        // Pas de notifications visibles
        notificationManager.cancelAll()
    }
    
    fun disable() {
        // Restaurer l'apparence normale
        setComponentEnabledSetting(
            "com.privacyguard.NormalIcon",
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        )
        setComponentEnabledSetting(
            "com.privacyguard.CalculatorIcon",
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        )
        changeAppName("Privacy Guard")
    }
}
```

## 🔍 Transparence et Auditabilité

### Export des Données

```kotlin
class DataExporter {
    
    suspend fun exportAllData(): File {
        val exportData = ExportData(
            version = BuildConfig.VERSION_NAME,
            exportTimestamp = System.currentTimeMillis(),
            settings = settingsRepository.getAll(),
            detectionEvents = detectionEventDao.getAll(),
            trustedFaces = trustedFaceDao.getAll().map { it.anonymize() },
            trustZones = trustZoneDao.getAll(),
            appConfigs = appConfigDao.getAll()
        )
        
        val json = Json.encodeToString(exportData)
        
        val file = File(context.getExternalFilesDir(null), "privacy_guard_export.json")
        file.writeText(json)
        
        return file
    }
    
    private fun TrustedFace.anonymize(): TrustedFaceExport {
        // Ne pas exporter les encodages faciaux réels
        return TrustedFaceExport(
            name = name,
            addedTimestamp = addedTimestamp,
            lastSeenTimestamp = lastSeenTimestamp,
            verificationCount = verificationCount
            // faceEncoding non inclus pour confidentialité
        )
    }
}
```

### Logs Détaillés (Opt-in)

```kotlin
class DetailedLogger {
    
    fun logDetectionEvent(
        cameraResult: CameraDetectionResult,
        audioResult: AudioDetectionResult,
        motionResult: MotionDetectionResult,
        threatScore: Int,
        actionTaken: ProtectionAction?
    ) {
        if (!settings.detailedLogsEnabled) return
        
        val logEntry = DetailedLogEntry(
            timestamp = System.currentTimeMillis(),
            
            // Données caméra (métadonnées uniquement)
            faceCount = cameraResult.faces.size,
            unknownFaceCount = cameraResult.unknownFaceCount,
            closestDistance = cameraResult.closestFaceDistance,
            
            // Données audio (métadonnées uniquement)
            voiceCount = audioResult.voiceCount,
            keywords = audioResult.detectedKeywords.map { it.keyword },
            soundPatterns = audioResult.soundPatterns.map { it.name },
            
            // Données mouvement
            gesture = motionResult.gesture?.name,
            
            // Résultat
            threatScore = threatScore,
            actionTaken = actionTaken?.javaClass?.simpleName,
            
            // Contexte
            currentApp = getCurrentAppPackage(),
            location = locationMonitor.currentZone?.name
        )
        
        detailedLogDao.insert(logEntry)
    }
}
```

### Dashboard de Confidentialité

```kotlin
@Composable
fun PrivacyDashboard() {
    Column {
        // Indicateurs en temps réel
        PrivacyIndicators(
            cameraActive = cameraMonitor.isActive,
            micActive = audioAnalyzer.isActive,
            locationTracking = locationMonitor.isActive
        )
        
        // Données collectées
        DataCollectionSummary(
            facesStored = trustedFaceDao.count(),
            eventsLogged = detectionEventDao.count(),
            zonesConfigured = trustZoneDao.count()
        )
        
        // Actions disponibles
        PrivacyActions(
            onExportData = { dataExporter.export() },
            onDeleteAllData = { showDeleteConfirmation() },
            onViewPermissions = { openPermissionsScreen() }
        )
    }
}
```

## 📜 Conformité Légale

### RGPD (Europe)

```kotlin
object GDPRCompliance {
    
    // Droit d'accès (Art. 15)
    fun provideDataAccess(userId: String): UserDataPackage {
        return UserDataPackage(
            personalData = getUserPersonalData(userId),
            processingPurpose = "Protection de la confidentialité de l'écran",
            dataCategories = listOf(
                "Métadonnées de détection faciale",
                "Patterns audio",
                "Données de localisation",
                "Configurations utilisateur"
            ),
            retentionPeriod = "30 jours maximum",
            recipients = "Aucun - traitement local uniquement"
        )
    }
    
    // Droit à l'effacement (Art. 17)
    suspend fun deleteAllUserData(userId: String) {
        database.clearAllTables()
        encryptedPrefs.edit().clear().apply()
        deleteMLModels()
        
        Toast.makeText(
            context,
            "✓ Toutes vos données ont été supprimées",
            Toast.LENGTH_LONG
        ).show()
    }
    
    // Droit à la portabilité (Art. 20)
    suspend fun exportPortableData(userId: String): File {
        return dataExporter.exportAllData()
    }
    
    // Consentement (Art. 7)
    fun obtainConsent() {
        showConsentDialog(
            purpose = "Protection de confidentialité",
            dataProcessed = listOf(
                "Images de la caméra frontale (non stockées)",
                "Audio ambiant (non stocké)",
                "Capteurs de mouvement",
                "Localisation GPS"
            ),
            legalBasis = "Consentement explicite",
            canWithdraw = true
        )
    }
}
```

### Conformité Biométrique

```kotlin
class BiometricDataHandler {
    
    // Conformité BIPA (Illinois) et lois biométriques
    fun handleFaceData(faceImage: Bitmap) {
        // 1. Consentement explicite
        if (!hasExplicitBiometricConsent()) {
            requestBiometricConsent()
            return
        }
        
        // 2. Informer de la durée de conservation
        // "Les encodages faciaux sont stockés de manière chiffrée
        //  et supprimés à votre demande ou après suppression de l'app"
        
        // 3. Extraction de features uniquement
        val faceEncoding = faceRecognizer.extractEncoding(faceImage)
        
        // 4. Image source immédiatement détruite
        faceImage.recycle()
        
        // 5. Encoding chiffré avant stockage
        storeTrustedFaceEncoding(faceEncoding, encrypted = true)
    }
    
    fun deleteAllBiometricData() {
        trustedFaceDao.deleteAll()
        
        // Notification à l'utilisateur
        notifyBiometricDataDeleted()
    }
}
```

## 🔐 Sécurité Multi-Couches

### Protection Active

```kotlin
class ActiveProtectionManager {
    
    fun enableScreenshotProtection(window: Window) {
        // Bloquer screenshots
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }
    
    fun disableScreenRecording() {
        // Détecter si screen recording actif
        if (isScreenRecordingActive()) {
            // Overlay de blocage
            showBlockingOverlay("⚠️ Enregistrement d'écran détecté")
            
            // Notification
            notificationManager.showHighPriority(
                "Screen recording bloqué",
                "Privacy Guard protège votre contenu"
            )
        }
    }
    
    fun secureClipboard() {
        // Vider le clipboard après timeout
        clipboardManager.addPrimaryClipChangedListener {
            handler.postDelayed({
                clipboardManager.clearPrimaryClip()
            }, 30000) // 30 secondes
        }
    }
    
    fun preventAccidentalSharing() {
        // Intercepter intent de partage
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_SEND) {
                    if (isSensitiveAppActive()) {
                        // Confirmation requise
                        showSharingConfirmation(intent)
                        abortBroadcast()
                    }
                }
            }
        }, IntentFilter(Intent.ACTION_SEND))
    }
}
```

### Détection de Caméras Externes

```kotlin
class ExternalCameraDetector {
    
    fun detectSurveillanceCameras() {
        // Utiliser la caméra pour détecter reflets de lentilles
        cameraManager.getCameraIdList().forEach { cameraId ->
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            
            if (characteristics.get(CameraCharacteristics.LENS_FACING) == 
                CameraCharacteristics.LENS_FACING_BACK) {
                
                // Analyser l'environnement pour détecter caméras
                detectLensReflections { camerasDetected ->
                    if (camerasDetected.isNotEmpty()) {
                        alertUser("⚠️ ${camerasDetected.size} caméra(s) détectée(s)")
                    }
                }
            }
        }
    }
}
```

---

Cette architecture de sécurité garantit une protection maximale de la vie privée tout en restant transparente et conforme aux régulations.

