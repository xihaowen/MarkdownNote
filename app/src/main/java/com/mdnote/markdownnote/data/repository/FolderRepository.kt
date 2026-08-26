package com.mdnote.markdownnote.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 文件夹中的文件信息
 */
data class FolderFile(
    val name: String,
    val uri: Uri,
    val mimeType: String,
    val isDirectory: Boolean,
    val lastModified: Long = 0,
    val size: Long = 0
)

/**
 * 最近打开的外部文件
 */
data class RecentExternalFile(
    val fileUri: Uri,
    val folderUri: Uri,
    val fileName: String,
    val lastOpened: Long = System.currentTimeMillis()
)

/**
 * 文件夹数据仓库
 * 用于管理用户选择的文件夹及最近打开的文件
 */
@Singleton
class FolderRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "FolderRepository"
        private const val PREFS_NAME = "folder_manager_prefs"
        private const val KEY_FOLDER_URI = "current_folder_uri"
        private const val KEY_RECENT_FILES = "recent_external_files"
        private const val MAX_RECENT_FILES = 20
    }

    // 当前文件夹 URI
    private val _currentFolderUri = MutableStateFlow<Uri?>(null)
    val currentFolderUri: StateFlow<Uri?> = _currentFolderUri.asStateFlow()

    // 最近打开的文件
    private val _recentFiles = MutableStateFlow<List<RecentExternalFile>>(emptyList())
    val recentFiles: StateFlow<List<RecentExternalFile>> = _recentFiles.asStateFlow()

    // 初始化状态
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // 内存缓存
    private var cachedFolderUri: Uri? = null
    private val recentFilesLock = Any()
    private var cachedRecentFiles: MutableList<RecentExternalFile> = mutableListOf()

    /**
     * 初始化（从持久化存储加载）
     */
    fun initialize() {
        if (_isInitialized.value) return

        val prefs = getPrefs()

        // 加载文件夹 URI
        val uriString = prefs.getString(KEY_FOLDER_URI, null)
        if (uriString != null) {
            val uri = Uri.parse(uriString)
            if (hasFolderPermission(uri)) {
                cachedFolderUri = uri
                _currentFolderUri.value = uri
            } else {
                prefs.edit().remove(KEY_FOLDER_URI).apply()
            }
        }

        // 加载最近打开的文件
        loadRecentFiles(prefs)

        _isInitialized.value = true
    }

    /**
     * 检查是否有文件夹的访问权限
     */
    fun hasFolderPermission(folderUri: Uri): Boolean {
        return try {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                folderUri,
                DocumentsContract.getTreeDocumentId(folderUri)
            )
            context.contentResolver.query(
                childrenUri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                null, null, null
            )?.use { cursor ->
                cursor.count >= 0
            } ?: false
        } catch (e: Exception) {
            Log.w(TAG, "文件夹权限验证失败: ${e.message}")
            false
        }
    }

    private fun getPrefs(): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 获取当前文件夹 URI
     */
    fun getCurrentFolderUri(): Uri? = cachedFolderUri

    /**
     * 设置当前文件夹 URI（并持久化）
     */
    fun setCurrentFolderUri(uri: Uri?) {
        cachedFolderUri = uri
        _currentFolderUri.value = uri
        val prefs = getPrefs()
        if (uri != null) {
            prefs.edit().putString(KEY_FOLDER_URI, uri.toString()).apply()
        } else {
            prefs.edit().remove(KEY_FOLDER_URI).apply()
        }
    }

    // ==================== 最近打开文件管理 ====================

    /**
     * 添加最近打开的文件
     */
    fun addRecentFile(file: RecentExternalFile) = synchronized(recentFilesLock) {
        // 移除已存在的相同文件
        cachedRecentFiles.removeAll { it.fileUri == file.fileUri }

        // 添加到列表开头
        cachedRecentFiles.add(0, file)

        // 限制数量
        while (cachedRecentFiles.size > MAX_RECENT_FILES) {
            cachedRecentFiles.removeAt(cachedRecentFiles.size - 1)
        }

        // 更新 Flow 并保存
        _recentFiles.value = cachedRecentFiles.toList()
        saveRecentFiles()
    }

    /**
     * 获取最近打开的文件列表
     */
    fun getRecentFiles(): List<RecentExternalFile> = synchronized(recentFilesLock) { cachedRecentFiles.toList() }

    /**
     * 根据文件 URI 获取关联的文件夹 URI
     */
    fun getFolderForFile(fileUri: Uri): Uri? = synchronized(recentFilesLock) {
        cachedRecentFiles.find { it.fileUri == fileUri }?.folderUri
            ?: cachedFolderUri
    }

    /**
     * 移除最近文件
     */
    fun removeRecentFile(fileUri: Uri) = synchronized(recentFilesLock) {
        cachedRecentFiles.removeAll { it.fileUri == fileUri }
        _recentFiles.value = cachedRecentFiles.toList()
        saveRecentFiles()
    }

    /**
     * 清空最近文件
     */
    fun clearRecentFiles() = synchronized(recentFilesLock) {
        cachedRecentFiles.clear()
        _recentFiles.value = emptyList()
        saveRecentFiles()
    }

    private fun loadRecentFiles(prefs: SharedPreferences) {
        synchronized(recentFilesLock) {
            try {
                val jsonStr = prefs.getString(KEY_RECENT_FILES, null) ?: return
                val jsonArray = JSONArray(jsonStr)
                cachedRecentFiles.clear()
                for (i in 0 until jsonArray.length()) {
                    val json = jsonArray.getJSONObject(i)
                    cachedRecentFiles.add(RecentExternalFile(
                    fileUri = Uri.parse(json.getString("fileUri")),
                    folderUri = Uri.parse(json.getString("folderUri")),
                    fileName = json.getString("fileName"),
                    lastOpened = json.getLong("lastOpened")
                ))
            }
            _recentFiles.value = cachedRecentFiles.toList()
        } catch (e: Exception) {
            Log.e(TAG, "加载最近文件失败", e)
        }
        }
    }

    private fun saveRecentFiles() {
        try {
            val jsonArray = JSONArray()
            cachedRecentFiles.forEach { file ->
                val json = JSONObject().apply {
                    put("fileUri", file.fileUri.toString())
                    put("folderUri", file.folderUri.toString())
                    put("fileName", file.fileName)
                    put("lastOpened", file.lastOpened)
                }
                jsonArray.put(json)
            }
            getPrefs().edit().putString(KEY_RECENT_FILES, jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "保存最近文件失败", e)
        }
    }

    // ==================== 文件操作 ====================

    /**
     * 列出文件夹中的文件
     */
    suspend fun listFiles(folderUri: Uri): List<FolderFile> = withContext(Dispatchers.IO) {
        val files = mutableListOf<FolderFile>()

        try {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                folderUri,
                DocumentsContract.getTreeDocumentId(folderUri)
            )

            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    DocumentsContract.Document.COLUMN_SIZE
                ),
                null, null,
                "${DocumentsContract.Document.COLUMN_DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIndex)
                    val name = cursor.getString(nameIndex)
                    val mimeType = cursor.getString(mimeIndex)
                    val lastModified = if (modifiedIndex >= 0) cursor.getLong(modifiedIndex) else 0
                    val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0

                    val isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, id)

                    files.add(FolderFile(
                        name = name,
                        uri = fileUri,
                        mimeType = mimeType,
                        isDirectory = isDirectory,
                        lastModified = lastModified,
                        size = size
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "列出文件失败", e)
        }

        files
    }

    /**
     * 获取 Markdown 文件列表
     */
    suspend fun getMarkdownFiles(folderUri: Uri): List<FolderFile> {
        return listFiles(folderUri).filter { file ->
            !file.isDirectory && (
                file.name.endsWith(".md", ignoreCase = true) ||
                file.name.endsWith(".markdown", ignoreCase = true) ||
                file.mimeType == "text/markdown" ||
                file.mimeType == "text/x-markdown" ||
                file.mimeType.startsWith("text/")
            )
        }
    }

    /**
     * 读取文件内容
     */
    suspend fun readFileContent(fileUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "读取文件失败", e)
            null
        }
    }

    /**
     * 获取文件夹名称
     */
    fun getFolderName(folderUri: Uri): String {
        return try {
            context.contentResolver.query(
                folderUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        return cursor.getString(nameIndex)
                    }
                }
            }

            // 从 URI 中提取名称
            DocumentsContract.getTreeDocumentId(folderUri).substringAfterLast("/")
        } catch (e: Exception) {
            "文件夹"
        }
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
     * 解析相对于文件夹的图片路径
     */
    suspend fun resolveImageUri(
        folderUri: Uri,
        imagePath: String
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            // 标准化路径
            val normalizedPath = imagePath
                .replace('\\', '/')
                .removePrefix("./")
                .let { path -> generateSequence(path) { if (it.startsWith("../")) it.removePrefix("../") else null }.lastOrNull() ?: path }

            Log.d(TAG, "解析图片路径: $imagePath -> $normalizedPath")

            // 获取文件夹 ID
            val folderId = DocumentsContract.getTreeDocumentId(folderUri)

            // 逐级查找文件
            val pathParts = normalizedPath.split("/").filter { it.isNotEmpty() }
            var currentParentId = folderId

            for ((index, part) in pathParts.withIndex()) {
                val isLastPart = index == pathParts.size - 1

                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, currentParentId)

                var found = false
                context.contentResolver.query(
                    childrenUri,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE
                    ),
                    null, null, null
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                        val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                        val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)

                        val name = cursor.getString(nameIndex)
                        val mimeType = cursor.getString(mimeIndex)
                        val id = cursor.getString(idIndex)

                        if (name == part) {
                            if (isLastPart) {
                                // 找到目标文件
                                Log.d(TAG, "找到图片: $name")
                                return@withContext DocumentsContract.buildDocumentUriUsingTree(folderUri, id)
                            } else if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                                // 进入子目录
                                currentParentId = id
                                found = true
                                break
                            }
                        }
                    }
                }

                if (!found && !isLastPart) {
                    Log.d(TAG, "未找到目录: $part")
                    return@withContext null
                }
            }

            Log.d(TAG, "未找到图片: $normalizedPath")
            null
        } catch (e: Exception) {
            Log.e(TAG, "解析图片路径失败", e)
            null
        }
    }
}
