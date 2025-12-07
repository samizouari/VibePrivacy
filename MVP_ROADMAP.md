# Roadmap MVP - 7 Jours (Privacy Guard)

## 🎯 Objectif : Application Fonctionnelle en 7 Jours

**Fonctionnalité Centrale : MODE DISCRET**
- Détection menaces directes uniquement
- Protection par floutage progressif
- 4 capteurs minimum (caméra, audio, mouvement, proximité)

## 📅 Planning Détaillé

### 🔵 JOUR 1 : Setup et Architecture (Fondations)

#### Matin (4h)
- [ ] Créer projet Android Studio
  - Package : `com.privacyguard`
  - Min SDK : 26 (Android 8.0)
  - Target SDK : 34
  - Kotlin + Jetpack Compose
- [ ] Configuration Gradle
  - Dépendances : CameraX, ML Kit, Room, Hilt
- [ ] Structure de dossiers selon ARCHITECTURE.md
- [ ] Init Git + premier commit

#### Après-midi (4h)
- [ ] Classes de base
  - `PrivacyGuardApplication.kt`
  - `MainActivity.kt`
  - Package structure complète
- [ ] Configuration Hilt (DI)
- [ ] Configuration Room Database
  - Entities basiques
  - DAOs
- [ ] Permissions dans Manifest
- [ ] **COMMIT** : "feat: initial project setup"

#### Soir (optionnel)
- [ ] Lire documentation ML Kit Face Detection
- [ ] Tester build sur device physique

**Livrable Jour 1** : Projet qui compile, s'installe sur device, structure en place

---

### 🟢 JOUR 2 : Capteurs Caméra et Audio ✅ COMPLET

#### Matin (4h)
- [x] **CameraSensor** ✅
  - `sensors/CameraSensor.kt` ✅
  - Configuration CameraX ✅
  - ML Kit Face Detection intégration ✅
  - Détection nombre de visages ✅
  - Estimation distance basique ✅
- [x] Tests unitaires CameraSensor ✅
- [x] **COMMIT** : "feat(camera): add face detection with ML Kit" ✅

#### Après-midi (4h)
- [x] **AudioSensor** ✅
  - `sensors/AudioSensor.kt` ✅
  - Capture audio (microphone) ✅
  - Détection niveau sonore ✅
  - Comptage voix basique (amplitude) ✅
- [x] Tests unitaires AudioSensor ✅
- [x] **MotionSensor** ✅ (fait en avance)
- [x] **ProximitySensor** ✅ (fait en avance)
- [x] Tests unitaires MotionSensor ✅
- [x] Tests unitaires ProximitySensor ✅
- [x] Test intégration sur device : tous les capteurs fonctionnent ✅
- [x] **COMMIT** : "feat(audio): add voice detection" ✅
- [x] **COMMIT** : "test(sensors): add unit tests for all sensors" ✅

#### Soir
- [x] Débug si problèmes ✅ (fix format image YUV, logs ProximitySensor)
- [x] Documentation des capteurs ✅

**Livrable Jour 2** : ✅ **TERMINÉ** - Tous les capteurs fonctionnent, tests unitaires complets, testé sur device

---

### 🟡 JOUR 3 : Fusion et Évaluation ✅ COMPLET

#### Matin (4h)
- [x] **MotionSensor** (fait Jour 2)
  - `sensors/MotionSensor.kt`
  - Accéléromètre avec détection mouvements brusques
- [x] **ProximitySensor** (fait Jour 2)
  - `sensors/ProximitySensor.kt`
  - Détection objet proche
- [x] Tests capteurs (fait Jour 2)

#### Après-midi (4h)
- [x] **ThreatAssessmentEngine** ✅
  - `assessment/ThreatAssessmentEngine.kt`
  - Fusion des 4 capteurs en temps réel
  - Pipeline Flow asynchrone avec debounce
  - Gestion du contexte (mode, zone confiance, bruit ambiant)
- [x] **SensorDataFusion** ✅
  - `assessment/SensorDataFusion.kt`
  - Combinaison et évaluation des résultats
  - Identification des raisons de déclenchement
