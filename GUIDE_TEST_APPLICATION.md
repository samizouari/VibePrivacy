# Guide de Test de l'Application Privacy Guard

## 🎯 Deux Types de Tests

### 1. Tests Unitaires (Ce qu'on vient de faire)
- **Quoi** : Tester le CODE (fonctions, logique)
- **Où** : Sur votre PC (JVM)
- **Commande** : `.\gradlew test`
- **Rapide** : Quelques secondes

### 2. Tests de l'Application Complète (Ce que vous demandez)
- **Quoi** : Tester l'APPLICATION entière
- **Où** : Sur votre téléphone physique
- **Comment** : Installer l'APK et tester manuellement
- **Plus lent** : Quelques minutes

---

## 📱 Comment Tester l'Application sur Votre Téléphone

### Étape 1 : Préparer Votre Téléphone

#### Activer le Mode Développeur
1. **Paramètres** → **À propos du téléphone**
2. Tapez 7 fois sur **"Numéro de build"**
3. Message : "Vous êtes maintenant développeur"

#### Activer le Débogage USB
1. **Paramètres** → **Système** → **Options pour les développeurs**
2. Activer **"Débogage USB"**
3. Brancher votre téléphone au PC (câble USB)
4. Sur le téléphone, autoriser le débogage (pop-up)

#### Vérifier la Connexion
```powershell
# Dans le terminal (à la racine du projet)
adb devices
```

**Résultat attendu** :
```
List of devices attached
XXXXXXXX    device    <-- Votre téléphone
```

---

### Étape 2 : Installer et Lancer l'App

#### Méthode A : Android Studio (Recommandé)

1. **Ouvrir Android Studio**
2. Vérifier que votre téléphone apparaît en haut (à côté du bouton ▶️)
3. Cliquer sur le bouton **▶️ Run 'app'** (ou Shift+F10)
4. Android Studio va :
   - Compiler l'application
   - Installer l'APK sur votre téléphone
   - Lancer l'application automatiquement

#### Méthode B : Ligne de Commande

```powershell
# Compiler et installer
.\gradlew installDebug

# Lancer l'application
adb shell am start -n com.privacyguard/.ui.MainActivity
```

---

### Étape 3 : Observer les Logs en Temps Réel

#### Dans Android Studio (Logcat)

1. Onglet **"Logcat"** en bas
2. Filtrer par package : `com.privacyguard`
3. Vous verrez tous les logs de l'application

**Logs importants** :
```
CameraSensor: Started successfully
AudioSensor: Started successfully
MotionSensor: Started successfully
ProximitySensor: Started successfully
```

#### En Ligne de Commande

```powershell
# Voir tous les logs de Privacy Guard
adb logcat | Select-String "privacyguard"

# Voir uniquement les logs d'erreur
adb logcat *:E | Select-String "privacyguard"

# Voir les logs des capteurs
adb logcat | Select-String "Sensor"
```

---

## ✅ Scénarios de Test Manuel

### Test 1 : Démarrage de l'Application ✅

**Actions** :
1. Ouvrir l'app
2. Vérifier que l'écran principal s'affiche
3. Vérifier le bouton "Démarrer la protection"

**Résultat attendu** :
- ✅ App s'ouvre sans crash
- ✅ UI s'affiche correctement
- ✅ Bouton interactif

---

### Test 2 : Permissions ✅

**Actions** :
1. Première ouverture → écran de permissions doit apparaître
2. Cliquer sur "Autoriser" pour chaque permission
3. Accorder les permissions :
   - 📹 Caméra
   - 🎤 Microphone
   - 📍 Localisation

**Résultat attendu** :
- ✅ Écran de permissions s'affiche
- ✅ Android demande les permissions
- ✅ Après autorisation, retour à l'écran principal

**Vérifier dans Logcat** :
```
PermissionManager: All critical permissions granted
```

---

### Test 3 : Démarrer la Protection ✅

**Actions** :
1. Cliquer sur **"Démarrer la protection"**
2. Observer la notification persistante
3. Observer les logs

**Résultat attendu** :
- ✅ Notification "Privacy Guard actif" apparaît
- ✅ Bouton devient "Arrêter la protection"
- ✅ Service démarre

