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
 * avec un message d'urgence et un code PIN pour débloquer.
 * 
 * Utilisé pour :
 * - Mode verrouillage instantané
 * - Mode panique
 * 
 * Désactivation :
 * - Code PIN : 1234
 */
class LockScreenOverlayView(context: Context) : FrameLayout(context) {
    
    companion object {
        private const val ANIMATION_DURATION = 200L
        private const val UNLOCK_CODE = "1234"
    }
    
    private var onDismissListener: (() -> Unit)? = null
    
    // Vues
    private val background: View
    private val contentContainer: LinearLayout
    private val iconText: TextView
    private val titleText: TextView
    private val subtitleText: TextView
    private val pinDisplay: TextView
    private val pinContainer: LinearLayout
    
    // Code PIN
    private var enteredPin = ""
    
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
            text = "Entrez le code PIN pour déverrouiller"
            textSize = 16f
            setTextColor(Color.parseColor("#AAAAAA"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }
        contentContainer.addView(subtitleText)
        
        // Affichage du PIN
        pinDisplay = TextView(context).apply {
            text = "••••"
            textSize = 32f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(32, 16, 32, 16)
            setBackgroundColor(Color.parseColor("#33FFFFFF"))
            letterSpacing = 0.3f
        }
        contentContainer.addView(pinDisplay)
        
        // Clavier numérique
        pinContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 0)
        }
        
        // Créer le clavier 3x4 (1-9, 0)
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "⌫")
        )
        
        rows.forEach { row ->
            val rowLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            
            row.forEach { digit ->
                val button = TextView(context).apply {
                    text = digit
                    textSize = 24f
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    setPadding(40, 30, 40, 30)
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        setMargins(8, 8, 8, 8)
                    }
                    
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        setColor(Color.parseColor("#3d5a80"))
                        cornerRadius = 12f
                    }
                    
                    setOnClickListener {
                        handlePinInput(digit)
                    }
                    
                    // Effet de pression
                    setOnTouchListener { v, event ->
                        when (event.action) {
                            android.view.MotionEvent.ACTION_DOWN -> {
                                alpha = 0.7f
                            }
                            android.view.MotionEvent.ACTION_UP, 
                            android.view.MotionEvent.ACTION_CANCEL -> {
                                alpha = 1.0f
                            }
                        }
                        false
                    }
                }
                rowLayout.addView(button)
            }
            
            pinContainer.addView(rowLayout)
        }
        
        contentContainer.addView(pinContainer)
        addView(contentContainer)
        
        isVisible = false
    }
    
    /**
     * Gère la saisie du code PIN
     */
    private fun handlePinInput(input: String) {
        when (input) {
            "C" -> {
                // Clear
                enteredPin = ""
                updatePinDisplay()
            }
            "⌫" -> {
                // Backspace
                if (enteredPin.isNotEmpty()) {
                    enteredPin = enteredPin.dropLast(1)
                    updatePinDisplay()
                }
            }
            else -> {
                // Chiffre
                if (enteredPin.length < 4) {
                    enteredPin += input
                    updatePinDisplay()
                    
                    // Vérifier le code si 4 chiffres
                    if (enteredPin.length == 4) {
                        checkPin()
                    }
                }
            }
        }
    }
    
    /**
     * Met à jour l'affichage du PIN
     */
    private fun updatePinDisplay() {
        val display = when (enteredPin.length) {
            0 -> "••••"
            1 -> "●•••"
            2 -> "●●••"
            3 -> "●●●•"
            4 -> "●●●●"
            else -> "••••"
        }
        pinDisplay.text = display
    }
    
    /**
     * Vérifie le code PIN
     */
    private fun checkPin() {
        if (enteredPin == UNLOCK_CODE) {
            // Code correct
            flashSuccess()
            postDelayed({
                onDismissListener?.invoke()
                enteredPin = ""
                updatePinDisplay()
            }, 300)
        } else {
            // Code incorrect
            flashError()
            enteredPin = ""
            postDelayed({
                updatePinDisplay()
            }, 500)
        }
    }
    
    /**
     * Flash de succès (vert)
     */
    private fun flashSuccess() {
        val originalColor = (background.background as? GradientDrawable)?.colors
        
        ValueAnimator.ofFloat(0f, 1f, 0f).apply {
            duration = 300
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                pinDisplay.setBackgroundColor(
                    Color.argb(
                        (progress * 100).toInt(),
                        76, 175, 80 // Vert
                    )
                )
            }
            start()
        }
    }
    
    /**
     * Flash d'erreur (rouge)
     */
    private fun flashError() {
        // Animation de secousse
        val shake = ValueAnimator.ofFloat(-10f, 10f, -10f, 10f, 0f).apply {
            duration = 400
            addUpdateListener { animator ->
                pinDisplay.translationX = animator.animatedValue as Float
            }
        }
        
        // Flash rouge
        ValueAnimator.ofFloat(0f, 1f, 0f).apply {
            duration = 400
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                pinDisplay.setBackgroundColor(
                    Color.argb(
                        (progress * 100).toInt(),
                        244, 67, 54 // Rouge
                    )
                )
            }
            start()
        }
        
        shake.start()
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


