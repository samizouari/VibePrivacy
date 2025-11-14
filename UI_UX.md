# Interface Utilisateur et Expérience Utilisateur

## 🎨 Principes de Design

### Invisibilité par Défaut
L'application doit être **présente mais discrète**. L'utilisateur oublie qu'elle est active.

### Feedback Approprié
Chaque action doit avoir un retour clair mais non intrusif.

### Restauration Intuitive
Démasquer le contenu doit être simple et rapide.

### Confiance et Transparence
L'utilisateur doit comprendre ce qui se passe et pourquoi.

## 📱 Composants UI Principaux

### 1. Overlay Flottant Minimal

#### Indicateur d'État

```kotlin
class PrivacyIndicator : View {
    enum class State(val color: Int, val icon: Int) {
        SAFE(Color.parseColor("#4CAF50"), R.drawable.ic_eye_safe),
        MONITORING(Color.parseColor("#FFC107"), R.drawable.ic_eye_alert),
        THREAT(Color.parseColor("#F44336"), R.drawable.ic_eye_threat)
    }
    
    var currentState: State = State.SAFE
        set(value) {
            field = value
            animateStateChange(value)
        }
    
    private fun animateStateChange(newState: State) {
        // Animation de couleur
        ValueAnimator.ofArgb(currentColor, newState.color).apply {
            duration = 300
            addUpdateListener { animator ->
                setBackgroundColor(animator.animatedValue as Int)
            }
            start()
        }
        
        // Animation d'icône
        if (newState == State.THREAT) {
            startBlinking()
        } else {
            stopBlinking()
        }
    }
    
    private fun startBlinking() {
        blinkAnimator = ValueAnimator.ofFloat(1f, 0.3f).apply {
            duration = 500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { animator ->
                alpha = animator.animatedValue as Float
            }
            start()
        }
    }
}
```

#### Positionnement

```xml
<!-- Indicateur en haut d'écran, centré -->
<com.privacyguard.ui.PrivacyIndicator
    android:id="@+id/privacyIndicator"
    android:layout_width="48dp"
    android:layout_height="8dp"
    android:layout_gravity="top|center_horizontal"
    android:layout_marginTop="4dp"
    android:elevation="100dp"
    android:clickable="true"
    android:focusable="true" />
```

#### Interactions

```kotlin
class PrivacyIndicatorController {
    init {
        indicator.setOnClickListener {
            // Simple tap: afficher état actuel
            showQuickStatus()
        }
        
        indicator.setOnLongClickListener {
            // Long press: menu rapide
            showQuickSettings()
            true
        }
    }
    
    private fun showQuickStatus() {
        Toast.makeText(
            context,
            "🟢 Environnement sûr - Aucune menace",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun showQuickSettings() {
        PopupMenu(context, indicator).apply {
            inflate(R.menu.quick_settings)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.pause_30min -> pauseProtection(30.minutes)
                    R.id.change_mode -> showModeSelector()
                    R.id.open_dashboard -> openDashboard()
                    else -> false
                }
            }
            show()
        }
    }
}
```

### 2. Écran de Protection (Overlay)

#### Masquage Doux

```kotlin
class SoftBlurOverlay : FrameLayout {
    private val blurView: BlurView
    private val dimView: View
    
    fun show(animated: Boolean = true) {
        visibility = View.VISIBLE
        
        if (animated) {
            // Animation d'apparition
            ObjectAnimator.ofFloat(this, "alpha", 0f, 1f).apply {
                duration = 300
                interpolator = DecelerateInterpolator()
                start()
            }
            
            // Animation de flou progressif
            ValueAnimator.ofFloat(0f, 25f).apply {
                duration = 300
                addUpdateListener { animator ->
                    blurView.setupWith(rootView)
                        .setBlurRadius(animator.animatedValue as Float)
                }
                start()
            }
        }
    }
    
    fun hide(animated: Boolean = true) {
        if (animated) {
            ObjectAnimator.ofFloat(this, "alpha", 1f, 0f).apply {
                duration = 200
                addListener(onEnd = {
                    visibility = View.GONE
                })
                start()
            }
        } else {
            visibility = View.GONE
        }
    }
}
```

#### Écran Leurre

