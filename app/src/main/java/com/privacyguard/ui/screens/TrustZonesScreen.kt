package com.privacyguard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.privacyguard.trust.TrustZonesManager
import com.privacyguard.trust.WiFiZoneDetector
import com.privacyguard.trust.models.TrustZone
import com.privacyguard.trust.models.TrustZoneProtection

/**
 * Écran de gestion des zones de confiance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrustZonesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddZone: () -> Unit
) {
    val context = LocalContext.current
    
    // Initialiser le manager de zones
    val wifiDetector = remember { WiFiZoneDetector(context) }
    val trustZonesManager = remember { TrustZonesManager(context, wifiDetector) }
    
    // Observer les zones
    val zones by trustZonesManager.zones.collectAsState()
    val currentZone by trustZonesManager.currentZone.collectAsState()
    val currentSsid by wifiDetector.currentSsid.collectAsState()
    
    // Démarrer le monitoring au lancement
    LaunchedEffect(Unit) {
        wifiDetector.start()
        trustZonesManager.startMonitoring()
    }
    
    // Arrêter le monitoring à la destruction
    DisposableEffect(Unit) {
        onDispose {
            wifiDetector.stop()
            trustZonesManager.stopMonitoring()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zones de confiance") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddZone,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Ajouter une zone")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // État actuel
            item {
                CurrentStatusCard(
                    currentZone = currentZone,
                    currentSsid = currentSsid
                )
            }
            
            // Liste des zones
            if (zones.isEmpty()) {
                item {
                    EmptyZonesCard(onAddZone = onNavigateToAddZone)
                }
            } else {
                items(zones, key = { it.id }) { zone ->
                    TrustZoneCard(
                        zone = zone,
                        isCurrentZone = currentZone?.id == zone.id,
                        onToggle = { trustZonesManager.toggleZone(zone.id) },
                        onDelete = { trustZonesManager.removeZone(zone.id) }
                    )
                }
            }
        }
    }
}

/**
 * Carte affichant l'état actuel
 */
@Composable
private fun CurrentStatusCard(
    currentZone: TrustZone?,
    currentSsid: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (currentZone != null) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (currentZone != null) Icons.Default.CheckCircle else Icons.Default.Home,
                    contentDescription = null,
                    tint = if (currentZone != null) Color(0xFF4CAF50) else Color.Gray
                )
                Text(
                    text = "État actuel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Divider()
            
            if (currentZone != null) {
                Text(
                    text = "✓ Zone: ${currentZone.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4CAF50)
                )
                Text(
                    text = "Protection: ${currentZone.protectionBehavior.displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "✗ Hors zone de confiance",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Protection normale active",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (currentSsid != null) {
                Text(
                    text = "📶 WiFi: $currentSsid",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Carte zone vide
 */
@Composable
private fun EmptyZonesCard(onAddZone: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAddZone),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Aucune zone de confiance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Ajoutez une zone (maison, bureau) pour adapter la protection",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onAddZone) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Ajouter une zone")
            }
        }
    }
}

/**
 * Carte d'une zone de confiance
 */
@Composable
private fun TrustZoneCard(
    zone: TrustZone,
    isCurrentZone: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentZone) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (isCurrentZone) {
                MaterialTheme.colorScheme.onTertiaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentZone) 4.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // En-tête avec nom et toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when {
                            zone.name.contains("Maison", ignoreCase = true) -> Icons.Default.Home
                            zone.name.contains("Bureau", ignoreCase = true) -> Icons.Default.Work
                            else -> Icons.Default.Place
                        },
                        contentDescription = null
                    )
                    Text(
                        text = zone.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isCurrentZone) {
                        Text(
                            text = "●",
                            color = Color(0xFF4CAF50),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
                
                Switch(
                    checked = zone.isEnabled,
                    onCheckedChange = { onToggle() }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // WiFi SSIDs
            if (zone.wifiSsids.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = zone.wifiSsids.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // GPS (si disponible)
            if (zone.latitude != null && zone.longitude != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "GPS: ${zone.latitude}, ${zone.longitude} (${zone.radiusMeters}m)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Niveau de protection
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "🛡️ ${zone.protectionBehavior.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Bouton supprimer
            TextButton(
                onClick = { showDeleteDialog = true },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Supprimer")
            }
        }
    }
    
    // Dialog de confirmation
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer la zone ?") },
            text = { Text("Voulez-vous vraiment supprimer '${zone.name}' ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

