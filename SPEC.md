# SPEC - Guide de Développement pour l'IA

> **Ce fichier sert de référence centrale pour l'IA pendant tout le développement du projet Privacy Guard.**
> Il contient les règles, processus, et rappels automatiques à suivre à chaque étape.

---

## 🎯 Mission Principale

Développer **Privacy Guard MVP** en **7 jours** pour le **TP N7** avec :
- **Fonctionnalité critique** : Mode Discret
- **4 capteurs minimum** : Caméra (ML Kit), Audio, Mouvement, Proximité
- **Protection** : Flou progressif + Écran leurre + Capture intrus
- **Contraintes strictes** : Latence < 200ms, Batterie < 10%/h, 0% télémétrie

---

## 📋 Processus Automatique à Chaque Action

### ✅ Avant CHAQUE génération de code

1. **Vérifier le contexte**
   - [ ] Consulter PROJECT_CONTEXT.md pour les décisions
   - [ ] Consulter MVP_ROADMAP.md pour l'étape actuelle
   - [ ] Vérifier dans quel JOUR on est (1-7)

2. **Confirmer les contraintes**
   - [ ] MVP uniquement (pas de features hors scope)
   - [ ] Compatibilité Android 8.0+ (API 26+)
   - [ ] Performance : latence < 200ms
   - [ ] 0% télémétrie, 100% local
   - [ ] Design sobre et moderne
   - [ ] Langue : français

3. **Planifier**
   - [ ] Identifier les dépendances nécessaires
   - [ ] Lister les fichiers à créer/modifier
   - [ ] Prévoir les tests associés

### ✅ Pendant la génération de code

1. **Standards de code**
   - [ ] Kotlin idiomatique
   - [ ] Coroutines + Flow pour async
   - [ ] Hilt pour DI
   - [ ] MVVM + Clean Architecture
   - [ ] Comments en français
   - [ ] KDoc pour fonctions publiques

2. **Conventions de nommage**
   - [ ] Classes : `PascalCase`
   - [ ] Fonctions : `camelCase`
   - [ ] Constants : `UPPER_SNAKE_CASE`
   - [ ] Packages : `lowercase`

3. **Structure fichiers**
   - [ ] Suivre PROJECT_STRUCTURE.md
   - [ ] Respecter la séparation des couches

### ✅ Après CHAQUE génération de code

1. **Documentation du workflow** ⚠️ IMPORTANT
   - [ ] **METTRE À JOUR WORKFLOW_VIBE_CODING_TEMPLATE.md**
   - [ ] Ajouter le prompt utilisé dans la section appropriée
   - [ ] Documenter le code généré (extraits clés)
   - [ ] Noter les modifications manuelles nécessaires
   - [ ] Documenter les problèmes rencontrés

2. **Tests**
   - [ ] Générer tests unitaires si applicable
   - [ ] Rappeler de tester sur device physique
   - [ ] Vérifier les imports et dépendances

3. **Git**
   - [ ] Suggérer un commit avec message conventionnel
   - [ ] Format : `<type>(<scope>): <description>`
   - [ ] Types : feat, fix, docs, style, refactor, perf, test, chore

4. **Checklist**
   - [ ] Mettre à jour les checkboxes dans MVP_ROADMAP.md
   - [ ] Cocher les tâches complétées
   - [ ] Identifier les tâches suivantes

---

## 📁 Fichiers de Référence (À TOUJOURS Consulter)

### Priorité 1 (Consulter SYSTÉMATIQUEMENT)
1. **SPEC.md** (ce fichier) - Processus et règles
2. **PROJECT_CONTEXT.md** - Toutes les décisions et contraintes
3. **MVP_ROADMAP.md** - Planning et tâches jour par jour

### Priorité 2 (Selon besoin)
4. **ARCHITECTURE.md** - Structure technique
5. **SENSORS.md** - Implémentation capteurs
6. **FEATURES.md** - Détails fonctionnalités
7. **UI_UX.md** - Design et interface
8. **SECURITY_PRIVACY.md** - Règles de sécurité
9. **TECHNICAL_CHALLENGES.md** - Solutions aux problèmes
10. **PROJECT_STRUCTURE.md** - Arborescence fichiers

