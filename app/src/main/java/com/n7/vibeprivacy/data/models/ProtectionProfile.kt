package com.n7.vibeprivacy.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "protection_profiles")
data class ProtectionProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val appName: String,
    val packageName: String,
    val sensitivityLevel: String,
    val protectionAction: String
)