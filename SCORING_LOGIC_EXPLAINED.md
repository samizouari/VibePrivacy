# 📊 Logique de Scoring des Menaces - Privacy Guard

## 🎯 Objectif

Détecter les **vraies menaces** tout en évitant les **fausses alertes** lors de l'usage normal.

---

## 🔢 Système de Scoring (0-100)

### Pondérations des Capteurs

| Capteur    | Poids | Rôle                                  |
|------------|-------|---------------------------------------|
| **Caméra** | 40%   | Détection visages (capteur principal) |
| **Audio**  | 30%   | Sons suspects (parole, bruit élevé)   |
| **Motion** | 20%   | Mouvements brusques (arrachage)       |
| **Proximité** | 10% | Objet TRÈS proche (main cache écran)  |

---

## 📷 Caméra (40% du score)

### Logique Intelligente

```
SI 1 seul visage détecté :
    SI regarde l'écran → 2% (usage normal ✅)
    SINON → 15% (personne à côté ?)
    
SI 2 visages → 40-70% (menace significative ⚠️)
SI 3+ visages → 50-90% (menace élevée 🚨)
```

### Pourquoi ?

- **1 visage qui regarde = TOI** → Score minimal (2%)
- **1 visage qui ne regarde pas** = Quelqu'un à côté → Score modéré (15%)
- **Plusieurs visages** = Situation anormale → Score élevé (40-90%)

### Exemples

| Scénario | Faces | Looking | Score | État |
|----------|-------|---------|-------|------|
| Tu utilises l'app | 1 | 1 | **2%** | ✅ Normal |
| Quelqu'un à côté | 1 | 0 | **15%** | ⚠️ Surveillance |
| 2 personnes | 2 | 1 | **70%** | 🚨 Menace |
| 3+ personnes | 3+ | 2+ | **90%** | 🔴 Critique |

---

## 🔊 Audio (30% du score)

### Logique

```
< 35 dB  → 0% (silence)
35-50 dB → 15% (calme)
50-60 dB → 30% (normal)
60-70 dB → 45% (conversation)
> 70 dB  → 60% (bruyant)

+ Parole détectée → +40%
```

### Exemples

| Scénario | dB | Parole | Score | Total |
|----------|----|----|-------|-------|
| Silence | 30 | Non | **0%** | 0% × 30% = **0** |
| Musique faible | 55 | Non | **30%** | 30% × 30% = **9** |
| Conversation | 65 | Oui | **45% + 40% = 85%** | 85% × 30% = **26** |
| Musique forte | 80 | Non | **60%** | 60% × 30% = **18** |

---

## 📱 Mouvement (20% du score)

### Logique Ajustée (Anti Faux Positifs)

**Avant (Problématique)** :
- Mouvement = 30%
- Intensité × 50%
- → **Usage normal = 30-50%** ❌

**Après (Corrigé)** :
```
< 12 m/s²  → 0% (normal, gravité ~9.8)
12-15 m/s² → 10% (léger)
15-20 m/s² → 30% (brusque ⚠️)
20-25 m/s² → 50% (très brusque 🚨)
> 25 m/s²  → 70% (arrachage 🔴)

+ Intensité > 70% → +30% (mouvement continu intense)
```

### Exemples

| Scénario | Magnitude | Intensité | Score | Total |
|----------|-----------|-----------|-------|-------|
| Téléphone posé | 9.8 m/s² | 0% | **0%** | 0% × 20% = **0** |
| Marcher avec | 11 m/s² | 30% | **0%** | 0% × 20% = **0** |
| Geste brusque | 18 m/s² | 50% | **30%** | 30% × 20% = **6** |
| Quelqu'un attrape | 26 m/s² | 90% | **70% + 30% = 100%** | 100% × 20% = **20** |

---

## 👋 Proximité (10% du score)

### Logique Ajustée (Anti Faux Positifs)

