# SPEC - Visages de Confiance

> **Version cible** : 1.5  
> **Priorité** : 🔴 Haute  
> **Complexité** : Élevée  
> **Durée estimée** : 1-2 semaines

---

## 🎯 Objectif

Permettre à l'utilisateur d'enregistrer des **visages de confiance** (famille, amis proches) pour que l'application ne déclenche pas d'alerte quand ces personnes sont détectées.

---

## 📋 User Stories

### US-1 : Enregistrement d'un visage de confiance
```
En tant qu'utilisateur,
Je veux enregistrer le visage d'une personne de confiance,
Afin que l'app ne me prévienne pas quand cette personne regarde mon écran.
```

**Critères d'acceptation** :
- [ ] L'utilisateur peut prendre 3-5 photos du visage
- [ ] L'app guide pour différents angles (face, profil gauche, profil droit)
- [ ] L'encodage facial est stocké de manière chiffrée
- [ ] L'utilisateur peut nommer la personne (optionnel)
- [ ] Confirmation visuelle que l'enregistrement a réussi

### US-2 : Reconnaissance en temps réel
```
En tant qu'utilisateur,
Je veux que l'app reconnaisse automatiquement mes contacts de confiance,
Afin de ne pas être dérangé par de fausses alertes.
```

**Critères d'acceptation** :
- [ ] Reconnaissance en moins de 200ms
- [ ] Taux de reconnaissance > 95%
- [ ] Pas de faux positifs (personne inconnue reconnue comme connue)
- [ ] Fonctionne avec différentes luminosités
- [ ] Fonctionne avec lunettes/sans lunettes

### US-3 : Gestion des visages enregistrés
```
En tant qu'utilisateur,
Je veux pouvoir voir et supprimer les visages enregistrés,
Afin de gérer ma liste de confiance.
```

**Critères d'acceptation** :
- [ ] Liste des visages avec miniature
- [ ] Option de suppression individuelle
- [ ] Option de réenregistrement (améliorer reconnaissance)
- [ ] Statistiques de reconnaissance par personne

---

## 🏗️ Architecture Technique

### Composants

```
┌─────────────────────────────────────────────────────┐
│              TrustFacesManager                       │
│  - registerFace()                                    │
│  - recognizeFace()                                   │
│  - deleteFace()                                      │
│  - listTrustedFaces()                               │
└─────────────────────────────────────────────────────┘
                          │
┌──────────────┬──────────────┬──────────────────────┐
│ FaceEncoder  │ FaceDatabase │ FaceMatcher          │
│ ML Kit       │ Room + AES   │ Cosine Similarity    │
└──────────────┴──────────────┴──────────────────────┘
```

### Nouveaux Fichiers

```
app/src/main/java/com/privacyguard/
├── trust/
│   ├── TrustFacesManager.kt       # Gestionnaire principal
│   ├── FaceEncoder.kt             # Encodage ML Kit
│   ├── FaceMatcher.kt             # Comparaison embeddings
│   └── models/
│       ├── TrustedFace.kt         # Entity Room
│       └── FaceEncoding.kt        # Embedding data
├── data/
│   ├── TrustDatabase.kt           # Room Database
│   └── dao/
│       └── TrustedFaceDao.kt      # DAO
└── ui/
    └── trust/
        ├── TrustFacesScreen.kt    # Liste visages
        ├── RegisterFaceScreen.kt  # Enregistrement
        └── FacePreviewComposable.kt
```

---

## 🔧 Implémentation

### 1. FaceEncoder.kt

```kotlin
/**
 * Encode un visage en vecteur d'embedding
 * Utilise ML Kit Face Detection + embedding custom
 */
class FaceEncoder(private val context: Context) {
    
    private val faceDetector: FaceDetector
    
    /**
     * Encode un bitmap en vecteur de 128 dimensions
     * @return FloatArray de 128 valeurs ou null si pas de visage
     */
    suspend fun encode(bitmap: Bitmap): FloatArray? {
        // 1. Détecter le visage avec ML Kit
        // 2. Cropper et normaliser
        // 3. Générer embedding
        // 4. Retourner vecteur 128D
    }
    
    /**
     * Encode plusieurs images pour meilleure précision
     */
    suspend fun encodeMultiple(bitmaps: List<Bitmap>): FloatArray? {
        // Moyenne des embeddings
    }
}
```

### 2. TrustedFace.kt (Entity Room)

