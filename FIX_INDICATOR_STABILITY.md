# 🔧 Correction de la Stabilité de l'Indicateur

## 🐛 Problèmes Identifiés

### Problème 1 : Scoring Caméra Incohérent
**Symptôme :** L'indicateur reste vert malgré 3 visages, ou devient rouge avec 1 seul visage.

**Cause :**
```kotlin
// ❌ AVANT (incohérent)
data.facesDetected == 1 -> 0.1f   // 10% seulement
data.facesDetected == 2 -> 0.35f  // 35%
else -> 0.4f                       // 40% max pour 3+ visages
```

Le problème : 
- 3+ visages ne dépassaient que 40-50% du score caméra
- Avec le poids caméra de 40%, cela donnait : `0.5 × 0.4 = 0.2` → 20% du score total
- Insuffisant pour déclencher le jaune (seuil 40)

**Solution :**
```kotlin
// ✅ APRÈS (cohérent)
data.facesDetected == 1 -> 0.05f   // 5% - probablement utilisateur
data.facesDetected == 2 -> 0.30f   // 30% - menace réelle
data.facesDetected == 3 -> 0.45f   // 45% - menace élevée
else -> 0.50f                       // 50% - menace max (4+ personnes)
```

Score final avec 3 visages qui regardent :
- `faceCountScore = 0.45`
- `lookingScore = 0.40` (plusieurs regardent)
- `Total = 0.85 × 100 × 0.4 (poids caméra) = 34%`

Avec audio (musique) :
- `Camera: 34% + Audio: 28% = 62%` → **MEDIUM** → **Indicateur JAUNE** ✅

---

### Problème 2 : Logique d'Indicateur Hybride Complexe

**Symptôme :** L'indicateur passe de vert à rouge brutalement, sans jaune intermédiaire.

**Cause :**
```kotlin
// ❌ AVANT (logique hybride incohérente)
val hasHighThreat = contrib.cameraScore > 0.6f || contrib.audioScore > 0.6f

val state = when {
    assessment.shouldTriggerProtection -> IndicatorState.THREAT
    hasHighThreat -> IndicatorState.THREAT  // ← Force le rouge si score > 60%
    assessment.threatScore >= 40 -> IndicatorState.MONITORING
    else -> IndicatorState.SAFE
}
```

Problème :
- `hasHighThreat` forçait le rouge si un capteur dépasse 60%
- Mais le score global pouvait être faible (exemple : camera=70%, audio=10%, motion=20% → total=35%)
- Incohérence entre couleur indicateur et score global

**Solution :**
```kotlin
// ✅ APRÈS (logique simple et cohérente)
val state = when (assessment.threatLevel) {
    ThreatLevel.CRITICAL, ThreatLevel.HIGH -> IndicatorState.THREAT       // Rouge
    ThreatLevel.MEDIUM -> IndicatorState.MONITORING                        // Jaune
    ThreatLevel.LOW, ThreatLevel.NONE -> IndicatorState.SAFE              // Vert
}
```

Avantages :
- ✅ Cohérence totale : couleur = ThreatLevel calculé par l'engine
- ✅ Pas de logique hybride complexe
- ✅ Facile à débugger : 1 seule source de vérité

---

### Problème 3 : Seuils ThreatLevel Mal Calibrés

**Symptôme :** Score de 42 reste en LOW, devrait être MEDIUM.

**Cause :**
```kotlin
// ❌ AVANT
score < 20 -> ThreatLevel.NONE
score < 40 -> ThreatLevel.LOW    // ← 39% = LOW
score < 60 -> ThreatLevel.MEDIUM // ← 41% = MEDIUM
```

Problème : Le seuil était trop bas, créant des sauts brusques.

**Solution :**
```kotlin
// ✅ APRÈS (seuils élargis et cohérents)
score < 25 -> ThreatLevel.NONE       // 0-24%   → Vert
score < 45 -> ThreatLevel.LOW        // 25-44%  → Vert
score < 65 -> ThreatLevel.MEDIUM     // 45-64%  → Jaune
score < 85 -> ThreatLevel.HIGH       // 65-84%  → Rouge
else       -> ThreatLevel.CRITICAL   // 85-100% → Rouge
```

Avantages :
- ✅ Zones plus larges → moins d'oscillations
- ✅ Cohérence avec les modes de protection (DISCRETE=75, BALANCED=50, PARANOIA=20)

---

### Problème 4 : Oscillations Rapides (Bruit)

**Symptôme :** L'indicateur clignote vert→jaune→vert en 1 seconde.

**Cause :**
- Données capteurs fluctuent naturellement (±5% chaque frame)
- `distinctUntilChangedBy` avec `score / 10` créait des émissions pour 38→42 (3.8→4.2)
- Pas de filtre de stabilité temporelle

**Solution : Filtre de Stabilité avec `.scan()`**
```kotlin
.scan(null as Pair<ThreatAssessment, Int>?) { prev, current ->
    // Compteur de stabilité
    val sameLevel = prev?.first?.threatLevel == current.threatLevel
    val stableCount = if (sameLevel) (prev?.second ?: 0) + 1 else 1
    Pair(current, stableCount)
}
.mapNotNull { (assessment, stableCount) ->
    when {
        assessment.threatLevel == ThreatLevel.CRITICAL -> assessment  // Immédiat
        stableCount >= 2 -> assessment                                // Stable depuis 200ms
        else -> null                                                  // Attendre
    }
}
```

Logique :
1. Si le niveau reste identique pendant 2 lectures consécutives (200ms) → émettre
2. Si CRITICAL → émettre immédiatement (urgence)
3. Sinon → ne pas émettre (attendre stabilisation)

