package com.privacyguard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.privacyguard.R
import com.privacyguard.ui.MainActivity
import timber.log.Timber

/**
 * Service de premier plan (Foreground Service) pour Privacy Guard
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
class PrivacyGuardService : Service() {
    
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
    
    override fun onCreate() {
        super.onCreate()
        Timber.d("PrivacyGuardService onCreate()")
        
        // Créer le canal de notification
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
    
    override fun onBind(intent: Intent?): IBinder? {
        // Service non lié (pas de binding)
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Timber.d("PrivacyGuardService onDestroy()")
        isRunning = false
        isPaused = false
        
        // TODO Jour 2: Arrêter tous les capteurs
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
        
        // TODO Jour 2: Initialiser et démarrer les capteurs
        // - CameraSensor
        // - AudioSensor
        // - MotionSensor
        // - ProximitySensor
        
        // TODO Jour 3: Démarrer l'analyse de fusion des capteurs
        
        Timber.i("Privacy protection started successfully")
    }
    
    /**
     * Arrête la protection
     */
    private fun stopProtection() {
        Timber.i("Stopping privacy protection...")
        
        isRunning = false
        isPaused = false
        
        // TODO Jour 2: Arrêter tous les capteurs
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
        
        // TODO Jour 2: Mettre en pause les capteurs (mais ne pas les détruire)
        
        Timber.i("Privacy protection paused")
    }
    
    /**
     * Crée le canal de notification (requis pour Android O+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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

