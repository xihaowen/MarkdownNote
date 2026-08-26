package com.mdnote.markdownnote.data.ai

import com.mdnote.markdownnote.domain.model.AiMessage
import kotlinx.coroutines.flow.Flow

/**
 * AI 服务接口
 * 定义笔记 AI 辅助功能的五大核心操作
 */
interface AiService {
    /**
     * 扫描分析笔记内容
     * 总结要点，指出语法、结构或逻辑问题
     */
    fun scan(content: String): Flow<AiResult>

    /**
     * 根据指令修改笔记内容
     * @param content 当前笔记全文
     * @param instruction 修改指令
     * @param selection 选中的文本，null 表示修改全文
     */
    fun modify(content: String, instruction: String, selection: String?): Flow<AiResult>

    /**
     * 根据提示生成 Markdown 内容
     * @param prompt 用户描述
     * @param context 可选的笔记上下文
     */
    fun generate(prompt: String, context: String?): Flow<AiResult>

    /**
     * 解释选中文本
     * @param selectedText 选中的文本
     * @param context 笔记上下文
     */
    fun explain(selectedText: String, context: String): Flow<AiResult>

    /**
     * 多轮对话
     * @param messages 完整对话历史
     */
    fun chat(messages: List<AiMessage>): Flow<AiResult>
}
