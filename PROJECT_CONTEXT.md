# Contexte du Projet - Privacy Guard

## 🎓 Contexte Académique

### Institution
**ENSEEIHT (N7) - 3ème Année**

### Type de Projet
**TP : Créer votre application mobile avec l'IA**

### Objectif du TP
Développer une application mobile innovante en utilisant les techniques de vibe coding apprises. 
L'application doit exploiter au minimum **3-4 capteurs ou fonctionnalités natives** de l'appareil mobile.

### Deadline
**Semaine prochaine** (7 jours)

## 📋 Critères d'Évaluation

| # | Critère | Poids | Détails |
|---|---------|-------|---------|
| 1 | **Workflow vibe coding documenté** | 20% | Méthodologie, outils utilisés, processus de développement |
| 2 | **Innovation technique** | 25% | Utilisation créative des capteurs, originalité de l'application |
| 3 | **Qualité du code** | 20% | Architecture, lisibilité, bonnes pratiques |
| 4 | **Tests et déploiement** | 20% | Tests sur device réel, compilation réussie, fonctionnalités opérationnelles |
| 5 | **Documentation** | 15% | README, guide d'utilisation, retour d'expérience |

## 📦 Livrables Attendus

- ✅ Code source complet avec historique Git
- ✅ Application compilée et testée sur mobile
- ✅ Documentation du workflow vibe coding + exemples de prompts clés
- ✅ Démonstration fonctionnelle (5-10 minutes)

## 🎯 Décisions Projet

### Scope - MVP Fonctionnel

**Fonctionnalité Critique : MODE DISCRET**
- Protection minimale mais efficace
- Uniquement menaces directes
- Floutage progressif
- Pas de verrouillage

### Capteurs Prioritaires (Ordre)

1. **📹 Caméra** - Détection de visages (ML Kit)
2. **🎤 Audio** - Détection de voix multiples
3. **📱 Mouvement** - Accéléromètre/Gyroscope (mouvements brusques)
4. **🤏 Proximité** - Main devant l'écran
5. **📍 GPS** - (Optionnel pour MVP)
6. **💡 Luminosité** - (Optionnel pour MVP)

**Pour le MVP : Utiliser les 4 premiers capteurs**

### Technologies

#### Machine Learning
- **ML Kit de Google** (recommandé pour rapidité de développement)
  - Face Detection API intégrée
  - Optimisé et bien documenté
  - Fonctionne offline
  - Facile à intégrer

#### Plateforme
- **Android natif** avec Kotlin
- **Jetpack Compose** pour UI moderne
- **CameraX** pour caméra
- **Room** pour base de données

#### Compatibilité
- **Android 8.0+ (API 26+)** - Large compatibilité
- Optimisé pour devices low-end aussi
- Test sur device physique réel disponible

## 🎨 Design et UX

### Style Visuel
- **Sobre et moderne**
- **Minimaliste** (pas trop de couleurs)
- **Palette** : Noir, blanc, gris + un accent (bleu pour privacy)
- Material Design 3

### Icône
- Affirme la fonction "Privacy"
- Suggestions : 
  - Œil barré
  - Bouclier avec œil
  - Cadenas + œil

### Langue
- **Français uniquement** pour le MVP

## 🔐 Politique de Données

### Confidentialité
- ✅ **0% télémétrie** - Aucune donnée envoyée
- ✅ **Traitement 100% local**
- ✅ **Pas de serveur backend**
- ✅ **Pas d'analytics**
- ✅ **Pas de crash reporting**

### Licence
- **Propriétaire** (code fermé)
- Pour TP académique uniquement

## 🚀 Fonctionnalités MVP

### Mode de Protection
- ✅ **Mode Discret** (priorité absolue)
  - Seuil élevé de déclenchement (75/100)
  - Détection menaces directes uniquement
  - Pas de verrouillage
  - Floutage progressif

### Actions de Protection
- ✅ **Niveau 1 : Masquage Doux** (flou gaussien)
- ✅ **Niveau 2 : Écran Leurre**
  - Contenus statiques ET dynamiques
  - Configuration par utilisateur
- ⚠️ **Niveau 3 : Verrouillage** (si temps permet)

### Écrans Leurres
**Statiques** (configurables par utilisateur) :
- Liste de courses personnalisée
- Notes de travail personnalisées
- Page web sauvegardée

**Dynamiques** :
- Météo réelle (API)
- Article Wikipedia aléatoire
- Actualités

### Fonctionnalités Importantes
- ✅ **Capture photo intrus** (stockage sécurisé)
- ✅ **Dashboard statistiques**
- ✅ **Journal des événements**
- ✅ **Configuration par app**

### Modes Spéciaux (Important mais Phase 2 si temps)
- Mode Transport
- Mode Réunion
- Mode Nuit
- Mode Présentation

## 📱 Distribution

**Toutes les options** :
- ✅ APK direct (pour le TP)
- ⚠️ Google Play Store (si temps et $25)
- ⚠️ F-Droid (si temps)

