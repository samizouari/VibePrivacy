# Fonctionnalités Détaillées

## 🛡️ Modes de Protection

### 1. Mode Paranoïa (Maximum Security)

**Caractéristiques :**
- Sensibilité maximale à tous les capteurs
- Détection de moindre mouvement autour du téléphone
- Exclusion uniquement du propriétaire (via face recognition)
- Pas besoin de regarder l'écran, juste être dans le champ de vision déclenche l'alerte
- Timeout de réactivation : 3 secondes d'immobilité totale requises

**Seuils de déclenchement :**
```kotlin
object ParanoiaMode {
    const val THREAT_THRESHOLD = 20 // Sur 100
    const val FACE_DETECTION_DISTANCE = 200 // cm
    const val UNKNOWN_FACE_TOLERANCE = 0 // Aucune tolérance
    const val AUDIO_SENSITIVITY = 1.0f // Maximum
    const val MOTION_SENSITIVITY = 0.8f
    const val REACTIVATION_DELAY = 3000L // ms
}
```

**Scénarios de déclenchement :**
- Visage inconnu détecté dans un rayon de 2m
- Tout mouvement du téléphone non initié par l'utilisateur
- Son de pas qui s'approchent
- Changement de luminosité (ombre)
- Voix détectées dans l'environnement

**Action par défaut :** Niveau 3 (Verrouillage Instantané)

### 2. Mode Équilibré (Recommandé)

**Caractéristiques :**
- Balance entre sécurité et ergonomie
- Détection de menaces réelles vs faux positifs
- Délai de grâce : 0.5 secondes avant masquage
- Apprentissage des patterns de faux positifs

**Seuils de déclenchement :**
```kotlin
object BalancedMode {
    const val THREAT_THRESHOLD = 50
    const val FACE_DETECTION_DISTANCE = 100 // cm
    const val UNKNOWN_FACE_TOLERANCE = 1 // 1 visage ok si de passage
    const val AUDIO_SENSITIVITY = 0.6f
    const val MOTION_SENSITIVITY = 0.5f
    const val GRACE_PERIOD = 500L // ms
}
```

**Scénarios de déclenchement :**
- 2+ visages inconnus détectés ET proche (< 1m)
- Mouvement brusque du téléphone (quelqu'un l'attrape)
- Combinaison audio + visuel (voix + visage)
- Mots-clés suspects détectés
- Occultation rapide du capteur de proximité

**Action par défaut :** Niveau 2 (Écran Leurre)

### 3. Mode Discret (Minimum)

**Caractéristiques :**
- Détection uniquement des menaces directes
- Pas de verrouillage, juste floutage progressif
- Minimal impact sur batterie
- Restauration automatique rapide

**Seuils de déclenchement :**
```kotlin
object DiscreteMode {
    const val THREAT_THRESHOLD = 75
    const val FACE_DETECTION_DISTANCE = 50 // cm
    const val UNKNOWN_FACE_TOLERANCE = 3
    const val AUDIO_SENSITIVITY = 0.3f
    const val MOTION_SENSITIVITY = 0.3f
    const val AUTO_RESTORE_DELAY = 2000L // ms
}
```

**Scénarios de déclenchement :**
- Visage inconnu TRÈS proche (< 50cm)
- Quelqu'un prend physiquement le téléphone
- Téléphone retourné face cachée

**Action par défaut :** Niveau 1 (Masquage Doux)

### 4. Mode Zones de Confiance

**Caractéristiques :**
- Adaptation automatique selon la localisation
- Apprentissage des lieux fréquents
- Géofencing intelligent
- Transition douce entre zones

**Configuration des zones :**
```kotlin
data class TrustZoneConfig(
    val home: ZoneSettings(
        autoDisable = true,
        radius = 50f // mètres
    ),
    val work: ZoneSettings(
        mode = ProtectionMode.BALANCED,
        radius = 100f
    ),
    val publicTransport: ZoneSettings(
        mode = ProtectionMode.PARANOIA,
        autoDetect = true // Via patterns de mouvement + GPS
    ),
    val publicPlaces: ZoneSettings(
        mode = ProtectionMode.BALANCED,
        radius = 200f
    )
)
```

