package com.privacyguard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.privacyguard.R
import com.privacyguard.assessment.ThreatAssessmentEngine
import com.privacyguard.assessment.models.ProtectionAction
import com.privacyguard.assessment.models.ProtectionMode
import com.privacyguard.assessment.models.ThreatAssessment
import com.privacyguard.data.AppDatabase
import com.privacyguard.data.SessionRepository
import com.privacyguard.protection.IndicatorState
import com.privacyguard.protection.IntruderCapture
import com.privacyguard.protection.OverlayManager
import com.privacyguard.protection.ProtectionExecutor
import com.privacyguard.sensors.SensorManager
import com.privacyguard.sensors.ThreatLevel
import com.privacyguard.trust.TrustZonesManager
import com.privacyguard.trust.WiFiZoneDetector
import com.privacyguard.trust.models.TrustZoneCheckResult
import com.privacyguard.ui.MainActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Service de premier plan (Foreground Service) pour Privacy Guard
 * 
 * Hérite de LifecycleService pour être compatible avec CameraX
 * 
 * Ce service tourne en continu pour :
 * - Surveiller les capteurs (caméra, micro, mouvement, etc.)
 * - Détecter les menaces en temps réel
 * - Afficher l'overlay de protection si nécessaire
 * 
 * Architecture :
 * - Service de premier plan avec notification permanente
 * - Lifecycle indépendant de l'UI
 * - Communication via Intents et Broadcasts
 */
class PrivacyGuardService : LifecycleService() {
    
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "privacy_guard_service"
        private const val NOTIFICATION_CHANNEL_NAME = "Protection Privacy Guard"
        private const val NOTIFICATION_ID = 1
        
        // Actions Intent
        const val ACTION_START_PROTECTION = "com.privacyguard.START_PROTECTION"
        const val ACTION_STOP_PROTECTION = "com.privacyguard.STOP_PROTECTION"
        const val ACTION_PAUSE_PROTECTION = "com.privacyguard.PAUSE_PROTECTION"
        
        // Broadcast Actions
        const val ACTION_THREAT_ASSESSMENT_UPDATE = "com.privacyguard.THREAT_ASSESSMENT_UPDATE"
        const val EXTRA_THREAT_SCORE = "threat_score"
        const val EXTRA_THREAT_LEVEL = "threat_level"
        const val EXTRA_SHOULD_TRIGGER = "should_trigger"
        
        const val ACTION_SESSION_STATS_UPDATE = "com.privacyguard.SESSION_STATS_UPDATE"
        const val EXTRA_SESSION_DURATION = "session_duration"
        const val EXTRA_THREATS_DETECTED = "threats_detected"
        const val EXTRA_AVG_SCORE = "avg_score"
        const val EXTRA_CURRENT_MODE = "current_mode"
        
        // État du service
        private var isRunning = false
        
        /**
         * Vérifie si le service est actuellement actif
         */
        fun isServiceRunning(): Boolean = isRunning
        
        /**
         * Démarre le service de protection
         */
        fun startService(context: Context) {
            val intent = Intent(context, PrivacyGuardService::class.java).apply {
                action = ACTION_START_PROTECTION
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        /**
         * Arrête le service de protection
         */
        fun stopService(context: Context) {
            val intent = Intent(context, PrivacyGuardService::class.java).apply {
                action = ACTION_STOP_PROTECTION
            }
            context.startService(intent)
        }
    }
    
    private var isPaused = false
    
    // Gestionnaire de capteurs
    private var sensorManager: SensorManager? = null
    
    // Moteur d'évaluation des menaces
    private var threatAssessmentEngine: ThreatAssessmentEngine? = null
    
    // Repository pour sauvegarder les sessions
    private var sessionRepository: SessionRepository? = null
    
    // Gestionnaire de zones de confiance
    private var trustZonesManager: TrustZonesManager? = null
    private var wifiDetector: WiFiZoneDetector? = null
    
    // Job de collecte des données
    private var assessmentJob: Job? = null
    
    // Gestionnaire d'overlays
    private var overlayManager: OverlayManager? = null
    
    // Exécuteur de protection
    private var protectionExecutor: ProtectionExecutor? = null
    
    // Capture d'intrus
    private var intruderCapture: IntruderCapture? = null
    
    override fun onCreate() {
        super.onCreate()
        Timber.d("PrivacyGuardService onCreate()")
        
        // Créer le canal de notification
        createNotificationChannel()
        
        // Initialiser la base de données et le repository
        val database = AppDatabase.getInstance(applicationContext)
        sessionRepository = SessionRepository(database.sessionDao())
        Timber.d("PrivacyGuardService: SessionRepository initialized")
        
        // Ne pas initialiser les capteurs ici, attendre startProtection()
        // L'initialisation se fera dans startProtection() quand le service est vraiment prêt
        Timber.d("PrivacyGuardService: onCreate completed, waiting for startProtection()")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId) // Appel du super pour LifecycleService
        Timber.d("PrivacyGuardService onStartCommand() - action: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_PROTECTION -> {
                startProtection()
            }
            ACTION_STOP_PROTECTION -> {
                stopProtection()
            }
            ACTION_PAUSE_PROTECTION -> {
                pauseProtection()
            }
        }
        
