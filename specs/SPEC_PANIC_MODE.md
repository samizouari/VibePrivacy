# SPEC - Mode Panique

> **Version cible** : 2.5  
> **Priorité** : 🟡 Moyenne  
> **Complexité** : Moyenne  
> **Durée estimée** : 1 semaine

---

## 🎯 Objectif

Permettre à l'utilisateur de déclencher rapidement des **actions de protection d'urgence** lorsqu'il se trouve dans une situation de danger.

---

## 📋 User Stories

### US-1 : Déclenchement rapide
```
En tant qu'utilisateur en danger,
Je veux pouvoir déclencher le mode panique très rapidement,
Sans avoir besoin de déverrouiller mon téléphone.
```

**Critères d'acceptation** :
- [ ] Déclenchement en moins de 2 secondes
- [ ] Fonctionne écran verrouillé
- [ ] Plusieurs méthodes de déclenchement
- [ ] Confirmation optionnelle (éviter faux positifs)

### US-2 : Actions automatiques
```
En tant qu'utilisateur ayant déclenché le mode panique,
Je veux que des actions de protection s'exécutent automatiquement,
Pour me protéger même si je ne peux plus utiliser mon téléphone.
```

**Critères d'acceptation** :
- [ ] Actions configurables
- [ ] Exécution silencieuse (pas d'alerte sonore)
- [ ] Capture photo discrète
- [ ] Option d'envoi de SOS

---

## 🔧 Déclencheurs Disponibles

### 1. Bouton Power (5 pressions)
```kotlin
/**
 * Détecte 5 pressions rapides sur le bouton Power
 */
class PowerButtonTrigger(private val context: Context) {
    
    private var pressCount = 0
    private var lastPressTime = 0L
    private val maxInterval = 500L // 500ms entre chaque pression
    private val requiredPresses = 5
    
    fun onScreenStateChanged(screenOn: Boolean) {
        val now = System.currentTimeMillis()
        
        if (now - lastPressTime > maxInterval) {
            pressCount = 0
        }
        
        pressCount++
        lastPressTime = now
        
        if (pressCount >= requiredPresses) {
            triggerPanic()
            pressCount = 0
        }
    }
    
    private fun triggerPanic() {
        PanicHandler.getInstance(context).triggerPanic()
    }
}
```

### 2. Secousse du téléphone
```kotlin
/**
 * Détecte une secousse violente (shake)
 */
class ShakeTrigger(private val context: Context) : SensorEventListener {
    
    private val shakeThreshold = 12.0f
    private val shakeDurationMs = 500L
    private var shakeStart = 0L
    
    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        
        val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
        
        if (acceleration > shakeThreshold) {
            if (shakeStart == 0L) {
                shakeStart = System.currentTimeMillis()
            } else if (System.currentTimeMillis() - shakeStart > shakeDurationMs) {
                triggerPanic()
                shakeStart = 0L
            }
        } else {
            shakeStart = 0L
        }
    }
}
```

### 3. Widget discret
```kotlin
/**
 * Widget qui ressemble à une app innocente mais déclenche le panic
 */
class PanicWidget : AppWidgetProvider() {
    
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, 
                          appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_panic)
            
            // Apparence innocente (ex: icône météo, calculatrice)
            views.setImageViewResource(R.id.widget_icon, R.drawable.ic_calculator)
            
            // Mais déclenche le panic au clic
            val intent = Intent(context, PanicTriggerReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 
                PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
```

### 4. Volume buttons combo
```kotlin
/**
 * Détecte Volume Up + Volume Down maintenus ensemble
 */
class VolumeButtonsTrigger {
    
    private var volumeUpPressed = false
    private var volumeDownPressed = false
    private var bothPressedTime = 0L
    private val holdDuration = 3000L // 3 secondes
    
    fun onKeyDown(keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> volumeUpPressed = true
            KeyEvent.KEYCODE_VOLUME_DOWN -> volumeDownPressed = true
        }
        
        if (volumeUpPressed && volumeDownPressed) {
            if (bothPressedTime == 0L) {
                bothPressedTime = System.currentTimeMillis()
            } else if (System.currentTimeMillis() - bothPressedTime > holdDuration) {
                triggerPanic()
                return true
            }
        }
        return false
    }
    
    fun onKeyUp(keyCode: Int) {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> volumeUpPressed = false
            KeyEvent.KEYCODE_VOLUME_DOWN -> volumeDownPressed = false
        }
        bothPressedTime = 0L
    }
}
```

---

## 🚨 Actions de Panique

### Configuration

```kotlin
data class PanicActions(
    // Protection immédiate
    val lockScreen: Boolean = true,
    val showDecoyScreen: Boolean = true,
    
    // Capture preuves
    val capturePhoto: Boolean = true,
    val recordAudio: Boolean = false,  // Légalité variable
    val saveLocation: Boolean = true,
    
    // Alerte contacts
    val sendSosMessage: Boolean = false,
    val sosContacts: List<EmergencyContact> = emptyList(),
    val includeLocation: Boolean = true,
    
    // Données
    val hideApp: Boolean = false,
    val deletePhotos: Boolean = false,
    val deleteTrustedFaces: Boolean = false,
    
    // Avancé
    val fakeShutdown: Boolean = false,  // Simule extinction
    val disableFingerprint: Boolean = false
)

data class EmergencyContact(
    val name: String,
    val phone: String,
    val email: String? = null
)
```

### Séquence d'Exécution

```kotlin
class PanicExecutor(
    private val config: PanicActions,
    private val context: Context
) {
    
    /**
     * Exécute les actions de panique dans l'ordre
     */
    suspend fun execute() {
        // PHASE 1: Protection immédiate (< 500ms)
        if (config.lockScreen) {
            lockDeviceScreen()
        }
        if (config.showDecoyScreen) {
            showInnocentScreen()
        }
        
        // PHASE 2: Capture preuves (async)
        launch {
            if (config.capturePhoto) {
                captureDiscreetPhoto()
            }
            if (config.saveLocation) {
                saveCurrentLocation()
            }
        }
        
        // PHASE 3: Alertes (async)
        launch {
            if (config.sendSosMessage) {
                sendSosToContacts()
            }
        }
        
        // PHASE 4: Nettoyage (si configuré)
        launch {
            if (config.deletePhotos) {
                deleteIntruderPhotos()
            }
            if (config.deleteTrustedFaces) {
                deleteTrustedFacesData()
            }
        }
    }
    
    /**
     * Capture photo sans flash ni son
     */
    private suspend fun captureDiscreetPhoto() {
        // Désactiver le son de l'appareil
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM)
        audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
        
        // Capturer avec caméra frontale
        val photo = cameraSensor.captureStillPhoto()
        
        // Sauvegarder avec metadata
        intruderCapture.captureFromBitmap(photo, "PANIC_EVIDENCE")
        
        // Restaurer le son
        audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, originalVolume, 0)
    }
    
    /**
     * Envoie SOS aux contacts d'urgence
     */
    private fun sendSosToContacts() {
        val location = if (config.includeLocation) {
            getCurrentLocation()?.let { "https://maps.google.com/?q=${it.latitude},${it.longitude}" }
        } else null
        
        val message = buildString {
            append("🆘 URGENCE - Message automatique de Privacy Guard\n")
            append("${config.sosContacts[0].name} a besoin d'aide.\n")
            if (location != null) {
                append("Position: $location")
            }
        }
        
        config.sosContacts.forEach { contact ->
            sendSms(contact.phone, message)
        }
    }
}
```

---

## 🎨 UI/UX

### Écran Configuration Panic

```
┌────────────────────────────────────────┐
│ ← Mode Panique                   🚨    │
├────────────────────────────────────────┤
│                                        │
│  🔘 Déclencheurs                       │
│  ─────────────────────────────────     │
│  ☑️ 5 pressions bouton Power           │
│  ☐ Secousse violente (shake)          │
│  ☐ Widget déguisé                      │
│  ☐ Volume Up+Down (3 sec)             │
│                                        │
│  🛡️ Actions immédiates                │
│  ─────────────────────────────────     │
│  ☑️ Verrouiller l'écran                │
│  ☑️ Afficher écran leurre              │
│  ☑️ Capturer photo discrète            │
│  ☐ Sauvegarder position GPS           │
│                                        │
│  📤 Alertes                            │
│  ─────────────────────────────────     │
│  ☐ Envoyer SOS par SMS                │
│                                        │
│  📞 Contacts d'urgence                 │
│  ┌─────────────────────────────────┐  │
│  │ 👤 Maman - 06 12 34 56 78      │  │
│  │ 👤 Papa - 06 98 76 54 32       │  │
│  │ + Ajouter un contact            │  │
│  └─────────────────────────────────┘  │
│                                        │
│  ⚠️ Test du mode panique              │
│  ┌─────────────────────────────────┐  │
│  │     🧪 Tester (sans SOS)        │  │
│  └─────────────────────────────────┘  │
│                                        │
└────────────────────────────────────────┘
```

### Écran Leurre pendant Panic

```
┌────────────────────────────────────────┐
│                                        │
│                                        │
│            ┌─────────┐                │
│            │  ⏰     │                │
│            │ 14:32   │                │
│            └─────────┘                │
│                                        │
│         Lundi 7 décembre              │
│                                        │
│                                        │
│    "Glissez vers le haut pour         │
│         déverrouiller"                │
│                                        │
│                                        │
│    ──────────────────────             │
│                                        │
│    📱          📷          📞         │
│                                        │
└────────────────────────────────────────┘

[Ressemble à un écran verrouillé normal]
[Aucun indice de Privacy Guard]
```

---

## 🔒 Sécurité et Confidentialité

### Photos Panic

- Stockées séparément des photos intrus normales
- Tag "PANIC_EVIDENCE" pour identification
- Timestamp et location GPS inclus
- Chiffrement AES-256

### Messages SOS

- Ne contiennent pas le nom de l'app
- Message neutre "Besoin d'aide"
- Localisation GPS (si autorisé)
- Pas de mention de "menace" ou "attaquant"

---

## 📦 Permissions

```xml
<!-- Pour détection Power button -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- Pour SMS -->
<uses-permission android:name="android.permission.SEND_SMS" />

<!-- Pour verrouillage -->
<uses-permission android:name="android.permission.DISABLE_KEYGUARD" />
```

---

## 🧪 Tests

### Test Mode Panic (sans SOS)

```kotlin
@Test
fun `panic mode should lock screen within 500ms`() {
    val startTime = System.currentTimeMillis()
    
    panicHandler.triggerPanic()
    
    val elapsed = System.currentTimeMillis() - startTime
    assertTrue(elapsed < 500)
    assertTrue(devicePolicyManager.isDeviceLocked())
}

@Test
fun `panic photo should be captured without sound`() {
    // Vérifier que le son est à 0 pendant la capture
}

@Test  
fun `5 power presses should trigger panic`() {
    repeat(5) {
        powerButtonTrigger.onScreenStateChanged(true)
        delay(100)
        powerButtonTrigger.onScreenStateChanged(false)
        delay(100)
    }
    
    verify(panicHandler).triggerPanic()
}
```

---

## ⚖️ Considérations Légales

- **SMS** : Informer les contacts d'urgence qu'ils sont enregistrés
- **Enregistrement audio** : Légalité variable selon pays
- **Photos** : Usage privé uniquement, pas de diffusion
- **Localisation** : Consentement préalable

---

**Dernière mise à jour** : 7 décembre 2025

