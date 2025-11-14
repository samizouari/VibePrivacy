package com.privacyguard.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detection_events")
data class DetectionEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val threatScore: Int,
    val cameraData: String?, // JSON serialized
    val audioData: String?,
    val motionData: String?,
    val protectionLevel: String,
    val wasBlocked: Boolean,
    val location: String?
)
