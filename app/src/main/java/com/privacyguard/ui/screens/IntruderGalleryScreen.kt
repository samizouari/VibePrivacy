package com.privacyguard.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.privacyguard.protection.IntruderCapture
import com.privacyguard.protection.IntruderPhoto
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Galerie des photos d'intrus capturés
 * 
 * Affiche :
 * - Grille des photos capturées
 * - Date et niveau de menace
 * - Possibilité de voir en grand et supprimer
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntruderGalleryScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val intruderCapture = remember { IntruderCapture(context) }
    val scope = rememberCoroutineScope()
    
    var photos by remember { mutableStateOf(intruderCapture.getIntruderPhotos()) }
    var selectedPhoto by remember { mutableStateOf<IntruderPhoto?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    
    // Charger l'image quand une photo est sélectionnée
    LaunchedEffect(selectedPhoto) {
        selectedPhoto?.let { photo ->
            selectedBitmap = intruderCapture.decryptPhoto(photo)
        }
    }
    
    // Dialog de confirmation suppression
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Supprimer toutes les photos ?") },
            text = { Text("Cette action est irréversible. Toutes les photos d'intrus seront supprimées.") },
            confirmButton = {
                Button(
                    onClick = {
                        intruderCapture.deleteAllPhotos()
                        photos = emptyList()
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Supprimer tout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
    
    // Affichage plein écran de la photo sélectionnée
    if (selectedPhoto != null && selectedBitmap != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Image
            Image(
                bitmap = selectedBitmap!!.asImageBitmap(),
                contentDescription = "Photo intrus",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            
            // Overlay avec infos
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Infos
                    Column {
                        Text(
                            text = "${selectedPhoto!!.getThreatEmoji()} ${selectedPhoto!!.threatLevel}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = selectedPhoto!!.getFormattedDate(),
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Row {
                        // Bouton supprimer
                        IconButton(
                            onClick = {
                                selectedPhoto?.let { photo ->
                                    intruderCapture.deletePhoto(photo)
                                    photos = intruderCapture.getIntruderPhotos()
                                }
                                selectedPhoto = null
                                selectedBitmap = null
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Supprimer",
                                tint = Color.Red
                            )
                        }
                        
                        // Bouton fermer
                        IconButton(
                            onClick = {
                                selectedPhoto = null
                                selectedBitmap = null
                            }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Fermer",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
        return
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📸 Photos d'intrus") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (photos.isNotEmpty()) {
                        IconButton(onClick = { showDeleteAllDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Tout supprimer",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (photos.isEmpty()) {
            // Écran vide
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "📷",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Text(
                        text = "Aucune photo d'intrus",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Les photos seront capturées automatiquement\nlors de la détection de menaces",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // Stats
                Text(
                    text = "${photos.size} photo(s) capturée(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Grille de photos
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(photos) { photo ->
                        IntruderPhotoCard(
                            photo = photo,
                            intruderCapture = intruderCapture,
                            onClick = { selectedPhoto = photo }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IntruderPhotoCard(
    photo: IntruderPhoto,
    intruderCapture: IntruderCapture,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    
    // Charger la miniature
    LaunchedEffect(photo) {
        thumbnail = intruderCapture.decryptPhoto(photo)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Image ou placeholder
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail!!.asImageBitmap(),
                    contentDescription = "Photo intrus",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            
            // Overlay avec infos
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = "${photo.getThreatEmoji()} ${photo.threatLevel}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = photo.getFormattedDate(),
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

