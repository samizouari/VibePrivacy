package com.n7.vibeprivacy.data.source

import com.n7.vibeprivacy.data.models.ProtectionProfile
import com.n7.vibeprivacy.data.models.ThreatEvent
import com.n7.vibeprivacy.data.models.WhitelistedFace
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface Repository {
    fun getAllThreatEvents(): Flow<List<ThreatEvent>>
    suspend fun insertThreatEvent(threatEvent: ThreatEvent)
    suspend fun deleteAllThreatEvents()

    fun getAllProtectionProfiles(): Flow<List<ProtectionProfile>>
    fun getProtectionProfileByPackageName(packageName: String): Flow<ProtectionProfile?>
    suspend fun insertProtectionProfile(protectionProfile: ProtectionProfile)
    suspend fun deleteProtectionProfileById(id: Int)

    fun getAllWhitelistedFaces(): Flow<List<WhitelistedFace>>
    suspend fun insertWhitelistedFace(whitelistedFace: WhitelistedFace)
    suspend fun deleteWhitelistedFaceById(id: Int)
}

@Singleton
class AppRepository @Inject constructor(
    private val threatEventDao: ThreatEventDao,
    private val protectionProfileDao: ProtectionProfileDao,
    private val whitelistedFaceDao: WhitelistedFaceDao
) : Repository {
    override fun getAllThreatEvents(): Flow<List<ThreatEvent>> = threatEventDao.getAll()
    override suspend fun insertThreatEvent(threatEvent: ThreatEvent) = threatEventDao.insert(threatEvent)
    override suspend fun deleteAllThreatEvents() = threatEventDao.deleteAll()

    override fun getAllProtectionProfiles(): Flow<List<ProtectionProfile>> = protectionProfileDao.getAll()
    override fun getProtectionProfileByPackageName(packageName: String): Flow<ProtectionProfile?> = protectionProfileDao.getByPackageName(packageName)
    override suspend fun insertProtectionProfile(protectionProfile: ProtectionProfile) = protectionProfileDao.insert(protectionProfile)
    override suspend fun deleteProtectionProfileById(id: Int) = protectionProfileDao.deleteById(id)

    override fun getAllWhitelistedFaces(): Flow<List<WhitelistedFace>> = whitelistedFaceDao.getAll()
    override suspend fun insertWhitelistedFace(whitelistedFace: WhitelistedFace) = whitelistedFaceDao.insert(whitelistedFace)
    override suspend fun deleteWhitelistedFaceById(id: Int) = whitelistedFaceDao.deleteById(id)
}