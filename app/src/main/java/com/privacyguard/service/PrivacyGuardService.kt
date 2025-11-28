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
import com.privacyguard.protection.IndicatorState
import com.privacyguard.protection.OverlayManager
import com.privacyguard.protection.ProtectionExecutor
import com.privacyguard.sensors.SensorManager
import com.privacyguard.sensors.ThreatLevel
import com.privacyguard.ui.MainActivity
import kotlinx.coroutines.Job
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
    
    // Job de collecte des données
    private var assessmentJob: Job? = null
    
    // Gestionnaire d'overlays
    private var overlayManager: OverlayManager? = null
    
    // Exécuteur de protection
    private var protectionExecutor: ProtectionExecutor? = null
    
    override fun onCreate() {
        super.onCreate()
        Timber.d("PrivacyGuardService onCreate()")
        
        // Créer le canal de notification
        createNotificationChannel()
        
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
        
        // Initialiser et démarrer tous les capteurs et la protection
        lifecycleScope.launch {
            try {
                // Initialiser le SensorManager si pas déjà fait
                if (sensorManager == null) {
                    Timber.d("PrivacyGuardService: Initializing SensorManager...")
                    sensorManager = SensorManager(this@PrivacyGuardService, this@PrivacyGuardService).apply {
                        initialize()
                    }
                    Timber.i("PrivacyGuardService: SensorManager initialized")
                }
                
                // Démarrer tous les capteurs
                sensorManager?.startAll()
                Timber.i("All sensors started successfully")
                
                // Initialiser le moteur d'évaluation des menaces
                if (threatAssessmentEngine == null) {
                    threatAssessmentEngine = ThreatAssessmentEngine().apply {
                        setProtectionMode(ProtectionMode.DISCRETE) // Mode Discret par défaut (MVP)
                    }
                    Timber.i("ThreatAssessmentEngine initialized with DISCRETE mode")
                }
                
                // Initialiser l'OverlayManager et le ProtectionExecutor si permission accordée
                initializeProtectionSystem()
                
                // Collecter et analyser les données des capteurs
                assessmentJob = launch {
                    sensorManager?.combinedSensorData?.let { sensorFlow ->
                        threatAssessmentEngine?.processFlow(sensorFlow)?.collectLatest { assessment ->
                            // Log de l'évaluation
                            Timber.d("Assessment: Score=${assessment.threatScore}, " +
                                    "Level=${assessment.threatLevel}, " +
                                    "Trigger=${assessment.shouldTriggerProtection}")
                            
                            // Mettre à jour l'indicateur selon le niveau de menace
                            updateIndicatorFromAssessment(assessment)
                            
                            // Exécuter l'action de protection si nécessaire
                            protectionExecutor?.executeProtection(assessment)
                        }
                    }
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
     * Met à jour l'indicateur selon l'évaluation
     */
    private fun updateIndicatorFromAssessment(assessment: ThreatAssessment) {
        val state = when {
            assessment.shouldTriggerProtection -> IndicatorState.THREAT
            assessment.threatScore >= 50 -> IndicatorState.MONITORING  // Seuil relevé de 30 à 50
            else -> IndicatorState.SAFE
        }
        protectionExecutor?.updateIndicatorState(state)
        Timber.v("Indicator state: $state (score=${assessment.threatScore})")
    }
    
    /**
     * Arrête la protection
     */
    private fun stopProtection() {
        Timber.i("Stopping privacy protection...")
        
        isRunning = false
        isPaused = false
        
        // Arrêter tous les capteurs
        lifecycleScope.launch {
            sensorManager?.stopAll()
        }
        
        // TODO Jour 4: Retirer l'overlay
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        
        Timber.i("Privacy protection stopped")
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
}

