package com.privacyguard.sensors

import android.graphics.Bitmap
import android.graphics.Rect

/**
 * Helper pour cropper un visage depuis un bitmap
 */
fun cropFaceFromBitmap(bitmap: Bitmap, boundingBox: Rect): Bitmap? {
    try {
        // S'assurer que le bounding box est dans les limites du bitmap
        val left = boundingBox.left.coerceAtLeast(0)
        val top = boundingBox.top.coerceAtLeast(0)
        val right = boundingBox.right.coerceAtMost(bitmap.width)
        val bottom = boundingBox.bottom.coerceAtMost(bitmap.height)
        
        val width = right - left
        val height = bottom - top
        
        if (width <= 0 || height <= 0) {
            return null
        }
        
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    } catch (e: Exception) {
        return null
    }
}

