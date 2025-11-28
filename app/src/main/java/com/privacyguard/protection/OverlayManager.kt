package com.privacyguard.protection

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Gestionnaire des overlays de protection
 * 
 * Responsabilités :
 * - Gérer la permission SYSTEM_ALERT_WINDOW
 * - Afficher/masquer les différents overlays
 * - Coordonner les transitions entre overlays
 * 
 * Types d'overlays :
 * - PrivacyIndicator : Petit indicateur flottant (toujours visible)
 * - SoftBlurOverlay : Flou progressif
 * - DecoyScreenOverlay : Écran leurre
 * - LockScreenOverlay : Écran de verrouillage
 */
class OverlayManager(private val context: Context) {
    
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    
    // Overlays
    private var indicatorView: View? = null
    private var blurOverlayView: View? = null
    private var decoyOverlayView: View? = null
    private var lockOverlayView: View? = null
    
    // État de l'indicateur
    private val _indicatorState = MutableStateFlow(IndicatorState.SAFE)
    val indicatorState: StateFlow<IndicatorState> = _indicatorState.asStateFlow()
    
    // Callback pour les événements d'overlay
    var onOverlayDismissed: (() -> Unit)? = null
    
    /**
     * Vérifie si la permission d'overlay est accordée
     */
    fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(context)
    }
    
    /**
     * Initialise l'indicateur de confidentialité (petit point flottant)
     */
    fun initializeIndicator() {
        if (!hasOverlayPermission()) {
            Timber.w("OverlayManager: No overlay permission, skipping indicator")
            return
        }
        
        if (indicatorView != null) {
            Timber.d("OverlayManager: Indicator already initialized")
            return
        }
        
        try {
            indicatorView = PrivacyIndicatorView(context).apply {
                setState(IndicatorState.SAFE)
            }
            
            val params = createIndicatorLayoutParams()
            windowManager.addView(indicatorView, params)
            
            Timber.i("OverlayManager: Indicator initialized")
        } catch (e: Exception) {
            Timber.e(e, "OverlayManager: Failed to initialize indicator")
        }
    }
    
    /**
     * Met à jour l'état de l'indicateur
     */
    fun updateIndicator(state: IndicatorState) {
        _indicatorState.value = state
        (indicatorView as? PrivacyIndicatorView)?.setState(state)
        Timber.d("OverlayManager: Indicator state updated to $state")
    }
    
    /**
     * Affiche l'overlay de flou
     */
    fun showBlurOverlay(intensity: Float, reasons: List<String>) {
        if (!hasOverlayPermission()) {
            Timber.w("OverlayManager: No overlay permission")
            return
        }
        
        // Retirer les autres overlays d'abord
        hideDecoyOverlay()
        hideLockOverlay()
        
        try {
            if (blurOverlayView == null) {
                blurOverlayView = SoftBlurOverlayView(context).apply {
                    setOnDismissListener {
                        hideBlurOverlay()
                        onOverlayDismissed?.invoke()
                    }
                }
                
                val params = createFullscreenLayoutParams()
                windowManager.addView(blurOverlayView, params)
            }
            
            (blurOverlayView as? SoftBlurOverlayView)?.apply {
                setIntensity(intensity)
                setReasons(reasons)
                show()
            }
            
            Timber.i("OverlayManager: Blur overlay shown (intensity=$intensity)")
        } catch (e: Exception) {
            Timber.e(e, "OverlayManager: Failed to show blur overlay")
        }
    }
    
    /**
     * Cache l'overlay de flou
     */
    fun hideBlurOverlay() {
        (blurOverlayView as? SoftBlurOverlayView)?.hide()
        Timber.d("OverlayManager: Blur overlay hidden")
    }
    
    /**
     * Affiche l'écran leurre
     */
    fun showDecoyScreen() {
        if (!hasOverlayPermission()) {
            Timber.w("OverlayManager: No overlay permission")
            return
        }
        
        // Retirer les autres overlays d'abord
        hideBlurOverlay()
        hideLockOverlay()
        
        try {
            if (decoyOverlayView == null) {
                decoyOverlayView = DecoyScreenOverlayView(context).apply {
                    setOnDismissListener {
                        hideDecoyOverlay()
                        onOverlayDismissed?.invoke()
                    }
                }
                
                val params = createFullscreenLayoutParams()
                windowManager.addView(decoyOverlayView, params)
            }
            
            (decoyOverlayView as? DecoyScreenOverlayView)?.show()
            
            Timber.i("OverlayManager: Decoy screen shown")
        } catch (e: Exception) {
            Timber.e(e, "OverlayManager: Failed to show decoy screen")
        }
    }
    
    /**
     * Cache l'écran leurre
     */
    fun hideDecoyOverlay() {
        (decoyOverlayView as? DecoyScreenOverlayView)?.hide()
        Timber.d("OverlayManager: Decoy overlay hidden")
    }
    
    /**
     * Affiche l'écran de verrouillage
     */
    fun showLockScreen() {
        if (!hasOverlayPermission()) {
            Timber.w("OverlayManager: No overlay permission")
            return
        }
        
        // Retirer les autres overlays d'abord
        hideBlurOverlay()
        hideDecoyOverlay()
        
        try {
            if (lockOverlayView == null) {
                lockOverlayView = LockScreenOverlayView(context).apply {
                    setOnDismissListener {
                        hideLockOverlay()
                        onOverlayDismissed?.invoke()
                    }
                }
                
                val params = createFullscreenLayoutParams()
                windowManager.addView(lockOverlayView, params)
            }
            
            (lockOverlayView as? LockScreenOverlayView)?.show()
            
            Timber.i("OverlayManager: Lock screen shown")
        } catch (e: Exception) {
            Timber.e(e, "OverlayManager: Failed to show lock screen")
        }
    }
    
    /**
     * Cache l'écran de verrouillage
     */
    fun hideLockOverlay() {
        (lockOverlayView as? LockScreenOverlayView)?.hide()
        Timber.d("OverlayManager: Lock overlay hidden")
    }
    
    /**
     * Cache tous les overlays de protection
     */
    fun hideAllOverlays() {
        hideBlurOverlay()
        hideDecoyOverlay()
        hideLockOverlay()
        Timber.i("OverlayManager: All overlays hidden")
    }
    
    /**
     * Crée les paramètres de layout pour l'indicateur
     */
    private fun createIndicatorLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 50 // Petit offset depuis le haut
        }
    }
    
    /**
     * Crée les paramètres de layout pour un overlay plein écran
     */
    private fun createFullscreenLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }
    
    /**
     * Nettoyage complet
     */
    fun cleanup() {
        try {
            indicatorView?.let { windowManager.removeView(it) }
            blurOverlayView?.let { windowManager.removeView(it) }
            decoyOverlayView?.let { windowManager.removeView(it) }
            lockOverlayView?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            Timber.e(e, "OverlayManager: Error during cleanup")
        }
        
        indicatorView = null
        blurOverlayView = null
        decoyOverlayView = null
        lockOverlayView = null
        
        Timber.i("OverlayManager: Cleanup complete")
    }
}


