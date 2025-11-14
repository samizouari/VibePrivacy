package com.privacyguard.di

import android.content.Context
import androidx.room.Room
import com.privacyguard.data.database.PrivacyGuardDatabase
import com.privacyguard.data.database.dao.DetectionEventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PrivacyGuardDatabase {
        return Room.databaseBuilder(
            context,
            PrivacyGuardDatabase::class.java,
            "privacy_guard.db"
        ).build()
    }

    @Provides
    fun provideDetectionEventDao(database: PrivacyGuardDatabase): DetectionEventDao {
        return database.detectionEventDao()
    }
}
