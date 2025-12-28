# Guide de Test - Privacy Guard

## 📋 Types de Tests

### 1. Tests Unitaires (Unit Tests)
- **Localisation** : `app/src/test/java/`
- **Exécution** : JVM local (pas besoin de device)
- **Rapides** : Quelques secondes
- **Exemple** : `CameraSensorTest.kt`, `AudioSensorTest.kt`

### 2. Tests Instrumentés (Instrumented Tests)
- **Localisation** : `app/src/androidTest/java/`
- **Exécution** : Device Android ou émulateur nécessaire
- **Plus lents** : Quelques minutes
- **Exemple** : Tests UI, tests d'intégration

---

## 🧪 Exécuter les Tests Unitaires

### Option 1 : Android Studio (Interface Graphique)

#### Méthode A : Exécuter TOUS les tests
1. Dans l'arborescence du projet (à gauche), naviguer vers :
   ```
   app/src/test/java/com/privacyguard/sensors/
   ```

2. **Clic droit** sur le dossier `sensors/`

3. Sélectionner : **"Run 'Tests in 'sensors''"** (icône ▶️ verte)

4. Android Studio va :
   - Compiler les tests
   - Exécuter tous les tests du dossier
   - Afficher les résultats dans l'onglet "Run" en bas

#### Méthode B : Exécuter UN SEUL fichier de test
1. Ouvrir le fichier (ex: `CameraSensorTest.kt`)

2. **Clic droit** dans le fichier

3. Sélectionner : **"Run 'CameraSensorTest'"**

#### Méthode C : Exécuter UN SEUL test
1. Ouvrir le fichier de test

2. Localiser une fonction de test (ex: `fun test no faces detected returns NONE threat level()`)

3. Cliquer sur l'icône ▶️ verte à gauche du numéro de ligne

4. Sélectionner **"Run 'test no faces...'"**

---

### Option 2 : Ligne de Commande (Gradle)

#### Exécuter TOUS les tests unitaires
```bash
# Windows (PowerShell)
.\gradlew test

# Ou avec plus de détails
.\gradlew test --info
```

#### Exécuter tests d'un module spécifique
```bash
.\gradlew :app:test
```

#### Exécuter tests d'une classe spécifique
```bash
.\gradlew test --tests CameraSensorTest
```

#### Exécuter UN SEUL test
```bash
.\gradlew test --tests CameraSensorTest."test no faces detected returns NONE threat level"
```

---

## 📊 Interpréter les Résultats

### Dans Android Studio

#### Onglet "Run" (en bas)

**Résultats possibles** :
- ✅ **Test passed** (vert) : Le test a réussi
- ❌ **Test failed** (rouge) : Le test a échoué
- ⚠️ **Test ignored** (jaune) : Test ignoré (annotation `@Ignore`)

**Structure d'affichage** :
```
▼ com.privacyguard.sensors
  ▼ CameraSensorTest
    ✅ test no faces detected returns NONE threat level (12ms)
    ✅ test single face far away returns LOW threat level (8ms)
    ✅ test single face close looking at screen returns MEDIUM threat level (10ms)
    ✅ test multiple faces return HIGH or CRITICAL threat level (15ms)
    ✅ test face not looking at screen returns lower threat level (9ms)
  ▼ AudioSensorTest
    ✅ test low audio level returns NONE threat level (5ms)
    ✅ test moderate audio level returns LOW threat level (6ms)
    ...
```

#### En cas d'échec ❌

Android Studio affiche :
1. **Message d'erreur** : Pourquoi le test a échoué
2. **Expected vs Actual** : Valeur attendue vs valeur obtenue
3. **Stack trace** : Où l'erreur s'est produite

**Exemple** :
```
❌ test single face close looking at screen returns MEDIUM threat level

Expected: ThreatLevel.MEDIUM
Actual: ThreatLevel.HIGH

at CameraSensorTest.test single face close looking at screen returns MEDIUM threat level(CameraSensorTest.kt:45)
```

---

### En Ligne de Commande

#### Sortie Console

```bash
> Task :app:test

CameraSensorTest > test no faces detected returns NONE threat level PASSED
CameraSensorTest > test single face far away returns LOW threat level PASSED
CameraSensorTest > test single face close looking at screen returns MEDIUM threat level PASSED
...

BUILD SUCCESSFUL in 8s
```

