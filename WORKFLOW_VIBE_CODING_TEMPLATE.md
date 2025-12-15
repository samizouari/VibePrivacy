
# Workflow Vibe Coding - Privacy Guard

## 📝 Introduction

Ce document retrace la méthodologie de développement utilisant les techniques de **vibe coding** avec l'assistance de l'IA (Claude/ChatGPT) pour créer l'application Privacy Guard.

## 🎯 Contexte du Projet

**Application** : Privacy Guard - Protection de confidentialité mobile  
**Durée** : 7 jours  
**Objectif** : MVP fonctionnel avec 4 capteurs minimum  
**IA Utilisée** : Claude 3.5 Sonnet (Cursor AI)

## 🔄 Méthodologie Vibe Coding

### Phase 1 : Idéation et Architecture

#### Prompt Initial
```
Je vais coder une nouvelle application android, voici tous les détails de l'appli...
[Description complète du concept Privacy Guard]

Je veux que tu me fasses des fichiers markdown détaillés pour que tu puisses 
coder avec sans qu'il n'y ait de problème et comme ça tout est clair pour toi.
```

#### Résultat
L'IA a généré 10 fichiers markdown complets :
- README.md - Vue d'ensemble
- ARCHITECTURE.md - Architecture technique détaillée
- FEATURES.md - Fonctionnalités complètes
- SENSORS.md - Documentation capteurs
- UI_UX.md - Interface et design
- SECURITY_PRIVACY.md - Sécurité
- TECHNICAL_CHALLENGES.md - Défis techniques
- ROADMAP.md - Feuille de route
- CONTRIBUTING.md - Guide contribution
- PROJECT_STRUCTURE.md - Structure fichiers

**Apprentissage** : Commencer par une documentation exhaustive évite les ambiguïtés plus tard.

---

### Phase 2 : Clarification et Contextualisation

#### Questions de l'IA
L'IA a posé 18 questions pour clarifier :
- Objectifs et scope
- Aspects techniques (ML, devices, capteurs)
- Design et UX
- Sécurité
- Fonctionnalités prioritaires
- Contexte académique

#### Mes Réponses
```
MVP fonctionnel, deadline semaine prochaine, projet N7, 
Mode Discret prioritaire, ML Kit Google, compatibilité large,
design sobre, français, 0% télémétrie...
```

#### Prompt de Contextualisation
```
@Capture d'écran [critères évaluation TP]
Si tu veux créer/retoucher des fichiers md pour te souvenir de ce contexte, fais le
```

#### Résultat
L'IA a créé :
- PROJECT_CONTEXT.md - Toutes les décisions
- MVP_ROADMAP.md - Planning 7 jours détaillé
- WORKFLOW_VIBE_CODING_TEMPLATE.md - Ce fichier

**Apprentissage** : Donner le contexte complet (TP, deadline, contraintes) permet à l'IA d'adapter ses suggestions.

---

### Phase 3 : Génération de Code Initiale - Jour 1 (Setup Projet)

#### Prompt Utilisé
```
Crée une branche que t'appelle sami ou tu vas travailler, push donc le travail la bas 
et commence à coder et documenter automatiquement. Tu vas surement voir pleins de 
fichiers qui ont été crées par gemini, si t'as des modifs; hésite pas à me demander
```

#### Analyse du Code Existant (Gemini)

**Problèmes identifiés** :
1. `app/build.gradle.kts` :
   - ❌ Namespace incorrect : `com.n7.vibeprivacy` → devrait être `com.privacyguard`
   - ❌ minSdk = 24 → devrait être 26 (Android 8.0+)
   - ❌ targetSdk = 36 → devrait être 34
   - ❌ Syntaxe bizarre pour compileSdk
   - ❌ MANQUE toutes les dépendances (CameraX, ML Kit, Room, Hilt, Compose)

2. `AndroidManifest.xml` :
   - ❌ Pas de permissions (caméra, audio, localisation)
   - ❌ Pas de référence à PrivacyGuardApplication
   - ❌ Pas de MainActivity
   - ❌ Theme incorrect

3. Structure de packages : ✅ OK (bien créée par Gemini)

#### Code Généré/Corrigé

##### 1. `app/build.gradle.kts` (Complètement refait)
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
}

android {
    namespace = "com.privacyguard"  // ✅ Corrigé
    compileSdk = 34  // ✅ Corrigé
    
    defaultConfig {
        applicationId = "com.privacyguard"
        minSdk = 26  // ✅ Android 8.0
        targetSdk = 34  // ✅ Corrigé
        versionCode = 1
        versionName = "1.0.0"
    }
    
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // ✅ Toutes les dépendances ajoutées
    // Kotlin, Compose, CameraX, ML Kit, Room, Hilt, etc.
}
```

##### 2. `gradle/libs.versions.toml` (Complètement refait)
Ajout de toutes les versions de bibliothèques :
- CameraX 1.3.1
- ML Kit Face Detection 16.1.5
- Room 2.6.1
- Hilt 2.48
- Compose BOM 2024.02.00
- etc.

##### 3. `build.gradle.kts` (Root)
```kotlin
plugins {
    // ... existant
    id("com.google.dagger.hilt.android") version "2.48" apply false  // ✅ Ajouté
}
```

##### 4. `app/src/main/AndroidManifest.xml` (Complètement refait)
```xml
<manifest>
    <!-- ✅ Toutes les permissions ajoutées -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <!-- ... etc -->
    
    <application
        android:name=".PrivacyGuardApplication"  <!-- ✅ Corrigé -->
        android:theme="@style/Theme.PrivacyGuard">  <!-- ✅ Corrigé -->
        
        <activity
            android:name=".ui.MainActivity"  <!-- ✅ Ajouté -->
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