**Vérifier dans Logcat** :
```
PrivacyGuardService: Starting protection...
SensorManager: Initializing all sensors...
CameraSensor: Started successfully
AudioSensor: Started successfully
MotionSensor: Started successfully
ProximitySensor: Started successfully
```

---

### Test 4 : Capteur Caméra 📹

**Actions** :
1. Protection activée
2. Mettre votre visage devant la caméra frontale
3. Observer les logs

**Résultat attendu** :
```
CameraSensor: Faces detected: 1
CameraSensor: Faces looking at screen: 1
CameraSensor: Threat level: MEDIUM, confidence: 0.6
```

**Tester différents scénarios** :
- ✅ Aucun visage : `NONE`
- ✅ 1 visage loin : `LOW`
- ✅ 1 visage proche : `MEDIUM` ou `HIGH`
- ✅ Plusieurs visages : `HIGH` ou `CRITICAL`

---

### Test 5 : Capteur Audio 🎤

**Actions** :
1. Protection activée
2. Parler près du téléphone
3. Observer les logs

**Résultat attendu** :
```
AudioSensor: Audio level: 52.3 dB
AudioSensor: Threat level: MEDIUM (speech detected)
```

**Tester différents scénarios** :
- ✅ Silence : `NONE`
- ✅ Bruit léger : `LOW`
- ✅ Parole : `MEDIUM`
- ✅ Bruit fort : `HIGH`

---

### Test 6 : Capteur Mouvement 📱

**Actions** :
1. Protection activée
2. Secouer légèrement le téléphone
3. Observer les logs

**Résultat attendu** :
```
MotionSensor: Acceleration magnitude: 12.5 m/s²
MotionSensor: Threat level: MEDIUM
```

**Tester différents scénarios** :
- ✅ Téléphone immobile : `NONE`
- ✅ Mouvement léger : `LOW`
- ✅ Mouvement modéré : `MEDIUM`
- ✅ Secousse : `HIGH`

---

### Test 7 : Capteur Proximité 🤏