**Détection automatique de contexte :**
- Maison : GPS + WiFi SSID + historique
- Bureau : Heures régulières + localisation
- Transport : Vitesse + patterns de mouvement
- Café/Restaurant : GPS + durée stationnaire

## 🎭 Actions de Protection

### Niveau 1 : Masquage Doux

**Implémentation visuelle :**
```kotlin
class SoftMaskingEffect {
    fun apply(view: View) {
        // Animation de flou gaussien
        ValueAnimator.ofFloat(0f, 25f).apply {
            duration = 300 // ms
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val blurRadius = animator.animatedValue as Float
                view.setRenderEffect(
                    RenderEffect.createBlurEffect(
                        blurRadius,
                        blurRadius,
                        Shader.TileMode.CLAMP
                    )
                )
            }
            // Overlay semi-transparent
            addListener(onEnd = {
                overlayView.alpha = 0.3f
                overlayView.visibility = View.VISIBLE
            })
        }.start()
    }
}
```

**Restauration :**
- Double-tap sur l'écran
- Face ID du propriétaire
- Geste personnalisé (ex: swipe en Z)
- Timeout automatique si menace disparue (2s)

**Paramètres ajustables :**
- Intensité du flou (0-50)
- Vitesse de transition (100-1000ms)
- Opacité de l'overlay (0-80%)
- Type de flou (gaussien, motion, zoom)

### Niveau 2 : Écran Leurre

**Types de contenu leurre :**

#### 1. Liste de Courses (par défaut)
```kotlin
val defaultShoppingList = listOf(
    "Lait",
    "Œufs",
    "Pain",
    "Tomates",
    "Fromage",
    "Pâtes"
)
```

#### 2. Article Wikipedia Aléatoire
- Base de données embarquée de 1000 articles
- Sélection aléatoire mais cohérente
- Scroll automatique lent pour naturalité

#### 3. Météo
- Météo réelle de la localisation actuelle
- Prévisions sur 7 jours
- Aspect standard d'app météo

#### 4. Notes de Travail Génériques
```kotlin
val genericWorkNotes = """
Réunion équipe - Jeudi 14h00
- Préparer présentation Q3
- Review budget marketing
- Planning sprints prochains

TODO:
- Envoyer rapport mensuel
- Appeler fournisseur
- Vérifier commande matériel
""".trimIndent()
```

#### 5. Contenu Personnalisé
- L'utilisateur peut enregistrer ses propres écrans leurres
- Screenshots d'apps innocentes
- Pages web pré-configurées
- Possibilité d'avoir plusieurs leurres qui alternent

**Transition animation :**
```kotlin
class DecoyTransition {
    fun animate(fromView: View, toView: View) {
        // Effet de "switch app" naturel
        fromView.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(200)
            .withEndAction {
                toView.alpha = 0f
                toView.visibility = View.VISIBLE
                toView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }
}
```

**Interaction avec l'écran leurre :**
- Scrolling fonctionnel
- Tap ne fait rien (ou action innocente)
- Geste secret pour restaurer (ex: tap 3 fois dans coin)
- Biométrie pour restauration

### Niveau 3 : Verrouillage Instantané

**Actions exécutées :**
```kotlin
class InstantLockProtection : ProtectionAction {
    override suspend fun execute(context: Context) {
        // 1. Écran noir immédiat
        screenManager.blackout(instant = true)
        
        // 2. Verrouillage système
        devicePolicyManager.lockNow()
        
        // 3. Notification discrète
        notificationManager.show(
            title = "Privacy Guard",
            text = "Écran verrouillé",
            silent = true,
            priority = PRIORITY_LOW
        )
        
        // 4. Bloquer screenshots
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        
        // 5. Désactiver Recent Apps preview
        activityManager.excludeFromRecents()
        
        // 6. Requiert biométrie
        biometricPrompt.authenticate(
            promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Déverrouiller Privacy Guard")
                .setSubtitle("Authentification requise")
                .setNegativeButtonText("Annuler")
                .build()
        )
    }
}
```

