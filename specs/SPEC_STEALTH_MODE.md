# SPEC - Mode Stealth (Furtif)

> **Version cible** : 2.5  
> **Priorité** : 🟡 Moyenne  
> **Complexité** : Très élevée  
> **Durée estimée** : 2 semaines

---

## 🎯 Objectif

Rendre Privacy Guard **complètement invisible** et indétectable par un attaquant potentiel qui aurait accès physique au téléphone.

---

## ⚠️ Avertissement Légal

> **IMPORTANT** : Les fonctionnalités de mode Stealth doivent être utilisées de manière responsable et légale. L'application ne doit pas être utilisée pour des activités illégales. Les utilisateurs sont responsables de l'utilisation conforme aux lois locales.

---

## 📋 User Stories

### US-1 : Cacher l'application
```
En tant qu'utilisateur dans une situation à risque,
Je veux pouvoir cacher complètement l'application,
Afin qu'un attaquant ne sache pas que j'ai une protection.
```

**Critères d'acceptation** :
- [ ] L'icône disparaît du launcher
- [ ] L'app n'apparaît pas dans les apps récentes
- [ ] L'app n'apparaît pas dans Paramètres > Applications
- [ ] Accès via code secret (dialer ou calculatrice)

### US-2 : Mode Panic
```
En tant qu'utilisateur menacé,
Je veux déclencher un mode panique rapidement,
Afin de protéger mes données immédiatement.
```

**Critères d'acceptation** :
- [ ] Déclenchement par geste (5 taps power, secousse, etc.)
- [ ] Verrouillage instantané de l'écran
- [ ] Capture photo discrète de l'attaquant
- [ ] Envoi localisation à contacts d'urgence (optionnel)
- [ ] Suppression données sensibles (optionnel)

### US-3 : Duress Password
```
En tant qu'utilisateur forcé de déverrouiller mon téléphone,
Je veux avoir un code qui semble déverrouiller normalement,
Mais qui cache mes vraies données.
```

**Critères d'acceptation** :
- [ ] Code alternatif qui ouvre une "session leurre"
- [ ] Session leurre avec fausses apps/données
- [ ] Le vrai contenu reste caché et protégé
- [ ] Aucun indice que ce n'est pas le vrai contenu

---

## 🏗️ Architecture Technique

### Composants Stealth

```
┌─────────────────────────────────────────────────────┐
│                 StealthManager                       │
│  - hideApp()                                         │
│  - showApp()                                         │
│  - triggerPanic()                                    │
│  - setupDuressPassword()                            │
└─────────────────────────────────────────────────────┘
                          │
┌──────────────┬──────────────┬──────────────────────┐
│ AppHider     │ PanicHandler │ DuressManager        │
│ Component    │ Emergency    │ Fake Session         │
│ Disabler     │ Actions      │ Manager              │
└──────────────┴──────────────┴──────────────────────┘
```

### Nouveaux Fichiers

```
app/src/main/java/com/privacyguard/
├── stealth/
│   ├── StealthManager.kt          # Gestionnaire principal
│   ├── AppHider.kt                # Cache l'application
│   ├── SecretAccessManager.kt     # Accès par code secret
│   ├── PanicHandler.kt            # Mode panique
│   └── DuressManager.kt           # Gestion duress password
├── ui/
│   └── stealth/
│       ├── StealthConfigScreen.kt
│       ├── PanicConfigScreen.kt
│       └── DuressSetupScreen.kt
└── receiver/
    ├── SecretCodeReceiver.kt      # Écoute codes secrets
    └── PanicTriggerReceiver.kt    # Écoute triggers panic
```

---

## 🔧 Implémentation

### 1. AppHider.kt - Cacher l'Application

```kotlin
/**
 * Cache l'application du launcher et des paramètres
 * 
 * ATTENTION: Certaines techniques nécessitent root ou Device Admin
 * La version sans root utilise des techniques limitées
 */
class AppHider(private val context: Context) {
    
    /**
     * Cache l'icône du launcher
     * Technique: Désactiver le composant Activity principal
     */
    fun hideFromLauncher() {
        val packageManager = context.packageManager
        val componentName = ComponentName(
            context,
            "${context.packageName}.ui.LauncherActivity"  // Alias pour MainActivity
        )
        
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        
        Timber.i("StealthMode: App hidden from launcher")
    }
    
    /**
     * Affiche l'icône du launcher
     */
    fun showInLauncher() {
        val packageManager = context.packageManager
        val componentName = ComponentName(
            context,
            "${context.packageName}.ui.LauncherActivity"
        )
        
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        
        Timber.i("StealthMode: App shown in launcher")
    }
    
    /**
     * Cache l'app des apps récentes
     */
    fun hideFromRecents(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.setTaskDescription(
                ActivityManager.TaskDescription(
                    "",  // Nom vide
                    null, // Pas d'icône
                    0     // Couleur transparente
                )
            )
        }
        
        // Exclure des recents
        val intent = activity.intent
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    }
}
```

### 2. SecretAccessManager.kt - Accès par Code Secret

