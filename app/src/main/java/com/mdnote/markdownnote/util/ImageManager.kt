package com.mdnote.markdownnote.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 图片管理工具类
 * 处理图片的保存、加载和管理
 */
object ImageManager {

    // 图片存储目录名
    private const val IMAGE_DIR = "images"

    // 日志标签
    private const val TAG = "ImageManager"

    // 支持的图片格式
    private val SUPPORTED_FORMATS = listOf("jpg", "jpeg", "png", "gif", "webp")

    // 复用 Markdown 图片路径提取规则，避免每次调用重新编译
    private val MARKDOWN_IMAGE_PATH_PATTERN = Regex("""!\[.*?\]\((.*?)\)""")

    /**
     * 获取图片存储目录
     */
    fun getImageDir(context: Context): File {
        val dir = File(context.filesDir, IMAGE_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * 保存图片到应用私有目录
     * @param context 上下文
     * @param uri 图片 Uri
     * @return 保存后的相对路径（用于 Markdown 引用）
     */
    fun saveImageToPrivateStorage(context: Context, uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                // 生成唯一文件名
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "IMG_$timestamp.jpg"
                val imageFile = File(getImageDir(context), fileName)

                // 复制图片到私有目录
                FileOutputStream(imageFile).use { outputStream ->
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    while (stream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                }

                // 返回相对路径
                "$IMAGE_DIR/$fileName"
            }
        } catch (e: Exception) {
            Log.e(TAG, "保存图片到私有存储失败", e)
            null
        }
    }

    /**
     * 保存 Bitmap 到应用私有目录
     * @param context 上下文
     * @param bitmap 图片 Bitmap
     * @return 保存后的相对路径
     */
    fun saveBitmapToPrivateStorage(context: Context, bitmap: Bitmap): String? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "IMG_$timestamp.jpg"
            val imageFile = File(getImageDir(context), fileName)

            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }

            "$IMAGE_DIR/$fileName"
        } catch (e: Exception) {
            Log.e(TAG, "保存 Bitmap 到私有存储失败", e)
            null
        }
    }

    /**
     * 从相对路径加载图片文件
     */
    fun loadImageFile(context: Context, relativePath: String): File? {
        val file = File(context.filesDir, relativePath)
        return if (file.exists()) file else null
    }

    /**
     * 从相对路径加载图片 Bitmap
     */
    fun loadImageBitmap(context: Context, relativePath: String): Bitmap? {
        val file = loadImageFile(context, relativePath) ?: return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    /**
     * 删除图片
     */
    fun deleteImage(context: Context, relativePath: String): Boolean {
        val file = File(context.filesDir, relativePath)
        return if (file.exists()) file.delete() else false
    }

    /**
     * 获取所有图片列表
     */
    fun getAllImages(context: Context): List<File> {
        val imageDir = getImageDir(context)
        return imageDir.listFiles()?.filter { file ->
            file.extension.lowercase() in SUPPORTED_FORMATS
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * 清理未使用的图片
     * @param context 上下文
     * @param usedPaths 正在使用的图片路径列表
     */
    fun cleanupUnusedImages(context: Context, usedPaths: List<String>) {
        val imageDir = getImageDir(context)
        imageDir.listFiles()?.forEach { file ->
            val relativePath = "$IMAGE_DIR/${file.name}"
            if (relativePath !in usedPaths && file.extension.lowercase() in SUPPORTED_FORMATS) {
                file.delete()
            }
        }
    }

    /**
     * 获取图片的绝对路径
     */
    fun getAbsolutePath(context: Context, relativePath: String): String {
        return File(context.filesDir, relativePath).absolutePath
    }

    /**
     * 检查路径是否为应用内图片路径
     */
    fun isInternalImagePath(path: String): Boolean {
        return path.startsWith("$IMAGE_DIR/") || path.startsWith("/$IMAGE_DIR/")
    }

    /**
     * 压缩图片
     * @param context 上下文
     * @param uri 原图 Uri
     * @param maxWidth 最大宽度
     * @param quality 压缩质量 (0-100)
     * @return 压缩后的相对路径
     */
    fun compressAndSaveImage(
        context: Context,
        uri: Uri,
        maxWidth: Int = 1920,
        quality: Int = 85
    ): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null

            // 先获取图片尺寸
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            // 计算采样率
            val sampleSize = calculateSampleSize(options.outWidth, options.outHeight, maxWidth)

            // 重新打开流并解码
            val newInputStream = context.contentResolver.openInputStream(uri) ?: return null
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val bitmap = BitmapFactory.decodeStream(newInputStream, null, decodeOptions)
            newInputStream.close()

            if (bitmap != null) {
                val result = saveBitmapToPrivateStorage(context, bitmap)
                bitmap.recycle()
                result
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "压缩保存图片失败", e)
            null
        }
    }

    /**
     * 计算采样率
     */
    private fun calculateSampleSize(width: Int, height: Int, maxWidth: Int): Int {
        var sampleSize = 1
        if (width > maxWidth || height > maxWidth) {
            val halfWidth = width / 2
            val halfHeight = height / 2
            while (halfWidth / sampleSize >= maxWidth && halfHeight / sampleSize >= maxWidth) {
                sampleSize *= 2
            }
        }
        return sampleSize
    }

    /**
     * 生成 Markdown 图片语法
     */
    fun generateMarkdownImage(altText: String = "图片", relativePath: String): String {
        return "![$altText]($relativePath)"
    }

    /**
     * 从 Markdown 内容中提取所有图片路径
     */
    fun extractImagePaths(content: String): List<String> {
        return MARKDOWN_IMAGE_PATH_PATTERN.findAll(content).map { it.groupValues[1] }.toList()
    }
}
