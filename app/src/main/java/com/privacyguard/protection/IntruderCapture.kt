package com.privacyguard.protection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Environment
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Capture et stockage sécurisé des photos d'intrus
 * 
 * Fonctionnalités :
 * - Capture photo lors de détection de menace
 * - Chiffrement AES des photos
 * - Stockage dans dossier privé de l'app
 * - Déchiffrement pour affichage
 * - Nettoyage automatique des anciennes photos
 */
class IntruderCapture(private val context: Context) {
    
    companion object {
        private const val TAG = "IntruderCapture"
        private const val INTRUDER_FOLDER = "intruders"
        private const val KEY_ALIAS = "intruder_key"
        private const val MAX_PHOTOS = 50 // Garder max 50 photos
        private const val MAX_AGE_DAYS = 30 // Supprimer après 30 jours
        private const val ENCRYPTION_ALGORITHM = "AES"
    }
    
    private val intruderDir: File by lazy {
        File(context.filesDir, INTRUDER_FOLDER).apply {
            if (!exists()) mkdirs()
        }
    }
    
    // Clé de chiffrement (en production, utiliser Android Keystore)
    private val secretKey: SecretKey by lazy {
        // Pour MVP, on génère une clé simple stockée dans SharedPreferences
        val prefs = context.getSharedPreferences("intruder_prefs", Context.MODE_PRIVATE)
        val keyBytes = prefs.getString(KEY_ALIAS, null)
        
        if (keyBytes != null) {
            SecretKeySpec(android.util.Base64.decode(keyBytes, android.util.Base64.DEFAULT), ENCRYPTION_ALGORITHM)
        } else {
            // Générer nouvelle clé
            val keyGen = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM)
            keyGen.init(256)
            val newKey = keyGen.generateKey()
            
            // Sauvegarder
            prefs.edit()
                .putString(KEY_ALIAS, android.util.Base64.encodeToString(newKey.encoded, android.util.Base64.DEFAULT))
                .apply()
            
            newKey
        }
    }
    
    /**
     * Capture une photo d'intrus depuis une ImageProxy CameraX
     */
    suspend fun captureFromImageProxy(imageProxy: ImageProxy, threatLevel: String): IntruderPhoto? {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("$TAG: Capturing intruder photo, threat level: $threatLevel")
                
                // Convertir ImageProxy en Bitmap
                val bitmap = imageProxyToBitmap(imageProxy)
                if (bitmap == null) {
                    Timber.e("$TAG: Failed to convert ImageProxy to Bitmap")
                    return@withContext null
                }
                
                // Générer nom de fichier
                val timestamp = System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val fileName = "intruder_${dateFormat.format(Date(timestamp))}_${threatLevel}.enc"
                
                // Sauvegarder la photo chiffrée
                val savedFile = saveEncryptedPhoto(bitmap, fileName)
                
                if (savedFile != null) {
                    Timber.i("$TAG: Intruder photo saved: ${savedFile.name}")
                    
                    // Nettoyer les anciennes photos
                    cleanupOldPhotos()
                    
                    return@withContext IntruderPhoto(
                        id = timestamp,
                        fileName = fileName,
                        timestamp = timestamp,
                        threatLevel = threatLevel,
                        file = savedFile
                    )
                } else {
                    Timber.e("$TAG: Failed to save encrypted photo")
                    return@withContext null
                }
                
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Error capturing intruder photo")
                null
            }
        }
    }
    
    /**
     * Capture une photo depuis un Bitmap
     */
    suspend fun captureFromBitmap(bitmap: Bitmap, threatLevel: String): IntruderPhoto? {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val fileName = "intruder_${dateFormat.format(Date(timestamp))}_${threatLevel}.enc"
                
                val savedFile = saveEncryptedPhoto(bitmap, fileName)
                
                if (savedFile != null) {
                    cleanupOldPhotos()
                    IntruderPhoto(
                        id = timestamp,
                        fileName = fileName,
                        timestamp = timestamp,
                        threatLevel = threatLevel,
                        file = savedFile
                    )
                } else null
                
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Error capturing from bitmap")
                null
            }
        }
    }
    
    /**
     * Convertir ImageProxy (YUV_420_888) en Bitmap
     */
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            val yBuffer = imageProxy.planes[0].buffer
            val uBuffer = imageProxy.planes[1].buffer
            val vBuffer = imageProxy.planes[2].buffer
            
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            
            val nv21 = ByteArray(ySize + uSize + vSize)
            
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)
            
            val yuvImage = YuvImage(
                nv21,
                ImageFormat.NV21,
                imageProxy.width,
                imageProxy.height,
                null
            )
            
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(
                Rect(0, 0, imageProxy.width, imageProxy.height),
                80,
                out
            )
            
            val imageBytes = out.toByteArray()
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error converting ImageProxy to Bitmap")
            null
        }
    }
    
    /**
     * Sauvegarder une photo chiffrée
     */
    private fun saveEncryptedPhoto(bitmap: Bitmap, fileName: String): File? {
        return try {
            // Compresser le bitmap en JPEG
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val imageBytes = outputStream.toByteArray()
            
            // Chiffrer avec AES/CBC/PKCS5Padding (même mode que le déchiffrement)
            val cipher = Cipher.getInstance("$ENCRYPTION_ALGORITHM/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encryptedBytes = cipher.doFinal(imageBytes)
            
            // Sauvegarder IV + données chiffrées
            val file = File(intruderDir, fileName)
            FileOutputStream(file).use { fos ->
                // Écrire l'IV (16 bytes pour AES)
                fos.write(cipher.iv)
                // Écrire les données chiffrées
                fos.write(encryptedBytes)
            }
            
            Timber.d("$TAG: Photo encrypted and saved: ${file.name}, size: ${file.length()} bytes")
            file
            
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error saving encrypted photo")
            null
        }
    }
    
    /**
     * Déchiffrer une photo pour affichage
     */
    suspend fun decryptPhoto(photo: IntruderPhoto): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val file = photo.file
                if (!file.exists()) {
                    Timber.e("$TAG: Photo file not found: ${file.name}")
                    return@withContext null
                }
                
                val fileBytes = file.readBytes()
                
                // Extraire l'IV (premiers 16 bytes)
                val iv = fileBytes.copyOfRange(0, 16)
                val encryptedData = fileBytes.copyOfRange(16, fileBytes.size)
                
                // Déchiffrer
                val cipher = Cipher.getInstance("$ENCRYPTION_ALGORITHM/CBC/PKCS5Padding")
                val ivSpec = javax.crypto.spec.IvParameterSpec(iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
                val decryptedBytes = cipher.doFinal(encryptedData)
                
                // Convertir en Bitmap
                BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size)
                
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Error decrypting photo")
                null
            }
        }
    }
    
    /**
     * Obtenir la liste des photos d'intrus
     */
    fun getIntruderPhotos(): List<IntruderPhoto> {
        return try {
            intruderDir.listFiles()
                ?.filter { it.name.startsWith("intruder_") && it.name.endsWith(".enc") }
                ?.map { file ->
                    // Parser le nom du fichier pour extraire les infos
                    val parts = file.nameWithoutExtension.split("_")
                    val dateStr = if (parts.size >= 2) parts[1] else ""
                    val timeStr = if (parts.size >= 3) parts[2] else ""
                    val threatLevel = if (parts.size >= 4) parts[3] else "UNKNOWN"
                    
                    IntruderPhoto(
                        id = file.lastModified(),
                        fileName = file.name,
                        timestamp = file.lastModified(),
                        threatLevel = threatLevel,
                        file = file
                    )
                }
                ?.sortedByDescending { it.timestamp }
                ?: emptyList()
                
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error getting intruder photos")
            emptyList()
        }
    }
    
    /**
     * Supprimer une photo
     */
    fun deletePhoto(photo: IntruderPhoto): Boolean {
        return try {
            photo.file.delete()
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error deleting photo")
            false
        }
    }
    
    /**
     * Supprimer toutes les photos
     */
    fun deleteAllPhotos(): Boolean {
        return try {
            intruderDir.listFiles()?.forEach { it.delete() }
            true
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error deleting all photos")
            false
        }
    }
    
    /**
     * Nettoyer les anciennes photos
     */
    private fun cleanupOldPhotos() {
        try {
            val photos = getIntruderPhotos().toMutableList()
            val now = System.currentTimeMillis()
            val maxAgeMs = MAX_AGE_DAYS * 24 * 60 * 60 * 1000L
            
            // Supprimer les photos trop anciennes
            val expiredPhotos = photos.filter { now - it.timestamp > maxAgeMs }
            expiredPhotos.forEach { deletePhoto(it) }
            
            // Supprimer les photos excédentaires (garder les plus récentes)
            val remainingPhotos = photos - expiredPhotos.toSet()
            if (remainingPhotos.size > MAX_PHOTOS) {
                remainingPhotos
                    .sortedByDescending { it.timestamp }
                    .drop(MAX_PHOTOS)
                    .forEach { deletePhoto(it) }
            }
            
            Timber.d("$TAG: Cleanup complete. Removed ${expiredPhotos.size} expired photos")
            
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error during cleanup")
        }
    }
    
    /**
     * Obtenir le nombre de photos stockées
     */
    fun getPhotoCount(): Int {
        return intruderDir.listFiles()?.size ?: 0
    }
}

/**
 * Données d'une photo d'intrus
 */
data class IntruderPhoto(
    val id: Long,
    val fileName: String,
    val timestamp: Long,
    val threatLevel: String,
    val file: File
) {
    /**
     * Date formatée pour affichage
     */
    fun getFormattedDate(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
    
    /**
     * Emoji de niveau de menace
     */
    fun getThreatEmoji(): String {
        return when (threatLevel.uppercase()) {
            "CRITICAL" -> "🔴"
            "HIGH" -> "🟠"
            "MEDIUM" -> "🟡"
            "LOW" -> "🟢"
            else -> "⚪"
        }
    }
}