```kotlin
/**
 * Gère l'accès secret à l'application cachée
 * 
 * Méthodes d'accès:
 * 1. Code dialer: *#*#PRIVACY#*#* (ouvre l'app)
 * 2. Calculatrice: Taper "31415926=" lance l'app
 * 3. Gesture: Pattern spécifique sur écran verrouillé
 */
class SecretAccessManager(private val context: Context) {
    
    companion object {
        // Code secret par défaut (personnalisable)
        const val DEFAULT_DIALER_CODE = "*#*#7749289#*#*" // PRIVACY
        const val DEFAULT_CALC_CODE = "31415926"          // Pi
    }
    
    /**
     * Enregistre le code secret dialer
     */
    fun registerDialerCode(code: String = DEFAULT_DIALER_CODE) {
        // Le code est enregistré dans le manifest via intent-filter
        // ou via setSecretCode si Device Admin
        
        saveSecretCode(code)
    }
    
    /**
     * Vérifie si un code calculatrice correspond
     */
    fun checkCalculatorCode(input: String): Boolean {
        val savedCode = getSecretCode()
        return input == savedCode
    }
    
    /**
     * Lance l'application de manière discrète
     */
    fun launchAppDiscreetly() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }
        context.startActivity(intent)
    }
    
    private fun saveSecretCode(code: String) {
        // Stocker de manière sécurisée (EncryptedSharedPreferences)
    }
    
    private fun getSecretCode(): String {
        // Récupérer le code sauvegardé
        return DEFAULT_CALC_CODE
    }
}

/**
 * BroadcastReceiver pour le code dialer
 */
class SecretCodeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SECRET_CODE") {
            // Lancer l'app
            SecretAccessManager(context).launchAppDiscreetly()
        }
    }
}
```

### 3. PanicHandler.kt - Mode Panique

```kotlin
/**
 * Gère le mode panique
 * 
 * Déclencheurs possibles:
 * - 5 pressions rapides sur Power
 * - Secousse violente du téléphone
 * - Widget discret
 * - Mot-clé vocal (si micro actif)
 */
class PanicHandler(
    private val context: Context,
    private val intruderCapture: IntruderCapture,
    private val overlayManager: OverlayManager
) {
    
    data class PanicConfig(
        val lockScreen: Boolean = true,
        val capturePhoto: Boolean = true,
        val sendLocation: Boolean = false,
        val emergencyContacts: List<String> = emptyList(),
        val deleteData: Boolean = false,
        val dataToDelete: List<DataType> = emptyList()
    )
    
    enum class DataType {
        INTRUDER_PHOTOS,
        TRUSTED_FACES,
        TRUST_ZONES,
        ALL_APP_DATA
    }
    
    private var config = PanicConfig()
    
    /**
     * Déclenche le mode panique
     */
    suspend fun triggerPanic() {
        Timber.w("PANIC MODE TRIGGERED!")
        
        // 1. Verrouiller l'écran immédiatement
        if (config.lockScreen) {
            lockScreen()
        }
        
        // 2. Capturer photo discrète
        if (config.capturePhoto) {
            captureDiscreetPhoto()
        }
        
        // 3. Envoyer localisation aux contacts d'urgence
        if (config.sendLocation && config.emergencyContacts.isNotEmpty()) {
            sendEmergencyLocation()
        }
        
        // 4. Supprimer données sensibles (si configuré)
        if (config.deleteData) {
            deleteSelectedData()
        }
        
        // 5. Afficher écran leurre innocent
        overlayManager.showPanicDecoy()
    }
    
    /**
     * Verrouille l'écran
     */
    private fun lockScreen() {
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) 
            as DevicePolicyManager
        
        if (devicePolicyManager.isAdminActive(adminComponent)) {
            devicePolicyManager.lockNow()
        } else {
            // Alternative: Afficher overlay opaque
            overlayManager.showLockScreen()
        }
    }
    
    /**
     * Capture photo discrète (sans flash, sans son)
     */
    private suspend fun captureDiscreetPhoto() {
        // Utiliser la caméra frontale
        // Désactiver le son de capture
        // Sauvegarder avec chiffrement
        intruderCapture.captureFromBitmap(
            getCameraFrame(),
            "PANIC"
        )
    }
    
    /**
     * Envoie la localisation aux contacts d'urgence
     */
    private fun sendEmergencyLocation() {
        val location = getLastKnownLocation()
        val message = "URGENCE: Je suis à $location. Besoin d'aide."
        
        config.emergencyContacts.forEach { contact ->
            sendSms(contact, message)
        }
    }
    
    /**
     * Supprime les données sélectionnées
     */
    private suspend fun deleteSelectedData() {
        config.dataToDelete.forEach { type ->
            when (type) {
                DataType.INTRUDER_PHOTOS -> intruderCapture.deleteAllPhotos()
                DataType.TRUSTED_FACES -> trustFacesManager.deleteAll()
                DataType.TRUST_ZONES -> trustZonesManager.deleteAll()
                DataType.ALL_APP_DATA -> deleteAllAppData()
            }
        }
    }
}
```

### 4. DuressManager.kt - Mot de Passe sous Contrainte

