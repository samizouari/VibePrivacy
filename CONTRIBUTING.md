# Guide de Contribution

Merci de votre intérêt pour contribuer à Privacy Guard! Ce guide vous aidera à démarrer.

## 🤝 Comment Contribuer

### Types de Contributions

Nous acceptons plusieurs types de contributions:

1. **Corrections de bugs** 🐛
2. **Nouvelles fonctionnalités** ✨
3. **Amélioration de la documentation** 📚
4. **Optimisations de performance** ⚡
5. **Tests** 🧪
6. **Traductions** 🌍

## 🚀 Démarrage

### Prérequis

- Android Studio Hedgehog ou supérieur
- JDK 17+
- Kotlin 1.9+
- Git
- Un appareil Android physique ou émulateur (API 26+)

### Configuration du Projet

```bash
# 1. Forker le repository sur GitHub

# 2. Cloner votre fork
git clone https://github.com/VOTRE_USERNAME/privacy-guard.git
cd privacy-guard

# 3. Ajouter le repo upstream
git remote add upstream https://github.com/privacy-guard/privacy-guard.git

# 4. Créer une branche pour votre contribution
git checkout -b feature/ma-nouvelle-fonctionnalite

# 5. Ouvrir le projet dans Android Studio
```

### Build du Projet

```bash
# Build debug
./gradlew assembleDebug

# Run tests
./gradlew test

# Run linter
./gradlew ktlintCheck
```

## 📝 Processus de Contribution

### 1. Créer une Issue

Avant de commencer à coder, créez une issue pour discuter de votre contribution:

```markdown
## Description
[Décrivez le bug ou la fonctionnalité]

## Motivation
[Pourquoi cette contribution est-elle nécessaire?]

## Proposition
[Comment comptez-vous l'implémenter?]
```

### 2. Coder

#### Standards de Code

**Kotlin Style Guide:**
```kotlin
// ✅ BON
class ThreatDetector(
    private val cameraMonitor: CameraMonitor,
    private val audioAnalyzer: AudioAnalyzer
) {
    suspend fun detectThreat(): ThreatLevel {
        return withContext(Dispatchers.Default) {
            // Implementation
        }
    }
}

// ❌ MAUVAIS
class threatdetector {
    fun detect_threat() { // Pas de snake_case
        // ...
    }
}
```

**Naming Conventions:**
- Classes: `PascalCase`
- Fonctions: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Variables: `camelCase`
- Packages: `lowercase`

**Documentation:**
```kotlin
/**
 * Analyzes sensor data to detect potential privacy threats.
 *
 * @param sensorData Combined data from all sensors
 * @param context Current device and app context
 * @return Threat assessment with score (0-100) and recommended action
 * @throws SecurityException if sensors are unavailable
 */
suspend fun assessThreat(
    sensorData: SensorData,
    context: ContextInfo
): ThreatAssessment
```

#### Structure des Commits

```bash
# Format
<type>(<scope>): <subject>

# Types
feat: Nouvelle fonctionnalité
fix: Correction de bug
docs: Documentation uniquement
style: Formatage (pas de changement de code)
refactor: Refactoring
perf: Amélioration de performance
test: Ajout/modification de tests
chore: Maintenance

# Exemples
feat(camera): Add face recognition for trusted contacts
fix(audio): Resolve memory leak in audio analyzer
docs(readme): Update installation instructions
perf(ml): Optimize inference speed by 30%
```

#### Tests

Toute nouvelle fonctionnalité doit inclure des tests:

```kotlin
class ThreatDetectorTest {
    
    @Test
    fun `should detect high threat when multiple unknown faces present`() = runTest {
        // Arrange
        val sensorData = SensorData(
            faceCount = 3,
            unknownFaceCount = 3,
            closestDistance = 30f
        )
        
        // Act
        val result = threatDetector.assessThreat(sensorData, testContext)
        
        // Assert
        assertThat(result.threatLevel).isEqualTo(ThreatLevel.HIGH)
        assertThat(result.score).isGreaterThan(70)
    }
    
    @Test
    fun `should not trigger on owner alone`() = runTest {
        val sensorData = SensorData(
            faceCount = 1,
            unknownFaceCount = 0,
            isOwnerPresent = true
        )
        
        val result = threatDetector.assessThreat(sensorData, testContext)
        
        assertThat(result.threatLevel).isEqualTo(ThreatLevel.NONE)
    }
}
```

### 3. Soumettre une Pull Request

```bash
# 1. Assurez-vous que votre code est à jour
git fetch upstream
git rebase upstream/main

# 2. Push votre branche
git push origin feature/ma-nouvelle-fonctionnalite

# 3. Créer une PR sur GitHub
```

#### Template de PR

