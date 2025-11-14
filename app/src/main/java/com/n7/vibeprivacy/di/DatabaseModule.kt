package com.n7.vibeprivacy.di

import android.content.Context
import androidx.room.Room
import com.n7.vibeprivacy.data.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object
DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "vibe_privacy_database"
        ).build()
    }

    @Provides
    fun provideThreatEventDao(appDatabase: AppDatabase) = appDatabase.threatEventDao()

    @Provides
    fun provideProtectionProfileDao(appDatabase: AppDatabase) = appDatabase.protectionProfileDao()

    @Provides
    fun provideWhitelistedFaceDao(appDatabase: AppDatabase) = appDatabase.whitelistedFaceDao()
}