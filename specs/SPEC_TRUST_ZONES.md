# SPEC - Zones de Confiance

> **Version cible** : 1.5  
> **Priorité** : 🔴 Haute  
> **Complexité** : Moyenne  
> **Durée estimée** : 1 semaine

---

## 🎯 Objectif

Permettre à l'utilisateur de définir des **zones géographiques de confiance** (maison, bureau) où la protection est automatiquement désactivée ou réduite.

---

## 📋 User Stories

### US-1 : Définir une zone de confiance GPS
```
En tant qu'utilisateur,
Je veux définir ma maison comme zone de confiance,
Afin que l'app ne me protège pas quand je suis chez moi.
```

**Critères d'acceptation** :
- [ ] L'utilisateur peut définir un point GPS
- [ ] Rayon configurable (50m - 500m)
- [ ] Nom personnalisable (Maison, Bureau, etc.)
- [ ] Activation/désactivation par zone
- [ ] Max 10 zones de confiance

### US-2 : Zone de confiance WiFi
```
En tant qu'utilisateur,
Je veux associer mon WiFi domestique comme zone de confiance,
Afin d'économiser la batterie (pas de GPS).
```

**Critères d'acceptation** :
- [ ] Détection par SSID WiFi
- [ ] Option complémentaire au GPS
- [ ] Plusieurs WiFi par zone possible
- [ ] Fonctionne même si GPS désactivé

### US-3 : Comportement en zone de confiance
```
En tant qu'utilisateur,
Je veux configurer le niveau de protection dans chaque zone,
Afin d'adapter la protection selon l'endroit.
```

**Options** :
- [ ] Protection désactivée complètement
- [ ] Protection minimale (mode TRUST_ZONE)
- [ ] Protection normale mais pas d'overlay
- [ ] Capteurs spécifiques désactivés

---

## 🏗️ Architecture Technique

### Composants

```
┌─────────────────────────────────────────────────────┐
│              TrustZonesManager                       │
│  - addZone()                                         │
│  - removeZone()                                      │
│  - isInTrustZone()                                  │
│  - getCurrentZone()                                  │
└─────────────────────────────────────────────────────┘
                          │
┌──────────────┬──────────────┬──────────────────────┐
│ GeoFencing   │ WiFiDetector │ ZonesDatabase        │
│ Location API │ WifiManager  │ Room + Encrypted     │
└──────────────┴──────────────┴──────────────────────┘
```

### Nouveaux Fichiers

```
app/src/main/java/com/privacyguard/
├── trust/
│   ├── TrustZonesManager.kt       # Gestionnaire principal
│   ├── GeoFenceMonitor.kt         # Monitoring GPS
│   ├── WiFiZoneDetector.kt        # Détection WiFi
│   └── models/
│       └── TrustZone.kt           # Entity Room
├── data/
│   └── dao/
│       └── TrustZoneDao.kt        # DAO
└── ui/
    └── zones/
        ├── TrustZonesScreen.kt    # Liste zones
        ├── AddZoneScreen.kt       # Ajout zone
        └── ZoneMapPicker.kt       # Sélecteur carte
```

---

## 🔧 Implémentation

### 1. TrustZone.kt (Entity)

```kotlin
@Entity(tableName = "trust_zones")
data class TrustZone(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String,                     // "Maison", "Bureau"
    
    // Localisation GPS
    val latitude: Double?,
    val longitude: Double?,
    val radiusMeters: Int = 100,          // Rayon en mètres
    
    // WiFi associés
    @ColumnInfo(name = "wifi_ssids")
    val wifiSsids: List<String> = emptyList(),
    
    // Comportement
    val protectionLevel: TrustZoneProtection = TrustZoneProtection.DISABLED,
    
    // État
    val isEnabled: Boolean = true,
    val createdAt: Long,
    val lastEnteredAt: Long? = null,
    val totalTimeInZone: Long = 0         // Stats
)

enum class TrustZoneProtection {
    DISABLED,           // Pas de protection
    MINIMAL,            // Mode TRUST_ZONE (seuil 95%)
    NO_OVERLAY,         // Protection sans overlay
    CAMERA_ONLY_OFF,    // Tout sauf caméra
    AUDIO_ONLY_OFF      // Tout sauf audio
}
```