```kotlin
class DecoyScreenOverlay : FrameLayout {
    private val decoyContainer: ViewGroup
    
    fun showDecoyContent(type: DecoyType) {
        val decoyView = when (type) {
            DecoyType.SHOPPING_LIST -> inflateShoppingList()
            DecoyType.WIKIPEDIA -> inflateWikipedia()
            DecoyType.WEATHER -> inflateWeather()
            DecoyType.WORK_NOTES -> inflateWorkNotes()
            DecoyType.CUSTOM -> inflateCustomDecoy()
        }
        
        decoyContainer.removeAllViews()
        decoyContainer.addView(decoyView)
        
        // Transition naturelle
        animateSwitch(decoyView)
    }
    
    private fun inflateShoppingList(): View {
        return LayoutInflater.from(context)
            .inflate(R.layout.decoy_shopping_list, null).apply {
                // Populate with default items
                findViewById<RecyclerView>(R.id.shoppingList).apply {
                    adapter = ShoppingListAdapter(defaultShoppingItems)
                    layoutManager = LinearLayoutManager(context)
                }
            }
    }
    
    private fun animateSwitch(newView: View) {
        // Effet de "task switch" Android naturel
        val currentView = decoyContainer.getChildAt(0)
        
        // Animate out
        currentView?.animate()
            ?.alpha(0f)
            ?.scaleX(0.8f)
            ?.scaleY(0.8f)
            ?.setDuration(150)
            ?.withEndAction {
                decoyContainer.removeView(currentView)
                decoyContainer.addView(newView)
                
                // Animate in
                newView.alpha = 0f
                newView.scaleX = 0.9f
                newView.scaleY = 0.9f
                newView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .start()
            }
            ?.start()
    }
}
```

#### Gestes de Restauration

```kotlin
class OverlayGestureDetector(
    private val overlay: ProtectionOverlay,
    private val onRestore: () -> Unit
) : GestureDetector.SimpleOnGestureListener() {
    
    // Double tap pour restaurer
    override fun onDoubleTap(e: MotionEvent): Boolean {
        authenticateAndRestore()
        return true
    }
    
    // Geste personnalisé (ex: Z-shape swipe)
    override fun onFling(
        e1: MotionEvent,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        val path = computePath(e1, e2)
        if (matchesCustomGesture(path)) {
            authenticateAndRestore()
            return true
        }
        return false
    }
    
    private fun authenticateAndRestore() {
        // Vérifier face ID
        biometricPrompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Déverrouiller l'écran")
                .setSubtitle("Confirmer votre identité")
                .setNegativeButtonText("Annuler")
                .build(),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    onRestore()
                    overlay.hide(animated = true)
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Toast.makeText(context, "Authentification échouée", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}
```

### 3. Dashboard de Confidentialité

#### Layout Principal

```xml
<androidx.coordinatorlayout.widget.CoordinatorLayout>
    
    <!-- App Bar -->
    <com.google.android.material.appbar.AppBarLayout>
        <com.google.android.material.appbar.MaterialToolbar
            android:title="Privacy Guard"
            app:menu="@menu/dashboard_menu" />
    </com.google.android.material.appbar.AppBarLayout>
    
    <androidx.core.widget.NestedScrollView>
        <LinearLayout android:orientation="vertical">
            
            <!-- Stats du Jour -->
            <com.google.android.material.card.MaterialCardView>
                <include layout="@layout/daily_stats" />
            </com.google.android.material.card.MaterialCardView>
            
            <!-- Zones à Risque -->
            <com.google.android.material.card.MaterialCardView>
                <include layout="@layout/risk_zones" />
            </com.google.android.material.card.MaterialCardView>
            
            <!-- Timeline des Événements -->
            <com.google.android.material.card.MaterialCardView>
                <include layout="@layout/events_timeline" />
            </com.google.android.material.card.MaterialCardView>
            
            <!-- Actions Rapides -->
            <include layout="@layout/quick_actions" />
            
        </LinearLayout>
    </androidx.core.widget.NestedScrollView>
    
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

#### Stats du Jour

```kotlin
@Composable
fun DailyStatsCard(stats: DailyStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Aujourd'hui",
                style = MaterialTheme.typography.h6
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Menaces bloquées
            StatRow(
                icon = Icons.Filled.Shield,
                label = "Menaces bloquées",
                value = "${stats.threatsBlocked}",
                color = Color.Green
            )
            
            // Visages inconnus
            StatRow(
                icon = Icons.Filled.Face,
                label = "Visages inconnus détectés",
                value = "${stats.unknownFaces}",
                color = Color.Orange
            )
            
            // Pics sonores
            StatRow(
                icon = Icons.Filled.VolumeUp,
                label = "Pics sonores suspects",
                value = "${stats.suspiciousAudio}",
                color = Color.Orange
            )
            
            // Mouvements brusques
            StatRow(
                icon = Icons.Filled.Vibration,
                label = "Mouvements brusques",
                value = "${stats.suddenMovements}",
                color = Color.Yellow
            )
        }
    }
}