### Priorité 3 (Documentation)
11. **WORKFLOW_VIBE_CODING_TEMPLATE.md** - À REMPLIR au fur et à mesure
12. **README.md** - Vue d'ensemble
13. **ROADMAP.md** - Vision long terme (hors MVP)
14. **CONTRIBUTING.md** - Standards (référence)

---

## 🚨 Règles Strictes (NE JAMAIS Violer)

### Sécurité et Confidentialité
```kotlin
// ❌ INTERDIT - JAMAIS stocker images/audio brutes
fun saveImage(image: Bitmap) {
    image.compress(...) // NON !
}

// ✅ OK - Traitement immédiat puis destruction
fun processImage(image: Bitmap): FaceDetectionResult {
    val result = detector.detect(image)
    image.recycle() // Libérer immédiatement
    return result
}
```

- ❌ **Jamais** stocker images de caméra
- ❌ **Jamais** stocker enregistrements audio
- ❌ **Jamais** envoyer de données vers serveur
- ❌ **Jamais** de télémétrie
- ✅ **Toujours** chiffrer données sensibles (encodages faciaux, logs)
- ✅ **Toujours** traitement local uniquement

### Performance
- ⚠️ **Latence totale < 200ms** (contrainte stricte)
- ⚠️ **Battery drain < 10%/h** (contrainte stricte)
- ⚠️ **Tester sur device physique** régulièrement

### Scope MVP
- ✅ Mode Discret uniquement
- ✅ 4 capteurs : caméra, audio, mouvement, proximité
- ❌ Pas de Mode Paranoïa/Équilibré (hors MVP)
- ❌ Pas de GPS/luminosité (nice-to-have)
- ❌ Pas de face recognition custom (ML Kit suffit)
- ❌ Pas de keyword spotting avancé (simple amplitude audio)

---

## 📝 Template de Mise à Jour du Workflow

### Après CHAQUE génération de code, ajouter dans WORKFLOW_VIBE_CODING_TEMPLATE.md :