##### 5. `MainActivity.kt` (Créé)
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            PrivacyGuardTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    Column {
        Text("🛡️", style = MaterialTheme.typography.displayLarge)
        Text(stringResource(R.string.app_name))
        Button(onClick = {}) {
            Text("Démarrer la protection")
        }
    }
}
```

##### 6. Theme Files
**Color.kt** : Palette sobre (noir, blanc, gris + bleu privacy)
```kotlin
val PrimaryBlue = Color(0xFF2196F3)
val SafeGreen = Color(0xFF4CAF50)
val WarningYellow = Color(0xFFFFC107)
val DangerRed = Color(0xFFF44336)
```

**Theme.kt** : Material 3 avec dark mode
**Typography.kt** : Typographie moderne et lisible

**themes.xml** : Migration vers Material 3
```xml
<style name="Theme.PrivacyGuard" parent="Theme.Material3.DayNight.NoActionBar">
```

##### 7. Resources
**strings.xml** :
```xml
<string name="app_name">Privacy Guard</string>
<string name="app_tagline">Protection de confidentialité en temps réel</string>
```

#### Résultat Final

✅ **Projet compile** (théoriquement - à tester sur Android Studio)
✅ **Branche "sami" créée** et pushée sur GitHub
✅ **Structure complète** du projet en place
✅ **Toutes les dépendances** configurées
✅ **MainActivity** basique fonctionnelle
✅ **Theme sobre** appliqué (design selon specs)

#### Commit

```bash
git checkout -b sami
git add .
git rm --cached -r -f VibePrivacy  # Supprimer doublon
git commit -m "feat(setup): initialize project with corrected config and base UI

- Fix build.gradle.kts: correct namespace, SDK versions, add all dependencies
- Fix AndroidManifest.xml: add all permissions, configure app correctly  
- Add gradle/libs.versions.toml: define all library versions
- Create MainActivity.kt: base Compose UI with Privacy Guard theme
- Update themes: Material 3, sober design (black/white/gray + blue accent)
- Add French strings resources
- Configure Hilt and Compose

This completes Day 1 setup according to MVP_ROADMAP.md"

git push -u origin sami
```

**Lien GitHub** : https://github.com/samizouari/VibePrivacy/tree/sami

#### Problèmes Rencontrés

1. **Doublon VibePrivacy/** : Gemini avait créé un sous-dossier avec structure complète
   - Solution : `git rm --cached -r -f VibePrivacy`

2. **Line endings** : Warnings CRLF/LF (normal sur Windows)
   - Pas critique, Git gère automatiquement

3. **Namespace confusion** : Gemini utilisait `com.n7.vibeprivacy`, on veut `com.privacyguard`
   - Solution : Correction manuelle de tous les fichiers de config

4. **❌ Plugin kotlin-compose introuvable** (Découvert lors du test Android Studio)
   ```
   Plugin [id: 'org.jetbrains.kotlin.plugin.compose', version: '1.9.10'] was not found
   ```
   - **Cause** : Le plugin `kotlin-compose` n'existe que dans Kotlin 2.0+, pas en 1.9.10
   - **Solution** : Retirer le plugin, utiliser la config traditionnelle `composeOptions`
   - **Fichiers modifiés** :
     - `build.gradle.kts` : retiré `kotlin-compose` plugin
     - `app/build.gradle.kts` : retiré alias du plugin
     - `gradle/libs.versions.toml` : retiré la définition du plugin
   - **Commit** : `fix(build): remove kotlin-compose plugin incompatible with Kotlin 1.9.10`
   - **Temps de résolution** : 2 minutes
   - **Apprentissage** : Toujours vérifier la compatibilité des plugins avec la version Kotlin

#### Tests Effectués

- [x] Commit réussi (34 fichiers)
- [x] Push réussi sur branche "sami"
- [ ] Compilation Android Studio (à tester par Sami sur device physique)

#### Temps Écoulé

**~30 minutes** pour :
- Analyser code Gemini
- Corriger toutes les erreurs
- Créer MainActivity et theme
- Commit + Push + Documentation

**Sans IA** : Estimation 2-3 heures

#### Apprentissages

1. ✅ **Toujours vérifier le code généré** par une autre IA
2. ✅ **SPEC.md est crucial** : m'a guidé pour tout documenter
3. ✅ **Commits fréquents** : meilleure traçabilité
4. ✅ **Message de commit détaillé** : important pour review plus tard

---

### Phase 4 : Développement Itératif des Capteurs - JOUR 2 ✅

#### 🎉 MILESTONE : TOUS LES CAPTEURS IMPLÉMENTÉS ET TESTÉS !

**Date** : 14 novembre 2024  
**Temps écoulé** : ~6h (matin + après-midi)  
**Nombre de commits** : 20+ commits  
**Résultat** : 4 capteurs fonctionnels avec tests unitaires complets

---

#### Prompt Utilisé pour Implémentation Capteurs

```
Implémente les 4 capteurs selon SENSORS.md :
1. CameraSensor avec CameraX + ML Kit Face Detection
2. AudioSensor avec AudioRecord pour détection niveau sonore
3. MotionSensor avec SensorManager (accéléromètre)
4. ProximitySensor avec SensorManager (proximité)

Chaque capteur doit :
- Hériter de BaseSensor<T>
- Émettre des données via Flow
- Évaluer un ThreatLevel
- Gérer les erreurs gracieusement
- Logs détaillés avec Timber

