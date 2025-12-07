# SPEC - Apprentissage Comportemental

> **Version cible** : 2.0  
> **Priorité** : 🟡 Moyenne  
> **Complexité** : Très élevée  
> **Durée estimée** : 3 semaines

---

## 🎯 Objectif

Permettre à Privacy Guard d'**apprendre les habitudes de l'utilisateur** pour réduire les faux positifs et améliorer la détection des vraies menaces.

---

## 📋 User Stories

### US-1 : Apprentissage automatique
```
En tant qu'utilisateur,
Je veux que l'app apprenne mes habitudes d'utilisation,
Afin de réduire les fausses alertes.
```

**Exemples** :
- Je lis toujours mon téléphone avec quelqu'un à côté dans le métro
- Mon collègue de bureau est souvent visible en arrière-plan
- Je parle souvent au téléphone en public

### US-2 : Adaptation contextuelle
```
En tant qu'utilisateur,
Je veux que l'app adapte sa sensibilité selon le contexte,
Afin d'avoir une protection intelligente.
```

**Exemples** :
- Plus vigilant le soir dans la rue
- Moins vigilant au bureau pendant les heures de travail
- Mode transport en commun automatique

### US-3 : Détection d'anomalies
```
En tant qu'utilisateur,
Je veux être alerté si quelque chose d'inhabituel se passe,
Même si ce n'est pas une "menace" évidente.
```

**Exemples** :
- Téléphone déverrouillé à 3h du matin (inhabituel)
- Plusieurs visages inconnus détectés (rare pour cet utilisateur)
- Mouvement brusque suivi de plusieurs visages

---

## 🏗️ Architecture

### Composants ML

```
┌─────────────────────────────────────────────────────┐
│              BehaviorLearningEngine                  │
│  - collectPattern()                                  │
│  - predictNormalBehavior()                          │
│  - detectAnomaly()                                  │
└─────────────────────────────────────────────────────┘
                          │
┌──────────────┬──────────────┬──────────────────────┐
│ PatternDB    │ ContextModel │ AnomalyDetector      │
│ Room + Stats │ TFLite       │ Isolation Forest     │
└──────────────┴──────────────┴──────────────────────┘
```

### Données Collectées (100% Local)

```kotlin
/**
 * Pattern d'utilisation collecté
 * Données anonymisées et agrégées
 */
data class UsagePattern(
    val id: Long = 0,
    
    // Temporel
    val hourOfDay: Int,           // 0-23
    val dayOfWeek: Int,           // 1-7
    val isWeekend: Boolean,
    
    // Contexte
    val location: LocationCluster?, // Zone générale, pas GPS précis
    val wifiConnected: Boolean,
    val isCharging: Boolean,
    val batteryLevel: Int,
    
    // Capteurs (agrégés)
    val avgFacesDetected: Float,
    val avgNoiseLevel: Float,
    val avgMotionLevel: Float,
    val avgProximityEvents: Float,
    
    // Comportement
    val screenOnDuration: Long,
    val appsUsed: List<String>,   // Catégories, pas noms
    val threatsDetected: Int,
    val falsePositives: Int,      // Marqués par utilisateur
    
    // Timestamp
    val timestamp: Long
)
```

---

## 🔧 Implémentation

### 1. PatternCollector.kt

