package com.privacyguard.protection

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible

/**
 * Overlay de flou doux progressif
 * 
 * Affiche un voile semi-transparent avec un effet de flou
 * pour masquer le contenu sensible.
 * 
 * Caractéristiques :
 * - Animation d'apparition/disparition fluide
 * - Intensité configurable
 * - Affichage des raisons de déclenchement
 * - Double-tap pour désactiver
 */
class SoftBlurOverlayView(context: Context) : FrameLayout(context) {
    
    companion object {
        private const val ANIMATION_DURATION = 300L
        private const val BASE_ALPHA = 0.85f
    }
    
    private var intensity: Float = 0.5f
    private var reasons: List<String> = emptyList()
    private var onDismissListener: (() -> Unit)? = null
    
    // Vues
    private val overlayBackground: View
    private val contentContainer: LinearLayout
    private val iconText: TextView
    private val titleText: TextView
    private val reasonsText: TextView
    private val hintText: TextView
    
    // Animation
    private var showAnimator: ValueAnimator? = null
    
    // Double-tap detection
    private var lastTapTime: Long = 0
    private val doubleTapTimeout = 300L
    
    init {
        // Configuration de la vue principale
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        
        // Background avec dégradé
        overlayBackground = View(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            background = createGradientBackground()
            alpha = 0f
        }
        addView(overlayBackground)
        
        // Container pour le contenu
        contentContainer = LinearLayout(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            alpha = 0f
        }
        
        // Icône
        iconText = TextView(context).apply {
            text = "🛡️"
            textSize = 64f
            gravity = Gravity.CENTER
        }
        contentContainer.addView(iconText)
        
        // Titre
        titleText = TextView(context).apply {
            text = "Protection Active"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 8)
        }
        contentContainer.addView(titleText)
        
        // Raisons
        reasonsText = TextView(context).apply {
            text = ""
            textSize = 14f
            setTextColor(Color.parseColor("#CCFFFFFF"))
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 24)
        }
        contentContainer.addView(reasonsText)
        
        // Indice
        hintText = TextView(context).apply {
            text = "Double-tap pour désactiver"
            textSize = 12f
            setTextColor(Color.parseColor("#99FFFFFF"))
            gravity = Gravity.CENTER
        }
        contentContainer.addView(hintText)
        
        addView(contentContainer)
        
        // Gestion du double-tap
        setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastTapTime < doubleTapTimeout) {
                // Double-tap détecté
                onDismissListener?.invoke()
            }
            lastTapTime = now
        }
        
        isVisible = false
    }
    
    /**
     * Crée le background dégradé
     */
    private fun createGradientBackground(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.parseColor("#E6000000"), // Noir 90%
                Color.parseColor("#CC1a1a2e"), // Bleu foncé 80%
                Color.parseColor("#E6000000")  // Noir 90%
            )
        )
    }
    
    /**
     * Définit l'intensité du flou (0.0 à 1.0)
     */
    fun setIntensity(intensity: Float) {
        this.intensity = intensity.coerceIn(0f, 1f)
        // Ajuster l'alpha en fonction de l'intensité
        overlayBackground.alpha = BASE_ALPHA * intensity
    }
    
    /**
     * Définit les raisons du déclenchement
     */
    fun setReasons(reasons: List<String>) {
        this.reasons = reasons
        reasonsText.text = if (reasons.isNotEmpty()) {
            reasons.joinToString("\n") { "• $it" }
        } else {
            "Menace détectée"
        }
    }
    
    /**
     * Définit le listener de fermeture
     */
    fun setOnDismissListener(listener: () -> Unit) {
        onDismissListener = listener
    }
    
    /**
     * Affiche l'overlay avec animation
     */
    fun show() {
        if (isVisible && overlayBackground.alpha > 0) return
        
        isVisible = true
        
        showAnimator?.cancel()
        showAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                overlayBackground.alpha = BASE_ALPHA * intensity * progress
                contentContainer.alpha = progress
                contentContainer.translationY = 50f * (1f - progress)
            }
            start()
        }
    }
    
    /**
     * Cache l'overlay avec animation
     */
    fun hide() {
        if (!isVisible) return
        
        showAnimator?.cancel()
        showAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = ANIMATION_DURATION / 2
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                overlayBackground.alpha = BASE_ALPHA * intensity * progress
                contentContainer.alpha = progress
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    isVisible = false
                }
            })
            start()
        }
    }
}


