package com.mdnote.markdownnote.di

import android.content.Context
import androidx.room.Room
import com.mdnote.markdownnote.BuildConfig
import com.mdnote.markdownnote.data.ai.AiProviderPresets
import com.mdnote.markdownnote.data.local.AppDatabase
import com.mdnote.markdownnote.util.SettingsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        val builder = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "markdown_note_db"
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7
            )
        // 仅 Debug 构建允许 fallback，Release 构建宁可崩溃也不静默删数据
        if (BuildConfig.DEBUG) {
            builder.fallbackToDestructiveMigration()
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideSettingsManager(
        @ApplicationContext context: Context,
        aiProviderPresets: AiProviderPresets
    ): SettingsManager {
        return SettingsManager(context, aiProviderPresets)
    }
}