```markdown
## Description
[Décrivez vos changements]

## Type de changement
- [ ] Bug fix
- [ ] Nouvelle fonctionnalité
- [ ] Breaking change
- [ ] Documentation

## Tests
- [ ] Tests unitaires ajoutés
- [ ] Tests UI ajoutés
- [ ] Testé sur device physique
- [ ] Testé sur émulateur

## Checklist
- [ ] Mon code suit les conventions du projet
- [ ] J'ai commenté le code complexe
- [ ] J'ai mis à jour la documentation
- [ ] Aucun warning de linter
- [ ] Tous les tests passent
- [ ] J'ai ajouté des tests pour mes changements

## Screenshots (si applicable)
[Ajoutez des captures d'écran]

## Issue liée
Closes #[numéro]
```

### 4. Review Process

Les mainteneurs vont:
1. Vérifier la qualité du code
2. Tester les changements
3. Demander des modifications si nécessaire
4. Merger une fois approuvé

**Soyez patient et réceptif aux feedbacks!**

## 🔒 Sécurité et Confidentialité

### Guidelines Strictes

1. **JAMAIS stocker d'images/audio brutes**
   ```kotlin
   // ❌ INTERDIT
   fun saveImage(image: Bitmap) {
       image.compress(...)
   }
   
   // ✅ OK
   fun processImage(image: Bitmap): FaceDetectionResult {
       val result = detector.detect(image)
       image.recycle() // Libérer immédiatement
       return result
   }
   ```

2. **Chiffrer toutes données sensibles**
   ```kotlin
   // ✅ OK
   fun storeTrustedFace(face: TrustedFace) {
       val encrypted = encryptionManager.encrypt(face.encoding)
       database.insert(face.copy(encoding = encrypted))
   }
   ```

3. **Minimiser les permissions**
   - Ne demander que ce qui est strictement nécessaire
   - Expliquer clairement l'usage

4. **Traitement local uniquement**
   - Aucun serveur backend pour données utilisateur
   - Pas de télémétrie sans opt-in explicite

### Audit de Sécurité

Avant de merger, vérifier:
- [ ] Aucune fuite de données sensibles
- [ ] Chiffrement approprié
- [ ] Logs ne contiennent pas d'infos personnelles
- [ ] Permissions justifiées

## 🐛 Signaler des Bugs

### Bugs de Sécurité

**NE PAS créer d'issue publique!**

Envoyez un email à: security@privacyguard.app

Incluez:
- Description de la vulnérabilité
- Steps to reproduce
- Impact potentiel
- Votre environnement (device, Android version)

### Bugs Normaux

Créez une issue avec:

```markdown
## Description du Bug
[Description claire]

## Steps to Reproduce
1. Ouvrir l'app
2. Aller dans paramètres
3. Cliquer sur X
4. Bug se produit

## Comportement Attendu
[Ce qui devrait se passer]

## Comportement Actuel
[Ce qui se passe réellement]

## Environnement
- Device: [ex: Pixel 6]
- Android Version: [ex: 13]
- App Version: [ex: 1.2.0]

## Logs
```
[Coller les logs si disponibles]
```

## Screenshots
[Ajouter captures d'écran]
```

## 🌍 Traductions

Nous cherchons des traducteurs pour:
- Français
- Espagnol
- Allemand
- Chinois
- Arabe
- Et plus!

### Processus

1. Copier `res/values/strings.xml`
2. Créer `res/values-XX/strings.xml` (XX = code langue)
3. Traduire toutes les strings
4. Tester dans l'app
5. Soumettre PR

**Important:** Conserver les placeholders `%s`, `%d`, etc.

## 🎨 Design & UI

### Principes

1. **Material Design 3**
2. **Accessibilité** (contraste, taille texte)
3. **Dark mode support**
4. **Animations fluides (< 300ms)**

### Proposer un Design

1. Créer des mockups (Figma, Sketch)
2. Ouvrir une issue avec designs
3. Discuter avec l'équipe
4. Implémenter après approbation

## 📚 Documentation

### Types de Documentation

1. **Code comments** (pour code complexe)
2. **KDoc** (pour API publiques)
3. **README** (pour setup)
4. **Markdown docs** (pour architecture, guides)

### Améliorer la Documentation

Les PRs de documentation sont très appréciées!

- Corriger typos
- Clarifier sections confuses
- Ajouter exemples
- Traduire

## ⚖️ Licence

En contribuant, vous acceptez que vos contributions soient sous la même licence que le projet (voir [LICENSE](./LICENSE)).

## 🙏 Remerciements

Merci à tous les contributeurs!

### Hall of Fame

<!-- Sera rempli automatiquement -->

## 📞 Contact

- **Issues GitHub**: Pour bugs et features
- **Discussions**: Pour questions générales
- **Email**: contribute@privacyguard.app
- **Discord**: [Lien à venir]

## 📖 Ressources

- [Architecture Documentation](./ARCHITECTURE.md)
- [Coding Standards](./docs/coding-standards.md)
- [Testing Guide](./docs/testing-guide.md)
- [Release Process](./docs/release-process.md)

---

**Encore une fois, merci de contribuer à Privacy Guard!** 🎉