**Avant (Problématique)** :
- `isNear=true` → **70%** ❌
- → **Usage normal = 70%** (tu regardes l'écran !)

**Après (Corrigé)** :
```
isNear=false → 0%
isNear=true :
    distance > 2cm → 15% (usage normal ✅)
    distance 1-2cm → 40% (proche)
    distance < 1cm → 70% (TRÈS proche 🚨)
```

### Exemples

| Scénario | isNear | Distance | Score | Total |
|----------|--------|----------|-------|-------|
| Téléphone loin | false | 10cm | **0%** | 0% × 10% = **0** |
| Tu regardes l'écran | true | 5cm | **15%** | 15% × 10% = **1.5** |
| Main très proche | true | 0.5cm | **70%** | 70% × 10% = **7** |

---

## 🎚️ Niveaux de Menace (Score Final)

| Score | Niveau | Couleur | Indicateur | Action |
|-------|--------|---------|------------|--------|
| **0-30** | NONE | 🟢 Vert | Safe | Aucune |
| **30-50** | LOW | 🟡 Jaune | Surveillance | Aucune |
| **50-70** | MEDIUM | 🟡 Jaune | Alerte modérée | Aucune |
| **70-85** | HIGH | 🔴 Rouge | Menace sérieuse | **Soft Blur** |
| **85+** | CRITICAL | 🔴 Rouge | Menace confirmée | **Decoy Screen** |

---

## ✅ Scénarios Corrigés

### 1️⃣ Usage Normal (Avant = Rouge ❌, Après = Vert ✅)

**Situation** : Tu utilises l'app normalement
- **Caméra** : 1 visage regardant = 2% × 40% = **0.8**
- **Audio** : Silence (30dB) = 0% × 30% = **0**
- **Motion** : Marche (11 m/s²) = 0% × 20% = **0**
- **Proximité** : isNear=true, 5cm = 15% × 10% = **1.5**

**Score Total : 2.3** → 🟢 **VERT** ✅

---

### 2️⃣ Quelqu'un Regarde (Rouge 🔴)

**Situation** : 2 personnes, 1 parle
- **Caméra** : 2 visages, 1 regarde = 70% × 40% = **28**
- **Audio** : Parole (65dB) = 85% × 30% = **25.5**
- **Motion** : Léger (13 m/s²) = 10% × 20% = **2**
- **Proximité** : Proche (1.5cm) = 40% × 10% = **4**

**Score Total : 59.5** → 🟡 **JAUNE** (Alerte modérée)

---

### 3️⃣ Menace Réelle (Rouge 🔴)

**Situation** : Quelqu'un attrape le téléphone + cache l'écran
- **Caméra** : 2 visages = 70% × 40% = **28**
- **Audio** : Bruit (70dB) = 60% × 30% = **18**
- **Motion** : Arrachage (28 m/s²) = 100% × 20% = **20**
- **Proximité** : Main cache (0.3cm) = 70% × 10% = **7**

**Score Total : 73** → 🔴 **ROUGE** - **Soft Blur activé** 🚨

---

## 🔧 Modes de Protection (Seuils)

| Mode | Seuil | Description |
|------|-------|-------------|
| **Discret** | 70 | Par défaut - Déclenche uniquement sur menace confirmée |
| **Équilibré** | 50 | Balance protection/convivialité |
| **Paranoïa** | 20 | Très agressif - Toute anomalie déclenche |

---

## 📈 Logs de Debug

Pour comprendre les scores en temps réel :

```bash
# Filtre pour voir les calculs
adb logcat | grep -E "ThreatScorer|CameraScore|AudioScore|MotionScore|ProximityScore"
```

**Exemple de logs** :
```
CameraScore: faces=1, looking=1, final=0.02
AudioScore: dB=32, speech=false, final=0.0
MotionScore: magnitude=10.2, intensity=0.15, final=0.0
ProximityScore: distance=4.5cm, isNear=true, final=0.15
ThreatScorer: Total: 2
SensorDataFusion: Score=2, Level=NONE, Trigger=false
```

---

## 🎯 Résumé des Corrections

| Problème | Avant | Après | Impact |
|----------|-------|-------|--------|
| **1 visage normal** | 15% | **2%** | Faux positifs éliminés ✅ |
| **Mouvement normal** | 30-50% | **0%** | Usage normal ignoré ✅ |
| **Proximité normale** | 70% | **15%** | Regarder l'écran OK ✅ |
| **Seuil Rouge** | 40 | **70** | Moins de fausses alertes ✅ |

---

**L'indicateur devrait maintenant être vert en usage normal et rouge uniquement pour les vraies menaces !** 🎉