```kotlin
@Entity(tableName = "trusted_faces")
data class TrustedFace(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String?,                    // Nom optionnel
    
    @ColumnInfo(name = "encoding")
    val encodingBlob: ByteArray,          // Embedding chiffré AES
    
    @ColumnInfo(name = "thumbnail")
    val thumbnailBlob: ByteArray?,        // Miniature chiffrée
    
    val createdAt: Long,
    val lastSeenAt: Long?,
    val recognitionCount: Int = 0,
    
    @ColumnInfo(name = "confidence_threshold")
    val confidenceThreshold: Float = 0.7f // Seuil personnalisé
)
```

### 3. FaceMatcher.kt

```kotlin
/**
 * Compare un visage détecté avec les visages de confiance
 */
class FaceMatcher(private val trustedFaceDao: TrustedFaceDao) {
    
    /**
     * Vérifie si un visage est de confiance
     * @return TrustedFace si reconnu, null sinon
     */
    suspend fun match(encoding: FloatArray): MatchResult {
        val trustedFaces = trustedFaceDao.getAll()
        
        for (face in trustedFaces) {
            val similarity = cosineSimilarity(encoding, face.encoding)
            if (similarity >= face.confidenceThreshold) {
                return MatchResult.Trusted(face, similarity)
            }
        }
        
        return MatchResult.Unknown
    }
    
    /**
     * Calcule la similarité cosinus entre deux embeddings
     */
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        // dot(a, b) / (norm(a) * norm(b))
    }
}

sealed class MatchResult {
    data class Trusted(val face: TrustedFace, val confidence: Float) : MatchResult()
    object Unknown : MatchResult()
}
```

### 4. Intégration avec ThreatScorer

```kotlin
// Dans ThreatScorer.kt - Modification

fun normalizeCameraData(
    cameraData: CameraData,
    trustedFaces: List<TrustedFace>  // NOUVEAU paramètre
): Float {
    val facesDetected = cameraData.facesDetected
    val trustedFacesCount = cameraData.trustedFacesCount  // NOUVEAU
    
    // Ne compter que les visages NON reconnus
    val unknownFaces = facesDetected - trustedFacesCount
    
    return when {
        unknownFaces == 0 -> 0f  // Tous les visages sont de confiance
        unknownFaces == 1 -> 0.3f
        unknownFaces >= 2 -> 0.7f
        else -> 0f
    }
}
```

---

## 🎨 UI/UX

### Écran Liste des Visages de Confiance

```
┌────────────────────────────────────────┐
│ ← Visages de confiance                 │
├────────────────────────────────────────┤
│                                        │
│  ┌─────┐  Marie (Maman)               │
│  │ 👤 │  Reconnu 45 fois              │
│  └─────┘  Dernière: il y a 2h         │
│           ────────────────────  🗑️    │
│                                        │
│  ┌─────┐  Papa                        │
│  │ 👤 │  Reconnu 23 fois              │
│  └─────┘  Dernière: hier              │
│           ────────────────────  🗑️    │
│                                        │
│  ┌─────┐  Sans nom                    │
│  │ 👤 │  Reconnu 12 fois              │
│  └─────┘  Dernière: 3 jours           │
│           ────────────────────  🗑️    │
│                                        │
│  ┌─────────────────────────────────┐  │
│  │     ➕ Ajouter un visage        │  │
│  └─────────────────────────────────┘  │
│                                        │
└────────────────────────────────────────┘
```

### Écran Enregistrement

```
┌────────────────────────────────────────┐
│ ← Enregistrer un visage                │
├────────────────────────────────────────┤
│                                        │
│  ┌────────────────────────────────┐   │
│  │                                │   │
│  │      [Prévisualisation        │   │
│  │         caméra avec           │   │
│  │       cadre de visage]        │   │
│  │                                │   │
│  └────────────────────────────────┘   │
│                                        │
│  📷 Photo 1/3 - Regardez la caméra    │
│                                        │
│  ○ ○ ○  (indicateurs de progression)  │
│                                        │
│  Instructions:                         │
│  "Tournez légèrement la tête           │
│   vers la gauche"                      │
│                                        │
│  ┌─────────────────────────────────┐  │
│  │         📸 Capturer             │  │
│  └─────────────────────────────────┘  │
│                                        │
└────────────────────────────────────────┘
```

---

## 🔒 Sécurité

### Stockage des Encodages