@Composable
fun StatRow(icon: ImageVector, label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.body1,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.h6,
            color = color
        )
    }
}
```

#### Zones à Risque

```kotlin
@Composable
fun RiskZonesCard(zones: List<RiskZoneStats>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Zones à risque",
                style = MaterialTheme.typography.h6
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Chart en barres
            BarChart(
                data = zones.map { it.incidentPercentage },
                labels = zones.map { it.name },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Liste détaillée
            zones.forEach { zone ->
                RiskZoneRow(zone)
            }
        }
    }
}

@Composable
fun RiskZoneRow(zone: RiskZoneStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(text = zone.name, style = MaterialTheme.typography.body1)
            Text(
                text = "${zone.incidentCount} incidents",
                style = MaterialTheme.typography.caption,
                color = Color.Gray
            )
        }
        
        Text(
            text = "${zone.incidentPercentage}%",
            style = MaterialTheme.typography.body1,
            color = getColorForPercentage(zone.incidentPercentage)
        )
    }
}
```

#### Timeline des Événements

```kotlin
@Composable
fun EventsTimeline(events: List<DetectionEvent>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Journal des événements",
                    style = MaterialTheme.typography.h6
                )
                
                TextButton(onClick = { /* Show all */ }) {
                    Text("Voir tout")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn {
                items(events.take(10)) { event ->
                    EventTimelineItem(event)
                }
            }
        }
    }
}

@Composable
fun EventTimelineItem(event: DetectionEvent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { /* Show details */ }
    ) {
        // Timeline indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = getThreatColor(event.threatScore),
                        shape = CircleShape
                    )
            )
            
            if (event != events.last()) {
                Divider(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp),
                    color = Color.LightGray
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Event details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.getDescription(),
                style = MaterialTheme.typography.body1
            )
            
            Text(
                text = event.getTimeAgo(),
                style = MaterialTheme.typography.caption,
                color = Color.Gray
            )
            
            if (event.location != null) {
                Text(
                    text = "📍 ${event.location}",
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray
                )
            }
        }
        
        // Threat indicator
        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .background(
                    color = getThreatColor(event.threatScore).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${event.threatScore}",
                style = MaterialTheme.typography.caption,
                color = getThreatColor(event.threatScore)
            )
        }
    }
}
```

### 4. Écran de Paramètres

#### Structure

```
Paramètres
├── Mode de Protection
│   ├── Paranoïa
│   ├── Équilibré (sélectionné)
│   ├── Discret
│   └── Zones de Confiance
│
├── Sensibilité des Capteurs
│   ├── Caméra [========|--] 80%
│   ├── Audio [=======|---] 70%
│   ├── Mouvement [======|----] 60%
│   ├── Proximité [=====|-----] 50%
│   └── Luminosité [====|------] 40%
│
├── Actions de Protection
│   ├── Type de masquage
│   │   ├── Flou doux
│   │   ├── Écran leurre (sélectionné)
│   │   ├── Verrouillage
│   │   └── Mode panique
│   ├── Délai de réaction [0.5s]
│   └── Restauration automatique [✓]
│
├── Applications Protégées
│   ├── Banking (Critique) [✓]
│   ├── WhatsApp (Critique) [✓]
│   ├── Photos (Sensible) [✓]
│   ├── Instagram (Normal) [✓]
│   └── [+ Ajouter app]
│
├── Visages de Confiance
│   ├── Marie (Conjoint) [photo]
│   ├── Jean (Famille) [photo]
│   └── [+ Ajouter personne]
│
├── Zones de Confiance
│   ├── 🏠 Maison
│   ├── 🏢 Bureau
│   └── [+ Ajouter zone]
│
├── Écrans Leurres
│   ├── Liste de courses (par défaut)
│   ├── Météo
│   ├── Notes de travail
│   └── [+ Personnaliser]
│
├── Feedback
│   ├── Vibration [✓]
│   ├── Son [  ]
│   └── Notification [✓]
│
└── Avancé
    ├── Capturer les intrus [  ]
    ├── Logs détaillés [✓]
    ├── Optimisation batterie
    └── Données et confidentialité
```

#### Implémentation (Jetpack Compose)

```kotlin
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Paramètres") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Mode de Protection
            item {
                SettingsSection(title = "Mode de Protection") {
                    ProtectionModeSelector(
                        currentMode = settings.protectionMode,
                        onModeSelected = viewModel::setProtectionMode
                    )
                }
            }
            
            // Sensibilité des Capteurs
            item {
                SettingsSection(title = "Sensibilité des Capteurs") {
                    SensorSensitivitySliders(
                        sensitivities = settings.sensorSensitivities,
                        onSensitivityChanged = viewModel::updateSensorSensitivity
                    )
                }
            }
            
            // Applications Protégées
            item {
                SettingsSection(title = "Applications Protégées") {
                    ProtectedAppsList(
                        apps = settings.protectedApps,
                        onAppToggled = viewModel::toggleAppProtection,
                        onAppConfigClicked = viewModel::openAppConfig
                    )
                }
            }
            
            // ... autres sections
        }
    }
}

