package com.privacyguard.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.privacyguard.data.AppDatabase
import com.privacyguard.data.SessionEntity
import com.privacyguard.data.SessionRepository
import com.privacyguard.data.ThreatEventEntity
import com.privacyguard.ui.screens.getModeEmoji
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

/**
 * Écran de détails d'une session avec timeline des événements
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: Long,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember {
        SessionRepository(AppDatabase.getInstance(context).sessionDao())
    }
    
    var session by remember { mutableStateOf<SessionEntity?>(null) }
    var events by remember { mutableStateOf<List<ThreatEventEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(sessionId) {
        Timber.d("SessionDetailScreen: Loading session $sessionId")
        
        // Charger la session
        val sessionData = repository.getSessionWithEvents(sessionId)
        session = sessionData?.session
        events = sessionData?.events ?: emptyList()
        isLoading = false
        
        Timber.d("SessionDetailScreen: Loaded session with ${events.size} events")
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détails de session") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (session == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Session introuvable")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Informations de la session
                item {
                    SessionInfoCard(session!!)
                }
                
                // Statistiques
                item {
                    SessionStatsGrid(session!!)
                }
                
                // Timeline des menaces
                item {
                    Text(
                        text = "📜 Historique des menaces",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (events.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "✅",
                                        style = MaterialTheme.typography.displayMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Aucune menace détectée",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(events) { event ->
                        ThreatEventTimelineItem(
                            event = event,
                            sessionStartTime = session!!.startTime
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SessionInfoCard(session: SessionEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(getThreatLevelColor(session.maxThreatLevel)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getThreatLevelEmoji(session.maxThreatLevel),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = getModeEmoji(androidx.compose.runtime.remember { 
                            try {
                                com.privacyguard.assessment.models.ProtectionMode.valueOf(session.protectionMode)
                            } catch (e: Exception) {
                                com.privacyguard.assessment.models.ProtectionMode.DISCRETE
                            }
                        }) + " " + session.protectionMode,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatSessionDateTime(session.startTime),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun SessionStatsGrid(session: SessionEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📊 Statistiques",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(
                    value = formatDuration(session.duration),
                    label = "Durée",
                    emoji = "⏱️"
                )
                StatColumn(
                    value = session.totalThreatsDetected.toString(),
                    label = "Menaces",
                    emoji = "⚠️"
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(
                    value = "${(session.avgThreatScore * 100).toInt()}%",
                    label = "Score moyen",
                    emoji = "📈"
                )
                StatColumn(
                    value = "${(session.maxThreatScore * 100).toInt()}%",
                    label = "Score max",
                    emoji = "🔺"
                )
            }
        }
    }
}

@Composable
fun StatColumn(value: String, label: String, emoji: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = emoji, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(4.dp))
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
fun ThreatEventTimelineItem(
    event: ThreatEventEntity,
    sessionStartTime: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Timeline visuelle (ligne verticale + point)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            // Point de timeline
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(getThreatLevelColor(event.threatLevel))
            )
            
            // Ligne de connexion
            Canvas(
                modifier = Modifier
                    .width(2.dp)
                    .height(100.dp)
            ) {
                val strokeWidth = 2.dp.toPx()
                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
            }
        }
        
        // Carte de l'événement
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = getThreatLevelColor(event.threatLevel).copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Timestamp relatif
                Text(
                    text = formatRelativeTime(event.timestamp, sessionStartTime),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Niveau et score
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = getThreatLevelEmoji(event.threatLevel),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = event.threatLevel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Text(
                        text = "${(event.threatScore * 100).toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = getThreatLevelColor(event.threatLevel)
                    )
                }
                
                // Action déclenchée
                if (event.actionTriggered != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🛡️ Action : ${formatAction(event.actionTriggered)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Raisons
                if (event.reasons != null && event.reasons.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Raisons :",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = event.reasons,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Contributions des capteurs
                Spacer(modifier = Modifier.height(8.dp))
                SensorContributions(event)
            }
        }
    }
}

@Composable
fun SensorContributions(event: ThreatEventEntity) {
    val contributions = listOf(
        Triple("📷 Caméra", event.cameraContribution, Color(0xFF2196F3)),
        Triple("🎤 Audio", event.audioContribution, Color(0xFFFF9800)),
        Triple("📱 Mouvement", event.motionContribution, Color(0xFF4CAF50)),
        Triple("👋 Proximité", event.proximityContribution, Color(0xFF9C27B0))
    ).filter { it.second > 0f }
    
    if (contributions.isNotEmpty()) {
        Column {
            Text(
                text = "Contributions :",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            contributions.forEach { (name, value, color) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(100.dp)
                    )
                    LinearProgressIndicator(
                        progress = value,
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = color
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${(value * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

private fun formatSessionDateTime(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("EEEE d MMMM yyyy à HH:mm:ss", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

private fun formatRelativeTime(eventTime: Long, sessionStartTime: Long): String {
    val diffMs = eventTime - sessionStartTime
    val seconds = (diffMs / 1000) % 60
    val minutes = (diffMs / (1000 * 60)) % 60
    val hours = (diffMs / (1000 * 60 * 60))
    
    return when {
        hours > 0 -> "+${hours}h ${minutes}min ${seconds}s"
        minutes > 0 -> "+${minutes}min ${seconds}s"
        else -> "+${seconds}s"
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    val hours = (durationMs / (1000 * 60 * 60))
    
    return when {
        hours > 0 -> "${hours}h ${minutes}min"
        minutes > 0 -> "${minutes}min ${seconds}s"
        else -> "${seconds}s"
    }
}

private fun formatAction(action: String): String {
    return when (action) {
        "SOFT_BLUR" -> "Flou progressif"
        "DECOY_SCREEN" -> "Écran leurre"
        "INSTANT_LOCK" -> "Verrouillage instantané"
        "PANIC_MODE" -> "Mode panique"
        else -> action
    }
}

private fun getThreatLevelColor(level: String): Color {
    return when (level) {
        "CRITICAL" -> Color(0xFFD32F2F)
        "HIGH" -> Color(0xFFFF6F00)
        "MEDIUM" -> Color(0xFFFFA000)
        "LOW" -> Color(0xFFFBC02D)
        else -> Color(0xFF388E3C)
    }
}

private fun getThreatLevelEmoji(level: String): String {
    return when (level) {
        "CRITICAL" -> "🚨"
        "HIGH" -> "⚠️"
        "MEDIUM" -> "⚡"
        "LOW" -> "📍"
        else -> "✅"
    }
}