```kotlin
/**
 * Chiffrement AES-256 des encodages faciaux
 */
object FaceEncodingCrypto {
    
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_SIZE = 256
    
    /**
     * Chiffre un embedding facial
     */
    fun encrypt(encoding: FloatArray, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        
        val floatBytes = encoding.toByteArray()
        val encrypted = cipher.doFinal(floatBytes)
        
        // Retourner IV + encrypted
        return cipher.iv + encrypted
    }
    
    /**
     * Déchiffre un embedding facial
     */
    fun decrypt(encryptedData: ByteArray, key: SecretKey): FloatArray {
        val iv = encryptedData.copyOfRange(0, 12)
        val encrypted = encryptedData.copyOfRange(12, encryptedData.size)
        
        val cipher = Cipher.getInstance(ALGORITHM)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        
        val decrypted = cipher.doFinal(encrypted)
        return decrypted.toFloatArray()
    }
}
```

### Règles de Sécurité

- ❌ **JAMAIS** stocker les photos originales
- ❌ **JAMAIS** exporter les encodages
- ✅ Stocker uniquement les embeddings chiffrés
- ✅ Clé de chiffrement dans Android Keystore
- ✅ Option de suppression totale (RGPD)

---

## 🧪 Tests

### Tests Unitaires

```kotlin
@Test
fun `encoding should produce 128-dimensional vector`() {
    val bitmap = loadTestBitmap("face_front.jpg")
    val encoding = faceEncoder.encode(bitmap)
    
    assertNotNull(encoding)
    assertEquals(128, encoding.size)
}

@Test
fun `same face should have high similarity`() {
    val bitmap1 = loadTestBitmap("face_front.jpg")
    val bitmap2 = loadTestBitmap("face_angle.jpg") // Même personne
    
    val encoding1 = faceEncoder.encode(bitmap1)!!
    val encoding2 = faceEncoder.encode(bitmap2)!!
    
    val similarity = faceMatcher.cosineSimilarity(encoding1, encoding2)
    assertTrue(similarity > 0.8f)
}

@Test
fun `different faces should have low similarity`() {
    val bitmap1 = loadTestBitmap("person_a.jpg")
    val bitmap2 = loadTestBitmap("person_b.jpg")
    
    val encoding1 = faceEncoder.encode(bitmap1)!!
    val encoding2 = faceEncoder.encode(bitmap2)!!
    
    val similarity = faceMatcher.cosineSimilarity(encoding1, encoding2)
    assertTrue(similarity < 0.5f)
}
```

### Tests d'Intégration

- [ ] Enregistrement complet d'un visage
- [ ] Reconnaissance en temps réel (caméra live)
- [ ] Performance avec 10+ visages enregistrés
- [ ] Fonctionne avec différentes luminosités
- [ ] Fonctionne avec masque (optionnel)

---

## 📊 Métriques

| Métrique | Objectif | MVP |
|----------|----------|-----|
| Précision reconnaissance | >95% | N/A |
| Faux positifs | <1% | N/A |
| Latence reconnaissance | <200ms | N/A |
| Taille embedding | 128 floats | N/A |
| Max visages supportés | 20 | N/A |

---

## 📦 Dépendances

```kotlin
// ML Kit Face Detection (déjà présent)
implementation("com.google.mlkit:face-detection:16.1.5")

// TensorFlow Lite pour embeddings custom (optionnel)
implementation("org.tensorflow:tensorflow-lite:2.13.0")

// Room pour stockage (à réactiver)
implementation("androidx.room:room-runtime:2.6.0")
kapt("androidx.room:room-compiler:2.6.0")
```

---

## 🚀 Plan d'Implémentation

### Phase 1 : Foundation (3 jours)
- [ ] Réactiver Room/Hilt
- [ ] Créer TrustDatabase et entités
- [ ] Implémenter FaceEncoder basique

### Phase 2 : Core (4 jours)
- [ ] Implémenter FaceMatcher
- [ ] Intégrer avec CameraSensor
- [ ] Modifier ThreatScorer

### Phase 3 : UI (3 jours)
- [ ] TrustFacesScreen
- [ ] RegisterFaceScreen
- [ ] Intégration navigation

### Phase 4 : Polish (2 jours)
- [ ] Tests complets
- [ ] Optimisation performance
- [ ] Documentation

---

**Dernière mise à jour** : 7 décembre 2025  
**Auteur** : Privacy Guard Team