@Composable
fun SensorSensitivitySliders(
    sensitivities: SensorSensitivities,
    onSensitivityChanged: (SensorType, Float) -> Unit
) {
    Column {
        SensitivitySlider(
            label = "Caméra",
            icon = Icons.Filled.Videocam,
            value = sensitivities.camera,
            onValueChange = { onSensitivityChanged(SensorType.CAMERA, it) }
        )
        
        SensitivitySlider(
            label = "Audio",
            icon = Icons.Filled.Mic,
            value = sensitivities.audio,
            onValueChange = { onSensitivityChanged(SensorType.AUDIO, it) }
        )
        
        SensitivitySlider(
            label = "Mouvement",
            icon = Icons.Filled.Vibration,
            value = sensitivities.motion,
            onValueChange = { onSensitivityChanged(SensorType.MOTION, it) }
        )
        
        SensitivitySlider(
            label = "Proximité",
            icon = Icons.Filled.TouchApp,
            value = sensitivities.proximity,
            onValueChange = { onSensitivityChanged(SensorType.PROXIMITY, it) }
        )
    }
}

@Composable
fun SensitivitySlider(
    label: String,
    icon: ImageVector,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, modifier = Modifier.weight(1f))
            Text(text = "${(value * 100).toInt()}%", fontWeight = FontWeight.Bold)
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
```

## 🎭 Animations et Transitions

### Transition vers Protection

```kotlin
class ProtectionTransitionAnimation {
    fun animate(from: View, overlay: View, level: ProtectionLevel) {
        when (level) {
            ProtectionLevel.SOFT_BLUR -> animateSoftBlur(overlay)
            ProtectionLevel.DECOY_SCREEN -> animateDecoySwitch(from, overlay)
            ProtectionLevel.INSTANT_LOCK -> animateInstantLock(from, overlay)
            ProtectionLevel.PANIC_MODE -> animatePanicMode(from)
        }
    }
    
    private fun animateSoftBlur(overlay: View) {
        // Flou progressif + fade in
        overlay.alpha = 0f
        overlay.visibility = View.VISIBLE
        
        overlay.animate()
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
    
    private fun animateDecoySwitch(from: View, overlay: View) {
        // Effet "task switch" Android
        AnimatorSet().apply {
            playSequentially(
                ObjectAnimator.ofFloat(from, "scaleX", 1f, 0.8f),
                ObjectAnimator.ofFloat(from, "scaleY", 1f, 0.8f),
                ObjectAnimator.ofFloat(from, "alpha", 1f, 0f)
            )
            duration = 150
            addListener(onEnd = {
                overlay.alpha = 0f
                overlay.scaleX = 0.9f
                overlay.scaleY = 0.9f
                overlay.visibility = View.VISIBLE
                
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(overlay, "scaleX", 0.9f, 1f),
                        ObjectAnimator.ofFloat(overlay, "scaleY", 0.9f, 1f),
                        ObjectAnimator.ofFloat(overlay, "alpha", 0f, 1f)
                    )
                    duration = 150
                    start()
                }
            })
            start()
        }
    }
}
```

### Feedback Haptique

```kotlin
class HapticFeedbackManager(private val vibrator: Vibrator) {
    
    fun onThreatDetected(level: ProtectionLevel) {
        if (!settings.hapticsEnabled) return
        
        val effect = when (level) {
            ProtectionLevel.SOFT_BLUR -> 
                VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            
            ProtectionLevel.DECOY_SCREEN -> 
                VibrationEffect.createWaveform(
                    longArrayOf(0, 50, 50, 50),
                    intArrayOf(0, 100, 0, 100),
                    -1
                )
            
            ProtectionLevel.INSTANT_LOCK -> 
                VibrationEffect.createOneShot(100, 200)
            
            ProtectionLevel.PANIC_MODE -> 
                VibrationEffect.createWaveform(
                    longArrayOf(0, 100, 50, 100),
                    intArrayOf(0, 255, 0, 255),
                    -1
                )
        }
        
        vibrator.vibrate(effect)
    }
}
```

## 🌙 Dark Mode Support

```kotlin
@Composable
fun PrivacyGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF90CAF9),
            secondary = Color(0xFF81C784),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            error = Color(0xFFCF6679)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF2196F3),
            secondary = Color(0xFF4CAF50),
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFF5F5F5),
            error = Color(0xFFB00020)
        )
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

---

Cette interface combine discrétion, efficacité et transparence pour une expérience utilisateur optimale.

