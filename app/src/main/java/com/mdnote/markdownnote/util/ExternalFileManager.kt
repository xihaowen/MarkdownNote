package com.mdnote.markdownnote.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.mdnote.markdownnote.data.repository.FolderRepository
import com.mdnote.markdownnote.data.repository.RecentExternalFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 外部文件状态
 */
data class ExternalFileState(
    val fileUri: Uri? = null,
    val folderUri: Uri? = null,
    val fileName: String = "",
    val content: String = "",
    val isLoading: Boolean = false
)

/**
 * 外部文件管理器
 * 处理从文件管理器打开的文件
 */
@Singleton
class ExternalFileManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val folderRepository: FolderRepository
) {
    // 当前打开的外部文件
    private val _currentExternalFile = MutableStateFlow(ExternalFileState())
    val currentExternalFile: StateFlow<ExternalFileState> = _currentExternalFile.asStateFlow()

    /**
     * 加载外部文件
     * @return 文件信息，如果加载失败返回 null
     */
    suspend fun loadExternalFile(fileUri: Uri): ExternalFileState? {
        _currentExternalFile.value = _currentExternalFile.value.copy(isLoading = true)

        val content = folderRepository.readFileContent(fileUri)
        if (content == null) {
            _currentExternalFile.value = ExternalFileState()
            return null
        }

        val fileName = getFileName(fileUri)
        val folderUri = folderRepository.getFolderForFile(fileUri)

        val state = ExternalFileState(
            fileUri = fileUri,
            folderUri = folderUri,
            fileName = fileName,
            content = content,
            isLoading = false
        )

        _currentExternalFile.value = state
        return state
    }

    /**
     * 加载外部文件并关联文件夹
     */
    suspend fun loadExternalFileWithFolder(fileUri: Uri, folderUri: Uri): ExternalFileState? {
        _currentExternalFile.value = _currentExternalFile.value.copy(isLoading = true)

        val content = folderRepository.readFileContent(fileUri)
        if (content == null) {
            _currentExternalFile.value = ExternalFileState()
            return null
        }

        val fileName = getFileName(fileUri)

        val state = ExternalFileState(
            fileUri = fileUri,
            folderUri = folderUri,
            fileName = fileName,
            content = content,
            isLoading = false
        )

        _currentExternalFile.value = state

        // 添加到最近文件
        folderRepository.addRecentFile(
            RecentExternalFile(
                fileUri = fileUri,
                folderUri = folderUri,
                fileName = fileName
            )
        )

        return state
    }

    /**
     * 清除当前外部文件状态
     */
    fun clearExternalFile() {
        _currentExternalFile.value = ExternalFileState()
    }

    /**
     * 获取文件名（从 URI）
     */
    fun getFileName(uri: Uri): String {
        // 尝试通过 ContentResolver 获取文件名
        val fileName = try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        cursor.getString(nameIndex)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }

        // 如果 ContentResolver 查询失败，尝试从 URI 解析
        val finalName = fileName ?: run {
            val lastSegment = uri.lastPathSegment ?: return "外部文件"
            lastSegment
                .substringAfterLast(":")
                .substringAfterLast("/")
        }

        // 移除扩展名
        return finalName
            .removeSuffix(".md")
            .removeSuffix(".markdown")
            .removeSuffix(".MD")
            .removeSuffix(".MARKDOWN")
            .ifEmpty { "外部文件" }
    }

    /**
     * 获取文件所在目录路径
     */
    fun getParentPath(uri: Uri): String? {
        return when (uri.scheme) {
            "content" -> {
                try {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val parentId = docId.substringBeforeLast("/", "")
                    if (parentId.isNotEmpty()) parentId else null
                } catch (e: Exception) {
                    null
                }
            }
            "file" -> {
                uri.path?.substringBeforeLast("/")
            }
            else -> null
        }
    }

    /**
     * 创建分享 Intent
     */
    fun createShareIntent(title: String, content: String): Intent? {
        return ExportManager.createShareIntent(context, title, content)
    }
}
