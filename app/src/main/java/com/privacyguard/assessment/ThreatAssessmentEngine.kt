package com.privacyguard.assessment

import com.privacyguard.assessment.models.*
import com.privacyguard.sensors.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

/**
 * Moteur principal d'évaluation des menaces
 * 
 * Responsabilités :
 * - Orchestrer la fusion des capteurs
 * - Gérer le pipeline d'évaluation en temps réel
 * - Émettre les évaluations via Flow
 * - Gérer le debounce et le filtrage
 * 
 * Architecture :
 * ```
 * SensorManager (4 capteurs)
 *        ↓
 * ThreatAssessmentEngine (ce fichier)
 *        ↓ (Flow<ThreatAssessment>)
 * PrivacyGuardService
 *        ↓
 * ProtectionExecutor
 * ```
 */
class ThreatAssessmentEngine(
    private val sensorDataFusion: SensorDataFusion = SensorDataFusion(),
    private val config: ThreatAssessmentConfig = ThreatAssessmentConfig()
) {
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Contexte d'évaluation (mis à jour dynamiquement)
    private val _context = MutableStateFlow(AssessmentContext())
    val context: StateFlow<AssessmentContext> = _context.asStateFlow()
    
    // Dernière évaluation
    private val _lastAssessment = MutableStateFlow<ThreatAssessment?>(null)
    val lastAssessment: StateFlow<ThreatAssessment?> = _lastAssessment.asStateFlow()
    
    // Historique récent des évaluations (pour détection de patterns)
    private val assessmentHistory = ArrayDeque<ThreatAssessment>(10)
    
    // Timestamp du dernier déclenchement (pour debounce)
    private var lastTriggerTime: Long = 0
    
    /**
     * Traite un flux de données de capteurs et émet des évaluations de menace
     * 
     * @param sensorDataFlow Flow de SensorDataSnapshot du SensorManager
     * @return Flow d'évaluations de menace
     */
    fun processFlow(sensorDataFlow: Flow<SensorDataSnapshot>): Flow<ThreatAssessment> {
        return sensorDataFlow
            .debounce(100) // Anti-rebond 100ms pour éviter surcharge
            .mapNotNull { snapshot ->
                processSnapshot(snapshot)
            }
            .distinctUntilChangedBy { assessment ->
                // Éviter émissions répétées si pas de changement significatif
                // IMPORTANT: Inclure le threatLevel pour mettre à jour l'indicateur
                Triple(
                    assessment.threatLevel,
                    assessment.shouldTriggerProtection,
                    assessment.threatScore / 10  // Arrondir par 10 pour réduire le bruit
                )
            }
            .onEach { assessment ->
                _lastAssessment.value = assessment
                addToHistory(assessment)
                
                Timber.i("ThreatAssessmentEngine: Assessment emitted - Score=${assessment.threatScore}, Level=${assessment.threatLevel}, Trigger=${assessment.shouldTriggerProtection}")
                
                if (assessment.shouldTriggerProtection) {
                    Timber.w("ThreatAssessmentEngine: ⚠️ THREAT DETECTED! " +
                            "Score=${assessment.threatScore}, " +
                            "Action=${assessment.recommendedAction}, " +
                            "Reasons=${assessment.triggerReasons}")
                }
            }
    }
    
    /**
     * Traite un snapshot de capteurs et retourne une évaluation
     */
    fun processSnapshot(snapshot: SensorDataSnapshot): ThreatAssessment? {
        // Vérifier qu'on a au moins un capteur avec des données
        if (snapshot.cameraData == null &&
            snapshot.audioData == null &&
            snapshot.motionData == null &&
            snapshot.proximityData == null) {
            Timber.v("ThreatAssessmentEngine: No sensor data available")
            return null
        }
        
        // Évaluer la menace
        val assessment = sensorDataFusion.evaluate(
            snapshot = snapshot,
            config = config,
            context = _context.value
        )
        
        // Appliquer le debounce sur les déclenchements
        if (assessment.shouldTriggerProtection) {
            val now = System.currentTimeMillis()
            if (now - lastTriggerTime < config.debounceTimeMs) {
                Timber.d("ThreatAssessmentEngine: Debounce - skipping trigger")
                return assessment.copy(shouldTriggerProtection = false)
            }
            lastTriggerTime = now
        }
        
        // Mettre à jour le contexte avec l'historique
        updateContextFromHistory(assessment)
        
        return assessment
    }
    
    /**
     * Évalue une menace de manière synchrone (pour tests ou usage ponctuel)
     */
    fun evaluate(
        cameraData: CameraData? = null,
        audioData: AudioData? = null,
        motionData: MotionData? = null,
        proximityData: ProximityData? = null
    ): ThreatAssessment {
        val snapshot = SensorDataSnapshot(
            timestamp = System.currentTimeMillis(),
            cameraData = cameraData,
            audioData = audioData,
            motionData = motionData,
            proximityData = proximityData
        )
        
        return sensorDataFusion.evaluate(
            snapshot = snapshot,
            config = config,
            context = _context.value
        )
    }
    
    /**
     * Met à jour le mode de protection
     */
    fun setProtectionMode(mode: ProtectionMode) {
        _context.value = _context.value.copy(currentMode = mode)
        Timber.i("ThreatAssessmentEngine: Protection mode changed to $mode (threshold=${mode.threshold})")
    }
    
    /**
     * Met à jour le contexte d'évaluation
     */
    fun updateContext(update: (AssessmentContext) -> AssessmentContext) {
        _context.value = update(_context.value)
    }
    
    /**
     * Indique si on est en zone de confiance
     */
    fun setTrustZone(inTrustZone: Boolean) {
        _context.value = _context.value.copy(isInTrustZone = inTrustZone)
        Timber.i("ThreatAssessmentEngine: Trust zone = $inTrustZone")
    }
    
    /**
     * Met à jour le niveau de bruit ambiant (pour adaptation des poids)
     */
    fun setAmbientNoiseLevel(level: Float) {
        _context.value = _context.value.copy(ambientNoiseLevel = level.coerceIn(0f, 1f))
    }
    
    /**
     * Met à jour le niveau de luminosité (pour adaptation des poids)
     */
    fun setLightLevel(level: Float) {
        _context.value = _context.value.copy(lightLevel = level.coerceIn(0f, 1f))
    }
    
    /**
     * Ajoute une évaluation à l'historique
     */
    private fun addToHistory(assessment: ThreatAssessment) {
        if (assessmentHistory.size >= 10) {
            assessmentHistory.removeFirst()
        }
        assessmentHistory.addLast(assessment)
    }
    
    /**
     * Met à jour le contexte en fonction de l'historique
     */
    private fun updateContextFromHistory(assessment: ThreatAssessment) {
        if (assessment.shouldTriggerProtection) {
            _context.value = _context.value.copy(
                lastThreatTime = assessment.timestamp,
                consecutiveThreatCount = _context.value.consecutiveThreatCount + 1
            )
        } else {
            // Reset compteur si pas de menace
            if (_context.value.consecutiveThreatCount > 0) {
                _context.value = _context.value.copy(consecutiveThreatCount = 0)
            }
        }
    }
    
    /**
     * Retourne les statistiques récentes
     */
    fun getRecentStats(): AssessmentStats {
        val recentAssessments = assessmentHistory.toList()
        
        val threatsDetected = recentAssessments.count { it.shouldTriggerProtection }
        val avgScore = if (recentAssessments.isNotEmpty()) {
            recentAssessments.map { it.threatScore }.average().toInt()
        } else 0
        
        return AssessmentStats(
            totalAssessments = recentAssessments.size,
            threatsDetected = threatsDetected,
            averageScore = avgScore,
            lastThreatTime = _context.value.lastThreatTime
        )
    }
    
    /**
     * Réinitialise le moteur
     */
    fun reset() {
        _context.value = AssessmentContext()
        _lastAssessment.value = null
        assessmentHistory.clear()
        lastTriggerTime = 0
        Timber.i("ThreatAssessmentEngine: Reset complete")
    }
    
    /**
     * Nettoyage
     */
    fun cleanup() {
        scope.cancel()
        reset()
    }
}

/**
 * Statistiques d'évaluation récentes
 */
data class AssessmentStats(
    val totalAssessments: Int,
    val threatsDetected: Int,
    val averageScore: Int,
    val lastThreatTime: Long?
)


