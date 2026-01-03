package com.privacyguard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.privacyguard.assessment.models.ProtectionMode
import com.privacyguard.ui.screens.getModeEmoji

/**
 * Écran d'accueil du Dashboard
 * Permet de sélectionner un mode de protection pour voir son historique
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardHomeScreen(
    onBackClick: () -> Unit,
    onModeSelected: (ProtectionMode) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Titre
            Text(
                text = "Sélectionnez un mode de protection",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Consultez l'historique des sessions et des menaces détectées pour chaque mode",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Cartes pour chaque mode
            ProtectionMode.values().forEach { mode ->
                ModeCard(
                    mode = mode,
                    onClick = { onModeSelected(mode) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeCard(
    mode: ProtectionMode,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (mode) {
                ProtectionMode.DISCRETE -> MaterialTheme.colorScheme.primaryContainer
                ProtectionMode.BALANCED -> MaterialTheme.colorScheme.secondaryContainer
                ProtectionMode.PARANOIA -> MaterialTheme.colorScheme.tertiaryContainer
                ProtectionMode.TRUST_ZONE -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = getModeEmoji(mode),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = mode.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Seuil : ${mode.threshold}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = getModeDescription(mode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Voir",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getModeDescription(mode: ProtectionMode): String {
    return when (mode) {
        ProtectionMode.DISCRETE -> "Protection discrète pour un usage quotidien"
        ProtectionMode.BALANCED -> "Équilibre entre sécurité et ergonomie"
        ProtectionMode.PARANOIA -> "Sécurité maximale pour les situations sensibles"
        ProtectionMode.TRUST_ZONE -> "Zone de confiance - Protection minimale"
    }
}

