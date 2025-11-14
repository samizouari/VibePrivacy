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

### Phase 3 : Génération de Code Initiale

#### [À COMPLÉTER AU FUR ET À MESURE]

#### Exemple de Prompt pour Setup Projet
```
Crée-moi la structure complète du projet Android avec :
- Configuration Gradle avec toutes les dépendances nécessaires
- AndroidManifest.xml avec permissions
- Structure de packages selon ARCHITECTURE.md
- Classes de base (Application, MainActivity)
- Configuration Hilt pour DI
```

#### Code Généré
[Copier ici les fichiers générés]

#### Modifications Nécessaires
[Noter ce qui a dû être ajusté manuellement]

---

### Phase 4 : Développement Itératif des Capteurs

#### [À COMPLÉTER JOUR 2]

#### Exemple Prompt CameraMonitor
```
Implémente CameraMonitor.kt selon SENSORS.md avec :
- CameraX pour la capture
- ML Kit Face Detection
- Détection nombre de visages
- Estimation distance basique
- Tests unitaires

Utilise les meilleures pratiques Kotlin et Coroutines.
```

#### Code Généré
[Coller le code généré]

#### Tests sur Device
- [ ] Caméra se lance correctement
- [ ] Détection de visages fonctionne
- [ ] Performance acceptable

#### Problèmes Rencontrés
[Documenter les bugs et comment ils ont été résolus avec l'IA]

---

### Phase 5 : Intégration et Fusion

#### [À COMPLÉTER JOUR 3]

#### Prompt ThreatAssessmentEngine
```
Implémente le moteur de fusion des capteurs qui :
- Combine les résultats de CameraMonitor, AudioAnalyzer, MotionDetector, ProximityWatcher
- Calcule un score de menace (0-100)
- Utilise les seuils du Mode Discret (75+)
- Gère les cas où certains capteurs sont indisponibles

Code avec Kotlin Flow pour la réactivité.
```

#### Résultat
[Code généré]

#### Ajustements
[Ce qui a été modifié après tests]

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

1. **[À COMPLÉTER] ML Kit Configuration**
   - Problème : [Décrire]
   - Solution : [Décrire]
   - Prompt utilisé : [Copier]

2. **[À COMPLÉTER] Permissions Runtime**
   - Problème : [Décrire]
   - Solution : [Décrire]

3. **[À COMPLÉTER] Performance Overlay**
   - Problème : [Décrire]
   - Solution : [Décrire]

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

- Nombre total : [À COMPLÉTER]
- Commits par jour : ~5-8
- Convention : Conventional Commits

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
- [Lister des repos GitHub consultés]

### Tutoriels Suivis
- [Lister les tutoriels]

### Stack Overflow
- [Lister les questions importantes]

---

## 🎯 Conclusion

### Objectifs Atteints
- [ ] Application fonctionnelle
- [ ] 4 capteurs intégrés
- [ ] ML Kit opérationnel
- [ ] Mode Discret fonctionnel
- [ ] Tests présents
- [ ] Documentation complète

### Améliorations Futures (Hors MVP)
- Reconnaissance faciale propriétaire
- Keyword spotting
- Modes spéciaux (transport, nuit, réunion)
- Zones de confiance GPS
- Écrans leurres plus sophistiqués

### Retour d'Expérience Personnel
[À REMPLIR À LA FIN]

**Ce qui m'a surpris** :  
[...]

**Ce que j'ai appris** :  
[...]

**Ce que je ferais différemment** :  
[...]

---

**Date de rédaction** : [Date]  
**Auteur** : Sami - ENSEEIHT N7  
**Version** : 1.0

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