### 2. TrustZonesManager.kt

```kotlin
class TrustZonesManager(
    private val context: Context,
    private val trustZoneDao: TrustZoneDao,
    private val geoFenceMonitor: GeoFenceMonitor,
    private val wifiDetector: WiFiZoneDetector
) {
    
    // État actuel
    private val _currentZone = MutableStateFlow<TrustZone?>(null)
    val currentZone: StateFlow<TrustZone?> = _currentZone.asStateFlow()
    
    private val _isInTrustZone = MutableStateFlow(false)
    val isInTrustZone: StateFlow<Boolean> = _isInTrustZone.asStateFlow()
    
    /**
     * Démarre le monitoring des zones
     */
    fun startMonitoring() {
        // Monitoring GPS
        geoFenceMonitor.onZoneEntered = { zoneId ->
            handleZoneEntered(zoneId)
        }
        geoFenceMonitor.onZoneExited = { zoneId ->
            handleZoneExited(zoneId)
        }
        
        // Monitoring WiFi
        wifiDetector.onWifiChanged = { ssid ->
            checkWifiZone(ssid)
        }
        
        geoFenceMonitor.start()
        wifiDetector.start()
    }
    
    /**
     * Ajoute une nouvelle zone de confiance
     */
    suspend fun addZone(
        name: String,
        latitude: Double? = null,
        longitude: Double? = null,
        radius: Int = 100,
        wifiSsids: List<String> = emptyList(),
        protection: TrustZoneProtection = TrustZoneProtection.DISABLED
    ): TrustZone {
        val zone = TrustZone(
            name = name,
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radius,
            wifiSsids = wifiSsids,
            protectionLevel = protection,
            createdAt = System.currentTimeMillis()
        )
        
        val id = trustZoneDao.insert(zone)
        
        // Configurer le geofence si GPS
        if (latitude != null && longitude != null) {
            geoFenceMonitor.addGeofence(id, latitude, longitude, radius.toFloat())
        }
        
        return zone.copy(id = id)
    }
    
    /**
     * Vérifie si actuellement dans une zone de confiance
     */
    fun checkCurrentLocation(): TrustZone? {
        // Vérifier d'abord par WiFi (plus rapide, moins de batterie)
        val currentWifi = wifiDetector.getCurrentSsid()
        if (currentWifi != null) {
            val wifiZone = findZoneByWifi(currentWifi)
            if (wifiZone != null) return wifiZone
        }
        
        // Sinon vérifier par GPS
        val currentLocation = geoFenceMonitor.getCurrentLocation()
        if (currentLocation != null) {
            return findZoneByLocation(currentLocation)
        }
        
        return null
    }
    
    /**
     * Retourne le niveau de protection à appliquer
     */
    fun getCurrentProtectionLevel(): TrustZoneProtection {
        return _currentZone.value?.protectionLevel 
            ?: TrustZoneProtection.DISABLED // Protection normale si pas en zone
    }
    
    private fun handleZoneEntered(zoneId: Long) {
        viewModelScope.launch {
            val zone = trustZoneDao.getById(zoneId)
            if (zone?.isEnabled == true) {
                _currentZone.value = zone
                _isInTrustZone.value = true
                
                // Mettre à jour stats
                trustZoneDao.updateLastEntered(zoneId, System.currentTimeMillis())
                
                Timber.i("TrustZones: Entered zone '${zone.name}'")
            }
        }
    }
    
    private fun handleZoneExited(zoneId: Long) {
        if (_currentZone.value?.id == zoneId) {
            _currentZone.value = null
            _isInTrustZone.value = false
            Timber.i("TrustZones: Exited zone")
        }
    }
}
```

