package com.privacyguard.assessment

import com.privacyguard.assessment.models.*
import com.privacyguard.data.SessionRepository
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
 * - Sauvegarder les sessions et événements de menace
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
    private val config: ThreatAssessmentConfig = ThreatAssessmentConfig(),
    private val sessionRepository: SessionRepository? = null // Optionnel pour les tests
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
    
    // === TRACKING SESSION ===
    private var sessionStartTime: Long = 0
    private var currentSessionId: Long? = null // ID de la session en cours dans la DB
    private var totalThreatsDetected: Int = 0
    private var totalThreatScore: Float = 0f
    private var totalAssessments: Int = 0
    private var maxThreatScore: Float = 0f
    private var maxThreatLevel: ThreatLevel = ThreatLevel.NONE
    private val threatEvents = mutableListOf<ThreatEvent>()
    
    /**
     * Traite un flux de données de capteurs et émet des évaluations de menace
     * 
     * @param sensorDataFlow Flow de SensorDataSnapshot du SensorManager
     * @return Flow d'évaluations de menace
     */
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    fun processFlow(sensorDataFlow: Flow<SensorDataSnapshot>): Flow<ThreatAssessment> {
        return sensorDataFlow
            .sample(500) // Prendre un échantillon toutes les 500ms (au lieu de debounce qui attend le silence)
            .onEach { snapshot ->
                Timber.i("ThreatAssessmentEngine: Sampled snapshot - camera=${snapshot.cameraData != null}, audio=${snapshot.audioData != null}")
            }
            .mapNotNull { snapshot ->
                val assessment = processSnapshot(snapshot)
                Timber.d("ThreatAssessmentEngine: processSnapshot returned ${assessment != null}")
                assessment
            }
            .scan(null as Pair<ThreatAssessment, Int>?) { prev, current ->
                // Compteur de stabilité : si le niveau change, reset à 0
                // Si le niveau reste stable, incrémenter
                val sameLevel = prev?.first?.threatLevel == current.threatLevel
                val stableCount = if (sameLevel) (prev?.second ?: 0) + 1 else 1
                Timber.d("ThreatAssessmentEngine processFlow: level=${current.threatLevel}, stableCount=$stableCount")
                Pair(current, stableCount)
            }
            .mapNotNull { pair ->
                val (assessment, stableCount) = pair ?: return@mapNotNull null
                
                // FILTRE DE STABILITÉ ASSOUPLI :
                // - CRITICAL : immédiat (urgence)
                // - MEDIUM/HIGH : nécessite 1 échantillon (réactivité)
                // - NONE/LOW : nécessite 1 échantillon (toujours afficher)
                when {
                    assessment.threatLevel == ThreatLevel.CRITICAL -> {
                        Timber.d("ThreatAssessmentEngine: Emitting CRITICAL immediately")
                        assessment
                    }
                    stableCount >= 1 -> {
                        // Niveau stable depuis 1+ lecture : émettre
                        Timber.d("ThreatAssessmentEngine: Emitting stable assessment (count=$stableCount)")
                        assessment
                    }
                    else -> {
                        // Jamais atteint avec stableCount >= 1
                        Timber.w("ThreatAssessmentEngine: Filtering unstable assessment")
                        null
                    }
                }
            }
            .distinctUntilChangedBy { assessment ->
                // Éviter émissions répétées si pas de changement significatif
                // IMPORTANT: Inclure le threatLevel pour mettre à jour l'indicateur
                Triple(
                    assessment.threatLevel,
                    assessment.shouldTriggerProtection,
                    assessment.threatScore / 15  // Arrondir par 15 pour réduire le bruit
                )
            }
            .onEach { assessment ->
                _lastAssessment.value = assessment
                addToHistory(assessment)
                trackSessionStats(assessment)
                
                Timber.i("✅ ThreatAssessmentEngine: STABLE assessment emitted - Score=${assessment.threatScore}, Level=${assessment.threatLevel}, Trigger=${assessment.shouldTriggerProtection}")
                
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
     * Démarre une nouvelle session et la sauvegarde en DB
     */
    fun startSession() {
        sessionStartTime = System.currentTimeMillis()
        totalThreatsDetected = 0
        totalThreatScore = 0f
        totalAssessments = 0
        maxThreatScore = 0f
        maxThreatLevel = ThreatLevel.NONE
        threatEvents.clear()
        
        // Créer la session en DB si repository disponible
        sessionRepository?.let { repo ->
            scope.launch {
                try {
                    val protectionMode = _context.value.currentMode.name
                    currentSessionId = repo.createSession(protectionMode)
                    Timber.i("ThreatAssessmentEngine: Session started and saved to DB with ID: $currentSessionId")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to create session in DB")
                }
            }
        } ?: run {
            Timber.i("ThreatAssessmentEngine: Session started (no DB persistence)")
        }
    }
    
    /**
     * Termine la session en cours et met à jour les stats finales en DB
     */
    fun stopSession() {
        val sessionId = currentSessionId
        if (sessionId != null && sessionRepository != null) {
            scope.launch {
                try {
                    val avgScore = if (totalAssessments > 0) {
                        totalThreatScore / totalAssessments
                    } else 0f
                    
                    sessionRepository.endSession(
                        sessionId = sessionId,
                        totalThreatsDetected = totalThreatsDetected,
                        totalThreatScore = totalThreatScore,
                        assessmentCount = totalAssessments,
                        avgThreatScore = avgScore,
                        maxThreatScore = maxThreatScore,
                        maxThreatLevel = maxThreatLevel.name
                    )
                    
                    Timber.i("ThreatAssessmentEngine: Session $sessionId ended. " +
                            "Duration: ${System.currentTimeMillis() - sessionStartTime}ms, " +
                            "Threats: $totalThreatsDetected, " +
                            "Avg Score: $avgScore")
                    
                    currentSessionId = null
                } catch (e: Exception) {
                    Timber.e(e, "Failed to end session in DB")
                }
            }
        } else {
            Timber.i("ThreatAssessmentEngine: Session stopped (no DB persistence)")
        }
    }
    
    /**
     * Track les stats de la session et sauvegarde les événements en DB
     */
    private fun trackSessionStats(assessment: ThreatAssessment) {
        if (sessionStartTime == 0L) {
            startSession()
        }
        
        totalAssessments++
        totalThreatScore += assessment.threatScore
        
        // Mettre à jour les max
        if (assessment.threatScore.toFloat() > maxThreatScore) {
            maxThreatScore = assessment.threatScore.toFloat()
        }
        if (assessment.threatLevel.ordinal > maxThreatLevel.ordinal) {
            maxThreatLevel = assessment.threatLevel
        }
        
        if (assessment.shouldTriggerProtection) {
            totalThreatsDetected++
            
            val threatEvent = ThreatEvent(
                timestamp = assessment.timestamp,
                score = assessment.threatScore,
                level = assessment.threatLevel,
                reasons = assessment.triggerReasons
            )
            threatEvents.add(threatEvent)
            
            // Sauvegarder l'événement en DB
            val sessionId = currentSessionId
            if (sessionId != null && sessionRepository != null) {
                scope.launch {
                    try {
                        sessionRepository.addThreatEvent(
                            sessionId = sessionId,
                            threatScore = assessment.threatScore.toFloat() / 100f, // Normaliser [0-100] -> [0-1]
                            threatLevel = assessment.threatLevel.name,
                            actionTriggered = assessment.recommendedAction?.name,
                            cameraContribution = assessment.sensorContributions.cameraScore,
                            audioContribution = assessment.sensorContributions.audioScore,
                            motionContribution = assessment.sensorContributions.motionScore,
                            proximityContribution = assessment.sensorContributions.proximityScore,
                            reasons = assessment.triggerReasons.joinToString(" • ")
                        )
                        Timber.d("ThreatEvent saved to DB for session $sessionId")
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to save threat event to DB")
                    }
                }
            }
            
            // Garder seulement les 50 dernières menaces en mémoire
            if (threatEvents.size > 50) {
                threatEvents.removeAt(0)
            }
        }
    }
    
    /**
     * Retourne les statistiques de la session complète
     */
    fun getSessionStats(): SessionStats {
        val now = System.currentTimeMillis()
        val durationMs = if (sessionStartTime > 0) now - sessionStartTime else 0
        val avgScore = if (totalAssessments > 0) (totalThreatScore / totalAssessments).toInt() else 0
        
        return SessionStats(
            sessionStartTime = sessionStartTime,
            sessionDurationMs = durationMs,
            totalThreatsDetected = totalThreatsDetected,
            averageScore = avgScore,
            currentMode = _context.value.currentMode,
            recentThreats = threatEvents.takeLast(10)
        )
    }
    
    /**
     * Retourne les statistiques récentes (pour compatibilité)
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

/**
 * Statistiques de la session complète
 */
data class SessionStats(
    val sessionStartTime: Long,
    val sessionDurationMs: Long,
    val totalThreatsDetected: Int,
    val averageScore: Int,
    val currentMode: com.privacyguard.assessment.models.ProtectionMode,
    val recentThreats: List<ThreatEvent>
)

/**
 * Événement de menace détectée
 */
data class ThreatEvent(
    val timestamp: Long,
    val score: Int,
    val level: com.privacyguard.sensors.ThreatLevel,
    val reasons: List<String>
)


