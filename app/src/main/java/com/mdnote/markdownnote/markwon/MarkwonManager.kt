package com.mdnote.markdownnote.markwon

import android.content.Context
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TableAwareMovementMethod
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.image.coil.CoilImagesPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin

/**
 * Markwon 实例管理器
 * 负责创建和缓存 Markwon 实例
 */
object MarkwonManager {

    @Volatile
    private var darkInstance: Markwon? = null
    @Volatile
    private var lightInstance: Markwon? = null

    /**
     * 获取 Markwon 实例
     * @param context 上下文
     * @param isDarkTheme 是否深色主题
     * @return Markwon 实例
     */
    fun getMarkwon(context: Context, isDarkTheme: Boolean): Markwon {
        return if (isDarkTheme) {
            darkInstance ?: synchronized(this) {
                darkInstance ?: createMarkwon(context).also { darkInstance = it }
            }
        } else {
            lightInstance ?: synchronized(this) {
                lightInstance ?: createMarkwon(context).also { lightInstance = it }
            }
        }
    }

    private fun createMarkwon(context: Context): Markwon {
        return Markwon.builder(context)
            // 内联解析器 (必需)
            .usePlugin(MarkwonInlineParserPlugin.create())
            // Coil 图片加载
            .usePlugin(CoilImagesPlugin.create(context))
            // 表格支持
            .usePlugin(TablePlugin.create(context))
            // 任务列表
            .usePlugin(TaskListPlugin.create(context))
            // 删除线
            .usePlugin(StrikethroughPlugin.create())
            .build()
    }

    /**
     * 清除缓存的主题实例
     * 用于主题切换时重新创建实例
     */
    fun invalidate() {
        darkInstance = null
        lightInstance = null
    }

    /**
     * 获取 TableAwareMovementMethod 实例
     * 用于支持表格内的点击和滚动
     */
    fun getTableAwareMovementMethod(): android.text.method.MovementMethod {
        return TableAwareMovementMethod.create()
    }
}
