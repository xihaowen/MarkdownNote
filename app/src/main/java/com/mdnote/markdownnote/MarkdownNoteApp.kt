package com.mdnote.markdownnote

import android.app.Application
import com.mdnote.markdownnote.util.SettingsManager
import com.mdnote.markdownnote.worker.DreamWorker
import com.mdnote.markdownnote.worker.TrashCleanupWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MarkdownNoteApp : Application() {

    // 应用级别的 CoroutineScope，绑定应用生命周期
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate() {
        super.onCreate()

        // 调度梦境整理 Worker
        applicationScope.launch {
            val settings = settingsManager.settings.first()
            if (settings.aiDreamEnabled && settings.aiEnabled) {
                DreamWorker.schedule(applicationContext)
            }
        }

        // 调度回收站自动清理 Worker
        TrashCleanupWorker.schedule(applicationContext)
    }
}