- [x] **ThreatScorer** ✅
  - `assessment/ThreatScorer.kt`
  - Scoring pondéré (Caméra 40%, Audio 30%, Motion 20%, Proximité 10%)
  - Normalisation des données capteurs
  - Redistribution des poids si capteurs manquants
- [x] **Modèles** ✅
  - `assessment/models/ThreatModels.kt`
  - ProtectionMode (PARANOIA/BALANCED/DISCRETE/TRUST_ZONE)
  - SensorWeights, ThreatAssessment, ProtectionAction
  - Seuils : Paranoïa=20, Équilibré=50, Discret=75
- [x] Intégration dans PrivacyGuardService ✅
- [x] Tests unitaires (ThreatAssessmentEngineTest, ThreatScorerTest) ✅
- [x] **COMMIT** : "feat(assessment): add threat scoring engine with multi-sensor fusion"

**Livrable Jour 3** : ✅ **TERMINÉ** - Système de détection complet qui calcule un score de menace en temps réel

---

---

### 🟢 JOUR 4 : Protection et Overlay UI ✅ COMPLET

#### Matin (4h)
- [x] **ProtectionExecutor** ✅
  - `protection/ProtectionExecutor.kt`
  - Logique d'exécution des actions de protection
  - Gestion des transitions entre niveaux de protection
  - Anti-oscillation avec délai minimum entre actions
  - Restauration automatique après timeout
- [x] **OverlayManager** ✅
  - `protection/OverlayManager.kt`
  - Gestion permission SYSTEM_ALERT_WINDOW
  - Affichage/masquage des overlays
  - Coordination des différents types d'overlays
- [x] **SoftBlurOverlayView** ✅
  - `protection/SoftBlurOverlayView.kt`
  - Voile semi-transparent avec dégradé
  - Intensité configurable
  - Affichage des raisons de déclenchement
  - Double-tap pour désactiver
- [x] **COMMIT** : "feat(protection): add blur protection and overlay system"

#### Après-midi (4h)
- [x] **PrivacyIndicatorView** ✅
  - `protection/PrivacyIndicatorView.kt`
  - Petit indicateur flottant (pilule)
  - États : Safe (vert), Monitoring (jaune), Threat (rouge)
  - Animation de pulsation pour THREAT
  - Transition de couleur animée
- [x] **DecoyScreenOverlayView** ✅
  - `protection/DecoyScreenOverlayView.kt`
  - Écran leurre ressemblant à un écran verrouillé
  - Affichage heure/date en temps réel
  - Pattern secret (5 taps) pour désactiver
- [x] **LockScreenOverlayView** ✅
  - `protection/LockScreenOverlayView.kt`
  - Écran de verrouillage opaque
  - Pattern secret (3 taps) pour désactiver
  - Animation de pulsation
- [x] Intégration dans PrivacyGuardService ✅
- [x] Gestion permission overlay dans MainActivity ✅
- [x] **COMMIT** : "feat(ui): add privacy indicator and protection overlays"

#### Soir
- [ ] Test E2E : Détection → Protection sur device
- [ ] Ajustement des seuils si nécessaire

**Livrable Jour 4** : ✅ **TERMINÉ** - Système de protection complet avec overlays (flou, écran leurre, verrouillage)

**Livrable Jour 4** : App détecte menaces et floute l'écran automatiquement

---

### 🟣 JOUR 5 : Dashboard, Config et Écrans Leurres ✅ COMPLET

#### Matin (4h)
- [x] **MainActivity et Navigation** ✅
  - `ui/MainActivity.kt` avec navigation Compose
  - Écrans principaux (Home, Settings, Dashboard, Gallery)
- [x] **Dashboard** ✅
  - `ui/screens/DashboardScreen.kt`
  - Stats en temps réel (durée session, menaces, score moyen)
  - État des capteurs (actif/inactif)
  - Historique des menaces
- [x] **COMMIT** : "feat(ui): add dashboard"

#### Après-midi (4h)
- [x] **Settings Screen** ✅
  - `ui/screens/SettingsScreen.kt`
  - Sélecteur de mode de protection
  - 4 modes : Paranoïa, Équilibré, Discret, Zone Confiance
  - Persistance SharedPreferences
