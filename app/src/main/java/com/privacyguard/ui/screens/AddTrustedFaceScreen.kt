package com.privacyguard.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.privacyguard.trust.FaceEncoder
import com.privacyguard.trust.FaceMatcher
import com.privacyguard.trust.TrustFacesManager
import com.privacyguard.trust.models.FaceRegistrationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

/**
 * Écran d'ajout d'un nouveau visage de confiance
 * 
 * Guide l'utilisateur pour prendre 3 photos (face, profil gauche, profil droit)
 * et enregistre le visage avec encodage facial.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTrustedFaceScreen(
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    // Managers
    val faceEncoder = remember { FaceEncoder(context) }
    val faceMatcher = remember { FaceMatcher() }
    val trustFacesManager = remember {
        TrustFacesManager(context, faceEncoder, faceMatcher)
    }
    
    // État
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var name by remember { mutableStateOf("") }
    var currentStep by remember { mutableStateOf(0) } // 0=face, 1=profil G, 2=profil D
    var capturedPhotos by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // Camera
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }
    
    // Demander la permission au démarrage si nécessaire
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajouter un visage") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        if (!hasCameraPermission) {
            // Permission requise
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "📷 Permission requise",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "L'accès à la caméra est nécessaire pour enregistrer un visage",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Autoriser la caméra")
                }
            }
        } else if (isProcessing) {
            // En cours de traitement
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Enregistrement du visage...")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Champ nom (en haut)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Nom (optionnel)",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Ex: Maman, Papa, Marie...") },
                            singleLine = true
                        )
                    }
                }
                
                // Instructions
                Text(
                    text = when (currentStep) {
                        0 -> "📸 Étape 1/3 : Photo de face"
                        1 -> "📸 Étape 2/3 : Profil gauche"
                        2 -> "📸 Étape 3/3 : Profil droit"
                        else -> ""
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Prévisualisation caméra
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                ) {
                    CameraPreview(
                        onImageCaptureReady = { capture, provider ->
                            imageCapture = capture
                            cameraProvider = provider
                        },
                        lifecycleOwner = lifecycleOwner
                    )
                }
                
                // Miniatures des photos prises
                if (capturedPhotos.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        capturedPhotos.forEachIndexed { index, bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Photo ${index + 1}",
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Message d'erreur
                error?.let {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "⚠️ $it",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Boutons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Bouton annuler/retour
                    if (capturedPhotos.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                if (capturedPhotos.isNotEmpty()) {
                                    capturedPhotos = capturedPhotos.dropLast(1)
                                    currentStep = maxOf(0, currentStep - 1)
                                    error = null
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retour")
                        }
                    }
                    
                    // Bouton capturer / valider
                    Button(
                        onClick = {
                            if (currentStep < 3) {
                                // Capturer une photo
                                val capture = imageCapture ?: return@Button
                                
                                capture.takePicture(
                                    Executors.newSingleThreadExecutor(),
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val bitmap = imageProxyToBitmap(image)
                                            if (bitmap != null) {
                                                capturedPhotos = capturedPhotos + bitmap
                                                currentStep++
                                                error = null
                                            } else {
                                                error = "Erreur lors de la capture"
                                            }
                                            image.close()
                                        }
                                        
                                        override fun onError(exception: ImageCaptureException) {
                                            error = "Erreur caméra : ${exception.message}"
                                            Timber.e(exception, "Camera capture error")
                                        }
                                    }
                                )
                            } else {
                                // Enregistrer le visage
                                scope.launch {
                                    isProcessing = true
                                    error = null
                                    
                                    try {
                                        // Convertir bitmaps en ByteArray
                                        val photoBytes = capturedPhotos.map { bitmap ->
                                            val stream = ByteArrayOutputStream()
                                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                                            stream.toByteArray()
                                        }
                                        
                                        val data = FaceRegistrationData(
                                            name = name.ifBlank { null },
                                            photos = photoBytes,
                                            thumbnailPhoto = photoBytes.firstOrNull()
                                        )
                                        
                                        val result = withContext(Dispatchers.IO) {
                                            trustFacesManager.registerFace(data)
                                        }
                                        
                                        if (result.isSuccess) {
                                            onSuccess()
                                        } else {
                                            error = result.exceptionOrNull()?.message 
                                                ?: "Erreur lors de l'enregistrement"
                                        }
                                    } catch (e: Exception) {
                                        error = e.message ?: "Erreur inconnue"
                                        Timber.e(e, "Error registering face")
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(if (capturedPhotos.isNotEmpty()) 1f else 1f),
                        enabled = !isProcessing
                    ) {
                        Icon(
                            if (currentStep < 3) Icons.Default.Add else Icons.Default.Check,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (currentStep < 3) "Capturer" else "Enregistrer"
                        )
                    }
                }
            }
        }
    }
    
    // Nettoyage
    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }
}

/**
 * Prévisualisation de la caméra
 */
@Composable
fun CameraPreview(
    onImageCaptureReady: (ImageCapture, ProcessCameraProvider) -> Unit,
    lifecycleOwner: LifecycleOwner
) {
    val context = LocalContext.current
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            
            cameraProviderFuture.addListener({
                val provider = cameraProviderFuture.get()
                
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()
                
                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    
                    onImageCaptureReady(imageCapture, provider)
                } catch (e: Exception) {
                    Timber.e(e, "Camera binding failed")
                }
            }, ContextCompat.getMainExecutor(ctx))
            
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Convertit ImageProxy en Bitmap
 */
private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    
    val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    
    // Rotation si nécessaire
    val rotationDegrees = image.imageInfo.rotationDegrees
    return if (rotationDegrees != 0) {
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } else {
        bitmap
    }
}