**Actions** :
1. Protection activée
2. Passer votre main devant la caméra (en haut de l'écran)
3. Observer les logs

**Résultat attendu** :
```
ProximitySensor: Distance=0.0cm, isNear=true, threat=HIGH
ProximitySensor: Distance=5.0cm, isNear=false, threat=NONE
```

**Note** : Le capteur de proximité est souvent binaire (0cm ou maxRange).

---

### Test 8 : Prévisualisation Caméra avec Détection 🎥

**Actions** :
1. Sur l'écran principal, observer le cadre de débogage
2. Vous devriez voir :
   - ✅ Flux caméra en direct
   - ✅ Cadres verts autour des visages détectés

**Résultat attendu** :
- ✅ Caméra fonctionne
- ✅ ML Kit détecte les visages
- ✅ Cadres verts affichés en temps réel

---

### Test 9 : Arrêter la Protection 🛑

**Actions** :
1. Cliquer sur **"Arrêter la protection"**
2. Observer les logs

**Résultat attendu** :
```
PrivacyGuardService: Stopping protection...
SensorManager: Stopping all sensors...
CameraSensor: Stopped
AudioSensor: Stopped
MotionSensor: Stopped
ProximitySensor: Stopped
```

- ✅ Notification disparaît
- ✅ Bouton redevient "Démarrer la protection"
- ✅ Tous les capteurs s'arrêtent

---

## 🐛 Si l'Application Crash

### Voir le Crash dans Logcat

```powershell
# Afficher uniquement les erreurs
adb logcat *:E

# Chercher "Exception" ou "Error"
adb logcat | Select-String "Exception"
```

**Exemple de crash** :
```
E/AndroidRuntime: FATAL EXCEPTION: main
    java.lang.RuntimeException: Unable to start service
    at com.privacyguard.service.PrivacyGuardService.onCreate
```

### Réinstaller l'Application Proprement

```powershell
# Désinstaller
adb uninstall com.privacyguard

# Réinstaller
.\gradlew installDebug

# Ou dans Android Studio : Run 'app'
```

---

## 📊 Checklist de Test Complet

### Fonctionnalités de Base
- [ ] App s'installe sans erreur
- [ ] App s'ouvre sans crash
- [ ] UI s'affiche correctement
- [ ] Permissions peuvent être accordées

### Protection
- [ ] Bouton "Démarrer" fonctionne
- [ ] Notification persistante apparaît
- [ ] Service démarre sans crash
- [ ] Tous les 4 capteurs démarrent

### Capteurs Individuels
- [ ] Caméra détecte les visages
- [ ] Audio détecte le son
- [ ] Mouvement détecte les secousses
- [ ] Proximité détecte les objets

### Interface de Débogage
- [ ] Prévisualisation caméra fonctionne
- [ ] Cadres verts autour des visages
- [ ] Pas de lag ou freeze

### Arrêt Propre
- [ ] Bouton "Arrêter" fonctionne
- [ ] Service s'arrête proprement
- [ ] Notification disparaît
- [ ] Capteurs s'arrêtent

---

## 🎯 Quand Tester l'Application ?

### Pendant le Développement (Maintenant)
- ✅ Tester après chaque grosse feature
- ✅ Vérifier que ça ne crash pas
- ✅ Observer les logs des capteurs

### À la Fin du Jour 2 (Aujourd'hui)
- ✅ Test complet de tous les capteurs
- ✅ Vérifier que tout fonctionne ensemble

### Avant Chaque Commit Important
- ✅ Build réussit
- ✅ App s'installe
- ✅ Pas de crash évident

### Avant le Rendu Final (Jour 7)
- ✅ Tests E2E complets
- ✅ Tous les scénarios testés
- ✅ Préparation démo

---

## 🚀 Commandes Utiles

### Build et Installation
```powershell
# Compiler l'app
.\gradlew assembleDebug

# Installer sur device
.\gradlew installDebug

# Compiler + Installer + Lancer
# Dans Android Studio : Shift+F10 ou bouton ▶️
```

### Logs
```powershell
# Tous les logs Privacy Guard
adb logcat | Select-String "privacyguard"

# Logs capteurs uniquement
adb logcat | Select-String "Sensor"

# Logs avec timestamp
adb logcat -v time | Select-String "privacyguard"

# Nettoyer les logs
adb logcat -c
```

### Device
```powershell
# Lister devices connectés
adb devices

# Redémarrer adb
adb kill-server
adb start-server

# Screenshot du device
adb shell screencap -p /sdcard/screen.png
adb pull /sdcard/screen.png
```

### App
```powershell
# Lancer l'app
adb shell am start -n com.privacyguard/.ui.MainActivity

# Forcer l'arrêt
adb shell am force-stop com.privacyguard

# Désinstaller
adb uninstall com.privacyguard
```

---

## 📱 État Actuel de l'Application

D'après nos développements Jour 1 et Jour 2 :

### ✅ Ce qui fonctionne :
- ✅ Interface principale avec bouton toggle
- ✅ Système de permissions
- ✅ Foreground Service
- ✅ Notification persistante
- ✅ CameraSensor avec ML Kit (détection visages)
- ✅ AudioSensor (niveau sonore)
- ✅ MotionSensor (accéléromètre)
- ✅ ProximitySensor (détection proximité)
- ✅ SensorManager (orchestration)
- ✅ Prévisualisation caméra avec cadres verts

### ⏳ Pas encore implémenté (Jour 3+) :
- ⏳ ThreatAssessmentEngine (fusion capteurs)
- ⏳ Scoring de menace global
- ⏳ Actions de protection (flou, écran leurre)
- ⏳ Dashboard
- ⏳ Capture intrus

**Donc aujourd'hui, vous pouvez tester** :
- Démarrage/arrêt de la protection
- Logs de chaque capteur individuellement
- Prévisualisation caméra avec détection

---

## 🎬 Prochaines Étapes

1. **Maintenant** : Tester l'app sur votre téléphone avec ce guide
2. **Jour 3** : Implémenter la fusion des capteurs
3. **Jour 4** : Ajouter les actions de protection
4. **Jour 7** : Tests E2E complets pour la démo

---

**Dernière mise à jour** : Jour 2 (15 novembre 2024)