- [x] **Écrans Leurres** ✅ (fait Jour 4)
  - DecoyScreenOverlayView : Écran verrouillé factice
  - Pattern secret (5 taps) pour désactiver
- [x] **COMMIT** : "feat(ui): add settings and decoy screen"

**Livrable Jour 5** : ✅ **TERMINÉ** - Interface complète avec Dashboard, Settings, et navigation

---

### 🟠 JOUR 6 : Capture Intrus, Tests et Optimisation ✅ COMPLET

#### Matin (4h)
- [x] **Capture Photo Intrus** ✅
  - `protection/IntruderCapture.kt`
  - Photo automatique sur menace HIGH/CRITICAL
  - Chiffrement AES des photos
  - Stockage sécurisé (dossier privé app)
  - Nettoyage automatique (30 jours, max 50 photos)
- [x] **Galerie Intrus** ✅
  - `ui/screens/IntruderGalleryScreen.kt`
  - Grille de photos avec miniatures
  - Affichage plein écran
  - Suppression individuelle ou totale
- [x] **COMMIT** : "feat(protection): add intruder photo capture with encryption"

#### Après-midi (4h)
- [x] **Tests Complets** ✅
  - Tests unitaires capteurs (CameraSensorTest, AudioSensorTest, etc.)
  - Tests ThreatScorer et ThreatAssessmentEngine
  - Tests sur device physique
- [x] **Intégration** ✅
  - CameraSensor : sauvegarde dernier frame
  - SensorManager : méthode getCameraSensor()
  - PrivacyGuardService : capture auto sur menace

**Livrable Jour 6** : ✅ **TERMINÉ** - Capture d'intrus fonctionnelle avec chiffrement et galerie

---

### 🔵 JOUR 7 : Documentation, Démo et Finition ✅ COMPLET

#### Matin (3h)
- [x] **Documentation complète** ✅
  - MVP_ROADMAP.md mis à jour
  - SPEC.md mis à jour
  - WORKFLOW_VIBE_CODING_TEMPLATE.md mis à jour
- [x] **Architecture documentée** ✅
  - Structure des packages
  - Flow des données capteurs → évaluation → protection

#### Après-midi (3h)
- [x] **Application fonctionnelle** ✅
  - 4 capteurs : Caméra, Audio, Mouvement, Proximité
  - Système de scoring avec fusion multi-capteurs
  - Overlays de protection (Indicateur, Flou, Écran Leurre, Lock)
  - Dashboard et Settings
  - Capture d'intrus chiffrée
- [x] **Tests sur device physique** ✅
  - Détection de visages fonctionnelle
  - Détection audio fonctionnelle
  - Indicateur flottant fonctionnel

**Livrable Jour 7** : ✅ **TERMINÉ** - MVP complet et fonctionnel

---

## 🎉 RÉSUMÉ MVP COMPLET

### ✅ Fonctionnalités Livrées

| Feature | Status | Description |
|---------|--------|-------------|
| **Capteurs** | ✅ | 4 capteurs (Camera, Audio, Motion, Proximity) |
| **Détection visages** | ✅ | ML Kit Face Detection |
| **Détection audio** | ✅ | Niveau sonore et parole |
| **Fusion capteurs** | ✅ | Score pondéré 0-100 |
| **Modes protection** | ✅ | 4 modes (Paranoïa → Zone Confiance) |
| **Indicateur flottant** | ✅ | Vert/Jaune/Rouge avec animation |
| **Overlay flou** | ✅ | Floutage progressif |
| **Écran leurre** | ✅ | Lock screen factice |
| **Capture intrus** | ✅ | Photo auto + chiffrement AES |
| **Dashboard** | ✅ | Stats temps réel |
| **Settings** | ✅ | Sélecteur de mode |
| **Galerie intrus** | ✅ | Voir/supprimer photos |

### 📊 Métriques Projet

- **Commits** : ~30+ commits sur 7 jours
- **Fichiers Kotlin** : ~25+ fichiers
- **Tests** : 5 fichiers de tests unitaires
- **Lignes de code** : ~5000+ lignes

---

## 📋 Checklist Finale MVP

