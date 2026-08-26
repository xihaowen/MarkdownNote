package com.mdnote.markdownnote.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * 时间工具类
 * 用于格式化时间显示
 */
object TimeUtils {

    /**
     * 格式化时间戳为友好显示
     * - 今天：显示时间
     * - 昨天：显示"昨天"
     * - 一周内：显示星期几
     * - 其他：显示完整日期
     */
    fun formatTime(timestamp: Long): String {
        // 每次读取当前 Locale；SimpleDateFormat 也不是线程安全对象，不能作为单例字段共享。
        val locale = Locale.getDefault()
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", locale)
        val timeFormat = SimpleDateFormat("HH:mm", locale)
        val dateTimeFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", locale)
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val nowCalendar = Calendar.getInstance().apply { timeInMillis = now }

        return when {
            isSameDay(calendar, nowCalendar) -> {
                "今天 ${timeFormat.format(Date(timestamp))}"
            }
            isYesterday(calendar, nowCalendar) -> {
                "昨天 ${timeFormat.format(Date(timestamp))}"
            }
            isSameWeek(calendar, nowCalendar) -> {
                val weekdays = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
                "${weekdays[calendar.get(Calendar.DAY_OF_WEEK) - 1]} ${timeFormat.format(Date(timestamp))}"
            }
            isSameYear(calendar, nowCalendar) -> {
                dateFormat.format(Date(timestamp))
            }
            else -> {
                dateTimeFormat.format(Date(timestamp))
            }
        }
    }

    /**
     * 判断是否同一天
     */
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * 判断是否昨天
     */
    private fun isYesterday(cal1: Calendar, cal2: Calendar): Boolean {
        cal2.add(Calendar.DAY_OF_YEAR, -1)
        return isSameDay(cal1, cal2).also {
            cal2.add(Calendar.DAY_OF_YEAR, 1) // 恢复
        }
    }

    /**
     * 判断是否同一周
     */
    private fun isSameWeek(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR)
    }

    /**
     * 判断是否同一年
     */
    private fun isSameYear(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
    }
}