```kotlin
/**
 * Gère le "duress password" (mot de passe sous contrainte)
 * 
 * Concept: Un second mot de passe qui déverrouille une "fausse" session
 * avec du contenu innocent, pendant que le vrai contenu reste caché.
 */
class DuressManager(private val context: Context) {
    
    /**
     * Vérifie si le code entré est le code duress
     */
    fun isDuressCode(code: String): Boolean {
        val savedDuressCode = getSavedDuressCode()
        return code == savedDuressCode
    }
    
    /**
     * Active la session leurre
     */
    fun activateDuressMode() {
        // 1. Masquer la vraie app
        hideRealApp()
        
        // 2. Afficher contenu innocent
        showDecoyContent()
        
        // 3. Capturer photo discrète de l'attaquant
        captureAttackerPhoto()
        
        // 4. Logger l'événement (pour analyse ultérieure)
        logDuressEvent()
        
        Timber.w("DURESS MODE ACTIVATED - Attacker detected")
    }
    
    /**
     * Configure le contenu leurre
     */
    fun setupDecoyContent(config: DecoyConfig) {
        // Photos innocentes
        // Contacts limités
        // Messages neutres
        // Pas d'apps sensibles
    }
    
    data class DecoyConfig(
        val showPhotos: Boolean = true,
        val limitedContacts: List<String> = emptyList(),
        val fakeMessages: List<FakeMessage> = emptyList(),
        val hiddenApps: List<String> = emptyList()
    )
}
```

---

## 🎨 UI/UX

### Écran Configuration Stealth

```
┌────────────────────────────────────────┐
│ ← Mode Furtif                    🔒    │
├────────────────────────────────────────┤
│                                        │
│  ⚠️ Mode Stealth                       │
│  ┌─────────────────────────────────┐  │
│  │ Cacher l'application     [OFF] │  │
│  └─────────────────────────────────┘  │
│  L'app disparaîtra du launcher.       │
│  Accès via code secret: *#*#PRIVACY   │
│                                        │
│  🚨 Mode Panique                       │
│  ┌─────────────────────────────────┐  │
│  │ Activer Mode Panique     [ON]  │  │
│  └─────────────────────────────────┘  │
│  Trigger: 5 pressions bouton Power    │
│                                        │
│  Actions en cas de panique:           │
│  ☑️ Verrouiller l'écran               │
│  ☑️ Capturer photo de l'attaquant     │
│  ☐ Envoyer position GPS               │
│  ☐ Supprimer données sensibles        │
│                                        │
│  🔐 Mot de passe sous contrainte      │
│  ┌─────────────────────────────────┐  │
│  │ Configurer code duress    →    │  │
│  └─────────────────────────────────┘  │
│  Code alternatif qui ouvre une        │
│  session leurre avec faux contenu.    │
│                                        │
│  📞 Contacts d'urgence                │
│  ┌─────────────────────────────────┐  │
│  │ + Ajouter un contact            │  │
│  └─────────────────────────────────┘  │
│                                        │
└────────────────────────────────────────┘
```

---

## 🔒 Considérations de Sécurité

### Limitations Techniques

| Fonctionnalité | Sans Root | Avec Root |
|----------------|-----------|-----------|
| Cacher du launcher | ✅ Partiel | ✅ Complet |
| Cacher de Paramètres | ❌ Non | ✅ Oui |
| Code dialer secret | ⚠️ Limité | ✅ Complet |
| Verrouillage Device Admin | ✅ Oui | ✅ Oui |
| Wipe données | ✅ App only | ✅ Complet |

### Risques

- **Détection par expertise** : Un forensicien peut toujours trouver l'app
- **Backup Google** : L'app peut apparaître dans les backups
- **Play Store** : L'historique d'installation reste
- **Legal** : Utilisation à des fins illégales prohibée

---

## 📦 Permissions Requises

```xml
<!-- Device Admin pour verrouillage -->
<uses-permission android:name="android.permission.BIND_DEVICE_ADMIN" />

<!-- SMS pour contacts d'urgence -->
<uses-permission android:name="android.permission.SEND_SMS" />

<!-- Localisation pour urgence -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

<!-- Recevoir codes secrets -->
<receiver android:name=".receiver.SecretCodeReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.provider.Telephony.SECRET_CODE" />
        <data android:scheme="android_secret_code" 
              android:host="7749289" />
    </intent-filter>
</receiver>
```

---

## 🚀 Plan d'Implémentation

### Phase 1 : App Hiding (4 jours)
- [ ] AppHider avec component disabling
- [ ] SecretAccessManager avec code dialer
- [ ] UI de configuration

### Phase 2 : Panic Mode (4 jours)
- [ ] PanicHandler avec triggers
- [ ] Capture photo discrète
- [ ] Contacts d'urgence

### Phase 3 : Duress (4 jours)
- [ ] DuressManager
- [ ] Session leurre basique
- [ ] Tests de sécurité

### Phase 4 : Polish (2 jours)
- [ ] Documentation
- [ ] Tests edge cases
- [ ] Disclaimer légal

---

**Dernière mise à jour** : 7 décembre 2025