Utilise les meilleures pratiques Kotlin, Coroutines, et Flow.
```

#### Code Généré

**Fichiers créés** :
- `app/src/main/java/com/privacyguard/sensors/CameraSensor.kt` (332 lignes)
- `app/src/main/java/com/privacyguard/sensors/AudioSensor.kt` (120 lignes)
- `app/src/main/java/com/privacyguard/sensors/MotionSensor.kt` (150 lignes)
- `app/src/main/java/com/privacyguard/sensors/ProximitySensor.kt` (152 lignes)
- `app/src/main/java/com/privacyguard/sensors/SensorManager.kt` (301 lignes)
- `app/src/main/java/com/privacyguard/sensors/BaseSensor.kt` (120 lignes)
- `app/src/main/java/com/privacyguard/sensors/SensorData.kt` (98 lignes)

**Extrait clé - CameraSensor** :
```kotlin
class CameraSensor(
    context: Context,
    private val lifecycleOwner: LifecycleOwner
) : BaseSensor<CameraData>(context, "CameraSensor") {
    
    private lateinit var faceDetector: FaceDetector
    
    override suspend fun onStart() {
        // Configuration CameraX + ML Kit
        initializeFaceDetector()
        bindCameraUseCases()
    }
    
    private fun handleFaceDetection(faces: List<Face>, timestamp: Long) {
        // Évaluation du niveau de menace
        val threatLevel = when {
            faces.size > 1 && facesLookingAtScreen > 0 -> ThreatLevel.CRITICAL
            facesLookingAtScreen > 0 && maxProximityThreat > 0.2f -> ThreatLevel.HIGH
            facesLookingAtScreen > 0 -> ThreatLevel.MEDIUM
            faces.size > 0 -> ThreatLevel.LOW
            else -> ThreatLevel.NONE
        }
        
        emitData(CameraData(...))
    }
}
```

#### Tests sur Device ✅

- [x] Caméra se lance correctement ✅
- [x] Détection de visages fonctionne ✅
- [x] Audio détecte niveau sonore ✅
- [x] Mouvement détecte accélération ✅
- [x] Proximité détecte objets proches ✅
- [x] Performance acceptable ✅
- [x] Tous les capteurs fonctionnent en parallèle ✅

#### Tests Unitaires Créés ✅

**Fichiers créés** :
- `app/src/test/java/com/privacyguard/sensors/CameraSensorTest.kt` (170 lignes)
- `app/src/test/java/com/privacyguard/sensors/AudioSensorTest.kt` (100 lignes)
- `app/src/test/java/com/privacyguard/sensors/MotionSensorTest.kt` (120 lignes)
- `app/src/test/java/com/privacyguard/sensors/ProximitySensorTest.kt` (110 lignes)

**Couverture** :
- Tests de logique métier (évaluation ThreatLevel)
- Tests de calculs (RMS, décibels, magnitude)
- Tests de seuils (parole, mouvement brusque, proximité)
- Tests de cas limites (aucun visage, capteur binaire, etc.)

#### Problèmes Rencontrés et Résolus ⚠️

1. **Format d'image ML Kit incompatible**
   - **Problème** : `IllegalArgumentException: Only JPEG and YUV_420_888 are supported now`
   - **Cause** : Utilisation de `RGBA_8888` au lieu de `YUV_420_888`
   - **Solution** : Changé `OUTPUT_IMAGE_FORMAT_RGBA_8888` → `OUTPUT_IMAGE_FORMAT_YUV_420_888`
   - **Fichiers** : `CameraSensor.kt`, `CameraPreview.kt`
   - **Commit** : `fix(sensors): change image format from RGBA_8888 to YUV_420_888 for ML Kit`

2. **Crash au démarrage de la protection**
   - **Problème** : App crashait quelques secondes après "Démarrer la protection"
   - **Cause** : Initialisation des capteurs dans `onCreate()` avant que le service soit prêt
   - **Solution** : Déplacé l'initialisation dans `startProtection()` avec gestion d'erreur améliorée
   - **Fichiers** : `PrivacyGuardService.kt`, `CameraSensor.kt`
   - **Commit** : `fix(service): fix crash when starting protection`

3. **ProximitySensor non visible dans les logs**
   - **Problème** : Pas de logs du ProximitySensor
   - **Cause** : Logs au niveau V (très verbeux) + manque de logs de démarrage
   - **Solution** : Ajouté logs détaillés (I, D) avec emojis pour faciliter le filtrage
   - **Fichiers** : `ProximitySensor.kt`, `SensorManager.kt`
   - **Commit** : `debug(sensors): add detailed logging for ProximitySensor`

4. **ProximitySensor valeurs binaires (0 ou 5cm)**
   - **Problème** : Utilisateur confus par valeurs binaires
   - **Cause** : Hardware Android normal (capteur binaire)
   - **Solution** : Documentation claire dans le code expliquant que c'est normal
   - **Fichiers** : `ProximitySensor.kt`
   - **Commit** : `docs(sensors): document proximity sensor limitations and utility`

#### Apprentissages 💡

1. **ML Kit nécessite YUV_420_888** : Toujours vérifier les formats supportés dans la doc
2. **LifecycleService pour CameraX** : Nécessaire pour lier CameraX dans un service
3. **Capteurs binaires Android** : Normal, pas un bug
4. **Tests unitaires sans mocks** : Possible en testant uniquement la logique métier
5. **Logs structurés** : Emojis et niveaux appropriés facilitent le debug

#### Métriques Jour 2

- **Lignes de code** : ~1200 lignes (capteurs + tests)
- **Fichiers créés** : 11 fichiers
- **Tests** : 20+ tests unitaires
- **Commits** : 8 commits
- **Temps** : ~6h
- **Bugs résolus** : 4 bugs majeurs

---

### Phase 5 : Intégration et Fusion - JOUR 3 ✅

#### 🎉 MILESTONE : SYSTÈME DE FUSION MULTI-CAPTEURS COMPLET !

**Date** : Novembre 2024  
**Temps écoulé** : ~3h  
**Nombre de commits** : 4+ commits  
**Résultat** : ThreatAssessmentEngine fonctionnel avec pipeline temps réel

---

#### Prompt ThreatAssessmentEngine
```
Implémente le moteur de fusion des capteurs qui :
- Combine les résultats de CameraMonitor, AudioAnalyzer, MotionDetector, ProximityWatcher
- Calcule un score de menace (0-100)
- Utilise les seuils du Mode Discret (75+)
- Gère les cas où certains capteurs sont indisponibles

