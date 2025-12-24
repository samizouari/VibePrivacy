package com.privacyguard.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.privacyguard.trust.FaceEncoder
import com.privacyguard.trust.FaceMatcher
import com.privacyguard.trust.TrustFacesManager
import com.privacyguard.trust.models.TrustedFace
import java.text.SimpleDateFormat
import java.util.*

/**
 * Écran de gestion des visages de confiance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrustedFacesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAdd: () -> Unit
) {
    val context = LocalContext.current
    
    // Manager des visages de confiance
    val faceEncoder = remember { FaceEncoder(context) }
    val faceMatcher = remember { FaceMatcher() }
    val trustFacesManager = remember {
        TrustFacesManager(context, faceEncoder, faceMatcher)
    }
    
    // État
    val trustedFaces by trustFacesManager.trustedFaces.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<TrustedFace?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visages de confiance") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                actions = {
                    // Bouton supprimer tout
                    if (trustedFaces.isNotEmpty()) {
                        IconButton(onClick = { showDeleteAllDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Tout supprimer",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Ajouter un visage")
            }
        }
    ) { paddingValues ->
        if (trustedFaces.isEmpty()) {
            // État vide
            EmptyFacesState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                onAddFace = onNavigateToAdd
            )
        } else {
            // Liste des visages
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info header
                item {
                    InfoCard(faceCount = trustedFaces.size)
                }
                
                // Liste des visages
                items(trustedFaces) { face ->
                    TrustedFaceCard(
                        face = face,
                        trustFacesManager = trustFacesManager,
                        onDelete = { showDeleteDialog = face }
                    )
                }
            }
        }
    }
    
    // Dialog de confirmation de suppression
    showDeleteDialog?.let { face ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Supprimer ce visage ?") },
            text = {
                Text(
                    "Voulez-vous vraiment supprimer ${face.name ?: "ce visage"} ? " +
                    "Cette action est irréversible."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        trustFacesManager.deleteFace(face.id)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Annuler")
                }
            }
        )
    }
    
    // Dialog de confirmation de suppression totale
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Supprimer tous les visages ?") },
            text = {
                Text(
                    "Voulez-vous vraiment supprimer tous les ${trustedFaces.size} visages " +
                    "de confiance ? Cette action est irréversible."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        trustFacesManager.deleteAll()
                        showDeleteAllDialog = false
                    }
                ) {
                    Text("Supprimer tout", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

/**
 * État vide - pas de visages enregistrés
 */
@Composable
fun EmptyFacesState(
    modifier: Modifier = Modifier,
    onAddFace: () -> Unit
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Face,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Aucun visage de confiance",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Ajoutez les visages de vos proches pour qu'ils ne déclenchent pas d'alertes",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onAddFace,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ajouter un visage")
        }
    }
}

/**
 * Card d'info sur les visages
 */
@Composable
fun InfoCard(faceCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "👥 $faceCount visage${if (faceCount > 1) "s" else ""} de confiance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ces personnes ne déclenchent pas d'alertes lorsqu'elles regardent votre écran",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Card représentant un visage de confiance
 */
@Composable
fun TrustedFaceCard(
    face: TrustedFace,
    trustFacesManager: TrustFacesManager,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo et info
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Miniature ou icône par défaut
                if (face.thumbnailBase64 != null) {
                    val bitmap = remember(face.thumbnailBase64) {
                        trustFacesManager.base64ToBitmap(face.thumbnailBase64)
                    }
                    
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Photo de ${face.name ?: "Inconnu"}",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        DefaultFaceIcon()
                    }
                } else {
                    DefaultFaceIcon()
                }
                
                // Infos
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = face.name ?: "Sans nom",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Ajouté le ${dateFormat.format(Date(face.createdAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (face.recognitionCount > 0) {
                        Text(
                            text = "✓ Reconnu ${face.recognitionCount} fois",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Bouton supprimer
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Supprimer",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Icône par défaut si pas de photo
 */
@Composable
fun DefaultFaceIcon() {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

