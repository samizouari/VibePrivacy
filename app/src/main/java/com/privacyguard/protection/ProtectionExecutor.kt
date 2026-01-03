package com.privacyguard.protection

import android.content.Context
import com.privacyguard.assessment.models.ProtectionAction
import com.privacyguard.assessment.models.ThreatAssessment
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

/**
 * Exécuteur des actions de protection
 * 
 * Responsabilités :
 * - Recevoir les évaluations de menace
 * - Exécuter l'action de protection appropriée
 * - Gérer les transitions entre niveaux de protection
 * - Coordonner OverlayManager et les différents overlays
 * 
 * Architecture :
 * ```
 * ThreatAssessmentEngine
 *        ↓
 * ProtectionExecutor (ce fichier)
 *        ↓
 * OverlayManager
 *    ├── SoftBlurOverlay
 *    ├── DecoyScreenOverlay
 *    └── PrivacyIndicator
 * ```
 */
class ProtectionExecutor(
    private val context: Context,
    private val overlayManager: OverlayManager
) {
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // État actuel de la protection
    private val _currentProtection = MutableStateFlow(ProtectionAction.NONE)
    val currentProtection: StateFlow<ProtectionAction> = _currentProtection.asStateFlow()
    
    // Historique des menaces récentes (pour éviter oscillations)
    private val recentThreats = ArrayDeque<ThreatAssessment>(5)
    
    // Timestamp de la dernière action
    private var lastActionTime: Long = 0
    
    // Délai minimum entre changements de protection (anti-oscillation)
    private val minActionInterval = 1000L // 1 seconde
    
    // Flag pour tracker si le lock est actif (ne se désactive que par PIN)
    private var isLockActive = false
    
    // Job de restauration automatique
    private var autoRestoreJob: Job? = null
    
    /**
     * Exécute l'action de protection appropriée
     * Jour 8 : Logique basée sur nombre de visages + score
     */
    suspend fun executeProtection(assessment: ThreatAssessment) {
        // Utiliser la nouvelle stratégie basée sur visages + score
        val plan = ProtectionStrategy.determineProtectionAction(assessment)
        
        Timber.i("ProtectionExecutor: Plan -> action=${plan.mainAction}, blur=${plan.shouldBlur}, decoy=${plan.shouldDecoy}, lock=${plan.shouldLock}")
        
        // Vérifier le délai minimum entre actions
        val now = System.currentTimeMillis()
        if (now - lastActionTime < minActionInterval && plan.mainAction != ProtectionAction.NONE) {
            Timber.d("ProtectionExecutor: Skipping action (too soon)")
            return
        }
        
        // Ajouter à l'historique
        addToHistory(assessment)
        
        // Mettre à jour l'indicateur basé sur la stratégie
        val indicatorState = ProtectionStrategy.determineIndicatorState(assessment)
        overlayManager.updateIndicator(indicatorState)
        
        // Si le lock est actif, ne rien faire (il ne se désactive que par PIN)
        if (isLockActive) {
            Timber.d("ProtectionExecutor: Lock is active, ignoring new assessment")
            return
        }
        
        // Activer les overlays selon le plan
        when {
            plan.shouldLock -> {
                // Verrouillage complet (priorité max)
                activateInstantLock(assessment)
                _currentProtection.value = ProtectionAction.INSTANT_LOCK
                isLockActive = true
            }
            plan.shouldDecoy && plan.shouldBlur -> {
                // Écran leurre + Flou
                activateDecoyScreen(assessment)
                overlayManager.showBlurOverlay(plan.blurIntensity, assessment.triggerReasons)
                _currentProtection.value = ProtectionAction.DECOY_SCREEN
            }
            plan.shouldBlur -> {
                // Flou uniquement
                activateSoftBlur(assessment, plan.blurIntensity)
                _currentProtection.value = ProtectionAction.SOFT_BLUR
            }
            else -> {
                // Désactiver tout sauf l'indicateur
                deactivateProtection()
                _currentProtection.value = ProtectionAction.NONE
            }
        }
        
        lastActionTime = now
        
        Timber.i("ProtectionExecutor: Protection executed -> ${_currentProtection.value}")
    }
    
    /**
     * Active le flou doux progressif
     */
    private suspend fun activateSoftBlur(assessment: ThreatAssessment, intensity: Float = 0.5f) {
        Timber.i("ProtectionExecutor: Activating SOFT_BLUR with intensity $intensity")
        
        // Annuler la restauration automatique précédente
        autoRestoreJob?.cancel()
        
        // Afficher l'overlay de flou
        withContext(Dispatchers.Main) {
            overlayManager.showBlurOverlay(
                intensity = intensity,
                reasons = assessment.triggerReasons
            )
        }
        
        // Mettre à jour l'indicateur
        overlayManager.updateIndicator(IndicatorState.THREAT)
        
        // Programmer restauration automatique après 5 secondes sans nouvelle menace
        scheduleAutoRestore(5000L)
    }
    
    /**
     * Active l'écran leurre
     */
    private suspend fun activateDecoyScreen(assessment: ThreatAssessment) {
        Timber.i("ProtectionExecutor: Activating DECOY_SCREEN")
        
        autoRestoreJob?.cancel()
        
        withContext(Dispatchers.Main) {
            overlayManager.showDecoyScreen()
        }
        
        overlayManager.updateIndicator(IndicatorState.THREAT)
        
        // Pas de restauration automatique pour l'écran leurre
        // L'utilisateur doit le désactiver manuellement
    }
    
    /**
     * Active le verrouillage instantané
     */
    private suspend fun activateInstantLock(assessment: ThreatAssessment) {
        Timber.i("ProtectionExecutor: Activating INSTANT_LOCK")
        
        autoRestoreJob?.cancel()
        
        withContext(Dispatchers.Main) {
            overlayManager.showLockScreen()
        }
        
        // Configurer le callback pour désactiver le flag quand le PIN est entré
        overlayManager.setOnLockDismissedListener {
            isLockActive = false
            Timber.i("ProtectionExecutor: Lock deactivated by PIN")
        }
        
        overlayManager.updateIndicator(IndicatorState.THREAT)
    }
    
    /**
     * Active le mode panique
     */
    private suspend fun activatePanicMode(assessment: ThreatAssessment) {
        Timber.i("ProtectionExecutor: Activating PANIC_MODE")
        
        autoRestoreJob?.cancel()
        
        withContext(Dispatchers.Main) {
            // Masquer tout immédiatement
            overlayManager.showLockScreen()
            
            // TODO: Actions supplémentaires mode panique
            // - Fermer apps sensibles
            // - Vider presse-papier
            // - etc.
        }
        
        overlayManager.updateIndicator(IndicatorState.THREAT)
    }
    
    /**
     * Désactive toute protection
     */
    private suspend fun deactivateProtection() {
        Timber.i("ProtectionExecutor: Deactivating protection")
        
        autoRestoreJob?.cancel()
        
        withContext(Dispatchers.Main) {
            overlayManager.hideAllOverlays()
        }
        
        overlayManager.updateIndicator(IndicatorState.SAFE)
    }
    
    /**
     * Programme une restauration automatique
     */
    private fun scheduleAutoRestore(delayMs: Long) {
        autoRestoreJob?.cancel()
        autoRestoreJob = scope.launch {
            delay(delayMs)
            
            // Vérifier qu'il n'y a pas eu de nouvelles menaces
            val recentHighThreats = recentThreats.count { 
                it.shouldTriggerProtection && 
                System.currentTimeMillis() - it.timestamp < delayMs 
            }
            
            if (recentHighThreats == 0) {
                Timber.i("ProtectionExecutor: Auto-restoring (no recent threats)")
                deactivateProtection()
                _currentProtection.value = ProtectionAction.NONE
            }
        }
    }
    
    /**
     * Calcule l'intensité du flou en fonction du score de menace
     */
    private fun calculateBlurIntensity(threatScore: Int): Float {
        // Score 75-100 → intensité 0.5-1.0
        return ((threatScore - 75).coerceAtLeast(0) / 25f * 0.5f + 0.5f).coerceIn(0.5f, 1f)
    }
    
    /**
     * Ajoute une évaluation à l'historique
     */
    private fun addToHistory(assessment: ThreatAssessment) {
        if (recentThreats.size >= 5) {
            recentThreats.removeFirst()
        }
        recentThreats.addLast(assessment)
    }
    
    /**
     * Force la désactivation de la protection (appelé par l'utilisateur)
     */
    fun forceDeactivate() {
        scope.launch {
            deactivateProtection()
            _currentProtection.value = ProtectionAction.NONE
            isLockActive = false
        }
    }
    
    /**
     * Met à jour l'état de l'indicateur
     */
    fun updateIndicatorState(state: IndicatorState) {
        overlayManager.updateIndicator(state)
    }
    
    /**
     * Nettoyage
     */
    fun cleanup() {
        autoRestoreJob?.cancel()
        scope.cancel()
        overlayManager.cleanup()
    }
}

/**
 * États de l'indicateur de confidentialité
 */
enum class IndicatorState {
    SAFE,       // Vert - Pas de menace
    MONITORING, // Jaune - Surveillance active
    THREAT      // Rouge - Menace détectée
}


