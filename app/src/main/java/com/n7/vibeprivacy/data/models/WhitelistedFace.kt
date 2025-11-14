package com.n7.vibeprivacy.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "whitelisted_faces")
data class WhitelistedFace(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val faceEncoding: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WhitelistedFace

        if (id != other.id) return false
        if (name != other.name) return false
        if (!faceEncoding.contentEquals(other.faceEncoding)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + faceEncoding.contentHashCode()
        return result
    }
}