package com.freelibrary.app.di

import android.content.Context
import com.freelibrary.app.data.AndroidNetworkState
import com.freelibrary.app.data.NetworkState
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConnectivityModule {
    @Provides
    @Singleton
    fun provideNetworkState(
        @ApplicationContext context: Context,
    ): NetworkState = AndroidNetworkState(context)
}
