package com.privacyguard.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.privacyguard.R
import com.privacyguard.ui.theme.PrivacyGuardTheme
// import dagger.hilt.android.AndroidEntryPoint // TODO: Réactiver au Jour 2

/**
 * Activité principale de Privacy Guard
 * 
 * Point d'entrée de l'application. Pour le MVP, affiche un écran simple
 * avec les informations de base et les boutons d'accès aux fonctionnalités.
 */
// @AndroidEntryPoint // TODO: Réactiver au Jour 2 quand on implémente DI
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            PrivacyGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var showPermissionsScreen by remember { mutableStateOf(false) }
    var isProtectionEnabled by remember { mutableStateOf(false) }
    
    // Vérifier les permissions au démarrage
    LaunchedEffect(Unit) {
        showPermissionsScreen = !com.privacyguard.utils.PermissionManager.areCriticalPermissionsGranted(context)
    }
    
    // Afficher l'écran de permissions si nécessaire
    if (showPermissionsScreen) {
        PermissionsScreen(
            onPermissionsGranted = {
                showPermissionsScreen = false
            }
        )
        return
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo ou icône (à ajouter plus tard)
        Text(
            text = if (isProtectionEnabled) "🛡️✅" else "🛡️",
            style = MaterialTheme.typography.displayLarge
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Bouton principal
        Button(
            onClick = { 
                isProtectionEnabled = !isProtectionEnabled
                
                // Démarrer ou arrêter le service
                if (isProtectionEnabled) {
                    com.privacyguard.service.PrivacyGuardService.startService(context)
                } else {
                    com.privacyguard.service.PrivacyGuardService.stopService(context)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = if (isProtectionEnabled) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            } else {
                ButtonDefaults.buttonColors()
            }
        ) {
            Text(
                if (isProtectionEnabled) "✓ Protection active" 
                else stringResource(R.string.start_protection)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Version et statut
        Text(
            text = "Version 1.0.0 - MVP",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Indicateur de statut
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isProtectionEnabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = if (isProtectionEnabled) {
                        "Statut : 🟢 Protection activée"
                    } else {
                        "Statut : ⚪ Protection désactivée"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Mode : Discret (MVP)",
                    style = MaterialTheme.typography.bodySmall
                )
                if (isProtectionEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "✅ Capteurs actifs : Caméra, Audio, Mouvement, Proximité",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📊 Vérifiez les logs Timber pour voir les détections",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    PrivacyGuardTheme {
        MainScreen()
    }
}
