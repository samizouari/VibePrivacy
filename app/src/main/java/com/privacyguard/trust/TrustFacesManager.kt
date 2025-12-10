package com.privacyguard.trust

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.privacyguard.trust.models.FaceMatchResult
import com.privacyguard.trust.models.FaceRegistrationData
import com.privacyguard.trust.models.TrustedFace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.UUID

/**
 * Gestionnaire des visages de confiance
 * 
 * Permet d'enregistrer, reconnaître et gérer les visages de confiance.
 * Utilise EncryptedSharedPreferences pour stocker les encodages de manière sécurisée.
 * 
 * Note: Pour une app en production, utiliser Room Database avec chiffrement.
 */
class TrustFacesManager(
    private val context: Context,
    private val faceEncoder: FaceEncoder,
    private val faceMatcher: FaceMatcher
) {
    
    companion object {
        private const val PREFS_NAME = "trusted_faces_prefs"
        private const val KEY_FACES = "faces"
        private const val MAX_FACES = 20
        private const val THUMBNAIL_MAX_SIZE = 200 // px
    }
    
    private val gson = Gson()
    
    // EncryptedSharedPreferences pour stockage sécurisé
    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    // État actuel
    private val _trustedFaces = MutableStateFlow<List<TrustedFace>>(emptyList())
    val trustedFaces: StateFlow<List<TrustedFace>> = _trustedFaces.asStateFlow()
    
    init {
        loadFaces()
    }
    
    /**
     * Enregistre un nouveau visage de confiance
     * 
     * @param data Données d'enregistrement (photos + nom)
     * @return Result avec le TrustedFace créé ou une erreur
     */
    suspend fun registerFace(data: FaceRegistrationData): Result<TrustedFace> {
        if (_trustedFaces.value.size >= MAX_FACES) {
            return Result.failure(Exception("Maximum $MAX_FACES visages atteint"))
        }
        
        if (data.photos.isEmpty()) {
            return Result.failure(Exception("Au moins une photo est requise"))
        }
        
        try {
            // Convertir ByteArray en Bitmap
            val bitmaps = data.photos.mapNotNull { bytes ->
                try {
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (e: Exception) {
                    Timber.e(e, "Error decoding photo")
                    null
                }
            }
            
            if (bitmaps.isEmpty()) {
                return Result.failure(Exception("Aucune photo valide"))
            }
            
            // Générer l'encoding facial (moyenne de toutes les photos)
            val encoding = faceEncoder.encodeMultiple(bitmaps)
                ?: return Result.failure(Exception("Impossible de détecter un visage dans les photos"))
            
            // Générer une miniature
            val thumbnailBase64 = data.thumbnailPhoto?.let { bytes ->
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                val thumbnail = createThumbnail(bitmap)
                bitmapToBase64(thumbnail)
            }
            
            // Créer le TrustedFace
            val trustedFace = TrustedFace(
                id = UUID.randomUUID().toString(),
                name = data.name,
                encoding = encoding,
                thumbnailBase64 = thumbnailBase64
            )
            
            // Ajouter à la liste
            val updatedFaces = _trustedFaces.value + trustedFace
            saveFaces(updatedFaces)
            _trustedFaces.value = updatedFaces
            
            Timber.i("TrustFacesManager: Registered face '${data.name ?: "Unnamed"}'")
            return Result.success(trustedFace)
            
        } catch (e: Exception) {
            Timber.e(e, "TrustFacesManager: Error registering face")
            return Result.failure(e)
        }
    }
    
    /**
     * Reconnaît un visage à partir d'un bitmap
     * 
     * @param bitmap Image contenant le visage
     * @return FaceMatchResult.Trusted si reconnu, Unknown sinon
     */
    suspend fun recognizeFace(bitmap: Bitmap): FaceMatchResult {
        try {
            // Encoder le visage
            val encoding = faceEncoder.encode(bitmap)
                ?: return FaceMatchResult.Unknown
            
            // Comparer avec les visages de confiance
            val result = faceMatcher.match(encoding, _trustedFaces.value)
            
            // Mettre à jour les stats si match
            if (result is FaceMatchResult.Trusted) {
                updateRecognitionStats(result.face)
            }
            
            return result
            
        } catch (e: Exception) {
            Timber.e(e, "TrustFacesManager: Error recognizing face")
            return FaceMatchResult.Unknown
        }
    }
    
    /**
     * Reconnaît un visage à partir d'un encoding (plus rapide)
     */
    fun recognizeFaceFromEncoding(encoding: FloatArray): FaceMatchResult {
        try {
            val result = faceMatcher.match(encoding, _trustedFaces.value)
            
            if (result is FaceMatchResult.Trusted) {
                updateRecognitionStats(result.face)
            }
            
            return result
            
        } catch (e: Exception) {
            Timber.e(e, "TrustFacesManager: Error recognizing face from encoding")
            return FaceMatchResult.Unknown
        }
    }
    
    /**
     * Supprime un visage de confiance
     */
    fun deleteFace(faceId: String) {
        val updatedFaces = _trustedFaces.value.filter { it.id != faceId }
        saveFaces(updatedFaces)
        _trustedFaces.value = updatedFaces
        
        Timber.i("TrustFacesManager: Deleted face $faceId")
    }
    
    /**
     * Supprime tous les visages
     */
    fun deleteAll() {
        saveFaces(emptyList())
        _trustedFaces.value = emptyList()
        
        Timber.i("TrustFacesManager: Deleted all faces")
    }
    
    /**
     * Met à jour les statistiques de reconnaissance
     */
    private fun updateRecognitionStats(face: TrustedFace) {
        val updatedFace = face.copy(
            lastSeenAt = System.currentTimeMillis(),
            recognitionCount = face.recognitionCount + 1
        )
        
        val updatedFaces = _trustedFaces.value.map {
            if (it.id == face.id) updatedFace else it
        }
        
        saveFaces(updatedFaces)
        _trustedFaces.value = updatedFaces
    }
    
    /**
     * Crée une miniature d'un bitmap
     */
    private fun createThumbnail(bitmap: Bitmap): Bitmap {
        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val width = if (ratio > 1f) THUMBNAIL_MAX_SIZE else (THUMBNAIL_MAX_SIZE * ratio).toInt()
        val height = if (ratio > 1f) (THUMBNAIL_MAX_SIZE / ratio).toInt() else THUMBNAIL_MAX_SIZE
        
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
    
    /**
     * Convertit un Bitmap en Base64
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
    
    /**
     * Convertit une chaîne Base64 en Bitmap
     */
    fun base64ToBitmap(base64: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            Timber.e(e, "Error decoding Base64 to Bitmap")
            null
        }
    }
    
    /**
     * Charge les visages depuis le stockage
     */
    private fun loadFaces() {
        try {
            val json = encryptedPrefs.getString(KEY_FACES, null)
            if (json != null) {
                val type = object : TypeToken<List<TrustedFace>>() {}.type
                val loadedFaces = gson.fromJson<List<TrustedFace>>(json, type)
                _trustedFaces.value = loadedFaces
                Timber.d("TrustFacesManager: Loaded ${loadedFaces.size} face(s)")
            }
        } catch (e: Exception) {
            Timber.e(e, "TrustFacesManager: Error loading faces")
        }
    }
    
    /**
     * Sauvegarde les visages dans le stockage
     */
    private fun saveFaces(faces: List<TrustedFace>) {
        try {
            val json = gson.toJson(faces)
            encryptedPrefs.edit().putString(KEY_FACES, json).apply()
            Timber.d("TrustFacesManager: Saved ${faces.size} face(s)")
        } catch (e: Exception) {
            Timber.e(e, "TrustFacesManager: Error saving faces")
        }
    }
}

