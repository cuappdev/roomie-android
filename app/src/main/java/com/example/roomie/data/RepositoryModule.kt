package com.example.roomie.data

import com.example.roomie.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Picks the app's [RoomieRepository]: the Roomie backend by default, or the in-memory demo data
 * when `roomie.fakeData=true` is in `local.properties` (handy without a backend running).
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideRoomieRepository(remote: Provider<RemoteRoomieRepository>): RoomieRepository =
        if (BuildConfig.USE_FAKE_DATA) FakeRoomieRepository else remote.get()
}
