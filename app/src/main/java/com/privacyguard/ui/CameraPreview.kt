package com.privacyguard.ui

import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Composant Compose pour afficher la prévisualisation de la caméra
 * avec encadrement des visages détectés en vert
 */
@Composable
fun CameraPreviewWithFaceDetection(
    modifier: Modifier = Modifier,
    onFacesDetected: (List<Face>) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var preview by remember { mutableStateOf<Preview?>(null) }
    var imageAnalysis by remember { mutableStateOf<ImageAnalysis?>(null) }
    var detectedFaces by remember { mutableStateOf<List<Face>>(emptyList()) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    
    // ML Kit Face Detector
    val faceDetectorOptions = remember {
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()
    }
    
    val faceDetector: FaceDetector = remember {
        FaceDetection.getClient(faceDetectorOptions)
    }
    
    // Initialiser CameraX
    LaunchedEffect(Unit) {
        val provider = ProcessCameraProvider.getInstance(context).get()
        cameraProvider = provider
    }
    
    // Configurer la caméra quand le provider est prêt
    LaunchedEffect(cameraProvider) {
        val provider = cameraProvider ?: run {
            Timber.w("CameraPreview: Camera provider is null")
            return@LaunchedEffect
        }
        
        try {
            // Preview
            val previewUseCase = Preview.Builder()
                .build()
            preview = previewUseCase
            Timber.d("CameraPreview: Preview use case created")
            
            // ImageAnalysis pour ML Kit
            val analysisUseCase = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageForDebug(imageProxy, faceDetector) { faces ->
                            detectedFaces = faces
                            onFacesDetected(faces)
                        }
                    }
                }
            imageAnalysis = analysisUseCase
            Timber.d("CameraPreview: ImageAnalysis use case created")
            
            // Lier au lifecycle
            provider.unbindAll()
            val camera = provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                previewUseCase,
                analysisUseCase
            )
            Timber.i("CameraPreview: Camera bound successfully, camera info: ${camera.cameraInfo}")
        } catch (e: Exception) {
            Timber.e(e, "CameraPreview: Failed to bind camera")
        }
    }
    
    // Nettoyer à la destruction
    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
            faceDetector.close()
        }
    }
    
    // UI
    Box(modifier = modifier) {
        // Debug: Fond coloré pour voir si le Box est bien rendu
        // Si vous voyez du bleu, le Box fonctionne mais pas le PreviewView
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(-1f)
                .background(Color.Blue.copy(alpha = 0.1f))
        )
        
        // Preview de la caméra
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    previewViewRef = this
                    Timber.d("CameraPreview: PreviewView created, size will be set by parent")
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0f), // PreviewView en dessous de l'overlay
            update = { previewView ->
                previewViewRef = previewView
            }
        )
        
        // Connecter le preview use case au PreviewView quand les deux sont prêts
        LaunchedEffect(preview, previewViewRef) {
            preview?.let { previewUseCase ->
                previewViewRef?.let { view ->
                    previewUseCase.setSurfaceProvider(view.surfaceProvider)
                    Timber.d("CameraPreview: Surface provider connected")
                } ?: Timber.w("CameraPreview: PreviewView not ready yet")
            } ?: Timber.w("CameraPreview: Preview use case not ready yet")
        }
        
        // Overlay pour dessiner les rectangles des visages (au-dessus du PreviewView)
        // Ne dessiner que s'il y a des visages détectés
        if (detectedFaces.isNotEmpty()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f) // S'assurer que l'overlay est au-dessus
            ) {
                detectedFaces.forEach { face ->
                val bounds = face.boundingBox
                
                // Les coordonnées ML Kit sont dans l'espace de l'image
                // On les utilise directement car PreviewView gère déjà le scaling
                val left = bounds.left.toFloat()
                val top = bounds.top.toFloat()
                val right = bounds.right.toFloat()
                val bottom = bounds.bottom.toFloat()
                
                val width = right - left
                val height = bottom - top
                
                // Dessiner le rectangle vert autour du visage
                drawRect(
                    color = androidx.compose.ui.graphics.Color(0xFF4CAF50), // Vert
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    style = Stroke(width = 4.dp.toPx())
                )
                
                // Dessiner un point au centre du visage
                val centerX = left + width / 2f
                val centerY = top + height / 2f
                drawCircle(
                    color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                    radius = 8.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
                }
            }
        }
        
        // Indicateur de debug en haut à droite
        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = "📷 Debug Caméra",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "${detectedFaces.size} visage(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Traite une image pour la détection de visages (debug)
 */
@androidx.camera.core.ExperimentalGetImage
private fun processImageForDebug(
    imageProxy: ImageProxy,
    faceDetector: FaceDetector,
    onFacesDetected: (List<Face>) -> Unit
) {
    val mediaImage = imageProxy.image ?: run {
        imageProxy.close()
        return
    }
    
    val image = InputImage.fromMediaImage(
        mediaImage,
        imageProxy.imageInfo.rotationDegrees
    )
    
    faceDetector.process(image)
        .addOnSuccessListener { faces ->
            Timber.d("CameraPreview: Detected ${faces.size} face(s)")
            onFacesDetected(faces)
        }
        .addOnFailureListener { e ->
            Timber.e(e, "CameraPreview: Face detection failed")
            onFacesDetected(emptyList())
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}

