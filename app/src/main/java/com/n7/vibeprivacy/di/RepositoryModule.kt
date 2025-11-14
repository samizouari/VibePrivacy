package com.n7.vibeprivacy.di

import com.n7.vibeprivacy.data.source.AppRepository
import com.n7.vibeprivacy.data.source.Repository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRepository(repository: AppRepository): Repository
}