### 3. GeoFenceMonitor.kt

```kotlin
class GeoFenceMonitor(private val context: Context) {
    
    private val geofencingClient = LocationServices.getGeofencingClient(context)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    
    var onZoneEntered: ((Long) -> Unit)? = null
    var onZoneExited: ((Long) -> Unit)? = null
    
    /**
     * Ajoute un geofence
     */
    @SuppressLint("MissingPermission")
    fun addGeofence(id: Long, lat: Double, lng: Double, radius: Float) {
        val geofence = Geofence.Builder()
            .setRequestId(id.toString())
            .setCircularRegion(lat, lng, radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or 
                Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .build()
        
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
        
        geofencingClient.addGeofences(request, geofencePendingIntent)
            .addOnSuccessListener { 
                Timber.d("GeoFence added: $id") 
            }
            .addOnFailureListener { e ->
                Timber.e(e, "Failed to add geofence")
            }
    }
    
    /**
     * Supprime un geofence
     */
    fun removeGeofence(id: Long) {
        geofencingClient.removeGeofences(listOf(id.toString()))
    }
    
    /**
     * Récupère la position actuelle
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        return suspendCoroutine { continuation ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    continuation.resume(location)
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        }
    }
}
```

### 4. WiFiZoneDetector.kt

```kotlin
class WiFiZoneDetector(private val context: Context) {
    
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    
    var onWifiChanged: ((String?) -> Unit)? = null
    
    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val ssid = getCurrentSsid()
            onWifiChanged?.invoke(ssid)
        }
    }
    
    fun start() {
        val filter = IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        context.registerReceiver(wifiReceiver, filter)
    }
    
    fun stop() {
        context.unregisterReceiver(wifiReceiver)
    }
    
    /**
     * Récupère le SSID du WiFi actuel
     */
    fun getCurrentSsid(): String? {
        val wifiInfo = wifiManager.connectionInfo
        return wifiInfo?.ssid?.removeSurrounding("\"")
    }
}
```

### 5. Intégration avec ThreatAssessmentEngine

```kotlin
// Dans ThreatAssessmentEngine.kt

class ThreatAssessmentEngine(
    private val sensorDataFusion: SensorDataFusion = SensorDataFusion(),
    private val config: ThreatAssessmentConfig = ThreatAssessmentConfig(),
    private val trustZonesManager: TrustZonesManager? = null  // NOUVEAU
) {
    
    fun processSnapshot(snapshot: SensorDataSnapshot): ThreatAssessment? {
        // Vérifier si dans zone de confiance
        if (trustZonesManager?.isInTrustZone?.value == true) {
            val protection = trustZonesManager.getCurrentProtectionLevel()
            
            when (protection) {
                TrustZoneProtection.DISABLED -> {
                    // Retourner évaluation minimale
                    return ThreatAssessment(
                        timestamp = snapshot.timestamp,
                        threatScore = 0,
                        threatLevel = ThreatLevel.NONE,
                        shouldTriggerProtection = false,
                        // ...
                    )
                }
                TrustZoneProtection.MINIMAL -> {
                    // Appliquer mode TRUST_ZONE (seuil 95%)
                    val adjustedConfig = config.copy(
                        protectionMode = ProtectionMode.TRUST_ZONE
                    )
                    return sensorDataFusion.evaluate(snapshot, adjustedConfig, _context.value)
                }
                // ... autres cas
            }
        }
        
        // Évaluation normale
        return sensorDataFusion.evaluate(snapshot, config, _context.value)
    }
}
```

---

## 🎨 UI/UX

### Écran Liste des Zones