#### Rapport HTML Détaillé

Après exécution, Gradle génère un rapport HTML :

**Chemin** : `app/build/reports/tests/testDebugUnitTest/index.html`

**Ouvrir le rapport** :
```bash
# Windows
start app/build/reports/tests/testDebugUnitTest/index.html

# Ou ouvrir manuellement dans le navigateur
```

**Contenu du rapport** :
- Nombre total de tests
- Nombre de tests réussis/échoués
- Temps d'exécution
- Détails par classe et par test
- Messages d'erreur complets

---

## 🔍 Vérifier la Couverture de Code

### Activer la couverture dans Android Studio

1. **Clic droit** sur le dossier `sensors/`

2. Sélectionner : **"Run 'Tests in 'sensors'' with Coverage"** (icône ▶️ avec bouclier)

3. Android Studio affiche :
   - Pourcentage de couverture par classe
   - Lignes couvertes en **vert**
   - Lignes non couvertes en **rouge**

### Rapport de couverture

**Localisation** : `app/build/reports/coverage/`

---

## 🐛 Debugging des Tests

### Exécuter en mode Debug

1. **Clic droit** sur un test

2. Sélectionner : **"Debug 'CameraSensorTest'"** (icône 🐞)

3. Ajouter des **breakpoints** :
   - Cliquer dans la marge gauche (cercle rouge)
   - Le test s'arrêtera à ce point

4. Utiliser les contrôles de debug :
   - **Step Over** (F8) : Ligne suivante
   - **Step Into** (F7) : Entrer dans la fonction
   - **Resume** (F9) : Continuer jusqu'au prochain breakpoint

---

## 📝 Commandes Utiles

### Nettoyer et reconstruire
```bash
.\gradlew clean test
```

### Voir les tests qui ont échoué uniquement
```bash
.\gradlew test --continue
```

### Exécuter avec stack traces complètes
```bash
.\gradlew test --stacktrace
```

### Forcer la réexécution (ignorer le cache)
```bash
.\gradlew test --rerun-tasks
```

---

## ✅ Checklist Avant de Commit

- [ ] Tous les tests passent en local (`.\gradlew test`)
- [ ] Pas de tests ignorés sans raison
- [ ] Nouveaux tests ajoutés pour nouveau code
- [ ] Couverture de code acceptable (>80% pour logique métier)
- [ ] Tests rapides (< 1 seconde par test unitaire)

---

## 🎯 Tests Actuels du Projet

### Tests Unitaires Créés (Jour 2)

| Fichier | Tests | Description |
|---------|-------|-------------|
| `CameraSensorTest.kt` | 5 tests | Évaluation menace visages |
| `AudioSensorTest.kt` | 5 tests | Détection audio et parole |
| `MotionSensorTest.kt` | 5 tests | Détection mouvement |
| `ProximitySensorTest.kt` | 6 tests | Détection proximité |

**Total** : 21 tests unitaires

### Exécuter tous nos tests
```bash
# Ligne de commande
.\gradlew test

# Ou dans Android Studio
Clic droit sur app/src/test/java/com/privacyguard/ -> Run 'Tests in 'privacyguard''
```

---

## 🚨 Problèmes Courants

### Problème 1 : "No tests found"
- **Cause** : Fichier pas dans le bon dossier
- **Solution** : Vérifier que les tests sont dans `src/test/java/`

### Problème 2 : "Unresolved reference"
- **Cause** : Dépendance de test manquante
- **Solution** : Vérifier `build.gradle.kts` (testImplementation)

### Problème 3 : Tests très lents
- **Cause** : Tests instrumentés au lieu d'unitaires
- **Solution** : Vérifier qu'ils sont dans `src/test/` et pas `src/androidTest/`

### Problème 4 : "Class not found"
- **Cause** : Gradle cache
- **Solution** : 
  ```bash
  .\gradlew clean
  .\gradlew test
  ```

---

## 📚 Ressources

- [Documentation Tests Android](https://developer.android.com/training/testing)
- [JUnit 4 Documentation](https://junit.org/junit4/)
- [Testing Best Practices](https://developer.android.com/training/testing/fundamentals)

---

**Dernière mise à jour** : Jour 2 (14 novembre 2024)

