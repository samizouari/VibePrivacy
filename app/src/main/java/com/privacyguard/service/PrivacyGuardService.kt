package com.privacyguard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.privacyguard.R
import com.privacyguard.assessment.ThreatAssessmentEngine
import com.privacyguard.assessment.models.ProtectionAction
import com.privacyguard.assessment.models.ProtectionMode
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
        
        // TODO Jour 4: Retirer l'overlay si présent
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
        
        // Initialiser et démarrer tous les capteurs
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
                
                // Collecter et analyser les données des capteurs
                assessmentJob = launch {
                    sensorManager?.combinedSensorData?.let { sensorFlow ->
                        threatAssessmentEngine?.processFlow(sensorFlow)?.collectLatest { assessment ->
                            // Log de l'évaluation
                            Timber.d("Assessment: Score=${assessment.threatScore}, " +
                                    "Level=${assessment.threatLevel}, " +
                                    "Trigger=${assessment.shouldTriggerProtection}")
                            
                            // Si protection doit être déclenchée
                            if (assessment.shouldTriggerProtection) {
                                handleThreatDetected(assessment.recommendedAction, assessment.triggerReasons)
                            }
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
     * Gère la détection d'une menace
     * TODO Jour 4: Implémenter les actions de protection réelles
     */
    private fun handleThreatDetected(action: ProtectionAction, reasons: List<String>) {
        Timber.w("⚠️ THREAT DETECTED! Action: $action")
        Timber.w("⚠️ Reasons: ${reasons.joinToString(", ")}")
        
        when (action) {
            ProtectionAction.NONE -> {
                // Rien à faire
            }
            ProtectionAction.SOFT_BLUR -> {
                // TODO Jour 4: Activer overlay flou
                Timber.i("TODO: Activate soft blur overlay")
            }
            ProtectionAction.DECOY_SCREEN -> {
                // TODO Jour 4: Afficher écran leurre
                Timber.i("TODO: Show decoy screen")
            }
            ProtectionAction.INSTANT_LOCK -> {
                // TODO Jour 4: Verrouillage instantané
                Timber.i("TODO: Instant lock")
            }
            ProtectionAction.PANIC_MODE -> {
                // TODO Jour 4: Mode panique
                Timber.i("TODO: Panic mode")
            }
        }
        
        // Mettre à jour la notification pour indiquer menace détectée
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createThreatNotification(reasons))
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