```kotlin
/**
 * Collecte les patterns d'utilisation
 * Agrège les données toutes les 15 minutes
 */
class PatternCollector(
    private val context: Context,
    private val patternDao: PatternDao
) {
    
    private val aggregationWindow = 15 * 60 * 1000L // 15 min
    private val currentWindow = mutableListOf<SensorSnapshot>()
    
    /**
     * Ajoute un snapshot à la fenêtre courante
     */
    fun addSnapshot(snapshot: SensorDataSnapshot) {
        currentWindow.add(snapshot.toSensorSnapshot())
        
        // Agréger si fenêtre complète
        if (shouldAggregate()) {
            aggregateAndSave()
        }
    }
    
    /**
     * Agrège les données de la fenêtre et sauvegarde
     */
    private fun aggregateAndSave() {
        val pattern = UsagePattern(
            hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK),
            isWeekend = isWeekend(),
            
            avgFacesDetected = currentWindow.map { it.faces }.average().toFloat(),
            avgNoiseLevel = currentWindow.map { it.noise }.average().toFloat(),
            avgMotionLevel = currentWindow.map { it.motion }.average().toFloat(),
            avgProximityEvents = currentWindow.count { it.proximityClose }.toFloat(),
            
            timestamp = System.currentTimeMillis()
        )
        
        patternDao.insert(pattern)
        currentWindow.clear()
        
        Timber.d("BehaviorLearning: Pattern saved for hour ${pattern.hourOfDay}")
    }
    
    /**
     * Retourne les patterns pour une période donnée
     */
    suspend fun getPatternsForContext(
        hourOfDay: Int,
        dayOfWeek: Int
    ): List<UsagePattern> {
        return patternDao.getPatternsByTime(hourOfDay, dayOfWeek)
    }
}
```

### 2. ContextPredictor.kt

```kotlin
/**
 * Prédit le comportement "normal" pour le contexte actuel
 */
class ContextPredictor(
    private val patternCollector: PatternCollector
) {
    
    /**
     * Prédit les valeurs normales pour le contexte actuel
     */
    suspend fun predictNormalBehavior(): NormalBehavior {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val day = now.get(Calendar.DAY_OF_WEEK)
        
        // Récupérer patterns historiques similaires
        val similarPatterns = patternCollector.getPatternsForContext(hour, day)
        
        if (similarPatterns.isEmpty()) {
            return NormalBehavior.DEFAULT
        }
        
        // Calculer les moyennes et écarts-types
        return NormalBehavior(
            expectedFaces = similarPatterns.map { it.avgFacesDetected }.average().toFloat(),
            faceStdDev = calculateStdDev(similarPatterns.map { it.avgFacesDetected }),
            
            expectedNoise = similarPatterns.map { it.avgNoiseLevel }.average().toFloat(),
            noiseStdDev = calculateStdDev(similarPatterns.map { it.avgNoiseLevel }),
            
            expectedMotion = similarPatterns.map { it.avgMotionLevel }.average().toFloat(),
            motionStdDev = calculateStdDev(similarPatterns.map { it.avgMotionLevel }),
            
            confidence = minOf(similarPatterns.size / 10f, 1f) // Plus de data = plus confiant
        )
    }
    
    data class NormalBehavior(
        val expectedFaces: Float,
        val faceStdDev: Float,
        val expectedNoise: Float,
        val noiseStdDev: Float,
        val expectedMotion: Float,
        val motionStdDev: Float,
        val confidence: Float
    ) {
        companion object {
            val DEFAULT = NormalBehavior(0f, 1f, 30f, 10f, 0.5f, 0.3f, 0f)
        }
    }
}
```

### 3. AnomalyDetector.kt