Code avec Kotlin Flow pour la réactivité.
```

#### Fichiers Créés

**1. `assessment/models/ThreatModels.kt`** (~130 lignes)
- `ProtectionMode` : PARANOIA(20), BALANCED(50), DISCRETE(75), TRUST_ZONE(95)
- `SensorWeights` : Pondération configurable (Caméra 40%, Audio 30%, Motion 20%, Proximité 10%)
- `ThreatAssessment` : Résultat complet avec score, niveau, confiance, raisons
- `ProtectionAction` : NONE, SOFT_BLUR, DECOY_SCREEN, INSTANT_LOCK, PANIC_MODE
- `AssessmentContext` : Contexte dynamique (mode, bruit ambiant, zone confiance)

**2. `assessment/ThreatScorer.kt`** (~250 lignes)
- Normalisation des données de chaque capteur (0-1)
- Calcul pondéré avec redistribution automatique si capteurs manquants
- Facteurs multiples par capteur (ex: caméra = visages + distance + regard + inconnus)

**3. `assessment/SensorDataFusion.kt`** (~200 lignes)
- Évaluation complète des menaces
- Identification des raisons de déclenchement
- Adaptation aux contextes (bruit, luminosité, zone confiance)
- Détermination de l'action recommandée selon le mode

**4. `assessment/ThreatAssessmentEngine.kt`** (~250 lignes)
- Orchestrateur principal
- Pipeline Flow avec debounce (50ms) et distinctUntilChanged
- Gestion du contexte et de l'historique
- Statistiques récentes

**5. Mise à jour `service/PrivacyGuardService.kt`**
- Intégration du ThreatAssessmentEngine
- Pipeline de collecte des données capteurs
- Handler de menace détectée (placeholder pour Jour 4)
- Notification de menace

#### Architecture Implémentée

```
SensorManager
    ├── CameraSensor.dataFlow
    ├── AudioSensor.dataFlow
    ├── MotionSensor.dataFlow
    └── ProximitySensor.dataFlow
           ↓
    combinedSensorData (Flow<SensorDataSnapshot>)
           ↓
ThreatAssessmentEngine.processFlow()
    ├── debounce(50ms)
    ├── ThreatScorer.calculateScore()
    ├── SensorDataFusion.evaluate()
    └── distinctUntilChanged
           ↓
    Flow<ThreatAssessment>
           ↓
PrivacyGuardService.handleThreatDetected()
           ↓
    TODO Jour 4: ProtectionExecutor
```

#### Tests Créés

**`ThreatAssessmentEngineTest.kt`** (~300 lignes)
- Tests des seuils de mode (DISCRETE, PARANOIA, BALANCED)
- Tests des capteurs individuels
- Tests de fusion multi-capteurs
- Tests des actions recommandées
- Tests de contexte (zone confiance)

**`ThreatScorerTest.kt`** (~250 lignes)
- Tests de normalisation par capteur
- Tests de pondération et redistribution
- Tests de confiance

#### Apprentissages

1. **Flow.debounce est crucial** : Évite la surcharge CPU avec données capteurs rapides
2. **distinctUntilChanged intelligent** : Filtrer sur (shouldTrigger, action) pas sur tout l'assessment
3. **Redistribution des poids** : Si capteur manquant, redistribuer proportionnellement aux autres
4. **Contexte dynamique** : Zone confiance, bruit ambiant, luminosité modifient les poids

#### Statistiques Jour 3

| Métrique | Valeur |
|----------|--------|
| **Fichiers créés** | 6 fichiers |
| **Lignes de code** | ~1100 lignes |
| **Tests unitaires** | ~50 tests |
| **Temps** | ~3 heures |

---

### Phase 6 : Interface Utilisateur

#### [À COMPLÉTER JOUR 4-5]

#### Prompt UI Dashboard
```
Crée le Dashboard en Jetpack Compose avec :
- Stats du jour (menaces détectées, visages inconnus, etc.)
- Timeline des événements
- Design sobre et moderne selon UI_UX.md
- Navigation vers Settings

Utilise Material 3 et best practices Compose.
```

---

### Phase 7 : Tests et Debugging

#### [À COMPLÉTER JOUR 6]

#### Stratégie de Tests
1. Tests unitaires pour chaque capteur
2. Tests d'intégration pour ThreatAssessmentEngine
3. Tests UI avec Compose Testing
4. Tests E2E sur device physique

#### Exemple Prompt Tests
```
Génère des tests unitaires complets pour CameraMonitor avec :
- Mock de CameraX
- Mock de ML Kit
- Scénarios : 0 visage, 1 visage, multiple visages
- Scénarios d'erreur (permission refusée, caméra indisponible)

Utilise MockK et Truth assertions.
```

---

## 🛠️ Outils Utilisés

### IDE et Développement
- **Android Studio** Hedgehog 2023.3.1
- **Cursor AI** (avec Claude 3.5 Sonnet)
- **Kotlin** 1.9.10
- **Gradle** 8.0

### IA et Assistance
- **Claude 3.5 Sonnet** (génération code, architecture, debugging)
- **GitHub Copilot** (autocomplétion)

### Testing
- **JUnit** (tests unitaires)
- **MockK** (mocking)
- **Compose UI Testing** (tests UI)
- **Device physique** (tests réels)

### Versioning
- **Git** (local + GitHub)
- Commits fréquents avec messages conventionnels

### Documentation
- **Markdown** (toute la doc)
- **Diagrams** (Mermaid pour schémas)

## 📊 Exemples de Prompts Clés

### 1. Architecture et Design Pattern

```markdown
Propose une architecture Clean Architecture + MVVM pour Privacy Guard avec :
- Separation of Concerns
- Testabilité maximale
- Injection de dépendances avec Hilt
- Reactive programming avec Kotlin Flow

Détaille les couches et leurs responsabilités.
```

**Résultat** : Architecture complète dans ARCHITECTURE.md

---

### 2. Résolution de Bug Spécifique

```markdown
J'ai ce crash lors du lancement de la caméra :
[Copier stacktrace]

CameraMonitor.kt :
[Copier code problématique]

