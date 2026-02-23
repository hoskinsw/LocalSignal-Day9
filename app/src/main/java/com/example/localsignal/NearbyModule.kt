package com.example.localsignal

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.ConnectionsClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NearbyModule {

    @Provides
    @Singleton
    fun provideConnectionsClient(
        @ApplicationContext context: Context
    ): ConnectionsClient {
        return Nearby.getConnectionsClient(context)
    }
}