```kotlin
/**
 * Détecte les anomalies par rapport au comportement normal
 * Utilise un modèle simple basé sur les écarts-types
 */
class AnomalyDetector(
    private val contextPredictor: ContextPredictor
) {
    
    /**
     * Évalue si les données actuelles sont anormales
     */
    suspend fun detectAnomaly(snapshot: SensorDataSnapshot): AnomalyResult {
        val normal = contextPredictor.predictNormalBehavior()
        
        // Pas assez de données pour juger
        if (normal.confidence < 0.3f) {
            return AnomalyResult.InsufficientData
        }
        
        val anomalies = mutableListOf<Anomaly>()
        
        // Vérifier chaque métrique
        val facesCount = snapshot.cameraData?.facesDetected ?: 0
        val facesZScore = calculateZScore(facesCount.toFloat(), normal.expectedFaces, normal.faceStdDev)
        if (facesZScore > 2.0f) {
            anomalies.add(Anomaly.UnusualFaces(facesCount, facesZScore))
        }
        
        val noiseLevel = snapshot.audioData?.averageDecibels ?: 0f
        val noiseZScore = calculateZScore(noiseLevel, normal.expectedNoise, normal.noiseStdDev)
        if (noiseZScore > 2.0f) {
            anomalies.add(Anomaly.UnusualNoise(noiseLevel, noiseZScore))
        }
        
        // ... autres métriques
        
        return if (anomalies.isEmpty()) {
            AnomalyResult.Normal
        } else {
            AnomalyResult.Anomalies(anomalies, calculateOverallScore(anomalies))
        }
    }
    
    /**
     * Calcule le Z-score (nombre d'écarts-types par rapport à la moyenne)
     */
    private fun calculateZScore(value: Float, mean: Float, stdDev: Float): Float {
        if (stdDev == 0f) return 0f
        return abs(value - mean) / stdDev
    }
    
    sealed class AnomalyResult {
        object Normal : AnomalyResult()
        object InsufficientData : AnomalyResult()
        data class Anomalies(
            val anomalies: List<Anomaly>,
            val overallScore: Float // 0-1, 1 = très anormal
        ) : AnomalyResult()
    }
    
    sealed class Anomaly {
        data class UnusualFaces(val count: Int, val zScore: Float) : Anomaly()
        data class UnusualNoise(val level: Float, val zScore: Float) : Anomaly()
        data class UnusualMotion(val level: Float, val zScore: Float) : Anomaly()
        data class UnusualTime(val hour: Int) : Anomaly()
        data class UnusualLocation(val cluster: String) : Anomaly()
    }
}
```

### 4. Intégration avec ThreatAssessmentEngine

```kotlin
/**
 * ThreatAssessmentEngine modifié pour utiliser l'apprentissage
 */
class SmartThreatAssessmentEngine(
    private val sensorDataFusion: SensorDataFusion,
    private val config: ThreatAssessmentConfig,
    private val anomalyDetector: AnomalyDetector?,
    private val contextPredictor: ContextPredictor?
) {
    
    fun processSnapshot(snapshot: SensorDataSnapshot): ThreatAssessment {
        // Évaluation standard
        var assessment = sensorDataFusion.evaluate(snapshot, config, context.value)
        
        // Ajuster selon le comportement appris
        if (contextPredictor != null && anomalyDetector != null) {
            assessment = adjustWithLearning(assessment, snapshot)
        }
        
        return assessment
    }
    
    private suspend fun adjustWithLearning(
        assessment: ThreatAssessment,
        snapshot: SensorDataSnapshot
    ): ThreatAssessment {
        val anomalyResult = anomalyDetector.detectAnomaly(snapshot)
        
        return when (anomalyResult) {
            is AnomalyResult.Normal -> {
                // Comportement normal → réduire le score de menace
                assessment.copy(
                    threatScore = (assessment.threatScore * 0.7f).toInt(),
                    triggerReasons = assessment.triggerReasons + "Comportement habituel"
                )
            }
            is AnomalyResult.Anomalies -> {
                // Comportement anormal → augmenter le score
                val boost = (anomalyResult.overallScore * 30).toInt()
                assessment.copy(
                    threatScore = minOf(100, assessment.threatScore + boost),
                    triggerReasons = assessment.triggerReasons + 
                        anomalyResult.anomalies.map { it.toReason() }
                )
            }
            AnomalyResult.InsufficientData -> assessment // Pas de modification
        }
    }
}
```

---

## 📊 Modèle de Données

### Tables Room

```kotlin
@Entity(tableName = "usage_patterns")
data class UsagePatternEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hourOfDay: Int,
    val dayOfWeek: Int,
    val avgFacesDetected: Float,
    val avgNoiseLevel: Float,
    val avgMotionLevel: Float,
    val avgProximityEvents: Float,
    val timestamp: Long
)

@Dao
interface PatternDao {
    @Insert
    suspend fun insert(pattern: UsagePatternEntity)
    
    @Query("""
        SELECT * FROM usage_patterns 
        WHERE hourOfDay = :hour 
        AND dayOfWeek = :day
        ORDER BY timestamp DESC
        LIMIT 100
    """)
    suspend fun getPatternsByTime(hour: Int, day: Int): List<UsagePatternEntity>
    
    @Query("DELETE FROM usage_patterns WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
```