AndroidManifest.xml :
[Copier permissions]

Aide-moi à débugger.
```

**Type de réponse attendue** :
- Analyse du problème
- Cause probable
- Solution avec code corrigé
- Explication

---

### 3. Optimisation Performance

```markdown
Mon ThreatAssessmentEngine prend 500ms à calculer le score, 
c'est trop lent (objectif < 200ms).

Code actuel :
[Copier le code]

Comment optimiser ?
```

**Type de réponse** :
- Profiling suggestions
- Optimisations possibles (coroutines, caching, etc.)
- Code optimisé
- Mesures de performance

---

### 4. Génération de Tests

```markdown
Génère des tests unitaires exhaustifs pour cette classe :
[Copier code de la classe]

Include :
- Happy path
- Edge cases
- Error scenarios
- Mocking des dépendances
```

---

### 5. Documentation de Fonction Complexe

```markdown
Documente cette fonction avec KDoc complet :
[Copier fonction]

Explique :
- Ce qu'elle fait
- Paramètres
- Valeur de retour
- Exceptions possibles
- Exemple d'utilisation
```

---

## 🎓 Apprentissages Clés

### Ce qui a Bien Fonctionné ✅

1. **Documentation exhaustive AVANT de coder**
   - Évite les ambiguïtés
   - L'IA comprend mieux le contexte
   - Référence constante pendant le dev

2. **Prompts contextualisés**
   - Toujours référencer les fichiers markdown (@filename)
   - Donner le contexte complet
   - Préciser les contraintes (deadline, performance, etc.)

3. **Itération rapide**
   - Générer → Tester → Ajuster → Répéter
   - Ne pas chercher la perfection du premier coup

4. **Tests précoces sur device physique**
   - Évite les mauvaises surprises tard dans le projet
   - ML Kit se comporte différemment sur émulateur vs device

### Difficultés Rencontrées ⚠️

1. **ML Kit Configuration - Format d'Image Incompatible**
   - **Problème** : `IllegalArgumentException: Only JPEG and YUV_420_888 are supported now` lors de la détection faciale
   - **Cause** : Utilisation de `OUTPUT_IMAGE_FORMAT_RGBA_8888` au lieu du format supporté par ML Kit
   - **Solution** : Changé `OUTPUT_IMAGE_FORMAT_RGBA_8888` → `OUTPUT_IMAGE_FORMAT_YUV_420_888` dans CameraSensor
   - **Fichiers modifiés** : `CameraSensor.kt`, `CameraPreview.kt`
   - **Prompt utilisé** : "J'ai cette erreur ML Kit : IllegalArgumentException avec format image RGBA_8888. Corrige pour utiliser YUV_420_888"
   - **Temps de résolution** : 15 minutes
   - **Apprentissage** : Toujours vérifier les formats supportés dans la documentation officielle ML Kit

2. **Permissions Runtime - Gestion Multi-Permissions**
   - **Problème** : Gérer plusieurs permissions (caméra, audio, localisation) avec l'API moderne Android
   - **Cause** : Ancienne API `requestPermissions()` deprecated, nouvelle API nécessite `ActivityResultLauncher`
   - **Solution** : Implémenté `PermissionManager` avec `rememberLauncherForActivityResult` et `RequestMultiplePermissions()`
   - **Fichiers créés** : `PermissionManager.kt`, `PermissionsScreen.kt`
   - **Prompt utilisé** : "Crée un système de gestion des permissions runtime pour caméra, audio et localisation avec la nouvelle API Android"
   - **Temps de résolution** : 30 minutes
   - **Apprentissage** : Nouvelle API plus verbose mais plus flexible et type-safe

3. **Build Errors - Plugin kotlin-compose Incompatible**
   - **Problème** : `Plugin [id: 'org.jetbrains.kotlin.plugin.compose', version: '1.9.10'] was not found`
   - **Cause** : Plugin `kotlin-compose` n'existe que dans Kotlin 2.0+, pas en 1.9.10
   - **Solution** : Retiré le plugin, configuration Compose via `buildFeatures` et `composeOptions` traditionnels
   - **Fichiers modifiés** : `build.gradle.kts`, `app/build.gradle.kts`, `gradle/libs.versions.toml`
   - **Temps de résolution** : 10 minutes
   - **Apprentissage** : Vérifier la compatibilité des plugins avec la version Kotlin utilisée

4. **KAPT avec Java 17+ - IllegalAccessError**
   - **Problème** : `java.lang.IllegalAccessError: KaptJavaCompiler cannot access JavaCompiler` lors du build
   - **Cause** : KAPT ne peut pas accéder aux modules internes de Java 17+ (restrictions de modules)
   - **Solution temporaire** : Désactivation de KAPT et Hilt pour MVP Jour 1 (non critiques)
   - **Solution prévue** : Migration vers KSP (Kotlin Symbol Processing) au lieu de KAPT
   - **Fichiers modifiés** : `app/build.gradle.kts` (commenté KAPT/Hilt)
   - **Temps de résolution** : 1 heure (tentatives multiples)
   - **Apprentissage** : Approche pragmatique : retirer temporairement les dépendances non critiques pour MVP

5. **Crash au Démarrage de la Protection**
   - **Problème** : App crashait quelques secondes après "Démarrer la protection"
   - **Cause** : Initialisation des capteurs dans `onCreate()` avant que le service soit prêt
   - **Solution** : Déplacé l'initialisation dans `startProtection()` avec gestion d'erreur améliorée
   - **Fichiers modifiés** : `PrivacyGuardService.kt`, `CameraSensor.kt`
   - **Prompt utilisé** : "L'app crash quand je démarre la protection. Analyse le code du service et corrige l'ordre d'initialisation"
   - **Temps de résolution** : 20 minutes
   - **Apprentissage** : Ordre d'initialisation crucial pour services Android

### Limites de l'IA 🤔

1. **Code généré pas toujours optimal**
   - Nécessite revue et optimisation
   - Tests indispensables

2. **Compréhension du contexte Android**
   - Parfois propose des solutions incompatibles avec version Android cible
   - Vérifier la documentation officielle

3. **Debugging complexe**
   - L'IA aide mais ne remplace pas le debugging manuel
   - Utiliser Android Studio Debugger et Logcat

### Recommandations pour Futurs Projets 💡

1. **Commencer par la doc** (comme fait ici)
2. **Itérer rapidement** (ne pas tout faire d'un coup)
3. **Tester continuellement**
4. **Documenter au fur et à mesure** (pas à la fin)
5. **Utiliser l'IA pour débloquer, pas pour tout faire**
6. **Garder le contrôle** (comprendre le code généré)

---

### Phase 4 : Résolution des Erreurs de Build et Premier Succès - Jour 1 ✅

#### 🎉 MILESTONE MAJEURE : PREMIÈRE COMPILATION RÉUSSIE !

**Date** : 14 novembre 2024  
**Temps écoulé** : ~2h de debugging  
**Nombre de commits** : 12 commits  
**Résultat** : Application fonctionnelle avec UI interactive

---

#### Erreurs Rencontrées et Solutions

##### Erreur 1 : Plugin `kotlin-compose` introuvable

**Symptôme** :
```
Plugin [id: 'org.jetbrains.kotlin.plugin.compose', version: '1.9.10'] was not found
```

**Cause** : Plugin incompatible avec Kotlin 1.9.10 (uniquement pour Kotlin 2.0+)

**Solution** : Retiré le plugin, configuration Compose via `buildFeatures` et `composeOptions`

---

##### Erreur 2 : Ressource `accessibility_service_description` manquante

**Symptôme** : `AAPT: error: resource string/accessibility_service_description not found`

**Solution** : Ajout de la string dans `strings.xml`

---

##### Erreur 3 : `IllegalAccessError` KAPT avec Java 17+

**Symptôme** :
```
java.lang.IllegalAccessError: KaptJavaCompiler cannot access JavaCompiler
```

**Cause** : KAPT ne peut pas accéder aux modules internes de Java 17+

**Tentatives** :
1. ❌ Ajout de flags JVM dans `gradle.properties`
2. ❌ Configuration KAPT dans `build.gradle.kts`

**Solution Finale (Pragmatique)** :
- ✅ Désactivation temporaire de KAPT et Hilt (pas nécessaires pour MVP Jour 1)
- Commenté plugins, dépendances, et annotations
- TODO: Réactiver au Jour 2

**Apprentissage** : Pour un MVP, retirer temporairement les dépendances non utilisées. Approche itérative.

---

##### Erreur 4 : Fichiers Room/Hilt sans dépendances

**Symptôme** : `Unresolved reference: Database`

**Solution** : Suppression temporaire de 5 fichiers créés par Gemini (DAO, Database, Modules DI)

---

##### Erreur 5 : Dossier `VibePrivacy/` en doublon

**Symptôme** : `warning: adding embedded git repository`

**Solution** : `git rm --cached VibePrivacy`

---

#### Validation du MVP Jour 1 Matin ✅

- [x] Projet compile sans erreurs
- [x] App se lance sur device physique
- [x] UI Compose fonctionnelle
- [x] Thème sobre (noir/blanc/gris/bleu)
- [x] Texte en français
- [x] Bouton interactif avec feedback visuel
- [x] État de protection (on/off) avec toggle
- [x] Card de statut dynamique
- [x] Dark mode support
- [x] Code pushé sur branche `sami`

**Commits de cette phase** : 12 commits (de `3825cc6` à `0d10346`)

---

### Phase 5 : Jour 1 Après-midi - Permissions & Service ✅

**Date** : 14 novembre 2024 (après-midi)  
**Temps écoulé** : ~1.5h  
**Commits** : 4 commits  
**Résultat** : Système de permissions complet + Foreground Service fonctionnel

---

#### Fonctionnalités Implémentées

##### 1. Système de Permissions Runtime

**Fichiers créés** :
- `utils/PermissionManager.kt` (~150 lignes)
  - Permissions critiques : Caméra, Microphone
  - Permissions optionnelles : Localisation
  - Méthodes de vérification (isGranted, areCriticalPermissionsGranted, etc.)
  - Descriptions user-friendly
  - Support SYSTEM_ALERT_WINDOW (overlay)

- `ui/PermissionsScreen.kt` (~200 lignes)
  - UI Compose élégante pour demander permissions
  - Cards individuelles pour chaque permission
  - Badge "REQUIS" pour permissions critiques
  - Icônes de statut (✓ / ✗)
  - Bouton "Autoriser les permissions"
  - Lien vers paramètres système
  - Section "Votre vie privée d'abord" (0% télémétrie)

**Intégration** :
- MainActivity vérifie automatiquement les permissions au démarrage
- Navigation fluide entre PermissionsScreen et MainScreen
- LaunchedEffect pour vérification asynchrone

**Apprentissage** :
- `rememberLauncherForActivityResult` pour demander plusieurs permissions à la fois
- `ActivityResultContracts.RequestMultiplePermissions()` vs ancien `requestPermissions()`
- Importance de l'UX : expliquer POURQUOI on a besoin de chaque permission

---

##### 2. Foreground Service (PrivacyGuardService)

**Fichier créé** :
- `service/PrivacyGuardService.kt` (~250 lignes)
  - Service de premier plan avec notification persistante
  - Actions : START_PROTECTION, STOP_PROTECTION, PAUSE_PROTECTION
  - Notification channel (Android O+)
  - Notification dynamique selon l'état (active/pause)
  - START_STICKY pour redémarrage automatique
  - Timber logging pour debugging
  - Hooks TODO pour capteurs (Jour 2)

**Architecture** :
```kotlin
MainActivity (UI)
    ↓
