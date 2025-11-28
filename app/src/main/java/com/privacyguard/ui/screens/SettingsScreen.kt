package com.privacyguard.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.privacyguard.assessment.models.ProtectionMode

/**
 * Écran de paramètres
 * 
 * Permet de :
 * - Sélectionner le mode de protection
 * - Voir les seuils de chaque mode
 * - Configurer les préférences
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onModeChanged: (ProtectionMode) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("privacy_guard_prefs", Context.MODE_PRIVATE) }
    
    // Charger le mode actuel depuis les préférences
    var selectedMode by remember {
        val savedMode = prefs.getString("protection_mode", ProtectionMode.DISCRETE.name)
        mutableStateOf(ProtectionMode.valueOf(savedMode ?: ProtectionMode.DISCRETE.name))
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ Paramètres") },
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
            // Section Mode de protection
            Text(
                text = "Mode de protection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Choisissez la sensibilité de détection des menaces",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Liste des modes
            ProtectionMode.values().forEach { mode ->
                ProtectionModeCard(
                    mode = mode,
                    isSelected = mode == selectedMode,
                    onClick = {
                        selectedMode = mode
                        // Sauvegarder dans les préférences
                        prefs.edit().putString("protection_mode", mode.name).apply()
                        onModeChanged(mode)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Info sur le mode actuel
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
                        text = "📊 Mode actuel : ${getModeEmoji(selectedMode)} ${selectedMode.name}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Seuil de déclenchement : ${selectedMode.threshold}/100",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = selectedMode.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Note de bas de page
            Text(
                text = "💡 Le mode Discret est recommandé pour un usage quotidien",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
fun ProtectionModeCard(
    mode: ProtectionMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }
    
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji du mode
            Text(
                text = getModeEmoji(mode),
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Infos du mode
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mode.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = mode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Seuil : ${mode.threshold}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = getModeColor(mode)
                )
            }
            
            // Icône de sélection
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Sélectionné",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

fun getModeEmoji(mode: ProtectionMode): String {
    return when (mode) {
        ProtectionMode.PARANOIA -> "🔴"
        ProtectionMode.BALANCED -> "🟡"
        ProtectionMode.DISCRETE -> "🟢"
        ProtectionMode.TRUST_ZONE -> "⚪"
    }
}

@Composable
fun getModeColor(mode: ProtectionMode): Color {
    return when (mode) {
        ProtectionMode.PARANOIA -> Color(0xFFF44336)
        ProtectionMode.BALANCED -> Color(0xFFFFC107)
        ProtectionMode.DISCRETE -> Color(0xFF4CAF50)
        ProtectionMode.TRUST_ZONE -> Color(0xFF9E9E9E)
    }
}

