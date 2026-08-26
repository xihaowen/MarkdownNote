package com.mdnote.markdownnote.domain.model

import android.net.Uri

/**
 * 笔记领域模型
 * 用于业务逻辑层的数据传递
 */
data class Note(
    val id: Long = 0,
    val title: String,
    val content: String,
    val lastModifiedTime: Long,
    val isStarred: Boolean = false,
    val isPinned: Boolean = false,
    val isDeleted: Boolean = false,        // 是否在回收站
    val deletedAt: Long? = null,           // 删除时间（用于30天自动清理）
    val externalFolderUri: String? = null  // 外部文件夹 URI（用于解析相对图片路径）
)