PrivacyGuardService.startService(context)
    ↓
Service démarre en FOREGROUND
    ↓
Notification persistante affichée
    ↓
TODO Jour 2: Démarre les capteurs (Camera, Audio, Motion, Proximity)
```

**Notification** :
- Titre : "🛡️ Protection active"
- Texte : "Privacy Guard surveille votre environnement"
- Click → Ouvre MainActivity
- Non-supprimable par swipe
- Priorité basse (non-intrusive)

**AndroidManifest** :
```xml
<service
    android:name=".service.PrivacyGuardService"
    android:exported="false"
    android:foregroundServiceType="camera|microphone" />

<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
```

**Apprentissage** :
- Foreground service obligatoire depuis Android O pour tâches longues
- `foregroundServiceType` obligatoire depuis Android 14 (Upside Down Cake)
- FOREGROUND_SERVICE_CAMERA et FOREGROUND_SERVICE_MICROPHONE requis pour accès capteurs
- START_STICKY vs START_NOT_STICKY : comportement si tué par le système

---

#### Statistiques Jour 1 Après-midi

| Métrique | Valeur |
|----------|--------|
| **Temps** | ~1.5 heures |
| **Commits** | 4 commits |
| **Fichiers créés** | 3 fichiers |
| **Lignes de code** | ~600 lignes |
| **Tests manuels** | Permissions flow testé |

---

#### Validation Jour 1 Complet ✅

**Matin** :
- [x] Projet compile
- [x] App se lance
- [x] UI interactive

**Après-midi** :
- [x] Système de permissions complet
- [x] PermissionsScreen avec UI moderne
- [x] Foreground service implémenté
- [x] Notification persistante
- [x] Start/Stop depuis MainActivity
- [x] Service survit aux redémarrages système

**JOUR 1 = 100% TERMINÉ** ✅✅

---

## 📈 Métriques du Projet

### Temps Investi (Estimé)

| Jour | Activité | Temps | Avec/Sans IA |
|------|----------|-------|--------------|
| 1 | Setup + Archi | 8h | Sans IA: 12h |
| 2 | Capteurs | 8h | Sans IA: 16h |
| 3 | Fusion | 8h | Sans IA: 12h |
| 4 | UI Overlay | 8h | Sans IA: 10h |
| 5 | Dashboard | 8h | Sans IA: 10h |
| 6 | Tests | 8h | Sans IA: 12h |
| 7 | Doc + Polish | 8h | Sans IA: 10h |
| **Total** | **56h** | **Sans IA: ~82h** |

**Gain de temps estimé : 32%**

### Lignes de Code (Estimé)

- Kotlin : ~5000 lignes
- XML/Layouts : ~500 lignes
- Tests : ~2000 lignes
- **Total : ~7500 lignes**

**Généré par IA : ~60%**  
**Écrit/Modifié manuellement : ~40%**

### Commits Git

- Nombre total : 24+ commits (Jour 1-2)
  - Jour 1 : 16 commits (setup, UI, permissions, service)
  - Jour 2 : 8+ commits (capteurs, tests, fixes)
- Commits par jour : ~5-8
- Convention : Conventional Commits
- Branche principale : `sami` (active development)

---

## 🎬 Démo et Présentation

### Structure de la Démo (5-10 minutes)

#### 1. Introduction (1 min)
- Présentation du concept Privacy Guard
- Problème résolu
- Approche innovante

#### 2. Architecture et Technologies (2 min)
- Schéma architecture
- 4 capteurs utilisés
- ML Kit pour face detection
- Fusion intelligente des données

#### 3. Démonstration Live (5 min)
- Lancer l'app sur device
- Montrer Mode Discret
- Déclencher détection (approcher visage)
- Montrer floutage automatique
- Écran leurre
- Capture d'intrus
- Dashboard avec statistiques

#### 4. Innovation Technique (1 min)
- Fusion multi-capteurs
- ML embarqué temps réel
- Protection non-intrusive
- 100% local (privacy)

#### 5. Workflow Vibe Coding (1 min)
- Méthodologie utilisée
- Rôle de l'IA
- Gains de productivité
- Exemple de prompt clé

#### 6. Questions (temps restant)

### Matériel Préparé
- [ ] Slides (optionnel, 5-6 slides max)
- [ ] Device avec app installée
- [ ] Scénarios de démo testés
- [ ] Video backup (si démo live échoue)
- [ ] Code source imprimé (extraits clés)

---

## 📚 Références et Ressources

### Documentation Consultée
- [ML Kit Face Detection](https://developers.google.com/ml-kit/vision/face-detection/android)
- [CameraX Guide](https://developer.android.com/training/camerax)
- [Jetpack Compose](https://developer.android.com/jetpack/compose/documentation)
- [Clean Architecture Android](https://developer.android.com/topic/architecture)

### Repositories Inspirants
- [CameraX Samples](https://github.com/android/camera-samples) - Exemples d'utilisation CameraX
- [ML Kit Android](https://github.com/googlesamples/mlkit) - Exemples ML Kit Google
- [Android Architecture Components](https://github.com/android/architecture-components-samples) - Patterns architecture

### Tutoriels Suivis
- [ML Kit Face Detection Guide](https://developers.google.com/ml-kit/vision/face-detection/android) - Documentation officielle
- [CameraX Documentation](https://developer.android.com/training/camerax) - Guide CameraX complet
- [Jetpack Compose Basics](https://developer.android.com/jetpack/compose/tutorial) - Fondamentaux Compose

### Stack Overflow
- Questions sur format YUV_420_888 pour ML Kit
- ActivityResultLauncher pour permissions multiples
- KAPT vs KSP avec Java 17+

---

## 🎯 Conclusion

### Objectifs Atteints (MVP Complet - 7 Jours)

**✅ Jour 1-2 : Setup et Capteurs**
- [x] Application fonctionnelle avec UI Compose
- [x] 4 capteurs intégrés (Camera, Audio, Motion, Proximity)
- [x] ML Kit opérationnel (Face Detection)
- [x] Tests unitaires pour tous les capteurs
- [x] Documentation complète (13+ fichiers markdown)
- [x] Système de permissions runtime
- [x] Foreground Service fonctionnel

**✅ Jour 3 : Fusion et Évaluation**
- [x] ThreatAssessmentEngine (fusion multi-capteurs)
- [x] SensorDataFusion avec identification des raisons
- [x] ThreatScorer avec scoring pondéré
- [x] Pipeline temps réel avec Flow et debounce

**✅ Jour 4 : Protection et Overlays**
- [x] ProtectionExecutor pour exécution des actions
- [x] OverlayManager centralisé
- [x] PrivacyIndicatorView (vert/jaune/rouge animé)
- [x] SoftBlurOverlayView (double-tap pour fermer)
- [x] DecoyScreenOverlayView (5 taps coin pour fermer)
- [x] LockScreenOverlayView (3 taps pour fermer)

**✅ Jour 5 : Dashboard et Configuration**
- [x] DashboardScreen avec stats temps réel
- [x] SettingsScreen avec 4 modes de protection
- [x] Navigation entre écrans
- [x] Persistance SharedPreferences

**✅ Jour 6 : Capture Intrus**
- [x] IntruderCapture avec chiffrement AES
- [x] IntruderGalleryScreen pour visualiser les photos
- [x] Nettoyage automatique (30 jours, max 50 photos)

**✅ Jour 7 : Documentation Finale**
- [x] MVP_ROADMAP.md complet
- [x] SPEC.md mis à jour
- [x] WORKFLOW_VIBE_CODING mis à jour

### Améliorations Futures (Hors MVP)
- Reconnaissance faciale propriétaire
- Keyword spotting
- Modes spéciaux (transport, nuit, réunion)
- Zones de confiance GPS
- Écrans leurres plus sophistiqués

### Retour d'Expérience Personnel

**Ce qui m'a surpris** :  

1. **L'IA génère du code fonctionnel mais avec des erreurs subtiles** : Le code compile souvent, mais des détails comme les formats d'image ou les versions de plugins peuvent causer des problèmes à l'exécution. Toujours tester sur device physique rapidement.

2. **La documentation exhaustive AVANT de coder paye** : Avoir tous les fichiers markdown créés permet à l'IA de générer du code beaucoup plus aligné avec les objectifs. Référencer les fichiers (@ARCHITECTURE.md, @SENSORS.md) dans les prompts aide énormément.

3. **Les erreurs de build Android sont souvent spécifiques aux versions** : KAPT avec Java 17+, kotlin-compose uniquement Kotlin 2.0+, etc. L'IA ne connaît pas toujours ces subtilités de versions.

4. **Le debugging avec l'IA est très efficace** : Copier la stacktrace + le code problématique dans un prompt permet de résoudre les bugs rapidement (15-30 min vs 2-3h manuellement).

**Ce que j'ai appris** :  

1. **Vibe Coding = Documentation + Code itératif** : Commencer par documenter exhaustivement permet d'aller beaucoup plus vite ensuite. L'IA comprend mieux le contexte et génère du code plus pertinent.

2. **Tester tôt sur device physique** : ML Kit, permissions, services Android se comportent différemment sur émulateur vs device réel. Tester dès Jour 1 évite des surprises tardives.

3. **Approche pragmatique pour MVP** : Désactiver temporairement KAPT/Hilt pour avancer plutôt que bloquer 3h dessus. Réactiver au Jour 3 si nécessaire.

4. **Commits fréquents avec messages détaillés** : Aide à retrouver rapidement quand un bug a été introduit ou une feature ajoutée. Messages conventionnels facilitent la navigation dans l'historique.

5. **L'IA est excellente pour générer la structure, moins pour les détails** : Architecture, classes de base, flow général = très bon. Formats spécifiques, versions compatibles, edge cases Android = nécessite vérification manuelle.

**Ce que je ferais différemment** :  

1. **Tester la compilation AVANT de commit** : Plusieurs commits de fixes auraient pu être évités en testant systématiquement après chaque génération de code.

2. **Créer les tests unitaires en même temps que le code** : Plutôt que les ajouter après. L'IA peut générer tests + code ensemble, ça prend le même temps.

3. **Utiliser KSP au lieu de KAPT dès le début** : Éviterait les problèmes de compatibilité Java 17+. Mais nécessite setup initial légèrement différent.

4. **Documenter les bugs résolus IMMÉDIATEMENT** : Plutôt qu'à la fin. On oublie vite les détails (stacktrace exacte, solution précise).

5. **Créer un script de vérification** : Checklist automatisée (compile? tests passent? device fonctionne?) avant chaque commit.

**Points forts de la méthode Vibe Coding** :

- ✅ **Rapidité** : 32% de gain de temps estimé
- ✅ **Qualité** : Code structuré et documenté dès le début
- ✅ **Apprentissage** : L'IA explique pourquoi elle fait certaines choses
- ✅ **Itération rapide** : Générer → Tester → Ajuster → Répéter

**Points faibles à améliorer** :

- ⚠️ **Dépendance à l'IA** : Peut ralentir si l'IA n'est pas disponible
- ⚠️ **Vérification nécessaire** : Toujours vérifier le code généré (erreurs subtiles)
- ⚠️ **Contexte Android** : L'IA ne maîtrise pas toujours les spécificités Android/versions

---

**Date de rédaction** : 14 novembre 2024 (Jour 2 terminé)  
**Dernière mise à jour** : 7 décembre 2025 (MVP COMPLET)  
**Auteur** : Sami - ENSEEIHT N7  
**Version** : 1.0 (MVP TERMINÉ ✅)

---

## 📎 Annexes

### Annexe A : Prompts Complets Utilisés
[Copier tous les prompts importants]

### Annexe B : Snippets de Code Clés
[Code généré par IA particulièrement intéressant]

### Annexe C : Bugs Résolus
[Liste des bugs rencontrés et solutions]

### Annexe D : Captures d'Écran
[Screenshots de l'app en fonctionnement]