**Protection supplémentaire :**
- Screenshot automatiquement flouté dans Recent Apps
- Partage de contenu désactivé temporairement
- Presse-papier vidé
- Timeout d'inactivité après déverrouillage (10s)

### Niveau 4 : Mode Panique

**Actions critiques :**
```kotlin
class PanicModeProtection : ProtectionAction {
    override suspend fun execute(context: Context) {
        coroutineScope {
            // Tout en parallèle pour rapidité maximale
            launch { appManager.forceStopCurrentApp() }
            launch { memoryManager.clearSensitiveData() }
            launch { clipboardManager.clear() }
            launch { recentAppsManager.clearEntry(currentApp) }
            launch { notificationManager.clearAll() }
            launch { navigationManager.goToHomeScreen() }
            
            // Optionnel : capture de l'intrus
            if (settings.captureIntruder) {
                launch { 
                    cameraManager.takeQuickPhoto(
                        savePath = secureStorage.intruderPhotosPath
                    )
                }
            }
        }
        
        // Log de l'incident
        securityLogger.logPanicEvent(
            timestamp = System.currentTimeMillis(),
            triggerReason = threatAssessment.currentThreat,
            context = locationMonitor.currentContext
        )
    }
}
```

**Conditions de déclenchement :**
- Utilisateur active manuellement (bouton panique)
- Combinaison de menaces extrêmes :
  - 3+ visages inconnus ET très proches
  - Téléphone arraché des mains
  - Mots-clés d'urgence détectés ("police", "contrôle")
- Pattern de saisie du téléphone par force

## 🎨 Personnalisation par Application

### Configuration Granulaire

```kotlin
data class AppProtectionProfile(
    val packageName: String,
    val displayName: String,
    val sensitivityLevel: SensitivityLevel,
    val protectionLevel: ProtectionLevel,
    val customThreshold: Int? = null,
    val allowedFaces: List<Long> = emptyList(), // IDs de TrustedFace
    val customActions: List<ProtectionAction> = emptyList(),
    val excludedZones: List<Long> = emptyList() // IDs de TrustZone
)

enum class SensitivityLevel(val description: String) {
    CRITICAL("Banking, messages privés - Réaction instantanée"),
    SENSITIVE("Photos, emails - Protection modérée"),
    NORMAL("Réseaux sociaux - Protection légère"),
    PUBLIC("Météo, actualités - Pas de protection")
}
```

### Profils Pré-définis

```kotlin
object AppProfiles {
    val BANKING = AppProtectionProfile(
        sensitivityLevel = SensitivityLevel.CRITICAL,
        protectionLevel = ProtectionLevel.INSTANT_LOCK,
        customThreshold = 15 // Très sensible
    )
    
    val MESSAGING = AppProtectionProfile(
        sensitivityLevel = SensitivityLevel.CRITICAL,
        protectionLevel = ProtectionLevel.DECOY_SCREEN,
        customThreshold = 25
    )
    
    val PHOTOS = AppProtectionProfile(
        sensitivityLevel = SensitivityLevel.SENSITIVE,
        protectionLevel = ProtectionLevel.SOFT_BLUR,
        customThreshold = 40
    )
    
    val SOCIAL_MEDIA = AppProtectionProfile(
        sensitivityLevel = SensitivityLevel.NORMAL,
        protectionLevel = ProtectionLevel.SOFT_BLUR,
        customThreshold = 60
    )
    
    val PUBLIC = AppProtectionProfile(
        sensitivityLevel = SensitivityLevel.PUBLIC,
        protectionLevel = ProtectionLevel.NONE,
        customThreshold = 100 // Désactivé
    )
}
```

