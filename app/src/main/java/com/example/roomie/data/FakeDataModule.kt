package com.example.roomie.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Lets ViewModels inject the in-memory data until each screen moves to its real repository. */
@Module
@InstallIn(SingletonComponent::class)
object FakeDataModule {

    @Provides
    fun provideFakeRoomieRepository(): FakeRoomieRepository = FakeRoomieRepository
}