```
┌────────────────────────────────────────┐
│ ← Zones de confiance                   │
├────────────────────────────────────────┤
│                                        │
│  🏠 Maison                     ✓ ON   │
│  📍 GPS: 48.8566° N, 2.3522° E        │
│  📶 WiFi: Livebox-1234                │
│  🛡️ Protection: Désactivée           │
│  ─────────────────────────────────    │
│                                        │
│  🏢 Bureau                     ✓ ON   │
│  📍 GPS: 48.8534° N, 2.3488° E        │
│  📶 WiFi: Corp-WiFi, Corp-Guest       │
│  🛡️ Protection: Minimale             │
│  ─────────────────────────────────    │
│                                        │
│  ☕ Café favori               ✗ OFF   │
│  📍 GPS: 48.8601° N, 2.3376° E        │
│  🛡️ Protection: Sans overlay         │
│  ─────────────────────────────────    │
│                                        │
│  ┌─────────────────────────────────┐  │
│  │     ➕ Ajouter une zone         │  │
│  └─────────────────────────────────┘  │
│                                        │
│  📍 Actuellement: 🏠 Maison           │
│                                        │
└────────────────────────────────────────┘
```

### Écran Ajout de Zone

```
┌────────────────────────────────────────┐
│ ← Nouvelle zone                        │
├────────────────────────────────────────┤
│                                        │
│  Nom de la zone                        │
│  ┌─────────────────────────────────┐  │
│  │ Maison                          │  │
│  └─────────────────────────────────┘  │
│                                        │
│  📍 Position GPS                       │
│  ┌─────────────────────────────────┐  │
│  │  [    Mini carte Google     ]   │  │
│  │  [    avec marker mobile    ]   │  │
│  └─────────────────────────────────┘  │
│  📌 Utiliser ma position actuelle     │
│                                        │
│  Rayon: [====●=========] 150m         │
│                                        │
│  📶 WiFi associés (optionnel)         │
│  ┌─────────────────────────────────┐  │
│  │ ✓ Livebox-1234                  │  │
│  │ ○ Neighbor-WiFi                 │  │
│  │ ○ FreeWifi                      │  │
│  └─────────────────────────────────┘  │
│                                        │
│  🛡️ Niveau de protection              │
│  ● Désactivée                         │
│  ○ Minimale (seuil 95%)              │
│  ○ Sans overlay                       │
│  ○ Caméra désactivée seulement       │
│                                        │
│  ┌─────────────────────────────────┐  │
│  │         ✓ Enregistrer           │  │
│  └─────────────────────────────────┘  │
│                                        │
└────────────────────────────────────────┘
```

---

## 🔒 Permissions Requises

```xml
<!-- AndroidManifest.xml -->

<!-- Localisation -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />

<!-- WiFi -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
```

---

## 📦 Dépendances

```kotlin
// Google Play Services Location
implementation("com.google.android.gms:play-services-location:21.0.1")

// Maps pour UI (optionnel)
implementation("com.google.maps.android:maps-compose:2.11.4")
implementation("com.google.android.gms:play-services-maps:18.2.0")
```

---

## 🧪 Tests

```kotlin
@Test
fun `should detect entry into GPS zone`() {
    val zone = TrustZone(
        name = "Test",
        latitude = 48.8566,
        longitude = 2.3522,
        radiusMeters = 100
    )
    
    val insideLocation = Location("test").apply {
        latitude = 48.8566
        longitude = 2.3522
    }
    
    assertTrue(zone.contains(insideLocation))
}

@Test
fun `should detect WiFi zone without GPS`() {
    val zone = TrustZone(
        name = "Office",
        wifiSsids = listOf("Corp-WiFi")
    )
    
    whenever(wifiDetector.getCurrentSsid()).thenReturn("Corp-WiFi")
    
    val detected = trustZonesManager.checkCurrentLocation()
    assertEquals(zone, detected)
}
```

---

## 📊 Impact Batterie

| Méthode | Consommation | Précision |
|---------|-------------|-----------|
| GPS continu | Haute | Très haute |
| Geofencing | Basse | Haute |
| WiFi seul | Très basse | Moyenne |
| Hybride (recommandé) | Basse | Haute |

**Stratégie recommandée** : WiFi en priorité, Geofencing en backup.

---

**Dernière mise à jour** : 7 décembre 2025

