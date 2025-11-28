package com.privacyguard.protection

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import java.text.SimpleDateFormat
import java.util.*

/**
 * Écran leurre (Decoy Screen)
 * 
 * Affiche un faux écran d'accueil qui ressemble à un
 * téléphone normal verrouillé pour tromper les observateurs.
 * 
 * Caractéristiques :
 * - Affichage de l'heure et de la date
 * - Fond neutre
 * - Pattern de déverrouillage secret pour désactiver
 */
class DecoyScreenOverlayView(context: Context) : FrameLayout(context) {
    
    companion object {
        private const val ANIMATION_DURATION = 400L
        private const val SECRET_TAP_COUNT = 5
        private const val SECRET_TAP_TIMEOUT = 2000L
    }
    
    private var onDismissListener: (() -> Unit)? = null
    
    // Vues
    private val background: View
    private val timeText: TextView
    private val dateText: TextView
    private val swipeHintText: TextView
    
    // Mise à jour de l'heure
    private val handler = Handler(Looper.getMainLooper())
    private val timeUpdateRunnable = object : Runnable {
        override fun run() {
            updateTime()
            handler.postDelayed(this, 1000)
        }
    }
    
    // Détection du pattern secret
    private var secretTapCount = 0
    private var lastSecretTapTime: Long = 0
    
    // Animation
    private var showAnimator: ValueAnimator? = null
    
    init {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        
        // Background noir
        background = View(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
            alpha = 0f
        }
        addView(background)
        
        // Container principal
        val mainContainer = LinearLayout(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        
        // Heure
        timeText = TextView(context).apply {
            text = "12:00"
            textSize = 72f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            gravity = Gravity.CENTER
            alpha = 0f
        }
        mainContainer.addView(timeText)
        
        // Date
        dateText = TextView(context).apply {
            text = "Vendredi 28 novembre"
            textSize = 18f
            setTextColor(Color.parseColor("#AAAAAA"))
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
            alpha = 0f
        }
        mainContainer.addView(dateText)
        
        // Spacer
        val spacer = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(1, 200)
        }
        mainContainer.addView(spacer)
        
        // Indice de swipe
        swipeHintText = TextView(context).apply {
            text = "Balayez vers le haut pour déverrouiller"
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            gravity = Gravity.CENTER
            alpha = 0f
        }
        mainContainer.addView(swipeHintText)
        
        addView(mainContainer)
        
        // Zone secrète pour désactiver (coin supérieur droit)
        val secretZone = View(context).apply {
            layoutParams = LayoutParams(150, 150, Gravity.TOP or Gravity.END)
            setOnClickListener {
                handleSecretTap()
            }
        }
        addView(secretZone)
        
        isVisible = false
    }
    
    /**
     * Met à jour l'heure affichée
     */
    private fun updateTime() {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE d MMMM", Locale.FRANCE)
        val now = Date()
        
        timeText.text = timeFormat.format(now)
        dateText.text = dateFormat.format(now).replaceFirstChar { it.uppercase() }
    }
    
    /**
     * Gère les taps secrets pour désactiver
     */
    private fun handleSecretTap() {
        val now = System.currentTimeMillis()
        
        if (now - lastSecretTapTime > SECRET_TAP_TIMEOUT) {
            // Reset si timeout dépassé
            secretTapCount = 0
        }
        
        secretTapCount++
        lastSecretTapTime = now
        
        if (secretTapCount >= SECRET_TAP_COUNT) {
            // Pattern secret activé !
            secretTapCount = 0
            onDismissListener?.invoke()
        }
    }
    
    /**
     * Définit le listener de fermeture
     */
    fun setOnDismissListener(listener: () -> Unit) {
        onDismissListener = listener
    }
    
    /**
     * Affiche l'écran leurre avec animation
     */
    fun show() {
        if (isVisible && background.alpha > 0) return
        
        isVisible = true
        updateTime()
        handler.post(timeUpdateRunnable)
        
        showAnimator?.cancel()
        showAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                background.alpha = progress
                timeText.alpha = progress
                dateText.alpha = progress
                swipeHintText.alpha = progress * 0.5f
                
                // Effet de zoom léger
                val scale = 0.95f + (0.05f * progress)
                timeText.scaleX = scale
                timeText.scaleY = scale
            }
            start()
        }
    }
    
    /**
     * Cache l'écran leurre avec animation
     */
    fun hide() {
        if (!isVisible) return
        
        handler.removeCallbacks(timeUpdateRunnable)
        
        showAnimator?.cancel()
        showAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = ANIMATION_DURATION / 2
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                background.alpha = progress
                timeText.alpha = progress
                dateText.alpha = progress
                swipeHintText.alpha = progress * 0.5f
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    isVisible = false
                }
            })
            start()
        }
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(timeUpdateRunnable)
        showAnimator?.cancel()
    }
}


