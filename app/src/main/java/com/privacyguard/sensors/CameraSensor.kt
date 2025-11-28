package com.privacyguard.sensors

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Capteur caméra avec détection de visages ML Kit
 * 
 * Utilise CameraX pour l'acquisition d'images et ML Kit pour la détection de visages.
 * Détecte :
 * - Nombre de visages présents
 * - Visages regardant l'écran (orientation de la tête)
 * - Proximité des visages
 * 
 * Évalue le niveau de menace selon :
 * - NONE : Aucun visage détecté
 * - LOW : 1 visage connu ou loin
 * - MEDIUM : 1 visage inconnu regardant l'écran
 * - HIGH : Plusieurs visages ou très proche
 * - CRITICAL : Plusieurs visages inconnus très proches
 */
class CameraSensor(
    context: Context,
    private val lifecycleOwner: LifecycleOwner
) : BaseSensor<CameraData>(context, "CameraSensor") {
    
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    // ML Kit Face Detector
    private val faceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST) // Mode rapide pour temps réel
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE) // Pas besoin des landmarks
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // Yeux ouverts/fermés, sourire
        .setMinFaceSize(0.15f) // Taille minimale du visage (15% de l'image)
        .enableTracking() // Tracking des visages entre les frames
        .build()
    
    private val faceDetector: FaceDetector = FaceDetection.getClient(faceDetectorOptions)
    
    private var analysisJob: Job? = null
    private val analysisScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Configuration
    private val minAnalysisIntervalMs = 500L // Analyse toutes les 500ms (2 FPS)
    private var lastAnalysisTime = 0L
    
    override suspend fun onStart() {
        Timber.d("CameraSensor: Initializing camera...")
        
        try {
            withContext(Dispatchers.Main) {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    try {
                        cameraProvider = cameraProviderFuture.get()
                        bindCameraUseCases()
                    } catch (e: Exception) {
                        Timber.e(e, "CameraSensor: Failed to initialize camera in listener")
                        // Émettre des données vides pour éviter le crash
                        emitData(
                            CameraData(
                                timestamp = System.currentTimeMillis(),
                                threatLevel = ThreatLevel.NONE,
                                confidence = 0f,
                                facesDetected = 0,
                                facesLookingAtScreen = 0,
                                unknownFacesCount = 0
                            )
                        )
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        } catch (e: Exception) {
            Timber.e(e, "CameraSensor: Failed to start camera initialization")
            // Émettre des données vides pour éviter le crash
            emitData(
                CameraData(
                    timestamp = System.currentTimeMillis(),
                    threatLevel = ThreatLevel.NONE,
                    confidence = 0f,
                    facesDetected = 0,
                    facesLookingAtScreen = 0,
                    unknownFacesCount = 0
                )
            )
        }
    }
    
    override suspend fun onStop() {
        Timber.d("CameraSensor: Stopping camera...")
        
        analysisJob?.cancel()
        analysisJob = null
        
        withContext(Dispatchers.Main) {
            cameraProvider?.unbindAll()
        }
        
        camera = null
        imageAnalysis = null
    }
    
    /**
     * Lie les cas d'usage de la caméra (ImageAnalysis pour ML Kit)
     */
    @androidx.camera.core.ExperimentalGetImage
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: run {
            Timber.e("CameraSensor: Camera provider is null")
            return
        }
        
        Timber.i("CameraSensor: Binding camera use cases...")
        
        // Caméra frontale pour détecter les personnes regardant l'écran
        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        
        // Compteur de frames pour debug
        var frameCount = 0
        
        // ImageAnalysis pour ML Kit
        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888) // YUV pour ML Kit (RGBA non supporté par ML Kit)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    frameCount++
                    if (frameCount % 10 == 1) {
                        Timber.d("CameraSensor: Frame #$frameCount received from camera")
                    }
                    processImageProxy(imageProxy)
                }
            }
        
        try {
            // Délier tous les cas d'usage existants
            cameraProvider.unbindAll()
            
            // Lier les cas d'usage au lifecycle
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                imageAnalysis
            )
            
            Timber.i("CameraSensor: Camera bound successfully to lifecycle")
            Timber.i("CameraSensor: Waiting for frames from front camera...")
        } catch (e: Exception) {
            Timber.e(e, "CameraSensor: Failed to bind camera use cases: ${e.message}")
        }
    }
    
    /**
     * Traite une image de la caméra avec ML Kit
     */
    @androidx.camera.core.ExperimentalGetImage
    private fun processImageProxy(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        
        // Throttling: ne pas analyser trop souvent
        if (currentTime - lastAnalysisTime < minAnalysisIntervalMs) {
            imageProxy.close()
            return
        }
        
        lastAnalysisTime = currentTime
        
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            Timber.w("CameraSensor: MediaImage is null, skipping frame")
            imageProxy.close()
            return
        }
        
        Timber.d("CameraSensor: Processing frame ${imageProxy.width}x${imageProxy.height}")
        
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        
        // Détection de visages avec ML Kit
        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                Timber.i("CameraSensor: ML Kit SUCCESS - ${faces.size} face(s) detected")
                handleFaceDetection(faces, currentTime)
            }
            .addOnFailureListener { e ->
                Timber.e(e, "CameraSensor: ML Kit FAILED - ${e.message}")
                // Émettre des données même en cas d'erreur pour que camera != null
                val errorData = CameraData(
                    timestamp = currentTime,
                    threatLevel = ThreatLevel.NONE,
                    confidence = 0f,
                    facesDetected = 0,
                    facesLookingAtScreen = 0,
                    unknownFacesCount = 0
                )
                Timber.d("CameraSensor: Emitting error fallback data")
                emitData(errorData)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
    
    /**
     * Traite les résultats de détection de visages
     */
    private fun handleFaceDetection(faces: List<Face>, timestamp: Long) {
        val facesCount = faces.size
        
        if (facesCount == 0) {
            // Aucun visage détecté = aucune menace
            emitData(
                CameraData(
                    timestamp = timestamp,
                    threatLevel = ThreatLevel.NONE,
                    confidence = 1.0f,
                    facesDetected = 0,
                    facesLookingAtScreen = 0,
                    unknownFacesCount = 0
                )
            )
            return
        }
        
        // Analyser chaque visage
        var facesLookingAtScreen = 0
        var closestFaceDistance = Float.MAX_VALUE
        
        for (face in faces) {
            // Vérifier si le visage regarde l'écran (angles de rotation de la tête)
            val headEulerAngleY = face.headEulerAngleY // Rotation gauche/droite
            val headEulerAngleZ = face.headEulerAngleZ // Rotation inclinaison
            
            // Considérer que le visage regarde l'écran si les angles sont faibles
            val isLookingAtScreen = 
                Math.abs(headEulerAngleY) < 20f && Math.abs(headEulerAngleZ) < 15f
            
            if (isLookingAtScreen) {
                facesLookingAtScreen++
            }
            
            // Estimer la distance basée sur la taille du bounding box
            val faceSize = face.boundingBox.width() * face.boundingBox.height()
            if (faceSize < closestFaceDistance) {
                closestFaceDistance = faceSize.toFloat()
            }
        }
        
        // Évaluer le niveau de menace
        val (threatLevel, confidence) = evaluateThreatLevel(
            facesCount = facesCount,
            facesLookingAtScreen = facesLookingAtScreen,
            closestFaceSize = closestFaceDistance
        )
        
        // Émettre les données
        val cameraDataToEmit = CameraData(
            timestamp = timestamp,
            threatLevel = threatLevel,
            confidence = confidence,
            facesDetected = facesCount,
            facesLookingAtScreen = facesLookingAtScreen,
            unknownFacesCount = facesCount, // Pour MVP, tous les visages sont "inconnus"
            distanceToCamera = null // Sera calculé plus tard avec reconnaissance faciale
        )
        
        Timber.i("CameraSensor: EMITTING data - faces=$facesCount, looking=$facesLookingAtScreen, threat=$threatLevel")
        emitData(cameraDataToEmit)
        
        Timber.d(
            "CameraSensor: Detected $facesCount face(s), " +
            "$facesLookingAtScreen looking at screen, " +
            "threat=$threatLevel (confidence=$confidence)"
        )
    }
    
    /**
     * Évalue le niveau de menace selon les données de visages
     */
    private fun evaluateThreatLevel(
        facesCount: Int,
        facesLookingAtScreen: Int,
        closestFaceSize: Float
    ): Pair<ThreatLevel, Float> {
        // Seuil de proximité (taille du bounding box en pixels²)
        val veryCloseThreshold = 50000f
        val closeThreshold = 20000f
        
        return when {
            // Aucun visage
            facesCount == 0 -> ThreatLevel.NONE to 1.0f
            
            // Plusieurs visages regardant l'écran = CRITIQUE
            facesLookingAtScreen >= 2 -> ThreatLevel.CRITICAL to 0.9f
            
            // 1 visage regardant l'écran et très proche = HIGH
            facesLookingAtScreen >= 1 && closestFaceSize > veryCloseThreshold -> 
                ThreatLevel.HIGH to 0.85f
            
            // 1 visage regardant l'écran et proche = MEDIUM
            facesLookingAtScreen >= 1 && closestFaceSize > closeThreshold -> 
                ThreatLevel.MEDIUM to 0.75f
            
            // 1 visage regardant l'écran mais loin = LOW
            facesLookingAtScreen >= 1 -> 
                ThreatLevel.LOW to 0.6f
            
            // Visages présents mais ne regardent pas l'écran = LOW
            facesCount > 0 -> 
                ThreatLevel.LOW to 0.4f
            
            else -> ThreatLevel.NONE to 1.0f
        }
    }
    
    /**
     * Nettoyage des ressources
     */
    fun cleanup() {
        analysisScope.cancel()
        cameraExecutor.shutdown()
        faceDetector.close()
    }
}

