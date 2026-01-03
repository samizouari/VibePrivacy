package com.privacyguard.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.ui.viewinterop.AndroidView
import com.privacyguard.R
import com.privacyguard.assessment.models.ProtectionMode
import com.privacyguard.ui.screens.AddZoneScreen
import com.privacyguard.ui.screens.AddTrustedFaceScreen
import com.privacyguard.ui.screens.DashboardHomeScreen
import com.privacyguard.ui.screens.SessionListScreen
import com.privacyguard.ui.screens.SessionDetailScreen
import com.privacyguard.ui.screens.IntruderGalleryScreen
import com.privacyguard.ui.screens.SettingsScreen
import com.privacyguard.ui.screens.TrustZonesScreen
import com.privacyguard.ui.screens.TrustedFacesScreen
import com.privacyguard.ui.theme.PrivacyGuardTheme
import timber.log.Timber
// import dagger.hilt.android.AndroidEntryPoint // TODO: Réactiver au Jour 2

/**
 * Écrans de l'application
 */
enum class Screen {
    HOME,
    SETTINGS,
    DASHBOARD_HOME,
    SESSION_LIST,
    SESSION_DETAIL,
    INTRUDER_GALLERY,
    TRUST_ZONES,
    ADD_ZONE,
    TRUSTED_FACES,
    ADD_TRUSTED_FACE
}

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
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var showOverlayDialog by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    
    // Paramètres de navigation pour le Dashboard
    var selectedMode by remember { mutableStateOf<ProtectionMode?>(null) }
    var selectedSessionId by remember { mutableStateOf<Long?>(null) }
    
    // Charger le mode depuis les préférences
    val prefs = remember { context.getSharedPreferences("privacy_guard_prefs", Context.MODE_PRIVATE) }
    var currentMode by remember {
        val savedMode = prefs.getString("protection_mode", ProtectionMode.DISCRETE.name)
        mutableStateOf(ProtectionMode.valueOf(savedMode ?: ProtectionMode.DISCRETE.name))
    }
    
    // Vérifier les permissions au démarrage
    LaunchedEffect(Unit) {
        showPermissionsScreen = !com.privacyguard.utils.PermissionManager.areCriticalPermissionsGranted(context)
        hasOverlayPermission = Settings.canDrawOverlays(context)
    }
    
    // Afficher l'écran de permissions si nécessaire
    if (showPermissionsScreen) {
        PermissionsScreen(
            onPermissionsGranted = {
                showPermissionsScreen = false
                hasOverlayPermission = Settings.canDrawOverlays(context)
            }
        )
        return
    }
    
    // Navigation entre écrans
    when (currentScreen) {
        Screen.SETTINGS -> {
            SettingsScreen(
                onBackClick = { currentScreen = Screen.HOME },
                onModeChanged = { mode ->
                    currentMode = mode
                    Timber.i("Mode changed to: ${mode.name}")
                }
            )
            return
        }
        Screen.DASHBOARD_HOME -> {
            DashboardHomeScreen(
                onBackClick = { currentScreen = Screen.HOME },
                onModeSelected = { mode ->
                    selectedMode = mode
                    currentScreen = Screen.SESSION_LIST
                }
            )
            return
        }
        Screen.SESSION_LIST -> {
            selectedMode?.let { mode ->
                SessionListScreen(
                    mode = mode,
                    onBackClick = { currentScreen = Screen.DASHBOARD_HOME },
                    onSessionClick = { sessionId ->
                        selectedSessionId = sessionId
                        currentScreen = Screen.SESSION_DETAIL
                    }
                )
            } ?: run {
                // Fallback si le mode n'est pas défini
                currentScreen = Screen.DASHBOARD_HOME
            }
            return
        }
        Screen.SESSION_DETAIL -> {
            selectedSessionId?.let { sessionId ->
                SessionDetailScreen(
                    sessionId = sessionId,
                    onBackClick = { currentScreen = Screen.SESSION_LIST }
                )
            } ?: run {
                // Fallback si la session n'est pas définie
                currentScreen = Screen.SESSION_LIST
            }
            return
        }
        Screen.TRUST_ZONES -> {
            TrustZonesScreen(
                onNavigateBack = { currentScreen = Screen.HOME },
                onNavigateToAddZone = { currentScreen = Screen.ADD_ZONE }
            )
            return
        }
        Screen.ADD_ZONE -> {
            AddZoneScreen(
                onNavigateBack = { currentScreen = Screen.TRUST_ZONES }
            )
            return
        }
        Screen.TRUSTED_FACES -> {
            TrustedFacesScreen(
                onNavigateBack = { currentScreen = Screen.HOME },
                onNavigateToAdd = { currentScreen = Screen.ADD_TRUSTED_FACE }
            )
            return
        }
        Screen.ADD_TRUSTED_FACE -> {
            AddTrustedFaceScreen(
                onNavigateBack = { currentScreen = Screen.TRUSTED_FACES },
                onSuccess = { currentScreen = Screen.TRUSTED_FACES }
            )
            return
        }
        Screen.INTRUDER_GALLERY -> {
            IntruderGalleryScreen(
                onBackClick = { currentScreen = Screen.HOME }
            )
            return
        }
        Screen.HOME -> { /* Continue below */ }
    }
    
    // Dialog pour demander la permission overlay
    if (showOverlayDialog) {
        AlertDialog(
            onDismissRequest = { showOverlayDialog = false },
            title = { Text("Permission requise") },
            text = { 
                Text("Pour afficher les overlays de protection (flou, écran leurre), " +
                     "Privacy Guard a besoin de la permission d'affichage par-dessus les autres applications.\n\n" +
                     "Sans cette permission, la protection sera limitée aux notifications.")
            },
            confirmButton = {
                Button(onClick = {
                    showOverlayDialog = false
                    // Ouvrir les paramètres de permission overlay
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }) {
                    Text("Autoriser")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showOverlayDialog = false
                    // Démarrer quand même le service sans overlay
                    isProtectionEnabled = true
                    com.privacyguard.service.PrivacyGuardService.startService(context)
                }) {
                    Text("Plus tard")
                }
            }
        )
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
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Afficher le score de menace si protection active
        if (isProtectionEnabled) {
            ThreatScoreCard()
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Bouton principal
        Button(
            onClick = { 
                if (isProtectionEnabled) {
                    // Arrêter la protection
                    isProtectionEnabled = false
                    com.privacyguard.service.PrivacyGuardService.stopService(context)
                } else {
                    // Vérifier la permission overlay avant de démarrer
                    hasOverlayPermission = Settings.canDrawOverlays(context)
                    if (!hasOverlayPermission) {
                        // Afficher le dialog pour demander la permission
                        showOverlayDialog = true
                    } else {
                        // Démarrer la protection avec overlay
                        isProtectionEnabled = true
                        com.privacyguard.service.PrivacyGuardService.startService(context)
                    }
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
        
        // Boutons de test overlay (seulement si protection active et permission accordée)
        if (isProtectionEnabled && hasOverlayPermission) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Variable pour stocker le test overlay manager
            var testOverlayManager by remember { mutableStateOf<com.privacyguard.protection.OverlayManager?>(null) }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Test flou
                OutlinedButton(
                    onClick = {
                        // Nettoyer l'ancien si existant
                        testOverlayManager?.cleanup()
                        
                        // Créer un nouveau OverlayManager pour tester
                        testOverlayManager = com.privacyguard.protection.OverlayManager(context).apply {
                            onOverlayDismissed = {
                                Timber.d("Test overlay dismissed")
                                cleanup()
                                testOverlayManager = null
                            }
                            showBlurOverlay(0.8f, listOf("Test: Double-tap pour fermer"))
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🔵 Test Flou", style = MaterialTheme.typography.bodySmall)
                }
                
                // Test écran leurre
                OutlinedButton(
                    onClick = {
                        // Nettoyer l'ancien si existant
                        testOverlayManager?.cleanup()
                        
                        testOverlayManager = com.privacyguard.protection.OverlayManager(context).apply {
                            onOverlayDismissed = {
                                Timber.d("Test overlay dismissed")
                                cleanup()
                                testOverlayManager = null
                            }
                            showDecoyScreen()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📱 Test Leurre", style = MaterialTheme.typography.bodySmall)
                }
            }
            
            Text(
                text = "💡 Flou: double-tap | Leurre: 5 taps coin ↗",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        // Boutons de navigation
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Bouton Paramètres
            OutlinedButton(
                onClick = { currentScreen = Screen.SETTINGS },
                modifier = Modifier.weight(1f)
            ) {
                Text("⚙️ Paramètres")
            }
            
            // Bouton Dashboard
            OutlinedButton(
                onClick = { currentScreen = Screen.DASHBOARD_HOME },
                modifier = Modifier.weight(1f)
            ) {
                Text("📊 Dashboard")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Bouton Zones de Confiance
        OutlinedButton(
            onClick = { currentScreen = Screen.TRUST_ZONES },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🏠 Zones de Confiance")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Bouton Visages de Confiance
        OutlinedButton(
            onClick = { currentScreen = Screen.TRUSTED_FACES },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("👤 Visages de Confiance")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Bouton Galerie Intrus
        OutlinedButton(
            onClick = { currentScreen = Screen.INTRUDER_GALLERY },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📸 Photos d'intrus")
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
                    text = "Mode : ${currentMode.name} (seuil ${currentMode.threshold}%)",
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
                        text = if (hasOverlayPermission) {
                            "🖼️ Overlays : Activés (flou, écran leurre)"
                        } else {
                            "⚠️ Overlays : Désactivés (protection par notification)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasOverlayPermission) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
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
        
        // Afficher la prévisualisation de la caméra avec détection de visages si protection activée
        if (isProtectionEnabled) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Utiliser Box au lieu de Card pour éviter les problèmes de fond
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                CameraPreviewWithFaceDetection(
                    modifier = Modifier.fillMaxSize(),
                    onFacesDetected = { faces ->
                        // Optionnel: Mettre à jour l'UI avec le nombre de visages
                        Timber.d("MainActivity: ${faces.size} visage(s) détecté(s) dans la preview")
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "💡 Prévisualisation debug : Les visages détectés sont encadrés en vert",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

/**
 * Composant affichant le score de menace en temps réel
 */
@Composable
fun ThreatScoreCard() {
    val context = LocalContext.current
    var threatScore by remember { mutableStateOf(0) }
    var threatLevel by remember { mutableStateOf("NONE") }
    var shouldTrigger by remember { mutableStateOf(false) }
    
    // Écouter les broadcasts du service
    androidx.compose.runtime.DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == com.privacyguard.service.PrivacyGuardService.ACTION_THREAT_ASSESSMENT_UPDATE) {
                    threatScore = intent.getIntExtra(
                        com.privacyguard.service.PrivacyGuardService.EXTRA_THREAT_SCORE, 
                        0
                    )
                    threatLevel = intent.getStringExtra(
                        com.privacyguard.service.PrivacyGuardService.EXTRA_THREAT_LEVEL
                    ) ?: "NONE"
                    shouldTrigger = intent.getBooleanExtra(
                        com.privacyguard.service.PrivacyGuardService.EXTRA_SHOULD_TRIGGER,
                        false
                    )
                    Timber.d("ThreatScoreCard: Received update - Score=$threatScore, Level=$threatLevel")
                }
            }
        }
        
        val filter = android.content.IntentFilter(
            com.privacyguard.service.PrivacyGuardService.ACTION_THREAT_ASSESSMENT_UPDATE
        )
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                threatScore < 25 -> MaterialTheme.colorScheme.primaryContainer
                threatScore < 65 -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Score de Menace",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Score affiché en grand
            Text(
                text = "$threatScore",
                style = MaterialTheme.typography.displayMedium,
                color = when {
                    threatScore < 25 -> MaterialTheme.colorScheme.primary
                    threatScore < 65 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                }
            )
            
            Text(
                text = "/100",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Niveau de menace
            Text(
                text = when {
                    threatScore < 25 -> "✅ SÉCURISÉ"
                    threatScore < 45 -> "🟢 FAIBLE"
                    threatScore < 65 -> "🟡 MOYEN"
                    threatScore < 85 -> "🔴 ÉLEVÉ"
                    else -> "⚠️ CRITIQUE"
                },
                style = MaterialTheme.typography.labelLarge
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Info capteurs
            Text(
                text = "4 capteurs actifs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Note explicative
            Text(
                text = "💡 Le score est calculé en temps réel à partir de la caméra, l'audio, les mouvements et la proximité",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
