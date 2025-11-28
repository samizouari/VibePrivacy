package com.privacyguard.protection

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible

/**
 * Écran de verrouillage de protection
 * 
 * Un écran opaque qui masque complètement le contenu
 * avec un message d'urgence.
 * 
 * Utilisé pour :
 * - Mode verrouillage instantané
 * - Mode panique
 * 
 * Désactivation :
 * - Pattern secret (3 taps rapides en bas à droite)
 */
class LockScreenOverlayView(context: Context) : FrameLayout(context) {
    
    companion object {
        private const val ANIMATION_DURATION = 200L
        private const val SECRET_TAP_COUNT = 3
        private const val SECRET_TAP_TIMEOUT = 1500L
    }
    
    private var onDismissListener: (() -> Unit)? = null
    
    // Vues
    private val background: View
    private val contentContainer: LinearLayout
    private val iconText: TextView
    private val titleText: TextView
    private val subtitleText: TextView
    
    // Détection du pattern secret
    private var secretTapCount = 0
    private var lastSecretTapTime: Long = 0
    
    // Animation
    private var showAnimator: ValueAnimator? = null
    private var pulseAnimator: ValueAnimator? = null
    
    init {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        
        // Background avec dégradé
        background = View(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    Color.parseColor("#1a1a2e"), // Bleu très foncé
                    Color.parseColor("#16213e"), // Bleu foncé
                    Color.parseColor("#0f3460")  // Bleu
                )
            )
            alpha = 0f
        }
        addView(background)
        
        // Container de contenu
        contentContainer = LinearLayout(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
            alpha = 0f
        }
        
        // Icône
        iconText = TextView(context).apply {
            text = "🔒"
            textSize = 80f
            gravity = Gravity.CENTER
        }
        contentContainer.addView(iconText)
        
        // Titre
        titleText = TextView(context).apply {
            text = "Protection Active"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 16)
        }
        contentContainer.addView(titleText)
        
        // Sous-titre
        subtitleText = TextView(context).apply {
            text = "Contenu protégé"
            textSize = 16f
            setTextColor(Color.parseColor("#AAAAAA"))
            gravity = Gravity.CENTER
        }
        contentContainer.addView(subtitleText)
        
        addView(contentContainer)
        
        // Zone secrète pour désactiver (coin inférieur droit)
        val secretZone = View(context).apply {
            layoutParams = LayoutParams(200, 200, Gravity.BOTTOM or Gravity.END)
            setOnClickListener {
                handleSecretTap()
            }
        }
        addView(secretZone)
        
        isVisible = false
    }
    
    /**
     * Gère les taps secrets pour désactiver
     */
    private fun handleSecretTap() {
        val now = System.currentTimeMillis()
        
        if (now - lastSecretTapTime > SECRET_TAP_TIMEOUT) {
            secretTapCount = 0
        }
        
        secretTapCount++
        lastSecretTapTime = now
        
        // Feedback visuel
        flashFeedback()
        
        if (secretTapCount >= SECRET_TAP_COUNT) {
            secretTapCount = 0
            onDismissListener?.invoke()
        }
    }
    
    /**
     * Flash de feedback visuel lors des taps
     */
    private fun flashFeedback() {
        ValueAnimator.ofFloat(1f, 0.8f, 1f).apply {
            duration = 100
            addUpdateListener { animator ->
                background.alpha = animator.animatedValue as Float
            }
            start()
        }
    }
    
    /**
     * Définit le listener de fermeture
     */
    fun setOnDismissListener(listener: () -> Unit) {
        onDismissListener = listener
    }
    
    /**
     * Affiche l'écran de verrouillage avec animation rapide
     */
    fun show() {
        if (isVisible && background.alpha > 0) return
        
        isVisible = true
        
        showAnimator?.cancel()
        showAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                background.alpha = progress
                contentContainer.alpha = progress
                contentContainer.scaleX = 0.9f + (0.1f * progress)
                contentContainer.scaleY = 0.9f + (0.1f * progress)
            }
            start()
        }
        
        // Démarrer l'animation de pulsation de l'icône
        startPulseAnimation()
    }
    
    /**
     * Cache l'écran de verrouillage avec animation
     */
    fun hide() {
        if (!isVisible) return
        
        pulseAnimator?.cancel()
        
        showAnimator?.cancel()
        showAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                background.alpha = progress
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
    
    /**
     * Animation de pulsation de l'icône
     */
    private fun startPulseAnimation() {
        pulseAnimator?.cancel()
        
        pulseAnimator = ValueAnimator.ofFloat(1f, 1.1f).apply {
            duration = 800
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val scale = animator.animatedValue as Float
                iconText.scaleX = scale
                iconText.scaleY = scale
            }
            start()
        }
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        showAnimator?.cancel()
        pulseAnimator?.cancel()
    }
}


