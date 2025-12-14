# 🐛 Guide de Debug - Privacy Guard

## 🔵 Problème 1 : Test Flou ne fonctionne pas

### Symptômes
- L'overlay de flou ne s'affiche pas, ou
- Le double-tap ne fonctionne pas, ou
- L'overlay se ferme immédiatement

### Vérifications

#### 1. Permission Overlay
```bash
# Vérifier si la permission est accordée
adb shell appops get com.privacyguard SYSTEM_ALERT_WINDOW
# Devrait retourner: No operations (permission accordée)
# ou: Mode: allow
```

**Si refusée :**
- Aller dans Paramètres Android
- Apps → Privacy Guard → Permissions avancées
- "Afficher par-dessus d'autres apps" → Activer

#### 2. Logs Temps Réel

**Option A : Logcat natif (si installé)**
```bash
adb logcat -s OverlayManager:* SoftBlurOverlayView:*
```

**Option B : Android Studio**
1. Ouvrir Android Studio
2. Onglet "Logcat" en bas
3. Filtrer : `tag:OverlayManager OR tag:SoftBlurOverlayView`

**Option C : Depuis le device**
1. Activer "Options développeur"
2. "Journaux d'erreur" → Bug report

#### 3. Test Manuel Simplifié

**Dans MainActivity, bouton Test Flou :**
1. Clique sur "🔵 Test Flou"
2. **Attendu :** Écran devient noir/flouté
3. **Voir :** Texte "Protection Activée" au centre
4. **Action :** Double-tap RAPIDE (< 300ms entre les 2 taps)

**Si rien ne s'affiche :**
- Logs attendus :
  ```
  OverlayManager: Blur overlay shown (intensity=0.8)
  SoftBlurOverlayView: Showing overlay
  ```
- Si erreur : `No overlay permission` → Voir point 1

**Si le double-tap ne fonctionne pas :**
- Tap plus rapidement (< 300ms)
- OU utilise 1 doigt (pas 2 doigts différents)
- Logs attendus :
  ```
  SoftBlurOverlayView: Tap detected (delta=250ms)
  SoftBlurOverlayView: Double-tap! Dismissing...
  OverlayManager: Blur overlay hidden
  ```

---

## 🏠 Problème 2 : WiFi Trust Zones

### Logs Système WiFi (Normaux)

Ces logs sont **normaux** et viennent du système Android :
```
WifiVendorHal: getWifiLinkLayerStats_1_3_Internal(l.927) failed {.code = ERROR_NOT_SUPPORTED}
PackageManager: Cannot whitelist unknown permission: android.permission.NEARBY_WIFI_DEVICES
```

**Raisons :**
- `ERROR_NOT_SUPPORTED` : Ton device ne supporte pas les stats WiFi détaillées (normal sur Android 10)
- `NEARBY_WIFI_DEVICES` : Permission Android 13+ non disponible sur ton Android 10

**→ Ces erreurs n'affectent PAS le fonctionnement de Privacy Guard**

### Vérifications Trust Zones

#### 1. Vérifier que le WiFi est connecté

```bash
# Voir le SSID connecté
adb shell dumpsys wifi | grep "mWifiInfo"
```

**Attendu :**
```
mWifiInfo SSID: "TON_SSID", BSSID: xx:xx:xx:xx:xx:xx
```

#### 2. Logs Privacy Guard WiFi

**Filtrer les bons logs :**
```bash
adb logcat | grep -E "WiFiZoneDetector|TrustZonesManager|PrivacyGuard.*trust"
```

**Logs attendus lors de l'ajout d'une zone :**
```
TrustZonesManager: Adding trust zone 'Maison' with 1 WiFi(s)
TrustZonesManager: Trust zone created successfully
```

**Logs attendus en continu (toutes les 5 sec) :**
```
WiFiZoneDetector: Checking WiFi connection...
WiFiZoneDetector: Connected to: "TON_SSID"
TrustZonesManager: Detected zone: Maison (ACTIVE)
PrivacyGuardService: In trust zone - adjusting protection
```

**Si pas de logs "Connected to:" :**
- Le WiFiZoneDetector ne reçoit pas les infos
- Vérifier permissions : `ACCESS_WIFI_STATE`, `ACCESS_NETWORK_STATE`

#### 3. Test Complet Trust Zone

**Étapes :**

1. **Ajouter une zone**
   - Paramètres → Zones de Confiance → Ajouter
   - Nommer "Maison"
   - Le SSID actuel doit apparaître automatiquement
   - Cocher le SSID
   - Protection : PROTECTION_OFF
   - Créer

2. **Vérifier qu'elle est active**
   - Retour sur la liste
   - Zone "Maison" doit avoir une **pastille verte** (●)
   - Status : "Actif"

