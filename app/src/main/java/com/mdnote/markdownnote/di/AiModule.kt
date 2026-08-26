package com.mdnote.markdownnote.di

import com.mdnote.markdownnote.data.ai.AiService
import com.mdnote.markdownnote.data.ai.AiServiceImpl
import com.mdnote.markdownnote.data.ai.memory.MemoryManager
import com.mdnote.markdownnote.data.ai.soul.SoulManager
import com.mdnote.markdownnote.data.local.AppDatabase
import com.mdnote.markdownnote.data.local.MemoryDao
import com.mdnote.markdownnote.util.SettingsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/** 限定符：SSE 流式请求专用的 OkHttpClient */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SseClient

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /** SSE 流式请求专用的 Client，readTimeout 设为 0（无限等待） */
    @Provides
    @Singleton
    @SseClient
    fun provideSseOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideMemoryDao(database: AppDatabase): MemoryDao {
        return database.memoryDao()
    }

    @Provides
    @Singleton
    fun provideAiService(
        okHttpClient: OkHttpClient,
        @SseClient sseClient: OkHttpClient,
        soulManager: SoulManager,
        memoryManager: MemoryManager,
        settingsManager: SettingsManager
    ): AiService {
        return AiServiceImpl(okHttpClient, sseClient, soulManager, memoryManager, settingsManager)
    }
}
