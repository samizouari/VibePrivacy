package com.privacyguard.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.privacyguard.utils.PermissionManager

/**
 * Écran de demande de permissions
 * 
 * Explique pourquoi chaque permission est nécessaire et guide l'utilisateur
 * à travers le processus d'autorisation.
 */
@Composable
fun PermissionsScreen(
    onPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    var permissionsState by remember { 
        mutableStateOf(PermissionManager.getMissingPermissions(context))
    }
    
    // Launcher pour demander les permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Mise à jour de l'état après la demande
        permissionsState = PermissionManager.getMissingPermissions(context)
        
        // Si toutes les permissions critiques sont accordées, on continue
        if (PermissionManager.areCriticalPermissionsGranted(context)) {
            onPermissionsGranted()
        }
    }
    
    // Launcher pour les paramètres système (overlay)
    val overlaySettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // Vérifier à nouveau les permissions après retour
        permissionsState = PermissionManager.getMissingPermissions(context)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // En-tête
        Text(
            text = "🔐",
            style = MaterialTheme.typography.displayMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Permissions nécessaires",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Privacy Guard a besoin de quelques permissions pour vous protéger efficacement.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Section permissions critiques
        Text(
            text = "PERMISSIONS CRITIQUES",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        PermissionManager.CRITICAL_PERMISSIONS.forEach { permission ->
            PermissionItem(
                permission = permission,
                isGranted = PermissionManager.isPermissionGranted(context, permission),
                isCritical = true
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Section permissions optionnelles
        Text(
            text = "PERMISSIONS OPTIONNELLES",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        PermissionManager.OPTIONAL_PERMISSIONS.forEach { permission ->
            PermissionItem(
                permission = permission,
                isGranted = PermissionManager.isPermissionGranted(context, permission),
                isCritical = false
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Bouton pour demander les permissions
        if (permissionsState.isNotEmpty()) {
            Button(
                onClick = {
                    permissionLauncher.launch(permissionsState.toTypedArray())
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Autoriser les permissions")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Bouton pour les permissions en paramètres si refusé
            TextButton(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    overlaySettingsLauncher.launch(intent)
                }
            ) {
                Text("Ouvrir les paramètres")
            }
        } else if (PermissionManager.areCriticalPermissionsGranted(context)) {
            // Toutes les permissions critiques sont accordées
            Button(
                onClick = onPermissionsGranted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("✓ Continuer")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Informations supplémentaires
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🔒 Votre vie privée d'abord",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Toutes les données restent sur votre appareil\n" +
                           "• Aucune donnée n'est envoyée sur Internet\n" +
                           "• 0% de télémétrie\n" +
                           "• Vous pouvez révoquer les permissions à tout moment",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Item de permission individuel
 */
@Composable
fun PermissionItem(
    permission: String,
    isGranted: Boolean,
    isCritical: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icône de statut
            Icon(
                imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Close,
                contentDescription = if (isGranted) "Accordée" else "Non accordée",
                tint = if (isGranted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Description
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = PermissionManager.getPermissionName(permission),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (isCritical) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "REQUIS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = PermissionManager.getPermissionDescription(permission),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

