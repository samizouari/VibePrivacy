package com.privacyguard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.privacyguard.trust.TrustZonesManager
import com.privacyguard.trust.WiFiZoneDetector
import com.privacyguard.trust.models.TrustZone
import com.privacyguard.trust.models.TrustZoneProtection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Écran d'ajout d'une zone de confiance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddZoneScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    // État du formulaire
    var zoneName by remember { mutableStateOf("") }
    var selectedWifiSsids by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedProtection by remember { mutableStateOf(TrustZoneProtection.MINIMAL) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Initialiser le manager
    val wifiDetector = remember { WiFiZoneDetector(context) }
    val trustZonesManager = remember { TrustZonesManager(context, wifiDetector) }
    
    // Observer le WiFi actuel
    val currentSsid by wifiDetector.currentSsid.collectAsState()
    
    // Démarrer la détection WiFi
    LaunchedEffect(Unit) {
        wifiDetector.start()
    }
    
    DisposableEffect(Unit) {
        onDispose {
            wifiDetector.stop()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouvelle zone") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Nom de la zone
            OutlinedTextField(
                value = zoneName,
                onValueChange = { zoneName = it },
                label = { Text("Nom de la zone") },
                placeholder = { Text("ex: Maison, Bureau") },
                leadingIcon = {
                    Icon(Icons.Default.Edit, null)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // WiFi actuel (suggestion)
            if (currentSsid != null && currentSsid !in selectedWifiSsids) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "WiFi actuel détecté",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "📶 $currentSsid",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = {
                                selectedWifiSsids = selectedWifiSsids + currentSsid!!
                            }
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Ajouter")
                        }
                    }
                }
            }
            
            // WiFi sélectionnés
            if (selectedWifiSsids.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "WiFi associés",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        selectedWifiSsids.forEach { ssid ->
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
                                        Icons.Default.Check,
                                        null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(ssid)
                                }
                                IconButton(
                                    onClick = {
                                        selectedWifiSsids = selectedWifiSsids - ssid
                                    }
                                ) {
                                    Icon(Icons.Default.Close, "Retirer")
                                }
                            }
                        }
                    }
                }
            }
            
            // Sélection du niveau de protection
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Niveau de protection",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    TrustZoneProtection.values().forEach { protection ->
                        ProtectionOption(
                            protection = protection,
                            isSelected = selectedProtection == protection,
                            onSelect = { selectedProtection = protection }
                        )
                    }
                }
            }
            
            // Message d'erreur
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            // Bouton enregistrer
            Button(
                onClick = {
                    // Validation
                    when {
                        zoneName.isBlank() -> {
                            errorMessage = "Veuillez entrer un nom pour la zone"
                        }
                        selectedWifiSsids.isEmpty() -> {
                            errorMessage = "Veuillez sélectionner au moins un WiFi"
                        }
                        else -> {
                            // Créer la zone
                            val zone = TrustZone(
                                id = "",  // Sera généré par le manager
                                name = zoneName,
                                wifiSsids = selectedWifiSsids,
                                protectionBehavior = selectedProtection
                            )
                            
                            val result = trustZonesManager.addZone(zone)
                            
                            if (result.isSuccess) {
                                showSuccessMessage = true
                                // Retour après 1 seconde
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                    kotlinx.coroutines.delay(1000)
                                    onNavigateBack()
                                }
                            } else {
                                errorMessage = result.exceptionOrNull()?.message
                                    ?: "Erreur lors de la création de la zone"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = zoneName.isNotBlank() && selectedWifiSsids.isNotEmpty()
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(8.dp))
                Text("Enregistrer")
            }
        }
        
        // Snackbar de succès
        if (showSuccessMessage) {
            Snackbar(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("✓ Zone '$zoneName' créée avec succès")
            }
        }
    }
}

/**
 * Option de protection
 */
@Composable
private fun ProtectionOption(
    protection: TrustZoneProtection,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = protection.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = protection.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
        }
    }
}



