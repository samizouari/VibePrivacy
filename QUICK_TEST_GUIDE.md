# 🧪 Guide de Test Rapide - Corrections Scoring

## ✅ Corrections Appliquées

### Problème Résolu
**Avant** : Indicateur rouge sans raison (1 visage + peu de bruit)  
**Après** : Indicateur vert en usage normal, rouge uniquement sur menace réelle

---

## 🎯 Tests à Faire Maintenant

### 1️⃣ Test Normal (Attendu : 🟢 VERT)

**Actions** :
1. Lance l'app
2. Active le service Privacy Guard
3. Active l'indicateur overlay
4. **Utilise l'app normalement** (regarde l'écran seul)

**Résultat Attendu** :
- ✅ Indicateur **VERT**
- ✅ Score ~2-5

**Logs à vérifier** :
```bash
adb logcat | grep -E "Indicator|Score calculated"
```

**Attendu** :
```
ThreatScorer: Score calculated - Camera: 2%, Audio: 0%, Motion: 0%, Proximity: 15%, Total: 2
Indicator: SAFE | Score=2
```

---

### 2️⃣ Test 1 Visage Seul (Attendu : 🟢 VERT)

**Actions** :
1. Reste seul
2. Regarde l'écran
3. Pas de bruit/musique

**Résultat Attendu** :
- ✅ Indicateur **VERT**
- ✅ Score ~2-10

**Avant/Après** :
- ❌ **Avant** : Score 40-60 → Rouge
- ✅ **Après** : Score 2-10 → Vert

---

### 3️⃣ Test 2 Visages (Attendu : 🔴 ROUGE)

**Actions** :
1. Demande à quelqu'un de se mettre à côté de toi
2. Vous regardez tous les deux l'écran
3. Éventuellement parlez un peu

**Résultat Attendu** :
- ✅ Indicateur **ROUGE**
- ✅ Score ~70-80
- ✅ **Soft Blur activé** automatiquement

**Logs Attendus** :
```
CameraScore: faces=2, looking=1-2, final=0.70
ThreatScorer: Total: 70+
SensorDataFusion: Level=HIGH, Trigger=true, Action=SOFT_BLUR
```

---

### 4️⃣ Test Mouvement Normal (Attendu : 🟢 VERT)

**Actions** :
1. Marche avec le téléphone
2. Bouge le téléphone normalement
3. Pas de geste brusque

**Résultat Attendu** :
- ✅ Indicateur reste **VERT**
- ✅ Motion score ~0-10%

**Avant/Après** :
- ❌ **Avant** : Motion 30-50% → Jaune/Rouge
- ✅ **Après** : Motion 0-10% → Vert

---

### 5️⃣ Test Proximité Normale (Attendu : 🟢 VERT)

**Actions** :
1. Regarde l'écran normalement
2. Téléphone à ~10-20cm du visage

**Résultat Attendu** :
- ✅ Indicateur **VERT**
- ✅ Proximity score ~15%

**Avant/Après** :
- ❌ **Avant** : Proximity 70% → Rouge
- ✅ **Après** : Proximity 15% → Vert

---

### 6️⃣ Test Musique Forte (Attendu : 🟡 JAUNE)

**Actions** :
1. 1 seul visage (toi)
2. Lance musique forte (~80dB) sur enceinte

**Résultat Attendu** :
- ✅ Indicateur **JAUNE** (Monitoring)
- ✅ Score ~20-30

**Logs** :
```
AudioScore: dB=80, speech=false, final=0.60
ThreatScorer: Total: 20-30
Indicator: MONITORING
```

---

### 7️⃣ Test Menace Réelle (Attendu : 🔴 ROUGE + BLUR)

**Actions** :
1. 2+ personnes
2. Parle fort / musique
3. Bouge le téléphone

**Résultat Attendu** :
- ✅ Indicateur **ROUGE**
- ✅ Score ~70-85+
- ✅ **Soft Blur OU Decoy Screen** activé

---

## 📊 Vérification Rapide (PowerShell)

```powershell
# Lance logcat filtré en continu
adb logcat -c  # Vider logs
adb logcat | Select-String "ThreatScorer: Score calculated|Indicator:"
```

**Interprétation** :
```
# Bon signe (VERT) :
ThreatScorer: ... Total: 2-10
Indicator: SAFE | Score=5

# Surveillance (JAUNE) :
ThreatScorer: ... Total: 30-50
Indicator: MONITORING | Score=45

# Menace (ROUGE) :
ThreatScorer: ... Total: 70+
Indicator: THREAT | Score=75
```

---

## 🐛 Si Problème Persiste

### Symptôme : Toujours Rouge
```bash
# Vérifie les scores détaillés
adb logcat | grep -E "CameraScore|AudioScore|MotionScore|ProximityScore"
```

**Cherche** :
- CameraScore > 20% avec 1 seul visage → ❌ Problème
- MotionScore > 10% sans mouvement brusque → ❌ Problème
- ProximityScore > 20% en usage normal → ❌ Problème

### Symptôme : Jamais Rouge
```bash
# Test avec 2 visages
# Vérifie si score camera monte
adb logcat | grep CameraScore
```

**Attendu** :
```
CameraScore: faces=2, looking=1+, final=0.70+
```

Si `final < 0.50` avec 2 visages → Copie les logs et envoie-moi

---

## ✅ Checklist Rapide

| Test | Situation | Couleur Attendue | Testé ? |
|------|-----------|------------------|---------|
| 1 | Seul, normal | 🟢 Vert | ⬜ |
| 2 | 1 visage seul | 🟢 Vert | ⬜ |
| 3 | 2 visages | 🔴 Rouge + Blur | ⬜ |
| 4 | Mouvement normal | 🟢 Vert | ⬜ |
| 5 | Proximité normale | 🟢 Vert | ⬜ |
| 6 | Musique forte seul | 🟡 Jaune | ⬜ |
| 7 | Menace réelle | 🔴 Rouge + Action | ⬜ |

---

## 📝 Rapport de Test

**Pour chaque test qui ne fonctionne pas comme attendu, note :**

1. **Test #** : (numéro du test)
2. **Couleur obtenue** : Vert/Jaune/Rouge
3. **Comportement** : (ex: "Rouge alors que seul")
4. **Logs** : (copie les 5 lignes de logs pertinentes)

**Exemple** :
```
Test #2 : 1 visage seul
Couleur : 🔴 Rouge (attendu : Vert)
Logs :
CameraScore: faces=1, looking=1, final=0.45
ThreatScorer: Total: 50
Indicator: MONITORING
```

---

**Teste maintenant et dis-moi ce qui fonctionne ou pas !** 🚀