        // Le service redémarre automatiquement si tué par le système
        return START_STICKY
    }
    
    // Service non lié (pas de binding)
    // onBind n'est pas nécessaire pour un service non lié
    
    override fun onDestroy() {
        super.onDestroy()
        Timber.d("PrivacyGuardService onDestroy()")
        isRunning = false
        isPaused = false
        
        // Arrêter le job d'évaluation
        assessmentJob?.cancel()
        assessmentJob = null
        
        // Arrêter tous les capteurs
        lifecycleScope.launch {
            sensorManager?.stopAll()
            sensorManager?.cleanup()
        }
        
        // Nettoyer le moteur d'évaluation
        threatAssessmentEngine?.cleanup()
        threatAssessmentEngine = null
        
        // Nettoyer les zones de confiance
        trustZonesManager?.stopMonitoring()
        trustZonesManager = null
        wifiDetector = null
        
        // Nettoyer le système de protection
        protectionExecutor?.cleanup()
        protectionExecutor = null
        
        overlayManager?.cleanup()
        overlayManager = null
        
        Timber.i("PrivacyGuardService: All resources cleaned up")
    }
    
    /**
     * Démarre la protection
     */
    private fun startProtection() {
        Timber.i("Starting privacy protection...")
        
        // Passer en service de premier plan avec notification
        startForeground(NOTIFICATION_ID, createNotification(isActive = true))
        
        isRunning = true
        isPaused = false
        
        // Initialiser le gestionnaire de zones de confiance
        wifiDetector = WiFiZoneDetector(applicationContext)
        trustZonesManager = TrustZonesManager(applicationContext, wifiDetector!!)
        trustZonesManager?.startMonitoring()
        
        // Initialiser le gestionnaire de visages de confiance
        val faceEncoder = com.privacyguard.trust.FaceEncoder(applicationContext)
        val faceMatcher = com.privacyguard.trust.FaceMatcher()
        val trustFacesManager = com.privacyguard.trust.TrustFacesManager(
            applicationContext,
            faceEncoder,
            faceMatcher
        )
        
        // Initialiser et démarrer tous les capteurs et la protection
        lifecycleScope.launch {
            try {
                // Initialiser le SensorManager si pas déjà fait
                if (sensorManager == null) {
                    Timber.d("PrivacyGuardService: Initializing SensorManager with TrustFaces...")
                    sensorManager = SensorManager(
                        this@PrivacyGuardService,
                        this@PrivacyGuardService,
                        trustFacesManager
                    ).apply {
                        initialize()
                    }
                    Timber.i("PrivacyGuardService: SensorManager initialized with face recognition")
                }
                
                // Démarrer tous les capteurs
                sensorManager?.startAll()
                Timber.i("All sensors started successfully")
                
                // Initialiser le moteur d'évaluation des menaces
                if (threatAssessmentEngine == null) {
                    // Lire le mode sauvegardé dans les préférences
                    val prefs = getSharedPreferences("privacy_guard_prefs", Context.MODE_PRIVATE)
                    val savedModeName = prefs.getString("protection_mode", ProtectionMode.DISCRETE.name)
                    val selectedMode = try {
                        ProtectionMode.valueOf(savedModeName ?: ProtectionMode.DISCRETE.name)
                    } catch (e: Exception) {
                        Timber.w("Invalid protection mode '$savedModeName', using DISCRETE")
                        ProtectionMode.DISCRETE
                    }
                    
                    threatAssessmentEngine = ThreatAssessmentEngine(
                        sessionRepository = sessionRepository
                    ).apply {
                        setProtectionMode(selectedMode)
                        startSession() // Démarrer le tracking de la session
                    }
                    Timber.i("ThreatAssessmentEngine initialized with ${selectedMode.name} mode (threshold=${selectedMode.threshold}) and DB persistence")
                }
                
                // Broadcaster les stats de session périodiquement
                launch {
                    while (isRunning) {
                        threatAssessmentEngine?.getSessionStats()?.let { stats ->
                            broadcastSessionStats(stats)
                        }
                        delay(2000) // Toutes les 2 secondes
                    }
                }
                
                        // Initialiser la capture d'intrus
                if (intruderCapture == null) {
                    intruderCapture = IntruderCapture(this@PrivacyGuardService)
                    Timber.i("IntruderCapture initialized")
                }
                
                // Initialiser l'OverlayManager et le ProtectionExecutor si permission accordée
                initializeProtectionSystem()
                
                // Surveiller les zones de confiance
                launch {
                    while (isRunning) {
                        checkTrustZone()
                        delay(5000) // Vérifier toutes les 5 secondes
                    }
                }
                
                // Collecter et analyser les données des capteurs
                assessmentJob = launch {
                    Timber.i("PrivacyGuardService: Starting assessment collection...")
                    Timber.i("PrivacyGuardService: sensorManager=${sensorManager != null}, threatEngine=${threatAssessmentEngine != null}")
                    
                    if (sensorManager == null) {
                        Timber.e("PrivacyGuardService: SensorManager is NULL! Cannot start assessment")
                        return@launch
                    }
                    
                    if (threatAssessmentEngine == null) {
                        Timber.e("PrivacyGuardService: ThreatAssessmentEngine is NULL! Cannot start assessment")
                        return@launch
                    }
                    
                    val sensorFlow = sensorManager?.combinedSensorData
                    Timber.i("PrivacyGuardService: Got sensor flow=${sensorFlow != null}, processing...")
                    
                    if (sensorFlow == null) {
                        Timber.e("PrivacyGuardService: combinedSensorData is NULL!")
                        return@launch
                    }
                    
                    threatAssessmentEngine?.processFlow(sensorFlow)?.collectLatest { assessment ->
                            // Log de l'évaluation
                            Timber.i("PrivacyGuardService: RECEIVED assessment - Score=${assessment.threatScore}, " +
                                    "Level=${assessment.threatLevel}, " +
                                    "Camera=${(assessment.sensorContributions.cameraScore * 100).toInt()}%, " +
                                    "Audio=${(assessment.sensorContributions.audioScore * 100).toInt()}%")
                            
                            // Broadcaster le score à l'UI
                            broadcastThreatAssessment(assessment)
                            
                            // Mettre à jour l'indicateur selon le niveau de menace
                            updateIndicatorFromAssessment(assessment)
                            
                            // Exécuter l'action de protection si nécessaire
                            protectionExecutor?.executeProtection(assessment)
                            
                            // Capturer photo selon la stratégie (2+ visages inconnus en BALANCED/PARANOIA)
                            val plan = com.privacyguard.protection.ProtectionStrategy.determineProtectionAction(assessment)
                            if (plan.shouldCapture) {
                                Timber.w("📸 PrivacyGuardService: ${assessment.unknownFacesCount} unknown faces detected! Capturing intruder photo...")
                                captureIntruderPhoto(assessment.threatLevel.name)
                            }
                        } ?: Timber.e("PrivacyGuardService: processFlow returned NULL!")
                }
                Timber.i("Threat assessment pipeline started")
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to start sensors: ${e.message}")
                Timber.e(e, "Stack trace:", e)
                // Ne pas faire crasher l'app, juste logger l'erreur
            }
        }
        
        Timber.i("Privacy protection started successfully")
    }
    
    /**
     * Initialise le système de protection (overlays)
     */
    private fun initializeProtectionSystem() {
        // Vérifier la permission d'overlay
        if (!Settings.canDrawOverlays(this)) {
            Timber.w("PrivacyGuardService: No overlay permission - protection will be limited to notifications")
            return
        }
        
        // Initialiser l'OverlayManager
        if (overlayManager == null) {
            overlayManager = OverlayManager(this).apply {
                onOverlayDismissed = {
                    Timber.i("PrivacyGuardService: Overlay dismissed by user")
                    protectionExecutor?.forceDeactivate()
                }
            }
            Timber.i("PrivacyGuardService: OverlayManager initialized")
        }
        
        // Initialiser le ProtectionExecutor
        if (protectionExecutor == null && overlayManager != null) {
            protectionExecutor = ProtectionExecutor(this, overlayManager!!)
            Timber.i("PrivacyGuardService: ProtectionExecutor initialized")
        }
        
        // Afficher l'indicateur de confidentialité
        overlayManager?.initializeIndicator()
        Timber.i("PrivacyGuardService: Privacy indicator shown")
    }
    
    /**
     * Vérifie si on est dans une zone de confiance
     */
    private fun checkTrustZone() {
        val result = trustZonesManager?.checkCurrentLocation()
        
        when (result) {
            is TrustZoneCheckResult.InTrustZone -> {
                val zone = result.zone
                Timber.d("TrustZones: In '${zone.name}' - behavior: ${zone.protectionBehavior}")
                
                // Informer le moteur d'évaluation qu'on est en zone de confiance
                threatAssessmentEngine?.setTrustZone(true)
                
                // Appliquer le comportement de la zone
                when (zone.protectionBehavior.protectionMode) {
                    ProtectionMode.TRUST_ZONE -> {
                        threatAssessmentEngine?.setProtectionMode(ProtectionMode.TRUST_ZONE)
                    }
                    ProtectionMode.DISCRETE -> {
                        threatAssessmentEngine?.setProtectionMode(ProtectionMode.DISCRETE)
                    }
                    ProtectionMode.BALANCED -> {
                        threatAssessmentEngine?.setProtectionMode(ProtectionMode.BALANCED)
                    }
                    null -> {
                        // Protection désactivée dans cette zone
                        // On ne fait rien, laisse le moteur gérer
                    }
                    else -> {}
                }
            }
            is TrustZoneCheckResult.NotInTrustZone -> {
                threatAssessmentEngine?.setTrustZone(false)
            }
            null -> {
                // TrustZonesManager non initialisé
            }
        }
    }
    
    /**
     * Met à jour l'indicateur selon l'évaluation
     * 
     * LOGIQUE CORRIGÉE POUR STABILITÉ :
     * - Utilise principalement le score global (pas de logique hybride complexe)
     * - Seuils clairs et cohérents avec ThreatLevel
     * - Correspond aux seuils de SensorDataFusion.scoreToThreatLevel()
     */
    private fun updateIndicatorFromAssessment(assessment: ThreatAssessment) {
        // Utiliser le ThreatLevel calculé par l'engine (plus cohérent)
        val state = when (assessment.threatLevel) {
            ThreatLevel.CRITICAL, ThreatLevel.HIGH -> {
                // Rouge : menace élevée ou critique
                IndicatorState.THREAT
            }
            ThreatLevel.MEDIUM -> {
                // Jaune : menace moyenne (score 40-59)
                IndicatorState.MONITORING
            }
            ThreatLevel.LOW, ThreatLevel.NONE -> {
                // Vert : pas de menace ou menace faible
                IndicatorState.SAFE
            }
        }
        
        protectionExecutor?.updateIndicatorState(state)
        
        // Log détaillé avec contributions
        val contrib = assessment.sensorContributions
        Timber.i("🚦 Indicator: $state | ThreatLevel=${assessment.threatLevel} | Score=${assessment.threatScore}/100 | " +
                "📹 Camera=${(contrib.cameraScore * 100).toInt()}% | " +
                "🔊 Audio=${(contrib.audioScore * 100).toInt()}% | " +
                "📱 Motion=${(contrib.motionScore * 100).toInt()}% | " +
                "👋 Proximity=${(contrib.proximityScore * 100).toInt()}%")
    }
    
    /**
     * Arrête la protection
     */
    private fun stopProtection() {
        Timber.i("Stopping privacy protection...")
        
        isRunning = false
        isPaused = false
        
        // Finaliser la session en DB
        threatAssessmentEngine?.stopSession()
        
        // Arrêter tous les capteurs
        lifecycleScope.launch {
            sensorManager?.stopAll()
        }
        
        // Arrêter les zones de confiance
        trustZonesManager?.stopMonitoring()
        
        // Retirer tous les overlays
        overlayManager?.hideAllOverlays()
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        
        Timber.i("Privacy protection stopped and session saved to DB")
    }
    
    /**
     * Met en pause la protection (garde le service actif mais désactive les capteurs)
     */
    private fun pauseProtection() {
        Timber.i("Pausing privacy protection...")
        
        isPaused = true
        
        // Mettre à jour la notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(isActive = false))
        
        // Mettre en pause les capteurs
        lifecycleScope.launch {
            sensorManager?.pauseAll()
        }
        
        // Annuler le job d'évaluation
        assessmentJob?.cancel()
        assessmentJob = null
        
        Timber.i("Privacy protection paused")
    }
    
    /**
     * Vérifie si la permission d'overlay est accordée
     */
    fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }
    
    /**
     * Capture une photo d'intrus en cas de menace
     */
    private fun captureIntruderPhoto(threatLevel: String) {
        lifecycleScope.launch {
            try {
                Timber.w("📸 captureIntruderPhoto: Starting capture for threat level $threatLevel")
                
                // Obtenir la dernière image de la caméra
                val cameraSensor = sensorManager?.getCameraSensor()
                Timber.d("📸 captureIntruderPhoto: cameraSensor = ${cameraSensor != null}")
                
                val lastImage = cameraSensor?.getLastCapturedBitmap()
                Timber.d("📸 captureIntruderPhoto: lastImage = ${lastImage != null}, size = ${lastImage?.width}x${lastImage?.height}")
                
                if (lastImage != null && intruderCapture != null) {
                    Timber.w("📸 captureIntruderPhoto: Calling captureFromBitmap...")
                    val photo = intruderCapture?.captureFromBitmap(lastImage, threatLevel)
                    if (photo != null) {
                        Timber.i("📸 ✅ Intruder photo captured successfully: ${photo.fileName}")
                    } else {
                        Timber.e("📸 ❌ captureFromBitmap returned null!")
                    }
                } else {
                    if (lastImage == null) {
                        Timber.e("📸 ❌ No camera image available for intruder capture")
                    }
                    if (intruderCapture == null) {
                        Timber.e("📸 ❌ IntruderCapture is null!")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error capturing intruder photo")
            }
        }
    }
    
    /**
     * Crée une notification de menace détectée
     */
    private fun createThreatNotification(reasons: List<String>): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("⚠️ Menace détectée")
            .setContentText(reasons.firstOrNull() ?: "Protection activée")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
    }
    
    /**
     * Crée le canal de notification (requis pour Android O+)
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW // Importance basse pour ne pas déranger
        ).apply {
            description = "Affiche l'état de la protection Privacy Guard"
            setShowBadge(false) // Pas de badge sur l'icône
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
    
    /**
     * Crée la notification du service
     */
    private fun createNotification(isActive: Boolean): Notification {
        // Intent pour ouvrir l'app au clic sur la notification
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        // Icône et texte selon l'état
        val (icon, title, text) = if (isActive) {
            Triple(
                android.R.drawable.ic_menu_view, // TODO: Remplacer par icône custom
                "🛡️ Protection active",
                "Privacy Guard surveille votre environnement"
            )
        } else {
            Triple(
                android.R.drawable.ic_menu_close_clear_cancel,
                "⏸️ Protection en pause",
                "Cliquez pour reprendre la protection"
            )
        }
        
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Notification non supprimable par swipe
            .setAutoCancel(false)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Priorité basse
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    /**
     * Envoie un broadcast avec l'évaluation de menace pour l'UI
     */
    private fun broadcastThreatAssessment(assessment: ThreatAssessment) {
        val intent = Intent(ACTION_THREAT_ASSESSMENT_UPDATE).apply {
            putExtra(EXTRA_THREAT_SCORE, assessment.threatScore)
            putExtra(EXTRA_THREAT_LEVEL, assessment.threatLevel.name)
            putExtra(EXTRA_SHOULD_TRIGGER, assessment.shouldTriggerProtection)
        }
        sendBroadcast(intent)
        Timber.v("PrivacyGuardService: Broadcasted assessment - Score=${assessment.threatScore}")
    }
    
    /**
     * Envoie un broadcast avec les stats de session pour le Dashboard
     */
    private fun broadcastSessionStats(stats: com.privacyguard.assessment.SessionStats) {
        val intent = Intent(ACTION_SESSION_STATS_UPDATE).apply {
            putExtra(EXTRA_SESSION_DURATION, stats.sessionDurationMs)
            putExtra(EXTRA_THREATS_DETECTED, stats.totalThreatsDetected)
            putExtra(EXTRA_AVG_SCORE, stats.averageScore)
            putExtra(EXTRA_CURRENT_MODE, stats.currentMode.name)
        }
        sendBroadcast(intent)
        Timber.v("PrivacyGuardService: Broadcasted session stats - Duration=${stats.sessionDurationMs}ms, Threats=${stats.totalThreatsDetected}")
    }
}