```markdown
### [Phase X] : [Nom de la fonctionnalité]

#### Prompt Utilisé
```
[Copier le prompt exact]
```

#### Code Généré
[Nom du fichier] :
```kotlin
[Extraits clés du code - pas tout, juste l'important]
```

#### Résultat
- ✅ Ce qui fonctionne
- ⚠️ Ce qui nécessite ajustement
- 📝 Notes importantes

#### Tests
- [ ] Compilé avec succès
- [ ] Tests unitaires passent
- [ ] Testé sur device physique
- [ ] Performance acceptable

#### Problèmes Rencontrés
[Décrire bugs/difficultés et comment résolu]

#### Apprentissage
[Ce qu'on a appris de cette implémentation]
```

---

## 📅 État Actuel du Projet

### Jour en Cours
**JOUR 7 - MVP COMPLET** ✅🎉

**🎉 MILESTONES COMPLETS :**

#### Jour 1-2 : Setup et Capteurs
- ✅ Projet Android configuré
- ✅ 4 capteurs (Camera, Audio, Motion, Proximity)
- ✅ ML Kit Face Detection
- ✅ Tests unitaires

#### Jour 3 : Fusion et Évaluation
- ✅ ThreatAssessmentEngine
- ✅ SensorDataFusion  
- ✅ ThreatScorer (scoring pondéré)
- ✅ Pipeline temps réel avec Flow

#### Jour 4 : Protection et Overlays
- ✅ ProtectionExecutor
- ✅ OverlayManager
- ✅ PrivacyIndicatorView (vert/jaune/rouge)
- ✅ SoftBlurOverlayView
- ✅ DecoyScreenOverlayView
- ✅ LockScreenOverlayView

#### Jour 5 : Dashboard et Config
- ✅ DashboardScreen (stats temps réel)
- ✅ SettingsScreen (4 modes protection)
- ✅ Navigation entre écrans
- ✅ Persistance SharedPreferences

#### Jour 6 : Capture Intrus
- ✅ IntruderCapture (photo auto + chiffrement AES)
- ✅ IntruderGalleryScreen (galerie photos)
- ✅ Nettoyage auto (30j, max 50)

#### Jour 7 : Documentation
- ✅ MVP_ROADMAP.md mis à jour
- ✅ SPEC.md mis à jour
- ✅ Tests sur device complets

### Architecture Finale
```
SensorManager (4 capteurs)
       ↓
ThreatAssessmentEngine (fusion)
       ↓ Flow<ThreatAssessment>
PrivacyGuardService
       ↓
ProtectionExecutor + IntruderCapture
       ↓
OverlayManager
   ├── PrivacyIndicatorView (pilule vert/jaune/rouge)
   ├── SoftBlurOverlayView (flou + raisons)
   ├── DecoyScreenOverlayView (faux écran)
   └── LockScreenOverlayView (verrouillage)
       ↓
UI Screens
   ├── MainActivity (Home)
   ├── DashboardScreen (Stats)
   ├── SettingsScreen (Config)
   └── IntruderGalleryScreen (Photos)
```

### Fichiers Code Créés (MVP Complet)

#### Capteurs (`sensors/`)
- [x] `BaseSensor.kt` - Classe de base
- [x] `CameraSensor.kt` - ML Kit Face Detection
- [x] `AudioSensor.kt` - Niveau sonore + parole
- [x] `MotionSensor.kt` - Accéléromètre
- [x] `ProximitySensor.kt` - Capteur proximité
- [x] `SensorManager.kt` - Gestionnaire centralisé

#### Évaluation (`assessment/`)
- [x] `ThreatAssessmentEngine.kt` - Moteur principal
- [x] `SensorDataFusion.kt` - Fusion multi-capteurs
- [x] `ThreatScorer.kt` - Scoring pondéré
- [x] `models/ThreatModels.kt` - Modèles données

#### Protection (`protection/`)
- [x] `ProtectionExecutor.kt` - Exécution actions
- [x] `OverlayManager.kt` - Gestion overlays
- [x] `PrivacyIndicatorView.kt` - Indicateur flottant
- [x] `SoftBlurOverlayView.kt` - Flou progressif
- [x] `DecoyScreenOverlayView.kt` - Écran leurre
- [x] `LockScreenOverlayView.kt` - Verrouillage
- [x] `IntruderCapture.kt` - Capture + chiffrement

#### UI (`ui/`)
- [x] `MainActivity.kt` - Home + navigation
- [x] `screens/DashboardScreen.kt` - Stats temps réel
- [x] `screens/SettingsScreen.kt` - Configuration
- [x] `screens/IntruderGalleryScreen.kt` - Galerie photos
- [x] `PermissionsScreen.kt` - Gestion permissions
- [x] `CameraPreviewWithFaceDetection.kt` - Preview debug

#### Service
- [x] `PrivacyGuardService.kt` - Foreground service

#### Tests
- [x] `CameraSensorTest.kt`
- [x] `AudioSensorTest.kt`
- [x] `MotionSensorTest.kt`
- [x] `ProximitySensorTest.kt`

### Git
- [x] Branche "sami" créée
- [x] ~30+ commits sur 7 jours
- [x] Lien: https://github.com/samizouari/VibePrivacy/tree/sami

---

## 🔄 Workflow Type pour une Session de Code

### 1. Début de Session
```
L'utilisateur dit : "Je veux implémenter [FEATURE]"
```

**MOI (IA) je dois :**
1. ✅ Consulter SPEC.md (ce fichier)
2. ✅ Consulter PROJECT_CONTEXT.md
3. ✅ Consulter MVP_ROADMAP.md (quel jour ?)
4. ✅ Vérifier si [FEATURE] est dans le scope MVP
5. ✅ Consulter fichier doc pertinent (SENSORS.md, FEATURES.md, etc.)
6. ✅ Expliquer ce que je vais faire
7. ✅ Générer le code
8. ✅ Générer les tests
9. ✅ Suggérer commit message
10. ✅ **METTRE À JOUR WORKFLOW_VIBE_CODING_TEMPLATE.md**
11. ✅ Mettre à jour les checkboxes MVP_ROADMAP.md
12. ✅ Mettre à jour SPEC.md section "État Actuel"

### 2. Debugging
```
L'utilisateur dit : "J'ai ce bug : [ERREUR]"
```

**MOI (IA) je dois :**
1. ✅ Analyser l'erreur
2. ✅ Consulter le code concerné
3. ✅ Proposer solution avec explication
4. ✅ Fournir code corrigé
5. ✅ **DOCUMENTER dans WORKFLOW_VIBE_CODING_TEMPLATE.md**
   - Section "Problèmes Rencontrés"
   - Bug + Solution + Apprentissage
6. ✅ Suggérer commit : `fix(scope): description`

### 3. Fin de Journée
```
L'utilisateur dit : "On a fini le jour X"
```

**MOI (IA) je dois :**
1. ✅ Récapituler ce qui a été fait
2. ✅ Vérifier checkboxes MVP_ROADMAP.md Jour X
3. ✅ Mettre à jour section "Métriques" dans WORKFLOW_VIBE_CODING_TEMPLATE.md
4. ✅ Préparer preview du Jour X+1
5. ✅ Mettre à jour SPEC.md "État Actuel"
6. ✅ Suggérer ce qui reste à faire

---

## 🎨 Standards Spécifiques au Projet

### Design
- **Couleurs** : Sobre (noir, blanc, gris + accent bleu)
- **Style** : Material Design 3, moderne, minimaliste
- **Langue UI** : Français uniquement
- **Icône** : Affirme "privacy" (œil barré, bouclier+œil, etc.)

### Nommage Packages
```
com.privacyguard/
├── service/
├── sensors/
│   ├── camera/
│   ├── audio/
│   ├── motion/
│   └── proximity/
├── assessment/
├── protection/
├── ml/
├── ui/
├── data/
├── domain/
├── di/
└── utils/
```

### Tests
- Tests unitaires : `*Test.kt` dans `test/`
- Tests intégration : `*IntegrationTest.kt` dans `test/`
- Tests UI : `*UITest.kt` dans `androidTest/`
- Coverage : Viser 60%+ pour MVP

### Commits
Format : `<type>(<scope>): <description>`

Exemples :
```
feat(camera): add ML Kit face detection
fix(audio): resolve memory leak in analyzer
docs(readme): update installation steps
test(sensors): add motion detector tests
perf(ml): optimize inference speed
refactor(ui): simplify overlay structure
```

---

## 📊 Métriques à Suivre

### À Mettre à Jour dans WORKFLOW_VIBE_CODING_TEMPLATE.md

#### Code
- Lignes Kotlin : [compteur]
- Lignes Tests : [compteur]
- Fichiers créés : [compteur]
- % généré par IA : [estimation]

#### Temps
- Temps par jour : [heures]
- Temps total : [heures]
- Gain vs sans IA : [estimation]

#### Qualité
- Tests passants : [X/Y]
- Couverture : [%]
- Linter warnings : [nombre]
- Latence mesurée : [ms]
- Battery drain mesuré : [%/h]

---

## 🎓 Points pour Évaluation TP

### À Mettre en Avant (pour la note)

#### 1. Workflow Vibe Coding (20%)
- ✅ Documentation exhaustive AVANT code
- ✅ Prompts structurés et contextualisés
- ✅ Itération rapide avec IA
- ✅ Tests continus
- **À documenter dans WORKFLOW_VIBE_CODING_TEMPLATE.md**

#### 2. Innovation Technique (25%)
- ✅ Fusion multi-capteurs intelligente
- ✅ ML embarqué temps réel (ML Kit)
- ✅ Protection non-intrusive
- ✅ 100% local (privacy first)
- ✅ Capture forensique intrus

#### 3. Qualité Code (20%)
- ✅ Architecture Clean + MVVM
- ✅ Kotlin idiomatique
- ✅ Coroutines/Flow
- ✅ Tests présents
- ✅ Code documenté

#### 4. Tests et Déploiement (20%)
- ✅ Tests unitaires
- ✅ Tests device physique
- ✅ APK fonctionnel
- ✅ Performance mesurée

#### 5. Documentation (15%)
- ✅ README complet
- ✅ Workflow vibe coding documenté
- ✅ Guide utilisateur
- ✅ Architecture expliquée

---

## 🚨 Rappels Importants

### Chaque Fois que je Génère du Code

**JE DOIS** :
1. 📝 **Documenter dans WORKFLOW_VIBE_CODING_TEMPLATE.md**
2. ✅ Cocher les tâches dans MVP_ROADMAP.md
3. 🔄 Mettre à jour SPEC.md "État Actuel"
4. 🧪 Générer les tests associés
5. 📋 Suggérer un commit message

**JE NE DOIS PAS** :
- ❌ Générer du code hors scope MVP
- ❌ Oublier de documenter
- ❌ Violer les règles de sécurité
- ❌ Ignorer les contraintes de performance
- ❌ Générer du code sans tests

### Questions à me Poser Systématiquement

Avant de générer du code :
1. ✅ Est-ce dans le scope MVP ?
2. ✅ Quel jour du MVP_ROADMAP ?
3. ✅ Quels fichiers doc consulter ?
4. ✅ Quelles dépendances nécessaires ?
5. ✅ Quels tests associés ?
6. ✅ Comment je vais documenter ça ?

---

## 📞 Actions Rapides (Commandes)

### Pour l'Utilisateur

Pour me déclencher facilement :

```bash
# Démarrer une nouvelle feature
"Implémente [FEATURE] selon [DOC.md]"

# Débugger
"J'ai ce bug : [STACKTRACE]"

# Générer tests
"Génère tests pour [CLASS]"

# Fin de journée
"Jour X terminé, récap"

# État du projet
"Où en sommes-nous ?"
```

### Pour Moi (IA)

Checklist rapide avant de répondre :

```
[ ] Consulté SPEC.md ?
[ ] Consulté PROJECT_CONTEXT.md ?
[ ] Consulté MVP_ROADMAP.md ?
[ ] Dans le scope MVP ?
[ ] Vais documenter dans WORKFLOW ?
[ ] Vais suggérer commit ?
[ ] Vais générer tests ?
```

---

## 🎯 Objectif Final

### Livrable dans 7 Jours

1. ✅ **Code source complet** (Git avec historique)
2. ✅ **APK fonctionnel** testé sur device
3. ✅ **Documentation workflow vibe coding** (WORKFLOW_VIBE_CODING_TEMPLATE.md rempli)
4. ✅ **Démo préparée** (5-10 minutes)
5. ✅ **README complet** avec guide utilisateur
6. ✅ **Tests passants** avec métriques

### Critères de Succès

- Application compile sans erreur
- 4 capteurs fonctionnels (caméra, audio, mouvement, proximité)
- Mode Discret détecte et protège (flou)
- Au moins 1 écran leurre
- Capture intrus fonctionne
- Dashboard avec statistiques
- Testé sur device physique réel
- Performance : latence < 200ms, batterie < 10%/h
- Documentation complète et workflow documenté

---

## 📝 Notes pour Moi (IA)

### Ce Document est Mon Guide

- **Consulter AVANT chaque action**
- **Suivre les processus définis**
- **Documenter systématiquement**
- **Rester dans le scope MVP**
- **Maintenir la qualité et les contraintes**

### Si Doute

1. Consulter SPEC.md (ce fichier)
2. Consulter PROJECT_CONTEXT.md
3. Consulter le doc pertinent (SENSORS.md, FEATURES.md, etc.)
4. Demander confirmation à l'utilisateur si vraiment incertain

### Garder en Tête

- ⏰ **Deadline : 7 jours**
- 🎯 **Priorité : Mode Discret fonctionnel**
- 📝 **Documentation = Aussi important que code**
- 🧪 **Tests = Non négociables**
- 🔒 **Privacy = Valeur fondamentale**

---

**Dernière mise à jour** : Jour 7 - MVP COMPLET  
**Statut** : ✅ PRÊT POUR LA DÉMO

---

## ✅ Checklist MVP Complète

- [x] SPEC.md créé et mis à jour
- [x] PROJECT_CONTEXT.md créé
- [x] MVP_ROADMAP.md créé et complété
- [x] WORKFLOW_VIBE_CODING_TEMPLATE.md documenté
- [x] Projet Android créé et fonctionnel
- [x] 4 capteurs implémentés et testés
- [x] Système de fusion et scoring
- [x] Overlays de protection
- [x] Dashboard et Settings
- [x] Capture intrus avec chiffrement
- [x] Tests sur device physique
- [x] ~30+ commits

**🎉 MVP PRIVACY GUARD TERMINÉ ! 🎉**

