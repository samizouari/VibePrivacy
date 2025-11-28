package com.privacyguard.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import java.text.SimpleDateFormat
import java.util.*

/**
 * Écran Dashboard
 * 
 * Affiche :
 * - Statistiques en temps réel
 * - État des capteurs
 * - Historique des menaces récentes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onBackClick: () -> Unit,
    isProtectionActive: Boolean
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("privacy_guard_prefs", Context.MODE_PRIVATE) }
    
    // Charger le mode actuel
    val currentMode = remember {
        val savedMode = prefs.getString("protection_mode", ProtectionMode.DISCRETE.name)
        ProtectionMode.valueOf(savedMode ?: ProtectionMode.DISCRETE.name)
    }
    
    // Statistiques simulées (en vrai, viendraient du service)
    var stats by remember { mutableStateOf(DashboardStats()) }
    
    // Simuler des mises à jour périodiques
    LaunchedEffect(isProtectionActive) {
        if (isProtectionActive) {
            // En production, ces stats viendraient du ThreatAssessmentEngine
            stats = DashboardStats(
                sessionDuration = "5 min",
                threatsDetected = 0,
                avgThreatScore = 15,
                cameraSensorActive = true,
                audioSensorActive = true,
                motionSensorActive = true,
                proximitySensorActive = true
            )
        }
    }
    
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Statut général
            item {
                StatusCard(isProtectionActive = isProtectionActive, currentMode = currentMode)
            }
            
            // Statistiques de session
            item {
                SessionStatsCard(stats = stats)
            }
            
            // État des capteurs
            item {
                SensorsStatusCard(stats = stats)
            }
            
            // Historique des menaces (placeholder pour MVP)
            item {
                ThreatHistoryCard()
            }
        }
    }
}

@Composable
fun StatusCard(isProtectionActive: Boolean, currentMode: ProtectionMode) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isProtectionActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicateur de statut
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        if (isProtectionActive) Color(0xFF4CAF50) else Color(0xFFF44336)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isProtectionActive) "🛡️" else "⚠️",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = if (isProtectionActive) "Protection Active" else "Protection Désactivée",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Mode : ${getModeEmoji(currentMode)} ${currentMode.name}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Seuil : ${currentMode.threshold}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SessionStatsCard(stats: DashboardStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📈 Statistiques de session",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = stats.sessionDuration,
                    label = "Durée",
                    emoji = "⏱️"
                )
                StatItem(
                    value = stats.threatsDetected.toString(),
                    label = "Menaces",
                    emoji = "⚠️"
                )
                StatItem(
                    value = "${stats.avgThreatScore}%",
                    label = "Score moyen",
                    emoji = "📊"
                )
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String, emoji: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = emoji, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SensorsStatusCard(stats: DashboardStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🔧 État des capteurs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            SensorStatusRow("📷 Caméra", stats.cameraSensorActive)
            SensorStatusRow("🎤 Microphone", stats.audioSensorActive)
            SensorStatusRow("📱 Mouvement", stats.motionSensorActive)
            SensorStatusRow("👋 Proximité", stats.proximitySensorActive)
        }
    }
}

@Composable
fun SensorStatusRow(name: String, isActive: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, style = MaterialTheme.typography.bodyMedium)
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isActive) Color(0xFF4CAF50) else Color(0xFFF44336))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isActive) "Actif" else "Inactif",
                style = MaterialTheme.typography.bodySmall,
                color = if (isActive) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}

@Composable
fun ThreatHistoryCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📜 Historique des menaces",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Placeholder pour MVP
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "✅",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Aucune menace détectée",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Données des statistiques du dashboard
 */
data class DashboardStats(
    val sessionDuration: String = "0 min",
    val threatsDetected: Int = 0,
    val avgThreatScore: Int = 0,
    val cameraSensorActive: Boolean = false,
    val audioSensorActive: Boolean = false,
    val motionSensorActive: Boolean = false,
    val proximitySensorActive: Boolean = false
)

