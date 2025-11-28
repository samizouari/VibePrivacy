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

### 🟣 JOUR 5 : Dashboard, Config et Écrans Leurres

#### Matin (4h)
- [ ] **MainActivity et Navigation**
  - `ui/MainActivity.kt`
  - Navigation Compose
  - Écrans principaux
- [ ] **Dashboard**
  - `ui/dashboard/DashboardActivity.kt`
  - Stats du jour (menaces détectées)
  - Timeline événements
  - Composant Compose
- [ ] **COMMIT** : "feat(ui): add dashboard"

#### Après-midi (4h)
- [ ] **Settings Screen**
  - `ui/settings/SettingsActivity.kt`
  - Mode Discret (par défaut)
  - Sensibilité par capteur
  - Applications protégées (liste)
- [ ] **Écran Leurre Statique**
  - `ui/overlay/DecoyScreenOverlay.kt`
  - Liste de courses (customisable)
- [ ] **COMMIT** : "feat(ui): add settings and decoy screen"

#### Soir
- [ ] **Écran Leurre Dynamique** (si temps)
  - API Météo (OpenWeatherMap gratuit)
  - Affichage météo réaliste
- [ ] Tests UI complets

**Livrable Jour 5** : Interface complète fonctionnelle avec dashboard et paramètres

---

### 🟠 JOUR 6 : Capture Intrus, Tests et Optimisation

#### Matin (4h)
- [ ] **Capture Photo Intrus**
  - `protection/IntruderCapture.kt`
  - Photo automatique lors de menace
  - Stockage chiffré
  - Galerie des intrus dans dashboard
- [ ] **ScreenshotBlocker**
  - `protection/ScreenshotBlocker.kt`
  - FLAG_SECURE pour apps sensibles
- [ ] **COMMIT** : "feat(protection): add intruder photo capture"

#### Après-midi (4h)
- [ ] **Tests Complets**
  - Tests unitaires manquants
  - Tests d'intégration
  - Tests UI (Compose)
  - Tests sur device physique
- [ ] **Performance**
  - Mesurer latence (doit être < 200ms)
  - Optimiser si nécessaire
  - Profiler batterie
- [ ] **COMMIT** : "test: add comprehensive test suite"

#### Soir
- [ ] **Polish et Bug Fixes**
  - Corriger tous les bugs trouvés
  - Améliorer animations
  - Stabilité
- [ ] APK de debug pour tests

**Livrable Jour 6** : Application stable, testée, performante avec capture intrus

---

### 🔵 JOUR 7 : Documentation, Démo et Finition

#### Matin (3h)
- [ ] **README.md complet**
  - Présentation du projet
  - Instructions installation
  - Guide d'utilisation
  - Screenshots/GIFs
- [ ] **WORKFLOW_VIBE_CODING.md**
  - Méthodologie utilisée
  - Prompts clés (exemples)
  - Outils utilisés
  - Processus itératif
  - Retour d'expérience
- [ ] **USER_GUIDE.md**
  - Guide utilisateur
  - Configuration
  - Cas d'usage

#### Après-midi (3h)
- [ ] **Préparation Démo**
  - Script de démo (5-10 min)
  - Scénarios à montrer
  - Slides si nécessaire
  - Réponses questions anticipées
- [ ] **APK Final**
  - Build release
  - Signature
  - Test sur device clean
- [ ] **Video démo** (optionnel)

#### Soir (2h)
- [ ] **Derniers Commits**
  - Nettoyage code
  - Comments finaux
  - Version 1.0.0
- [ ] **Git Push Final**
- [ ] **Package pour rendu**
  - Code source (zip)
  - APK
  - Documentation
  - Tout prêt à rendre

**Livrable Jour 7** : Projet complet, documenté, prêt à rendre et démo

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

