package com.privacyguard.protection

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat

/**
 * Indicateur de confidentialité flottant
 * 
 * Un petit indicateur qui reste visible en haut de l'écran
 * et change de couleur selon l'état de protection :
 * - Vert (SAFE) : Pas de menace
 * - Jaune (MONITORING) : Surveillance active
 * - Rouge (THREAT) : Menace détectée
 */
class PrivacyIndicatorView(context: Context) : View(context) {
    
    companion object {
        // Couleurs
        private const val COLOR_SAFE = 0xFF4CAF50.toInt()      // Vert
        private const val COLOR_MONITORING = 0xFFFFC107.toInt() // Jaune
        private const val COLOR_THREAT = 0xFFF44336.toInt()     // Rouge
        
        // Dimensions (en dp)
        private const val WIDTH_DP = 48f
        private const val HEIGHT_DP = 8f
        private const val CORNER_RADIUS_DP = 4f
    }
    
    private val density = context.resources.displayMetrics.density
    
    private val widthPx = (WIDTH_DP * density).toInt()
    private val heightPx = (HEIGHT_DP * density).toInt()
    private val cornerRadiusPx = CORNER_RADIUS_DP * density
    
    private var currentState = IndicatorState.SAFE
    private var currentColor = COLOR_SAFE
    private var targetColor = COLOR_SAFE
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = currentColor
    }
    
    private val rect = RectF()
    
    // Animation de pulsation pour l'état THREAT
    private var pulseAnimator: ValueAnimator? = null
    private var pulseAlpha = 1f
    
    // Animation de transition de couleur
    private var colorAnimator: ValueAnimator? = null
    
    init {
        // Définir la taille de la vue
        minimumWidth = widthPx
        minimumHeight = heightPx
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(widthPx, heightPx)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        
        // Appliquer l'alpha de pulsation
        paint.alpha = (255 * pulseAlpha).toInt()
        paint.color = currentColor
        
        // Dessiner la pilule arrondie
        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint)
    }
    
    /**
     * Définit l'état de l'indicateur
     */
    fun setState(state: IndicatorState) {
        if (currentState == state) return
        
        currentState = state
        targetColor = when (state) {
            IndicatorState.SAFE -> COLOR_SAFE
            IndicatorState.MONITORING -> COLOR_MONITORING
            IndicatorState.THREAT -> COLOR_THREAT
        }
        
        // Animer la transition de couleur
        animateColorChange()
        
        // Gérer la pulsation
        when (state) {
            IndicatorState.THREAT -> startPulseAnimation()
            else -> stopPulseAnimation()
        }
    }
    
    /**
     * Anime le changement de couleur
     */
    private fun animateColorChange() {
        colorAnimator?.cancel()
        
        colorAnimator = ValueAnimator.ofArgb(currentColor, targetColor).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                currentColor = animator.animatedValue as Int
                invalidate()
            }
            start()
        }
    }
    
    /**
     * Démarre l'animation de pulsation (pour THREAT)
     */
    private fun startPulseAnimation() {
        pulseAnimator?.cancel()
        
        pulseAnimator = ValueAnimator.ofFloat(1f, 0.4f).apply {
            duration = 600
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                pulseAlpha = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
    
    /**
     * Arrête l'animation de pulsation
     */
    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        pulseAlpha = 1f
        invalidate()
    }
    
    /**
     * Nettoyage
     */
    fun cleanup() {
        colorAnimator?.cancel()
        pulseAnimator?.cancel()
    }
}