**Priorité : APK fonctionnel pour démo**

## 🧪 Tests

### Tests Requis
- ✅ Tests unitaires (capteurs, détection)
- ✅ Tests d'intégration
- ✅ Tests UI (Compose)
- ✅ Tests sur device physique réel
- ✅ Tests de performance (latence < 200ms)
- ✅ Tests de batterie (< 10%/h drain)

### Contraintes de Performance
**STRICTES** :
- Latence totale < 200ms
- Battery drain < 10%/h
- FPR (Faux Positifs) < 5%
- FNR (Faux Négatifs) < 1%

## 📊 Planning MVP (7 jours)

### Jour 1 : Setup + Architecture
- Création projet Android Studio
- Structure des dossiers
- Configuration Gradle
- Setup Git

### Jour 2 : Capteurs de Base
- CameraMonitor + ML Kit Face Detection
- AudioAnalyzer (détection voix)
- Tests unitaires capteurs

### Jour 3 : Détection et Fusion
- MotionDetector (accéléromètre)
- ProximityWatcher
- ThreatAssessmentEngine
- Scoring et seuils

### Jour 4 : Protection et UI
- ProtectionExecutor
- Overlay flou
- Écran leurre basique
- PrivacyIndicator

### Jour 5 : Dashboard et Config
- Interface principale
- Paramètres
- Configuration apps
- Écrans leurres configurables

### Jour 6 : Tests et Polish
- Tests complets
- Corrections bugs
- Optimisation performance
- Capture intrus

### Jour 7 : Documentation et Démo
- README complet
- Documentation workflow vibe coding
- Préparation démo
- APK final

## 📝 Documentation Workflow Vibe Coding

### À Documenter
1. **Prompts clés utilisés**
   - Exemple : prompt pour architecture
   - Exemple : prompt pour implémentation capteurs
   - Exemple : prompt pour résolution bugs

2. **Outils utilisés**
   - Claude/ChatGPT pour génération code
   - Android Studio
   - Git
   - Device physique pour tests

3. **Processus itératif**
   - Comment l'IA a aidé à structurer le projet
   - Itérations et améliorations
   - Problèmes résolus avec l'IA

4. **Retour d'expérience**
   - Ce qui a bien fonctionné
   - Difficultés rencontrées
   - Apprentissages

## 🎯 Critères de Succès MVP

### Fonctionnel
- ✅ Application compile et s'installe
- ✅ 4 capteurs fonctionnels
- ✅ Détection de menaces basique marche
- ✅ Masquage flou fonctionne
- ✅ Au moins 1 écran leurre opérationnel
- ✅ Interface utilisable

### Technique
- ✅ Architecture propre et documentée
- ✅ Code Kotlin idiomatique
- ✅ Tests présents et passent
- ✅ Performance acceptable
- ✅ Git bien utilisé (commits réguliers)

### Documentation
- ✅ README clair
- ✅ Workflow vibe coding documenté
- ✅ Code commenté
- ✅ Guide utilisation

### Démo
- ✅ Démo 5-10 minutes préparée
- ✅ Cas d'usage concrets montrés
- ✅ Innovation technique mise en avant
- ✅ Réponses aux questions préparées

## 🚨 Risques et Mitigations

| Risque | Probabilité | Impact | Mitigation |
|--------|-------------|--------|------------|
| Deadline trop courte | Haute | Haute | Scope réduit au strict nécessaire (Mode Discret) |
| ML Kit complexe | Moyenne | Haute | Tutoriels officiels Google, exemples existants |
| Performance insuffisante | Moyenne | Haute | Tests précoces, optimisations ciblées |
| Bugs device réel | Moyenne | Moyenne | Tests réguliers sur device physique |
| Accessibility Service refusé | Faible | Haute | Alternative : Overlay sans Accessibility |

## 💡 Innovations Techniques (pour évaluation)

### Points Forts à Mettre en Avant

1. **Fusion Multi-Capteurs Intelligente**
   - Combinaison caméra + audio + mouvement + proximité
   - Scoring pondéré adaptatif
   - Réduction faux positifs

2. **Machine Learning Embarqué**
   - Détection faciale temps réel
   - Traitement 100% local (privacy)
   - Optimisé pour batterie

3. **Protection Non-Intrusive**
   - Overlay discret
   - Animations fluides
   - UX pensée pour être invisible

4. **Écrans Leurres Dynamiques**
   - Génération contenu réaliste
   - APIs externes (météo, wiki)
   - Configuration utilisateur

5. **Capture Forensique**
   - Photo automatique intrus
   - Stockage sécurisé chiffré
   - Timeline des incidents

## 📞 Contact Projet

- **Développeur** : Sami
- **Établissement** : ENSEEIHT (N7)
- **Année** : 3A
- **Date** : Novembre 2024

---

**Note** : Ce document sert de référence pour toutes les décisions du projet. Il sera mis à jour au fil du développement.

