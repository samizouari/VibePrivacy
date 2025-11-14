package com.n7.vibeprivacy.data.source

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.n7.vibeprivacy.data.models.WhitelistedFace
import kotlinx.coroutines.flow.Flow

@Dao
interface WhitelistedFaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(whitelistedFace: WhitelistedFace)

    @Query("SELECT * FROM whitelisted_faces")
    fun getAll(): Flow<List<WhitelistedFace>>

    @Query("DELETE FROM whitelisted_faces WHERE id = :id")
    suspend fun deleteById(id: Int)
}