### Auto-détection des Apps Sensibles

```kotlin
class SensitiveAppDetector {
    fun detectSensitiveApps(installedApps: List<ApplicationInfo>): Map<String, SensitivityLevel> {
        return installedApps.associate { app ->
            app.packageName to when {
                // Banking
                app.packageName.contains("bank", ignoreCase = true) ||
                app.packageName.contains("paypal") ||
                app.packageName.contains("wallet") -> SensitivityLevel.CRITICAL
                
                // Messaging
                app.packageName in listOf(
                    "com.whatsapp",
                    "org.telegram.messenger",
                    "com.facebook.orca",
                    "com.snapchat.android",
                    "com.discord"
                ) -> SensitivityLevel.CRITICAL
                
                // Email
                app.packageName.contains("mail") ||
                app.packageName.contains("gmail") -> SensitivityLevel.SENSITIVE
                
                // Photos/Gallery
                app.packageName.contains("gallery") ||
                app.packageName.contains("photos") -> SensitivityLevel.SENSITIVE
                
                // Social Media
                app.packageName in listOf(
                    "com.instagram.android",
                    "com.facebook.katana",
                    "com.twitter.android",
                    "com.linkedin.android"
                ) -> SensitivityLevel.NORMAL
                
                else -> SensitivityLevel.NORMAL
            }
        }
    }
}
```

## 👥 Liste Blanche de Contacts

### Reconnaissance Faciale des Personnes de Confiance

```kotlin
class TrustedFaceManager {
    suspend fun addTrustedFace(name: String, faceImages: List<Bitmap>) {
        // Extraire l'encoding du visage
        val faceEncodings = faceImages.map { image ->
            faceRecognizer.extractEncoding(image)
        }
        
        // Moyenner pour robustesse
        val averageEncoding = faceEncodings.average()
        
        // Sauvegarder
        trustedFaceDao.insert(
            TrustedFace(
                name = name,
                faceEncoding = averageEncoding.toByteArray(),
                addedTimestamp = System.currentTimeMillis(),
                lastSeenTimestamp = 0L,
                verificationCount = 0
            )
        )
    }
    
    suspend fun verifyFace(detectedFaceEncoding: FloatArray): TrustedFace? {
        val trustedFaces = trustedFaceDao.getAll()
        
        return trustedFaces.firstOrNull { trusted ->
            val similarity = cosineSimilarity(
                detectedFaceEncoding,
                trusted.faceEncoding.toFloatArray()
            )
            similarity > TRUST_THRESHOLD // 0.85
        }?.also { face ->
            // Mettre à jour les stats
            trustedFaceDao.update(
                face.copy(
                    lastSeenTimestamp = System.currentTimeMillis(),
                    verificationCount = face.verificationCount + 1
                )
            )
        }
    }
}
```

### Processus d'Ajout

**UI Flow :**
1. Utilisateur va dans Paramètres → Visages de Confiance
2. Tap sur "Ajouter une personne"
3. Guide d'enregistrement :
   - Prise de 5-10 photos différentes
   - Angles variés (face, profil, etc.)
   - Conditions d'éclairage diverses
   - Expressions faciales variées
4. Processus ML d'extraction des features
5. Demande de nom/relation
6. Confirmation et sauvegarde chiffrée

**Apprentissage Progressif :**
```kotlin
class FaceRecognitionTrainer {
    suspend fun improveRecognition(trustedFaceId: Long) {
        // Lors de confirmations utilisateur, améliorer le modèle
        val recentDetections = detectionEventDao.getRecentFaceDetections(
            trustedFaceId = trustedFaceId,
            limit = 10
        )
        
        if (recentDetections.size >= 5) {
            // Re-calculer l'encoding moyen avec nouvelles données
            val updatedEncoding = (
                existingEncoding * 0.7f + 
                newDetectionsAverage * 0.3f
            )
            
            trustedFaceDao.updateEncoding(trustedFaceId, updatedEncoding)
        }
    }
}
```

