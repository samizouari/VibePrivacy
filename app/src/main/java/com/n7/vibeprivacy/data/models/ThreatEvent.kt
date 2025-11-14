package com.n7.vibeprivacy.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "threat_events")
data class ThreatEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long,
    val threatType: String,
    val sensorData: String,
    val actionTaken: String,
    val photoPath: String? = null
)