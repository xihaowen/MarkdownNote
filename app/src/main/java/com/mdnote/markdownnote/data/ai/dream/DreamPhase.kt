package com.mdnote.markdownnote.data.ai.dream

/**
 * 梦境阶段枚举
 * 模拟人类睡眠三阶段，逐步对记忆进行深度整理
 */
enum class DreamPhaseEnum {
    LIGHT_SLEEP,   // 浅睡：整理近期片段记忆，去重、合并
    DEEP_SLEEP,    // 深睡：提炼为每日摘要
    REM            // REM：整合为根记忆
}

/**
 * 梦境阶段状态
 * @param phase 阶段类型
 * @param description 阶段描述
 * @param isRunning 是否正在执行
 */
data class DreamPhase(
    val phase: DreamPhaseEnum,
    val description: String,
    val isRunning: Boolean = false
)