### Fonctionnalités Core ✅
- [ ] Détection faciale (ML Kit)
- [ ] Détection audio (voix)
- [ ] Détection mouvement (brusque)
- [ ] Détection proximité
- [ ] Fusion capteurs avec scoring
- [ ] Protection par flou progressif
- [ ] Écran leurre (au moins 1)
- [ ] Capture photo intrus
- [ ] Dashboard statistiques
- [ ] Configuration de base
- [ ] Indicateur privacy flottant

### Technique ✅
- [ ] Architecture propre (MVVM + Clean)
- [ ] Code Kotlin idiomatique
- [ ] Dépendances injectées (Hilt)
- [ ] Base de données (Room)
- [ ] Permissions gérées correctement
- [ ] Pas de crash
- [ ] Performance acceptable (latence < 200ms)
- [ ] Tests présents et passants

### Documentation ✅
- [ ] README complet
- [ ] Workflow vibe coding documenté
- [ ] Guide utilisateur
- [ ] Code commenté
- [ ] Architecture expliquée

### Livrables ✅
- [ ] Code source sur Git
- [ ] APK fonctionnel
- [ ] Documentation complète
- [ ] Démo préparée

## 🎯 Features MVP vs Nice-to-Have

### ✅ MUST HAVE (MVP)
- Mode Discret
- 4 capteurs (caméra, audio, motion, proximity)
- Flou gaussien
- 1 écran leurre minimum
- Dashboard basique
- Capture intrus
- Config basique

### ⚠️ NICE TO HAVE (si temps)
- Écrans leurres dynamiques (météo, wiki)
- Mode Équilibré/Paranoïa
- Zones de confiance
- Visages de confiance
- Modes spéciaux (transport, nuit, etc.)
- Export de données
- Statistiques avancées

### ❌ HORS SCOPE MVP
- Face Recognition custom
- Keyword spotting
- Mode Panique
- Mode Stealth
- Smart glasses integration
- Federated learning
- Enterprise features

## ⚡ Conseils pour Réussir en 7 Jours

### 1. Priorisation Stricte
- Se concentrer UNIQUEMENT sur le Mode Discret
- Pas de perfectionnisme
- "Done is better than perfect"

### 2. Tests Réguliers
- Tester sur device physique CHAQUE JOUR
- Ne pas accumuler les bugs

### 3. Commits Fréquents
- Commit après chaque feature
- Permet de revenir en arrière si problème

### 4. Documentation Au Fur et à Mesure
- Noter les prompts utilisés
- Documenter en codant, pas à la fin

### 5. Demander de l'Aide
- Utiliser l'IA pour débloquer rapidement
- Consulter documentation officielle
- Stack Overflow pour bugs spécifiques

### 6. Scope Creep = Ennemi
- Résister à la tentation d'ajouter features
- Rester focus sur MVP
- Noter les idées pour "après le rendu"

### 7. Dormir !
- Pas de all-nighters
- Cerveau reposé = plus efficace

## 🚨 Points de Vigilance

### Jour 2-3
- ML Kit peut être tricky → Suivre tutoriel Google à la lettre
- Permissions Runtime importantes

### Jour 4
- Overlay nécessite permission spéciale → Bien gérer
- RenderEffect nécessite API 31+ → Fallback si nécessaire

### Jour 5
- Ne pas passer trop de temps sur UI
- Garder design simple

### Jour 6
- Chiffrement photos → Utiliser EncryptedFile
- Tests peuvent révéler bugs cachés → Buffer temps

### Jour 7
- Ne pas attendre dernier moment pour doc
- APK peut avoir problèmes de signature → Tester avant

## 📞 Ressources Utiles

### Documentation Officielle
- [ML Kit Face Detection](https://developers.google.com/ml-kit/vision/face-detection/android)
- [CameraX Documentation](https://developer.android.com/training/camerax)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room Database](https://developer.android.com/training/data-storage/room)

### Tutoriels
- Rechercher "ML Kit Face Detection Android tutorial"
- Exemple CameraX + ML Kit sur GitHub

### Outils
- Android Studio Profiler (pour performance)
- Logcat (pour debugging)

---

**Dernière mise à jour** : Avant de commencer

**Bonne chance ! 🚀**

