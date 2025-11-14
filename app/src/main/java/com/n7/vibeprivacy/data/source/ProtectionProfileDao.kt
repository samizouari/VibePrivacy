package com.n7.vibeprivacy.data.source

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.n7.vibeprivacy.data.models.ProtectionProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProtectionProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(protectionProfile: ProtectionProfile)

    @Query("SELECT * FROM protection_profiles")
    fun getAll(): Flow<List<ProtectionProfile>>

    @Query("SELECT * FROM protection_profiles WHERE packageName = :packageName")
    fun getByPackageName(packageName: String): Flow<ProtectionProfile?>

    @Query("DELETE FROM protection_profiles WHERE id = :id")
    suspend fun deleteById(id: Int)
}