---

## 🎨 UI/UX

### Écran Statistiques d'Apprentissage

```
┌────────────────────────────────────────┐
│ ← Apprentissage                  📊    │
├────────────────────────────────────────┤
│                                        │
│  🧠 État de l'apprentissage            │
│  ─────────────────────────────────     │
│  Données collectées: 1,234 patterns   │
│  Confiance modèle: 78%                │
│  Dernière mise à jour: il y a 15 min  │
│                                        │
│  📈 Votre profil typique              │
│  ─────────────────────────────────     │
│                                        │
│  Matin (6h-12h)                       │
│  • 0.5 visages en moyenne             │
│  • Bruit: 35 dB                       │
│  • Peu de mouvements                  │
│                                        │
│  Après-midi (12h-18h)                 │
│  • 1.2 visages en moyenne             │
│  • Bruit: 55 dB (bureau)              │
│  • Mouvements modérés                 │
│                                        │
│  Soir (18h-24h)                       │
│  • 0.3 visages en moyenne             │
│  • Bruit: 40 dB                       │
│  • Peu de mouvements                  │
│                                        │
│  🎯 Anomalies détectées (7 jours)     │
│  ─────────────────────────────────     │
│  • 2 utilisations inhabituelles       │
│  • 1 pic de bruit anormal             │
│  • 0 détection nocturne               │
│                                        │
│  ┌─────────────────────────────────┐  │
│  │     🗑️ Réinitialiser            │  │
│  └─────────────────────────────────┘  │
│                                        │
└────────────────────────────────────────┘
```

---

## 🔒 Confidentialité

### Données Collectées

| Donnée | Stockée | Durée | Usage |
|--------|---------|-------|-------|
| Heure d'utilisation | ✅ | 30 jours | Patterns temporels |
| Nb visages (agrégé) | ✅ | 30 jours | Baseline faces |
| Niveau bruit (agrégé) | ✅ | 30 jours | Baseline audio |
| GPS précis | ❌ | - | Non collecté |
| Photos | ❌ | - | Non collecté |
| Noms apps | ❌ | - | Non collecté |

### Règles Strictes

- ❌ **JAMAIS** stocker de données personnelles identifiables
- ❌ **JAMAIS** envoyer de données vers serveur
- ✅ **TOUJOURS** agréger les données (pas de snapshots individuels)
- ✅ **TOUJOURS** permettre la suppression complète
- ✅ Rétention max 30 jours

---

## 📦 Dépendances

```kotlin
// TensorFlow Lite (optionnel, pour modèle plus avancé)
implementation("org.tensorflow:tensorflow-lite:2.13.0")

// Room (déjà présent)
implementation("androidx.room:room-runtime:2.6.0")

// Statistics
implementation("org.apache.commons:commons-math3:3.6.1")
```

---

## 🚀 Plan d'Implémentation

### Phase 1 : Collection (1 semaine)
- [ ] PatternCollector
- [ ] Room database pour patterns
- [ ] Agrégation toutes les 15 min

### Phase 2 : Prédiction (1 semaine)
- [ ] ContextPredictor
- [ ] Calcul moyennes et écarts-types
- [ ] Tests de précision

### Phase 3 : Détection anomalies (1 semaine)
- [ ] AnomalyDetector
- [ ] Intégration ThreatAssessmentEngine
- [ ] UI statistiques

---

## 📊 Métriques de Succès

| Métrique | Objectif | Mesure |
|----------|----------|--------|
| Réduction faux positifs | -50% | Après 2 semaines d'apprentissage |
| Précision anomalies | >80% | Validation utilisateur |
| Impact batterie | <2%/h | Profiling |
| Taille données | <10 MB | Après 30 jours |

---

**Dernière mise à jour** : 7 décembre 2025

