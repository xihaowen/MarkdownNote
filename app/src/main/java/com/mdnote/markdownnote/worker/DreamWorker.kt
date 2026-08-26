package com.mdnote.markdownnote.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mdnote.markdownnote.data.ai.dream.DreamManager
import com.mdnote.markdownnote.util.SettingsManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 梦境周期 Worker
 * 定期执行三阶段梦境整理，对 AI 记忆进行去重、摘要和根记忆整合
 */
@HiltWorker
class DreamWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val dreamManager: DreamManager,
    private val settingsManager: SettingsManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "dream_cleanup_work"

        /**
         * 调度每日梦境整理任务
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DreamWorker>(
                repeatInterval = 24,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .setRequiresDeviceIdle(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // 检查梦境功能是否启用
        val settings = settingsManager.settings.first()
        if (!settings.aiDreamEnabled || !settings.aiEnabled) {
            return@withContext Result.success()
        }

        return@withContext try {
            val result = dreamManager.runDreamCycle()
            if (result.failed) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