Avantages :
- ✅ Filtre les variations temporaires (bruit)
- ✅ Garde la réactivité pour les menaces critiques
- ✅ Indicateur stable et prévisible

---

## 📊 Scénarios de Test

### Scénario 1 : Utilisateur Seul
**Capteurs :**
- Camera: 1 visage, 1 looking
- Audio: 45 dB (silence)
- Motion: 8 m/s² (immobile)

**Score Attendu :**
- Camera: `(0.05 + 0.02) × 40% = 2.8%`
- Audio: `0.05 × 30% = 1.5%`
- Motion: `0.3 × 20% = 6%`
- **Total : ~10%** → **ThreatLevel.NONE** → **🟢 Vert** ✅

---

### Scénario 2 : 2 Personnes, 1 Regarde l'Écran
**Capteurs :**
- Camera: 2 visages, 1 looking
- Audio: 62 dB (conversation)
- Motion: 10 m/s²

**Score Attendu :**
- Camera: `(0.30 + 0.35) × 40% = 26%`
- Audio: `(0.15 + 0.30) × 30% = 13.5%`
- Motion: `0.3 × 20% = 6%`
- **Total : ~45%** → **ThreatLevel.MEDIUM** → **🟡 Jaune** ✅

---

### Scénario 3 : 3 Visages + Musique Forte
**Capteurs :**
- Camera: 3 visages, 2 looking
- Audio: 88 dB (musique très forte)
- Motion: 12 m/s²

**Score Attendu :**
- Camera: `(0.45 + 0.40) × 40% = 34%`
- Audio: `(0.50 + 0.30) × 30% = 24%`
- Motion: `0.4 × 20% = 8%`
- **Total : ~66%** → **ThreatLevel.HIGH** → **🔴 Rouge** ✅

---

### Scénario 4 : 1 Visage mais Aucun Son (Utilisateur)
**Capteurs :**
- Camera: 1 visage, 0 looking (regarde ailleurs)
- Audio: 38 dB (silence)
- Motion: 9 m/s²

**Score Attendu :**
- Camera: `(0.05 + 0) × 40% = 2%`
- Audio: `0 × 30% = 0%`
- Motion: `0.3 × 20% = 6%`
- **Total : ~8%** → **ThreatLevel.NONE** → **🟢 Vert** ✅

---

## 🎯 Résumé des Corrections

| Correction | Avant | Après | Impact |
|------------|-------|-------|--------|
| **Scoring Caméra (3+ visages)** | 40% max | 45-50% | Score plus élevé pour menaces réelles |
| **Logique Indicateur** | Hybride (incohérent) | Simple (ThreatLevel) | Cohérence totale |
| **Seuils ThreatLevel** | 20/40/60/80 | 25/45/65/85 | Zones plus larges, moins d'oscillations |
| **Filtre Stabilité** | Aucun | 2 échantillons stables | Anti-clignotement |
| **Debounce** | 50ms | 100ms | Moins de bruit |
| **distinctUntilChanged** | score/10 | score/15 | Moins d'émissions inutiles |

---

## 🧪 Comment Tester

### Test 1 : Utilisateur Seul (Vert)
1. Lance l'app, active le service
2. Assieds-toi seul devant le téléphone
3. **Attendu :** 🟢 Indicateur VERT stable

### Test 2 : 2 Personnes (Jaune)
1. Demande à quelqu'un de s'approcher
2. Mets une conversation ou de la musique
3. **Attendu :** 🟡 Indicateur JAUNE après 200ms

### Test 3 : 3+ Personnes + Bruit (Rouge)
1. Réunis 2-3 personnes autour du téléphone
2. Mets de la musique forte (80+ dB)
3. **Attendu :** 🔴 Indicateur ROUGE après 200ms

### Test 4 : Retour au Calme (Vert)
1. Éloigne les personnes
2. Coupe le son
3. **Attendu :** 🟢 Indicateur VERT après 200-400ms (filtre de stabilité)

---

## 📝 Logs de Debug

**Logs améliorés pour débugger :**

```
CameraScore DETAIL: faces=3, looking=2, unknown=0 
  → faceScore=0.45, lookingScore=0.40, unknownScore=0.00 
  → FINAL=0.85 (85%)

AudioScore DETAIL: dB=88, speech=true 
  → dbScore=0.50, speechBonus=0.30 
  → FINAL=0.80 (80%)

ThreatScorer: Score calculated - 
  Camera: 85% (w=40%), Audio: 80% (w=30%), Motion: 40% (w=20%), Proximity: 0% (w=10%)
  Total: 66

SensorDataFusion: Assessment complete - 
  Score=66, Level=HIGH, Trigger=false, Action=SOFT_BLUR

✅ ThreatAssessmentEngine: STABLE assessment emitted - 
  Score=66, Level=HIGH, Trigger=false

🚦 Indicator: THREAT | ThreatLevel=HIGH | Score=66/100 | 
  📹 Camera=85% | 🔊 Audio=80% | 📱 Motion=40% | 👋 Proximity=0%
```

---

## ✅ Résultat Final

**Avant :**
- ❌ Indicateur vert malgré 3 visages
- ❌ Indicateur rouge avec 1 visage seul
- ❌ Oscillations vert↔jaune↔rouge
- ❌ Incohérence entre score et couleur

**Après :**
- ✅ Indicateur cohérent avec la menace réelle
- ✅ Transitions douces (filtre de stabilité)
- ✅ Seuils clairs et prévisibles
- ✅ Une seule source de vérité (ThreatLevel)
- ✅ Logs détaillés pour debugging

🎉 **L'indicateur est maintenant stable, cohérent et fiable !**