3. **Tester l'effet**
   - Retour écran principal
   - Regarde la caméra → Indicateur devrait s'adapter
   - Logs :
     ```
     PrivacyGuardService: Trust zone active - using PROTECTION_OFF
     ThreatAssessmentEngine: setTrustZone(true)
     ```

4. **Tester désactivation**
   - Désactive le WiFi du téléphone
   - Attends 10 secondes
   - Logs :
     ```
     WiFiZoneDetector: No WiFi connection
     TrustZonesManager: Not in any trust zone
     PrivacyGuardService: Left trust zone - resuming normal protection
     ```

---

## 🔍 Debug Avancé

### Activer les Logs Verbeux

**Dans le terminal Android Studio :**
```bash
# Tout Privacy Guard
adb logcat -s "com.privacyguard:V"

# Spécifique aux problèmes
adb logcat -s "OverlayManager:V" "SoftBlurOverlayView:V" "WiFiZoneDetector:V" "TrustZonesManager:V"
```

### Vérifier les Permissions App

```bash
adb shell dumpsys package com.privacyguard | grep permission
```

**Attendu :**
```
android.permission.CAMERA: granted=true
android.permission.RECORD_AUDIO: granted=true
android.permission.ACCESS_WIFI_STATE: granted=true
android.permission.ACCESS_NETWORK_STATE: granted=true
SYSTEM_ALERT_WINDOW: mode=allowed
```

### Forcer un Refresh

```bash
# Killer l'app et relancer
adb shell am force-stop com.privacyguard
adb shell am start -n com.privacyguard/.ui.MainActivity
```

---

## ✅ Solutions Rapides

### Test Flou - Ne s'affiche pas
1. ✅ Accorder permission Overlay
2. ✅ Relancer l'app
3. ✅ Appuyer sur bouton "🔵 Test Flou"
4. ✅ Vérifier logs : `OverlayManager: Blur overlay shown`

### Test Flou - Double-tap ne marche pas
1. ✅ Taper PLUS VITE (< 300ms entre taps)
2. ✅ Utiliser 1 seul doigt
3. ✅ Taper au centre de l'écran

### WiFi - Zone pas détectée
1. ✅ Vérifier WiFi connecté : `adb shell dumpsys wifi | grep SSID`
2. ✅ Vérifier logs : `adb logcat | grep WiFiZoneDetector`
3. ✅ Relancer le service : Désactiver/Activer dans l'app
4. ✅ Attendre 5-10 secondes (check toutes les 5s)

---

## 📊 Logs de Référence (Fonctionnement Normal)

### Au démarrage
```
PrivacyGuardService: Starting Privacy Guard Service...
SensorManager: Initializing sensors...
CameraSensor: Initializing camera...
AudioSensor: Initializing audio recorder...
OverlayManager: Initializing overlay manager
WiFiZoneDetector: Starting WiFi detection...
TrustZonesManager: Loaded 1 zone(s) from storage
PrivacyGuardService: Service started successfully
```

### Test Flou
```
MainActivity: Test blur button clicked
OverlayManager: Blur overlay shown (intensity=0.8)
SoftBlurOverlayView: Showing overlay with animation
SoftBlurOverlayView: Animation complete - overlay visible
[User double-tap]
SoftBlurOverlayView: Tap at 1234567890
SoftBlurOverlayView: Tap at 1234568120 (delta=230ms)
SoftBlurOverlayView: Double-tap detected! Dismissing...
OverlayManager: Blur overlay hidden
MainActivity: Test overlay dismissed
```

### WiFi Trust Zone
```
WiFiZoneDetector: Checking WiFi connection...
WiFiZoneDetector: Connected to: "MaBox-WiFi"
TrustZonesManager: Checking zone for SSID: MaBox-WiFi
TrustZonesManager: Match found - Zone: Maison
TrustZonesManager: Emitting zone: Maison (PROTECTION_OFF)
PrivacyGuardService: Trust zone activated: Maison
ThreatAssessmentEngine: setTrustZone(true)
```

---

## 🆘 Si Rien ne Marche

1. **Désinstaller l'app complètement**
   ```bash
   adb uninstall com.privacyguard
   ```

2. **Réinstaller**
   ```bash
   ./gradlew installDebug
   ```

3. **Accorder TOUTES les permissions**
   - Caméra ✅
   - Micro ✅
   - Overlay ✅
   - WiFi State ✅

4. **Capture d'écran + Logs**
   - Screenshot du problème
   - Copier les logs complets :
     ```bash
     adb logcat -d > logs.txt
     ```

---

**Dis-moi ce que tu vois maintenant avec ces infos !** 🔍