### Scénarios d'Utilisation

**Famille/Conjoint :**
- Pas de masquage si visage reconnu
- Option "Modes partagés" pour certaines activités
- Exceptions temporaires (ex: montrer une photo)

**Collègues de Confiance :**
- Masquage sélectif (certaines apps protégées, d'autres non)
- Logs de qui a vu quoi
- Révocation facile

**Enfants :**
- Protection même contre visages connus selon app
- Profil "Contrôle Parental" intégré
- Notifications aux parents

## 🔄 Modes Spéciaux

### Mode Présentation

```kotlin
class PresentationMode {
    fun activate(duration: Duration) {
        // Désactiver protection temporairement
        privacyGuardService.pause(duration)
        
        // Mais garder surveillance
        val expectedAudienceSize = detectCurrentFaces().size
        
        lifecycleScope.launch {
            while (isActive) {
                delay(5000) // Check toutes les 5s
                
                val currentFaces = detectCurrentFaces().size
                if (currentFaces > expectedAudienceSize + 2) {
                    // Personnes non prévues ont rejoint
                    notificationManager.showAlert(
                        "⚠️ Audience augmentée détectée"
                    )
                }
            }
        }
    }
}
```

### Mode Réunion

```kotlin
class MeetingMode {
    fun autoDetect(): Boolean {
        // Détection via plusieurs signaux
        return (
            calendarManager.hasOngoingMeeting() ||
            (audioAnalyzer.voiceCount >= 3 && locationMonitor.zone == Zone.WORK)
        )
    }
    
    fun applyProtections() {
        // Protéger uniquement les notifications
        notificationManager.filterSensitiveNotifications()
        
        // Si téléphone passé à quelqu'un
        motionDetector.onHandoff = {
            screenManager.lock()
        }
    }
}
```

### Mode Transport

```kotlin
class TransportMode {
    fun autoDetect(): Boolean {
        val speed = locationMonitor.currentSpeed
        val movementPattern = motionDetector.pattern
        
        return when {
            // Train/Métro
            speed in 40..120 && movementPattern == Pattern.RAIL_VIBRATION -> true
            // Bus
            speed in 20..60 && movementPattern == Pattern.ROAD_BUMPS -> true
            // Voiture (passager)
            speed in 30..130 && movementPattern == Pattern.SMOOTH_ROAD -> true
            else -> false
        }
    }
    
    fun applyProtections() {
        // Sensibilité maximale
        setMode(ProtectionMode.PARANOIA)
        
        // Réduire angle de vue (protection shoulder surfing)
        privacyScreenFilter.apply(angle = 45) // degrees
        
        // Détection de regard par-dessus épaule
        gazeEstimator.onSideGaze = { direction ->
            if (direction in listOf(Direction.LEFT, Direction.RIGHT)) {
                protectionExecutor.execute(ProtectionLevel.SOFT_BLUR)
            }
        }
    }
}
```

### Mode Nuit

```kotlin
class NightMode {
    fun activate() {
        // Utiliser principalement luminosité comme indicateur
        sensorWeights = SensorWeights(
            camera = 0.2f,
            audio = 0.2f,
            motion = 0.2f,
            light = 0.4f // Poids augmenté
        )
        
        // Détecter mouvements dans le lit
        motionDetector.sensitivity = HIGH
        motionDetector.onSubtleMovement = {
            // Quelqu'un bouge à côté
            protectionExecutor.execute(ProtectionLevel.SOFT_BLUR)
        }
        
        // Mode très discret (pas de vibration)
        feedbackManager.disableHaptics()
        feedbackManager.disableSounds()
    }
}
```

---

Cette structure de fonctionnalités offre une protection complète et adaptable à tous les scénarios d'utilisation.

