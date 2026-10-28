package com.freelibrary.app.di

import android.content.Context
import com.freelibrary.app.data.download.DownloadClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private const val CONNECT_TIMEOUT_SECONDS = 20L
private const val READ_TIMEOUT_SECONDS = 30L

@Module
@InstallIn(SingletonComponent::class)
object DownloadModule {
    /**
     * The client for book downloads. Project Gutenberg asks apps to say who they are with a contact
     * address, so every request carries one. HTTPS to HTTP redirects are not followed, and Android
     * itself refuses plain HTTP for this app.
     */
    @Provides
    @Singleton
    @DownloadClient
    fun provideDownloadClient(
        @ApplicationContext context: Context,
    ): OkHttpClient {
        val userAgent = appUserAgent(context)
        return OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .followSslRedirects(false)
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("User-Agent", userAgent).build())
            }
            .build()
    }
}
