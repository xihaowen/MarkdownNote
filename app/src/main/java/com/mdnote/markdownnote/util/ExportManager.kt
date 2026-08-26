package com.mdnote.markdownnote.util

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 导出和分享管理工具类
 * 处理 Markdown 文件的导出和分享
 */
object ExportManager {

    // 导出目录名
    private const val EXPORT_DIR = "MarkdownNote"

    // 日志标签
    private const val TAG = "ExportManager"

    // 复用文件名非法字符替换规则，避免每次导出重新编译
    private val INVALID_FILE_NAME_CHARACTER_PATTERN = Regex("[\\\\/:*?\"<>|]")

    /**
     * 导出为 .md 文件到应用私有目录
     * @param context 上下文
     * @param fileName 文件名（不含扩展名）
     * @param content Markdown 内容
     * @return 导出的文件
     */
    fun exportToPrivateStorage(
        context: Context,
        fileName: String,
        content: String
    ): File? {
        return try {
            val exportDir = File(context.filesDir, EXPORT_DIR)
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val safeFileName = sanitizeFileName(fileName)
            val file = File(exportDir, "$safeFileName.md")

            FileOutputStream(file).use { outputStream ->
                outputStream.write(content.toByteArray(Charsets.UTF_8))
            }

            file
        } catch (e: Exception) {
            Log.e(TAG, "导出到私有存储失败", e)
            null
        }
    }

    /**
     * 导出为 .md 文件到公共下载目录
     * 需要 Android 10+ 使用 MediaStore，或申请存储权限
     * @param context 上下文
     * @param fileName 文件名
     * @param content Markdown 内容
     * @return 导出的文件路径
     */
    fun exportToDownloads(
        context: Context,
        fileName: String,
        content: String
    ): String? {
        return try {
            // 在 Android 10+ 使用应用私有目录，然后通过 MediaStore
            // 为简化实现，这里导出到应用私有目录
            val file = exportToPrivateStorage(context, fileName, content)
            file?.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "导出到下载目录失败", e)
            null
        }
    }

    /**
     * 创建分享 Intent
     * @param context 上下文
     * @param fileName 文件名
     * @param content Markdown 内容
     * @return 分享 Intent
     */
    fun createShareIntent(
        context: Context,
        fileName: String,
        content: String
    ): Intent? {
        return try {
            // 先导出文件
            val file = exportToPrivateStorage(context, fileName, content) ?: return null

            // 获取文件 Uri
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            // 创建分享 Intent
            Intent(Intent.ACTION_SEND).apply {
                type = "text/markdown"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                putExtra(Intent.EXTRA_TEXT, content)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            Log.e(TAG, "创建分享 Intent 失败", e)
            null
        }
    }

    /**
     * 创建纯文本分享 Intent
     * @param content 要分享的文本内容
     * @return 分享 Intent
     */
    fun createTextShareIntent(content: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
        }
    }

    /**
     * 导出所有笔记为单个文件
     * @param context 上下文
     * @param notes 笔记列表（Pair<标题, 内容>）
     * @return 导出的文件
     */
    fun exportAllNotes(
        context: Context,
        notes: List<Pair<String, String>>
    ): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "所有笔记_$timestamp"

            val content = notes.joinToString("\n\n---\n\n") { (title, text) ->
                "# $title\n\n$text"
            }

            exportToPrivateStorage(context, fileName, content)
        } catch (e: Exception) {
            Log.e(TAG, "导出所有笔记失败", e)
            null
        }
    }

    /**
     * 清理文件名（移除非法字符）
     */
    private fun sanitizeFileName(fileName: String): String {
        val safeName = fileName.replace(INVALID_FILE_NAME_CHARACTER_PATTERN, "_")
        return if (safeName.isNotBlank()) safeName else "untitled"
    }

    /**
     * 获取导出文件列表
     */
    fun getExportedFiles(context: Context): List<File> {
        val exportDir = File(context.filesDir, EXPORT_DIR)
        return exportDir.listFiles()?.filter {
            it.extension == "md"
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * 删除导出文件
     */
    fun deleteExportedFile(file: File): Boolean {
        return if (file.exists()) file.delete() else false
    }
}
