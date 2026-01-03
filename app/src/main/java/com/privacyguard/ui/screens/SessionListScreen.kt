package com.privacyguard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
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
import com.privacyguard.data.AppDatabase
import com.privacyguard.data.SessionEntity
import com.privacyguard.data.SessionRepository
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

/**
 * Écran affichant la liste des sessions pour un mode donné
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(
    mode: ProtectionMode,
    onBackClick: () -> Unit,
    onSessionClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val repository = remember {
        SessionRepository(AppDatabase.getInstance(context).sessionDao())
    }
    
    // Liste des sessions pour ce mode
    var sessions by remember { mutableStateOf<List<SessionEntity>>(emptyList()) }
    
    LaunchedEffect(mode) {
        Timber.d("SessionListScreen: Loading sessions for mode ${mode.name}")
        repository.getSessionsByMode(mode.name).collectLatest { sessionList ->
            sessions = sessionList
            Timber.d("SessionListScreen: Loaded ${sessionList.size} sessions")
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Sessions ${mode.name}")
                        Text(
                            text = "${sessions.size} session(s)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (sessions.isEmpty()) {
            // État vide
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📂",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Aucune session enregistrée",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Activez la protection en mode ${mode.name} pour commencer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Liste des sessions
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(sessions) { session ->
                    SessionCard(
                        session = session,
                        onClick = { onSessionClick(session.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionCard(
    session: SessionEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicateur de niveau de menace max
            Box(
                modifier = Modifier
                    .size(50.dp)
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
            
            Column(modifier = Modifier.weight(1f)) {
                // Date/heure
                Text(
                    text = formatSessionDate(session.startTime),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Durée et stats
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "⏱️ ${formatDuration(session.duration)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "⚠️ ${session.totalThreatsDetected}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Score moyen
                if (session.avgThreatScore > 0) {
                    Text(
                        text = "Score moyen : ${(session.avgThreatScore * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Voir détails",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatSessionDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("d MMM yyyy • HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    val hours = (durationMs / (1000 * 60 * 60))
    
    return when {
        hours > 0 -> "${hours}h ${minutes}min"
        minutes > 0 -> "${minutes}min"
        else -> "${seconds}s"
    }
}

private fun getThreatLevelColor(level: String): Color {
    return when (level) {
        "CRITICAL" -> Color(0xFFD32F2F) // Rouge foncé
        "HIGH" -> Color(0xFFFF6F00) // Orange foncé
        "MEDIUM" -> Color(0xFFFFA000) // Orange
        "LOW" -> Color(0xFFFBC02D) // Jaune
        else -> Color(0xFF388E3C) // Vert (NONE)
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

